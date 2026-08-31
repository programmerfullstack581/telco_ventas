# Etapa 1: compilacion con Maven + JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY backend/pom.xml .
RUN mvn -B dependency:go-offline

COPY backend/src ./src
RUN mvn -B package -DskipTests

# Etapa 2: imagen ligera JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S telco && adduser -S telco -G telco
USER telco

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Xmx384m", "-Dspring.profiles.active=prod", "-jar", "app.jar"]