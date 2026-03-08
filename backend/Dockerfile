# ─────────────────────────────────────────────
# Stage 1 : Build
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Gradle Wrapper 먼저 복사 → 의존성 레이어 캐싱
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# 빌드 스크립트만 먼저 복사 (소스 변경 시에도 의존성 재다운로드 방지)
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon 2>/dev/null || true

# 소스 전체 복사 & 빌드
COPY src src
RUN ./gradlew build -x test --no-daemon

# ─────────────────────────────────────────────
# Stage 2 : Runtime
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 업로드 디렉토리 생성
RUN mkdir -p /app/uploads/temp

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

VOLUME /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Dspring.profiles.active=docker", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
