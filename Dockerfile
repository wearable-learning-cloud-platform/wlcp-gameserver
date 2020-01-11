FROM openjdk:8-jre-alpine
RUN apk add curl jq
COPY start.sh /start.sh
RUN chmod +x /start.sh
COPY /target/wlcp-gameserver-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 3333
CMD ["sh", "/start.sh"]