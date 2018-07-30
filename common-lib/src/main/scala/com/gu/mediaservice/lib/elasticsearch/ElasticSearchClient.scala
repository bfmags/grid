package com.gu.mediaservice.lib.elasticsearch

import com.gu.mediaservice.lib.logging.GridLogger
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.{ImmutableSettings, Settings}
import org.elasticsearch.common.transport.InetSocketTransportAddress

trait ElasticSearchClient {

  def host: String
  def port: Int
  def cluster: String
  def imagesAlias: String

  protected val imagesIndexPrefix = "images"
  protected val imageType = "image"

  val initialImagesIndex = "images"

  private lazy val settings: Settings =
    ImmutableSettings.settingsBuilder
      .put("cluster.name", cluster)
      .put("client.transport.sniff", false)
      .build

  lazy val client: Client =
    new TransportClient(settings)
      .addTransportAddress(new InetSocketTransportAddress(host, port))

  def ensureAliasAssigned() {
    GridLogger.info(s"Checking alias $imagesAlias is assigned to index…")

    if (getCurrentAlias.isEmpty) {
      ensureIndexExists(initialImagesIndex)
      assignAliasTo(initialImagesIndex)
    }
  }

  def ensureIndexExists(index: String) {
    GridLogger.info("Checking index exists…")
    val indexExists = client.admin.indices.prepareExists(index)
                        .execute.actionGet.isExists

    if (!indexExists) createIndex(index)
  }

  def createIndex(index: String) {
    GridLogger.info(s"Creating index $index")
    client.admin.indices
      .prepareCreate(index)
      .addMapping(imageType, Mappings.imageMapping)
      .setSettings(IndexSettings.imageSettings)
      .execute.actionGet
  }

  def deleteIndex(index: String) {
    GridLogger.info(s"Deleting index $index")
    client.admin.indices.delete(new DeleteIndexRequest(index)).actionGet
  }

  def getCurrentAlias: Option[String] = {
    // getAliases returns null, so wrap it in an Option
    Option(client.admin.cluster
      .prepareState.execute
      .actionGet.getState
      .getMetaData.getAliases.get(imagesAlias))
      .map(_.keys.toArray.head.toString)
  }

  def getCurrentIndices: List[String] = {
    Option(client.admin.cluster
      .prepareState.execute
      .actionGet.getState
      .getMetaData.getAliases.get(imagesAlias))
      .map(_.keys.toArray.map(_.toString).toList).getOrElse(Nil)
  }

  def assignAliasTo(index: String) = {
    GridLogger.info(s"Assigning alias $imagesAlias to $index")
    client.admin.indices
      .prepareAliases
      .addAlias(index, imagesAlias)
      .execute.actionGet
  }

  def removeAliasFrom(index: String) = {
    GridLogger.info(s"Removing alias $imagesAlias from $index")
    client.admin.indices
      .prepareAliases
      .removeAlias(index, imagesAlias)
      .execute.actionGet
  }

}
