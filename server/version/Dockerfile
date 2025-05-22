##########################
# 1️⃣ Build stage
##########################
FROM gradle:8.7-jdk21 AS builder

WORKDIR /app

# Copy Gradle wrapper and build scripts first for layer caching
COPY settings.gradle build.gradle ./

# Copy the actual source code
COPY src ./src

# Build the Spring Boot fat jar (bootJar)
RUN gradle --no-daemon clean bootJar

##########################
# 2️⃣ Runtime stage
##########################
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy only the boot jar (there will be exactly one in build/libs)
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "app.jar"]
