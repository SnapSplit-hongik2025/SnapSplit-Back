# 1. 빌드(build) 단계
FROM amazoncorretto:17 AS builder
WORKDIR /workspace/app

# gradlew + wrapper 먼저 복사
COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

# 나머지 빌드 스크립트 먼저 복사해서 캐시 잡기
COPY build.gradle settings.gradle* ./

# 의존성 미리 다운로드
RUN ./gradlew dependencies --no-daemon|| true

# 소스 복사
COPY . .

# 실제 빌드 (테스트 제외)
RUN ./gradlew clean bootJar -x test --no-daemon

# 2. 실행(run) 단계 (alpine → non-alpine)
FROM amazoncorretto:17
WORKDIR /app

# 빌드 단계에서 생성된 jar 파일 복사
COPY --from=builder /workspace/app/build/libs/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-Dspring.config.location=file:/app/config/application.yml","-jar","/app/app.jar"]
