# 作业进度看板（截至 2026-04-14）

## 1. 系统设计文档
- [x] 系统架构草图（README 有整体架构图）
- [~] 服务拆分到用户/商品/订单/库存（当前代码仍是单体模块，逻辑上有分层，未做独立微服务）
- [x] RESTful API 定义（controller 与 README 已覆盖核心接口）
- [x] 数据库 ER 设计（docs/database.md 已覆盖 users/products/stock/orders 等）
- [x] 技术栈选型说明（README 已说明后端/前端/中间件）

## 2. 环境准备
- [x] Git 仓库初始化
- [x] Spring Boot + MyBatis + MySQL 基础环境
- [x] 用户注册/登录功能

## 3. 容器环境
- [x] Dockerfile
- [x] docker-compose（MySQL/Redis/Backend/Nginx）

## 4. 负载均衡
- [x] 后端多实例（8081/8082）
- [x] Nginx 80 反代与转发
- [x] Nginx 多算法示例（轮询/加权/IP Hash/最少连接）
- [x] JMeter 压测指引文档（docs/jmeter-test-guide.md）

## 5. 动静分离
- [x] 前端静态页面（frontend）
- [x] Nginx 动静分离配置
- [x] 静态 vs 动态压测指引

## 6. 分布式缓存
- [x] 商品详情 Redis 缓存
- [x] 缓存穿透（空值缓存）
- [x] 缓存击穿（互斥锁）
- [x] 缓存雪崩（随机过期）

## 7. 读写分离
- [x] 双数据源路由（master/slave）
- [~] MySQL 主从复制链路（compose 有 master/replica 容器，但未完成真实复制配置）
- [x] 代码层读写路由测试基础（AOP + 注解）

## 8. 消息队列
- [ ] Kafka 异步下单（未实现）
- [x] 雪花算法订单号（本次已实现）
- [x] 幂等性（用户-活动唯一约束 + Redis 标记）
- [~] 一致性（当前为同步事务 + Redis 回补；未实现 MQ 最终一致）

## 9. 分库分表（选做）
- [ ] ShardingSphere 未实现

## 10. 事务与一致性（微服务场景）
- [~] Redis 预扣减与防超卖（已实现同步版）
- [ ] 消息一致性或 TCC（未实现）
- [~] 下单+库存扣减一致性（单体事务可用，未拆分服务）
- [ ] 支付+订单状态一致性跨服务方案（未实现）

## 11. 服务注册发现与配置
- [ ] Nacos 未实现
- [ ] Spring Cloud Gateway 未实现
- [ ] 动态配置刷新未实现

## 12. 流量治理
- [ ] 熔断未实现
- [ ] 限流未实现
- [ ] 降级未实现

## 本次新增（小步）
- [x] 完成秒杀核心同步链路（SeckillServiceImpl.executeSeckill）
- [x] 完成秒杀订单创建（OrderServiceImpl.createSeckillOrder）
- [x] 完成 Redis 库存预热（SeckillServiceImpl.preheatStock）
- [x] 编译验证通过（mvn -DskipTests compile）
