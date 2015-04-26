lazy val sbtOsgi = uri("git://github.com/sbt/sbt-osgi.git")
//uri("git://github.com/arashi01/sbt-osgi.git")

lazy val plugins = (project in file("."))
  .dependsOn(sbtOsgi)

