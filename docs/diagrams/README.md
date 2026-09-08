# 架构图源文件

这些 PlantUML 文件位于 IDEA 项目内，内容以当前代码、数据库脚本和 V1.1 接口为准。模板明确示例了用例视图、逻辑视图、包与类关系、部署视图；本项目另外增加 GPS 接入顺序图，用于说明设备认证、幂等和围栏状态变化。

在 IntelliJ IDEA 2024.3 中可安装或启用 PlantUML Integration 插件后打开 `.puml` 文件预览。没有插件时，可通过 Maven 获取 PlantUML JAR，并在项目根目录运行 `scripts/render-diagrams.ps1` 生成 PNG。生成图片存放在 `docs/diagrams/rendered`，由架构设计说明书引用。

图与模块对应关系：

- `01-system-use-case.puml`：全部业务模块和四类外部角色。
- `02-logical-layer-architecture.puml`：前端、访问边界、接口、业务、持久化和数据库。
- `03-backend-module-packages.puml`：认证、业务接口、服务、Mapper、领域对象和初始化配置。
- `04-care-domain-class.puml`：人口、健康和随访领域。
- `05-device-geo-domain-class.puml`：腕表、GPS、绑定、围栏、告警和轨迹领域。
- `06-deployment-topology.puml`：管理端、设备网关、应用服务器和 MySQL。
- `07-device-gps-ingest-sequence.puml`：真实 GPS 接入的认证、幂等和围栏处理时序。
