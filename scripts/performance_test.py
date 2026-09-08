"""Bounded load test. Only permits the task-owned isolated MySQL instance on 13316.
Adds uniquely marked synthetic rows and removes only those rows in finally.
Requires an already running demo and private credential files; never logs secrets.
"""
import argparse, concurrent.futures, http.cookiejar, json, os, platform, statistics, subprocess, time, urllib.request, uuid
from datetime import datetime
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--seconds',type=int,default=60);p.add_argument('--workers',type=int,default=20);a=p.parse_args()
root=Path(__file__).resolve().parents[1];private=json.loads((root/'.local/demo-env.json').read_text(encoding='utf-8-sig'))
assert '127.0.0.1:13316/smart_care' in private['DB_URL'], 'Only the isolated task database is allowed'
cnf=Path(os.environ['TEMP'])/'smart-care-mysql-01a07b9c/client.cnf';prefix='PERF_'+uuid.uuid4().hex
def sql(s):
 r=subprocess.run(['D:/Mysql/bin/mysql.exe','--defaults-extra-file='+str(cnf),'--batch','--skip-column-names','--default-character-set=utf8mb4','smart_care'],input=s.encode(),capture_output=True)
 if r.returncode:raise RuntimeError(r.stderr.decode(errors='replace'))
 return r.stdout.decode().strip()
jar=http.cookiejar.CookieJar();opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar));base='http://127.0.0.1:18080/api/v1'
def call(path,body=None,token=None):
 headers={'Content-Type':'application/json'}
 if token:headers[token['headerName']]=token['token']
 with opener.open(urllib.request.Request(base+path,data=None if body is None else json.dumps(body).encode(),headers=headers),timeout=45) as r:return json.load(r)
t=call('/auth/csrf')['data'];call('/auth/login',{'username':'admin','password':private['APP_ADMIN_PASSWORD']},t)
cookie='; '.join(c.name+'='+c.value for c in jar)
before=sql('SELECT (SELECT COUNT(*) FROM health_record),(SELECT COUNT(*) FROM location_point);')
report={'runAt':datetime.now().astimezone().isoformat(),'platform':platform.platform(),'cpuLogicalCount':os.cpu_count(),'database':'MySQL 8.0.46 isolated 13316','java':'Zulu 25.0.1 / release 17','workers':a.workers,'requestedSeconds':a.seconds,'before':before,'addedPerTable':100000,'conditions':'Default rolling 30 day range, first page size 20, no region filter. Synthetic rows spread over 27.8 hours. Read endpoints only; no write throughput claim.'}
try:
 digits='(SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9)'
 numbers=f'SELECT a.n+10*b.n+100*c.n+1000*d.n+10000*e.n n FROM {digits} a CROSS JOIN {digits} b CROSS JOIN {digits} c CROSS JOIN {digits} d CROSS JOIN {digits} e'
 sql(f"INSERT INTO health_record(elder_id,measured_at,heart_rate,abnormal,rule_snapshot,source,event_id) SELECT (SELECT MIN(id) FROM elder),DATE_SUB(NOW(6),INTERVAL n SECOND),75,FALSE,'[]','PERF',CONCAT('{prefix}_',n) FROM ({numbers}) nums;".replace("CONCAT('{prefix}_',n)",f"CONCAT('{prefix}_',n)"))
 sql(f"INSERT INTO location_point(device_id,binding_id,elder_id,event_id,longitude,latitude,recorded_at,received_at) SELECT b.device_id,b.id,b.elder_id,CONCAT('{prefix}_',n),120.16,30.25,DATE_SUB(NOW(6),INTERVAL n SECOND),NOW(6) FROM ({numbers}) nums CROSS JOIN (SELECT * FROM device_binding ORDER BY id LIMIT 1) b;")
 report['during']=sql('SELECT (SELECT COUNT(*) FROM health_record),(SELECT COUNT(*) FROM location_point);')
 endpoints=['/health-records?size=20','/statistics/health','/dashboard','/devices/distribution']
 for path in endpoints:call(path)
 deadline=time.perf_counter()+a.seconds
 def worker(i):
  out=[];j=i
  while time.perf_counter()<deadline:
   path=endpoints[j%len(endpoints)];j+=1;start=time.perf_counter();ok=False;error=None
   try:
    with urllib.request.urlopen(urllib.request.Request(base+path,headers={'Cookie':cookie}),timeout=45) as r:ok=r.status==200 and json.load(r)['code']=='OK'
   except Exception as e:error=type(e).__name__
   out.append((path,(time.perf_counter()-start)*1000,ok,error))
  return out
 start=time.perf_counter()
 with concurrent.futures.ThreadPoolExecutor(max_workers=a.workers) as pool:results=[x for batch in pool.map(worker,range(a.workers)) for x in batch]
 report['elapsedSeconds']=round(time.perf_counter()-start,3);report['endpoints']=[]
 for path in endpoints:
  rows=[r for r in results if r[0]==path];values=sorted(r[1] for r in rows);p95=values[max(0,__import__('math').ceil(len(values)*.95)-1)];errors=sum(not r[2] for r in rows)
  report['endpoints'].append({'path':path,'samples':len(rows),'p95Milliseconds':round(p95,2),'meanMilliseconds':round(statistics.mean(values),2),'errors':errors,'passed':p95<=2000 and errors/len(rows)<.01})
 report['totalRequests']=len(results);report['errors']=sum(not r[2] for r in results);report['passed']=all(r['passed'] for r in report['endpoints'])
finally:
 sql(f"DELETE FROM location_point WHERE event_id LIKE '{prefix}%'; DELETE FROM health_record WHERE source='PERF' AND event_id LIKE '{prefix}%';")
 report['after']=sql('SELECT (SELECT COUNT(*) FROM health_record),(SELECT COUNT(*) FROM location_point);');report['cleanupVerified']=report['after']==before
 (root/'docs/evidence/performance.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))

