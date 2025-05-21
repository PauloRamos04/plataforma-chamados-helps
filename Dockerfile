# Estágio de build com Maven
FROM maven:3.9-eclipse-temurin-17 AS build

# Definir diretório de trabalho
WORKDIR /app

# Copiar o projeto completo
COPY . .

# Desabilitar filtragem de recursos
RUN mvn clean package -DskipTests -Dmaven.resources.skip=false -Dmaven.resources.filtering=false

# Estágio de produção
FROM eclipse-temurin:17-jre

# Definir diretório de trabalho
WORKDIR /app

# Copiar o arquivo JAR do estágio de build
COPY --from=build /app/target/*.jar app.jar

# Expor porta
EXPOSE 8080

# Definir opções do Java
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# Executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]