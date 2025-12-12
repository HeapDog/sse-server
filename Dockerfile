FROM amazoncorretto:25-alpine AS builder

RUN apk add --no-cache bash

WORKDIR /app

COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew
RUN ./gradlew --version

RUN ./gradlew build -x test || true

COPY . .
RUN ./gradlew clean build -x test

FROM amazoncorretto:25-alpine

WORKDIR /app

ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE --server.port=$SERVER_PORT"]
