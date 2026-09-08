# 腕表 GPS 接入指南

## 接入流程

1. 管理员在“腕表分布”注册并启用腕表，完成老人绑定。
2. 点击该设备的“接入密钥”，确认后立即安全保存一次性显示的密钥。
3. 设备或设备网关通过 HTTPS 调用 `/api/device/v1/locations` 和 `/api/device/v1/heartbeats`。
4. 每次请求在 `X-Device-Key` 头传密钥，请求体的 `deviceId` 必须与密钥所属设备一致。
5. 每个事件使用设备内永久唯一的 `eventId`。网络超时可以原样重传，服务端不会重复写入。

## 定位上报

单批允许 1～500 点，使用 WGS84 经纬度。`recordedAt` 是设备采集时间，必须包含时区，且不能超过服务器允许的未来时钟偏差。

```bash
curl -X POST "https://example.com/api/device/v1/locations" \
  -H "Content-Type: application/json" \
  -H "X-Device-Key: scd_xxx" \
  -d '{"deviceId":1,"points":[{"eventId":"watch1-000001","longitude":120.1601,"latitude":30.2501,"recordedAt":"2026-09-08T09:00:00+08:00"}]}'
```

设备在定位发生时必须处于有效绑定区间。历史定位可以乱序补传；它会归属到采集时刻对应的绑定人，但不会回退当前围栏状态。

## 心跳上报

```bash
curl -X POST "https://example.com/api/device/v1/heartbeats" \
  -H "Content-Type: application/json" \
  -H "X-Device-Key: scd_xxx" \
  -d '{"deviceId":1,"eventId":"watch1-heartbeat-000001","recordedAt":"2026-09-08T09:00:00+08:00"}'
```

心跳只更新为更晚的设备时间。重复事件返回成功，不一致的同编号事件返回 409。

## 错误处理

| HTTP | 含义 | 设备侧处理 |
| --- | --- | --- |
| 400 | 字段、坐标、时间或批量大小不合法 | 修正数据，不要原样重试 |
| 401 | 密钥无效、设备停用或密钥与 deviceId 不匹配 | 停止上报并申请新密钥 |
| 409 | 绑定状态冲突或相同 eventId 内容不一致 | 查询设备配置或生成新事件编号 |
| 500 | 服务暂时异常 | 保留原 eventId，指数退避后原样重试 |

## 数据库升级

全新数据库直接使用 `database/01-schema.sql`。已有 V1.0 数据库先备份，再执行一次 `database/03-device-ingest-upgrade.sql`，新增设备凭据和心跳事件表。
