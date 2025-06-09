##########################
# 1️⃣ Build stage
##########################
FROM gradle:8.7-jdk21 AS builder

WORKDIR /app

COPY settings.gradle build.gradle ./

COPY src ./src

RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /src/build/libs/keycloak.jar /app/keycloak.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "gateway.jar"]
