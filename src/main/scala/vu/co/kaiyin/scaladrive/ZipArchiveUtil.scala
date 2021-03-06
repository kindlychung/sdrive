package vu.co.kaiyin.scaladrive

import java.io.{File, _}
import java.nio.file.{Path, Paths}
import java.util.zip.{ZipEntry, ZipOutputStream}

import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.utils.IOUtils


object ZipArchiveUtil {

  def main(args: Array[String]) {
    val zipFile = "C:/archive.zip"
    val srcDir = "C:/srcdir"
    try {
      val fos = new FileOutputStream(zipFile)
      val zos = new ZipOutputStream(fos)
      val srcFile = new File(srcDir)
      addOneEntry(zos, srcFile)
      zos.close()
    } catch {
      case ioe: IOException => println("Error creating zip file: " + ioe)
    }
  }

  def addOneEntry(zos: ZipOutputStream, srcFile: File): Unit = {
    if (srcFile.isFile) {
      addFile(zos, srcFile)
    } else {
      val files = srcFile.listFiles()
      for (i <- 0 until files.length) {
        addOneEntry(zos, files(i))
      }
    }
  }

  def addFile(zos: ZipOutputStream, file: File): Unit = {
    try {
      val buffer = Array.ofDim[Byte](1024)
      val fis = new FileInputStream(file)
      zos.putNextEntry(new ZipEntry(file.getName))
      var length: Int = 0
      while ( {
        length = fis.read(buffer)
        length > 0
      }) {
        zos.write(buffer, 0, length)
      }
      zos.closeEntry()
      fis.close()
    } catch {
      case ioe: IOException => println("IOException :" + ioe)
    }
  }

  def addToTgz(tgzOs: TarArchiveOutputStream, file: File, base: String = ""): Unit = {
    val entryName = base + file.getName
    val tarEntry = new TarArchiveEntry(file, entryName)
    tgzOs.putArchiveEntry(tarEntry)
    if (file.isFile) {
      println(s"Gzipping: ${file.getName}")
      IOUtils.copy(new FileInputStream(file), tgzOs)
      tgzOs.closeArchiveEntry()
    } else {
      tgzOs.closeArchiveEntry()
      val children = file.listFiles()
      if (children != null) {
        for (child <- children) {
          addToTgz(tgzOs, child, entryName + "/")
        }
      }
    }
  }

  def tarStream(tgzFile: File): (TarArchiveOutputStream, GzipCompressorOutputStream, BufferedOutputStream, FileOutputStream) = {
    val f = new FileOutputStream(tgzFile)
    val b = new BufferedOutputStream(f)
    val g = new GzipCompressorOutputStream(b)
    val t = new TarArchiveOutputStream(g)
    (t, g, b, f)
  }

  def zipFile(filePath: Path): File = {
    filePath.resolveSibling(filePath.getFileName + ".zip").toFile
  }

  def tgzFile(filePath: Path): File = {
    filePath.resolveSibling(filePath.getFileName + ".tar.gz").toFile
  }

  def tgzFile(src: String): File = {
    tgzFile(Paths.get(src))
  }

  def zipFile(src: String): File = {
    zipFile(Paths.get(src))
  }
}