FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode dependency:go-offline

COPY src src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd --system workworth && useradd --system --gid workworth workworth

COPY --from=build /workspace/target/workworth-backend-*.jar app.jar

USER workworth

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
