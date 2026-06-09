# Aries 项目

基于 Spring Boot 的企业级应用项目。

## 目录结构

```
Aries/
├── src/                    # 源代码目录
│   ├── main/
│   │   ├── java/          # Java 源代码
│   │   └── resources/     # 配置文件
│   └── test/              # 测试代码
├── target/                 # 编译输出目录
├── resource/               # 静态资源目录
├── logs/                   # 日志文件目录
├── .env                    # 环境变量配置（本地开发）
├── .env.example            # 环境变量模板
├── start.sh                # 本地启动脚本
├── start-prod.sh           # 生产环境启动脚本
├── stop-prod.sh            # 生产环境停止脚本
└── pom.xml                 # Maven 配置文件
```

## 快速开始

### 环境要求

- JDK 11 或更高版本
- Maven 3.6+
- MySQL 5.7+
- Redis 5.0+

### 本地开发

1. **配置环境变量**

复制环境变量模板：
```bash
cp .env.example .env
```

编辑 `.env` 文件，填写实际的配置信息。

2. **安装依赖**

```bash
mvn clean install
```

3. **启动应用**

```bash
./start.sh
```

或使用 Maven：
```bash
mvn spring-boot:run
```

4. **访问应用**

应用启动后，访问：`http://localhost:9200/aries`

## 环境配置

### 开发环境（dev）

使用 `.env` 文件管理环境变量，便于本地开发。

推荐添加 `spring-dotenv` 依赖以支持 `.env` 文件：

```xml
<dependency>
    <groupId>me.paulschwarz</groupId>
    <artifactId>spring-dotenv</artifactId>
    <version>4.0.0</version>
</dependency>
```

### 生产环境（prd）

生产环境使用启动脚本管理环境变量，不推荐使用 `.env` 文件。

1. **编辑启动脚本**

修改 `start-prod.sh` 中的环境变量为生产环境实际值。

2. **打包应用**

```bash
mvn clean package -DskipTests
```

3. **启动生产环境**

```bash
chmod +x start-prod.sh stop-prod.sh
./start-prod.sh
```

4. **查看日志**

```bash
tail -f logs/aries.log
```

5. **停止应用**

```bash
./stop-prod.sh
```

## 配置说明

### Spring Profiles

项目支持多环境配置：

- `dev` - 开发环境
- `test` - 测试环境
- `prd` - 生产环境

通过 `spring.profiles.active` 参数指定环境：
```bash
java -jar aries.jar --spring.profiles.active=prd
```

### 环境变量列表

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| MYSQL_HOST | MySQL 主机地址 | - |
| MYSQL_PORT | MySQL 端口 | - |
| MYSQL_DATABASE | 数据库名称 | - |
| MYSQL_USERNAME | 数据库用户名 | - |
| MYSQL_PASSWORD | 数据库密码 | - |
| REDIS_HOST | Redis 主机地址 | - |
| REDIS_PORT | Redis 端口 | - |
| REDIS_DB | Redis 数据库索引 | - |
| REDIS_PASSWORD | Redis 密码 | - |
| TX_CLOUD_COS_UID | 腾讯云 COS UID | - |
| TX_CLOUD_COS_REGION | 腾讯云 COS 区域 | - |
| TX_CLOUD_COS_SECRET_ID | 腾讯云 COS 密钥 ID | - |
| TX_CLOUD_COS_SECRET_KEY | 腾讯云 COS 密钥 Key | - |
| TX_CLOUD_COS_BUCKET | 腾讯云 COS 存储桶名称 | - |
| SMS_BASE_URL | 短信服务 URL | - |
| SMS_KEY | 短信服务密钥 | - |
| SMS_TPL_ID | 短信模板 ID | - |
| ARIES_SECURITY_JWT_KEY_ID | JWT Key ID | - |
| ARIES_SECURITY_JWT_PUBLIC_KEY | JWT 公钥 | - |
| ARIES_SECURITY_JWT_PRIVATE_KEY | JWT 私钥 | - |
| ARIES_SECURITY_RSA_PRIVATE_KEY | RSA 私钥 | - |
| ARIES_SNOWFLAKE_WORKER_ID | 雪花算法 Worker ID | 1 |
| ARIES_SNOWFLAKE_DATACENTER_ID | 雪花算法 Datacenter ID | 1 |

## 部署建议

### 生产环境部署

1. **使用 systemd 管理服务**（推荐）

创建 `/etc/systemd/system/aries.service`：
```ini
[Unit]
Description=Aries Application
After=network.target mysql.service redis.service

[Service]
Type=simple
User=aries
WorkingDirectory=/opt/aries
Environment="MYSQL_HOST=prod-db.example.com"
Environment="MYSQL_PASSWORD=your_password"
# ... 其他环境变量
ExecStart=/usr/bin/java -Xms512m -Xmx2048m -XX:+UseG1GC -jar /opt/aries/aries.jar --spring.profiles.active=prd
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
sudo systemctl daemon-reload
sudo systemctl enable aries
sudo systemctl start aries
```

2. **Docker 容器化部署**

创建 `Dockerfile`：
```dockerfile
FROM openjdk:11-jre-slim
COPY target/aries.jar /app/aries.jar
WORKDIR /app
EXPOSE 9200
ENTRYPOINT ["java", "-jar", "aries.jar"]
```

创建 `docker-compose.yml`：
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "9200:9200"
    environment:
      - MYSQL_HOST=mysql
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
      - REDIS_HOST=redis
    depends_on:
      - mysql
      - redis

  mysql:
    image: mysql:5.7
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: aries_prod
    volumes:
      - mysql-data:/var/lib/mysql

  redis:
    image: redis:6-alpine
    volumes:
      - redis-data:/data

volumes:
  mysql-data:
  redis-data:
```

## 常见问题

### 1. 应用启动失败

检查日志文件：`tail -f logs/aries.log`，确认：
- MySQL 和 Redis 服务是否正常运行
- 环境变量配置是否正确
- 端口 9200 是否被占用

### 2. 数据库连接失败

确认：
- MySQL 服务是否启动
- 数据库用户名和密码是否正确
- 防火墙是否允许连接

### 3. Redis 连接失败

确认：
- Redis 服务是否启动
- Redis 密码配置是否正确

## 维护操作

### 查看应用状态

```bash
ps aux | grep aries.jar
```

### 重启应用

```bash
./stop-prod.sh
./start-prod.sh
```

### 清理日志

```bash
rm -f logs/*.gz
```

## 项目文档

- [接口说明.md](docs/接口说明.md) - API 接口文档
- [项目说明.md](docs/项目说明.md) - 项目详细说明

## 技术栈

- Spring Boot
- Spring Data JPA
- MySQL
- Redis
- 腾讯云 COS
- 腾讯云短信

## 许可证

Copyright © 2025
