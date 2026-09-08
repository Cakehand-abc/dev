# 智慧医养大数据决策分析系统

面向医养服务管理人员的课程项目。包含登录、首页、人口统计、健康检测统计、医生随访统计、腕表分布、电子围栏、轨迹回放和管理员数据模拟，并提供老人、医生、设备、账号的配套维护及真实设备 GPS 接入接口。

技术采用 **Spring Boot 3.5.16、Spring MVC、Spring 事务、MyBatis、MyBatis-Plus 3.5.17、Vue 3、Element Plus 2.14.5、MySQL 8.0、Maven Wrapper 3.9.16**。Java 编译目标为 17。前端数据模拟页实际使用 Element Plus，当前统计图继续使用 ECharts 6，但 ECharts 不是项目必选约束。后端 CRUD 使用 BaseMapper，聚合统计使用 Mapper XML。测试为 Boot BOM 管理的 JUnit 5，未引入 Spring Boot 4 或 JUnit 6。

## 文件入口

- [文档交付索引](docs/README.md)：10 份 Word、1 份答辩 PPT。
- [接口说明](docs/API.md)：权限、参数、响应与请求示例。
- [数据库脚本](database/01-schema.sql)：18 张表与完整性约束；已有 V1.0 库使用 [接入升级脚本](database/03-device-ingest-upgrade.sql)。
- [腕表 GPS 接入指南](docs/DEVICE-INGEST.md)：设备密钥、批量定位、心跳与重试约定。
- [部署说明源稿](docs/source/11-项目部署环境说明书.md)：环境、构建、启动与故障处理。
- [实际测试证据](docs/evidence)：JUnit、HTTP、浏览器与性能记录。
- [继续工作进度](PROGRESS.md)：已完成内容和尚待团队确认事项。

## 本地运行

先准备 JDK 17、MySQL 8.0.16 以上的 8.0 系列，以及 Node 22.12 以上的 22 系列。本次机器使用 Zulu 25.0.1 以 `release 17` 编译并运行通过；**不等同 JDK 17 或 IDEA 内置测试运行器已实测**。

1. 在专用空数据库环境执行 `database/00-create-database.sql` 和 `database/01-schema.sql`。建表使用管理账号，应用账号授予该库的 SELECT、INSERT、UPDATE、DELETE 权限。数据库业务时区设为 `+08:00`。
2. 在项目根目录的 PowerShell 设置环境变量。将示例值换成自己的密码，不将私有值提交 Git：

```powershell
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/smart_care?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Shanghai'
$env:DB_USERNAME='smart_care'
$env:DB_PASSWORD='填写自己的数据库密码'
$env:APP_ADMIN_PASSWORD='填写至少12字符的初始管理员密码'
```

3. 构建并启动：

```powershell
.\scripts\build.ps1
.\scripts\start.ps1 -Profile demo -Port 8080
```

`build.ps1` 默认执行 `npm ci`、Vue 生产构建、静态资源复制和 Maven 打包。跳过测试必须显式使用 `-SkipTests`。如已安装依赖且 Windows 上运行中的 Vite 锁定 `esbuild.exe`，可先停止该 Vite 进程，或确认 `package-lock.json` 未变化后使用 `-SkipNpmInstall`。

**后端端口不是固定的 8080**：应用读取环境变量 `SERVER_PORT`（缺省 8080），`start.ps1` 的 `-Port` 参数设置的就是它。8080 被本机其他服务占用时，改传任意空闲端口即可（如 `-Port 8084`），浏览器打开对应的 `http://127.0.0.1:<端口>`。

浏览器打开 `http://127.0.0.1:8080`。首次创建的管理员名为 `admin`，密码为自己设置的 `APP_ADMIN_PASSWORD`。demo 首次空库生成 `operator` 和 `analyst` 演示账号，初始密码相同；正式使用应分别修改。前台按 Ctrl+C 停止。

也可将四个私有配置项存为 JSON，使用 `-EnvironmentFile 私有文件路径`。`.env.example` 只是说明，应用不会自动加载 `.env`。已有账号库不会因环境变量变化而重置密码。

**Windows 再次构建前先停止正在运行的同一 JAR**，否则 Maven repackage 可能因文件锁无法重命名。不要批量结束其他 Java 进程。

本次开发演示使用应用端口 **18080**、隔离 MySQL 端口 **13316**，不会占用用户原有的 8080 服务。本机私有入口保存在 `.local/demo-access.txt`；临时演示库不是持久部署方案。

## 模拟数据与地图

推荐通过 demo profile 自动生成相对当前时间的数据：120 位虚构老人、6 位医生、1080 条健康记录、120 条随访计划、18 个设备、648 个定位点、1 个圆形围栏。只有老人表为空时才生成，不覆盖已有数据。

`database/02-demo-data.sql` 提供固定日期的独立 SQL 种子，与 Java DemoSeeder **二选一**。只在全新空业务库导入；SQL 不含账号密码，后续普通启动由 Bootstrap 创建管理员，其他角色可由管理界面创建。种子日期靠近 2026-09-07，查询时选择相应日期。

地图为 WGS84 离线坐标示意图，没有道路底图。设备在线由最近 5 分钟心跳判断，演示数据经过一段时间转为离线是预期行为。健康阈值仅用于教学演示。

管理员登录 demo 环境后可使用“数据模拟”页面，手动注入单点，或生成直线、环形、越界返回轨迹；单批最多 500 点并可同时发送心跳。真实设备先由管理员在“腕表分布”签发独立接入密钥，再按 [接入指南](docs/DEVICE-INGEST.md) 上报。

## 开发与验证

后端独立运行：

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

`spring-boot:run` 不经过 `start.ps1`，按 `SERVER_PORT` 的缺省值 8080 启动；该端口被占用时先设置再运行，例如 `$env:SERVER_PORT='8084'`。

前端独立运行：

```powershell
cd frontend
npm ci
npm run dev
```

Vite 的 `/api` 代理指向后端 8080；改变后端端口时同步修改 `frontend/vite.config.js`。打包 JAR 后页面和 API 同源，无需启动 Vite。

JUnit 使用 H2 测试库，可不配置 MySQL 凭据。IDEA 将 Project SDK 与 Maven Runner JRE 设为 JDK 17，重新加载 Maven 后右键运行 `BusinessTests` 和 `DevApplicationTests`，记录完整 IDEA 版本及实际结果。

HTTP 冒烟脚本使用 Python 标准库：

```powershell
python scripts/smoke_http.py --base-url http://127.0.0.1:18080 --env-file .local/demo-env.json
```

`browser-qa.mjs` 是开发验证脚本，需 Playwright、Chrome 和指向其包目录的 `RUNTIME_NODE_MODULES`。它们不是运行 JAR 的依赖。`performance_test.py` 和 `export_demo_sql.py` 明确绑定本任务隔离环境，不直接用于其他数据库。

当前 Maven 回归共 28 项通过；V1.1 隔离 MySQL 验证了全新 18 表、16 表升级到 18 表、16 项基础 HTTP、5 项新增写路径和 4 项 Chrome 页面检查。原 V1.0 还完成双十万数据量下20并发约60秒、2664请求无错误、四端点 P95 均低于2秒。

## 角色与范围

ADMIN 管理全部业务与账号；OPERATOR 维护业务；ANALYST 只读查询。后端执行权限检查，写接口使用 CSRF，密码使用 BCrypt。

项目组四人为 A 项目经理兼测试、B 后端、C 后端兼数据、D 前端，姓名待填写。第一版不设独立 Agent 岗位。短信、外部地图、多租户、临床诊断与公开部署不属于本次课程版范围；真实腕表已有通用 HTTP 接口，具体厂商协议适配仍需联调。

## 目录

```text
src/main/java/com/johnnylin/dev/  后端认证、业务服务、控制器、实体与 Mapper
src/main/resources/mapper/       MyBatis 统计 SQL
src/test/                       JUnit 与 H2 测试
frontend/                       Vue 源代码与 npm 锁文件
database/                       建库、建表、可选演示 SQL
scripts/                        构建、启动、验证及文档生成
docs/deliverables/              最终 Word 与 PPT
docs/source/                   可维护的文档正文源稿
docs/evidence/                 实际验证记录
```
