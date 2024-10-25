FROM gradle:jdk21 AS BUILD
WORKDIR /usr/app/
COPY . .
RUN gradlew build

FROM eclipse-temurin:21
VOLUME /tmp
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY --from=BUILD $APP_HOME .
ENTRYPOINT exec java -jar $APP_HOME/build/libs/banking-bot-1.0.jar