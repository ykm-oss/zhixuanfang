-- ============================================
-- 智选坊 - 数据库初始化脚本
-- 数据库名：ds
-- ============================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
                                      `id` bigint NOT NULL AUTO_INCREMENT,
                                      `username` varchar(20) NOT NULL,
    `password` varchar(64) NOT NULL,
    `phone` varchar(11) DEFAULT NULL,
    `avatar` varchar(255) DEFAULT NULL,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `username_unique` (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 商品表
CREATE TABLE IF NOT EXISTS `product` (
                                         `id` bigint NOT NULL AUTO_INCREMENT,
                                         `name` varchar(50) NOT NULL,
    `price` decimal(10,2) NOT NULL,
    `stock` int NOT NULL DEFAULT '0',
    `pic` varchar(255) DEFAULT NULL,
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '1上架 0下架',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 购物车表
CREATE TABLE IF NOT EXISTS `cart` (
                                      `id` bigint NOT NULL AUTO_INCREMENT,
                                      `user_id` bigint NOT NULL,
                                      `product_id` bigint NOT NULL,
                                      `num` int NOT NULL DEFAULT '1',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (`id`),
    UNIQUE KEY `user_product_unique` (`user_id`,`product_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 订单主表
CREATE TABLE IF NOT EXISTS `order_info` (
                                            `id` bigint NOT NULL AUTO_INCREMENT,
                                            `order_no` varchar(32) NOT NULL,
    `user_id` bigint NOT NULL,
    `total_price` decimal(10,2) NOT NULL,
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待支付 1已支付 2已取消 3已关闭',
    `pay_time` datetime DEFAULT NULL,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `order_no_unique` (`order_no`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 订单明细表
CREATE TABLE IF NOT EXISTS `order_item` (
                                            `id` bigint NOT NULL AUTO_INCREMENT,
                                            `order_id` bigint NOT NULL,
                                            `product_id` bigint NOT NULL,
                                            `product_name` varchar(50) NOT NULL,
    `product_price` decimal(10,2) NOT NULL,
    `num` int NOT NULL,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 秒杀商品表
CREATE TABLE IF NOT EXISTS `seckill_product` (
                                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                                 `product_id` bigint NOT NULL,
                                                 `seckill_price` decimal(10,2) NOT NULL,
    `stock` int NOT NULL,
    `start_time` datetime NOT NULL,
    `end_time` datetime NOT NULL,
    `status` tinyint DEFAULT '1',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 秒杀订单表
CREATE TABLE IF NOT EXISTS `seckill_order` (
                                               `id` bigint NOT NULL AUTO_INCREMENT,
                                               `order_no` varchar(32) NOT NULL,
    `user_id` bigint NOT NULL,
    `product_id` bigint NOT NULL,
    `seckill_price` decimal(10,2) NOT NULL,
    `num` int DEFAULT '1',
    `status` tinyint DEFAULT '0',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `pay_time` datetime DEFAULT NULL,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 用户行为表（推荐系统）
CREATE TABLE IF NOT EXISTS `user_behavior` (
                                               `id` bigint NOT NULL AUTO_INCREMENT,
                                               `user_id` bigint NOT NULL,
                                               `product_id` bigint NOT NULL,
                                               `behavior_type` tinyint NOT NULL COMMENT '1浏览 2收藏 3购买',
                                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                               PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`product_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 1. 用户表测试数据
-- ============================================
-- 密码都是 123456 的 MD5 加密值：e10adc3949ba59abbe56e057f20f883e
INSERT INTO `user` (id, username, password, phone, avatar, create_time, update_time) VALUES
                                                                                         (1, 'test001', 'e10adc3949ba59abbe56e057f20f883e', '13800138000', NULL, NOW(), NOW()),
                                                                                         (2, 'test002', 'e10adc3949ba59abbe56e057f20f883e', '13900139000', NULL, NOW(), NOW()),
                                                                                         (3, 'root', 'e10adc3949ba59abbe56e057f20f883e', '18864676136', NULL, NOW(), NOW());

-- ============================================
-- 2. 商品表测试数据
-- ============================================
INSERT INTO `product` (id, name, price, stock, pic, status, create_time) VALUES
                                                                             (1, 'Apple iPhone 15 Pro', 7999.00, 100, NULL, 1, NOW()),
                                                                             (2, '华为 Mate 60 Pro', 6999.00, 50, NULL, 1, NOW()),
                                                                             (3, '小米 14 Ultra', 6499.00, 80, NULL, 1, NOW()),
                                                                             (4, 'OPPO Find X7', 4999.00, 60, NULL, 1, NOW()),
                                                                             (5, 'vivo X100 Pro', 5499.00, 70, NULL, 1, NOW()),
                                                                             (6, '荣耀 Magic 6 Pro', 5999.00, 40, NULL, 1, NOW()),
                                                                             (7, '三星 S24 Ultra', 8999.00, 30, NULL, 1, NOW()),
                                                                             (8, '一加 12', 5299.00, 55, NULL, 1, NOW());

-- ============================================
-- 3. 秒杀商品表测试数据
-- ============================================
-- 注意：时间设置为今天到明天，确保秒杀在进行中
INSERT INTO `seckill_product` (product_id, seckill_price, stock, start_time, end_time, status) VALUES
                                                                                                   (1, 5999.00, 10, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 23 HOUR), 1),
                                                                                                   (2, 4999.00, 5, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 23 HOUR), 1),
                                                                                                   (3, 4499.00, 8, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 23 HOUR), 1);

-- ============================================
-- 4. 用户行为表测试数据（用于推荐系统）
-- ============================================
-- 用户1（test001）的购买记录
INSERT INTO `user_behavior` (user_id, product_id, behavior_type, create_time) VALUES
                                                                                  (1, 1, 3, NOW()),  -- 购买 iPhone
                                                                                  (1, 2, 3, NOW()),  -- 购买 华为
                                                                                  (1, 1, 2, NOW()),  -- 收藏 iPhone
                                                                                  (1, 3, 1, NOW());  -- 浏览 小米

-- 用户2（test002）的购买记录（与用户1相似）
INSERT INTO `user_behavior` (user_id, product_id, behavior_type, create_time) VALUES
                                                                                  (2, 1, 3, NOW()),  -- 购买 iPhone
                                                                                  (2, 2, 3, NOW()),  -- 购买 华为
                                                                                  (2, 4, 1, NOW());  -- 浏览 OPPO

-- 用户3（root）的购买记录
INSERT INTO `user_behavior` (user_id, product_id, behavior_type, create_time) VALUES
                                                                                  (3, 3, 3, NOW()),  -- 购买 小米
                                                                                  (3, 5, 3, NOW()),  -- 购买 vivo
                                                                                  (3, 1, 1, NOW());  -- 浏览 iPhone

-- ============================================
-- 5. 验证数据插入是否成功
-- ============================================
SELECT 'user' as table_name, COUNT(*) as count FROM user
UNION ALL
SELECT 'product', COUNT(*) FROM product
UNION ALL
SELECT 'seckill_product', COUNT(*) FROM seckill_product
UNION ALL
SELECT 'user_behavior', COUNT(*) FROM user_behavior; Pro', 5499.00, 70, 1);