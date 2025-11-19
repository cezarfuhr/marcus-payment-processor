FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent pom and common module first for better caching
COPY pom.xml .
COPY common/pom.xml common/
COPY reconciliation-service/pom.xml reconciliation-service/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY common/src common/src
COPY reconciliation-service/src reconciliation-service/src

# Build the application
RUN mvn clean package -pl reconciliation-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy the built artifact
COPY --from=build /app/reconciliation-service/target/reconciliation-service-*.jar app.jar

# Expose port
EXPOSE 8084

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
