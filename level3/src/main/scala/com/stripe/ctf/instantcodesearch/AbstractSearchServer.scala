package com.stripe.ctf.instantcodesearch

import java.io._
import org.apache.commons.lang.StringUtils
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Http => HttpCodec}
import com.twitter.finagle.builder.ServerBuilder
import com.twitter.server.TwitterServer
import com.twitter.util.Future
import java.net.InetSocketAddress
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.util.CharsetUtil.UTF_8
import scala.collection.JavaConverters._
import scala.collection.Map
import scala.collection.mutable.Buffer
import java.nio.charset.StandardCharsets
import java.util

import scala.collection.JavaConversions._
import sun.security.ssl.ByteBufferInputStream
;


abstract class AbstractSearchServer(port: Int, id: Int) extends TwitterServer {
  def query(q: String): Future[HttpResponse]
  def index(path: String): Future[HttpResponse] = null
  def indexInter(path: String, id : Int): Future[HttpResponse] = null
  def healthcheck(): Future[HttpResponse]
  def isIndexed(): Future[HttpResponse]

  def handle(request: HttpRequest): Future[HttpResponse] = {
    val decoder = new QueryStringDecoder(request.getUri)
    val params = decoder.getParameters.asScala.mapValues {_.asScala}

    try {
      decoder.getPath() match {
        case "/index" => index(getParam(params, "path"))
        case "/index-inter" => indexInter(getParam(params, "path"), getParam(params, "id").toInt)
        case "/" => query(getParam(params, "q"))
        case "/healthcheck" => healthcheck()
        case "/isIndexed" => isIndexed()
        case path => throw new NotFoundException(path + " not found")
      }
    }
    catch {
      case err: HttpException => Future.value(
        errorResponse(err.code, err.message)
      )
      case _: Exception => {
        Future.value(
          errorResponse(
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            "Something went wrong"
          )
        )
      }
    }
  }



  val server = ServerBuilder()
    .codec(HttpCodec())
    .bindTo(new InetSocketAddress(port))
    .name("server-" + id)
    .build(new Service[HttpRequest, HttpResponse] {
      override def apply(request : HttpRequest) : Future[HttpResponse] = {
        handle(request)
      }
    })
}
