// sbt clean karaf
// sbt docker
def versionNumber = "0.1-SNAPSHOT"

def groupId = "com.example"
def artifactId = "sample"
def artifactVersion = "0.1.0.SNAPSHOT"

def projectDescription = "Sample project using Finagle, OSGi(Karaf) and Docker for microservices"
def authorName = "example"
def sbtProjectName = "sample"

// OSGi Settings
def osgiExportPackage = "sample.api"
def osgiPrivatePackage = "sample.osgi"
def osgiImportPackage = Seq(
  """org.osgi.framework;version="[1.6,2)"""",
  "sun.misc;resolution:=optional",
  "org.osgi.service.blueprint;version=\"[1.0.0,2.0.0)\""
)
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

def projectFileName = s"$artifactId-osgi-$versionNumber"

scalaVersion := scalaVersionString

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
  val _ = assembly.value
  val assemblyFile = (assemblyJarName in assembly).value

  val zipFile = new File("target/osgi/" + karFileName)
  IO.delete(zipFile)

  val distdir = new File("target/osgi/karaf")
  IO.delete(distdir)

  IO.createDirectory(new File("target/osgi/karaf/META-INF"))

  val lines = Seq[String](
    "Manifest-Version: 1.0",
    "Archiver-Version: Plexus Archiver",
    "Created-By: Apache Maven",
    "Built-By: " + authorName,
    "Build-Jdk: " + jdkVersion)
  IO.writeLines(new File("target/osgi/karaf/META-INF/MANIFEST.MF"), lines)

  IO.createDirectory(new File("target/osgi/karaf/META-INF/maven/" + groupId + "/" + mavenArtifactId))

  // place pom.properties and pom.xml here
  copyFileWithTemplate(new File("resource/maven/pom.properties"), 
      new File("target/osgi/karaf/META-INF/maven/" + groupId + "/" + mavenArtifactId + "/pom.properties"))
  copyFileWithTemplate(new File("resource/maven/pom.xml"), 
      new File("target/osgi/karaf/META-INF/maven/" + groupId + "/" + mavenArtifactId + "/pom.xml"))

  IO.createDirectory(new File("target/osgi/karaf/repository/" + groupIdPath() + "/" + mavenArtifactId + 
      "/" + artifactVersion))
  
  IO.copyFile(new File("target/scala-" + majorVersion() + "/" + assemblyFile), 
              new File("target/osgi/karaf/repository/" + groupIdPath() + "/" + mavenArtifactId + 
                "/" + artifactVersion + "/" + artifactId + ".kar-" + 
                artifactVersion + ".jar"))

  val featureDestFile = new File("target/osgi/karaf/repository/" + groupIdPath() + "/" + mavenArtifactId + 
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

val dockerTask = TaskKey[Unit]("docker", "Create Dockerfile")

dockerTask := {
  val target = new File("target/docker/Dockerfile")
  copyFileWithTemplate(new File("resource/docker/Dockerfile"), target)
}
