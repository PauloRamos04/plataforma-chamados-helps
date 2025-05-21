# Estágio de build com Maven
FROM eclipse-temurin:17-jdk AS build

# Definir diretório de trabalho
WORKDIR /app

# Copiar apenas o necessário para resolver dependências primeiro
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Configurar permissão de execução e baixar dependências
RUN chmod +x ./mvnw && \
    ./mvnw dependency:go-offline -B

# Agora copiar o código-fonte e construir
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Estágio de produção
FROM eclipse-temurin:17-jre

# Evitar usar Alpine que pode causar problemas com aplicações Java
# Definir diretório de trabalho
WORKDIR /app

# Copiar o arquivo JAR do estágio de build
COPY --from=build /app/target/*.jar app.jar

# Expor porta
EXPOSE 8080

# Definir opções do Java
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# Executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]