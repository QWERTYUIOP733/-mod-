# MARD Pixel Blueprint（MARD拼豆图纸导入附属模组）

MARD像素色块模组的附属模组，支持导入拼豆图纸图片，自动映射MARD颜色，一键在世界中生成像素画。

## 依赖

- **Minecraft 1.20.1**
- **Forge 47.2.0+**
- **MARD Pixel Mod（主模组）v1.2.0+**

## 功能

| 功能 | 说明 |
|------|------|
| 图纸导入 | 支持导入PNG/JPG格式的拼豆图纸图片 |
| 颜色映射 | 自动将图片颜色映射到最近的MARD 221色号 |
| 图纸预览 | 映射后的色块布局预览，支持缩放 |
| 一键生成 | 在玩家位置一键生成图纸对应的像素画建筑 |
| 主模组集成 | 主UI按钮三自动检测附属模组，安装后可直接使用 |

## 安装

1. 确保已安装主模组 `mard_pixel_forge-1.2.0.jar`
2. 将 `mard_pixel_blueprint_forge-1.0.0.jar` 放入 `mods/` 文件夹
3. 启动游戏

## 使用方法

### 方式一：通过主UI按钮三
1. 按 **G** 键打开主模组UI
2. 点击 **按钮三「导入外部图纸」**
3. 自动打开附属模组的图纸导入界面

### 方式二：快捷键
- 按 **B** 键直接打开图纸导入界面（可在控制设置中修改）

### 图纸导入界面操作
1. 将图纸图片放入 `config/mard_pixel_blueprint/blueprints/` 目录
2. 在左侧列表中选择图纸文件
3. 右侧预览映射后的色块布局
4. 点击 **「在世界中生成」** 按钮
5. 图纸将在玩家位置生成

## 图纸要求

- 支持格式：PNG、JPG、JPEG
- 最大尺寸：64x64像素（超过自动缩放）
- 建议使用像素风格的拼豆图纸
- 透明像素和纯白/纯黑像素会被跳过

## 项目结构

```
blueprint/
├── build.gradle              # 构建配置
├── settings.gradle           # 项目设置
├── gradle.properties         # 版本属性
└── src/main/
    ├── java/com/mard/blueprint/
    │   ├── MardBlueprintMod.java           # 主类
    │   ├── client/
    │   │   ├── MardBlueprintClient.java    # 客户端主类
    │   │   └── BlueprintScreen.java        # 图纸导入UI
    │   ├── blueprint/
    │   │   ├── Blueprint.java               # 图纸数据类
    │   │   ├── BlueprintLoader.java         # 图纸加载器
    │   │   └── ColorMapper.java             # 颜色映射器
    │   └── network/
    │       └── BlueprintNetwork.java        # 网络包
    └── resources/
        ├── META-INF/mods.toml              # 模组元数据
        ├── pack.mcmeta                      # 资源包元数据
        └── assets/mard_pixel_blueprint/lang/
            ├── en_us.json                    # 英文语言
            └── zh_cn.json                    # 中文语言
```

## 构建

需要：**JDK 17**、**Gradle 8.8**

```bash
cd blueprint
gradle build
```

构建产物位于 `build/libs/` 目录。

## 版本历史

| 版本 | 说明 |
|------|------|
| v1.0.0 | 初始版本，支持图纸导入、颜色映射、预览、一键生成 |

## 开源地址

https://github.com/QWERTYUIOP733/-mod-

## 许可

MIT License
