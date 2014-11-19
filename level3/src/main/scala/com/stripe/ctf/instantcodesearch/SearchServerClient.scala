package com.stripe.ctf.instantcodesearch

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Http => HttpCodec}
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.server.TwitterServer
import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.QueryStringEncoder
import scala.util.parsing.json.JSON

class SearchServerClient(port : Int, id: Int) {
  def client: Service[HttpRequest, HttpResponse] = ClientBuilder()
    .codec(HttpCodec())
    .hosts(new InetSocketAddress(port))
    .hostConnectionLimit(1)
    .build()

  def queryPath(path: String, key: String, value: String): String = {
    val encoder = new QueryStringEncoder(path)
    encoder.addParam(key, value)
    encoder.toString
  }

  def queryPath(path: String, key: String, value: String, key2: String, value2 : String): String = {
    val encoder = new QueryStringEncoder(path)
    encoder.addParam(key, value)
    encoder.addParam(key2, value2)
    encoder.toString
  }

  def healthcheck() = {
    executeRequest("/healthcheck", HttpMethod.GET)
  }

  def isIndexed() = {
    executeRequest("/isIndexed", HttpMethod.GET)
  }
  def query(q: String) = {
    executeRequest(queryPath("/", "q", q), HttpMethod.GET)
  }

  def index(path : String, id : Int) = {
    executeRequest(queryPath("/index-inter", "path", path, "id", id.toString), HttpMethod.GET)
  }

  def executeRequest(path: String, method : HttpMethod) = {
    val request = new DefaultHttpRequest(
      HttpVersion.HTTP_1_1, method, path)

    client(request)
  }
}
