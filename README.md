https://github.com/PhilAndrew/Scala-Karaf

sbt clean test assembly deploy karaf

cp deploy/sample-0.1-SNAPSHOT.kar ~/lib/apache-karaf-4.0.0.M1/deploy

Build Docker Image
------------------

```docker build -t asami/finagle-karaf-docker .
```

Run foreground
--------------

```docker run -t --rm -p 1099:1099 -p 8101:8101 -p 44444:44444 -p 8080:8080 asami/finagle-karaf-docker
```

Access the Finagle server on Karaf in Docker
--------------------------------------------

```curl http://192.168.59.104:8080 -v
```


shell.init.script
