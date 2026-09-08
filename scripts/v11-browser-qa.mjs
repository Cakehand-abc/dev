import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';

const modules = process.env.RUNTIME_NODE_MODULES;
const password = process.env.APP_TEST_PASSWORD;
if (!modules) throw new Error('Set RUNTIME_NODE_MODULES.');
if (!password) throw new Error('Set APP_TEST_PASSWORD.');
const { chromium } = createRequire(path.join(modules, 'package.json'))('playwright');
const root = path.resolve(import.meta.dirname, '..');
const output = path.join(root, 'docs/evidence/v11-screenshots');
await fs.mkdir(output, { recursive: true });

const browser = await chromium.launch({ channel: 'chrome', headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
const errors = [];
page.on('pageerror', error => errors.push(error.message));
page.on('response', response => {
  if (response.url().includes('/api/') && response.status() >= 500) errors.push(`${response.status()} ${response.url()}`);
});

await page.goto(process.env.APP_URL || 'http://127.0.0.1:18081');
await page.getByLabel('用户名', { exact: true }).fill('admin');
await page.getByLabel('密码', { exact: true }).fill(password);
await page.getByRole('button', { name: '登录平台 →' }).click();
await page.getByRole('heading', { name: '医养服务运行总览' }).waitFor();

const results = [];
await page.locator('nav').getByRole('button', { name: '数据模拟', exact: true }).click();
await page.getByRole('heading', { name: '定位数据模拟' }).waitFor();
const count = page.getByLabel('定位点数');
await count.fill('4');
await count.press('Tab');
await page.getByRole('button', { name: '注入 4 个定位点' }).click();
await page.getByText(/注入完成：接收 4 点，新增 4 点/).waitFor();
results.push({ name: '数据模拟页面可预览并注入四点轨迹', passed: true });
await page.screenshot({ path: path.join(output, '01-simulation.png'), fullPage: true });

await Promise.all([
  page.waitForResponse(response => response.url().includes('/api/v1/elders?')),
  page.locator('nav').getByRole('button', { name: '人口信息分析', exact: true }).click(),
]);
results.push({ name: '管理员可见删除误录入口', passed: await page.getByRole('button', { name: '删除误录' }).count() > 0 });
await page.screenshot({ path: path.join(output, '02-restricted-delete.png'), fullPage: true });

await Promise.all([
  page.waitForResponse(response => response.url().includes('/api/v1/devices/distribution')),
  page.locator('nav').getByRole('button', { name: '腕表分布', exact: true }).click(),
]);
results.push({ name: '管理员可见设备接入密钥入口', passed: await page.getByRole('button', { name: '接入密钥' }).count() > 0 });
await page.screenshot({ path: path.join(output, '03-device-credential.png'), fullPage: true });

await page.setViewportSize({ width: 390, height: 844 });
await Promise.all([
  page.waitForResponse(response => response.url().includes('/api/v1/devices/distribution')),
  page.waitForResponse(response => response.url().includes('/api/v1/geofences')),
  page.locator('nav').getByRole('button', { name: '数据模拟', exact: true }).click(),
]);
await page.getByRole('heading', { name: '定位数据模拟' }).waitFor();
results.push({ name: '窄屏数据模拟页面无横向溢出', passed: await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth) });
await page.screenshot({ path: path.join(output, '04-simulation-mobile.png'), fullPage: true });

const report = { runAt: new Date().toISOString(), browser: await browser.version(), results, errors };
await fs.writeFile(path.join(root, 'docs/evidence/v11-browser-qa.json'), JSON.stringify(report, null, 2));
console.log(JSON.stringify(report));
await browser.close();
if (errors.length || results.some(result => !result.passed)) process.exitCode = 1;
