//
// sbt clean assembly deploy karaf
//

def versionNumber = "0.1-SNAPSHOT"

def groupId = "com.example"
def artifactId = "sample"
def artifactVersion = "0.1.0.SNAPSHOT"

def projectDescription = "Scala Osgi(Karaf) with Sbt for Finagle Service"
def authorName = "author"
def sbtProjectName = "Sample"

// OSGi Settings
def osgiExportPackage = "sample.api"
def osgiPrivatePackage = "sample.osgi"
def osgiImportPackage = Seq(
  """org.osgi.framework;version="[1.6,2)"""",
  "sun.misc;resolution:=optional",
  "org.osgi.service.blueprint;version=\"[1.0.0,2.0.0)\""
)
// def osgiImportPackage = Seq(
//   "sun.misc;resolution:=optional",
//   "org.osgi.service.blueprint;version=\"[1.0.0,2.0.0)\"",
//   "!aQute.bnd.annotation.*",
//   "*"
// )
// def osgiImportPackage = Seq(
//   "com.twitter.finagle",
//   "sample",
//   """org.osgi.framework;version="[1.6,2)"""",
//   """scala;version="[2.10,3)"""",
//   """scala.reflect;version="[2.10,3)"""",
//   """scala.runtime;version="[2.10,3)"""",
//   "sun.misc;resolution:=optional",
//   "org.osgi.service.blueprint;version=\"[1.0.0,2.0.0)\"",
//   "!aQute.bnd.annotation.*"
// )
// def osgiAdditionalHeaders = Map(
//   "Service-Component" -> "*",
//   "Conditional-Package" -> "scala.*"
// )
def osgiAdditionalHeaders = Map.empty[String, String]
def bundleActivator = "sample.osgi.Activator"
def jdkVersion = "1.7.0_75"
def scalaVersionString = "2.10.5"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  // OSGi
  "org.osgi" % "org.osgi.core" % "4.3.0" % "provided" // try 6.0.0 ?
  // See https://github.com/PhilAndrew/Scala-Karaf/blob/master/src/main/scala/demo/run/Run.scala
  // "org.apache.felix" % "org.apache.felix.framework" % "4.6.0" % "runtime"
)

libraryDependencies += "com.twitter" %% "finagle-http" % "6.25.0"

def mavenArtifactId = artifactId + ".kar"
def groupIdPath() = groupId.replace('.','/')
def artifactFileName = artifactId + "-" + artifactVersion
def karFileName = artifactFileName + ".kar"
def majorVersion() = scalaVersionString.split('.').dropRight(1).mkString(".")

// def projectFileName = artifactId + "_" + majorVersion() + "-" + versionNumber
def projectFileName = s"$artifactId-osgi-$versionNumber"

scalaVersion := scalaVersionString

//lazy val fooProject = (project in file(".")) // Obtain the root project reference
//  .enablePlugins(SbtOsgi)  // Enables sbt-osgi for this project. This will automatically append
                           // the plugin's default settings to this project thus providing the
                           // `osgiBundle` task.

// This is where we define the OSGi information, change the following to fit your OSGi needs
// outputs target/scala-2.11/scalaosgiwithsbt_2.11-1.0.jar

name := sbtProjectName

version := versionNumber

organization := groupId

unmanagedBase <<= baseDirectory ( base => base / "libs" )

assemblyJarName in assembly := s"$projectFileName.jar"

// OSGI

packageOptions in (Compile, packageBin) ++= Seq(
  Package.ManifestAttributes("Bundle-Activator" -> bundleActivator),
  Package.ManifestAttributes("Bundle-Description" -> projectDescription),
  Package.ManifestAttributes("Bundle-ManifestVersion" -> "2"),
  Package.ManifestAttributes("Bundle-Name" -> sbtProjectName),
  Package.ManifestAttributes("Bundle-SymbolicName" -> s"$groupId.$artifactId"),
  Package.ManifestAttributes("Bundle-Vendor" -> groupId),
  Package.ManifestAttributes("Bundle-Version" -> artifactVersion),
  Package.ManifestAttributes("Export-Package" -> osgiExportPackage),
  Package.ManifestAttributes("Import-Package" -> osgiImportPackage.mkString(",")),
  Package.ManifestAttributes("DynamicImport-Package" -> "*"), // not moduler, avoid javax.management problem
  Package.ManifestAttributes("Private-Package" -> osgiPrivatePackage),
  Package.ManifestAttributes("Require-Capability" -> """osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.7))"""")
)

// OsgiKeys.exportPackage := Seq(osgiExportPackage)

// OsgiKeys.privatePackage := Seq(osgiPrivatePackage)

// OsgiKeys.importPackage := osgiImportPackage

// OsgiKeys.bundleActivator := Option(bundleActivator)

// OsgiKeys.additionalHeaders := osgiAdditionalHeaders

// osgiSettings

def entriesInDir(f: File):List[File] = f :: (if (f.isDirectory) IO.listFiles(f).toList.flatMap(entriesInDir(_)) else Nil)

def mapForZip(x: List[File], here: String) = x.map(d => (d, {
    if (here.length == d.getAbsolutePath.length) "" 
      else d.getAbsolutePath.substring(here.length+1)
  }
  ))

def copyFileWithTemplate(in: File, out: File) = {
  val featureLines = IO.readLines(in)
  val featureLinesReplaced = featureLines.map( (line: String) => {
    line.replaceAll("\\{\\{groupId}}", groupId)
        .replaceAll("\\{\\{artifactId}}", artifactId)
        .replaceAll("\\{\\{artifactVersion}}", artifactVersion)
        .replaceAll("\\{\\{description}}", projectDescription)
  })
  IO.writeLines(out, featureLinesReplaced)
}

val karafTask = TaskKey[Unit]("karaf", "Create Karaf kar file")

karafTask := {
  val zipFile = new File("deploy/" + karFileName)
  IO.delete(zipFile)

  val distdir = new File("deploy/karaf")
  IO.delete(distdir)

  IO.createDirectory(new File("deploy/karaf/META-INF"))

  val lines = Seq[String](
    "Manifest-Version: 1.0",
    "Archiver-Version: Plexus Archiver",
    "Created-By: Apache Maven",
    "Built-By: " + authorName,
    "Build-Jdk: " + jdkVersion)
  IO.writeLines(new File("deploy/karaf/META-INF/MANIFEST.MF"), lines)

  IO.createDirectory(new File("deploy/karaf/META-INF/maven/" + groupId + "/" + mavenArtifactId))

  // place pom.properties and pom.xml here
  copyFileWithTemplate(new File("resource/maven/pom.properties"), 
      new File("deploy/karaf/META-INF/maven/" + groupId + "/" + mavenArtifactId + "/pom.properties"))
  copyFileWithTemplate(new File("resource/maven/pom.xml"), 
      new File("deploy/karaf/META-INF/maven/" + groupId + "/" + mavenArtifactId + "/pom.xml"))

  IO.createDirectory(new File("deploy/karaf/repository/" + groupIdPath() + "/" + mavenArtifactId + 
      "/" + artifactVersion))
  
  IO.copyFile(new File("target/scala-" + majorVersion() + "/" + projectFileName + ".jar"), 
              new File("deploy/karaf/repository/" + groupIdPath() + "/" + mavenArtifactId + 
                "/" + artifactVersion + "/" + artifactId + ".kar-" + 
                artifactVersion + ".jar"))

  val featureDestFile = new File("deploy/karaf/repository/" + groupIdPath() + "/" + mavenArtifactId + 
                "/" + artifactVersion + "/" + artifactId + ".kar-" +
                artifactVersion + "-features.xml")
  copyFileWithTemplate(new File("resource/karaf/features.xml"), featureDestFile)

  // Create zip file
  val here = distdir.getAbsolutePath.toString
  val mapped = mapForZip(entriesInDir(distdir), here)
  IO.delete(zipFile)
  IO.zip(mapped, zipFile)
  //IO.delete(distdir)
}

val deployTask = TaskKey[Unit]("deploy", "Place the jar into a destination directory")

deployTask := {
  IO.copyFile(new File("target/scala-" + majorVersion() + "/" + projectFileName + ".jar"), 
              new File("deploy/" + projectFileName + ".jar"))
}
