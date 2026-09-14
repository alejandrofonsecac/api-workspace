FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/api-0.0.1-SNAPSHOT.jar app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=production
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
