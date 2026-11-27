// 分块下载（2MB Range + Connection: close）——长连接会卡死，故短连接逐块续传
import { appendFile, writeFile, stat, mkdir } from 'node:fs/promises';

const CHUNK = 2 * 1048576;
const OUT = '.toolchain/downloads';
await mkdir(OUT, { recursive: true });

const sizeOf = async p => { try { return (await stat(p)).size; } catch { return 0; } };

async function fetchChunk(url, start, end, tries = 8) {
  for (let i = 0; i < tries; i++) {
    try {
      const r = await fetch(url, {
        headers: { Range: `bytes=${start}-${end}`, Connection: 'close' },
        signal: AbortSignal.timeout(25000),
      });
      if (r.status !== 206 && r.status !== 200) throw new Error(`HTTP ${r.status}`);
      const buf = Buffer.from(await r.arrayBuffer());
      if (!buf.length) throw new Error('empty');
      return buf;
    } catch (e) {
      if (i === tries - 1) throw e;
      await new Promise(res => setTimeout(res, 800));
    }
  }
}

async function totalSize(url) {
  const r = await fetch(url, { headers: { Range: 'bytes=0-0', Connection: 'close' } });
  const cr = r.headers.get('content-range');
  return cr ? Number(cr.split('/')[1]) : Number(r.headers.get('content-length') || 0);
}

async function download(name, url) {
  const file = `${OUT}/${name}`;
  const total = await totalSize(url);
  if (!total) { console.log(`[fail] ${name}: no size`); return; }
  let have = await sizeOf(file);
  if (have >= total) { console.log(`[skip] ${name}`); return; }
  if (!have) await writeFile(file, Buffer.alloc(0));
  const t0 = Date.now();
  let lastLog = 0;
  while (have < total) {
    const end = Math.min(have + CHUNK, total) - 1;
    const buf = await fetchChunk(url, have, end);
    await appendFile(file, buf);
    have += buf.length;
    if (have - lastLog >= 16 * 1048576 || have >= total) {
      lastLog = have;
      const sp = have / 1048576 / ((Date.now() - t0) / 1000);
      console.log(`[${name}] ${Math.floor(have / total * 100)}% ${(have/1048576).toFixed(0)}/${(total/1048576).toFixed(0)}MB ${sp.toFixed(2)}MB/s`);
    }
  }
  console.log(`[ok] ${name} ${(have/1048576).toFixed(1)}MB in ${((Date.now()-t0)/1000).toFixed(0)}s`);
}

const jobs = [
  ['jdk21.zip', 'https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/x64/windows/OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip'],
  ['platform-36.zip', 'https://mirrors.cloud.tencent.com/AndroidSDK/platform-36_r02.zip'],
  ['build-tools-r36.zip', 'https://mirrors.cloud.tencent.com/AndroidSDK/build-tools_r36_windows.zip'],
  ['platform-tools.zip', 'https://mirrors.cloud.tencent.com/AndroidSDK/platform-tools_r37.0.3-win.zip'],
  ['gradle-8.14.5-bin.zip', 'https://mirrors.cloud.tencent.com/gradle/gradle-8.14.5-bin.zip'],
];

for (const [name, url] of jobs) {
  try { await download(name, url); } catch (e) { console.log(`[fail] ${name} ${e.message}`); }
}
console.log('ALL DONE');
