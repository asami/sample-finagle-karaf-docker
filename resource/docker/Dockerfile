FROM asami/karaf-docker
MAINTAINER asami

COPY target/osgi/{{artifactId}}-{{artifactVersion}}.kar /opt/karaf/deploy/{{artifactId}}-{{artifactVersion}}.kar

EXPOSE 1099 8101 44444 8080
ENTRYPOINT ["/opt/karaf/bin/karaf"]
