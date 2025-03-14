FROM eclipse-temurin:17-jdk-alpine AS build

ENV APP_HOME=/app
WORKDIR $APP_HOME

COPY gradlew $APP_HOME/gradlew
COPY gradle $APP_HOME/gradle
COPY build.gradle $APP_HOME/

RUN chmod +x ./gradlew
RUN ./gradlew dependencies

COPY src $APP_HOME/src
COPY .env $APP_HOME/.env

RUN ./gradlew build

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]