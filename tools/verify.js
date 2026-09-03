const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
let failed = 0;

function check(name, cond, detail) {
    if (cond) console.log('[OK]   ' + name);
    else { console.log('[FAIL] ' + name + (detail ? ' :: ' + detail : '')); failed++; }
}

try {
    const mard = JSON.parse(fs.readFileSync(path.join(ROOT, 'colors', 'mard_295.json'), 'utf8'));
    const colors = mard.colors;
    check('mard_295.json 存在且可解析', Array.isArray(colors), 'colors 非数组');
    check('MARD 共 295 色', colors && colors.length === 295, '实际 ' + (colors ? colors.length : 0));
    const codes = new Set(colors.map(c => c.code.toUpperCase()));
    check('MARD 色号唯一', codes.size === 295, '唯一 ' + codes.size);
    const badHex = colors.filter(c => !/^#[0-9A-Fa-f]{6}$/.test(c.hex));
    check('MARD 色值全部为 #RRGGBB', badHex.length === 0, '非法 ' + badHex.length + ' 个');
    const totalBySeries = Object.values(mard.series).reduce((a, b) => a + b.count, 0);
    check('系列计数合计 = 295', totalBySeries === 295, '实际 ' + totalBySeries);
} catch (e) {
    check('mard_295.json', false, e.message);
}

try {
    const brands = JSON.parse(fs.readFileSync(path.join(ROOT, 'shared', 'assets', 'mard_pixel', 'brands', 'brands.json'), 'utf8'));
    const b = brands.brands;
    const perler = b.perler ? b.perler.length : 0;
    const hama = b.hama ? b.hama.length : 0;
    const artkal = b.artkal ? b.artkal.length : 0;
    check('brands.json 存在且可解析', b);
    check('Perler 117 色', perler === 117, '实际 ' + perler);
    check('Hama 89 色', hama === 89, '实际 ' + hama);
    check('Artkal 176 色', artkal === 176, '实际 ' + artkal);
    check('品牌合计 382 色', perler + hama + artkal === 382, '实际 ' + (perler + hama + artkal));
} catch (e) {
    check('brands.json', false, e.message);
}

try {
    const src = fs.readFileSync(path.join(ROOT, 'shared', 'java', 'com', 'mard', 'pixel', 'common', 'MardPalette.java'), 'utf8');
    const count = (src.match(/new MardColor\(/g) || []).length;
    check('MardPalette.java 含 295 个 new MardColor', count === 295, '实际 ' + count);
    const arrFix = src.includes('List.of(new MardColor[]{') && src.includes('});');
    check('MardPalette 使用数组形式 List.of（规避 javac 参数上限）', arrFix);
} catch (e) {
    check('MardPalette.java', false, e.message);
}

console.log('\n' + (failed === 0 ? '全部通过 ✔' : failed + ' 项未通过 ✘'));
process.exit(failed === 0 ? 0 : 1);
