-- ============================================================
-- Flash Sale System - Database Initialization Script
-- ============================================================
-- 创建数据库
-- ============================================================
CREATE DATABASE IF NOT EXISTS `flash_sale`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE `flash_sale`;
-- ============================================================
-- 1. 用户表 (users)
-- ============================================================
-- 存储注册用户基本信息，用于登录鉴权和订单归属。
-- ============================================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键自增',
    `username`    VARCHAR(64)  NOT NULL                COMMENT '用户名，唯一，用于登录',
    `password`    VARCHAR(128) NOT NULL                COMMENT '密码，BCrypt 加密存储',
    `nickname`    VARCHAR(64)  DEFAULT ''              COMMENT '昵称，前端展示',
    `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号，唯一',
    `email`       VARCHAR(128) DEFAULT ''              COMMENT '邮箱地址',
    `avatar`      VARCHAR(256) DEFAULT ''              COMMENT '头像 URL',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0-禁用, 1-正常',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
-- ============================================================
-- 2. 用户收货地址表 (user_address)
-- ============================================================
-- 存储用户收货地址，支持多地址、默认地址。
-- ============================================================
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id`      BIGINT       NOT NULL                COMMENT '用户ID',
    `receiver`     VARCHAR(64)  NOT NULL                COMMENT '收货人姓名',
    `phone`        VARCHAR(20)  NOT NULL                COMMENT '收货人手机号',
    `province`     VARCHAR(32)  NOT NULL DEFAULT ''     COMMENT '省',
    `city`         VARCHAR(32)  NOT NULL DEFAULT ''     COMMENT '市',
    `district`     VARCHAR(32)  NOT NULL DEFAULT ''     COMMENT '区/县',
    `detail`       VARCHAR(256) NOT NULL DEFAULT ''     COMMENT '详细地址',
    `is_default`   TINYINT      NOT NULL DEFAULT 0      COMMENT '是否默认地址: 0-否, 1-是',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址表';
-- ============================================================
-- 3. 商品表 (products)
-- ============================================================
-- 存储商品基本信息。秒杀价格和时间转移到 seckill_events 表，
-- 支持一个商品多次参与不同秒杀活动。
-- ============================================================
DROP TABLE IF EXISTS `products`;
CREATE TABLE `products` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID，主键自增',
    `name`          VARCHAR(128)  NOT NULL                COMMENT '商品名称',
    `description`   TEXT                                  COMMENT '商品描述',
    `image`         VARCHAR(512)  DEFAULT ''              COMMENT '商品主图 URL',
    `price`         DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '商品原价（元）',
    `category`      VARCHAR(64)   DEFAULT ''              COMMENT '商品分类',
    `status`        TINYINT       NOT NULL DEFAULT 0      COMMENT '状态: 0-未上架, 1-已上架, 2-已下架',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';
-- ============================================================
-- 4. 秒杀活动表 (seckill_events)
-- ============================================================
-- 独立秒杀活动表：
--   - 一个商品可多次参与秒杀（3.15活动、6.18活动、双11活动...）
--   - 每次活动有独立的秒杀价格、时间、库存、限购数
--   - stock 表通过 event_id 关联，实现每场活动独立库存
-- ============================================================
DROP TABLE IF EXISTS `seckill_events`;
CREATE TABLE `seckill_events` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '秒杀活动ID',
    `title`           VARCHAR(128)  NOT NULL DEFAULT ''     COMMENT '活动标题（如：3.15大促）',
    `product_id`      BIGINT        NOT NULL                COMMENT '关联商品ID',
    `seckill_price`   DECIMAL(10,2) NOT NULL                COMMENT '秒杀价格（元）',
    `start_time`      DATETIME      NOT NULL                COMMENT '秒杀开始时间',
    `end_time`        DATETIME      NOT NULL                COMMENT '秒杀结束时间',
    `total_stock`     INT           NOT NULL DEFAULT 0      COMMENT '活动总库存',
    `available_stock` INT           NOT NULL DEFAULT 0      COMMENT '活动可用库存',
    `limit_per_user`  INT           NOT NULL DEFAULT 1      COMMENT '每人限购数量',
    `status`          TINYINT       NOT NULL DEFAULT 0      COMMENT '状态: 0-未开始, 1-进行中, 2-已结束, 3-已取消',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`),
    CONSTRAINT `chk_stock` CHECK (`available_stock` >= 0),
    CONSTRAINT `chk_time`  CHECK (`end_time` > `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动表';
-- ============================================================
-- 5. 库存表 (stock)
-- ============================================================
-- 独立库存表：
--   - 通过 event_id 关联秒杀活动，每场活动独立库存
--   - version 乐观锁防超卖
--   - CHECK 约束防止负库存
-- ============================================================
DROP TABLE IF EXISTS `stock`;
CREATE TABLE `stock` (
    `id`              BIGINT  NOT NULL AUTO_INCREMENT COMMENT '库存记录ID',
    `product_id`      BIGINT  NOT NULL                COMMENT '关联商品ID',
    `event_id`        BIGINT  NOT NULL                COMMENT '关联秒杀活动ID',
    `total`           INT     NOT NULL DEFAULT 0      COMMENT '总库存量',
    `available`       INT     NOT NULL DEFAULT 0      COMMENT '可用库存（当前剩余）',
    `locked`          INT     NOT NULL DEFAULT 0      COMMENT '已锁定库存（下单未支付）',
    `sold`            INT     NOT NULL DEFAULT 0      COMMENT '已售出数量',
    `version`         INT     NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_product_id` (`product_id`),
    CONSTRAINT `chk_available` CHECK (`available` >= 0),
    CONSTRAINT `chk_locked`    CHECK (`locked` >= 0),
    CONSTRAINT `chk_sold`      CHECK (`sold` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表';
-- ============================================================
-- 6. 订单表 (orders)
-- ============================================================
-- 统一订单表，通过 order_type 区分普通/秒杀订单。
-- expire_time 支持支付超时自动取消并释放库存。
-- ============================================================
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`      VARCHAR(64)   NOT NULL                COMMENT '订单编号，全局唯一（雪花算法）',
    `user_id`       BIGINT        NOT NULL                COMMENT '下单用户ID',
    `product_id`    BIGINT        NOT NULL                COMMENT '商品ID',
    `event_id`      BIGINT        DEFAULT NULL            COMMENT '秒杀活动ID（普通订单为NULL）',
    `product_name`  VARCHAR(128)  NOT NULL DEFAULT ''     COMMENT '商品名称（冗余，避免联查）',
    `product_image` VARCHAR(512)  DEFAULT ''              COMMENT '商品图片（冗余快照）',
    `quantity`      INT           NOT NULL DEFAULT 1      COMMENT '购买数量',
    `unit_price`    DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '下单时单价（元）',
    `total_price`   DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '订单总价（元）',
    `order_type`    TINYINT       NOT NULL DEFAULT 1      COMMENT '订单类型: 1-普通订单, 2-秒杀订单',
    `status`        TINYINT       NOT NULL DEFAULT 0      COMMENT '订单状态: 0-待支付, 1-已支付, 2-已取消, 3-已退款, 4-已超时',
    `pay_channel`   TINYINT       DEFAULT NULL            COMMENT '支付渠道: 1-支付宝, 2-微信, 3-银行卡',
    `pay_time`      DATETIME      DEFAULT NULL            COMMENT '支付时间',
    `expire_time`   DATETIME      NOT NULL                COMMENT '订单过期时间（超时未支付自动取消）',
    `address_id`    BIGINT        DEFAULT NULL            COMMENT '收货地址ID',
    `remark`        VARCHAR(256)  DEFAULT ''              COMMENT '订单备注',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';
-- ============================================================
-- 7. 秒杀订单表 (seckill_orders)
-- ============================================================
-- 独立秒杀订单表：
--   - (user_id, event_id) 联合唯一索引 -> 同一用户同一活动只能秒杀一次
--   - 关联 orders.id 便于查询完整订单
-- ============================================================
DROP TABLE IF EXISTS `seckill_orders`;
CREATE TABLE `seckill_orders` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '秒杀订单ID',
    `user_id`     BIGINT   NOT NULL                COMMENT '秒杀用户ID',
    `product_id`  BIGINT   NOT NULL                COMMENT '秒杀商品ID',
    `event_id`    BIGINT   NOT NULL                COMMENT '秒杀活动ID',
    `order_id`    BIGINT   NOT NULL                COMMENT '关联订单ID（orders表）',
    `status`      TINYINT  NOT NULL DEFAULT 1      COMMENT '秒杀状态: 0-失败, 1-成功',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_event` (`user_id`, `event_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀订单表';
-- ============================================================
-- 8. 操作日志表 (operation_logs)
-- ============================================================
-- 记录关键操作，方便排查问题和审计。
-- ============================================================
DROP TABLE IF EXISTS `operation_logs`;
CREATE TABLE `operation_logs` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`      BIGINT       DEFAULT NULL            COMMENT '操作用户ID（系统操作为NULL）',
    `action`       VARCHAR(64)  NOT NULL                COMMENT '操作类型：REGISTER/LOGIN/SECKILL/ORDER/CANCEL 等',
    `target_type`  VARCHAR(32)  DEFAULT ''              COMMENT '目标类型：USER/PRODUCT/ORDER/STOCK 等',
    `target_id`    BIGINT       DEFAULT NULL            COMMENT '目标ID',
    `detail`       VARCHAR(512) DEFAULT ''              COMMENT '操作详情',
    `ip`           VARCHAR(64)  DEFAULT ''              COMMENT '操作IP地址',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_action` (`action`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
-- ============================================================
-- 初始化测试数据
-- ============================================================
-- 测试用户 (密码明文: 123456, BCrypt 加密)
INSERT INTO `users` (`username`, `password`, `nickname`, `phone`) VALUES
('testuser1', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBrkjghwRtSQLfziTOShga1Mnx2', '测试用户A', '13800138001'),
('testuser2', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBrkjghwRtSQLfziTOShga1Mnx2', '测试用户B', '13800138002'),
('testuser3', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBrkjghwRtSQLfziTOShga1Mnx2', '测试用户C', '13800138003');
-- 用户地址
INSERT INTO `user_address` (`user_id`, `receiver`, `phone`, `province`, `city`, `district`, `detail`, `is_default`) VALUES
(1, '张三', '13800138001', '北京市', '北京市', '海淀区', '中关村大街1号', 1),
(2, '李四', '13800138002', '上海市', '上海市', '浦东新区', '世纪大道100号', 1),
(3, '王五', '13800138003', '广东省', '深圳市', '南山区', '科技园南路1号', 1);
-- 商品数据
INSERT INTO `products` (`name`, `description`, `image`, `price`, `category`, `status`) VALUES
('iPhone 16 Pro Max', '苹果最新旗舰手机，A18 Pro 芯片，钛金属设计', '/images/iphone16.jpg', 9999.00, '手机数码', 1),
('MacBook Pro 14', 'M4 Pro 芯片，18GB 内存，512GB 存储', '/images/macbook14.jpg', 14999.00, '电脑办公', 1),
('AirPods Pro 3', '主动降噪，自适应音频，USB-C 充电盒', '/images/airpods3.jpg', 1899.00, '手机数码', 1),
('iPad Air M3', 'M3 芯片，11 英寸 Liquid Retina 屏幕', '/images/ipad-air.jpg', 5999.00, '电脑办公', 1),
('Apple Watch Ultra 3', '钛金属表壳，双频 GPS，深度计', '/images/watch-ultra3.jpg', 6499.00, '智能穿戴', 1);
-- 秒杀活动数据（同一商品可参加多场活动）
INSERT INTO `seckill_events` (`title`, `product_id`, `seckill_price`, `start_time`, `end_time`, `total_stock`, `available_stock`, `limit_per_user`, `status`) VALUES
('3.15 大促 - iPhone 秒杀',    1, 6999.00, '2026-03-15 10:00:00', '2026-03-15 12:00:00', 100, 100, 1, 1),
('3.15 大促 - MacBook 秒杀',   2, 11999.00, '2026-03-15 10:00:00', '2026-03-15 12:00:00', 50, 50, 1, 1),
('3.15 大促 - AirPods 秒杀',   3, 999.00, '2026-03-15 14:00:00', '2026-03-15 16:00:00', 200, 200, 2, 1),
('3.15 大促 - iPad 秒杀',      4, 4299.00, '2026-03-15 14:00:00', '2026-03-15 16:00:00', 80, 80, 1, 1),
('6.18 年中大促 - iPhone 秒杀', 1, 6499.00, '2026-06-18 10:00:00', '2026-06-18 12:00:00', 200, 200, 1, 0);
-- 库存数据（每场活动独立库存）
INSERT INTO `stock` (`product_id`, `event_id`, `total`, `available`, `locked`, `sold`, `version`) VALUES
(1, 1, 100, 100, 0, 0, 0),
(2, 2, 50,  50,  0, 0, 0),
(3, 3, 200, 200, 0, 0, 0),
(4, 4, 80,  80,  0, 0, 0),
(1, 5, 200, 200, 0, 0, 0);
