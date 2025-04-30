# 1) pick a JDK 17 image
FROM eclipse-temurin:17-jdk-jammy

# 2) tell Docker where the fat-jar will be
ARG JAR_FILE=build/libs/addressbook-fullstack-ktor-1.0.0-SNAPSHOT.jar

# 3) copy it in
COPY ${JAR_FILE} /opt/app/app.jar

# 4) expose the Ktor port
EXPOSE 8080

# 5) run it
ENTRYPOINT ["java","-jar","/opt/app/app.jar"]
