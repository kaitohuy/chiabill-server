# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY . .
# Cấp quyền thực thi cho gradlew
RUN chmod +x ./gradlew
# Build file jar (bỏ qua test để nhanh hơn)
RUN ./gradlew build -x test

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy file jar từ build stage sang
COPY --from=build /app/build/libs/chia-bill-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
#-Xms200m -Xmx300m -XX:+UseSerialGC -XX:MaxMetaspaceSize=80m