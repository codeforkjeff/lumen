package lumen

import views.html.helper

object Util {

  /**
    * this must already exist in play somewhere, but i can't find it
    * @return query string in param name order
    **/
  def queryString(qsMap: Map[String, Seq[String]]): String = {
    val pairs = qsMap.keySet.toList.sorted.flatMap((key) => {
      val values = qsMap(key)
      values.sorted.map((key, _))
    })
    pairs.map((pair) => s"${pair._1}=${helper.urlEncode(pair._2)}").mkString("&")
  }

  // TODO: implement more fully
  def humanize(s: String) =
    if(s != null) {
      s.replace('_', ' ').capitalize
    } else {
      s
    }

  def parameterSeparator = "-"

  // port of Rails' #parameterize
  def parameterize(s: String): String = {
    if(s != null) {
      s.replaceAll("/[^a-z0-9\\-_]+/", "")
        .replaceAll(s"/${parameterSeparator}{2,}/", parameterSeparator)
        .replaceAll(s"/^${parameterSeparator}|${parameterSeparator}$$/", "")
        .toLowerCase
    } else {
      s
    }
  }

  /**
    * Evaluates the passed-in function, returning a tuple of its
    * return value and the time it took to execute.
    * @param f
    * @tparam T
    * @return
    */
  def timeFunction[T](f: () => T): (T, Long) = {
    val start = System.currentTimeMillis()
    val returnVal: T = f()
    (returnVal, System.currentTimeMillis() - start)
  }

  // this is a stock rails helper
  def numberWithDelimiter(n: Number): String =
    java.text.NumberFormat.getIntegerInstance().format(n)

}
