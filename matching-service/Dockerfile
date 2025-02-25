FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/matching-service-0.0.1-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]
EXPOSE 8080
