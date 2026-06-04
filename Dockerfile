# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy gradle wrapper và cấu hình dependencies để cache dependencies trước
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# Copy source code và build
COPY src src
RUN ./gradlew build -x test --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy file jar từ build stage sang
COPY --from=build /app/build/libs/*.jar app.jar

# Copy file key firebase nếu có trong context
COPY chiabill-firebase.json* ./

EXPOSE 8080

# Tối ưu hóa JVM cho VPS tài nguyên trung bình (2GB - 4GB RAM)
ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]