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
COPY tokens $APP_HOME/tokens

RUN ./gradlew build -x test

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
COPY --from=build /app/tokens ./tokens

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]