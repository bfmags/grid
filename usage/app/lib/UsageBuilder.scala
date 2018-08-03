package lib

import org.joda.time.DateTime
import com.gu.mediaservice.model._
import model.{MediaUsage, UsageTableFullKey}

object UsageBuilder {
  import com.gu.mediaservice.lib.IntUtils._

  def build(usage: MediaUsage) = Usage(
    buildId(usage),
    buildUsageReference(usage),
    usage.usageType,
    usage.mediaType,
    buildStatusString(usage),
    usage.dateAdded,
    usage.dateRemoved,
    usage.lastModified,
    usage.printUsageMetadata,
    usage.digitalUsageMetadata
  )

  private def buildStatusString(usage: MediaUsage): UsageStatus = if (usage.isRemoved) RemovedUsageStatus else usage.status

  private def buildId(usage: MediaUsage): String = {
    UsageTableFullKey.build(usage).toString
  }

  private def buildUsageReference(usage: MediaUsage): List[UsageReference] = {
    usage.usageType match {
      case "digital" => buildDigitalUsageReference(usage)
      case "print" => buildPrintUsageReference(usage)
    }
  }

  private def buildPrintUsageReference(usage: MediaUsage):List[UsageReference] =
    usage.printUsageMetadata.map(metadata => {
      val title = List(
        new DateTime(metadata.issueDate).toString("YYYY-MM-dd"),
        metadata.publicationName,
        metadata.sectionName,
        s"Page ${metadata.pageNumber}"
      ).mkString(", ")

      List(UsageReference("indesign", None, Some(title)))

    }).getOrElse(List[UsageReference]())

  private def buildDigitalUsageReference(usage: MediaUsage): List[UsageReference] = usage.digitalUsageMetadata.map {
    case article: ArticleUsageMetadata => List(
      UsageReference("frontend", Some(article.webUrl), Some(article.webTitle))
    ) ++ article.composerUrl.map(url => UsageReference("composer", Some(url)))
    case syndication: SyndicationUsageMetadata => List(
      UsageReference("syndication", name = Some(syndication.partnerName))
    )
  }.getOrElse(List[UsageReference]())
}
