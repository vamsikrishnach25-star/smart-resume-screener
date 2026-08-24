# --- build stage -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies before copying sources
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin screener
COPY --from=build /build/target/smart-resume-screener-*.jar app.jar
RUN mkdir -p /app/data && chown -R screener:screener /app
USER screener

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
