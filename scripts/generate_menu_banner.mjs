/**
 * メニュー画面のヘッダー画像を生成する。
 *
 * 入力: `assets/feature-graphic.jpg`（Play のフィーチャーグラフィックと同じ原画）
 * 出力: `app/src/main/res/drawable-nodpi/menu_banner.webp`
 *
 * 原画の縦横比（1024×495）を保ったまま WebP にする。アプリ内で幅いっぱいに拡大縮小されるため、
 * 密度別のリソースは持たず `drawable-nodpi` に置く。
 */

import { createCanvas, loadImage } from '@napi-rs/canvas';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const SOURCE = path.join('assets', 'feature-graphic.jpg');
const OUT_DIR = path.join('app', 'src', 'main', 'res', 'drawable-nodpi');
const OUT = path.join(OUT_DIR, 'menu_banner.webp');
const QUALITY = 85;

const img = await loadImage(SOURCE);
const canvas = createCanvas(img.width, img.height);
canvas.getContext('2d').drawImage(img, 0, 0);

await mkdir(OUT_DIR, { recursive: true });
const buffer = canvas.toBuffer('image/webp', QUALITY);
await writeFile(OUT, buffer);
console.log(`✓ ${OUT} (${img.width}×${img.height}, ${(buffer.length / 1024).toFixed(1)} KiB)`);
