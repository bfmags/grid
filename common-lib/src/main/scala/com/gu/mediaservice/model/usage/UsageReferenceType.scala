package com.gu.mediaservice.model.usage

import play.api.libs.json._

trait UsageReferenceType {
  override def toString: String = this match {
    case InDesignUsageReference => "indesign"
    case FrontendUsageReference => "frontend"
    case ComposerUsageReference => "composer"
    case SyndicationUsageReference => "syndication"
  }
}

object UsageReferenceType {
  implicit val reads: Reads[UsageReferenceType] = JsPath.read[String].map(UsageReferenceType(_))
  implicit val writer: Writes[UsageReferenceType] = (usageReferenceType: UsageReferenceType) => JsString(usageReferenceType.toString)

  def apply(usageReferenceType: String): UsageReferenceType = usageReferenceType.toLowerCase match {
    case "indesign" => InDesignUsageReference
    case "frontend" => FrontendUsageReference
    case "composer" => ComposerUsageReference
    case "syndication" => SyndicationUsageReference
  }
}

object InDesignUsageReference extends UsageReferenceType
object FrontendUsageReference extends UsageReferenceType
object ComposerUsageReference extends UsageReferenceType
object SyndicationUsageReference extends UsageReferenceType
