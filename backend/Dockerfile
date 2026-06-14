FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/waimai-backend-2.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
