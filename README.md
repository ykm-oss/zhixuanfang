## 智选坊 

## 项目简介
基于 Spring Boot + Redis + MySQL 开发的电商秒杀系统，支持用户认证、商品管理、购物车、订单、秒杀、商品推荐等核心功能。

## 技术栈
- Spring Boot 2.7.x
- MyBatis-Plus
- MySQL 8.0
- Redis
- JWT
- Knife4j

## 核心功能
- 用户注册/登录（JWT认证）
- 商品管理（列表、详情）
- 购物车（增删改查）
- 订单管理（创建、支付、取消、超时关闭）
- 秒杀功能（Redis预减库存 + Lua脚本）
- 商品推荐（协同过滤算法）

## 项目亮点
- **缓存优化**：Redis缓存商品详情，解决缓存穿透/击穿/雪崩
- **防重复提交**：基于Redis分布式锁，99%重复请求被拦截
- **秒杀系统**：Redis预减库存 + Lua脚本原子操作，QPS提升显著
- **智能推荐**：协同过滤算法实现个性化商品推荐

## 快速启动

### 环境要求
- JDK 1.8+
- MySQL 8.0+
- Redis

### 1. 克隆项目
```bash
git clone https://github.com/ykm-oss/zxf.git