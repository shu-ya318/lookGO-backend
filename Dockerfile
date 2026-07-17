# ===== Stage 1: Build the app =====
FROM registry.access.redhat.com/ubi8/openjdk-17:1.20-2 AS builder

WORKDIR /home/jboss

ARG SETTINGS_FILE
COPY ${SETTINGS_FILE} /home/jboss/.m2/settings.xml

# 設定檔案的擁有者與群組(root) # 目的地路徑 (. 為當前目錄)
COPY --chown=185:0 pom.xml .

# 因應內網下載套件，跳過 SSL 
ENV MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"

# 下載依賴套件、插件到本地.m2 repository
RUN mvn dependency:go-offline -s /home/jboss/.m2/settings.xml

COPY --chown=185:0 src/ ./src/

RUN mvn clean package -DskipTests -s /home/jboss/.m2/settings.xml

# ===== Stage 2: Final runtime image =====
FROM registry.access.redhat.com/ubi8/openjdk-17-runtime:1.20-2

COPY --from=builder --chown=185:0 /home/jboss/target/lookGo-backend.jar ./lookGo-backend.jar

EXPOSE 8080

CMD ["java", "-jar", "lookGo-backend.jar"]
