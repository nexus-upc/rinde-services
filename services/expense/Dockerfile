FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY --chmod=755 mvnw ./mvnw
COPY libraries/shared-kernel/pom.xml libraries/shared-kernel/pom.xml
COPY libraries/shared-kernel/src/ libraries/shared-kernel/src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -f libraries/shared-kernel/pom.xml install -DskipTests
COPY services/expense/pom.xml services/expense/pom.xml
COPY services/expense/src/ services/expense/src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -f services/expense/pom.xml package -DskipTests \
    && cp services/expense/target/rinde-expense-service-*.jar /tmp/application.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
COPY --from=build --chown=10001:10001 /tmp/application.jar ./application.jar
# Carpeta de comprobantes; compose la monta como volumen con el mismo propietario.
ENV RINDE_EVIDENCE_STORAGE_PATH=/var/lib/rinde/evidences
RUN mkdir -p /var/lib/rinde/evidences && chown 10001:10001 /var/lib/rinde/evidences
USER 10001:10001
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
