"""Verify the exported seed in a fresh schema on the task-owned temporary MySQL."""
import json,os,subprocess,uuid,re
from pathlib import Path
from datetime import datetime
root=Path(__file__).resolve().parents[1];env=json.loads((root/'.local/demo-env.json').read_text(encoding='utf-8-sig'))
assert '127.0.0.1:13316/smart_care' in env['DB_URL']
name='verify_'+uuid.uuid4().hex[:10]+'_'
private=Path(os.environ['TEMP'])/'smart-care-mysql-01a07b9c/client.cnf'
schema=(root/'database/01-schema.sql').read_text(encoding='utf-8')
tables=re.findall(r'CREATE TABLE (\w+)',schema)
constraints=re.findall(r'CONSTRAINT (\w+)',schema)
def rewrite(text):
 for token in sorted(tables+constraints+['seed_smart_care_demo_v1'],key=len,reverse=True):text=re.sub(r'\b'+re.escape(token)+r'\b',name+token,text)
 return text
def sql(text,db=None):
 args=['D:/Mysql/bin/mysql.exe','--defaults-extra-file='+str(private),'--batch','--skip-column-names']
 args+=['smart_care']
 return subprocess.run(args,input=text.encode(),capture_output=True)
try:
 r=sql(rewrite(schema));assert r.returncode==0,r.stderr.decode(errors='replace')
 seed=rewrite((root/'database/02-demo-data.sql').read_text(encoding='utf-8'))
 first=sql(seed);assert first.returncode==0,first.stderr.decode(errors='replace')
 query='SELECT (SELECT COUNT(*) FROM elder),(SELECT COUNT(*) FROM health_record),(SELECT COUNT(*) FROM location_point),(SELECT COUNT(*) FROM sys_user);'
 counts=sql(rewrite(query)).stdout.decode().strip();assert counts=='120\t1080\t648\t0',counts
 second=sql(seed);assert second.returncode!=0 and b'Demo seed requires empty business tables' in second.stderr
 assert sql(rewrite(query)).stdout.decode().strip()==counts
 result={'runAt':datetime.now().astimezone().isoformat(),'database':'smart_care on isolated 13316, unique temporary table prefix '+name,'schemaTables':16,'firstImport':'passed','counts':{'elders':120,'healthRecords':1080,'locationPoints':648,'users':0},'secondImport':'rejected nonempty business tables','dataUnchanged':True,'passed':True}
 (root/'docs/evidence/sql-seed.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
 print(json.dumps(result,ensure_ascii=False))
finally:
 # Delete only names generated in this invocation; original business tables are never targets.
 assert re.fullmatch(r'verify_[a-f0-9]{10}_',name)
 drop_order=['geofence_state']+[t for t in reversed(tables) if t!='geofence_state']
 for table in drop_order:
  result=sql('DROP TABLE IF EXISTS `'+name+table+'`;')
  if result.returncode:raise RuntimeError(result.stderr.decode(errors='replace'))
 sql('DROP PROCEDURE IF EXISTS `'+name+'seed_smart_care_demo_v1`;')
