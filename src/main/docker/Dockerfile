# Alpine Linux with OpenJDK JRE
FROM openjdk:16-jdk-alpine3.12
# copy JAR into image
COPY ./target/properties-webapp-1.0-SNAPSHOT.jar /app.jar
# run application with this command line
RUN apk update && apk add bash

COPY ./src/main/docker/start.sh /
RUN chgrp 0 /start.sh && \
    chmod 0775 /start.sh && \
    cat /etc/*release*

ENTRYPOINT ["/start.sh"]

LABEL maintainer="Vincent Russell <vincent.russell@gmail.com>"