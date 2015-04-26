Sample project using Finagle, OSGi(Karaf) and Docker for microservices
======================================================================

https://github.com/PhilAndrew/Scala-Karaf

Build KAR
---------

```sbt clean karaf
```

Build Dockerfile
----------------

```sbt docker
```

Build Docker Image
------------------

```docker build -f target/docker/Dockerfile -t asami/finagle-karaf-docker .
```

Run foreground
--------------

```docker run -t --rm -p 1099:1099 -p 8101:8101 -p 44444:44444 -p 8080:8080 asami/finagle-karaf-docker
```

Access the Finagle server on Karaf in Docker
--------------------------------------------

```curl http://192.168.59.104:8080 -v
```
