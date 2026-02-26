# Usar una imagen base de Maven para construir el proyecto
FROM maven:3.9-eclipse-temurin-21-jammy AS build

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar el archivo pom.xml y las carpetas necesarias
COPY pom.xml ./
COPY src ./src

# Construir el proyecto
RUN mvn clean package -DskipTests

# Usar una imagen base de JDK para ejecutar la aplicación
FROM eclipse-temurin:21-jdk-alpine

# Copiar el archivo JAR generado
COPY --from=build /app/target/*.jar app.jar

# Copiar carpeta wallet de oracle cloud
COPY wallet /app/wallet

# Exponer el puerto de la aplicación
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]