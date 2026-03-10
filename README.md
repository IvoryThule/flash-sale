# Flash Sale System -- 高并发电商秒杀系统

> 一个基于 Spring Boot + Vue3 的高并发电商秒杀系统，支持 Redis 缓存预热、库存预减、分布式锁防超卖，并提供完整的 Docker 容器化部署方案。

---

## 项目介绍

Flash Sale System 是一个面向学习与生产实践的高并发秒杀电商平台。系统涵盖用户注册登录、商品浏览、限时秒杀抢购、订单管理等核心电商功能，重点解决秒杀场景下的 **库存超卖**、**接口限流**、**缓存一致性** 等高并发难题。

### 核心亮点

- **Redis 库存预减**：秒杀请求先经 Redis 原子扣减，未命中者直接拦截，大幅减少数据库压力
- **分布式锁防超卖**：基于 Redis 分布式锁保证同一商品不会被重复下单
- **缓存三防策略**：针对缓存穿透、缓存击穿、缓存雪崩分别设计布隆过滤器、互斥锁、随机过期时间
- **前后端分离**：后端提供 RESTful API，前端 Vue3 + Element Plus 构建现代化交互界面
- **容器化部署**：Docker Compose 一键启动 MySQL / Redis / Backend / Nginx 全栈环境
- **压力测试**：JMeter 脚本覆盖 100 / 500 / 1000 并发，验证系统极限吞吐

---

## 技术栈

### Backend

| 技术            | 说明                     |
| --------------- | ----------------------- |
| Java 17         | 开发语言                  |
| Spring Boot 3.x | 应用框架                  |
| MyBatis         | ORM 持久层               |
| MySQL 8.0       | 关系型数据库              |
| Redis 7.x       | 缓存 / 分布式锁 / 库存预减 |

### Frontend

| 技术         | 说明              |
| ------------ | ---------------- |
| Vue 3        | 前端框架           |
| Vite         | 构建工具           |
| Axios        | HTTP 请求          |
| Element Plus | UI 组件库          |
| Vue Router   | 路由管理           |
| Pinia        | 状态管理           |

### Infrastructure

| 技术       | 说明               |
| ---------- | ----------------- |
| Docker     | 容器化部署          |
| Nginx      | 反向代理 / 负载均衡  |
| JMeter     | 压力测试           |

---

## 系统架构图

```
                            +------------------+
                            |     Browser      |
                            +--------+---------+
                                     |
                                     v
                            +------------------+
                            |      Nginx       |
                            | (负载均衡/静态资源) |
                            +--------+---------+
                                     |
                      +--------------+--------------+
                      |                             |
                      v                             v
              +---------------+             +---------------+
              | Vue3 Frontend |             | Spring Boot   |
              | (Vite Build)  |             | REST API      |
              +---------------+             +-------+-------+
                                                    |
                                  +-----------------+-----------------+
                                  |                                   |
                                  v                                   v
                          +---------------+                   +---------------+
                          |     Redis     |                   |     MySQL     |
                          | (缓存/预减/锁) |                   |   (持久化)    |
                          +---------------+                   +---------------+
```

### 秒杀请求处理流程

```
用户点击秒杀
    |
    v
Nginx 转发请求
    |
    v
Spring Boot 接收请求
    |
    v
Redis 库存预减 (DECR 原子操作)
    |
    +---> 库存 <= 0 ? ---> 返回 "已售罄"
    |
    v
获取 Redis 分布式锁
    |
    +---> 获取失败 ? ---> 返回 "请求繁忙，请重试"
    |
    v
MySQL 扣减库存 (乐观锁 WHERE stock > 0)
    |
    v
创建秒杀订单
    |
    v
释放分布式锁
    |
    v
返回 "秒杀成功"
```

---

## 模块说明

```
flash-sale/
├── docs/                    # 项目文档
│   ├── PRD.md              #   产品需求文档
│   ├── architecture.md     #   系统架构设计文档
│   └── database.md         #   数据库设计文档
│
├── backend/                 # 后端服务 (Spring Boot)
│   └── flash-sale-api/     #   主服务模块
│       ├── controller/     #     接口层
│       ├── service/        #     业务逻辑层
│       ├── mapper/         #     数据访问层
│       ├── entity/         #     数据实体
│       └── config/         #     配置类
│
├── frontend/                # 前端项目 (Vue3 + Vite)
│   ├── src/
│   │   ├── views/          #   页面组件
│   │   ├── components/     #   公共组件
│   │   ├── api/            #   接口封装
│   │   ├── router/         #   路由配置
│   │   └── store/          #   状态管理
│   └── public/             #   静态资源
│
├── deploy/                  # 部署配置
│   ├── docker-compose.yml  #   Docker 编排
│   ├── nginx/              #   Nginx 配置
│   ├── mysql/              #   MySQL 初始化脚本
│   └── jmeter/             #   JMeter 压测脚本
│
├── .gitignore
└── README.md
```

### 各模块职责

| 模块       | 职责                                                         |
| ---------- | ----------------------------------------------------------- |
| `docs`     | 存放产品需求、架构设计、数据库设计等项目文档                        |
| `backend`  | Spring Boot 后端服务，提供用户、商品、秒杀、订单等 RESTful API     |
| `frontend` | Vue3 前端应用，提供用户交互界面，包含登录、商品列表、秒杀、订单等页面   |
| `deploy`   | Docker / Nginx / MySQL / JMeter 等部署与测试相关配置文件          |

---

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7.x+
- Docker & Docker Compose
- Maven 3.9+

### 本地开发

```bash
# 1. 克隆项目
git clone https://github.com/IvoryThule/flash-sale.git
cd flash-sale

# 2. 启动后端
cd backend/flash-sale-api
mvn spring-boot:run

# 3. 启动前端
cd frontend
npm install
npm run dev
```

### Docker 部署

```bash
cd deploy
docker-compose up -d
```

---

## API 概览

| 方法   | 路径                    | 说明        |
| ------ | ---------------------- | ---------- |
| POST   | `/user/register`       | 用户注册    |
| POST   | `/user/login`          | 用户登录    |
| GET    | `/user/info`           | 用户信息    |
| GET    | `/product/list`        | 商品列表    |
| GET    | `/product/{id}`        | 商品详情    |
| POST   | `/seckill/{productId}` | 执行秒杀    |
| GET    | `/order/list`          | 订单列表    |
| GET    | `/order/{id}`          | 订单详情    |

---

## License

MIT License
