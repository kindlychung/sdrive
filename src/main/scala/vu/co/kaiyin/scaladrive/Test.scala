package vu.co.kaiyin.scaladrive

import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Paths}
import java.util.zip.ZipOutputStream


/**
  * Created by IDEA on 4/8/16.
  */
object Test extends App {
  println(FileUtil.tree(new StringBuffer(), Paths.get("/tmp")))
}
