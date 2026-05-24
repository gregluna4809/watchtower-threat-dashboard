FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN groupadd --system watchtower \
    && useradd --system --gid watchtower --home-dir /app --shell /usr/sbin/nologin watchtower

COPY --from=builder --chown=watchtower:watchtower /workspace/target/watchtower-0.0.1-SNAPSHOT.jar /app/watchtower.jar

USER watchtower

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "/app/watchtower.jar"]
