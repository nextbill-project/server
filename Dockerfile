FROM openjdk:8-jdk-alpine
ARG JAR_FILE=target/nextbill-server.jar
COPY ${JAR_FILE} nextbill-server.jar
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=docker", "/nextbill-server.jar"]