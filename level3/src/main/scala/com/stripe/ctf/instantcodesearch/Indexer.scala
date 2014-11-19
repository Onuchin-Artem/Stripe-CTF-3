package com.stripe.ctf.instantcodesearch

import java.io._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

class Indexer(indexPath: String, ident: Int) {
  val root = FileSystems.getDefault().getPath(indexPath)
  val id = ident
  val idx = new Index(root.toAbsolutePath.toString)

  def index(): Indexer = {
    val children = root.toFile.list.sorted
    System.err.println("Total: " + children.length + " id " + id)
    try {
      (0 to children.length - 1).filter {
        i => (i % 4) == id
      }.map {
        i => children.apply(i)
      }.foreach {
        child =>
          System.err.println(id + " indexing: " + child);
          Files.walkFileTree(Paths.get(root.toString, child), new SimpleFileVisitor[Path] {
            override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
              if (Files.isHidden(dir) && dir.toString != ".")
                return FileVisitResult.SKIP_SUBTREE
              return FileVisitResult.CONTINUE
            }

            override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
              if (Files.isHidden(file))
                return FileVisitResult.CONTINUE
              if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                return FileVisitResult.CONTINUE
              if (Files.size(file) > (1 << 20))
                return FileVisitResult.CONTINUE
              try {

                  idx.addFile(root.relativize(file).toString, file)
              } catch {
                case e: IOException => {
                  return FileVisitResult.CONTINUE
                }
              }

              return FileVisitResult.CONTINUE
            }
          })
      }
    } catch {
      case t: Throwable => t.printStackTrace()
    }
    System.err.println("ID " + id + " submited all!!!! Tasks " + (100000 - submitedTasks.availablePermits))
    idx.waitIndexation()
    System.err.println("ID " + id + " I have index of size: " + idx.filesPerNeedle.keySet.size)
    idx.buildSecondaryIndex()
    System.err.println("ID " + id + " built index!")
    return this
  }
}
