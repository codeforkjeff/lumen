package lumen

object Util {

  /** this must already exist in play somewhere, but i can't find it */
  def queryString(qsMap: Map[String, Seq[String]]): String = {
    val begin = Seq.empty[(String, String)]
    val pairs = qsMap.foldLeft(begin)((acc, entry) => {
      val param = entry._1
      val values = entry._2
      values.map((param, _)) ++ acc
    })
    pairs.map(pair => pair._1 + "=" + pair._2).mkString("&")
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
