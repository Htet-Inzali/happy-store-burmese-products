# 🌟 အဆင့် ၁: Code များကို Build လုပ်ခြင်း
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew build -x test

# 🌟 အဆင့် ၂: Build ထားသော ဖိုင်ကို Run ခြင်း
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/happy-store-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]