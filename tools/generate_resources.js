/**
 * MARD 像素画 Mod —— 资源生成脚本（Node.js）
 *
 * 读取 colors/mard_295.json，生成：
 *   1. shared/assets/mard_pixel/blockstates/mard_<code>.json  （291 个）
 *   2. shared/assets/mard_pixel/models/item/mard_<code>.json  （291 个）
 *   3. shared/assets/mard_pixel/lang/{en_us,zh_cn}.json        （物品名 + UI 文本）
 *
 * 用法：node tools/generate_resources.js
 * 在 GitHub Actions 构建流程中会先运行本脚本再构建。
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const COLORS_JSON = path.join(ROOT, 'colors', 'mard_295.json');
const ASSETS = path.join(ROOT, 'shared', 'assets', 'mard_pixel');

function main() {
    if (!fs.existsSync(COLORS_JSON)) {
        console.error('[ERROR] 未找到 colors/mard_295.json');
        process.exit(1);
    }
    const data = JSON.parse(fs.readFileSync(COLORS_JSON, 'utf8'));
    const colors = data.colors;
    if (!Array.isArray(colors) || colors.length === 0) {
        console.error('[ERROR] colors 数据为空');
        process.exit(1);
    }

    // 1. blockstates
    const bsDir = path.join(ASSETS, 'blockstates');
    fs.mkdirSync(bsDir, { recursive: true });
    for (const c of colors) {
        const name = 'mard_' + c.code.toLowerCase();
        // 所有颜色统一使用基础模型
        const bs = { variants: { "": { model: "mard_pixel:block/mard_base" } } };
        fs.writeFileSync(path.join(bsDir, name + '.json'), JSON.stringify(bs));
    }
    // mard_custom 方块的 blockstate
    fs.writeFileSync(path.join(bsDir, 'mard_custom.json'), JSON.stringify({
        variants: { "": { model: "mard_pixel:block/mard_base" } }
    }));

    // 2. item models
    // 所有颜色（包括透明色）的物品模型都使用 mard_base（不透明白色混凝土纹理），
    // 这样物品栏中的颜色就会和世界中的方块颜色完全一致，不会因为玻璃纹理的
    // 透明部分在物品栏中渲染不正确而导致颜色偏差。
    const imDir = path.join(ASSETS, 'models', 'item');
    fs.mkdirSync(imDir, { recursive: true });
    for (const c of colors) {
        const name = 'mard_' + c.code.toLowerCase();
        // 物品统一使用不透明基础模型，确保颜色一致
        const im = { parent: "mard_pixel:block/mard_base" };
        fs.writeFileSync(path.join(imDir, name + '.json'), JSON.stringify(im));
    }
    // mard_custom 物品的 item model（手持尺寸与标准色块一致）
    fs.writeFileSync(path.join(imDir, 'mard_custom.json'), JSON.stringify({
        parent: "mard_pixel:block/mard_base"
    }));

    // 3. lang
    const langDir = path.join(ASSETS, 'lang');
    fs.mkdirSync(langDir, { recursive: true });
    const en = {}, zh = {};
    en['itemGroup.mard_pixel'] = 'MARD Pixel Blocks';
    zh['itemGroup.mard_pixel'] = 'MARD 像素色块';
    for (const c of colors) {
        const key = 'block.mard_pixel.mard_' + c.code.toLowerCase();
        en[key] = 'MARD ' + c.code;
        zh[key] = 'MARD ' + c.code;
    }
    en['block.mard_pixel.mard_custom'] = 'MARD Custom Block';
    zh['block.mard_pixel.mard_custom'] = 'MARD 自定义色块';
    en['key.mard_pixel.open'] = 'Open MARD Color Palette';
    zh['key.mard_pixel.open'] = '打开 MARD 色板';
    en['key.categories.mard_pixel'] = 'MARD Pixel';
    zh['key.categories.mard_pixel'] = 'MARD 像素画';
    en['screen.mard_pixel.title'] = 'MARD Color Palette';
    zh['screen.mard_pixel.title'] = 'MARD 色板';
    en['screen.mard_pixel.switchbag'] = 'Switch Bag to This System';
    zh['screen.mard_pixel.switchbag'] = '整体换包到此色系';
    en['screen.mard_pixel.pick'] = 'Pick Color';
    zh['screen.mard_pixel.pick'] = '吸取颜色';
    en['screen.mard_pixel.add'] = 'Add';
    zh['screen.mard_pixel.add'] = '新增';
    en['screen.mard_pixel.remove'] = 'Remove';
    zh['screen.mard_pixel.remove'] = '删除';
    en['screen.mard_pixel.tri'] = 'HSV Triangle';
    zh['screen.mard_pixel.tri'] = 'HSV 三角';
    en['screen.mard_pixel.wheel'] = 'RGB Wheel';
    zh['screen.mard_pixel.wheel'] = 'RGB 色环';

    fs.writeFileSync(path.join(langDir, 'en_us.json'), JSON.stringify(en, null, 1));
    fs.writeFileSync(path.join(langDir, 'zh_cn.json'), JSON.stringify(zh, null, 1));

    console.log('[OK] 已生成 ' + colors.length + ' 个 blockstates, ' + colors.length + ' 个 item models, 2 个 lang 文件');
}

main();
