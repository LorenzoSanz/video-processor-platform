# Etapa 1: compilar la aplicación
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Copiar primero Maven y el pom para aprovechar la caché
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Normalizar los finales de línea del wrapper en Windows
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Descargar dependencias antes de copiar el código
RUN ./mvnw -B -ntp dependency:go-offline

COPY src/ src/

# Los tests se ejecutarán por separado antes del despliegue
RUN ./mvnw -B -ntp -DskipTests package


# Etapa 2: ejecutar con una imagen más pequeña
FROM eclipse-temurin:21-jre

WORKDIR /app

# Ejecutar como usuario no privilegiado
RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app \
       --shell /usr/sbin/nologin spring \
    && mkdir -p /var/lib/upload-service/uploads \
    && chown -R spring:spring /app /var/lib/upload-service

COPY --from=build --chown=spring:spring \
    /workspace/target/*.jar /app/app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]