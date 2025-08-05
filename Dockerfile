# Use Java 21 base image (Temurin - lightweight and reliable)
FROM eclipse-temurin:21-jdk-alpine

# Expose the application port
EXPOSE 8080

# Set working directory
WORKDIR /app

# Copy the JAR file into the container
COPY target/DeliveryTeamDashboard-0.0.1-SNAPSHOT.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
