FROM maven:3.8.4-openjdk-17-slim AS build
COPY . /code
WORKDIR /code

# Necessary in order to run on Jenkins. Comment out when running on local machine without access to nexus.
COPY settings.xml /root/.m2/settings.xml

## Build
RUN mvn clean package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM openjdk:17.0.2-slim

ARG DEPENDENCY=/code/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","se.sundsvall.supportmanagement.Application"]