# 数据库设计文档
## ER 关系图
```
+------------+         +----------------+
|   users    |         | user_address   |
+------------+         +----------------+
| id (PK)    |<------->| user_id (FK)   |
| username   |   1:N   | receiver       |
| password   |         | phone          |
| nickname   |         | province/city  |
| phone (UK) |         | detail         |
| email      |         | is_default     |
| status     |         +----------------+
+-----+------+
      |
      | 1:N
      v
+------------+         +------------------+
|   orders   |<------->| seckill_orders   |
+------------+   1:1   +------------------+
| id (PK)    |         | id (PK)          |
| order_no   |         | user_id          |
| user_id    |         | product_id       |
| product_id |         | event_id         |
| event_id   |         | order_id (FK)    |
| order_type |         +------------------+
| status     |         UK: (user_id,
| pay_channel|              event_id)
| expire_time|
| address_id |
+-----+------+
      |
      | N:1
      v
+------------+         +------------------+         +------------+
|  products  |<------->| seckill_events   |<------->|   stock    |
+------------+   1:N   +------------------+   1:1   +------------+
| id (PK)    |         | id (PK)          |         | id (PK)    |
| name       |         | title            |         | product_id |
| description|         | product_id (FK)  |         | event_id   |
| image      |         | seckill_price    |         | total      |
| price      |         | start_time       |         | available  |
| category   |         | end_time         |         | locked     |
| status     |         | total_stock      |         | sold       |
+------------+         | available_stock  |         | version    |
                       | limit_per_user   |         +------------+
                       | status           |
                       +------------------+
+------------------+
| operation_logs   |
+------------------+
| id (PK)          |
| user_id          |
| action           |
| target_type      |
| target_id        |
| detail           |
| ip               |
| created_at       |
+------------------+
```
## 表设计说明
### 1. users -- 用户表
| 字段       | 类型          | 约束              | 说明                              |
| ---------- | ------------- | ----------------- | --------------------------------- |
| id         | BIGINT        | PK, AUTO_INC      | 用户ID                            |
| username   | VARCHAR(64)   | UNIQUE, NOT NULL  | 登录用户名                         |
| password   | VARCHAR(128)  | NOT NULL          | BCrypt 加密密码                    |
| nickname   | VARCHAR(64)   |                   | 前端展示昵称                       |
| phone      | VARCHAR(20)   | UNIQUE            | 手机号，唯一约束                    |
| email      | VARCHAR(128)  |                   | 邮箱                              |
| avatar     | VARCHAR(256)  |                   | 头像 URL                          |
| status     | TINYINT       | DEFAULT 1         | 0-禁用, 1-正常                     |
| created_at | DATETIME      | DEFAULT NOW       | 创建时间                           |
| updated_at | DATETIME      | ON UPDATE NOW     | 更新时间                           |
### 2. user_address -- 用户收货地址表
| 字段       | 类型          | 约束              | 说明                              |
| ---------- | ------------- | ----------------- | --------------------------------- |
| id         | BIGINT        | PK, AUTO_INC      | 地址ID                            |
| user_id    | BIGINT        | NOT NULL, INDEX   | 用户ID                            |
| receiver   | VARCHAR(64)   | NOT NULL          | 收货人姓名                         |
| phone      | VARCHAR(20)   | NOT NULL          | 收货人手机号                       |
| province   | VARCHAR(32)   |                   | 省                                |
| city       | VARCHAR(32)   |                   | 市                                |
| district   | VARCHAR(32)   |                   | 区/县                             |
| detail     | VARCHAR(256)  |                   | 详细地址                           |
| is_default | TINYINT       | DEFAULT 0         | 0-否, 1-默认地址                   |
### 3. products -- 商品表
| 字段          | 类型           | 约束              | 说明                              |
| ------------- | -------------- | ----------------- | --------------------------------- |
| id            | BIGINT         | PK, AUTO_INC      | 商品ID                            |
| name          | VARCHAR(128)   | NOT NULL          | 商品名称                           |
| description   | TEXT           |                   | 商品描述                           |
| image         | VARCHAR(512)   |                   | 商品主图 URL                       |
| price         | DECIMAL(10,2)  | NOT NULL          | 原价                              |
| category      | VARCHAR(64)    | INDEX             | 商品分类                           |
| status        | TINYINT        | DEFAULT 0         | 0-未上架, 1-已上架, 2-已下架        |
**设计变更**：秒杀价格、时间已移至 seckill_events 表，商品表只保留基本属性。
### 4. seckill_events -- 秒杀活动表 [新增]
| 字段            | 类型           | 约束              | 说明                              |
| --------------- | -------------- | ----------------- | --------------------------------- |
| id              | BIGINT         | PK, AUTO_INC      | 秒杀活动ID                        |
| title           | VARCHAR(128)   |                   | 活动标题（如"3.15大促"）            |
| product_id      | BIGINT         | NOT NULL, INDEX   | 关联商品ID                        |
| seckill_price   | DECIMAL(10,2)  | NOT NULL          | 秒杀价格                          |
| start_time      | DATETIME       | NOT NULL          | 秒杀开始时间                       |
| end_time        | DATETIME       | NOT NULL          | 秒杀结束时间                       |
| total_stock     | INT            | NOT NULL          | 活动总库存                         |
| available_stock | INT            | NOT NULL, CHECK>=0| 活动可用库存                       |
| limit_per_user  | INT            | DEFAULT 1         | 每人限购数量                       |
| status          | TINYINT        | DEFAULT 0         | 0-未开始, 1-进行中, 2-已结束, 3-取消|
**设计要点**：一个商品可参加多场秒杀活动（3.15、6.18、双11），每场活动独立价格和库存。
### 5. stock -- 库存表
| 字段       | 类型     | 约束                    | 说明                              |
| ---------- | -------- | ----------------------- | --------------------------------- |
| id         | BIGINT   | PK, AUTO_INC            | 库存记录ID                         |
| product_id | BIGINT   | NOT NULL, INDEX         | 关联商品ID                         |
| event_id   | BIGINT   | UNIQUE, NOT NULL        | 关联秒杀活动ID（一对一）            |
| total      | INT      | NOT NULL, DEFAULT 0     | 总库存量                           |
| available  | INT      | CHECK >= 0              | 当前可用库存                       |
| locked     | INT      | CHECK >= 0              | 已锁定库存（下单未支付）             |
| sold       | INT      | CHECK >= 0              | 已售出数量                         |
| version    | INT      | DEFAULT 0               | 乐观锁版本号                       |
**设计要点**：
- 通过 event_id 关联活动，每场独立库存
- CHECK 约束数据库层面防止负库存
- `version` 乐观锁：`UPDATE ... SET version = version + 1 WHERE version = ? AND available > 0`
- `sold` 字段便于统计销量
### 6. orders -- 订单表
| 字段         | 类型           | 约束              | 说明                              |
| ------------ | -------------- | ----------------- | --------------------------------- |
| id           | BIGINT         | PK, AUTO_INC      | 订单ID                            |
| order_no     | VARCHAR(64)    | UNIQUE, NOT NULL  | 订单编号（雪花算法）                 |
| user_id      | BIGINT         | NOT NULL          | 下单用户ID                         |
| product_id   | BIGINT         | NOT NULL          | 商品ID                            |
| event_id     | BIGINT         | NULLABLE          | 秒杀活动ID（普通订单为NULL）         |
| product_name | VARCHAR(128)   |                   | 商品名称快照                       |
| product_image| VARCHAR(512)   |                   | 商品图片快照                       |
| quantity     | INT            | DEFAULT 1         | 购买数量                           |
| unit_price   | DECIMAL(10,2)  | NOT NULL          | 下单时单价                         |
| total_price  | DECIMAL(10,2)  | NOT NULL          | 订单总价                           |
| order_type   | TINYINT        | NOT NULL          | 1-普通订单, 2-秒杀订单              |
| status       | TINYINT        | DEFAULT 0         | 0-待支付, 1-已支付, 2-取消, 3-退款, 4-超时|
| pay_channel  | TINYINT        | NULLABLE          | 1-支付宝, 2-微信, 3-银行卡          |
| pay_time     | DATETIME       | NULLABLE          | 支付时间                           |
| expire_time  | DATETIME       | NOT NULL          | 订单过期时间                       |
| address_id   | BIGINT         | NULLABLE          | 收货地址ID                         |
| remark       | VARCHAR(256)   |                   | 订单备注                           |
**设计要点**：
- `order_type` 区分普通/秒杀订单，不需要单独判断逻辑
- `expire_time` 支持 15 分钟支付超时 -> 自动取消 -> 释放库存
- `(user_id, status)` 联合索引，高效支持"我的待支付订单"查询
- `expire_time` 索引，定时任务扫描超时订单
### 7. seckill_orders -- 秒杀订单表
| 字段       | 类型     | 约束                           | 说明                              |
| ---------- | -------- | ------------------------------ | --------------------------------- |
| id         | BIGINT   | PK, AUTO_INC                   | 秒杀订单ID                        |
| user_id    | BIGINT   | NOT NULL                       | 秒杀用户ID                        |
| product_id | BIGINT   | NOT NULL                       | 秒杀商品ID                        |
| event_id   | BIGINT   | NOT NULL                       | 秒杀活动ID                        |
| order_id   | BIGINT   | NOT NULL, INDEX                | 关联订单ID                        |
| status     | TINYINT  | DEFAULT 1                      | 0-失败, 1-成功                     |
**设计要点**：`(user_id, event_id)` 联合唯一索引 -- 同一用户同一活动只能秒杀一次（数据库层兜底）。
### 8. operation_logs -- 操作日志表 [新增]
| 字段        | 类型          | 约束              | 说明                              |
| ----------- | ------------- | ----------------- | --------------------------------- |
| id          | BIGINT        | PK, AUTO_INC      | 日志ID                            |
| user_id     | BIGINT        | NULLABLE, INDEX   | 操作用户（系统操作为NULL）           |
| action      | VARCHAR(64)   | NOT NULL, INDEX   | REGISTER/LOGIN/SECKILL/ORDER 等   |
| target_type | VARCHAR(32)   |                   | USER/PRODUCT/ORDER/STOCK 等       |
| target_id   | BIGINT        |                   | 目标ID                            |
| detail      | VARCHAR(512)  |                   | 操作详情                           |
| ip          | VARCHAR(64)   |                   | 操作IP                            |
| created_at  | DATETIME      | INDEX             | 操作时间                           |
## 防超卖机制（四层防线）
```
第一层：Redis DECR 原子预减库存
  |  拦截 99% 无效请求，不落库
  v
第二层：Redis 分布式锁
  |  串行化同一活动的秒杀请求
  v
第三层：MySQL 乐观锁 + CHECK 约束
  |  UPDATE ... SET version = version + 1 WHERE version = ? AND available > 0
  |  CHECK (available >= 0) 数据库层最终兜底
  v
第四层：seckill_orders (user_id, event_id) 唯一索引
  |  防止同一用户重复秒杀同一活动
  v
[安全]
```
## 支付超时释放库存
```
下单成功
  |
  v
锁定库存 (locked + 1, available - 1)
  |
  v
写入 expire_time = NOW() + 15分钟
  |
  v
定时任务扫描: WHERE status = 0 AND expire_time < NOW()
  |
  v
自动取消订单 + 释放锁定库存 (locked - 1, available + 1)
  |
  v
Redis 库存回补 (INCR)
```
