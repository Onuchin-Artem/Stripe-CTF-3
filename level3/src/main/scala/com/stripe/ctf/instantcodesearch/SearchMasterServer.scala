package com.stripe.ctf.instantcodesearch

import com.twitter.util.{Promise, Future}
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpResponseStatus}
import org.jboss.netty.util.CharsetUtil.UTF_8
import java.io.File
import sun.nio.cs.US_ASCII
import com.google.gson.Gson
import com.google.common.base.Charsets
import scala.concurrent.Await
import scala.concurrent.duration.{Duration}
import java.util.concurrent.TimeUnit
import main.scala.com.stripe.ctf.instantcodesearch.SearcherService

class SearchMasterServer(port: Int, id: Int) extends AbstractSearchServer(port, id) {
  val NumNodes = 3
  lazy val searcher = new SearcherService(4)

  def this(port: Int) { this(port, 0) }

  val clients = (1 to NumNodes)
    .map { id => new SearchServerClient(port + id, id)}
    .toArray


  override def isIndexed() = {
    val responsesF = Future.collect(searcher.isIndexed :: clients.map {client => client.isIndexed()}.toList)
    val successF = responsesF.map {responses => responses.forall { response =>

        (response.getStatus() == HttpResponseStatus.OK
          && response.getContent.toString(UTF_8).contains("true"))
      }
    }
    successF.map {success =>
      if (success) {
        successResponse()
      } else {
        errorResponse(HttpResponseStatus.BAD_GATEWAY, "Nodes are not indexed")
      }
    }.rescue {
      case ex: Exception => Future.value(
        errorResponse(HttpResponseStatus.BAD_GATEWAY, "Nodes are not indexed")
      )
    }
  }

  override def healthcheck() = {
    val responsesF = Future.collect(searcher.healthcheck :: clients.map {client => client.healthcheck()}.toList)
    val successF = responsesF.map {responses => responses.forall { response =>
        response.getStatus() == HttpResponseStatus.OK
      }
    }
    successF.map {success =>
      if (success) {
        successResponse()
      } else {
        errorResponse(HttpResponseStatus.BAD_GATEWAY, "All nodes are not up")
      }
    }.rescue {
      case ex: Exception => Future.value(
        errorResponse(HttpResponseStatus.BAD_GATEWAY, "All nodes are not up")
      )
    }
  }

  override def index(path: String) = {
    System.err.println(
      "[master] Requesting " + NumNodes + " nodes to index path: " + path
    )
    val responses = Future.collect(searcher.indexInter(path, 3) :: (0 to NumNodes).map {
      id : Int  => clients.apply(id).index(path, id)}.toList)
    responses.map {_ => successResponse()}
  }


  override def query(q: String) = {
    val promise = Promise[HttpResponse]
    try {
    val responses = searcher.query(q) :: clients.map {client => client.query(q)}.toList

    val matches = responses.map {
      response => readResponseInter(response.get)}.flatten

    promise.setValue(querySuccessResponse(matches))
    } catch {
      case t: Throwable  => t.printStackTrace()
    }
    promise
  }
}
