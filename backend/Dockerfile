# --- Build stage: compile and package the Spring Boot jar ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Resolve dependencies first so they are cached between source changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -B package -DskipTests && cp target/*.jar app.jar

# --- Runtime stage: JRE only, non-root user ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app && mkdir -p /app/logs && chown app:app /app/logs
COPY --from=build /build/app.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
