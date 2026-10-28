# ===========================================
# 智能教师管理系统 - 后端 Docker 镜像
# 构建: docker build -t xzs-backend:3.9.0 .
# ===========================================

# ---- Stage 1: Maven 构建 ----
FROM maven:3.8.6-openjdk-8-slim AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ---- Stage 2: JRE 运行 ----
FROM openjdk:8-jre-slim
WORKDIR /app

# 创建数据目录
RUN mkdir -p /app/data /app/log

COPY --from=builder /build/target/xzs-3.9.0.jar app.jar
COPY --from=builder /build/target/xzs-3.9.0.jar app.jar

EXPOSE 8001

ENV JAVA_OPTS="-Xms256m -Xmx1024m -XX:+UseG1GC"

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD java -cp app.jar org.springframework.boot.loader.JarLauncher --spring.profiles.active=docker 2>&1 | grep -q "Started" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
