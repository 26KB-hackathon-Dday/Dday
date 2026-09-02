# ── build ──────────────────────────────────────────────────────────────────
# 빌드는 이미지 안에서 한다. 로컬 JDK 버전이 뭐든 결과가 같아야 하기 때문이다.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 소스보다 먼저 복사해서 의존성 레이어를 캐시한다.
# 소스만 고친 재빌드는 여기를 건너뛰므로 배포가 훨씬 빠르다.
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

COPY src src
# 테스트는 CI가 이미 MySQL을 띄워 돌렸다. 여기서 또 돌리면 DB가 없어 실패한다.
RUN ./gradlew bootJar --no-daemon -x test

# ── runtime ────────────────────────────────────────────────────────────────
# JDK가 아니라 JRE 이미지를 쓴다 — 이미지가 절반 이하로 줄고 배포가 빨라진다.
FROM eclipse-temurin:17-jre
WORKDIR /app

# 컨테이너 OS 시각(로그 타임스탬프). JVM 기본 타임존은 DdayApplication이 따로 고정한다.
ENV TZ=Asia/Seoul

# root로 돌리지 않는다.
RUN useradd -r -u 1001 dday
USER dday

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
