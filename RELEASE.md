# 发布与维护规范（RELEASE.md）

本文件说明版本命名、单一版本源、CI 行为与发布流程。目标：**任何一次发布都能被 CI 复现，jar 名、内部版本号、tag 三者永远一致**。

---

## 1. 版本与 tag 规范

| 规则 | 说明 |
|---|---|
| tag 格式 | `mc<Minecraft版本>-v<模组版本>`，例如 `mc26.3-v3.0.0`、`mc1.21.4-v2.1.0` |
| 历史 tag | `v1.2.0` ~ `v1.2.3`、`v1.20.1-stable` 属早期格式，不再新增同类 tag（其中 `v1.20.1-stable` 只有 tag 没有 Release，见 §7） |
| 一条版本线一个分支 | `mc/1.20.1`、`main`（1.21.1 线）、`mc/1.21.4`、`mc/26.3`；tag 打在该分支上（仓库里**没有** `mc/1.21.1` 分支） |
| 禁止 | 不要用 `git tag -f` 覆盖已发布 tag；不要手工在网页上把旧 jar 挂到新版本号下 |

> 已发布的 1.20.1 请引导到 **`mc1.20.1-v1.2.1`**；`v1.2.2` / `v1.2.3` 是 1.20.1 线的历史过渡版本，其中 v1.2.3 的资产与 v1.2.2 完全相同（同一 SHA-256），不要再作为推荐下载。

## 2. 单一版本源

版本号出现在三个地方，必须同步：

| 位置 | 当前写法 | 角色 |
|---|---|---|
| `forge/build.gradle` | `version = '2.0.0'` | 决定 jar 文件名（`base.archivesName` + `version`）与 manifest 的 `Implementation-Version` |
| `forge/src/main/resources/META-INF/mods.toml` | `version="2.0.0"` | 游戏内显示的模组版本、Forge 依赖解析 |
| git tag | `mc1.21.1-v2.0.0` | 发布触发器与 Release 名称 |

`tools/ci-meta.js` 会校验前两者一致，`release.yml` 会校验它们与 tag 一致——**不一致时 CI 直接失败，不会再产出错版本 jar**。

> v1.2.3 事故的根因就是：只升级了构建脚本，`build.gradle` / `mods.toml` 的版本仍是 1.2.1，于是 Release 标题写 1.2.3、资产却是 1.2.1 的旧 jar。

**可选（推荐）**：把版本收敛到 `forge/gradle.properties` 的一处 `mod_version`，由 `build.gradle` 与 `mods.toml` 引用。补丁在 `optional/version-single-source/`，应用前请本地跑一次 `gradle build` 验证。

## 3. 分支矩阵与 CI 参数

CI 从 `mods.toml` 的 `minecraft versionRange` 推导 Minecraft 版本，再按下表或 `forge/gradle.properties` 里的 `ci.*` 取 JDK / Gradle：

| 分支 | Minecraft | JDK | Gradle | 备注 |
|---|---|---|---|---|
| `mc/1.20.1` | 1.20.1 | 17 | 8.8 | Forge 47.x / ForgeGradle 6 |
| `main`（= 1.21.1 线，无单独的 `mc/1.21.1` 分支） | 1.21.1 | 21 | 8.8 | Forge 52.x / ForgeGradle 6 |
| `mc/1.21.4` | 1.21.4 | 21 | 9.7.1 | **ForgeGradle 7.0.40**，FG7 需要 Gradle 9.x；另需 JDK 8 时用 `org.gradle.java.installations.paths` |
| `mc/26.3` | 26.3 | 25 | 9.7.1 | Forge 66.x / ForgeGradle 7 |

需要覆盖时在分支的 `forge/gradle.properties` 里显式声明（`apply-fixes.ps1` 会按分支自动写入正确默认值）：

```properties
ci.javaVersion=21
ci.gradleVersion=8.8
```

> `.github/workflows/*.yml` 只需要在 **每个分支各有一份相同文件**（本补丁已加到 `main`，其余分支 cherry-pick 同一个 commit 即可）。workflow 本身不含任何版本号，因此四个分支可以共用。

> `tools/generate_resources.js` / `tools/verify.js` 会自动探测资源目录：新分支用 `shared/assets/mard_pixel`，`mc/1.20.1` 老分支用 `forge/src/main/resources/assets/mard_pixel`；老分支若没有 `MardPalette.java` 则跳过 Java 调色板校验。因此同一套工具/CI 可以同时服务四个版本分支。

## 4. 发布流程

```text
1. 在版本分支上改代码
2. 同时更新 mods.toml 的 version + build.gradle 的 version（§2）
3. 在 forge/CHANGELOG.md 顶部加一节：## [x.y.z] - YYYY-MM-DD
4. 本地三连：node tools/generate_resources.js && node tools/verify.js && node tools/ci-meta.js
5. 提交并推送分支，确认 Build workflow 绿
6. 打 tag 并推送：git tag mc1.21.1-v2.0.0 && git push origin mc1.21.1-v2.0.0
7. Release workflow 自动：校验 tag/版本 → 构建 → 校验 jar 内部版本 → 生成 SHA256SUMS.txt → 发布 Release
8. 检查 Release 页：标题 [Forge <MC>] v<版本>、资产名 mard_pixel_forge-<版本>.jar、说明来自 CHANGELOG
```

本地核对清单（发布前）：

- [ ] `node tools/verify.js` 全绿
- [ ] `mods.toml` 与 `build.gradle` 版本相同，且等于准备打的 tag
- [ ] `forge/CHANGELOG.md` 有对应 `## [x.y.z]` 段落
- [ ] 该分支 CI 已绿（`Build` workflow）
- [ ] changelog 里明确写了「是否需要玩家重新生成/替换存档」之类的注意事项（如有）

## 5. 资产命名与校验

| 项 | 规范 |
|---|---|
| Forge 资产 | `mard_pixel_forge-<模组版本>.jar` |
| 校验和 | 与 jar 一起上传 `SHA256SUMS.txt` |
| Release 标题 | `[Forge <Minecraft版本>] v<模组版本>` |
| 旧资产 | `color_blocks_extension-*.jar` 是 1.20.1 早期命名，保留历史 Release 不再改名 |
| 重复发布 | 允许重跑：`release.yml` 检测到同名 Release 时用 `--clobber` 覆盖资产并更新说明 |

## 6. CI 说明

| Workflow | 触发 | 做什么 |
|---|---|---|
| `.github/workflows/build.yml` | 任意分支 push（受限路径）、PR、手动 | 推导版本/JDK/Gradle → 生成资源 → `verify.js` → `gradle build` → 上传 jar 产物 |
| `.github/workflows/release.yml` | push tag `v*` / `mc*-v*` | 同上 + tag 版本校验 + jar 内部版本校验 + SHA256SUMS + 发布 Release |

两者都不需要仓库 Secrets（`GITHUB_TOKEN` 由 Actions 自动提供，`release.yml` 已声明 `permissions: contents: write`）。

**注意**：如果是在网页上「Draft a new release」并新建 tag，workflow 不会等构建完成，容易发出空 Release。正确做法是 `git push` 一个 tag，让 CI 去创建 Release。

## 7. 一次性修正历史遗留（需要 `gh` 登录）

```bash
# 1) 更新仓库简介（当前还停在 v1.2.0）
gh repo edit QWERTYUIOP733/-mod- \
  --description "彩色方块扩展（MARD 221 色像素画色块）。Forge 主仓库，支持 Minecraft 1.20.1 / 1.21.1 / 1.21.4 / 26.3，Fabric 版见 color-blocks-extension-fabric。"

# 2) 删除没有任何 Release 的空 tag（本地 + 远端）
git tag -d v1.20.1-stable
git push origin :refs/tags/v1.20.1-stable

# 3) 处理 v1.2.3（构建脚本更新、二进制与 v1.2.2 完全相同）
#    推荐：保留 tag 作为历史，但在 Release 说明顶部加提示（--notes 会整体覆盖，必须拼接旧说明）
gh release view v1.2.3 --json body --jq .body > v123-old.md
{
  echo "> ⚠️ 本版本仅升级构建脚本（Forge 47.4.23 / ForgeGradle 6.0.54），"
  echo "> 发布资产与 v1.2.2 完全相同（SHA-256 35211cd5…），请改用 mc1.20.1-v1.2.1。"
  echo
  cat v123-old.md
} > v123-new.md
gh release edit v1.2.3 --notes-file v123-new.md

#    或者彻底删除该 Release 与 tag（它没有实际产物变化）
# gh release delete v1.2.3 --yes
# git push origin :refs/tags/v1.2.3
# git tag -d v1.2.3

# 4) v1.2.2 的资产名是 color_blocks_extension-1.2.1.jar（版本号写错）
#    保留文件、只改展示名不方便，建议在说明里注明；若重新构建过，再上传正确命名的新资产：
# gh release upload v1.2.2 path/to/mard_pixel_forge-1.2.1.jar --clobber
```

可选（**会重写历史，谨慎**）：把 `release/` 里的二进制从 git 历史中彻底清掉，减小仓库体积：

```bash
pip install git-filter-repo
git filter-repo --path release --invert-paths
# 之后所有协作者需要重新 clone
```

## 8. 回滚

| 场景 | 做法 |
|---|---|
| 版本号写错、tag 还没发布 | 删除本地 tag，改版本号后重新打 |
| tag 已推、CI 已发 Release | 修版本号 → 重新打新 tag（递增 patch）→ CI 覆盖发布；不要把旧 tag 强推成新内容 |
| 需要撤回某个 Release | `gh release edit <tag> --draft`（先下架），确认后再决定删除 |
| 紧急热修 | 从出错 tag 拉分支 → 修 → 打新 patch 版本 tag |

## 9. 相关文件

- `.github/workflows/build.yml` / `release.yml`：CI
- `tools/ci-meta.js`：版本/JDK/Gradle 推导与一致性校验
- `tools/verify.js`：色板 ↔ Java 调色板 ↔ 资源 ↔ lang 一致性
- `tools/generate_resources.js`：由色板生成 blockstate/item model/lang，并清理孤儿资源
- `tools/changelog-note.js`：从 CHANGELOG 抽取发布说明
- `.gitmodules`：`fabric/` 子模块指向 Fabric 独立仓库

## 10. 推荐的本地工具（按 GitHub 真实 star，2026-10）

| 用途 | 工具 | Star | 安装 |
|---|---|---|---|
| 版本控制 | git | 63,981 | `winget install --id Git.Git -e` |
| 发 Release / 改仓库信息 | GitHub CLI | 46,727 | `winget install --id GitHub.cli -e` |
| workflow 静态检查 | actionlint | 4,347 | `winget install --id rhysd.actionlint -e`（或官方 release zip） |
| 本地跑 Actions | act | 72,441 | `winget install --id nektos.act -e`（需 Docker） |
| PowerShell 脚本检查 | PSScriptAnalyzer | 2,179 | `Install-Module PSScriptAnalyzer -Scope CurrentUser` |
| 清理历史大文件（可选） | git-filter-repo | 13,440 | `pip install git-filter-repo` |

提交前建议至少跑：`actionlint .github/workflows/*.yml` + `Invoke-ScriptAnalyzer -Path tools`（本仓库当前只有两个 .ps1 在修复包里，仓库本身没有 .ps1）。
