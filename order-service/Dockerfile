
FROM openjdk:11
ARG SPRING_PROFILES_ACTIVE=dev
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
RUN mkdir -p /home/app
WORKDIR /home/app
COPY target/order-service-v0.1.0.jar order-service.jar
EXPOSE 8300
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "order-service.jar"]
