package com.stripe.ctf

import java.io._
import java.nio.file._
import java.nio.charset._
import scala.collection.{mutable, Map, Seq, Set}
import java.util
import org.arabidopsis.ahocorasick.AhoCorasick
import com.google.gson.Gson
import org.jboss.netty.handler.codec.http.{HttpVersion, DefaultHttpResponse, HttpResponse, HttpResponseStatus}
import org.jboss.netty.buffer.ChannelBuffers._
import org.jboss.netty.util.CharsetUtil._
import scala.collection.mutable.StringBuilder
import java.util.concurrent.{Executors, Semaphore}
import com.twitter.util.FuturePool
import java.nio.ByteBuffer
import scala.io.Source
import scala.collection.immutable.HashMap.HashTrieMap

package object instantcodesearch {
  val utf8Decoder = Charset.forName("UTF-8").newDecoder().
    onMalformedInput(CodingErrorAction.REPORT).
    onUnmappableCharacter(CodingErrorAction.REPORT)

  def slurp(r: Reader): String = {
    val sb = new StringBuilder
    val buf = new Array[Char](4096)
    var n = 0
    while (n != -1) {
      n = r.read(buf)
      if (n > 0)
        sb.appendAll(buf, 0, n)
    }
    return sb.toString
  }

  def slurp(p: Path): String = {
    val r = new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(p)), utf8Decoder)
    return slurp(r)
  }

  @volatile var gson = new Gson()
  var dictSet:Set[String] = null

  def dict(): Set[String] = {
    if (dictSet == null) {
      this.synchronized {
        if (dictSet == null) {
        System.err.println("Loading dict")
        try {
          val dictSetInst = Source.fromInputStream(getClass.getResourceAsStream("/words"))
            .getLines().map(s => (s.trim)).toSet
          dictSet = dictSetInst
        System.err.println("Loaded dict")
      }
    }
  }}
  return dictSet;
  }
  lazy val submitedTasks = new Semaphore(100000);

  lazy val notWord = "[^a-zA-Z]+".r

  lazy val pool = FuturePool.apply(Executors.newFixedThreadPool(2))
def httpResponse (message: String, code: HttpResponseStatus): HttpResponse = {
val response = new DefaultHttpResponse (HttpVersion.HTTP_1_1, code)
response.setContent (copiedBuffer (message, UTF_8) )

response
}

def successResponse (): HttpResponse = {
val content = "{\"success\": true}"
httpResponse (content, HttpResponseStatus.OK)
}

def querySuccessResponse (results: Seq[Match] ): HttpResponse = {
val response = new DefaultHttpResponse (HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
val resultString = results
.map {
r => "\"" + r.path + ":" + r.line + "\""
}
.mkString ("[", ",\n", "]")
val content = "{\"success\": true,\n \"results\": " + resultString + "}"
response.setContent (copiedBuffer (content, UTF_8) )

response
}


def querySuccessResponseInter (results: List[Match] ): HttpResponse = {
val response = new DefaultHttpResponse (HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
val bytes = new ByteArrayOutputStream (64)
new ObjectOutputStream (bytes).writeObject (results)
response.setContent (copiedBuffer (bytes.toByteArray) )

response
}

def readResponseInter (response: HttpResponse): List[Match] = {
val content = response.getContent
var ar = new Array[Byte] (content.capacity () )
content.getBytes (0, ar)
new ObjectInputStream (new ByteArrayInputStream (ar) ).readObject ().asInstanceOf[List[Match]]
}

def errorResponse (code: HttpResponseStatus, message: String) = {
val content = "{\"success\": false, \"error\": \"" + message + "\"}"
httpResponse (content, code)
}

def getParam (params: Map[String, mutable.Buffer[String]], key: String): String = {
params.get (key)
.flatMap {
_.headOption
}
.getOrElse {
throw new BadRequestException ("Parameter '" + key + "' not specified")
}
}

def readIndex (path: String): Index = {
new ObjectInputStream (
new FileInputStream (new File (path) )
).readObject.asInstanceOf[Index]
}
}
