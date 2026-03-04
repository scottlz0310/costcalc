/**
 * アプリアイコン生成スクリプト
 *
 * 【事前準備】
 *   npm install        # @napi-rs/canvas をインストール（初回のみ）
 *
 * 使い方:
 *   1. icon_source.png（1024×1024 以上推奨）をプロジェクトルートに置く
 *   2. node scripts/generate_icons.mjs
 *      または npm run gen-icons
 *
 * 備考:
 *   - icon_source.png が正方形でない場合は中央を基準にクロップして処理する
 *
 * 出力:
 *   - app/src/main/res/mipmap-[density]/ic_launcher.png（各解像度）
 *   - app/src/main/res/mipmap-[density]/ic_launcher_round.png（同上・丸マスク適用）
 *   - app/src/main/res/mipmap-[density]/ic_launcher_foreground_full.png（アダプティブアイコン用・108dp相当）
 *   - app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml（アダプティブアイコン設定）
 *   - play_store_icon.png（512×512・Play Store 提出用）
 */

import { createCanvas, loadImage } from '@napi-rs/canvas';
import { writeFileSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, '..');

// Android 各解像度のアイコンサイズ（px）
const SIZES = [
  { dir: 'mipmap-mdpi',    size: 48,  adaptiveSize: 108 },
  { dir: 'mipmap-hdpi',    size: 72,  adaptiveSize: 162 },
  { dir: 'mipmap-xhdpi',   size: 96,  adaptiveSize: 216 },
  { dir: 'mipmap-xxhdpi',  size: 144, adaptiveSize: 324 },
  { dir: 'mipmap-xxxhdpi', size: 192, adaptiveSize: 432 },
];

const PLAY_STORE_SIZE = 512;
const SRC = join(ROOT, 'icon_source.png');
const RES = join(ROOT, 'app', 'src', 'main', 'res');

/**
 * 段階的ダウンスケール（一気に縮小せず半分ずつ縮小することで品質を向上）
 * Canvas の imageSmoothingQuality を 'high' に設定して補間精度を最大化する。
 * ソース画像が正方形でない場合は中央を正方形にクロップしてからリサイズする。
 */
function stepDownscale(srcImg, targetSize) {
  // 正方形にクロップ（中央基準）
  const cropSize = Math.min(srcImg.width, srcImg.height);
  const sx = (srcImg.width - cropSize) / 2;
  const sy = (srcImg.height - cropSize) / 2;

  let currentSize = cropSize;
  let canvas = createCanvas(currentSize, currentSize);
  let ctx = canvas.getContext('2d');
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(srcImg, sx, sy, cropSize, cropSize, 0, 0, currentSize, currentSize);

  // targetSize の2倍を超えている間は半分ずつ縮小
  while (currentSize > targetSize * 2) {
    const nextSize = Math.max(Math.floor(currentSize / 2), targetSize);
    const next = createCanvas(nextSize, nextSize);
    const nctx = next.getContext('2d');
    nctx.imageSmoothingEnabled = true;
    nctx.imageSmoothingQuality = 'high';
    nctx.drawImage(canvas, 0, 0, nextSize, nextSize);
    canvas = next;
    currentSize = nextSize;
  }

  // 最終サイズへ
  const out = createCanvas(targetSize, targetSize);
  const octx = out.getContext('2d');
  octx.imageSmoothingEnabled = true;
  octx.imageSmoothingQuality = 'high';
  octx.drawImage(canvas, 0, 0, targetSize, targetSize);
  return out;
}

/**
 * 指定サイズに画像をリサイズして PNG バッファを返す
 */
async function resize(img, size) {
  const canvas = stepDownscale(img, size);
  return canvas.toBuffer('image/png');
}

/**
 * 丸マスクを適用して PNG バッファを返す
 */
async function resizeRound(img, size) {
  const scaled = stepDownscale(img, size);
  const canvas = createCanvas(size, size);
  const ctx = canvas.getContext('2d');
  const r = size / 2;
  ctx.beginPath();
  ctx.arc(r, r, r, 0, Math.PI * 2);
  ctx.closePath();
  ctx.clip();
  ctx.drawImage(scaled, 0, 0, size, size);
  return canvas.toBuffer('image/png');
}

async function main() {
  console.log(`ソース画像を読み込み中: ${SRC}`);
  const img = await loadImage(SRC);
  console.log(`元画像サイズ: ${img.width}×${img.height}`);

  for (const { dir, size, adaptiveSize } of SIZES) {
    const outDir = join(RES, dir);
    mkdirSync(outDir, { recursive: true });

    const square = await resize(img, size);
    writeFileSync(join(outDir, 'ic_launcher.png'), square);

    const round = await resizeRound(img, size);
    writeFileSync(join(outDir, 'ic_launcher_round.png'), round);

    // アダプティブアイコン用フォアグラウンド（108dp 相当、セーフゾーン 72/108 に収める）
    const fgCanvas = createCanvas(adaptiveSize, adaptiveSize);
    const fgCtx = fgCanvas.getContext('2d');
    fgCtx.imageSmoothingEnabled = true;
    fgCtx.imageSmoothingQuality = 'high';
    const safeZoneRatio = 72 / 108;
    const iconSize = Math.round(adaptiveSize * safeZoneRatio);
    const offset = Math.round((adaptiveSize - iconSize) / 2);
    const scaledCanvas = stepDownscale(img, iconSize);
    fgCtx.drawImage(scaledCanvas, offset, offset, iconSize, iconSize);
    writeFileSync(join(outDir, 'ic_launcher_foreground_full.png'), fgCanvas.toBuffer('image/png'));

    console.log(`  ✓ ${dir} (${size}×${size}, adaptive foreground ${adaptiveSize}×${adaptiveSize})`);
  }

  // Play Store 用 512×512
  const playStore = await resize(img, PLAY_STORE_SIZE);
  writeFileSync(join(ROOT, 'play_store_icon.png'), playStore);
  console.log(`  ✓ play_store_icon.png (512×512)`);

  // anydpi-v26 アダプティブアイコン XML
  const anydpiDir = join(RES, 'mipmap-anydpi-v26');
  mkdirSync(anydpiDir, { recursive: true });
  const adaptiveXml = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@android:color/transparent" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground_full" />
</adaptive-icon>
`;
  writeFileSync(join(anydpiDir, 'ic_launcher.xml'), adaptiveXml);
  writeFileSync(join(anydpiDir, 'ic_launcher_round.xml'), adaptiveXml);
  console.log(`  ✓ mipmap-anydpi-v26/ic_launcher*.xml`);

  console.log('\n完了！');
}

main().catch(err => {
  console.error('エラー:', err.message);
  if (err.message.includes('icon_source.png')) {
    console.error('→ icon_source.png をプロジェクトルートに置いてください。');
  }
  process.exit(1);
});
