package vu.co.kaiyin.scaladrive

import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Paths}
import java.util.zip.ZipOutputStream

import com.j256.simplemagic.ContentInfoUtil

/**
  * Created by IDEA on 4/8/16.
  */
object Test extends App {

  import ZipArchiveUtil._

  val infoUtil = new ContentInfoUtil()
//  println(infoUtil.findMatch("/tmp/test.zip").getMimeType)
  println(infoUtil.findMatch("/tmp/test.tar.gz").getMimeType)
  println({
    val i = infoUtil.findMatch("/tmp/test.tar.gz")
    if (i != null) i.getMimeType else "Unknown"
  })
  //  val streams = tarStream(tgzFile("/tmp/test"))
  //  try addToTgz(streams._1, new File("/tmp/test")) finally {
  //    streams._1.finish()
  //    streams._1.close()
  //    streams._2.close()
  //    streams._3.close()
  //    streams._4.close()
  //  }
}
