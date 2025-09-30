# Dockerfile

# 1. 빌드(build) 단계
FROM amazoncorretto:17 AS builder
WORKDIR /workspace/app
COPY . .
RUN ./gradlew build

# 2. 실행(run) 단계
FROM amazoncorretto:17-alpine
WORKDIR /app

# 빌드 단계에서 생성된 jar 파일을 복사
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

# API가 사용할 포트 번호 (예: 8080)
EXPOSE 8080

# 컨테이너가 시작될 때 실행할 명령어
ENTRYPOINT ["java","-jar","/app/app.jar"]