FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/chessgame-1.0-SNAPSHOT.jar app.jar
CMD ["java", "-cp", "app.jar", "com.chessgame.server.matchmaker.MatchmakerMain"]