FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :server1:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
COPY --from=build /workspace/server1/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
