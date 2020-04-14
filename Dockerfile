FROM openjdk:8-jdk-alpine
ARG DEPENDENCY=target/nextbill-server
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-Dspring.profiles.active=docker","-cp","app:app/lib/*","de.nextbill.Application"]