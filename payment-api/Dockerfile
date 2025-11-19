FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent pom and common module first for better caching
COPY pom.xml .
COPY common/pom.xml common/
COPY payment-api/pom.xml payment-api/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY common/src common/src
COPY payment-api/src payment-api/src

# Build the application
RUN mvn clean package -pl payment-api -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy the built artifact
COPY --from=build /app/payment-api/target/payment-api-*.jar app.jar

# Expose port
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
