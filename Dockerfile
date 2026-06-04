# ===== Stage 1: Build the app =====
FROM registry.access.redhat.com/ubi8/openjdk-17:1.20-2 AS builder
# FROM harbor.mli.com.corp/rhscl/ubi8/openjdk-17:1.20-2 AS builder

WORKDIR /home/jboss

COPY --chown=185:0 pom.xml .

RUN mvn dependency:go-offline

COPY --chown=185:0 src/ ./src/

RUN mvn clean package -DskipTests

# ===== Stage 2: Final runtime image =====
FROM registry.access.redhat.com/ubi8/openjdk-17-runtime:1.20-2

COPY --from=builder --chown=185:0 /home/jboss/target/lookGo-backend.jar ./lookGo-backend.jar

EXPOSE 8080

CMD ["java", "-jar", "lookGo-backend.jar"]
