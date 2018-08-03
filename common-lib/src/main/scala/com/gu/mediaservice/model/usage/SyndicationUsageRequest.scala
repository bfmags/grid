package com.gu.mediaservice.model.usage

import com.gu.mediaservice.model.{SyndicatedUsageStatus, SyndicationUsageMetadata, UsageStatus}
import org.joda.time.DateTime
import play.api.libs.json._

case class SyndicationUsageRequest (
  name: String,
  mediaId: String,
  dateAdded: DateTime
) {
  val usageStatus: UsageStatus = SyndicatedUsageStatus
  val syndicationUsageMetadata = SyndicationUsageMetadata(name)
}

object SyndicationUsageRequest {
  import JodaWrites._
  import JodaReads._

  implicit val reads: Reads[SyndicationUsageRequest] = Json.reads[SyndicationUsageRequest]
  implicit val writes: Writes[SyndicationUsageRequest] = Json.writes[SyndicationUsageRequest]
}
