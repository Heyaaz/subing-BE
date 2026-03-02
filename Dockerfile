# Build stage
FROM gradle:8.14.3-jdk17 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./gradlew
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

# JVM 메모리 최적화 (Railway 1GB 서버 기준, 총 RSS ~800MB 목표)
# Heap(512m) + Metaspace(128m) + DirectMem(128m) + CodeCache(48m) + etc ≈ 800MB
ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:MaxMetaspaceSize=128m \
  -XX:MaxDirectMemorySize=128m \
  -XX:+UseSerialGC \
  -Xss256k \
  -XX:ReservedCodeCacheSize=48m \
  -XX:+UseCompressedOops \
  -XX:+UseCompressedClassPointers \
  -XX:CompressedClassSpaceSize=32m \
  -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]
