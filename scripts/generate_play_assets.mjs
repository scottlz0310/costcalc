/**
 * Google Play ストア掲載用のグラフィック素材を生成する。
 *
 * 入力（`assets/`）:
 *   - feature-graphic.jpg       … 任意サイズ。1024×500 へ引き伸ばす
 *   - screenshot/*.png          … 端末で撮影したスクリーンショット
 *
 * 出力（`assets/play/`）:
 *   - feature-graphic.jpg       … 1024×500 JPEG（Play の要求仕様）
 *   - screenshot/*.png          … 1440×2560（9:16）
 *
 * スクリーンショットの変換内容:
 *   1. 下端のシステムナビゲーションバー（OS の描画領域でアプリの一部ではない）を切り落とす
 *   2. 9:16 のキャンバス中央上寄せで配置し、余白をアプリの背景色で埋める
 *
 * Play は「長辺が短辺の2倍を超えてはならない」と定めており、撮影元の 1096×2560
 * （2.34倍）はそのままでは受け付けられない。9:16 に整えることで、この制約と
 * 「おすすめ掲載の対象条件（9:16 かつ短辺 1080px 以上）」の双方を満たす。
 */

import { createCanvas, loadImage } from '@napi-rs/canvas';
import { mkdir, readdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const ASSETS = 'assets';
const OUT = path.join(ASSETS, 'play');

const FEATURE_W = 1024;
const FEATURE_H = 500;

const SHOT_W = 1440;
const SHOT_H = 2560;
/** 撮影端末のシステムナビゲーションバーの高さ（px）。 */
const NAV_BAR_H = 126;
/** アプリの背景色。余白をこの色で埋めると継ぎ目が目立たない。 */
const PAD_COLOR = '#fff8fa';

async function generateFeatureGraphic() {
  const img = await loadImage(path.join(ASSETS, 'feature-graphic.jpg'));
  const canvas = createCanvas(FEATURE_W, FEATURE_H);
  const ctx = canvas.getContext('2d');
  ctx.drawImage(img, 0, 0, FEATURE_W, FEATURE_H);

  const out = path.join(OUT, 'feature-graphic.jpg');
  // Play は「JPEG または 24bit PNG（アルファなし）」を要求する。canvas の PNG 出力は
  // 常にアルファチャンネルを持つため、JPEG で書き出して曖昧さを避ける。
  await writeFile(out, canvas.toBuffer('image/jpeg', 95));
  console.log(`  ✓ ${out} (${img.width}×${img.height} → ${FEATURE_W}×${FEATURE_H})`);
}

async function generateScreenshots() {
  const dir = path.join(ASSETS, 'screenshot');
  const files = (await readdir(dir)).filter((f) => f.endsWith('.png'));

  for (const file of files) {
    const img = await loadImage(path.join(dir, file));
    const cropH = img.height - NAV_BAR_H;

    const canvas = createCanvas(SHOT_W, SHOT_H);
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = PAD_COLOR;
    ctx.fillRect(0, 0, SHOT_W, SHOT_H);
    ctx.drawImage(img, 0, 0, img.width, cropH, Math.round((SHOT_W - img.width) / 2), 0, img.width, cropH);

    const out = path.join(OUT, 'screenshot', file);
    await writeFile(out, canvas.toBuffer('image/png'));
    console.log(`  ✓ ${out} (${img.width}×${img.height} → ${SHOT_W}×${SHOT_H})`);
  }
}

await mkdir(path.join(OUT, 'screenshot'), { recursive: true });
console.log('フィーチャーグラフィック:');
await generateFeatureGraphic();
console.log('スクリーンショット:');
await generateScreenshots();
