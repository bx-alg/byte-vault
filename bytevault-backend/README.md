# ByteVault 后端项目

这是一个使用Spring Boot + MyBatis-Plus构建的文件管理系统后端项目。

## 技术栈

- Spring Boot 2.7.0 - Java框架
- MyBatis-Plus 3.5.2 - ORM框架
- MySQL - 数据库
- Spring Security - 安全框架（待集成）
- JWT - 身份验证（待集成）

## 项目结构

```
bytevault-backend/
├── src/                               # 源代码目录
│   ├── main/                          # 主要代码
│   │   ├── java/                      # Java代码
│   │   │   └── com/bytevault/app/     # 应用程序包
│   │   │       ├── auth/              # 认证授权模块
│   │   │       │   ├── controller/    # 控制器
│   │   │       │   ├── service/       # 服务
│   │   │       │   ├── model/         # 模型/实体
│   │   │       │   └── mapper/        # MyBatis映射器
│   │   │       ├── file/              # 文件管理模块（待实现）
│   │   │       ├── config/            # 配置类
│   │   │       ├── controller/        # 通用控制器
│   │   │       ├── model/             # 通用模型/实体
│   │   │       ├── mapper/            # 通用映射器
│   │   │       ├── service/           # 通用服务
│   │   │       └── ByteVaultApplication.java  # 应用程序入口
│   │   └── resources/                 # 资源文件
│   │       ├── application.yml        # 应用配置
│   │       ├── db/                    # 数据库脚本
│   │       ├── mapper/                # MyBatis XML映射文件
│   │       ├── static/                # 静态资源
│   │       └── templates/             # 模板文件
│   └── test/                          # 测试代码
│       └── java/                      # Java测试代码
├── pom.xml                            # Maven配置
└── README.md                          # 项目说明
```

## 数据库设计

系统包含以下主要表：
- user: 用户表
- role: 角色表
- permission: 权限表
- user_role: 用户角色关联表
- role_permission: 角色权限关联表

## 如何运行

### 环境要求

- JDK 11+
- Maven 3.6+
- MySQL 8.0+

### 数据库设置

1. 创建MySQL数据库
2. 执行`src/main/resources/db/init-mysql.sql`初始化数据库

### 构建与运行

```bash
cd bytevault-backend
mvn clean package
java -jar target/bytevault-app-0.0.1-SNAPSHOT.jar
```

或者使用Maven直接运行：

```bash
mvn spring-boot:run
```

### API端点

- `POST /api/auth/login` - 用户登录
- `GET /api/users` - 获取所有用户
- `GET /api/users/{username}` - 通过用户名获取用户
- `POST /api/users` - 添加新用户

## 管理员账号

- 用户名: admin
- 密码: admin 

## 启动 MinIO 服务

```bash
cd bytevault-backend
docker-compose -f docker/minio.yml up -d
```