package model

import com.gu.mediaservice.model.PrintUsageRecord
import com.gu.mediaservice.model.usage.SyndicationUsageRequest
import lib.MD5


case class UsageId(id: String) {
  override def toString = id
}
object UsageId {
  def buildId(parts: List[Option[Any]]) =
    UsageId(MD5.hash(parts.flatten.map(_.toString).mkString("_")))

  def build(printUsageRecord: PrintUsageRecord) = buildId(List(
    Some(printUsageRecord.mediaId),
    Some(printUsageRecord.printUsageMetadata.pageNumber),
    Some(printUsageRecord.printUsageMetadata.sectionCode),
    Some(printUsageRecord.printUsageMetadata.issueDate),
    Some(printUsageRecord.usageStatus)
  ))

  def build(mediaWrapper: MediaWrapper) = buildId(List(
    Some(mediaWrapper.mediaId),
    Some(mediaWrapper.index),
    Some(mediaWrapper.contentStatus)
  ))

  def build(syndicationUsageRequest: SyndicationUsageRequest) = buildId(List(
    Some(syndicationUsageRequest.mediaId),
    Some(syndicationUsageRequest.syndicationUsageMetadata.partnerName),
    Some(syndicationUsageRequest.usageStatus)
  ))
}
