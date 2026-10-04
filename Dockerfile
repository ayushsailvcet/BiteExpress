# Dockerfile for BiteExpress Food Ordering System
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean compile
RUN mkdir -p target/classes/static && cp -r src/main/resources/static/* target/classes/static/

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/classes /app/classes
EXPOSE 8080
ENV PORT=8080
CMD ["java", "-cp", "classes", "com.foodapp.FoodOrderingApplication"]
