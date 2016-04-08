package vu.co.kaiyin.scaladrive

import java.io.File

import com.j256.simplemagic.ContentInfoUtil
import org.apache.commons.io.FileUtils

/**
  * Created by IDEA on 4/8/16.
  */
object FileUtil {
  val infoUtil = new ContentInfoUtil()
  def copyToDir(src: File, dest: File): Unit = {
    if(src.isFile) {
      FileUtils.copyFileToDirectory(src, dest)
    } else {
      FileUtils.copyDirectoryToDirectory(src, dest)
    }
  }
  def getMime(f: File): String = {
    try {
      val i = infoUtil.findMatch(f).getMimeType
    } catch {
      case e: NullPointerException =>
    }
    try {
      infoUtil.findMatch(f).getMimeType
    } catch {
      case e: NullPointerException => "application/octet-stream"
    }
  }
}
