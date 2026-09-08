# 接口说明

版本 V1.1。后台业务接口前缀 `/api/v1`，真实设备接入接口前缀 `/api/device/v1`，JSON UTF-8。以下以实际 Controller 与 Service 为准。

## 认证和响应

1. `GET /auth/csrf`，保存 `data.token` 与 `data.headerName`，同时保留 Cookie。
2. `POST /auth/login`，提交 `username`、`password`，带上步骤 1 的 CSRF 头。
3. 登录会轮换会话，重新 `GET /auth/csrf`，后续写入使用新 token。
4. `GET /auth/me` 查询当前用户；`POST /auth/logout` 带 CSRF 注销。

成功：

```json
{"code":"OK","message":"成功","data":{"records":[],"total":"0","page":1,"size":20}}
```

Long 类型（主键和部分计数）输出为字符串。时间输出带 `+08:00` 偏移。分页从 1 开始，默认 20 条，最大 100 条。

错误状态：400 参数错误，401 未登录或认证已失效，403 权限或 CSRF 不通过，404 不存在，405 方法不支持，409 重复编号、过期版本或业务状态冲突，500 服务异常。业务异常处理器响应包含 `requestId`；Security Filter 的 401/403 响应目前不含此字段。

## 权限

除获取 CSRF 与登录外，后台业务查询要求认证。ADMIN 与 OPERATOR 可写业务，ANALYST 只读。`/users`、`/demo`、设备密钥签发和老人误录删除仅 ADMIN 可访问。后台写入要求 CSRF，不能通过隐藏按钮替代后端鉴权。

`/api/device/v1/**` 不使用浏览器 Session 或 CSRF。设备在 `X-Device-Key` 请求头携带独立密钥，服务端仅保存 SHA-256 摘要。密钥由管理员在腕表分布页面签发或轮换，只显示一次。生产传输必须使用 HTTPS。

## 端点目录

| 方法 | 路径 | 内容 |
| --- | --- | --- |
| GET | /regions | 地区列表 |
| GET POST | /elders | 老人分页、新增 |
| GET PUT | /elders/{id} | 老人详情、更新 |
| POST | /elders/{id}/archive | 归档并关闭绑定、监测，取消待随访 |
| DELETE | /elders/{id} | 仅管理员删除无任何业务关系的误录档案 |
| GET POST | /doctors | 医生分页、新增 |
| PUT | /doctors/{id} | 医生更新 |
| GET POST | /health-records | 检测记录分页、录入 |
| GET | /elders/{id}/health-trend | 个体指标趋势，最多 5000 条 |
| GET POST | /followup-plans | 随访计划分页、新增 |
| POST | /followup-plans/{id}/complete | 填写内容并完成计划 |
| POST | /followup-plans/{id}/cancel | 取消待完成计划 |
| GET | /statistics/population | 总人数、年龄、性别、地区分布 |
| GET | /statistics/health | 检测次数、人数、异常、日趋势 |
| GET | /statistics/followups | 计划、完成、逾期及医生统计 |
| GET | /dashboard | 首页汇总 |
| GET POST | /devices | 设备分页、新增 |
| PUT | /devices/{id} | 设备更新 |
| POST | /devices/{id}/bind | 绑定老人 |
| POST | /devices/{id}/unbind | 结束有效绑定 |
| POST | /devices/{id}/credential | 管理员签发或轮换设备接入密钥，仅显示一次 |
| GET | /devices/distribution | 设备、绑定人、状态及最后定位 |
| GET POST | /geofences | 围栏列表、新增 |
| PUT | /geofences/{id} | 更新围栏并重置监测 |
| PUT | /geofences/{id}/members | 替换关联老人集合 |
| GET | /geofence-alerts | 越界事件分页 |
| POST | /geofence-alerts/{id}/handle | 人工处理事件 |
| GET | /elders/{id}/trajectory | 24 小时内、最多 5000 点的历史轨迹 |
| GET POST | /users | 账号分页、新增 |
| PUT | /users/{id} | 更新显示名、角色及启用状态 |
| POST | /users/{id}/reset-password | 重置密码并使旧认证失效 |
| POST | /demo/locations | 单条模拟定位，仅 demo profile |
| POST | /demo/locations/batch | 1～500 条模拟定位，仅 demo profile |
| POST | /demo/heartbeats | 单条模拟心跳，仅 demo profile |
| POST | /api/device/v1/locations | 真实设备批量定位，使用 X-Device-Key |
| POST | /api/device/v1/heartbeats | 真实设备心跳，使用 X-Device-Key |

## 查询参数

| 端点 | 可选筛选（除另行说明） |
| --- | --- |
| /elders | regionId、keyword、status、page、size |
| /doctors /devices /users | page、size |
| /health-records | regionId、elderId、start、end、abnormal、page、size |
| /health-trend | **必需** metric、start、end；路径指定老人 |
| /followup-plans | regionId、elderId、doctorId、start、end、status、page、size |
| /statistics/population | regionId |
| /statistics/health /dashboard | regionId、start、end |
| /statistics/followups | regionId、doctorId、start、end |
| /devices/distribution | regionId、status |
| /geofence-alerts | elderId、handled、start、end、page、size |
| /trajectory | **必需** start、end；路径指定老人 |

时间使用 ISO 偏移格式，如 `2026-09-07T00:00:00+08:00`，URL 中 `+` 需编码为 `%2B`。区间统一左闭右开 `[start,end)`。健康、随访、告警无日期时默认近 30 天。轨迹范围不可超过 24 小时。

## 写入字段

| 资源 | 字段 |
| --- | --- |
| 老人 | code、name、gender（MALE/FEMALE/UNKNOWN）、regionId；可选 birthDate、phone；更新带 version |
| 医生 | code、name、department、enabled；可选 phone；更新带 version |
| 健康 | elderId、measuredAt；systolic/diastolic/heartRate/oxygen/temperature 至少一项；可选 eventId |
| 随访新增 | elderId、doctorId、dueAt |
| 随访完成 | completedAt、content、result |
| 随访取消 | reason |
| 设备 | serialNo、model、enabled；更新带 version |
| 绑定 | elderId；解绑无需请求体 |
| 围栏 | name、centerLon、centerLat、radiusM、enabled；更新带 version |
| 围栏成员 | elderIds 数组，最多 1000 项 |
| 事件处理 | note、version |
| 账号新增 | username、displayName、role、enabled、password |
| 账号更新 | displayName、role、enabled；username 不可改，password 走单独重置接口 |
| 密码重置 | password，至少 12 字符、不超过 72 UTF-8 字节 |
| 模拟定位 | deviceId、eventId、longitude、latitude、recordedAt |
| 批量定位 | deviceId、points；每个点含 eventId、longitude、latitude、recordedAt，最多 500 点 |
| 模拟心跳 | deviceId、eventId、recordedAt |

未列出的输入字段将被拒绝。新增和更新都提交完整业务字段；更新不是 PATCH。出生日期和电话可以置空。编辑界面原样提交脱敏电话时，后端保留原号码。

## 示例

新增老人：

```json
{"code":"DEMO-E-NEW","name":"演示老人新增","gender":"UNKNOWN","regionId":"1","birthDate":"1952-05-10","phone":"13800000000"}
```

录入健康：

```json
{"elderId":"1","measuredAt":"2026-09-07T09:00:00+08:00","heartRate":75,"oxygen":98,"eventId":"demo-health-unique-001"}
```

圆形围栏：

```json
{"name":"演示活动范围","centerLon":120.16,"centerLat":30.25,"radiusM":500,"enabled":true}
```

关联人员：

```json
{"elderIds":["1","2"]}
```

模拟定位（设备必须在该时刻有有效绑定，日期按实际环境调整）：

```json
{"deviceId":"1","eventId":"demo-point-unique-001","longitude":120.17,"latitude":30.25,"recordedAt":"2026-09-07T09:00:00+08:00"}
```

设备批量定位：

```http
POST /api/device/v1/locations
Content-Type: application/json
X-Device-Key: scd_签发后仅显示一次的密钥
```

```json
{
  "deviceId":"1",
  "points":[
    {"eventId":"gps-20260908-0001","longitude":120.1601,"latitude":30.2501,"recordedAt":"2026-09-08T09:00:00+08:00"},
    {"eventId":"gps-20260908-0002","longitude":120.1602,"latitude":30.2502,"recordedAt":"2026-09-08T09:01:00+08:00"}
  ]
}
```

成功结果包含 `received`、`accepted`、`duplicates`、`firstRecordedAt` 和 `lastRecordedAt`。批次中任一点不合法时整个批次回滚。

## 状态和重试语义

- 老人 ACTIVE 可维护，ARCHIVED 保留历史且不能编辑。
- 随访 PENDING 可完成或取消，COMPLETED/CANCELED 不再重复操作；计划结果唯一。
- 围栏半径 50～5000 米，球面距离大于半径为外部，边界在内。连续在外只一条事件，返回后再离开新建事件。
- 相同设备与 eventId 的相同定位重传返回原记录；改内容返回 409。历史点按发生时绑定归属，不污染新绑定人。
- 心跳只前进不回退；相同设备和 eventId 的相同心跳可安全重传，不同内容返回 409。
- 老人档案只有在健康、随访、设备绑定、定位及围栏关系均为零时才可物理删除；已有历史只能归档。
- 设备密钥轮换后旧密钥立即失效。定位和心跳以设备内唯一 eventId 实现持久化幂等。
- 人工处理和实际返回分别记录。没有返回证据时，处理不能伪造 returnedAt。
- 409 要刷新状态再操作，不自动覆盖版本。普通新增没有统一的幂等请求中间件，网络超时后先查业务编码或事件标识再重试。
