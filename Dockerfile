# Build & run ISO-prod (Spring Boot 3.3, JDK aligné sur pom: 21)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

RUN apk add --no-cache bash

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENV APP_UPLOAD_DIR=/app/uploads
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
