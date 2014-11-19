package com.stripe.ctf.instantcodesearch

import com.twitter.util.{Future, Promise, FuturePool}
import com.twitter.concurrent.Broker
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpResponseStatus}
import main.scala.com.stripe.ctf.instantcodesearch.SearcherService

class SearchServer(port : Int, id : Int) extends AbstractSearchServer(port, id) {
  lazy val searcher = new SearcherService(id)

  override def healthcheck() = {
    searcher.healthcheck()
  }

  override def isIndexed() = {
    searcher.isIndexed()
  }
  override def indexInter(path: String, id : Int) = {
    searcher.indexInter(path, id)
  }

  override def query(q: String) = {
    searcher.query(q)
  }
}
