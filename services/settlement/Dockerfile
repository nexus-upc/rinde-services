FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY --chmod=755 mvnw ./mvnw
COPY libraries/shared-kernel/pom.xml libraries/shared-kernel/pom.xml
COPY libraries/shared-kernel/src/ libraries/shared-kernel/src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -f libraries/shared-kernel/pom.xml install -DskipTests
COPY services/settlement/pom.xml services/settlement/pom.xml
COPY services/settlement/src/ services/settlement/src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -f services/settlement/pom.xml package -DskipTests \
    && cp services/settlement/target/rinde-settlement-service-*.jar /tmp/application.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
COPY --from=build --chown=10001:10001 /tmp/application.jar ./application.jar
USER 10001:10001
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
