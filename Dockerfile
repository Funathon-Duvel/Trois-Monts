FROM openjdk:11-jre-slim

ADD ./target/troismonts-*.jar app.jar
COPY ./target/lib/ lib/

CMD ["java","-jar","./app.jar"]
