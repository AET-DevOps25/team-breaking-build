FROM openjdk:21-jdk-slim-bullseye AS build
WORKDIR /src
COPY . .
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /src/build/libs/keycloak.jar /app/keycloak.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "keycloak.jar"]
