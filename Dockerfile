FROM eclipse-temurin:21-jre

ARG JAR_FILE=target/resume-analyzer-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /app/resume-analyzer.jar
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/resume-analyzer.jar"]
