package lumen

import java.util.Calendar
import javax.inject.Singleton

/**
  * This should be moved into the main application project eventually.
  */
@Singleton
class DefaultConfig extends Config {

  override def defineIndexFields = List(
    new IndexFieldConfig("title_display", "Title"),
    new IndexFieldConfig("title_vern_display", "Title"),
    new IndexFieldConfig("author_display", "Author"),
    new IndexFieldConfig("author_vern_display", "Author"),
    new IndexFieldConfig("format", "Format"),
    new IndexFieldConfig("language_facet", "Language"),
    new IndexFieldConfig("published_display", "Published"),
    new IndexFieldConfig("published_vern_display", "Published"),
    new IndexFieldConfig("lc_callnum_display", "Call number")
  )

  override def defineShowFields = List(
    new ShowFieldConfig("title_display", "Title"),
    new ShowFieldConfig("title_vern_display", "Title"),
    new ShowFieldConfig("subtitle_display", "Subtitle"),
    new ShowFieldConfig("subtitle_vern_display", "Subtitle"),
    new ShowFieldConfig("author_display", "Author"),
    new ShowFieldConfig("author_vern_display", "Author"),
    new ShowFieldConfig("format", "Format"),
    new ShowFieldConfig("url_fulltext_display", "URL"),
    new ShowFieldConfig("url_suppl_display", "More Information"),
    new ShowFieldConfig("language_facet", "Language"),
    new ShowFieldConfig("published_display", "Published"),
    new ShowFieldConfig("published_vern_display", "Published"),
    new ShowFieldConfig("lc_callnum_display", "Call number"),
    new ShowFieldConfig("isbn_t", "ISBN")
  )

  override def defineSearchFields = List(
    new SearchFieldConfig("all_fields", "All Fields"),
    new SearchFieldConfig("title", "Title",
      solrParameters = Map (
        "spellcheck.dictionary" -> "title"),
      solrLocalParameters = Map(
      "qf" -> "$title_qf",
      "pf" -> "$title_pf")
    ),
    new SearchFieldConfig("author", "Author",
      solrParameters = Map (
        "spellcheck.dictionary" -> "author"),
      solrLocalParameters = Map(
        "qf" -> "$author_qf",
        "pf" -> "$author_pf")
    ),
    new SearchFieldConfig("subject", "Subject",
      solrParameters = Map (
        "spellcheck.dictionary" -> "subject"),
      qt = "search",
      solrLocalParameters = Map(
        "qf" -> "$subject_qf",
        "pf" -> "$subject_pf")
    )
  )

  override def defineFacetFields = List(
    new FacetFieldConfig("format", "Format"),
    new FacetFieldConfig("pub_date", "Publication Year", single = true),
    new FacetFieldConfig("subject_topic_facet", "Topic", limit = 20,
      indexRange = Some(('A' to 'Z').map(_.toString))),
    new FacetFieldConfig("language_facet", "Language", limit = 10),
    new FacetFieldConfig("lc_1letter_facet", "Call Number"),
    new FacetFieldConfig("subject_geo_facet", "Region"),
    new FacetFieldConfig("subject_era_facet", "Era"),
    new FacetFieldConfig("example_pivot_field", "Pivot Field",
      pivot = List("format", "language_facet")
    ),
    new FacetFieldConfig("example_query_facet_field", "Publish Date",
      query = List(
        FacetQueryItem(
          paramValue = "years_5",
          label = "within 5 Years",
          fq = () => s"pub_date:[${Calendar.getInstance().get(Calendar.YEAR) - 5} TO *]"),
        FacetQueryItem(
          paramValue = "years_10",
          label = "within 10 Years",
          fq = () => s"pub_date:[${Calendar.getInstance().get(Calendar.YEAR) - 10} TO *]"),
        FacetQueryItem(
          paramValue = "years_25",
          label = "within 25 Years",
          fq = () => s"pub_date:[${Calendar.getInstance().get(Calendar.YEAR) - 25} TO *]")
      )
    )
  )

  override def defineSortFields: Seq[SortFieldConfig] = List(
    new SortFieldConfig("score desc, pub_date_sort desc, title_sort asc", "relevance"),
    new SortFieldConfig("pub_date_sort desc, title_sort asc", "year"),
    new SortFieldConfig("author_sort asc, title_sort asc", "author"),
    new SortFieldConfig("title_sort asc, pub_date_sort desc", "title")
  )
}
