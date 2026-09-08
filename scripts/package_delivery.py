"""Package a shareable delivery using an explicit allowlist, without local credentials/caches."""
from pathlib import Path
import zipfile,json,hashlib
root=Path(__file__).resolve().parents[1];out=root/'release';out.mkdir(exist_ok=True)
selected=[]
for name in ['README.md','PROGRESS.md','pom.xml','mvnw','mvnw.cmd','.gitignore','.gitattributes','.env.example']:
 selected.append(root/name)
for folder in ['.mvn','src','frontend','database','scripts','docs']:
 for p in (root/folder).rglob('*'):
  if not p.is_file():continue
  rel=p.relative_to(root);parts=rel.parts
  if any(x in parts for x in ['node_modules','dist','.qa','__pycache__','review']):continue
  if str(rel).replace('\\','/').startswith('src/main/resources/static/'):continue
  if p.name=='trajectory-debug.png':continue
  selected.append(p)
selected.append(root/'target/dev-0.0.1-SNAPSHOT.jar')
private=root/'.local/demo-env.json'
if private.exists():
 secrets=[v.encode() for k,v in json.loads(private.read_text(encoding='utf-8-sig')).items() if ('PASSWORD' in k) and isinstance(v,str) and v]
 for p in selected:
  if any(secret in p.read_bytes() for secret in secrets):raise RuntimeError('Private credential detected in selected delivery file: '+str(p.relative_to(root)))
package=out/'smart-care-v1.0.zip'
with zipfile.ZipFile(package,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 for p in sorted(selected):z.write(p,Path('smart-care')/p.relative_to(root))
with zipfile.ZipFile(package) as z:
 assert z.testzip() is None
 assert len([x for x in z.namelist() if x.endswith('.docx')])==10
 assert len([x for x in z.namelist() if x.endswith('.pptx')])==1
 assert not any('/.local/' in x or '/.qa/' in x or '/node_modules/' in x for x in z.namelist())
sha=hashlib.sha256(package.read_bytes()).hexdigest();(out/'SHA256SUMS.txt').write_text(sha+'  '+package.name+'\n',encoding='utf-8')
print(json.dumps({'file':str(package),'files':len(selected),'bytes':package.stat().st_size,'sha256':sha},ensure_ascii=False))
