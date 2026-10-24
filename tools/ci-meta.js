#!/usr/bin/env node
/**
 * ci-meta.js — 为 GitHub Actions 提供构建元数据（单一来源）
 *
 * 读取当前分支：
 *   forge/gradle.properties                     -> ci.javaVersion / ci.gradleVersion（可缺省，缺省用下表推导）
 *   forge/src/main/resources/META-INF/mods.toml -> version、minecraft 的 versionRange
 *   forge/build.gradle                          -> 顶层 version（字面量或引用 gradle.properties 的标识符）
 *
 * 输出（存在 GITHUB_OUTPUT 时写入）：
 *   mc_version / mod_version / java_version / gradle_version / artifact_prefix
 *
 * 参数：
 *   --check-tag <tag>   发布用：校验 tag 与版本一致（支持 v1.2.3 与 mc1.21.1-v2.0.0 两种历史格式）
 *   --json              以 JSON 打印（同时也写 GITHUB_OUTPUT，便于 CI 调试）
 *   --quiet             不打印人类可读摘要
 */
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const MODS_TOML = path.join(ROOT, 'forge', 'src', 'main', 'resources', 'META-INF', 'mods.toml');
const GRADLE_PROPS = path.join(ROOT, 'forge', 'gradle.properties');
const BUILD_GRADLE = path.join(ROOT, 'forge', 'build.gradle');
const ARTIFACT_PREFIX = 'mard_pixel_forge';

// Minecraft -> JDK / Gradle 回退表。
// 依据实际分支结构：
//   1.20.1  = ForgeGradle 6 + Gradle 8.8 + JDK 17
//   1.21.1  = ForgeGradle 6 + Gradle 8.8 + JDK 21   （main）
//   1.21.4  = ForgeGradle 7.0.40          + JDK 21   （FG7 需要 Gradle 9.x）
//   26.x    = ForgeGradle 7               + JDK 25
const JAVA_FALLBACK = [
    [/^1\.20(\.|$)/, 17],
    [/^1\.21\.1$/, 21],
    [/^1\.21/, 21],
    [/^2[0-9]/, 25],
];
const GRADLE_FALLBACK = [
    [/^1\.20(\.|$)/, '8.8'],
    [/^1\.21\.1$/, '8.8'],
    [/^1\.21/, '9.7.1'],
    [/^2[0-9]/, '9.7.1'],
];

function fail(message) {
    console.error('[ERROR] ' + message);
    process.exit(1);
}

function warn(message) {
    console.warn('[WARN] ' + message);
}

function readText(file) {
    if (!fs.existsSync(file)) fail('缺少文件：' + path.relative(ROOT, file));
    return fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
}

function parseProperties(text) {
    const map = {};
    for (const rawLine of text.split(/\r?\n/)) {
        const line = rawLine.trim();
        if (!line || line.startsWith('#') || line.startsWith('!')) continue;
        const eq = line.indexOf('=');
        if (eq <= 0) continue;
        map[line.slice(0, eq).trim()] = line.slice(eq + 1).trim();
    }
    return map;
}

/**
 * 极简 TOML 解析（只处理本仓库用到的子集），但会跟踪 ''' / """ 多行字符串，
 * 避免 description 里的 "version=\"...\"" 或多行文本中的 [xxx] 干扰解析。
 */
function parseTomlSections(text) {
    const top = {};
    const sections = {};
    let current = top;
    let triple = null;

    for (const rawLine of text.split(/\r?\n/)) {
        const raw = rawLine.trim();
        if (triple) {
            if (raw.indexOf(triple) >= 0) triple = null;
            continue;
        }
        if (!raw || raw.startsWith('#')) continue;

        // 多行字符串起始：key = ''' 或 key = """
        const tripleStart = raw.match(/^(?:[A-Za-z0-9_.-]+)\s*=\s*('''|""")/);
        if (tripleStart) {
            const marker = tripleStart[1];
            const rest = raw.slice(raw.indexOf(marker) + marker.length);
            if (rest.indexOf(marker) < 0) triple = marker;
            continue;
        }

        const header = raw.match(/^\[\[?([^\]]+)\]\]?$/);
        if (header) {
            const name = header[1].trim();
            if (!sections[name]) sections[name] = [];
            const block = {};
            sections[name].push(block);
            current = block;
            continue;
        }

        const kv = raw.match(/^([A-Za-z0-9_.-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^#\s]+))/);
        if (kv && current) {
            if (kv[2] !== undefined) current[kv[1]] = kv[2];
            else if (kv[3] !== undefined) current[kv[1]] = kv[3];
            else current[kv[1]] = kv[4];
        }
    }
    return { top, sections };
}

/** build.gradle 顶层 version：字面量或引用 gradle.properties 的标识符 */
function resolveBuildVersion(text, props) {
    const m = text.match(/^[ \t]*version[ \t]*=[ \t]*(?:['"]([^'"\n]+)['"]|([A-Za-z_][\w.]*))[ \t]*$/m);
    if (!m) return null;
    if (m[1]) return { value: m[1], source: 'literal' };
    const ident = m[2];
    if (props[ident]) return { value: props[ident], source: 'property:' + ident };
    warn('build.gradle 的 version 引用了 ' + ident + '，但 gradle.properties 中没有该属性，跳过一致性校验');
    return null;
}

function pickFallback(table, mcVersion) {
    for (const [re, value] of table) {
        if (re.test(mcVersion)) return value;
    }
    return null;
}

function parseTag(tag) {
    const withMc = tag.match(/^mc([\d.]+)-v(.+)$/);
    if (withMc) return { mc: withMc[1], version: withMc[2] };
    const plain = tag.match(/^v(.+)$/);
    if (plain) return { mc: null, version: plain[1] };
    return null;
}

function checkTag(tag, meta) {
    const parsed = parseTag(tag);
    if (!parsed) {
        fail('tag 格式不认识：' + tag + '（期望 v1.2.3 或 mc1.21.1-v2.0.0）');
    }
    if (parsed.version !== meta.mod_version) {
        fail(
            'tag 与模组版本不一致：tag=' + tag + ' -> 期望版本 ' + parsed.version +
            '，但 mods.toml/build.gradle 的版本是 ' + meta.mod_version +
            '\n        修复：把 mods.toml 的 version（以及 build.gradle 的 version/mod_version）改成 ' +
            parsed.version + ' 后重新打 tag；不要用旧 jar 直接上传发布。'
        );
    }
    if (parsed.mc && parsed.mc !== meta.mc_version) {
        fail('tag 的 Minecraft 版本与分支不一致：tag 声明 mc' + parsed.mc +
            '，但本分支 mods.toml 的 minecraft versionRange 是 ' + meta.mc_version);
    }
}

function main() {
    const argv = process.argv.slice(2);
    const tagIndex = argv.indexOf('--check-tag');
    const tagValue = tagIndex >= 0 ? argv[tagIndex + 1] : null;
    const asJson = argv.includes('--json');
    const quiet = argv.includes('--quiet');
    if (tagIndex >= 0 && !tagValue) fail('--check-tag 需要一个 tag 参数');

    const props = parseProperties(readText(GRADLE_PROPS));
    const toml = parseTomlSections(readText(MODS_TOML));
    const buildVersion = resolveBuildVersion(readText(BUILD_GRADLE), props);

    const modBlock = (toml.sections['mods'] || [])[0];
    if (!modBlock || !modBlock.version) fail('mods.toml 的 [[mods]] 段缺少 version 字段');
    let modVersion = modBlock.version;
    if (modVersion.indexOf('${') >= 0) {
        modVersion = props['mod_version'] || props['version'] || '';
        if (!modVersion) {
            fail('mods.toml 的 version 是占位符（' + modBlock.version +
                '），但 forge/gradle.properties 里没有 mod_version');
        }
    }

    // 一致性校验：build.gradle 与 mods.toml 版本不能漂移（v1.2.3 事故根因）
    if (buildVersion && buildVersion.value !== modVersion) {
        fail('版本漂移：forge/build.gradle version=' + buildVersion.value +
            '（' + buildVersion.source + '），但 mods.toml version=' + modVersion +
            '\n        两者必须一致，否则 jar 名称/内部版本会和发布 tag 不一致。');
    }

    let mcVersion = null;
    for (const dep of toml.sections['dependencies.mard_pixel'] || []) {
        if (dep.modId !== 'minecraft') continue;
        const range = dep.versionRange || '';
        const first = range.replace(/^[[(]/, '').split(',')[0].trim();
        if (first) { mcVersion = first; break; }
    }
    if (!mcVersion) fail('mods.toml 中找不到 [[dependencies.*]] 里 modId="minecraft" 的 versionRange');

    const meta = {
        mc_version: mcVersion,
        mod_version: modVersion,
        java_version: String(props['ci.javaVersion'] || pickFallback(JAVA_FALLBACK, mcVersion) || '21'),
        gradle_version: String(props['ci.gradleVersion'] || pickFallback(GRADLE_FALLBACK, mcVersion) || '8.8'),
        artifact_prefix: ARTIFACT_PREFIX,
    };

    if (tagValue) checkTag(tagValue, meta);

    if (!quiet && !asJson) {
        console.log('[meta] Minecraft ' + meta.mc_version + ' | mod ' + meta.mod_version +
            ' | JDK ' + meta.java_version + ' | Gradle ' + meta.gradle_version);
    }

    const lines = Object.keys(meta).map(function (k) { return k + '=' + meta[k]; });
    if (process.env.GITHUB_OUTPUT) {
        fs.appendFileSync(process.env.GITHUB_OUTPUT, lines.join('\n') + '\n', 'utf8');
    }
    if (asJson) {
        console.log(JSON.stringify(meta, null, 2));
    } else if (!quiet) {
        console.log(lines.join('\n'));
    }
}

main();
