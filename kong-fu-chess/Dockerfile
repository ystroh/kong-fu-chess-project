FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-slim
WORKDIR /app
COPY --from=build /app/target/chessgame-1.0-SNAPSHOT.jar app.jar
EXPOSE 8887
CMD ["java", "-jar", "app.jar"]