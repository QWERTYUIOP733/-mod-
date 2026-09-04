# MARD 像素色块 Mod（Minecraft 1.20.1 · Forge）

一套将 **MARD 拼豆官方 221 基础色**标准色卡做成 Minecraft 实心色块的像素画 Mod。

> **当前版本仅支持 Forge 加载器。** Fabric 版本因构建兼容性问题已停止维护，后续如有需要可单独重启。

- 版本：v1.2.0
- 游戏版本：Minecraft 1.20.1
- 加载器：Forge 47.2.0+
- 色卡：MARD 官方 221 基础色（A–H/M 系列，色准已校准与官方色卡完全一致）

---

## 功能一览

| 功能 | 说明 |
|---|---|
| 221 基础色块 | 每个色号一个实心色块，颜色使用 MARD 官方标准 RGB，色准标准 |
| 创造标签页 | 按字母系列分类（A、B、C、D、E、F、G、H、M），标签页名称仅为字母 |
| 主 UI（快捷键 **G**） | 三按钮主界面：颜色选取 / 色号输入 / 图纸导入 |
| 按钮1：颜色选取 | 浏览全部 221 色号网格，点击给一组（64个），支持连续选择 |
| 按钮2：色号输入 | 输入色号（如 A1、B5、M3）后自动放入快捷栏一组（64个） |
| 按钮3：图纸导入 | 开发中，敬请期待 |
| 自定义颜色 | PS 风格取色器，支持新增/删除自定义色号，编号循环回收机制 |
| PNG 色卡导入 | 支持导入自制色卡（PNG 格式），自动提取颜色生成色系 |
| 物品名称双行显示 | 第一行：色号编号（白色）；第二行：RGB 值（灰色） |

---

## 安装

### Forge 端

1. 安装 Minecraft 1.20.1 + Forge 47.2.0+
2. 把 `mard_pixel_forge-1.2.0.jar` 放入 `mods/` 文件夹
3. 启动游戏

---

## 使用方法

### 主 UI（推荐）

- 按 **G** 打开主界面
- **按钮1：MARD颜色选取** — 浏览全部色号网格，点击任意色号 → 对应物品一组（64个）进入背包，支持连续选择
- **按钮2：输入想用的色号** — 在输入框中输入色号（如 A1、B5、M3），点击确认 → 自动放入快捷栏一组（64个）
- **按钮3：导入外部图纸** — 开发中，敬请期待

### 创造模式标签页

- 在创造模式物品栏中切换到 MARD 系列标签页
- 标签页按字母分类：A、B、C、D、E、F、G、H、M
- 每个标签页包含对应系列的所有色块
- 自定义色块在最后一个系列标签页中

### 自定义颜色

1. 按 **G** 打开主界面
2. 进入自定义颜色功能
3. 使用 PS 风格取色器选择颜色
4. 点击「新增」创建自定义色块
5. 自定义色块自动出现在物品栏中
6. 若物品栏已满，自动加载到背包中
7. 若都没有空间，则直接扔出到地面
8. 支持删除自定义色号，编号会循环回收利用

### 物品名称显示

- 每个色块的物品名称采用双行显示格式
- **第一行**：色号编号（白色，如 `A1`）
- **第二行**：RGB 值（灰色，如 `RGB #FAF4C8`）
- 悬停物品时可查看完整信息

---

## 色号格式

支持以下色号格式：

| 系列 | 色数 | 示例 |
|---|---|---|
| A 系列（黄橙系） | 26 色 | A1, A2, ..., A26 |
| B 系列（绿色系） | 32 色 | B1, B2, ..., B32 |
| C 系列（蓝青系） | 29 色 | C1, C2, ..., C29 |
| D 系列（紫色系） | 26 色 | D1, D2, ..., D26 |
| E 系列（粉色系） | 24 色 | E1, E2, ..., E24 |
| F 系列（红色系） | 25 色 | F1, F2, ..., F25 |
| G 系列（棕色系） | 21 色 | G1, G2, ..., G21 |
| H 系列（灰黑系） | 23 色 | H1, H2, ..., H23 |
| M 系列（混色系） | 15 色 | M1, M2, ..., M15 |
| **合计** | **221 色** | |

---

## PNG 色卡导入

把自制的色卡图片（PNG 格式）放入 **`config/mard_pixel/import/`** 目录，然后在主 UI 中使用导入功能，模组会自动：

1. 扫描目录下所有 `.png` 文件
2. 读取每张图片的所有像素，提取独特颜色（自动去重）
3. 跳过纯白、纯黑、透明像素
4. 以文件名作为色系名，自动生成可切换的色系
5. 保存到配置文件（全局共享）

### 使用步骤

1. 把色卡 PNG 复制到 `config/mard_pixel/import/`（首次运行会自动创建该目录）
2. 进入游戏，按 **G** 打开主界面
3. 使用导入功能扫描色卡
4. 导入的色系可在主 UI 中查看和使用

### 注意事项

- 图片中的颜色会自动去重，相同颜色只保留一个
- 色系中的颜色按图片中首次出现的顺序排列
- 支持任意尺寸的 PNG，建议色卡图片不要太大（颜色数量建议 < 500）

---

## 从源码构建

需要：**JDK 17**、**Gradle 8.8**、**Node.js 18+**（都加入 PATH）。

### 本地构建

```bash
cd forge
gradle build
```

构建产物位于 `forge/build/libs/` 目录。

### GitHub Actions 云构建

项目已配置 GitHub Actions 自动构建，每次提交到 `main` 分支会自动触发构建：

1. 提交代码到 GitHub 仓库
2. GitHub Actions 自动运行构建工作流
3. 构建成功后可在 Actions 页面下载 Artifacts
4. 下载并解压得到 `mard_pixel_forge-1.2.0.jar`

---

## 目录结构

```
mard-pixel-mod/
├── colors/                  颜色数据源（MARD 221 基础色）
│   └── mard_295.json       221 色数据（code/hex/series）
├── shared/                  共享：Java 源码 + 资源
│   ├── java/com/mard/pixel/common/   共享逻辑（色卡/颜色数学/自定义色存储/PNG导入）
│   └── assets/mard_pixel/            资源（模型、lang、blockstates、item models）
├── forge/                   Forge 工程
│   ├── build.gradle         构建配置
│   └── src/main/java/com/mard/pixel/forge/
│       ├── MardPixelForge.java       主类（方块/物品注册）
│       ├── MardBlock.java            基础方块类
│       ├── MardBlockItem.java        方块物品类（tooltip显示）
│       ├── MardCustomBlock.java      自定义方块类
│       ├── MardCustomBlockEntity.java 自定义方块实体
│       ├── MardNetwork.java          网络包
│       └── client/
│           ├── MardPixelForgeClient.java  客户端主类（按键/颜色处理器/tooltip清理）
│           └── MardColorScreen.java        主UI界面（三按钮/颜色选取/色号输入）
├── tools/
│   ├── generate_resources.js  资源生成脚本（Node，生成221个blockstates和item models）
│   └── verify.js              验证脚本
├── .github/workflows/
│   └── build.yml              GitHub Actions 构建配置
└── README.md                  本说明文件
```

---

## 配置文件

- `config/mard_pixel_custom.json` —— 自定义色号（全局共享，不按存档/版本隔离）
- `config/mard_pixel/import/` —— PNG 色卡导入目录（放入 .png 后使用导入功能）
- `config/mard_pixel/mard_pixel_imported_palettes.json` —— 已导入的 PNG 色系数据（自动生成）

---

## 版本历史

| 版本 | 说明 |
|---|---|
| v1.2.0 | 清除70个扩展色，校准221基础色色准，优化UI引导词，重构主界面三按钮设计 |
| v1.1.0 | 添加色号转换功能，支持品牌色号转换 |
| v1.0.0 | 初始版本，295色基础功能 |

---

## 开源地址

https://github.com/QWERTYUIOP733/-mod-

## 许可

MIT License
