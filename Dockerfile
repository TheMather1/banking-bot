FROM gradle:jdk21 AS BUILD
ARG GITHUB_TOKEN
WORKDIR /usr/app/
COPY . .
ENV GITHUB_TOKEN=$GITHUB_TOKEN
RUN gradle build

FROM eclipse-temurin:21
VOLUME /tmp
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY --from=BUILD $APP_HOME .
ENTRYPOINT exec java -jar $APP_HOME/build/libs/banking-bot-1.0.jar