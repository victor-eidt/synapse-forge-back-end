# Imagem do back-end para o Render (SYN-89). Dois estágios:
#   1. build: compila o jar com o Maven Wrapper num JDK 17
#   2. runtime: só o JRE 17 + o jar, rodando com usuário sem privilégios
# Os testes NÃO rodam aqui (-DskipTests): já rodaram na CI ("build · testes") antes do deploy.

# ---------- 1. build ----------
FROM eclipse-temurin:17-jdk-noble AS build
WORKDIR /build

# Dependências primeiro: enquanto o pom.xml não muda, esta camada vem do cache
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package \
    && cp target/crud-*.jar /build/app.jar

# ---------- 2. runtime ----------
FROM eclipse-temurin:17-jre-noble

RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /build/app.jar app.jar
USER app

# Plano free do Render = 512 MB. 75% vai para o heap e o resto fica para metaspace, threads e
# code cache. SerialGC e só o compilador C1 gastam menos memória e CPU (0,1 CPU no free) e
# deixam a subida mais rápida. Se faltar memória, a JVM sai e o Render reinicia o serviço.
# Para ajustar sem nova imagem, sobrescreva JAVA_TOOL_OPTIONS nas variáveis do serviço.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError"

# O Render injeta PORT (padrão 10000); o application.properties lê server.port=${PORT:8081}
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
