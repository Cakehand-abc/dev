import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
const modules=process.env.RUNTIME_NODE_MODULES;
if(!modules) throw new Error('Set RUNTIME_NODE_MODULES to the provided runtime packages directory.');
const { chromium }=createRequire(path.join(modules,'package.json'))('playwright');
const root=path.resolve(import.meta.dirname,'..');
const credentials=JSON.parse((await fs.readFile(path.join(root,'.local/demo-env.json'),'utf8')).replace(/^\uFEFF/,''));
const pptOut=path.join(root,'docs/.qa/ppt-shots');await fs.mkdir(pptOut,{recursive:true});
const out=path.join(root,'docs/evidence/screenshots');await fs.mkdir(out,{recursive:true});
const browser=await chromium.launch({channel:'chrome',headless:true});
const page=await browser.newPage({viewport:{width:1440,height:1000},deviceScaleFactor:1});
const errors=[];const results=[];
page.on('pageerror',error=>errors.push(error.message));
page.on('response',response=>{if(response.url().includes('/api/')&&response.status()>=500)errors.push(`${response.status()} ${response.url()}`)});
await page.goto(process.env.APP_URL||'http://127.0.0.1:18080');
await page.getByLabel('用户名',{exact:true}).waitFor();
await page.screenshot({path:path.join(out,'01-login.png'),fullPage:true});
await page.getByLabel('用户名',{exact:true}).fill('admin');await page.getByLabel('密码',{exact:true}).fill(credentials.APP_ADMIN_PASSWORD);
await page.getByRole('button',{name:'登录平台 →'}).click();
await page.getByRole('heading',{name:'医养服务运行总览'}).waitFor();
await page.waitForLoadState('networkidle');
const pages=[['home','首页总览'],['population','人口信息分析'],['health','健康检测分析'],['followup','医生随访分析'],['devices','腕表分布'],['fences','电子围栏'],['trajectory','轨迹回放'],['doctors','医生管理'],['users','账号管理']];
for(let i=0;i<pages.length;i++){
 const [id,name]=pages[i];await page.locator('nav').getByRole('button',{name,exact:true}).click();await page.waitForLoadState('networkidle');
 if(id==='trajectory'){
  const latest=await page.evaluate(async()=>{const data=await(await fetch('/api/v1/devices/distribution')).json();const device=data.data.find(x=>x.location);return device?{elderId:String(device.elderId),date:device.location.recordedAt.slice(0,10)}:null});
  if(latest){
   await page.locator('.filterbar label').filter({hasText:/^老人/}).locator('select').selectOption(latest.elderId);
   await page.getByLabel('回放日期',{exact:true}).fill(latest.date);
   const [response]=await Promise.all([page.waitForResponse(r=>r.url().includes(`/elders/${latest.elderId}/trajectory?`)),page.getByRole('button',{name:'查询',exact:true}).click()]);
   const body=await response.json();if(!body.data?.length)throw new Error('Seed trajectory should contain points');
   await page.waitForFunction(()=>Array.from(document.querySelectorAll('button')).some(b=>b.textContent==='播放'&&!b.disabled));
  }
 }
 const hasError=await page.locator('.content > .message.error').count();results.push({page:id,passed:!hasError});
 await page.screenshot({path:path.join(pptOut,`${String(i+2).padStart(2,'0')}-${id}.png`)});
 await page.screenshot({path:path.join(out,`${String(i+2).padStart(2,'0')}-${id}.png`),fullPage:true});
}
await page.locator('nav').getByRole('button',{name:'人口信息分析',exact:true}).click();await page.waitForLoadState('networkidle');
await page.getByRole('button',{name:'＋ 新增档案',exact:true}).click();await page.getByRole('dialog').getByRole('heading',{name:'新增老人档案'}).waitFor();
await page.screenshot({path:path.join(out,'11-elder-dialog.png')});await page.getByRole('button',{name:'取消',exact:true}).click();
results.push({page:'elder-create-dialog',passed:true});
await page.setViewportSize({width:390,height:844});await page.screenshot({path:path.join(out,'12-mobile.png'),fullPage:true});
results.push({page:'mobile-horizontal-overflow',passed:await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)});
await page.getByRole('button',{name:'退出',exact:true}).click();await page.getByRole('heading',{name:'登录工作台'}).waitFor();
await page.getByLabel('用户名',{exact:true}).fill('analyst');await page.getByLabel('密码',{exact:true}).fill(credentials.APP_ADMIN_PASSWORD);await page.getByRole('button',{name:'登录平台 →'}).click();await page.waitForLoadState('networkidle');
await page.locator('nav').getByRole('button',{name:'人口信息分析',exact:true}).click();await page.waitForLoadState('networkidle');
results.push({page:'analyst-no-edit',passed:await page.getByRole('button',{name:'＋ 新增档案',exact:true}).count()===0});
await fs.writeFile(path.join(root,'docs/evidence/browser-qa.json'),JSON.stringify({runAt:new Date().toISOString(),browser:await browser.version(),results,errors},null,2));
console.log(JSON.stringify({results,errors}));await browser.close();
if(errors.length||results.some(r=>!r.passed))process.exitCode=1;
