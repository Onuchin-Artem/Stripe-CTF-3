package main.scala.com.stripe.ctf.instantcodesearch

import com.twitter.util.{Future, Promise, FuturePool}
import com.twitter.concurrent.Broker
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpResponseStatus}
import com.stripe.ctf.instantcodesearch._;


class SearcherService(id : Int) {
  val IndexPath = "instantcodesearch-" + id + ".index"
  case class Query(q : String, broker : Broker[SearchResult])
  @volatile var searcher :Searcher = null
  @volatile var indexed = false

  def healthcheck() = {
    Future.value(successResponse())
  }

  def isIndexed() = {
    if (indexed) {
      Future.value(successResponse())
    }
    else {
      Future.value(errorResponse(HttpResponseStatus.OK, "Not indexed"))
    }
  }

  def indexInter(path: String, id : Int) = {
    System.out.println("I have " + Runtime.getRuntime().availableProcessors() + "")
    val indexer  = new Indexer(path, id)
    System.err.println("I'm here!!!!111111111 " + id)
    FuturePool.unboundedPool {
      try {
      System.err.println("[node #" + id + "] Indexing path: " + path)
      indexer.index()
      searcher = new Searcher(IndexPath, indexer.idx)
      indexed = true
      System.err.println("[node #" + id + "] Indexed ")
      } catch {
        case t : Throwable => t.printStackTrace()
      }
    }

    Future.value(successResponse())
  }

  def query(q: String) = {
    //System.err.println("[node #" + id + "] Searching for: " + q)
    try {
      handleSearch(q)
    } catch {
      case t : Throwable =>
        System.err.println("FAILLL " + id)
        t.printStackTrace()
       throw t
    }
  }

  def handleSearch(q:  String) = {
    val searches = new Broker[Query]()
    searches.recv foreach { q =>
      FuturePool.unboundedPool {searcher.search(q.q, q.broker)}
    }

    val matches = new Broker[SearchResult]()
    searches ! new Query(q, matches)

    val promise = Promise[HttpResponse]
    var results = List[Match]()

    matches.recv foreach { m =>
      m match {
        case m : Match => results = m :: results
        case Done() => promise.setValue(querySuccessResponseInter(results))
      }
    }

    promise
  }

}
