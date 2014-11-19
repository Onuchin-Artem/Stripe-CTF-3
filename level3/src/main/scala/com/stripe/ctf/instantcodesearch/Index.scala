package com.stripe.ctf.instantcodesearch

import java.io._
import scala.io.Source
import com.twitter.util.FuturePool
import scala.collection.JavaConversions._
import scala.math
import scala.collection.mutable._
import com.google.common.base.Charsets
import java.util.Collections
import java.nio.file.Path

class Index(repoPath: String) extends Serializable {
  var filesPerNeedle = new HashMap[String, Set[Match]] with MultiMap[String, Match]

  var NeedlePerQuery = new HashMap[String, Set[Match]] with MultiMap[String, Match]

  def path() = repoPath

  def addFile(file: String, fullPath: Path) {
    submitedTasks.acquire()
    pool {
      var lineNum = 0;
      val src = Source.fromFile(fullPath.toFile)
      try {
        for (line <- src.getLines()) {
          lineNum = lineNum + 1
          for (term <- notWord.split(line)) {
            this.synchronized {
              filesPerNeedle.addBinding(term.toString, new Match(file, lineNum))
            }
          }
        }
        val tasks = 100000 - submitedTasks.availablePermits;
        if (tasks % 5 == 0) {
          System.err.println("ID: " + System.identityHashCode(this) + " tasks: " + tasks)
        }
      } catch {
        case t: Throwable => t.printStackTrace();
      } finally {
        submitedTasks.release()
        src.close()
      }
    }
  }

  def waitIndexation() {
    submitedTasks.acquire(100000)
    submitedTasks.release(100000)
  }

  def inc(x: String) = {
    assert(x.length > 0)
    val last = x.length - 1
    (x take last) + (x(last) + 1).asInstanceOf[Char]
  }

  def buildSecondaryIndex() {
    if (!filesPerNeedle.keySet.isEmpty) {
      for (word : String <- filesPerNeedle.keySet) {
        0 to word.length - 1 foreach {
          i =>
            i + 1 to word.length foreach {
              j =>
                val term = word.substring(i, j)
                new String()
                this.synchronized {
                  if (dict().contains(term)) {
                    for (m <- filesPerNeedle.get(word).getOrElse(Set.empty)) {
                      NeedlePerQuery.addBinding(term, m)
                    }
                  }
                }
            }
        }
      }
    }
  }

}

