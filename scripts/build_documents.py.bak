"""Build ten project documents from the selected retained System Design template.
Run with the Codex bundled Python. Render all outputs with render_docx.py afterwards.
"""
from pathlib import Path
from datetime import datetime
import json,re,hashlib
from xml.etree import ElementTree as ET
from docx import Document
from docx.shared import Pt, Inches, RGBColor, Cm
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT, WD_ROW_HEIGHT_RULE

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/deliverables';OUT.mkdir(parents=True,exist_ok=True)
SOURCE=ROOT/'docs/source';SOURCE.mkdir(exist_ok=True)
DATE='2026年9月9日';PROJECT='智慧医养大数据决策分析系统';VERSION='V1.1'
PROJECT_NO='SC-2026-01'
DEPARTMENT='软件工程项目组'
MEMBERS={
 'A':('周虹宏','202417020210','项目经理兼测试负责人'),
 'B':('钟毓林','202417020128','后端工程师'),
 'C':('胡吉涵','202417020202','后端兼数据工程师'),
 'D':('李治宏','202417020205','前端工程师'),
}
perf=json.loads((ROOT/'docs/evidence/performance.json').read_text(encoding='utf-8-sig'))
tests=[]
for f in (ROOT/'target/surefire-reports').glob('TEST-*.xml'):
 t=ET.parse(f).getroot();tests += [{'name':x.attrib['name'],'class':x.attrib['classname'],'seconds':float(x.attrib['time']),'passed':x.find('failure') is None and x.find('error') is None} for x in t.findall('testcase')]
(ROOT/'docs/evidence/maven-tests.json').write_text(json.dumps({'runDate':DATE,'java':'25.0.1','compilerRelease':17,'springBoot':'3.5.16','tests':tests},ensure_ascii=False,indent=2),encoding='utf-8')

def P(s):return ('p',s)
def H(s):return ('h',s)
def T(headers,rows):return ('t',headers,rows)
def IMG(file,caption,width=6.2):return ('img',file,caption,width)

ROLES=[['管理员 ADMIN','全部查询、业务维护、账号管理、demo 数据模拟、误录删除与设备密钥签发'],['业务人员 OPERATOR','全部业务查询、档案录入、设备绑定、围栏配置及事件处理'],['分析员 ANALYST','业务查询、统计与轨迹查看；无写入和账号管理权限']]
STACK=[['Java 与构建','Java 17 编译目标；Maven Wrapper 3.9.16'],['后端框架','Spring Boot 3.5.16；Spring MVC；Spring Security；Spring 事务'],['数据访问','MyBatis-Plus 3.5.17 BaseMapper；MyBatis XML 聚合统计'],['数据库','MySQL 8.0.16 及以上 8.0 系列；当前实测 8.0.46；H2 仅测试'],['前端','Vue 3、Vite 7、Element Plus 2.14.5；ECharts 6 为当前可选图表实现；npm 锁定依赖'],['测试','Boot BOM 管理 JUnit Jupiter 5.12.2 与 Platform 1.12.2']]
FR=[
 ['FR01','登录与退出','凭据正确进入首页；未认证 API 返回 401；退出后会话失效'],
 ['FR02','首页总览','地区和日期筛选；人口、健康、随访、设备与围栏概览；入口跳转'],
 ['FR03','人口统计','新增、编辑、归档和分页查询；管理员仅可删除无业务关系的误录档案；未知值单列'],
 ['FR04','健康检测','至少一项指标；时间校验；规则快照；检测次数与人数分开；个体趋势'],
 ['FR05','医生随访','创建计划、完成或取消；计划最多一次完成；零分母显示无计划'],
 ['FR06','腕表分布','设备维护、绑定和解绑；心跳状态、最新有效定位与无定位提示'],
 ['FR07','电子围栏','圆形围栏及成员管理；越界一次告警；返回关闭；人工处理留痕'],
 ['FR08','轨迹回放','按老人查询 24 小时内轨迹；最多 5000 点；播放暂停倍速；缺口标记'],
 ['FR09','数据模拟','管理员在 demo 环境生成单点、直线、环形、越界返回轨迹；预览后批量注入最多 500 点'],
 ['FR10','设备 GPS 接入','每台启用腕表签发独立密钥；设备批量上报定位和心跳；eventId 持久化幂等']]
RULES=[
 ['时间','API 带时区偏移，入库统一 Asia/Shanghai，精确到微秒。筛选采用 [start,end)，结束日期在前端换成次日零点。'],
 ['人口','仅统计 ACTIVE 档案，按查询当天周岁与当前地区分组；不提供历史人口快照。年龄未知、性别未知分别计入。'],
 ['健康','次数按记录数，人数按 elder_id 去重。至少一项指标超演示阈值即为异常；缺测保留 null；保存规则版本和快照。'],
 ['随访','分母为期间到期且截至截止时未取消的计划；分子为截止时已完成计划。截止为 end 与当前时间较早者。零计划返回 null 完成率。'],
 ['设备','最近心跳不超过 5 分钟为在线，无心跳为未知；停用单列。静态演示数据随时间转离线属于正常现象。'],
 ['围栏','WGS84 球面距离大于半径判越界；首点在外也告警；连续在外不重复；返回后再离开产生新事件。'],
 ['乱序与绑定','以设备和 eventId 去重，相同事件不同内容返回 409。历史定位按发生时间查找绑定，不归到当前新绑定人。'],
 ['轨迹','按 recorded_at、id 稳定升序。相邻点间隔超过 10 分钟显示缺口；不补点、不推测道路。']]
LIMITS='本版用于单单位教学演示，使用虚构老人和设备数据。系统提供通用 HTTP GPS 接口，但尚未与具体腕表厂商硬件联调。坐标画布没有道路底图；未接短信、第三方地图、Agent、分布式计算或多租户。健康规则为教学参数，不承担临床诊断用途。'

docs=[]
def add(number,title,owner,scope,pages):docs.append({'number':number,'title':title,'owner':owner,'scope':scope,'pages':pages})
add('01','软件需求规范','周虹宏主责，全员参与','定义十项功能、权限、统计口径及验收边界',[
 [H('1 项目目的与范围'),P('本系统面向医养服务管理与分析人员，将老人档案、健康检测、医生随访及设备定位整合到同一浏览器工作台，支持服务情况查询和统计分析。项目背景采用用户提供的医养结合示范项目建设要求，不宣称已经获得示范项目认定。'),P(LIMITS),H('2 用户与权限'),T(['系统角色','允许操作'],ROLES),H('3 工程约束'),T(['项目','实现基线'],STACK),P('按团队要求使用 Spring Boot 3，避免引入 Boot 4 技术线。本机已识别 IntelliJ IDEA 2024.3.4；尚未在 IDEA 内记录右键运行 JUnit 的结果，兼容性结论以团队使用 JDK 17 的实际运行记录为准。')],
 [H('4 功能需求'),T(['编号','功能','验收条件'],FR),H('5 配套管理'),P('管理员维护账号、数据模拟、设备接入密钥和误录档案删除；业务人员维护医生。老人归档会关闭绑定与监测、取消未完成随访计划，保留历史记录且不提供恢复。仅当健康、随访、设备绑定、定位和围栏关系均为零时允许物理删除。修改采用版本校验，过期版本返回冲突并要求刷新。'),P('页面提供加载、空数据和失败状态，列表支持分页。电话在响应中脱敏；系统不采集真实身份证号码。')],
 [H('6 数据与统计口径'),T(['主题','规则'],RULES[:4]),H('7 设备与空间规则'),T(['主题','规则'],RULES[4:]),P('圆形围栏半径为 50 至 5000 米。定位时间允许最多 60 秒时钟偏差，超范围坐标和未来异常时间拒收；旧定位允许存档，但不回退监测状态。')],
 [H('8 非功能需求'),T(['编号','要求','验证方法'],[['NFR01','后台写接口通过 CSRF 与角色检查；设备接口通过独立密钥认证','MockMvc 权限与设备认证测试'],['NFR02','10 万健康与 10 万定位数据，20 并发 60 秒，常规查询 P95 ≤ 2 秒、错误率 < 1%','隔离 MySQL 压力测试，记录端点和样本'],['NFR03','事务保证完成随访、绑定、批量定位与告警状态一致','业务断言、唯一约束和外键'],['NFR04','同源打包部署，后端独立 Maven 测试','构建脚本、JAR 启动、前端构建'],['NFR05','1440 桌面与 390 像素窄屏可访问；表格局部滚动','浏览器检查及逐页截图']]),H('9 验收与变更'),P('以十项功能演示、自动测试、接口检查和交付文件核对作为课程版验收依据。当前验证结果见测试日志；实际 IDEA 直接运行、团队 JDK 17、腕表厂商硬件联调和公开生产部署不记为已通过。'),P('新增真实道路底图、外部 Agent、付费接口、真实个人数据、清空数据库或公开部署属于范围或环境变化，先评估影响并由用户确认。项目成员姓名和学号已经依据四份实训报告统一；截止日期与签收信息由团队补充。'),H('10 追踪关系'),T(['需求','实现位置','验证'],[['FR01','auth 与 SecurityConfig','TC01 至 TC06'],['FR02 至 FR05','CareService、StatisticsService、App.vue','TC07 至 TC13、TC21、HTTP 与浏览器'],['FR06 至 FR08','GeoService、GeoCanvas.vue','TC14 至 TC20、TC22、浏览器'],['FR09 至 FR10','SimulationPanel、DeviceIngestController、DeviceAccessService','TC20、TC23 至 TC27，V1.1 HTTP 与 Chrome 已通过']])]
])

schema=(ROOT/'database/01-schema.sql').read_text(encoding='utf-8')
def split_sql(s):
 parts=[];depth=0;start=0;quote=False
 for i,ch in enumerate(s):
  if ch=="'":quote=not quote
  if not quote:
   if ch=='(':depth+=1
   elif ch==')':depth-=1
   elif ch==',' and depth==0:parts.append(s[start:i].strip());start=i+1
 parts.append(s[start:].strip());return parts
tables=[]
for m in re.finditer(r'CREATE TABLE (\w+) \((.*?)\);',schema,re.S):
 cols=[];cons=[]
 for s in split_sql(m[2]):
  c=re.match(r'(\w+)\s+(BIGINT|INT|VARCHAR\(\d+\)|BOOLEAN|DATE\b|DATETIME\(6\)|DOUBLE|DECIMAL\(\d+,\d+\)|TEXT)(.*)',s,re.S)
  if c:cols.append((c[1],c[2],c[3].strip()))
  else:cons.append(' '.join(s.split()))
 tables.append((m[1],cols,cons))
names={'sys_user':'系统账号','region':'地区','elder':'老人档案','doctor':'医生','health_rule':'教学阈值规则','health_record':'健康检测记录','followup_plan':'随访计划','followup_record':'随访结果','watch_device':'腕表设备','device_credential':'设备接入凭据','device_heartbeat_event':'设备心跳事件','device_binding':'历史绑定','location_point':'定位点','geofence':'圆形围栏','geofence_member':'围栏成员','geofence_state':'围栏监测状态','geofence_alert':'越界事件','audit_log':'操作审计'}
labels=dict(x.split('=',1) for x in 'id=自增主键|username=登录名|password_hash=BCrypt 密文|display_name=显示名|role=ADMIN OPERATOR ANALYST|enabled=是否启用|auth_version=认证版本|code=业务编号|name=名称|parent_id=父地区主键|gender=MALE FEMALE UNKNOWN|birth_date=出生日期 可未知|region_id=地区主键|phone=电话 响应脱敏|status=业务状态|version=并发版本|department=科室|metric=检测指标名|lower_bound=下界 包含|upper_bound=上界 包含|unit=单位|elder_id=老人主键|measured_at=检测时间|systolic=收缩压 mmHg|diastolic=舒张压 mmHg|heart_rate=心率 次每分|oxygen=血氧百分比|temperature=体温 摄氏度|abnormal=是否超教学阈值|rule_snapshot=判定规则 JSON 快照|source=数据来源|event_id=设备内唯一事件编号|doctor_id=医生主键|due_at=计划到期时间|canceled_at=取消时间|cancel_reason=取消原因|plan_id=随访计划主键 唯一|completed_at=完成时间|content=随访内容|result=结果摘要|serial_no=设备序列号|model=设备型号|last_seen_at=最后心跳|device_id=设备主键|key_hash=设备密钥 SHA-256 摘要|created_at=创建时间|rotated_at=最近轮换时间|bound_at=绑定开始时间|unbound_at=解绑时间|active_device_id=有效设备唯一占位|active_elder_id=有效老人唯一占位|binding_id=发生时绑定主键|longitude=WGS84 经度|latitude=WGS84 纬度|recorded_at=设备记录时间|received_at=服务端接收时间|center_lon=围栏中心经度|center_lat=围栏中心纬度|radius_m=半径 米|fence_id=围栏主键|member_id=围栏成员主键|last_point_id=最后参与监测的定位|last_recorded_at=监测事件时间|inside=是否在围栏内 可未知|active_alert_id=当前未结束越界事件|exit_point_id=触发定位点|triggered_at=越界触发时间|returned_at=实际返回时间|closed_at=监测关闭时间|close_reason=关闭原因|handled_by=处理账号|handled_at=处理时间|handling_note=处理说明|geometry_snapshot=围栏几何 JSON 快照|user_id=操作账号 可系统执行|action=动作编码|target_type=对象类型|target_id=对象主键|occurred_at=操作时间'.split('|'))
dbpages=[[H('1 数据库概览'),P('数据库 smart_care 使用 MySQL 8.0，包含 18 张表。database/01-schema.sql 为全新数据库结构来源；V1.0 已有库通过 database/03-device-ingest-upgrade.sql 新增设备接入表。脚本不包含删除或覆盖业务数据的逻辑。'),T(['分组','表与职责'],[['身份和档案','sys_user、region、elder、doctor'],['健康与随访','health_rule、health_record、followup_plan、followup_record'],['设备与定位','watch_device、device_credential、device_heartbeat_event、device_binding、location_point'],['围栏与审计','geofence、geofence_member、geofence_state、geofence_alert、audit_log']]),H('2 关系与生命周期'),P('地区一对多老人；老人一对多健康记录和随访计划；医生一对多计划；计划与完成记录为一对零或一。设备和老人通过历史绑定建立关系，当前绑定分别唯一。定位点同时保留设备、绑定及老人外键。设备凭据与设备一对一，只保存密钥摘要；心跳事件按设备和 eventId 唯一。'),P('围栏通过成员表关联老人。每名成员最多一条监测状态；状态指向当前活动事件，事件指向越界定位与处理账号。先建立状态和事件表，最后追加 active_alert_id 外键以处理循环引用。'),P('业务主键为 BIGINT，JSON 响应中的 Long 以字符串输出，避免 JavaScript 精度损失。业务 DATETIME(6) 保存上海本地时间；地区、规则与演示数据初始化由应用控制。')]]
for i,(name,cols,cons) in enumerate(tables):
 rows=[]
 for field,typ,extra in cols:
  null='否' if 'NOT NULL' in extra or 'PRIMARY KEY' in extra else '是'
  flags='主键 自增' if 'PRIMARY KEY' in extra else ('唯一 ' if 'UNIQUE' in extra else '')
  default=re.search(r'DEFAULT\s+(\S+)',extra)
  if default:flags+='默认 '+default[1]
  rows.append([field,typ,('可空 ' if null=='是' else '必填 ')+flags,({('health_rule','version'):'规则版本',('audit_log','result'):'执行结果码'}.get((name,field),labels.get(field,field)))])
 page=[H(f'{i+3} {names[name]} {name}'),T(['字段','类型','约束与默认','含义'],rows)]
 if cons:page.append(P('表级约束：'+'；'.join(cons)))
 indexes=re.findall(r'CREATE INDEX ([^;]+) ON '+name+r'\(([^;]+)\);',schema)
 if indexes:page.append(P('索引：'+'；'.join(a+' ('+b+')' for a,b in indexes)))
 dbpages.append(page)
dbpages.append([H('21 索引与完整性设计'),T(['约束','作用'],[['source 与 event_id 唯一','健康重复录入产生冲突，客户端可识别重试'],['device_id 与 event_id 唯一','定位和心跳重传不重复入库'],['device_credential.device_id 唯一','每台设备只有一个当前有效密钥摘要'],['active_device_id 和 active_elder_id 分别唯一','一个设备和老人同时最多一条有效绑定，历史解绑后置空'],['followup_record.plan_id 唯一','防止同一随访计划重复完成'],['围栏成员及状态唯一','防止同一关联存在多条监测状态']]),P('归档以状态变更保留历史，不级联删除。只有完全没有健康、随访、绑定、定位和围栏关系的误录档案允许管理员物理删除。定位、告警和绑定属于历史事实；修改围栏保存新的几何状态并关闭原监测事件，旧事件保留几何快照。'),H('22 初始化 备份与迁移'),P('先创建专用数据库，再执行表结构，再由 Bootstrap 建管理员、地区与教学规则。demo 模式且 APP_SEED_DEMO=true 时，空老人表触发演示种子；非空库不会重复生成。独立 SQL 种子见 database/02-demo-data.sql，与 Java 种子二选一。'),P('V1.0 已有库先备份，再执行 database/03-device-ingest-upgrade.sql。当前项目没有自动 Flyway 迁移，也没有自动备份任务。导出与恢复命令见部署说明；生产恢复必须先在独立库验证，不覆盖现有业务库。')])
add('02','数据库设计说明书','胡吉涵主责，钟毓林复核','说明实际 18 张表 全字段 索引及数据生命周期',dbpages)

perfrows=[[e['path'],str(e['samples']),str(e['p95Milliseconds'])+' ms',str(e['errors'])] for e in perf['endpoints']]
add('03','测试日志','周虹宏汇总，全员执行','记录真实构建 联调 缺陷回归及未执行项',[
 [H('1 测试结论'),P('截至 2026年9月8日，V1.1 Maven 回归共 28 项通过，覆盖批量定位、持久心跳幂等、受限删除和设备密钥认证。全新 18 表结构、从 16 表升级到 18 表、V1.1 实际 HTTP 写路径及新增页面浏览器复核均在隔离 MySQL 实例通过。此结论不等同腕表厂商硬件或公开生产环境验收。'),T(['项目','结果','证据'],[['Maven JUnit','28 项通过 0 失败 0 错误','docs/evidence/maven-tests.json'],['V1.1 MySQL 实际 HTTP','原 16 项冒烟与新增 5 项写路径通过','docs/evidence/http-smoke-v11.json、v11-http-qa.json'],['V1.1 Chrome 页面检查','4 项通过 无页面脚本异常','docs/evidence/v11-browser-qa.json'],['V1.1 数据库结构与升级','全新 18 表；16 表升级后 18 表且哨兵数据保留','docs/evidence/mysql-v11-schema.json'],['V1.0 负载测试','2664 请求 0 错误 四端点 P95 < 2 秒','docs/evidence/performance.json']]),H('2 执行环境'),T(['项目','实测'],[['操作系统','Windows 11 10.0.26100'],['CPU','Intel Core i7-13650HX 14 核 20 逻辑处理器'],['Java','Zulu 25.0.1 编译 release 17；未冒充 JDK 17 实测'],['数据库','H2 18 表；MySQL 8.0.46 全新结构和升级路径通过'],['应用和浏览器','V1.1 JAR 端口 18081；Chrome 152.0.7977.83'],['构建','Maven 3.9.16；Vite 7.3.6；Element Plus 2.14.5']])],
 [H('3 实际测试过程与修复'),T(['记录','问题与原因','修复及回归'],[['D01','统计 value 别名与 H2 保留字冲突','为别名加反引号；统计测试与 MySQL 冒烟通过'],['D02','先添加 ORDER BY 再调用 selectCount 导致计数 SQL 不兼容','计数后再添加排序；分页与轨迹测试通过'],['D03','Java 纳秒与数据库微秒精度不同导致同事件比较不一致','输入与当前时间统一截断微秒；重传与乱序测试通过'],['D04','窄屏饼图标签越出图表边界','取消外侧标签，图例显示名称和值；重新截图检查'],['D05','Windows 运行中的演示 JAR 阻止重新打包改名','停止本任务进程后打包成功；部署说明写明先停止'],['D06','窄屏设备详情被样式隐藏','详情改为在地图下方显示；前端构建与窄屏复核'],['D07','原隔离 MySQL 未运行且项目路径含中文不能作为新数据目录','在用户临时目录启动 13317 独立实例；18 表、升级脚本、HTTP 与 Chrome 复核通过'],['D08','浏览器脚本只等待页面导航，未等待 SPA 数据接口','改为等待对应 API 响应后断言按钮并截图；4 项复核通过']]),P('以上为开发过程中的实际缺陷和环境问题，不补造发现人的姓名、工时或线下会议。测试执行与页面检查由开发自动化完成，项目经理周虹宏负责后续人工复核与签收。'),H('4 本次构建记录'),P('2026年9月8日，27 项 BusinessTests 与 1 项应用上下文测试通过。测试覆盖原功能以及批量轨迹、心跳事件幂等、受限删除、管理员删除权限和设备独立密钥。前端 Vite 生产构建通过；MySQL HTTP 与 Chrome 页面复核随后通过。')],
 [H('5 性能实测'),P('2026年9月8日 00时28分开始。原库 1080 条健康、648 条定位；各加入 100000 条带本次唯一前缀的模拟记录，测试时分别为 101080 和 100648 条。20 个工作线程持续约 60.371 秒，默认近 30 天范围、首页 20 条、无地区过滤。'),T(['端点','样本数','P95','错误数'],perfrows),P('端点均满足 P95 ≤ 2000 ms、错误率 < 1%。结果仅代表本机、本数据分布与这四类只读端点，不包含 V1.1 批量写入吞吐。测试脚本在 finally 中删除本次专属记录，已核对恢复为 1080 和 648 条。'),H('6 未执行和待签收'),T(['检查','当前状态','后续负责人'],[['JDK 17 真正运行','未执行；当前机器测试 JVM 为 25','钟毓林配置团队 JDK 17 后运行 mvnw test'],['IDEA 内置 JUnit 运行','本机识别 IDEA 2024.3.4；尚未右键运行','钟毓林使用团队 JDK 17 记录结果'],['Word 模板版页面渲染','10 份 Word 已由 LibreOffice 转 PDF 并逐页复核','周虹宏、李治宏完成最终内容签收'],['腕表厂商硬件联调','通用接口已实现，真实硬件未执行','胡吉涵获取厂商协议和样机后联调'],['正式部署 恢复演练','未执行公开部署及备份恢复','周虹宏、胡吉涵确认目标环境后执行']]),P('人工复核人：待团队填写。验收日期与签字：待团队实际验收后填写。')]
])

add('04','系统架构设计说明书','钟毓林、胡吉涵','按 UML 用例视图 逻辑视图 类图 动态视图和部署视图说明当前系统',[
 [H('1 简介'),P('本文档面向项目组成员、指导教师和后续维护人员，说明智慧医养大数据决策分析系统的结构、关键约束、模块边界、核心数据关系和部署方式。所有图均根据当前仓库生成，模板中的原项目名称、类名和部署节点只用于识别格式与图类型，不作为本系统需求。'),H('1.1 目的'),P('系统采用 Vue 3 单页前端与 Spring Boot 3.5.16 模块化单体。构建后把前端静态资源纳入可执行 JAR，由同一应用提供后台接口和设备接口；MySQL 8 保存业务事实。该结构适合四人小组开发、课程演示和单机部署。'),H('1.2 范围'),P('本文覆盖登录、首页、人口、健康、随访、腕表分布、电子围栏、轨迹回放、数据模拟、批量轨迹注入、受限删除以及通用 GPS 和心跳接口。问答 Agent、真实道路地图、腕表厂商协议适配、短信和多租户不在当前实现范围。'),H('2 架构表示方式'),P('参照交付模板，使用 UML 用例视图、逻辑分层与包视图、核心类图和部署视图。为解释真实设备接口的认证、幂等与围栏处理，补充一张顺序图；模板没有要求活动图，本版不为普通 CRUD 重复绘制活动图。'),H('3 架构目标和约束'),T(['目标或约束','设计响应'],[['IDEA 2024 及以下开发','采用 Spring Boot 3.5.16 与 Java 17 编译目标，不引入 Spring Boot 4'],['四人协作与简化部署','使用模块化单体和 Maven，前后端可分开开发、交付时同源运行'],['数据一致性','事务、行锁、乐观锁、唯一约束和事件幂等共同保护'],['权限与隐私','后台 Session、CSRF、三角色 RBAC、电话脱敏；设备使用独立密钥摘要'],['演示和设备并存','demo 数据模拟与真实设备接口分开，模拟入口仅管理员且仅 demo 环境可用']])],
 [H('4 关键用例视图'),P('系统包含三类后台用户和一类设备调用方。管理员可以执行全部查询和维护、账号与密钥管理、数据模拟及受限删除；业务人员可以查询并维护业务资料；分析员只读；腕表或设备网关只调用 GPS 与心跳接口。'),IMG('docs/diagrams/rendered/01-system-use-case.png','图 4-1 系统关键用例视图',6.1)],
 [H('5 层次结构'),P('整体分为展示、访问与接口、业务、持久化和数据存储五个层次。Vue 页面通过 api.js 调用 REST 接口；Spring Security 区分后台 Session 与设备密钥；Service 维护事务和业务不变量；MyBatis-Plus 处理常规 CRUD，MyBatis XML 处理聚合统计。'),IMG('docs/diagrams/rendered/02-logical-layer-architecture.png','图 5-1 系统分层架构图',6.2)],
 [H('6 逻辑视图'),P('逻辑视图按 Java 包表达代码组织。auth 负责身份和授权，web 只处理 HTTP 契约，service 落实业务规则，mapper 访问数据，domain 对应数据库实体，config 完成初始化和 JSON 规则。'),IMG('docs/diagrams/rendered/03-backend-module-packages.png','图 6-1 后端包与依赖关系图',5.25)],
 [H('6.1 人口 健康与随访领域'),P('Region、Elder、Doctor、HealthRule、HealthRecord、FollowupPlan 和 FollowupRecord 构成核心医养业务对象。归档老人不再进入人口统计且不提供恢复操作；有健康、随访、设备绑定、定位或围栏关系时禁止物理删除。随访计划最多对应一条完成记录。'),IMG('docs/diagrams/rendered/04-care-domain-class.png','图 6-2 人口 健康与随访领域类图',5.8)],
 [H('6.2 设备 定位与围栏领域'),P('Device、DeviceCredential、HeartbeatEvent、Binding、Location、Fence、FenceMember、FenceState 和 Alert 构成设备空间领域。定位同时保存设备、发生时绑定与老人主键，换绑不改变历史归属；设备加 eventId 唯一，持续越界仅保留一个活动事件。'),IMG('docs/diagrams/rendered/05-device-geo-domain-class.png','图 6-3 设备 定位 围栏与告警领域类图',6.1)],
 [H('7 动态视图'),P('设备接口不使用浏览器 Session。设备网关在 HTTPS 请求头携带设备编号和独立密钥；服务端只保存 SHA-256 摘要。新定位在事务内查重、解析发生时绑定、写入定位并推进围栏状态。相同事件同内容可安全重传，相同事件编号改内容返回 409。'),IMG('docs/diagrams/rendered/07-device-gps-ingest-sequence.png','图 7-1 GPS 定位接入顺序图',6.0)],
 [H('8 部署视图'),P('开发态由 Vite 代理后台接口；交付态为浏览器和设备网关共同访问 Spring Boot JAR，JAR 同时提供 Vue 静态资源和 REST 接口，并通过 JDBC 连接 MySQL。生产环境需要在 JAR 前配置 HTTPS 反向代理，当前项目不自动配置公网域名、证书或防火墙。'),IMG('docs/diagrams/rendered/06-deployment-topology.png','图 8-1 系统部署拓扑图',4.4)],
 [H('9 接口设计'),T(['边界','路径','认证和责任'],[['后台认证','/api/v1/auth','Session、CSRF、登录注销和认证版本'],['业务与统计','/api/v1','三角色权限、分页筛选、业务事务和统计口径'],['演示注入','/api/v1/demo','仅 demo 环境和管理员，单批最多 500 点'],['真实设备','/api/device/v1','X-Device-Key，GPS 与心跳 eventId 幂等']]),P('浏览器先取得 CSRF 再登录；设备接口不携带 Cookie。普通增删改查经 MyBatis-Plus Mapper，统计聚合经 StatisticsMapper XML。后台和设备接口共享 GeoService 的定位、绑定和围栏业务规则，避免两套逻辑产生差异。'),H('10 关键技术决策'),T(['决策','原因','影响'],[['Spring Boot 3.5.16','满足团队 IDEA 与 JUnit 5 使用习惯','由 Boot BOM 管理测试组件版本'],['模块化单体加 MySQL','控制四人小组部署和事务复杂度','当前不是微服务，不支持跨节点 Session'],['Element Plus 为前端组件库','满足表单、选择器、日期和提示等交互要求','SimulationPanel 已实际使用；ECharts 是当前图表实现而非强制框架'],['不设 Agent 工程师','当前功能是确定性查询、统计和规则处理','问答 Agent 仅作为明确新增需求后的扩展'],['通用设备接口','先形成厂商无关契约、密钥和幂等机制','尚未与具体腕表硬件联调']]),H('11 图源与维护'),P('图源位于 docs/diagrams/*.puml，PNG 位于 docs/diagrams/rendered。IDEA 2024.3 可通过 PlantUML 预览插件直接打开；也可运行 scripts/render-diagrams.ps1。修改类名、表结构或接口边界后，应同步更新图源、重新渲染并复核本说明书。')]
])

case_data=[
 ('anonymousCannotReadData','FR01','未认证读取','不带会话 GET /elders','HTTP 401'),
 ('realLoginAndLogoutInvalidateSession','FR01','登录注销闭环','获取 CSRF；admin 正确密码登录；GET me；POST logout','登录 200 且无密码字段；me 200；旧 Session 失效'),
 ('invalidPasswordIsRejected','FR01','错误密码','带 CSRF 用 admin 和错误密码登录','HTTP 401'),
 ('csrfAndReadOnlyRoleAreEnforced','FR01','写权限与 CSRF','管理员写入不带 CSRF；ANALYST 带 CSRF 写入；ANALYST 查询','两次写入 403；查询 200'),
 ('roleChangeInvalidatesExistingAuthentication','FR01','会话降权','创建 OPERATOR 保存认证对象；降为 ANALYST；用旧认证访问','HTTP 401'),
 ('lastAdministratorCannotBeDisabled','FR01','最后管理员保护','仅一名有效管理员时尝试停用该账号','409 业务冲突；管理员保留'),
 ('paginationAndMassAssignmentAreRejected','FR03','参数边界','GET elders?size=101；POST elders 提交不允许的 id 字段','均为 400'),
 ('populationIncludesUnknownAndExcludesArchived','FR03','人口未知与归档','创建未知性别出生日期的有效老人；核对总数；归档后重查','初始总数 1 且年龄未知；归档后 0'),
 ('staleElderUpdateIsRejected','FR03','过期版本','对 version=0 档案提交 version=4','409 并提示刷新'),
 ('healthCountsPeopleAndRecordsSeparately','FR04','健康计数','同一老人录入 160 收缩压和 70 心率两条记录','检测 2 次、1 人、异常 1 人'),
 ('healthMissingMetricsAndFutureDataAreRejected','FR04','缺测与未来','所有指标缺省提交；再以明天时间和血氧 99 提交','均拒绝，不入库'),
 ('completionIsAtomicAndCannotBeRepeated','FR05','随访完成','创建一条到期计划；完成；用相同内容再次完成','第二次冲突；只有一条完成记录；完成率 100'),
 ('noPlansHaveNoArtificialCompletionRate','FR05','零计划','空计划集查询随访统计','completionRate 为 null'),
 ('duplicateBindingIsRejected','FR06','重复绑定','设备已绑定该老人，再绑定一次','业务冲突，无重复有效绑定'),
 ('persistentOutsideOnlyCreatesOneAlertAndReturnAllowsAnother','FR07','越界生命周期','500 米圆；连续两个外点；一个内点；再一个外点','事件数 1、1；返回时间写入；最终 2'),
 ('duplicateAndOutOfOrderPointsDoNotCorruptMonitoring','FR07','重传与乱序','外点同内容重传；更早内点；原 eventId 改内容重传','只存两个点；外部状态不回退；改内容 409'),
 ('trajectoryIsSortedAndRestricted','FR08','轨迹顺序与范围','先写较晚点再写较早点；查 1 小时；再查 48 小时','升序返回；48 小时拒绝'),
 ('rebindingPreservesHistoricalOwner','FR06 FR08','换绑历史','解绑老人甲，绑定乙，补传甲绑定期间的定位','历史定位归甲；乙当前定位不被污染'),
 ('heartbeatDoesNotMoveBackwards','FR06','旧心跳','先更新较新心跳，再写更旧心跳','lastSeenAt 不后退'),
 ('heartbeatEventsAreDurablyIdempotent','FR10','心跳幂等','相同设备和 eventId 原样重传，再以同编号改时间','只保存一条；改内容返回冲突'),
 ('batchLocationIngestionAcceptsMultiplePoints','FR09 FR10','批量定位','一次提交两个点，再原样重传同一批次','首次新增 2 点；重传识别 2 个重复'),
 ('restrictedDeleteOnlyRemovesUnusedRecords','FR03','受限删除','删除无关系误录档案；再删除已有绑定档案','前者成功；后者提示只能归档'),
 ('onlyAdministratorCanDeleteElder','FR03','删除权限','业务人员与管理员分别删除无关系档案','业务人员 403；管理员成功'),
 ('authenticatedDeviceEndpointAcceptsKeyAndRejectsMissingKey','FR10','设备独立认证','不带密钥上报，再用设备签发密钥上报','前者 401；后者成功新增定位'),
 ('productionDoesNotExposeDemoIngestion','FR07','演示边界','非 demo 配置，ADMIN 带 CSRF 调用模拟定位入口','HTTP 404'),
 ('dashboardAndStatisticsContractsWork','FR02','统计响应','认证后访问 dashboard 和三个统计端点','均为 200 且 code=OK'),
 ('distanceHasCorrectScale','FR07','球面距离','计算同点与赤道纬度相差 1 度的距离','同点 0；后一结果 111000 至 111300 米'),
 ('contextLoads','工程','应用上下文','加载默认测试配置与 H2 结构','Spring 上下文启动成功')]
casepages=[[H('1 用例说明'),P('自动化用例以实际 src/test/java 中的方法为准。除登录类特例外，BusinessTests 每项在独立回滚事务中建立一位未知信息老人、一位医生、一只设备和三天前开始的有效绑定。测试管理员密码只用于 H2 测试，不是部署账号密码。'),P('TC01 至 TC28 已于 2026年9月8日执行通过。涉及 HTTP 状态的用例通过 MockMvc 断言；直接业务调用用例断言 Api.Failure 或数据状态，不将其冒充跨网络 HTTP 测试。'),T(['级别','范围','执行方式'],[['自动化功能','认证、统计、历史、围栏、批量定位、删除与设备认证','mvnw.cmd test'],['MySQL 联调','V1.1 原 16 项冒烟和新增 5 项写路径通过','scripts/smoke_http.py、v11_http_qa.py'],['浏览器','V1.1 数据模拟、删除、密钥入口和窄屏共 4 项通过','scripts/v11-browser-qa.mjs'],['数据库升级','16 表升级到 18 表且哨兵数据保留','database/03-device-ingest-upgrade.sql'],['负载','V1.0 四端点 20 并发 60 秒','scripts/performance_test.py，仅本任务隔离库']])]]
for start in range(0,len(case_data),4):
 page=[H(f'2 自动化用例 {start+1} 至 {min(start+4,len(case_data))}')]
 for idx,(method,fr,title,steps,expected) in enumerate(case_data[start:start+4],start+1):
  page += [H(f'TC{idx:02} {title}'),P(f'需求：{fr}。步骤：{steps}。预期：{expected}。结果：通过。'),P('方法：'+method)]
 casepages.append(page)
casepages.append([H('3 浏览器和人工复核用例'),T(['编号','操作','预期与状态'],[['UI01','逐项进入原九个业务导航页','V1.0 页面加载并可见；已通过'],['UI02','管理员进入数据模拟页生成直线轨迹并注入','预览、批量注入、心跳结果正确；V1.1 Chrome 已通过'],['UI03','人口页验证删除入口；HTTP 分别删除误录和已有业务档案','入口可见，前者删除，后者返回只能归档；V1.1 已通过'],['UI04','腕表分布查看密钥入口；HTTP 验证密钥认证','入口可见，缺密钥 401、有效密钥接收；V1.1 已通过'],['MAN01','IDEA 选择 JDK 17，右键运行两个测试类','应发现 JUnit 5 并通过；未执行'],['MAN02','轨迹播放 暂停 倍速 拖动进度','时间和点位一致；需团队人工演示复核'],['MAN03','真实设备或厂商网关调用定位和心跳','认证、重传、错误处理符合指南；待获得硬件'],['MAN04','真实 V1.0 业务库备份恢复后执行 03 升级脚本','隔离结构升级已通过；正式库恢复演练未执行']]),P('正式验收时由周虹宏将人工实际结果、执行人、日期和截图位置写入测试日志。失败项应附复现数据及服务端 requestId，修复后先回归相关用例，再执行核心冒烟。')])
add('05','测试用例','周虹宏组织，全员执行','给出真实自动测试映射和人工补充用例',casepages)

add('06','测试计划','周虹宏','规定测试范围 资源 顺序 准入准出和缺陷处理',[
 [H('1 目标与范围'),P('本计划用于课程版持续回归与团队验收。验证十项功能、角色隔离、统计口径、设备历史归属、数据模拟、受限删除和设备认证，并通过隔离 MySQL 检查实际 SQL。临床准确性、具体腕表厂商硬件和公开生产安全验收不属于当前已通过范围。'),T(['阶段','输入','活动和产物'],[['环境确认','代码、pom、专用数据库','记录 Java Maven Node MySQL IDEA 完整版本'],['单体回归','编译成功','28 项 JUnit 业务及 MockMvc；Surefire 报告'],['数据库联调','18 表与模拟数据','升级、设备密钥、批量定位、删除与原功能冒烟'],['页面验收','可运行 JAR','数据模拟、误录删除、密钥显示及原页面'],['性能测试','隔离环境可清理专属数据','原只读基线；新增批量写入另行测量'],['交付复核','代码文档冻结','部署命令、文件完整性、答辩演示与签收']]),H('2 角色与资源'),P('周虹宏维护计划、用例、缺陷单与验收结论；钟毓林负责认证、健康、随访和删除权限测试；胡吉涵负责 SQL、设备密钥、批量定位、围栏和负载；李治宏负责模拟页面与浏览器联调。使用虚构数据，不引入个人真实健康或轨迹信息。')],
 [H('3 准入与准出'),T(['环节','准入条件','准出条件'],[['功能测试','依赖下载、测试库可用','全部核心自动测试通过；失败有定位记录'],['接口联调','仅专用库初始化、服务启动成功','16 项冒烟通过；SQL 无方言错误'],['页面验收','前端构建和接口通过','关键页面可使用；阻断与严重缺陷清零'],['性能','记录硬件 数据量 端点 筛选','P95 ≤ 2 秒、错误率 < 1%；恢复原始测试数据'],['交付','文档与代码一致','所有交付文件打开与排版正常；剩余未测项明确列出']]),H('4 缺陷处理'),P('缺陷记录包含编号、版本、前置数据、复现步骤、实际与期望、严重性、负责人及回归结果。阻断为无法登录或无法启动；严重为越权、数据串人、重复业务事实；一般为图表口径或操作错误；轻微为不阻断使用的排版问题。'),P('发现数据库污染、写入到非测试库或账号凭据泄漏时立即停止相关测试并保留最小证据。环境不可用属于未执行，不能计入通过率。并发写入正确性还需专门多连接测试，不以单事务测试替代。'),H('5 相对排期'),P('截止日期未提供，不虚构日历。建议团队验收按 T1 环境与自动回归、T2 MySQL 与页面、T3 性能及缺陷修复、T4 部署与答辩顺序安排；任何失败按影响回退相关阶段。测试日志已记录开发过程实际结果，团队可以在此基础上补充人工验证。')]
])

add('07','项目例会纪要','周虹宏','保留已确认决策并提供真实例会填写页',[
 [H('1 已确认的沟通决策'),P('本页依据用户在项目沟通中的指令整理，不是虚构的线下例会记录。实际会议时间、参会名单和签字尚未提供，不填入假设的出席事实。'),T(['事项','已确认内容','落实位置'],[['工程方案','使用 Java Spring MVC Spring Boot MyBatis MyBatis-Plus Vue 与 Element Plus；ECharts 非必选','pom.xml 与前后端代码'],['构建管理','补充 Maven，并增加部署环境说明文档','Maven Wrapper scripts 及第 11 项文档'],['版本调整','Spring Boot 4 降到 3，团队 IDEA 2024 及以下','Boot 3.5.16 与 JUnit 5'],['人员与 Agent','周虹宏负责项目需求管理测试；钟毓林负责核心后端；胡吉涵负责数据和设备后端；李治宏负责前端；本版无独立 Agent 岗位','成员分工说明'],['数据与地图','虚构演示数据，圆形围栏和离线坐标画布','DemoSeeder GeoService GeoCanvas'],['优化顺序','不做归档恢复；依次完成数据模拟、批量轨迹、受限删除、真实 GPS 接口','SimulationPanel CareService DeviceIngestController'],['删除规则','仅管理员删除无业务关系的误录档案；其余只能归档','CareService 与后端权限'],['重复文档','第 8 项改为系统详细设计说明书','08 系统详细设计说明书']]),H('2 当前需要团队补充'),P('仍需补充完整 IDEA 版本、实际截止日期、最终验收与答辩时间。周虹宏汇总签收信息；钟毓林补录 JDK 17 和 IDEA 直接测试结果；V1.1 实库和页面自动复核已完成；胡吉涵和李治宏继续补真实腕表与人工演示。')],
 [H('3 项目例会填写模板'),T(['字段','待填写内容'],[['会议编号','待填写'],['会议日期 地点或方式','待实际开会后填写'],['主持人 记录人','待填写'],['实际参会及缺席人员','待填写'],['本次目标','待填写']]),H('4 议题与结论'),T(['议题','讨论依据','实际结论'],[['上次行动项检查','待填写','待填写'],['本周功能与缺陷','待填写','待填写'],['风险和范围变更','待填写','待填写']]),H('5 行动项'),T(['行动','负责人','完成日期与验收'],[['补录 JDK 17 与 IDEA 实测','钟毓林','待填写'],['人工演示关键流程','周虹宏、李治宏','待填写'],['真实腕表或网关联调','胡吉涵','待获得设备'],['其他会议新增任务','待填写','待填写']]),P('会议结论确认人及确认时间：待实际确认后填写。保留事实、决策和负责人即可，无需编写与真实项目不符的进度或工时。')]
])

add('08','系统详细设计说明书','钟毓林、胡吉涵、李治宏','描述接口规范 输入字段 状态流转 算法与前端实现',[
 [H('1 接口总则'),P('后台接口前缀 /api/v1，设备接口前缀 /api/device/v1，JSON UTF-8。成功返回 {code:"OK",message:"成功",data:...}。Long 值序列化为字符串；时间返回带偏移的 ISO 格式。业务异常含业务码、消息和 requestId。'),T(['方法与路径','说明','权限'],[['GET POST /auth/*','CSRF、登录、当前账号、注销','后台会话'],['GET /dashboard /statistics/*','汇总与图表','三角色'],['POST PUT 业务资源','字段白名单、版本和事务','ADMIN OPERATOR'],['DELETE /elders/{id}','仅无业务关系误录档案','ADMIN'],['POST /demo/locations/batch','1 至 500 点，仅 demo profile','ADMIN'],['POST /devices/{id}/credential','签发或轮换一次性显示密钥','ADMIN'],['POST /api/device/v1/locations','批量 GPS 定位','X-Device-Key'],['POST /api/device/v1/heartbeats','设备心跳','X-Device-Key']]),P('后台首次登录流程为 GET csrf、POST login、再次 GET csrf。设备接口不使用浏览器 Cookie 和 CSRF；服务端按 deviceId 校验独立密钥摘要，生产必须使用 HTTPS。'),H('2 分页与筛选'),P('分页 page 默认 1，size 默认 20，最大 100。地区与老人、医生 id 为可选查询条件。统计和记录时间默认近 30 天；轨迹必须显式提供 start 与 end。')],
 [H('3 写入请求字段'),T(['资源','必需字段','可选字段或编辑约束'],[['老人 /elders','code name gender regionId','birthDate phone；编辑带 version；无业务关系才可删'],['健康 /health-records','elderId measuredAt；至少一个指标','五项指标及 eventId'],['随访 /followup-plans','elderId doctorId dueAt','完成或取消字段'],['设备 /devices','serialNo model enabled','编辑带 version；绑定 elderId；密钥无请求体'],['围栏 /geofences','name centerLon centerLat radiusM enabled','编辑带 version；成员 elderIds'],['模拟批量','deviceId points','点含 eventId 经度 纬度 采集时间；最多 500'],['真实设备批量','deviceId points 与 X-Device-Key','格式同模拟批量'],['设备心跳','deviceId eventId recordedAt 与 X-Device-Key','相同事件可原样重传']]),H('4 字段校验'),P('健康指标输入边界：收缩压 20～300、舒张压 10～200、心率 10～300、血氧 0～100、体温 25～45；同时填写血压时收缩压须大于舒张压。检测时间和随访完成时间不可晚于当前时间。'),P('设备批量包含 1 至 500 点，经纬度使用 WGS84，时间必须带时区且不得超过允许的未来偏差。每台设备密钥由 32 随机字节生成，明文只在签发时返回一次。')],
 [H('5 定位与围栏算法'),P('批量请求顶层为 deviceId 和 points。每点包含 eventId、longitude、latitude、recordedAt。经度 -180～180，纬度 -90～90，四舍五入到 7 位小数。查询事件时间对应的 [bound_at,unbound_at) 绑定；不存在有效历史绑定则整批回滚。'),P('距离采用 Haversine 球面距离。d 大于 radiusM 为外部，否则内部。'),T(['原状态与新点','数据库操作','效果'],[['未知或内部收到外点','创建告警并指向新告警','开始一次越界'],['外部收到较新外点','更新最后点，不新增告警','持续越界不重复'],['外部收到较新内点','写返回与关闭时间','结束本次越界'],['旧时间定位','入历史表，不推进当前状态','避免迟到点造成假返回'],['相同 eventId 原样重传','返回已存事实','不重复写入'],['相同 eventId 内容改变','返回 409','阻止事件编号复用']]),P('设备凭据表只保存 SHA-256 摘要；轮换后旧密钥立即失效。心跳事件单独持久化，使定位与心跳都具备可验证幂等语义。')],
 [H('6 前端交互设计'),P('App.vue 管理业务导航；SimulationPanel.vue 按需使用 Element Plus 表单、选择器、日期时间、数字输入、提示和按钮，为管理员提供手动单点、直线、环形和越界返回模板，提交前在 GeoCanvas 预览。腕表页生成接入密钥并明确只显示一次；人口页提供受限删除确认。'),T(['页面组','交互与状态'],[['人口','新增 编辑 归档；管理员删除误录；无归档恢复'],['设备','绑定解绑、定位详情、接入密钥签发'],['数据模拟','设备和场景选择、参数校验、轨迹预览、批量结果'],['轨迹围栏','回放、缺口、围栏和告警状态'],['通用','加载 失败 空数据；角色隐藏未授权入口']]),H('7 事务与错误恢复'),P('批量任一点失败时整个事务回滚。设备网络超时保留原 eventId 原样重传；400 修正数据，401 停止并申请新密钥，409 检查绑定或事件编号。后台普通表单继续按现有 CSRF 和版本冲突处理。'),P('设备分布仍存在按条补查，扩大规模时需批量查询并重测。当前监测串行锁适合课程版一致性验证，不代表高吞吐硬件接入架构。'),H('8 开发者入口'),P('完整端点与请求示例见 docs/API.md 和 docs/DEVICE-INGEST.md。全新结构使用 database/01-schema.sql，V1.0 升级使用 database/03-device-ingest-upgrade.sql。')]
])

add('10','项目组成员分工','周虹宏','定义四人责任与协作交付边界',[
 [H('1 四人配置'),P('当前功能以数据管理、统计、设备接入和确定性空间规则为主，无须独立 Agent 开发工程师。分工以当前代码和用户给定岗位边界为准：项目经理负责需求、项目管理和测试，两名后端分别负责核心业务与数据设备，前端负责 Vue 页面与交互。'),T(['成员与学号','岗位','主要交付'],[['周虹宏 202417020210','项目经理兼测试负责人','需求、计划、例会、用例、日志、进度与答辩汇总'],['钟毓林 202417020128','后端工程师','认证权限、受限删除、健康随访、业务测试与接口'],['胡吉涵 202417020202','后端兼数据工程师','18 表、设备密钥、批量 GPS、围栏轨迹、负载与部署'],['李治宏 202417020205','前端工程师','Vue 与 Element Plus 页面、数据模拟、图表、坐标画布、交互联调']]),H('2 交叉复核'),T(['交付物','主责','复核'],[['需求与范围变更','周虹宏','钟毓林 胡吉涵 李治宏评估影响'],['数据库与 GPS 接入','胡吉涵','钟毓林检查认证、事务和约束'],['认证和受限删除','钟毓林','胡吉涵检查安全和数据一致性'],['模拟页面与图表口径','李治宏','周虹宏验收，后端核对接口'],['测试与缺陷关闭','周虹宏','对应开发负责人修复，其他成员回归'],['部署与答辩','胡吉涵 周虹宏','全员各自模块演示']])],
 [H('3 协作流程'),P('接口先明确请求、响应、权限和错误码，再分别实现前后端。每次提交附关联需求和验证结果；SQL 变更新增迁移说明，避免直接修改他人演示库。设备密钥只交付给负责网关配置的成员，不写入代码、文档或聊天记录。'),P('周虹宏维护需求清单与未测项；钟毓林和胡吉涵互审事务与 SQL；李治宏使用真实接口联调并同步空数据、失败和只读状态。'),H('4 答辩建议分配'),T(['成员','讲解内容','建议时长'],[['周虹宏','背景、目标、范围、测试结论','2 分钟'],['钟毓林','Spring Boot 3、权限、受限删除与随访','3 分钟'],['胡吉涵','18 表、设备密钥、GPS 接口与围栏算法','3 分钟'],['李治宏','数据模拟、批量轨迹、图表与回放','4 分钟']]),P('时长为建议安排，并非学校已规定要求。截止日期、正式答辩总时长与成员贡献工时应由团队按事实补充。'),H('5 Agent 扩展条件'),P('四份报告中的问答 Agent 只出现在未来展望，并非当前已实现功能。只有新增自然语言统计问答、辅助报告生成等明确需求时，再评估 Agent 工作量与岗位；届时可先由胡吉涵负责数据接入、钟毓林负责权限审计。Agent 输出须展示数据来源，且不替代临床判断。')]
])

add('11','项目部署环境说明书','胡吉涵主责，钟毓林协助','说明环境 凭据 初始化 构建 启动 验证及故障处理',[
 [H('1 部署结论与环境矩阵'),P('交付形态为 Vue 静态资源内置的 Spring Boot 可执行 JAR，加一套 MySQL 数据库。后台和设备 GPS 接口由同一 JAR 提供。首次部署建立 18 表空库；V1.0 已有库必须先备份再执行设备接入升级脚本。'),T(['组件','团队基线','本机验证'],[['操作系统','Windows 10 或 11 开发；其他系统另测','Windows 11'],['Java','团队建议 JDK 17，pom release 17','Zulu 25.0.1 编译 测试 运行通过'],['IDEA','2024 系列及以下按完整版本测试','已安装 2024.3.4；内置 JUnit 未实测'],['Maven','Wrapper 3.9.16；失败时可回退 PATH Maven','28 项测试与编译通过'],['Node 与前端','Node 22.12 以上的 22 系列或满足 Vite 7 要求','Vite 7.3.6、Element Plus 2.14.5 构建通过'],['MySQL','8.0.16 以上；utf8mb4；上海时区','8.0.46 全新 18 表和 16→18 表升级通过'],['浏览器','现代 Chrome 或 Edge','Chrome 152，V1.1 新页面 4 项通过'],['端口','应用 8080 数据库 3306 可配置','隔离验收使用 18081 和 13317']]),P('资源建议为 4 核 CPU、8 GB 内存和 SSD；这是小型教学部署建议，不是经测量的最低配置。')],
 [H('2 初始化与升级'),P('全新数据库执行 database/00-create-database.sql 和 database/01-schema.sql。脚本不含 DROP，非空库不要重复运行。V1.0 已有库先执行 mysqldump 备份并在独立恢复库验证，再执行一次 database/03-device-ingest-upgrade.sql；脚本只新增 device_credential 和 device_heartbeat_event。'),P('应用账号需要 smart_care 库的 SELECT、INSERT、UPDATE、DELETE 权限，运行时不需要 DDL 权限。升级脚本由具备建表权限的管理账号执行，密码交互输入，不写进命令行或仓库。'),P('升级后核对新增两表存在，并确认 elder、health_record、followup_plan、location_point 原有计数未变化，再启动 V1.1 JAR。'),H('3 必需环境变量'),T(['变量','用途'],[['DB_URL','jdbc:mysql://127.0.0.1:3306/smart_care?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Shanghai'],['DB_USERNAME DB_PASSWORD','专用应用数据库账号和密码'],['APP_ADMIN_PASSWORD','首次空账号库创建 admin，至少 12 字符、不超过 72 UTF-8 字节'],['SERVER_PORT SERVER_ADDRESS','默认 8080 与 127.0.0.1'],['SPRING_PROFILES_ACTIVE','demo 开放管理员模拟入口；prod 启用 HTTPS Cookie'],['APP_SEED_DEMO','仅 demo 配合 true，空老人库填充虚构数据']]),P('设备接入密钥由管理员在腕表页面签发，不配置为全局环境变量。生产通过 HTTPS 传递 X-Device-Key，数据库只保存摘要。')],
 [H('4 构建与运行'),P('在项目根目录 PowerShell 执行 .\\scripts\\build.ps1。脚本依次 npm ci、npm run build、复制静态资源、mvnw.cmd package；默认执行测试。若 Vite 占用 esbuild 且依赖未变化，可用 -SkipNpmInstall；若 Wrapper 在特殊 Windows 路径失败，脚本会改用 PATH 中的 mvn.cmd。首次构建需要访问 Maven 和 npm 仓库，后续受本机缓存影响。生成 target/dev-0.0.1-SNAPSHOT.jar。'),P('构建前先停止正在运行的同一 JAR。Windows 会锁定文件，未停止可能出现 repackage 无法重命名。前台启动可按 Ctrl+C；不要按进程名批量结束其他 Java 服务。'),P('演示启动：.\\scripts\\start.ps1 -Profile demo -Port 18080 -EnvironmentFile .\\.local\\demo-env.json。该命令在前台运行，浏览器打开 http://127.0.0.1:18080。账号 admin，密码来自自己设置的 APP_ADMIN_PASSWORD。演示空库还会创建 operator 与 analyst，初始演示密码相同，正式使用须分别更改。'),P('普通启动：.\\scripts\\start.ps1 -Profile local -Port 8080 -EnvironmentFile 私有JSON路径。已有账号库不会因修改 APP_ADMIN_PASSWORD 而重置密码；请通过管理员界面重置。'),H('5 演示数据选择'),P('推荐 Java DemoSeeder：空库首次 demo 启动生成 120 老人、6 医生、1080 健康记录、120 随访计划、18 设备、648 定位、1 围栏。时间相对启动时生成，便于答辩。database/02-demo-data.sql 为独立固定日期 SQL 种子，二者只能选一种，不能向非空业务库重复导入。'),P('本次 V1.1 验收在用户临时目录启动全新隔离库，数据库端口 13317、应用端口 18081。该实例仅用于自动化验证，凭据未写入交付物；重启机器或清理临时目录后需按上述步骤重新建立持久专用库。')],
 [H('6 开发与测试'),P('前后端分开开发：后端 .\\mvnw.cmd spring-boot:run，前端进入 frontend 执行 npm ci、npm run dev。Vite 默认将 /api 代理到 8080；如果后端改端口，同步修改 vite.config.js 的代理。交付 JAR 采用同源，无需 Vite 服务。'),P('独立测试：.\\mvnw.cmd test。测试使用 H2 与测试配置，不需要 MySQL 密码。IDEA 选择 Project SDK 17 和 Maven Runner JRE 17，刷新 Maven 后对 BusinessTests 或 DevApplicationTests 右键运行。使用 Boot BOM 管理的 JUnit 5 API、Engine 与 Launcher，避免手工加入 JUnit 6。'),P('实际 HTTP：用 Python 执行 scripts/smoke_http.py --base-url http://127.0.0.1:18080 --env-file 私有JSON路径。浏览器验证脚本需要 Playwright 与 Chrome；当前工具运行时由 RUNTIME_NODE_MODULES 指定包目录，此环境为本次开发自动化依赖，不是应用运行依赖。'),H('7 常见故障'),T(['现象','处理'],[['启动提示缺少 APP_ADMIN_PASSWORD','仅首次空账号库必须设置符合长度的密码'],['Access denied 或连接失败','核对 host 端口 库名 账号授权与服务状态'],['8080 已占用','更换 SERVER_PORT，不停止无关服务'],['JAR 无法重命名','先关闭本项目正在运行的 JAR，再 build'],['prod 登录后会话不保持','Secure Cookie 要求 HTTPS；本地 HTTP 用 local 或 demo'],['图表无数据或设备全离线','检查时间范围、模拟数据日期；5 分钟无新心跳转离线'],['Maven 依赖解析失败','检查仓库网络与 settings.xml，不混改 JUnit 单包版本']])],
 [H('8 运维与回滚'),P('数据库系统时区设为 +08:00，并与应用 Asia/Shanghai 保持一致。正式后台和设备接口均通过 HTTPS 反向代理；证书、域名、防火墙与公共发布尚未执行。设备密钥泄漏时由管理员立即轮换。'),P('备份示例：mysqldump --single-transaction --routines --triggers -h 127.0.0.1 -P 3306 -u 备份账号 -p --result-file=backup.sql smart_care。密码交互输入。先在独立恢复库验证，再执行升级。'),P('发布前保留旧 JAR、配置、SQL 版本和备份。V1.1 新增表不会阻止旧 JAR 运行，但回滚前仍须验证；当前没有自动迁移、自动备份或集群会话共享。'),H('9 部署验收记录'),T(['检查项','本次状态'],[['H2 18 表结构','28 项 Maven 测试通过'],['Vite 生产构建','通过'],['V1.1 MySQL HTTP','原 16 项冒烟与新增 5 项写路径通过'],['V1.1 MySQL 结构与升级','全新 18 表、16→18 表及哨兵保留通过'],['数据模拟页面','Chrome 4 项检查通过，含 390 像素窄屏'],['真实设备硬件','通用接口与密钥测试通过；厂商样机未联调'],['压力测试','V1.0 2664 次请求 0 错误'],['Word V1.1 页面渲染','10 份文档转 PDF 并逐页复核通过'],['JDK 17 IDEA 直接运行','IDEA 2024.3.4 已安装，待团队使用 JDK 17 实测']]),P('环境复核人、正式部署路径、目标服务器配置和签收日期：待团队实际部署后填写。')]
])

def setfont(run,size=None,bold=None,color='000000',east_asia='宋体'):
 run.font.name='Times New Roman'
 run._element.get_or_add_rPr().get_or_add_rFonts().set(qn('w:eastAsia'),east_asia)
 if size is not None:run.font.size=Pt(size)
 if bold is not None:run.bold=bold
 if color:run.font.color.rgb=RGBColor.from_string(color)

def set_shading(cell,fill):
 tcpr=cell._tc.get_or_add_tcPr();shd=tcpr.find(qn('w:shd'))
 if shd is None:shd=OxmlElement('w:shd');tcpr.append(shd)
 shd.set(qn('w:fill'),fill)

def set_cell_margins(cell,top=70,bottom=70,left=90,right=90):
 tcpr=cell._tc.get_or_add_tcPr();margins=tcpr.find(qn('w:tcMar'))
 if margins is None:margins=OxmlElement('w:tcMar');tcpr.append(margins)
 for edge,value in (('top',top),('bottom',bottom),('left',left),('right',right)):
  node=margins.find(qn('w:'+edge))
  if node is None:node=OxmlElement('w:'+edge);margins.append(node)
  node.set(qn('w:w'),str(value));node.set(qn('w:type'),'dxa')

def set_table_borders(tbl,color='B7B7B7',size='6'):
 tblpr=tbl._tbl.tblPr;borders=tblpr.find(qn('w:tblBorders'))
 if borders is None:borders=OxmlElement('w:tblBorders');tblpr.append(borders)
 for edge in ('top','left','bottom','right','insideH','insideV'):
  node=borders.find(qn('w:'+edge))
  if node is None:node=OxmlElement('w:'+edge);borders.append(node)
  node.set(qn('w:val'),'single');node.set(qn('w:sz'),size);node.set(qn('w:color'),color)

def no_table_borders(tbl):
 tblpr=tbl._tbl.tblPr;borders=tblpr.find(qn('w:tblBorders'))
 if borders is None:borders=OxmlElement('w:tblBorders');tblpr.append(borders)
 for edge in ('top','left','bottom','right','insideH','insideV'):
  node=OxmlElement('w:'+edge);node.set(qn('w:val'),'nil');borders.append(node)

def paragraph(container,text,style=None,size=10.5,align=None,bold=None,east_asia=None):
 p=container.add_paragraph(style=style) if style else container.add_paragraph()
 p.paragraph_format.space_after=Pt(6);p.paragraph_format.line_spacing=1.35
 if align is not None:p.alignment=align
 grid=OxmlElement('w:snapToGrid');grid.set(qn('w:val'),'0');p._p.get_or_add_pPr().append(grid)
 for lineidx,line in enumerate(str(text).split('\n')):
  if lineidx:p.add_run().add_break()
  r=p.add_run(line);setfont(r,size,bold,east_asia=east_asia or ('黑体' if style in ('Title','Heading 1','Heading 2') else '宋体'))
 return p

def table(container,heads,rows):
 tbl=container.add_table(rows=1,cols=len(heads));tbl.alignment=WD_TABLE_ALIGNMENT.CENTER;tbl.autofit=True
 set_table_borders(tbl)
 for row in rows:tbl.add_row()
 for ri,row in enumerate([heads]+rows):
  tr=tbl.rows[ri]._tr;props=tr.get_or_add_trPr();props.append(OxmlElement('w:cantSplit'))
  if ri==0:props.append(OxmlElement('w:tblHeader'))
  for ci,value in enumerate(row):
   cell=tbl.cell(ri,ci);cell.text=str(value);cell.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER;set_cell_margins(cell)
   if ri==0:set_shading(cell,'D9D9D9')
   elif ri%2==0:set_shading(cell,'F7F7F7')
   for p in cell.paragraphs:
    p.paragraph_format.space_before=Pt(1);p.paragraph_format.space_after=Pt(1);p.paragraph_format.line_spacing=1.15
    if ci==0 and len(heads)<=3:p.alignment=WD_ALIGN_PARAGRAPH.CENTER
    for r in p.runs:setfont(r,9,ri==0,east_asia='黑体' if ri==0 else '宋体')
 paragraph(container,'',size=2).paragraph_format.space_after=Pt(0)
 return tbl

def configure_document(doc):
 sec=doc.sections[0];sec.page_width=Cm(21);sec.page_height=Cm(29.7)
 sec.top_margin=Cm(2.54);sec.bottom_margin=Cm(2.54);sec.left_margin=Cm(2.54);sec.right_margin=Cm(2.54)
 sec.header_distance=Cm(1.25);sec.footer_distance=Cm(1.25)
 normal=doc.styles['Normal'];normal.font.name='Times New Roman';normal.font.size=Pt(10.5);normal._element.rPr.rFonts.set(qn('w:eastAsia'),'宋体')
 for name,size in (('Title',26),('Heading 1',16),('Heading 2',13)):
  style=doc.styles[name];style.font.name='Times New Roman';style.font.size=Pt(size);style.font.bold=True;style.font.color.rgb=RGBColor(0,0,0);style._element.rPr.rFonts.set(qn('w:eastAsia'),'黑体')
  style.paragraph_format.keep_with_next=True
  ppr=style._element.get_or_add_pPr();border=ppr.find(qn('w:pBdr'))
  if border is not None:ppr.remove(border)
 settings=doc.settings.element
 update=OxmlElement('w:updateFields');update.set(qn('w:val'),'true');settings.append(update)
 footer=sec.footer.paragraphs[0];footer.alignment=WD_ALIGN_PARAGRAPH.CENTER
 r=footer.add_run(PROJECT+'  '+VERSION+'  ');setfont(r,8,color='666666')
 fld=OxmlElement('w:fldSimple');fld.set(qn('w:instr'),'PAGE');footer._p.append(fld)

def add_cover(doc,spec):
 outer=doc.add_table(rows=1,cols=2);outer.alignment=WD_TABLE_ALIGNMENT.CENTER;outer.autofit=False;no_table_borders(outer)
 outer.columns[0].width=Cm(2.5);outer.columns[1].width=Cm(13.2)
 row=outer.rows[0];row.height=Cm(21.8);row.height_rule=WD_ROW_HEIGHT_RULE.EXACTLY
 left,right=row.cells;left.width=Cm(2.5);right.width=Cm(13.2)
 set_shading(left,'D9D9D9');left.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.BOTTOM;right.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.TOP
 set_cell_margins(left,100,160,110,110);set_cell_margins(right,20,20,240,80)
 lp=left.paragraphs[0];lp.alignment=WD_ALIGN_PARAGRAPH.LEFT
 lr=lp.add_run(f'分  类：项目交付文档\n使用者：项目组\n文档编号：\n{PROJECT_NO}-{spec["number"]}\n\n{DEPARTMENT}')
 setfont(lr,7.5,east_asia='宋体')
 right.paragraphs[0].paragraph_format.space_after=Pt(0)
 top=right.add_table(rows=3,cols=2);top.alignment=WD_TABLE_ALIGNMENT.RIGHT;top.autofit=False;set_table_borders(top,'8A8A8A','5')
 for i,label in enumerate(('卷  号','卷内编号','密  级')):
  top.cell(i,0).text=label;top.cell(i,1).text=''
  top.cell(i,0).width=Cm(2.1);top.cell(i,1).width=Cm(3.6)
  for c in top.rows[i].cells:
   set_cell_margins(c,20,20,50,50)
   for p in c.paragraphs:
    p.alignment=WD_ALIGN_PARAGRAPH.CENTER
    for r in p.runs:setfont(r,7.5,bold=(c is top.cell(i,0)),east_asia='宋体')
 for _ in range(4):paragraph(right,'',size=4).paragraph_format.space_after=Pt(6)
 p=paragraph(right,'智慧医养大数据公共服务平台\n大数据决策分析系统',size=19,align=WD_ALIGN_PARAGRAPH.CENTER,bold=True,east_asia='黑体');p.paragraph_format.space_after=Pt(20)
 p=paragraph(right,spec['title'],style='Title',size=25,align=WD_ALIGN_PARAGRAPH.CENTER,bold=True,east_asia='黑体');p.paragraph_format.space_after=Pt(28)
 for label,value in (('项 目 承 担 部 门',DEPARTMENT),('撰 写 人（签名）',spec['owner']),('完 成 日 期',DATE),('本文档使用部门','■项目组  □主管领导  □维护人员  □用户'),('评审负责人（签名）','待项目组评审'),('评 审 日 期','待填写')):
  p=paragraph(right,f'{label}：  {value}',size=10.5,east_asia='宋体');p.paragraph_format.space_after=Pt(8)
 doc.add_page_break()

def add_control_pages(doc,spec):
 p=paragraph(doc,'文档信息',style='Heading 1',size=16,align=WD_ALIGN_PARAGRAPH.CENTER,bold=True,east_asia='黑体');p.paragraph_format.space_after=Pt(14)
 table(doc,['文档信息','内容'],[['标题',PROJECT+' '+spec['title']],['作者',spec['owner']],['创建日期',DATE],['上次更新日期',DATE],['版本',VERSION],['部门名称',DEPARTMENT],['适用范围',spec['scope']]])
 p=paragraph(doc,'修订文档历史记录',style='Heading 1',size=16,align=WD_ALIGN_PARAGRAPH.CENTER,bold=True,east_asia='黑体');p.paragraph_format.space_before=Pt(18)
 table(doc,['日期','版本','说明','作者'],[[DATE,VERSION,'按三创谷交付模板重排并与当前代码和验证证据同步','智慧医养项目组']])
 doc.add_page_break()
 p=paragraph(doc,'目  录',style='Heading 1',size=16,align=WD_ALIGN_PARAGRAPH.CENTER,bold=True,east_asia='黑体');p.paragraph_format.space_after=Pt(14)
 seen=set()
 for page in spec['pages']:
  for block in page:
   if block[0]!='h' or block[1] in seen:continue
   seen.add(block[1]);entry=paragraph(doc,block[1],size=10.5,east_asia='宋体')
   entry.paragraph_format.left_indent=Cm(0.65 if re.match(r'^\d+\.\d+',block[1]) else 0)
   entry.paragraph_format.space_after=Pt(4)

def add_figure(doc,path,caption,width):
 p=doc.add_paragraph();p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.keep_with_next=True
 p.add_run().add_picture(str(ROOT/path),width=Inches(width))
 cp=paragraph(doc,caption,size=9,align=WD_ALIGN_PARAGRAPH.CENTER,east_asia='宋体');cp.paragraph_format.keep_with_next=True;cp.paragraph_format.space_after=Pt(10)

def build(spec):
 doc=Document();configure_document(doc);add_cover(doc,spec);add_control_pages(doc,spec)
 for page in spec['pages']:
  first_block=True
  for block in page:
   if block[0]=='h':
    p=paragraph(doc,block[1],'Heading 1',16,bold=True,east_asia='黑体');p.paragraph_format.space_before=Pt(8);p.paragraph_format.space_after=Pt(7);p.paragraph_format.keep_with_next=True
    if first_block:p.paragraph_format.page_break_before=True
   elif block[0]=='p':paragraph(doc,block[1])
   elif block[0]=='t':table(doc,block[1],block[2])
   elif block[0]=='img':add_figure(doc,block[1],block[2],block[3])
   first_block=False
 doc.core_properties.title=PROJECT+' '+spec['title'];doc.core_properties.author='智慧医养项目组';doc.core_properties.subject=spec['scope'];doc.core_properties.comments='依据用户提供的三创谷格式模板重排，内容以当前仓库实现和验证证据为准。'
 out=OUT/(spec['number']+' '+spec['title']+'.docx');doc.save(out)
 md=['# '+spec['title'],'版本 '+VERSION+'　'+DATE]
 for page in spec['pages']:
  for b in page:
   if b[0]=='h':md+=['','## '+b[1]]
   elif b[0]=='p':md+=['',b[1]]
   elif b[0]=='t':md+=['','| '+' | '.join(b[1])+' |','| '+' | '.join(['---']*len(b[1]))+' |']+['| '+' | '.join(map(str,r))+' |' for r in b[2]]
   elif b[0]=='img':md+=['',f'![{b[2]}]({b[1]})','',b[2]]
 (SOURCE/(spec['number']+'-'+spec['title']+'.md')).write_text('\n'.join(md)+'\n',encoding='utf-8')
 print(out.name)
import sys
for spec in docs:
 if len(sys.argv)==1 or spec['number'] in sys.argv[1:]:build(spec)
(ROOT/'docs/.qa/document-manifest.json').write_text(json.dumps([{'file':s['number']+' '+s['title']+'.docx','plannedPages':3+len(s['pages'])} for s in docs],ensure_ascii=False,indent=2),encoding='utf-8')
