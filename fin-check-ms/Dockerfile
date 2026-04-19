# ─────────────────────────────────────────────────────────────────────────────
# Stage 1: Build — Maven + JDK 17
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Baixa dependências antes de copiar o código-fonte (otimiza cache do Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2: Runtime — Imagem Playwright com Chromium embutido
# OBRIGATÓRIO: usar esta imagem — ela inclui Chromium, fontes e libs do sistema
# NÃO usar eclipse-temurin:17-jre-alpine pois faltarão libs do Chromium
# ─────────────────────────────────────────────────────────────────────────────
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Porta do microserviço coletor (diferente do backend principal na 8080)
EXPOSE 8081

# --no-sandbox já configurado no PlaywrightConfig — necessário em ambiente Docker
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
