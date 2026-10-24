#!/usr/bin/env node
/**
 * verify.js — 资源一致性检查（CI 与本地都能跑）
 *
 * 检查项：
 *   1. 色板 JSON 结构、色号唯一、色值格式
 *   2. series 计数之和 == colors.length
 *   3. Java 端 MardPalette 的色数 == 色板色数（防止 295/221 这类两端漂移）
 *   4. blockstates / item models 每色一份且内容指向基础模型
 *   5. 资源目录中没有色板外的孤立文件（历史迁移残留）
 *   6. lang 含全部色号与所需 UI 文案
 *   7. （可选）brands.json 存在时校验其内部色数
 *
 * 用法：node tools/verify.js      # 全部通过 exit 0，否则 exit 1
 */
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const PALETTE_FILE = path.join(ROOT, 'colors', 'mard_295.json');

// 资源根目录：新分支在 shared/assets（双加载器共享），1.20.1 老分支直接在 forge 工程内。
const ASSETS_CANDIDATES = [
    path.join(ROOT, 'shared', 'assets', 'mard_pixel'),
    path.join(ROOT, 'forge', 'src', 'main', 'resources', 'assets', 'mard_pixel'),
    path.join(ROOT, 'common', 'src', 'main', 'resources', 'assets', 'mard_pixel'),
];
// 调色板 Java 文件：新分支在 shared/java，老分支在 forge 工程内；都找不到时跳过该检查（老分支结构不同）
const JAVA_PALETTE_CANDIDATES = [
    path.join(ROOT, 'shared', 'java', 'com', 'mard', 'pixel', 'common', 'MardPalette.java'),
    path.join(ROOT, 'forge', 'src', 'main', 'java', 'com', 'mard', 'pixel', 'common', 'MardPalette.java'),
    path.join(ROOT, 'forge', 'src', 'main', 'java', 'com', 'mard', 'pixel', 'forge', 'MardPalette.java'),
];

function resolveAssetsRoot() {
    for (const candidate of ASSETS_CANDIDATES) {
        if (fs.existsSync(candidate)) return candidate;
    }
    return null;
}

function resolveJavaPalette() {
    for (const candidate of JAVA_PALETTE_CANDIDATES) {
        if (fs.existsSync(candidate)) return candidate;
    }
    return null;
}

const BASE_MODEL = 'mard_pixel:block/mard_base';
const NON_COLOR_RESOURCES = new Set(['mard_crafting_table', 'mard_pigment']);

const REQUIRED_LANG_KEYS = [
    'itemGroup.mard_pixel',
    'item.mard_pixel.mard_pigment',
    'block.mard_pixel.mard_crafting_table',
    'key.mard_pixel.open',
    'key.categories.mard_pixel',
    'screen.mard_pixel.title',
    'screen.mard_pixel.crafting.select_color',
];

let failed = 0;
let passed = 0;

function check(name, condition, detail) {
    if (condition) {
        passed++;
        console.log('[OK]   ' + name);
    } else {
        failed++;
        console.log('[FAIL] ' + name + (detail ? ' :: ' + detail : ''));
    }
}

function readText(file) {
    return fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
}

function hasBom(file) {
    const buf = fs.readFileSync(file);
    return buf.length >= 3 && buf[0] === 0xEF && buf[1] === 0xBB && buf[2] === 0xBF;
}

function readJson(file) {
    return JSON.parse(readText(file));
}

function loadColors() {
    check('colors/mard_295.json 存在', fs.existsSync(PALETTE_FILE));
    if (!fs.existsSync(PALETTE_FILE)) return null;
    check('colors/mard_295.json 无 BOM', !hasBom(PALETTE_FILE), 'BOM 会让部分构建/解析失败');

    const data = readJson(PALETTE_FILE);
    const colors = data.colors;
    check('色板 colors 为非空数组', Array.isArray(colors) && colors.length > 0, '实际 ' + (colors ? colors.length : 'null'));
    if (!Array.isArray(colors)) return null;

    const codes = colors.map(function (c) { return String(c.code || '').toUpperCase(); });
    const unique = new Set(codes);
    check('色号唯一', unique.size === colors.length, '唯一 ' + unique.size + ' / 总 ' + colors.length);

    const badHex = colors.filter(function (c) { return !/^#[0-9A-Fa-f]{6}$/.test(c.hex || ''); });
    check('色值全部为 #RRGGBB', badHex.length === 0, '非法 ' + badHex.length + ' 个');

    const badSeries = colors.filter(function (c) { return String(c.code || '').toUpperCase()[0] !== String(c.series || '').toUpperCase(); });
    check('每条色号的 series 与色号前缀一致', badSeries.length === 0,
        badSeries.slice(0, 5).map(function (c) { return c.code; }).join(', '));

    // series 既可能是 {A:26} 也可能是 {A:{count:26}}，两种都兼容
    const series = data.series || {};
    let seriesSum = 0;
    let seriesShapeOk = true;
    for (const key of Object.keys(series)) {
        const value = series[key];
        const n = typeof value === 'number' ? value : (value && typeof value.count === 'number' ? value.count : NaN);
        if (Number.isNaN(n)) { seriesShapeOk = false; break; }
        seriesSum += n;
    }
    check('series 计数格式可解析', seriesShapeOk);
    check('series 计数之和 == 色数', seriesSum === colors.length, seriesSum + ' vs ' + colors.length);
    check('声明 total 与数据一致', data.total === undefined || data.total === colors.length,
        'total=' + data.total + ' 实际 ' + colors.length);

    return { colors, codes };
}

function verifyJavaPalette(colors) {
    const javaPalette = resolveJavaPalette();
    if (!javaPalette) {
        console.log('[SKIP] 未找到 MardPalette.java（老版本分支结构不同），跳过 Java 调色板校验');
        console.log('       已尝试：' + JAVA_PALETTE_CANDIDATES.map(function (p) { return path.relative(ROOT, p); }).join(' / '));
        return;
    }
    check('MardPalette.java 无 BOM', !hasBom(javaPalette), 'BOM 会导致 javac 编译失败');
    const src = readText(javaPalette);
    const declared = (src.match(/new MardColor\(/g) || []).length;
    check('Java 色数 == 色板色数', declared === colors.length,
        'Java ' + declared + ' vs 色板 ' + colors.length + '（改色板时必须同步改 MardPalette）');

    // 进一步校验色号/色值集合（数量对但内容错是最隐蔽的漂移）
    const pairs = [];
    const pairRe = /new\s+MardColor\(\s*"([^"]+)"\s*,\s*"([^"]+)"/g;
    let m;
    while ((m = pairRe.exec(src)) !== null) {
        pairs.push({ code: m[1].toUpperCase(), hex: m[2].toUpperCase() });
    }
    if (pairs.length === 0) {
        console.log('[SKIP] MardPalette.java 的构造参数格式无法识别，跳过色号/色值集合校验');
        return;
    }
    const javaCodes = new Set(pairs.map(function (p) { return p.code; }));
    const jsonCodes = new Set(colors.map(function (c) { return String(c.code).toUpperCase(); }));
    const missingCodes = [...jsonCodes].filter(function (c) { return !javaCodes.has(c); });
    const extraCodes = [...javaCodes].filter(function (c) { return !jsonCodes.has(c); });
    check('Java 色号集合 == 色板色号集合', missingCodes.length === 0 && extraCodes.length === 0,
        '缺 ' + missingCodes.length + ' 个（' + missingCodes.slice(0, 8).join(',') + '），多 ' +
        extraCodes.length + ' 个（' + extraCodes.slice(0, 8).join(',') + '）');

    const jsonByCode = {};
    for (const c of colors) jsonByCode[String(c.code).toUpperCase()] = String(c.hex).toUpperCase();
    const hexMismatch = pairs.filter(function (p) {
        return jsonByCode[p.code] && jsonByCode[p.code] !== p.hex;
    });
    check('Java 色值与色板一致', hexMismatch.length === 0,
        hexMismatch.length + ' 个不一致：' + hexMismatch.slice(0, 5).map(function (p) {
            return p.code + ' Java ' + p.hex + ' vs JSON ' + jsonByCode[p.code];
        }).join('；'));
}

function verifyResources(colors) {
    const assets = resolveAssetsRoot();
    if (!assets) {
        check('资源目录存在（shared/assets 或 forge/src/main/resources/assets）', false,
            '已尝试：' + ASSETS_CANDIDATES.map(function (p) { return path.relative(ROOT, p); }).join(' / '));
        return;
    }
    console.log('[INFO] 资源目录：' + path.relative(ROOT, assets));

    const bsDir = path.join(assets, 'blockstates');
    const imDir = path.join(assets, 'models', 'item');
    const baseModelFile = path.join(assets, 'models', 'block', 'mard_base.json');

    check('blockstates 目录存在', fs.existsSync(bsDir));
    check('models/item 目录存在', fs.existsSync(imDir));
    check('基础模型 models/block/mard_base.json 存在', fs.existsSync(baseModelFile));

    let missingBs = [];
    let missingIm = [];
    let wrongBs = [];
    let wrongIm = [];

    for (const color of colors) {
        const name = 'mard_' + String(color.code).toLowerCase();
        const bsFile = path.join(bsDir, name + '.json');
        const imFile = path.join(imDir, name + '.json');

        if (!fs.existsSync(bsFile)) { missingBs.push(color.code); continue; }
        if (!fs.existsSync(imFile)) { missingIm.push(color.code); }

        try {
            const bs = readJson(bsFile);
            const variant = bs.variants && bs.variants[''];
            if (!variant || variant.model !== BASE_MODEL) wrongBs.push(color.code);
        } catch (e) {
            wrongBs.push(color.code + '(解析失败)');
        }
        try {
            const im = readJson(imFile);
            if (im.parent !== BASE_MODEL) wrongIm.push(color.code);
        } catch (e) {
            wrongIm.push(color.code + '(解析失败)');
        }
    }

    check('每个色号都有 blockstate', missingBs.length === 0, '缺 ' + missingBs.length + ' 个：' + missingBs.slice(0, 8).join(','));
    check('每个色号都有 item model', missingIm.length === 0, '缺 ' + missingIm.length + ' 个：' + missingIm.slice(0, 8).join(','));
    check('blockstate 均指向基础模型', wrongBs.length === 0, '异常 ' + wrongBs.length + ' 个：' + wrongBs.slice(0, 8).join(','));
    check('item model 均指向基础模型', wrongIm.length === 0, '异常 ' + wrongIm.length + ' 个：' + wrongIm.slice(0, 8).join(','));

    const expected = new Set(colors.map(function (c) { return 'mard_' + String(c.code).toLowerCase(); }));
    const known = new Set([...NON_COLOR_RESOURCES].map(function (n) { return n.toLowerCase(); }).concat(['mard_base']));
    function orphans(dir, ext, codeLikeOnly) {
        if (!fs.existsSync(dir)) return [];
        return fs.readdirSync(dir).filter(function (name) {
            const re = new RegExp('^mard_.*\\.' + ext + '$', 'i');
            if (!re.test(name)) return false;
            if (codeLikeOnly && !/^mard_[a-z]{1,2}\d+\.(png|json)$/i.test(name)) return false;
            const base = name.replace(/\.[^.]+$/, '').toLowerCase();
            return !expected.has(base) && !known.has(base);
        });
    }
    const orphanBs = orphans(bsDir, 'json', false);
    const orphanIm = orphans(imDir, 'json', false);
    // 程序化染色不应有「每色一张贴图」，只检查形如 mard_a12.png 的残留（mard_base.png 等不在范围内）
    const orphanTex = orphans(path.join(assets, 'textures', 'block'), 'png', true);
    check('blockstates 无孤立文件', orphanBs.length === 0,
        orphanBs.length + ' 个：' + orphanBs.slice(0, 8).join(',') + '（重新运行 tools/generate_resources.js 可清理）');
    check('item models 无孤立文件', orphanIm.length === 0,
        orphanIm.length + ' 个：' + orphanIm.slice(0, 8).join(','));
    check('textures/block 无每色残留贴图', orphanTex.length === 0,
        orphanTex.length + ' 个：' + orphanTex.slice(0, 8).join(','));
}

function verifyLang(colors) {
    const assets = resolveAssetsRoot();
    if (!assets) return;
    const langDir = path.join(assets, 'lang');
    check('lang 目录存在', fs.existsSync(langDir));
    if (!fs.existsSync(langDir)) return;

    for (const lang of ['en_us', 'zh_cn']) {
        const file = path.join(langDir, lang + '.json');
        check('lang/' + lang + '.json 存在', fs.existsSync(file));
        if (!fs.existsSync(file)) continue;
        let dict;
        try {
            dict = readJson(file);
        } catch (e) {
            check('lang/' + lang + '.json 可解析', false, e.message);
            continue;
        }
        const missing = colors
            .map(function (c) { return 'block.mard_pixel.mard_' + String(c.code).toLowerCase(); })
            .filter(function (k) { return !(k in dict); });
        check('lang/' + lang + ' 含全部色号', missing.length === 0,
            '缺 ' + missing.length + ' 个：' + missing.slice(0, 8).join(','));
        const missingUi = REQUIRED_LANG_KEYS.filter(function (k) { return !(k in dict); });
        check('lang/' + lang + ' 含必需 UI 文案', missingUi.length === 0, missingUi.join(','));
    }
}

function verifyBrands() {
    const assets = resolveAssetsRoot();
    if (!assets) return;
    const file = path.join(assets, 'brands', 'brands.json');
    if (!fs.existsSync(file)) {
        console.log('[SKIP] brands.json 不存在，跳过品牌色数校验（该数据尚未入库）');
        return;
    }
    let data;
    try {
        data = readJson(file);
    } catch (e) {
        check('brands.json 可解析', false, e.message);
        return;
    }
    const brands = data.brands || {};
    const names = Object.keys(brands);
    check('brands.json 至少含一个品牌', names.length > 0);
    for (const name of names) {
        check('brands.' + name + ' 为数组', Array.isArray(brands[name]));
        if (Array.isArray(brands[name])) {
            const codes = brands[name].map(function (c) { return (c && c.code) || ''; });
            check('brands.' + name + ' 色号唯一', new Set(codes).size === codes.length);
        }
    }
}

function main() {
    console.log('== 资源一致性检查 ==\n');
    const loaded = loadColors();
    if (!loaded) {
        console.log('\n结果：色板加载失败');
        process.exit(1);
    }
    verifyJavaPalette(loaded.colors);
    verifyResources(loaded.colors);
    verifyLang(loaded.colors);
    verifyBrands();

    console.log('\n通过 ' + passed + ' 项，失败 ' + failed + ' 项。');
    if (failed > 0) {
        console.log('结果：不一致，请先修复（常见修法：node tools/generate_resources.js 重新生成）');
        process.exit(1);
    }
    console.log('结果：全部通过 ✔');
    process.exit(0);
}

main();
