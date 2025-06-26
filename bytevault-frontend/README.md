# ByteVault 前端项目

这是一个使用 Vue 3 + Vite + Pinia + Axios 构建的文件管理系统前端项目。

## 技术栈

- Vue 3 - 渐进式JavaScript框架
- Vite - 现代前端构建工具
- TypeScript - JavaScript的超集，提供类型检查
- Pinia - Vue状态管理库
- Axios - 基于Promise的HTTP客户端
- Element Plus - 基于Vue 3的UI组件库

## 项目结构

```
bytevault-frontend/
├── public/           # 静态资源目录
├── src/              # 源代码目录
│   ├── api/          # API接口定义
│   ├── assets/       # 资源文件(图片、样式等)
│   ├── components/   # 通用组件
│   ├── router/       # 路由配置
│   ├── stores/       # Pinia状态管理
│   ├── utils/        # 工具函数
│   ├── views/        # 页面视图组件
│   ├── App.vue       # 根组件
│   └── main.ts       # 入口文件
├── index.html        # HTML模板
├── package.json      # 项目依赖配置
├── tsconfig.json     # TypeScript配置
└── vite.config.ts    # Vite配置
```

## 开发指南

### 环境要求

- Node.js 16.x 或更高版本
- npm 7.x 或更高版本

### 安装依赖

```bash
cd bytevault-frontend
npm install
```

### 开发服务器

```bash
npm run dev
```

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 连接后端

前端项目默认配置与SpringBoot后端API对接，API前缀为`/api`。后端服务默认运行在`http://localhost:8088`。

## 登录信息

默认管理员账号:
- 用户名: admin
- 密码: admin 