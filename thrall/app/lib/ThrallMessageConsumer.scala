package lib

import _root_.play.api.libs.json._
import com.gu.mediaservice.lib.aws.MessageConsumer
import com.gu.mediaservice.lib.logging.GridLogger
import org.elasticsearch.action.delete.DeleteResponse

import scala.concurrent.{ExecutionContext, Future}

class ThrallMessageConsumer(config: ThrallConfig, es: ElasticSearch, thrallMetrics: ThrallMetrics, store: ThrallStore, dynamoNotifications: DynamoNotifications, notifications: ThrallNotifications)(implicit ec: ExecutionContext) extends MessageConsumer(
  config.queueUrl, config.awsEndpoint, config, thrallMetrics.snsMessage) {

  override def chooseProcessor(subject: String): Option[JsValue => Future[Any]] = {
    PartialFunction.condOpt(subject) {
      case "image"                      => indexImage
      case "delete-image"               => deleteImage
      case "update-image"               => indexImage
      case "delete-image-exports"       => deleteImageExports
      case "update-image-exports"       => updateImageExports
      case "update-image-user-metadata" => updateImageUserMetadata
      case "update-image-usages"        => updateImageUsages
      case "update-image-leases"        => updateImageLeases
      case "set-image-collections"      => setImageCollections
      case "heartbeat"                  => heartbeat
      case "delete-usages"              => deleteAllUsages
      case "update-rcs-rights"          => updateRcsRights // Used by the RCS-poller-lambda
      case "apply-rcs-rights"           => applyRcsRights // Used to propagate rights in an album
    }
  }

  def heartbeat(msg: JsValue) = Future {
    None
  }

  def updateImageUsages(usages: JsValue) =
   Future.sequence( withImageId(usages)(id => es.updateImageUsages(id, usages \ "data", usages \ "lastModified")) )

  def indexImage(image: JsValue) =
    Future.sequence( withImageId(image)(id => es.indexImage(id, image)) )

  def updateImageExports(exports: JsValue) =
    Future.sequence( withImageId(exports)(id => es.updateImageExports(id, exports \ "data")) )

  def deleteImageExports(exports: JsValue) =
    Future.sequence( withImageId(exports)(id => es.deleteImageExports(id)) )

  def updateImageUserMetadata(metadata: JsValue) = {
    val data = metadata \ "data"
    val lastModified = metadata \ "lastModified"
    Future.sequence( withImageId(metadata)(id => es.applyImageMetadataOverride(id, data, lastModified)))
  }

  def updateImageLeases(leaseByMedia: JsValue) =
    Future.sequence( withImageId(leaseByMedia)(id => es.updateImageLeases(id, leaseByMedia \ "data", leaseByMedia \ "lastModified")) )

  def setImageCollections(collections: JsValue) =
    Future.sequence(withImageId(collections)(id => es.setImageCollection(id, collections \ "data")) )

  def deleteImage(image: JsValue) =
    Future.sequence(
      withImageId(image) { id =>
        // if we cannot delete the image as it's "protected", succeed and delete
        // the message anyway.
        es.deleteImage(id).map { requests =>
          requests.map {
            case r: DeleteResponse =>
              store.deleteOriginal(id)
              store.deleteThumbnail(id)
              store.deletePng(id)
              dynamoNotifications.publish(Json.obj("id" -> id), "image-deleted")
              EsResponse(s"Image deleted: $id")
          } recoverWith {
            case ImageNotDeletable =>
              GridLogger.info("Could not delete image", id)
              Future.successful(EsResponse(s"Image cannot be deleted: $id"))
          }
        }
      }
    )

  def deleteAllUsages(usage: JsValue) =
    Future.sequence( withImageId(usage)(id => es.deleteAllImageUsages(id)) )

  def applyRcsRights(rightsWithId: JsValue)=
    Future.sequence(withImageId(rightsWithId) { id =>
      println(s"Apply rcs rights: $id")
      es.updateImageSyndicationRights(id, rightsWithId \ "data")
    })

  def updateRcsRights(rightsWithId: JsValue) =
    Future.sequence( withImageId(rightsWithId) { id =>
      println(s"Update rcs rights: $id")
      val rightsData = rightsWithId \ "data"
      (rightsData \ "rights").toOption match {
        case Some(_) =>
          // Apply rights to all album images that have no syndication rights
          for {
            albumName <- es.getImageAlbum(id)
            imagesInAlbum <- es.getImageIdsInAlbum(albumName)
          } yield {
            imagesInAlbum.filter(_ != id).foreach { imgId =>
              val newRights: JsValue = Json.obj("id" -> imgId, "data" -> rightsData.getOrElse(JsNull)).as[JsValue]

              notifications.publish(newRights, "apply-rcs-rights")
            }
          }

          es.updateImageSyndicationRights(id, rightsData)
        case None => es.updateImageSyndicationRights(id, rightsData)
      }
    })
}

// TODO: improve and use this (for logging especially) else where.
case class EsResponse(message: String)
