FROM gradle:8.7-jdk21 AS builder
WORKDIR /recipe
COPY settings.gradle build.gradle ./
COPY src ./src
RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /recipe
COPY --from=builder /recipe/build/libs/*.jar recipe.jar
EXPOSE 8080
CMD ["java", "-jar", "recipe.jar"]
