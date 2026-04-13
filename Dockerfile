FROM eclipse-temurin:17-jre

WORKDIR /app

RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

COPY app.jar app.jar

EXPOSE 8082

ENTRYPOINT ["sh", "-c",  "java ${JAVA_OPTS} -jar app.jar"]