package com.gu.mediaservice.lib.logging
import ch.qos.logback.classic.Logger
import com.gu.mediaservice.lib.auth.ApiKey
import net.logstash.logback.marker.Markers

import scala.collection.JavaConverters._

object GridLogger {
  private val logger: Logger = LogConfig.rootLogger

  private def apiKeyMarkers(apiKey: ApiKey) = Map(
    "key-tier" -> apiKey.tier,
    "key-name" -> apiKey.name
  )

  private def imageIdMarker(imageId: String) = Map("image-id" -> imageId)

  def info(message: String, imageId: String): Unit = info(message, imageIdMarker(imageId))
  def info(message: String, apiKey: ApiKey): Unit = info(message, apiKeyMarkers(apiKey))
  def info(message: String, apiKey: ApiKey, imageId: String): Unit = info(message, apiKeyMarkers(apiKey) ++ imageIdMarker(imageId))
  def info(message: String, markers: Map[String, Any] = Map()): Unit = logger.info(Markers.appendEntries(markers.asJava), message)

  def warn(message: String, imageId: String): Unit = warn(message, imageIdMarker(imageId))
  def warn(message: String, apiKey: ApiKey): Unit = warn(message, apiKeyMarkers(apiKey))
  def warn(message: String, apiKey: ApiKey, imageId: String): Unit = warn(message, apiKeyMarkers(apiKey) ++ imageIdMarker(imageId))
  def warn(message: String, markers: Map[String, Any] = Map()): Unit = logger.warn(Markers.appendEntries(markers.asJava), message)

  def error(message: String, imageId: String): Unit = error(message, imageIdMarker(imageId))
  def error(message: String, apiKey: ApiKey): Unit = error(message, apiKeyMarkers(apiKey))
  def error(message: String, apiKey: ApiKey, imageId: String): Unit = error(message, apiKeyMarkers(apiKey) ++ imageIdMarker(imageId))
  def error(message: String, markers: Map[String, Any] = Map()): Unit = logger.error(Markers.appendEntries(markers.asJava), message)
}
