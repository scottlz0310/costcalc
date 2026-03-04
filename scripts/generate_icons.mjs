/**
 * アプリアイコン生成スクリプト
 *
 * 使い方:
 *   1. icon_source.png（1024×1024 以上推奨）をプロジェクトルートに置く
 *   2. node scripts/generate_icons.mjs
 *
 * 出力:
 *   - app/src/main/res/mipmap-[density]/ic_launcher.png（各解像度）
 *   - app/src/main/res/mipmap-[density]/ic_launcher_round.png（同上・丸マスク適用）
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
  { dir: 'mipmap-mdpi',    size: 48  },
  { dir: 'mipmap-hdpi',    size: 72  },
  { dir: 'mipmap-xhdpi',   size: 96  },
  { dir: 'mipmap-xxhdpi',  size: 144 },
  { dir: 'mipmap-xxxhdpi', size: 192 },
];

const PLAY_STORE_SIZE = 512;
const SRC = join(ROOT, 'icon_source.png');
const RES = join(ROOT, 'app', 'src', 'main', 'res');

/**
 * 指定サイズに画像をリサイズして PNG バッファを返す
 */
async function resize(img, size) {
  const canvas = createCanvas(size, size);
  const ctx = canvas.getContext('2d');
  ctx.drawImage(img, 0, 0, size, size);
  return canvas.toBuffer('image/png');
}

/**
 * 丸マスクを適用して PNG バッファを返す
 */
async function resizeRound(img, size) {
  const canvas = createCanvas(size, size);
  const ctx = canvas.getContext('2d');
  const r = size / 2;
  ctx.beginPath();
  ctx.arc(r, r, r, 0, Math.PI * 2);
  ctx.closePath();
  ctx.clip();
  ctx.drawImage(img, 0, 0, size, size);
  return canvas.toBuffer('image/png');
}

async function main() {
  console.log(`ソース画像を読み込み中: ${SRC}`);
  const img = await loadImage(SRC);
  console.log(`元画像サイズ: ${img.width}×${img.height}`);

  for (const { dir, size } of SIZES) {
    const outDir = join(RES, dir);
    mkdirSync(outDir, { recursive: true });

    const square = await resize(img, size);
    writeFileSync(join(outDir, 'ic_launcher.png'), square);

    const round = await resizeRound(img, size);
    writeFileSync(join(outDir, 'ic_launcher_round.png'), round);

    console.log(`  ✓ ${dir} (${size}×${size})`);
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
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
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
