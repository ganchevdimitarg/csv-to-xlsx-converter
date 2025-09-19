# Use an official OpenJDK runtime as a parent image
FROM openjdk:25-jdk-slim

# Set the working directory
WORKDIR /app

# Install required libraries for Java AWT / font support
RUN apt-get update && apt-get install -y \
    libfreetype6 \
    libfontconfig1 \
    libx11-6 \
    libxext6 \
    libxrender1 \
    && rm -rf /var/lib/apt/lists/*

# Copy Maven wrapper and pom.xml first (for dependency caching)
COPY mvnw .
COPY .mvn/ .mvn/
COPY pom.xml .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src ./src

# Build the Spring Boot application
RUN ./mvnw clean package -DskipTests

# Expose the port
EXPOSE 8089

# Run the application
CMD ["java", "-jar", "target/csv-to-xlsx-parser-0.0.1-SNAPSHOT.jar"]
