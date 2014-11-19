package com.stripe.ctf.instantcodesearch

import java.io._
import java.nio.file._

import com.twitter.concurrent.Broker

abstract class SearchResult

 case class Match(path: String, line: Int) extends SearchResult {
  override def canEqual(that: Any): Boolean = {
    return that.getClass() == this.getClass()
  }

  override def hashCode(): Int = {
    return (path.hashCode() + 13) * 17 + line.hashCode()
  }

  override def equals(obj: scala.Any): Boolean = {
    if (obj.getClass() != this.getClass()) {
      return false;
    }
    val that = obj.asInstanceOf[Match];
    return path == that.path && line == that.line
  }
}
case class Done() extends SearchResult

class Searcher(indexPath : String, idx : Index)  {
  val index : Index = idx
  val root = FileSystems.getDefault().getPath(index.path)

  def search(needle : String, b : Broker[SearchResult]) = {
      index.NeedlePerQuery.get(needle).getOrElse(Set.empty).foreach { m : Match =>
        b !! m
    }
    b !! new Done()
  }
}
