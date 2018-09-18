FROM openjdk:8-jre-alpine

ADD target/scala-2.12/scalazF-assembly-1.0-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]