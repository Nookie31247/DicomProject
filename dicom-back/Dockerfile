# === 1단계: 빌드 스테이지 ===
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle 빌드에 필요한 파일들만 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# 종속성 먼저 다운로드 (캐싱 활용)
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# === 2단계: 실행 스테이지 ===
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드 스테이지에서 생성된 얇은 jar 파일만 추출해서 가져옴
COPY --from=build /app/build/libs/*.jar app.jar
COPY models ./models

# 홈 서버 RAM 환경을 고려한 JVM 가동 옵션 (컴포즈에서 오버라이딩 가능)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]