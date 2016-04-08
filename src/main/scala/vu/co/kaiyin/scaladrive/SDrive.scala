package vu.co.kaiyin.scaladrive

import java.io.{File => JFile, _}
import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.util.{Calendar, Collections}

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.{FileContent, HttpTransport}
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.model.File
import com.google.api.services.drive.{Drive, DriveScopes}
import com.j256.simplemagic.ContentInfoUtil
import org.docopt.Docopt

class Dummy

object SDrive extends App {
  val doc =
    """Usage:
      |  sdrive [--desc] [--zipdir=<zd>] <file>...
      |
      |Options:
      |  --desc           To specify that the last file is a description for this upload.
      |  --zipdir=<zd>    Put files in zd and then zip it. Only applies when you are uploading at least 2 files.
    """.stripMargin
  private val APPLICATION_NAME: String = "sdrive"
  private val DATA_STORE_DIR: JFile = new JFile(System.getProperty("user.home"), ".credentials")
  private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance
  private val dataStoreFactory: FileDataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR)
  private val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport
  val credential: Credential = authorize
  private val drive: Drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build

  main1(args)

  def main1(_args: Array[String]): Unit = {
    val args = Docopt(doc, _args)
    val desc = args.getBoolean("--desc", default = false)
    val files = args.getStrings("<file>")
    val zd = args.getString("--zipdir")
    var descriptionFile: JFile = null
    var filesToUpload = files.map(x => new JFile(x))
    assume(filesToUpload.forall(_.exists()), "Some of files don't exist.")

    def containsFile(f1: JFile, f2: JFile): Boolean = {
      if(f1.isDirectory) {
        f1.listFiles().map(x => x.getAbsoluteFile).contains(f2.getAbsoluteFile)
      } else {
        false
      }
    }

    // if the description file is already contained in one of the folders to be uploaded,
    // then the description file itself needn't be in the uploading list.
    if (desc) {
      descriptionFile = filesToUpload.last
      val nonDescriptionFiles = filesToUpload.dropRight(1)
      if(nonDescriptionFiles.exists(containsFile(_, descriptionFile))) {
        filesToUpload = nonDescriptionFiles
      }
    }

    try {
      println("\nCreating a folder for today...")
      val folderId = createFolderForToday
      if (filesToUpload.length > 1) {
        uploadFiles(filesToUpload, zd.get, folderId, descriptionFile)
      } else {
        uploadFile(filesToUpload.head, parentId = folderId, description = descriptionFile)
      }
      println("\nSuccess!")
    }
    catch {
      case e: IOException =>
        println(e.getMessage)
      case t: Throwable =>
        t.printStackTrace()
    }
  }

  def createFolder(folderName: String, parentId: String = null): String = {
    create(folderName, "application/vnd.google-apps.folder", parentId)
  }

  def create(name: String, mimetype: String = "application/octet-stream", parentId: String = null, description: String = ""): String = {
    val fileMetadata: File = createMetaData(name, mimetype, parentId, description)
    val file = drive.files().create(fileMetadata)
      .setFields("id")
      .execute()
    file.getId
  }

  def createMetaData(name: String, mimetype: String = "application/octet-stream", parentId: String = null, description: String = ""): File = {
    val fileMetadata = new File()
    fileMetadata.setName(name).setMimeType(mimetype).setDescription(description)
    if (parentId != null) {
      fileMetadata.setParents(Collections.singletonList(parentId))
    }
    fileMetadata
  }

  @throws[Exception]
  private def authorize: Credential = {
    val clientSecrets: GoogleClientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(classOf[Dummy].getResourceAsStream("/client_secret.json")))
    if (clientSecrets.getDetails.getClientId.startsWith("Enter") || clientSecrets.getDetails.getClientSecret.startsWith("Enter ")) {
      println("\nEnter Client ID and Secret from https://code.google.com/apis/console/?api=drive " + "into drive-cmdline-sample/src/main/resources/client_secrets.json")
      System.exit(1)
    }
    val flow: GoogleAuthorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, Collections.singleton(DriveScopes.DRIVE)).setDataStoreFactory(dataStoreFactory).build
    new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver).authorize("user")
  }

  /**
    * Upload multiple files to google drive
    *
    * @param fileList    An array of filenames.
    * @param _zipDir     Move the above-mentioned files to this directory. The directory will be created if it does not exist.
    * @param parentId    Parent folder id in google drive. Uploads will be put there. Defaut to null, meaning that the root folder will be used.
    * @param description A description of this upload. Will be registered on google drive.
    */
  private def uploadFiles(fileList: Seq[JFile], _zipDir: String = null, parentId: String = null, description: JFile = null): Unit = {
    var zipDir: Path = null
    if (_zipDir == null) {
      zipDir = fileList.head.toPath
      zipDir = zipDir.resolveSibling(zipDir.getFileName + "_zipped_by_sdrive")
    } else {
      zipDir = Paths.get(_zipDir)
    }
    zipDir.toFile.mkdir()
    for (elem <- fileList) {
      FileUtil.copyToDir(elem, zipDir.toFile)
    }
    uploadFile(zipDir.toFile, parentId = parentId, description = description)
  }

  @throws[IOException]
  private def uploadFile(file: JFile, _mimeType: String = "application/octet-stream", parentId: String = null, description: JFile = null): File = {
    assume(file.exists(), s"${file.getName} doesn't exist!")
    val desc = new StringBuffer()
    if (description != null) {
      val source = scala.io.Source.fromFile(description)
      try desc.append(source.mkString) finally source.close()
      desc.append("\n\n")
    }
    var filePath = Paths.get(file.getAbsolutePath)
    // if file is dir, then zip it and upload the zipped file instead.
    if (file.isDirectory) {
      val tgzFile = ZipArchiveUtil.tgzFile(filePath)
      val fileList = file.listFiles().map(_.getName).mkString("\n", "\n", "\n")
      if (description != null) {
        val ofs = new FileOutputStream(description, true)
        try ofs.write(fileList.getBytes(Charset.forName("utf-8"))) finally ofs.close()
      }
      val streams = ZipArchiveUtil.tarStream(tgzFile)
      try {
        ZipArchiveUtil.addToTgz(streams._1, file)
        uploadFile(tgzFile, _mimeType, parentId, description)
      } finally {
        streams._1.finish()
        streams._1.close()
        streams._2.close()
        streams._3.close()
        streams._4.close()
      }
    } else {
      val mimeType = FileUtil.getMime(file)
      println(s"${file.getName}: ${mimeType}")
      val fileMetadata = createMetaData(file.getName, mimeType, parentId, desc.toString)
      val mediaContent: FileContent = new FileContent(mimeType, file)
      drive.files.create(fileMetadata, mediaContent).setFields("id, parents").execute()
    }
  }

  private def createFolderForToday: String = {
    val today = Calendar.getInstance()
    val folderName = s"${today.get(Calendar.YEAR)}-${today.get(Calendar.MONTH)}-${today.get(Calendar.DAY_OF_MONTH)}-sdrive"
    createFolder(folderName)
  }
}

