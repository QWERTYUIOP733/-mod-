#!/usr/bin/env node
/**
 * generate_resources.js — 由色板 JSON 生成 Mod 资源（blockstate / item model / lang）
 *
 * 读取 colors/mard_295.json（文件名沿用历史命名，内容为当前色板），生成：
 *   1. shared/assets/mard_pixel/blockstates/mard_<code>.json
 *   2. shared/assets/mard_pixel/models/item/mard_<code>.json
 *   3. shared/assets/mard_pixel/lang/{en_us,zh_cn}.json
 *
 * 相比旧版本的改动：
 *   - 不再在注释/日志里写死 221 或 295，全部由数据决定（色板扩容后不会漏生成）
 *   - 生成前校验色号唯一 / 色值格式，数据脏时立刻失败
 *   - 自动清理色板中已不存在的历史资源（避免 295→221 之类的迁移残留）
 *
 * 用法：node tools/generate_resources.js
 */
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
// 历史文件名：色板早期按 295 色规划，实际为 221 色；改名会影响 Java 端引用，暂不改
const PALETTE_FILE = path.join(ROOT, 'colors', 'mard_295.json');

// 资源根目录：新分支在 shared/assets（双加载器共享），1.20.1 老分支直接在 forge 工程内。
// 自动探测，避免同一套工具在四个版本分支上要写两份。
const ASSETS_CANDIDATES = [
    path.join(ROOT, 'shared', 'assets', 'mard_pixel'),
    path.join(ROOT, 'forge', 'src', 'main', 'resources', 'assets', 'mard_pixel'),
    path.join(ROOT, 'common', 'src', 'main', 'resources', 'assets', 'mard_pixel'),
];

function resolveAssetsRoot() {
    for (const candidate of ASSETS_CANDIDATES) {
        if (fs.existsSync(candidate)) return candidate;
    }
    return null;
}

// 非色号资源，不参与孤儿清理
const NON_COLOR_RESOURCES = new Set(['mard_crafting_table', 'mard_pigment']);

const BASE_MODEL = 'mard_pixel:block/mard_base';

function fail(message) {
    console.error('[ERROR] ' + message);
    process.exit(1);
}

function loadPalette() {
    if (!fs.existsSync(PALETTE_FILE)) fail('未找到 colors/mard_295.json');
    const data = JSON.parse(fs.readFileSync(PALETTE_FILE, 'utf8').replace(/^\uFEFF/, ''));
    const colors = data.colors;
    if (!Array.isArray(colors) || colors.length === 0) fail('色板 colors 为空或不是数组');

    const seen = new Set();
    for (const color of colors) {
        if (!color || typeof color.code !== 'string' || color.code.length === 0) {
            fail('存在缺少 code 的色号条目：' + JSON.stringify(color));
        }
        const key = color.code.toUpperCase();
        if (seen.has(key)) fail('色号重复：' + color.code);
        seen.add(key);
        if (!/^#[0-9A-Fa-f]{6}$/.test(color.hex || '')) {
            fail('色号 ' + color.code + ' 的色值不是 #RRGGBB：' + color.hex);
        }
    }
    return { colors, meta: data };
}

function writeJson(file, obj) {
    fs.writeFileSync(file, JSON.stringify(obj), 'utf8');
}

function pruneOrphans(dir, expected) {
    if (!fs.existsSync(dir)) return [];
    const removed = [];
    // 大小写不敏感 + 保留 mard_base 等非色号资源
    const keep = new Set([...NON_COLOR_RESOURCES, 'mard_base'].map(function (n) { return n.toLowerCase(); }));
    for (const name of fs.readdirSync(dir)) {
        if (!/^mard_.*\.json$/i.test(name)) continue;
        const base = name.replace(/\.json$/i, '').toLowerCase();
        if (expected.has(base) || keep.has(base)) continue;
        fs.unlinkSync(path.join(dir, name));
        removed.push(name);
    }
    return removed;
}

function main() {
    const { colors } = loadPalette();
    const counts = { blockstates: 0, models: 0 };

    const assets = resolveAssetsRoot();
    if (!assets) {
        fail('找不到资源目录。已尝试：\n        ' + ASSETS_CANDIDATES.map(function (p) {
            return path.relative(ROOT, p);
        }).join('\n        '));
    }
    console.log('[INFO] 资源目录：' + path.relative(ROOT, assets));

    const bsDir = path.join(assets, 'blockstates');
    const imDir = path.join(assets, 'models', 'item');
    fs.mkdirSync(bsDir, { recursive: true });
    fs.mkdirSync(imDir, { recursive: true });

    const expected = new Set();
    for (const color of colors) {
        const name = 'mard_' + color.code.toLowerCase();
        expected.add(name);
        writeJson(path.join(bsDir, name + '.json'), { variants: { '': { model: BASE_MODEL } } });
        writeJson(path.join(imDir, name + '.json'), { parent: BASE_MODEL });
        counts.blockstates++;
        counts.models++;
    }

    const removedBlockstates = pruneOrphans(bsDir, expected);
    const removedModels = pruneOrphans(imDir, expected);

    const langDir = path.join(assets, 'lang');
    fs.mkdirSync(langDir, { recursive: true });
    const en = {};
    const zh = {};
    en['itemGroup.mard_pixel'] = 'Color Blocks Extension';
    zh['itemGroup.mard_pixel'] = '彩色方块扩展';
    for (const color of colors) {
        const key = 'block.mard_pixel.mard_' + color.code.toLowerCase();
        en[key] = 'Color Block ' + color.code;
        zh[key] = '色块 ' + color.code;
    }
    en['item.mard_pixel.mard_pigment'] = 'Rainbow Powder';
    zh['item.mard_pixel.mard_pigment'] = '七彩粉末';
    en['block.mard_pixel.mard_crafting_table'] = 'Block Dyeing Table';
    zh['block.mard_pixel.mard_crafting_table'] = '方块染色台';
    en['key.mard_pixel.open'] = 'Open Color Palette';
    zh['key.mard_pixel.open'] = '打开色板';
    en['key.categories.mard_pixel'] = 'Color Blocks Extension';
    zh['key.categories.mard_pixel'] = '彩色方块扩展';
    en['screen.mard_pixel.title'] = 'Color Palette';
    zh['screen.mard_pixel.title'] = '色板';
    en['screen.mard_pixel.crafting.select_color'] = 'Select Color';
    zh['screen.mard_pixel.crafting.select_color'] = '选择颜色';

    // 与仓库里已有的 lang 合并：保留人工维护的其他键，只替换/清理本工具生成的部分
    function mergeLang(file, generated) {
        let existing = {};
        if (fs.existsSync(file)) {
            try {
                existing = JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
            } catch (e) {
                console.warn('[WARN] ' + path.relative(ROOT, file) + ' 解析失败，将重新生成：' + e.message);
                existing = {};
            }
        }
        const merged = Object.assign({}, existing);
        for (const key of Object.keys(merged)) {
            // 清掉色板里已不存在的旧色号翻译，其余键原样保留
            if (/^block\.mard_pixel\.mard_/.test(key) && !(key in generated)) delete merged[key];
        }
        for (const key of Object.keys(generated)) merged[key] = generated[key];
        return merged;
    }

    const enFile = path.join(langDir, 'en_us.json');
    const zhFile = path.join(langDir, 'zh_cn.json');
    fs.writeFileSync(enFile, JSON.stringify(mergeLang(enFile, en), null, 1), 'utf8');
    fs.writeFileSync(zhFile, JSON.stringify(mergeLang(zhFile, zh), null, 1), 'utf8');

    console.log('[OK] 色板 ' + colors.length + ' 色 -> ' + counts.blockstates + ' blockstates, ' +
        counts.models + ' item models, 2 lang 文件');
    if (removedBlockstates.length || removedModels.length) {
        console.log('[OK] 清理色板外的历史资源：blockstates ' + removedBlockstates.length +
            ' 个，item models ' + removedModels.length + ' 个');
    }
}

main();
