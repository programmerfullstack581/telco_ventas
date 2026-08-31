# Etapa 1: compilación con Maven + JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cachea dependencias: si pom.xml no cambia, no se re-descargan
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# Etapa 2: imagen ligera solo con el JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S telco && adduser -S telco -G telco
USER telco

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
