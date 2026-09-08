"""Read-only API smoke checks against an already-running local demo.
Credentials are read from an environment variable or an explicitly provided private file.
No credentials or cookies are written to the test report.
"""
import argparse, http.cookiejar, json, os, time, urllib.request, urllib.error
from datetime import datetime, timezone, timedelta
from pathlib import Path

parser=argparse.ArgumentParser()
parser.add_argument('--base-url',default='http://127.0.0.1:18080')
parser.add_argument('--env-file')
parser.add_argument('--output',default='docs/evidence/http-smoke.json')
args=parser.parse_args()
password=os.environ.get('APP_TEST_PASSWORD')
if args.env_file: password=json.loads(Path(args.env_file).read_text(encoding='utf-8-sig'))['APP_ADMIN_PASSWORD']
if not password: raise SystemExit('Set APP_TEST_PASSWORD or --env-file.')
jar=http.cookiejar.CookieJar();client=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
def request(path,method='GET',body=None,token=None):
    headers={'Content-Type':'application/json'}
    if token: headers[token['headerName']]=token['token']
    r=urllib.request.Request(args.base_url+'/api/v1'+path,data=None if body is None else json.dumps(body).encode(),headers=headers,method=method)
    with client.open(r,timeout=30) as response:return json.load(response)
token=request('/auth/csrf')['data']
request('/auth/login','POST',{'username':'admin','password':password},token)
token=request('/auth/csrf')['data']
results=[]
for path in ['/auth/me','/regions','/elders?size=10','/doctors?size=10','/health-records?size=10','/followup-plans?size=10','/devices/distribution','/geofences','/geofence-alerts?size=10','/statistics/population','/statistics/health','/statistics/followups','/dashboard','/users?size=10']:
    began=time.perf_counter()
    try:
        response=request(path);assert response['code']=='OK';results.append({'path':path,'passed':True,'milliseconds':round((time.perf_counter()-began)*1000,2)})
    except Exception as e:results.append({'path':path,'passed':False,'error':str(e)})
device=request('/devices/distribution')['data'][0]
if device['location']:
    stamp=datetime.fromisoformat(device['location']['recordedAt']);start=stamp.replace(hour=0,minute=0,second=0,microsecond=0)
    query=urllib.parse.urlencode({'start':start.isoformat(),'end':(start+timedelta(days=1)).isoformat()})
    points=request(f"/elders/{device['elderId']}/trajectory?{query}")['data']
    assert all(points[i]['recordedAt']<=points[i+1]['recordedAt'] for i in range(len(points)-1))
    results.append({'path':'/elders/{id}/trajectory','passed':True,'points':len(points)})
request('/auth/logout','POST',None,token)
try:request('/auth/me');results.append({'path':'logout-session-invalidated','passed':False})
except urllib.error.HTTPError as e:results.append({'path':'logout-session-invalidated','passed':e.code==401})
output=Path(args.output);output.parent.mkdir(parents=True,exist_ok=True)
output.write_text(json.dumps({'runAt':datetime.now(timezone(timedelta(hours=8))).isoformat(),'baseUrl':args.base_url,'results':results},ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'passed':sum(x['passed'] for x in results),'total':len(results),'report':str(output)},ensure_ascii=False))
raise SystemExit(0 if all(x['passed'] for x in results) else 1)
