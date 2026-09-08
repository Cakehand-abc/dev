# 项目文档交付索引

2026-09-08，课程演示版 V1.0。八项业务功能、前后端代码、16 表 SQL、10 份 Word 和 16 页答辩 PPT 已完成。

| 文件 | 用途 |
| --- | --- |
| [01 软件需求规范](<deliverables/01 软件需求规范.docx>) | 功能、权限、统计口径与验收边界 |
| [02 数据库设计说明书](<deliverables/02 数据库设计说明书.docx>) | 16 张实际表的字段、索引及完整性约束 |
| [03 测试日志](<deliverables/03 测试日志.docx>) | 真实测试结果、缺陷回归与未执行项 |
| [04 系统架构设计说明书](<deliverables/04 系统架构设计说明书.docx>) | 系统分层、事务、安全与部署架构 |
| [05 测试用例](<deliverables/05 测试用例.docx>) | 23 项自动用例及浏览器、人工补充用例 |
| [06 测试计划](<deliverables/06 测试计划.docx>) | 测试范围、资源、准入准出和缺陷流程 |
| [07 项目例会纪要](<deliverables/07 项目例会纪要.docx>) | 已确认沟通决策及实际例会填写页 |
| [08 系统详细设计说明书](<deliverables/08 系统详细设计说明书.docx>) | 接口、字段、状态流转与空间算法 |
| [09 项目答辩](<deliverables/09 项目答辩.pptx>) | 项目范围、功能截图、测试、分工与部署 |
| [10 项目组成员分工](<deliverables/10 项目组成员分工.docx>) | 四人职责、交叉复核及答辩分配 |
| [11 项目部署环境说明书](<deliverables/11 项目部署环境说明书.docx>) | 环境矩阵、初始化、Maven、启动及故障处理 |

## 代码与验证入口

- [项目运行说明](../README.md)，[接口目录](API.md)，[SQL 结构](../database/01-schema.sql)，[独立演示数据](../database/02-demo-data.sql)。
- [Maven 23 项](evidence/maven-tests.json)、[HTTP 16 项](evidence/http-smoke.json)、[浏览器 12 项](evidence/browser-qa.json) 均通过，另补充[窄屏设备详情](evidence/mobile-device-qa.json)。
- [性能实测](evidence/performance.json)：双十万数据，20 并发约 60 秒，2664 请求零错误，四端点 P95 均低于 2 秒。
- [SQL 种子实测](evidence/sql-seed.json)：首次导入正确，非空业务表再次导入被拒绝。
- Word 共 62 页、PPT 共 16 页，均已逐页渲染检查。PPT 保留可编辑表格和图表。

## 团队补充事项

成员姓名、学号、实际例会信息与验收签字需要团队填写。测试 JVM 实际为 Zulu 25.0.1、编译目标 17，团队 JDK 17 与 IDEA 内置 JUnit 尚未实测。地图为无道路底图的离线坐标画布，数据均为虚构。公开部署、真实腕表与备份恢复演练未执行。

`source/` 是可编辑内容源稿。`review/` 保留早期评审历史，当前实现以本索引、交付文件和代码为准。文档生成脚本依赖本次模板及制作运行环境，应用构建和运行不依赖这些工具。
