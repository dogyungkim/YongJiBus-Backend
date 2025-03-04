FROM openjdk:17-jdk-slim AS build

ENV APP_HOME=/app
WORKDIR $APP_HOME

COPY gradlew $APP_HOME/gradlew
COPY gradle $APP_HOME/gradle
COPY build.gradle $APP_HOME/

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

COPY src $APP_HOME/src
COPY .env $APP_HOME/.env

RUN ./gradlew build --no-daemon

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]