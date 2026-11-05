FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -Dmaven.test.skip=true package && \
    cp target/*.jar app.jar


FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S -g 10001 radar && \
    adduser -S -u 10001 -G radar -H -s /sbin/nologin radar

WORKDIR /app

COPY --from=build --chown=root:root --chmod=444 /build/app.jar app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]