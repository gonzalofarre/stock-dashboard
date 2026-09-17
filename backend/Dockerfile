# Multi-stage build: compile with the JDK, run on the slimmer JRE.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache dependencies separately from source so a source-only change doesn't
# re-download the whole Maven repo.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Railway (and most PaaS providers) inject PORT dynamically; server.port in
# application.yml reads it via ${PORT:8080}, so this just documents the
# default for local `docker run`.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
