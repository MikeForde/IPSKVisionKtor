# 1) pick a JDK 21 image
FROM eclipse-temurin:21-jre

RUN apt-get update && \
    apt-get purge -y sqlite3 libsqlite3-0 libsqlite3-dev && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*

# 2) tell Docker where the fat-jar will be
ARG JAR_FILE=build/libs/ips-kvision-ktor-1.0.0-SNAPSHOT.jar

# 3) copy it in
COPY ${JAR_FILE} /opt/app/app.jar

# 4) expose the Ktor port
EXPOSE 8080

# 5) run it
ENTRYPOINT ["java","-jar","/opt/app/app.jar"]
