#!/usr/bin/env node
/**
 * changelog-note.js — 从 forge/CHANGELOG.md 抽取某个版本的发布说明
 *
 * 约定：CHANGELOG 使用 Forge/Keep a Changelog 风格标题
 *   ## [2.1.0] - 2026-10-09
 *   正文...
 *
 * 用法：
 *   node tools/changelog-note.js --version 2.1.0
 *   node tools/changelog-note.js --version 2.1.0 --out dist/RELEASE_NOTES.md
 *   node tools/changelog-note.js --version 2.1.0 --strict   # 找不到条目时 exit 1
 *
 * 找不到条目时默认输出一段带提示的占位说明并 exit 0，避免卡住紧急发版。
 */
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');

function argValue(argv, name) {
    const i = argv.indexOf(name);
    return i >= 0 ? argv[i + 1] : null;
}

function main() {
    const argv = process.argv.slice(2);
    const version = argValue(argv, '--version');
    const file = argValue(argv, '--file') || path.join(ROOT, 'forge', 'CHANGELOG.md');
    const out = argValue(argv, '--out');
    const strict = argv.includes('--strict');

    if (!version) {
        console.error('[ERROR] 缺少 --version');
        process.exit(1);
    }
    if (!fs.existsSync(file)) {
        console.error('[ERROR] 找不到 CHANGELOG：' + file);
        process.exit(1);
    }

    const text = fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
    const lines = text.split(/\r?\n/);
    const headingRe = /^##\s*\[([^\]]+)\]/;
    const wanted = version.replace(/^v/, '');

    let start = -1;
    let end = lines.length;
    for (let i = 0; i < lines.length; i++) {
        const m = lines[i].match(headingRe);
        if (!m) continue;
        const v = m[1].replace(/^v/, '');
        if (v === wanted) {
            start = i;
            for (let j = i + 1; j < lines.length; j++) {
                if (headingRe.test(lines[j])) { end = j; break; }
            }
            break;
        }
    }

    let body;
    if (start >= 0) {
        body = lines.slice(start + 1, end).join('\n').trim();
    } else {
        const msg = '未在 forge/CHANGELOG.md 中找到 [' + wanted + '] 的更新条目。';
        if (strict) {
            console.error('[ERROR] ' + msg);
            process.exit(1);
        }
        console.warn('[WARN] ' + msg + ' 使用占位说明。');
        body = '本次发布未在 CHANGELOG 中找到对应条目，请补充。\n\n下载后请核对 SHA256SUMS.txt。';
    }

    const footer = '\n\n---\n本文件由 GitHub Actions 根据 forge/CHANGELOG.md 自动生成。';
    const content = body + footer + '\n';

    if (out) {
        fs.mkdirSync(path.dirname(out), { recursive: true });
        fs.writeFileSync(out, content, 'utf8');
        console.log('[OK] 已写入 ' + out);
    } else {
        process.stdout.write(content);
    }
}

main();
