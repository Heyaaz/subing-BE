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

# JVM 메모리 최적화 (Railway Hobby 플랜: 총 RSS ~450MB 목표)
# Heap(350m) + Metaspace(100m) + CodeCache(48m) + Threads + Native ≈ 450MB
ENV JAVA_OPTS="-Xms200m -Xmx350m \
  -XX:MaxMetaspaceSize=100m \
  -XX:MaxDirectMemorySize=64m \
  -XX:+UseSerialGC \
  -Xss256k \
  -XX:ReservedCodeCacheSize=48m \
  -XX:+UseCompressedOops \
  -XX:+UseCompressedClassPointers \
  -XX:CompressedClassSpaceSize=32m \
  -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]
