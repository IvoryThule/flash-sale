# JMeter 压测使用指南

## 一、测试目标

| 实验 | 目的 |
|------|------|
| 负载均衡验证 | 确认 Nginx 将请求均匀分发到 app1 / app2 |
| 静态文件压测 | 观察 Nginx 直接提供静态资源的吞吐量 |
| 动态接口压测 | 观察后端集群的响应时间和吞吐量 |
| Redis 缓存对比 | 有缓存 vs. 无缓存的商品详情接口性能对比 |

---

## 二、环境准备

```bash
# 1. 构建后端镜像
cd flash-sale/backend
docker build -t flash-sale-api:latest .

# 2. 启动全部容器（Nginx 80 / app1 8081 / app2 8082 / mysql / redis）
cd ../deploy
docker compose up -d

# 3. 检查各容器状态
docker compose ps

# 4. 验证 Nginx 健康
curl http://localhost/nginx-health
# 期望返回：{"status":"ok","server":"nginx"}

# 5. 验证后端健康
curl http://localhost/api/health
```

---

## 三、JMeter 测试计划

### 3.1 测试计划一：负载均衡验证（轮询算法）

**目标：** 100 并发，连续请求，观察 app1/app2 处理比例接近 1:1

| 参数 | 值 |
|------|----|
| 线程数 | 100 |
| 每线程循环次数 | 50 |
| Ramp-Up | 10s |
| 请求 URL | `http://localhost/api/product/list` |

**配置步骤：**
1. 新建测试计划 → 添加线程组（Thread Group）
2. 线程数：100，Ramp-Up：10，循环：50
3. 添加 HTTP Request：
   - Server: `localhost`，Port: `80`
   - Method: `GET`，Path: `/api/product/list`
4. 添加响应断言（Response Assertion）：`$.code == 200`
5. 添加聚合报告（Aggregate Report）
6. 添加 **响应头提取器（Regular Expression Extractor）**，提取 `X-Upstream-Addr` 响应头

**结果验证：**
```bash
# 测试结束后统计 Nginx 日志中各节点请求数
docker exec flash-sale-nginx \
  awk '{print $NF}' /var/log/nginx/access.log | \
  grep -oP 'upstream=\K[^\s]+' | sort | uniq -c
# 期望 app1:8080 和 app2:8080 数量大致相等（误差 < 5%）
```

---

### 3.2 测试计划二：切换负载均衡算法

修改 `deploy/nginx/conf.d/flash-sale.conf` 中 `proxy_pass` 行：

```nginx
# 轮询（默认）
proxy_pass http://backend_roundrobin;

# 加权轮询（app1 权重 3：app2 权重 1，比例约 3:1）
proxy_pass http://backend_weighted;

# IP Hash（同一 IP 固定后端节点）
proxy_pass http://backend_iphash;

# 最少连接
proxy_pass http://backend_leastconn;
```

```bash
# 修改后热加载 Nginx（无需重启）
docker exec flash-sale-nginx nginx -s reload
```

重新执行测试计划一，对比各算法的请求分布差异。

---

### 3.3 测试计划三：动静分离 - 静态文件 vs. 动态接口

| 测试项 | URL | 说明 |
|--------|-----|------|
| 静态 HTML | `http://localhost/index.html` | Nginx 直接读文件，不走后端 |
| 静态 CSS | `http://localhost/style.css` | 同上 |
| 动态接口 | `http://localhost/api/product/list` | Nginx 反代 → Spring Boot |

**JMeter 配置（对比测试）：**
1. 创建 **Thread Group A** → 200 线程 → 请求 `/index.html`
2. 创建 **Thread Group B** → 200 线程 → 请求 `/api/product/list`
3. 两组共享同一个**聚合报告**（Aggregate Report）
4. 对比 Average RT / Throughput（TPS）

**预期结果：**
- 静态文件：平均响应时间 < 5 ms，TPS > 5000
- 动态接口：平均响应时间 10–50 ms（受 DB 查询影响），TPS 受后端限制

---

### 3.4 测试计划四：Redis 缓存效果验证

**第一轮（冷缓存）：** 重启 Redis 清空缓存后立即压测
```bash
docker exec flash-sale-redis redis-cli FLUSHDB
```

**第二轮（热缓存）：** 第一轮结束后立即再次压测

| 参数 | 值 |
|------|----|
| 线程数 | 200 |
| 循环 | 20 |
| URL | `http://localhost/api/product/1` |

**观察指标：**
- 第一轮（冷）：请求进入 DB，日志显示 `[Cache] 缓存未命中`，RT 较高
- 第二轮（热）：请求命中 Redis，日志显示 `[Cache] 缓存命中`，RT 显著下降

```bash
# 实时查看缓存命中情况
docker logs -f flash-sale-app1 | grep "\[Cache\]"
```

---

## 四、关键指标观察

### 负载均衡请求分布
```bash
# 统计 Nginx 日志中各后端节点处理的请求数
docker exec flash-sale-nginx \
  grep -oP 'upstream=\K[0-9.:]+' /var/log/nginx/access.log | \
  sort | uniq -c | sort -rn
```

### Redis 缓存统计
```bash
docker exec flash-sale-redis redis-cli INFO stats | grep -E "hit|miss"
# keyspace_hits   → 缓存命中总次数
# keyspace_misses → 缓存未命中总次数
# 命中率 = hits / (hits + misses)
```

### 读写分离验证
```bash
# 查看后端日志中的数据源路由
docker logs flash-sale-app1 | grep "\[DataSource\]"
# 读操作应显示 → 从库
# 写操作应显示 → 主库
```

---

## 五、切换负载均衡算法快速参考

```bash
# 1. 修改配置文件中的 proxy_pass
# deploy/nginx/conf.d/flash-sale.conf → location /api/ → proxy_pass 行

# 2. 热加载
docker exec flash-sale-nginx nginx -t && \
docker exec flash-sale-nginx nginx -s reload

# 3. 重新运行 JMeter 测试计划，对比结果
```

---

## 六、容器服务端口一览

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | `80` | 统一入口（负载均衡 + 动静分离） |
| app1 | `8081` | 后端实例 1（直接访问，绕过 Nginx） |
| app2 | `8082` | 后端实例 2（直接访问，绕过 Nginx） |
| MySQL 主库 | `3307` | 写操作 |
| MySQL 从库 | `3308` | 读操作 |
| Redis | `6379` | 缓存 |
