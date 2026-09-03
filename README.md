# MARD 像素色块 Mod（Minecraft 1.20.1 · Forge）

一套将 **MARD 拼豆 295 色系**标准色卡做成 Minecraft 实心色块的像素画 Mod。

> **当前版本仅支持 Forge 加载器。** Fabric 版本因构建兼容性问题已停止维护，后续如有需要可单独重启。

- 版本：v1.2.0
- 游戏版本：Minecraft 1.20.1
- 加载器：Forge 47.2.0
- 色卡：MARD 官方 295 色（221 标准 A–H/M + 74 特殊 P/Q/R/T/Y/ZG，dehumaker.cn 对照表）

---

## 功能一览

| 功能 | 说明 |
|---|---|
| 295 标准色块 | 每个色号一个实心色块，颜色使用 MARD 官方标准 RGB，色准标准 |
| 创造标签页 | 「MARD 像素色块」标签页一键取用全部 295 色 + 自定义色块 |
| 色板 UI（快捷键 **G**） | 打开色板，点色块即入包；顶部按钮切换色系 |
| 色系切换 | MARD / 导入的 PNG 色系 / 自定义 一键切换 |
| 整体换包 | 把背包里已有色块按当前色系整体就地转换（CIEDE2000 最近色） |
| PNG 色卡导入 | 把自制色卡 PNG 放入 `config/mard_pixel/import/`，点「导入PNG」自动提取颜色生成色系 |
| 自定义色号 | HSV 三角 + RGB 色环 + 屏幕吸取，自定义色全局共享（config 保存） |
| 联机同步 | 自定义色随玩家加入自动同步 |

---

## 安装

### Forge 端

1. 安装 Minecraft 1.20.1 + Forge 47.2.0
2. 把 `MARD_Pixel_Forge_1.20.1_v1.2.0.jar` 放入 `mods/` 文件夹
3. 启动游戏

---

## 使用方法

### 色板（推荐）

- 按 **G** 打开色板
- 顶部按钮切换色系（MARD / 导入的 PNG 色系 / CUSTOM）
- 点击任意色块 → 对应物品进入背包
- 点「整体换包」→ 背包内所有本模组色块按当前色系整体转换
- 点「导入PNG」→ 扫描 `config/mard_pixel/import/` 目录，自动导入所有 PNG 色卡
- 选中导入的色系后，点「删除该色系」可移除

### 自定义色号（CUSTOM 页）

1. 切换到 **CUSTOM** 色系
2. 在 **RGB 色环** 上点选色相
3. 在 **HSV 三角** 上点选饱和度/明度
4. 输入名称 → 点「新增」→ 写入 `config/mard_pixel_custom.json`（全局共享，不按存档/版本隔离）
5. 点「吸取颜色」后再点任意色块，可把该色作为当前编辑色
6. 点「删除」移除最后一个自定义色（也可用命令精确删除）

### 命令

| 命令 | 说明 |
|---|---|
| `/mardp brands` | 列出内置品牌 + 外部色系及色号数量 |
| `/mardp find <#RRGGBB>` | 任意颜色 → 最近的 MARD 色 |
| `/mardp convert <品牌> <色号>` | 品牌色 → 最近 MARD 色（例：`/mardp convert perler P20`） |
| `/mardp give <目标>` | 直接给物品。目标格式：`MARD:A1` / `BRAND:perler:P20` / `CUSTOM:C0001` / `CUSTOM_RAW:16711680` |
| `/mardp switch <色系>` | 整体换包（例：`/mardp switch perler`） |
| `/mardp reload` | 重新扫描 `config/mard_pixel_brands/` |

---

## PNG 色卡导入

把自制的色卡图片（PNG 格式）放入 **`config/mard_pixel/import/`** 目录，然后在色板 UI 中点「导入PNG」按钮，模组会自动：

1. 扫描目录下所有 `.png` 文件
2. 读取每张图片的所有像素，提取独特颜色（自动去重）
3. 跳过纯白、纯黑、透明像素（可在源码中调整）
4. 以文件名作为色系名，自动生成可切换的色系
5. 保存到 `config/mard_pixel/mard_pixel_imported_palettes.json`（全局共享）

### 使用步骤

1. 把色卡 PNG 复制到 `config/mard_pixel/import/`（首次运行会自动创建该目录）
2. 进入游戏，按 **G** 打开色板
3. 点右上角「导入PNG」按钮
4. 顶部按钮列表会出现新导入的色系（以文件名命名）
5. 点击色系按钮 → 显示该色系的所有色块 → 点击色块入包
6. 选中导入的色系后，点「删除该色系」可移除

### 注意事项

- 图片中的颜色会自动去重，相同颜色只保留一个
- 色系中的颜色按图片中首次出现的顺序排列
- 每个色块的编号格式为 `色系名前3字母+序号`（如 `MYC1`, `MYC2`）
- 导入的色块使用 NBT 染色的自定义方块（`mard_custom`），颜色精确还原
- 同名色系不会重复导入（跳过已存在的）
- 支持任意尺寸的 PNG，建议色卡图片不要太大（颜色数量建议 < 500）

---

## 从源码构建

需要：**JDK 17**、**Gradle 8.8**、**Node.js 18+**（都加入 PATH）。

1. 双击运行 **`build.bat`**
2. 脚本自动：生成资源 → 构建 Forge → 收集 JAR 到 `dist/`
3. 首次构建会下载 Minecraft/Forge 依赖，耗时较长（网络需可达 maven.minecraftforge.net）

也可手动：
```
cd forge && gradle build
```

---

## 目录结构

```
mard-pixel-mod/
├── build.bat                一键构建脚本
├── colors/                  数据源（MARD 295 色、外部色卡示例）
├── shared/                  共享：Java 源码(common) + 资源(assets)
│   ├── java/com/mard/pixel/common/   共享逻辑（色卡/转换/存储/解析）
│   └── assets/mard_pixel/            资源（模型、lang、内置品牌色卡）
├── forge/                   Forge 工程（build.gradle + 主类 + UI）
├── tools/generate_resources.js  资源生成脚本（Node）
└── dist/                    构建产物（JAR）
```

## 配置文件

- `config/mard_pixel_custom.json` —— 自定义色号（全局共享）
- `config/mard_pixel/import/` —— PNG 色卡导入目录（放入 .png 后点「导入PNG」）
- `config/mard_pixel/mard_pixel_imported_palettes.json` —— 已导入的 PNG 色系数据（自动生成）

## 许可

MIT License
