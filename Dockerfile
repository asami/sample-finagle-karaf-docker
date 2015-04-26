FROM asami/karaf-docker
MAINTAINER asami

COPY deploy/sample-0.1.0.SNAPSHOT.kar /opt/karaf/deploy/sample-0.1.0.SNAPSHOT.kar

EXPOSE 1099 8101 44444 8080
ENTRYPOINT ["/opt/karaf/bin/karaf"]
