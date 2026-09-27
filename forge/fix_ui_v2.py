file = r"D:\文档\GitHub\游戏mod开发\forge\src\main\java\com\mard\pixel\forge\client\MardColorScreen.java"
with open(file, 'r', encoding='utf-8') as f:
    content = f.read()

# 修改 initMainPage
old_init = '''    private void initMainPage() {
        // 大面板布局：左右分栏，左侧按钮，右侧说明
        int panelX = 50;
        int panelY = 80;
        int panelW = width - 100;
        int panelH = height - 160;

        // 左侧按钮区
        int leftAreaW = (int) (panelW * 0.45);
        int btnW = Math.min(240, leftAreaW - 60);
        int btnH = 32;
        int btnX = panelX + (leftAreaW - btnW) / 2;
        int btnCenterY = panelY + panelH / 2;
        int gapY = 70;

        // 按钮一：颜色选取
        addRenderableWidget(Button.builder(Component.literal("颜色选取"), btn -> {
            currentPage = Page.SWATCHES;
            scrollOffset = 0;
            init();
        }).bounds(btnX, btnCenterY - gapY / 2 - btnH / 2, btnW, btnH).build());

        // 按钮二：输入想用的色号
        addRenderableWidget(Button.builder(Component.literal("输入想用的色号"), btn -> {
            currentPage = Page.INPUT;
            init();
        }).bounds(btnX, btnCenterY + gapY / 2 - btnH / 2, btnW, btnH).build());
    }'''

new_init = '''    private void initMainPage() {
        // 大面板布局参数
        int panelX = 40;
        int panelY = 70;
        int panelW = width - 80;
        int panelH = height - 130;

        // 左侧按钮区：占面板左侧45%
        int leftW = (int) (panelW * 0.45);
        int btnW = Math.min(220, leftW - 50);
        int btnH = 30;
        int btnX = panelX + (leftW - btnW) / 2;

        // 按钮垂直居中，间距60
        int btnCenterY = panelY + panelH / 2;
        int btn1Y = btnCenterY - 45;
        int btn2Y = btnCenterY + 15;

        // 按钮一：颜色选取
        addRenderableWidget(Button.builder(Component.literal("颜色选取"), btn -> {
            currentPage = Page.SWATCHES;
            scrollOffset = 0;
            init();
        }).bounds(btnX, btn1Y, btnW, btnH).build());

        // 按钮二：输入想用的色号
        addRenderableWidget(Button.builder(Component.literal("输入想用的色号"), btn -> {
            currentPage = Page.INPUT;
            init();
        }).bounds(btnX, btn2Y, btnW, btnH).build());
    }'''

content = content.replace(old_init, new_init)

# 修改 renderMainPage 中的面板参数
old_render_params = '''        // 大面板布局参数（与initMainPage一致）
        int panelX = 50;
        int panelY = 80;
        int panelW = width - 100;
        int panelH = height - 160;

        // 标题（面板上方）
        String title = "彩色方块扩展";
        g.drawString(font, Component.literal(title), (width - font.width(title)) / 2, panelY - 35, 0xFFFFFF);

        // 当前模式（标题下方）
        String modeText = "当前模式：" + getGameModeName();
        int modeColor = isSurvivalMode() ? 0xFF5555 : 0x55FF55;
        g.drawString(font, Component.literal(modeText), (width - font.width(modeText)) / 2, panelY - 18, modeColor);'''

new_render_params = '''        // 大面板布局参数（与initMainPage一致）
        int panelX = 40;
        int panelY = 70;
        int panelW = width - 80;
        int panelH = height - 130;

        // 标题（面板上方居中）
        String title = "彩色方块扩展";
        g.drawString(font, Component.literal(title), (width - font.width(title)) / 2, panelY - 28, 0xFFFFFF);

        // 当前模式（标题下方）
        String modeText = "当前模式：" + getGameModeName();
        int modeColor = isSurvivalMode() ? 0xFF5555 : 0x55FF55;
        g.drawString(font, Component.literal(modeText), (width - font.width(modeText)) / 2, panelY - 14, modeColor);'''

content = content.replace(old_render_params, new_render_params)

# 修改说明面板参数
old_info = '''        // 右侧说明面板
        int infoPanelX = panelX + (int) (panelW * 0.50);
        int infoPanelW = (int) (panelW * 0.45);
        int infoPanelY = panelY + 20;
        int infoPanelH = panelH - 40;'''

new_info = '''        // 右侧说明面板
        int infoPanelX = panelX + (int) (panelW * 0.50);
        int infoPanelW = (int) (panelW * 0.45);
        int infoPanelY = panelY + 15;
        int infoPanelH = panelH - 30;'''

content = content.replace(old_info, new_info)

# 修改说明文字渲染
old_text_render = '''        g.drawString(font, Component.literal("mod 使用说明"), infoPanelX + 12, infoPanelY + 10, 0xFFFFAA);'''
new_text_render = '''        g.drawString(font, Component.literal("mod 使用说明"), infoPanelX + 10, infoPanelY + 8, 0xFFFFAA);'''
content = content.replace(old_text_render, new_text_render)

old_lines = '''        int y = infoPanelY + 28;
        int lineH = 12;
        for (String line : lines) {
            if (y + 10 < infoPanelY + infoPanelH - 5) {
                g.drawString(font, Component.literal(line), infoPanelX + 12, y, 0xCCCCCC);
            }
            y += lineH;
        }'''
new_lines = '''        int y = infoPanelY + 24;
        int lineH = 11;
        for (String line : lines) {
            if (y + 10 < infoPanelY + infoPanelH - 3) {
                g.drawString(font, Component.literal(line), infoPanelX + 10, y, 0xCCCCCC);
            }
            y += lineH;
        }'''
content = content.replace(old_lines, new_lines)

# 修改版本号位置
old_version = '''        // 版本号（面板下方）
        String bottomText = "彩色方块扩展 v1.2.0";
        g.drawString(font, Component.literal(bottomText),
                (width - font.width(bottomText)) / 2, panelY + panelH + 15, 0x888888);'''
new_version = '''        // 版本号（面板下方居中）
        String bottomText = "彩色方块扩展 v1.2.0";
        g.drawString(font, Component.literal(bottomText),
                (width - font.width(bottomText)) / 2, panelY + panelH + 10, 0x888888);'''
content = content.replace(old_version, new_version)

with open(file, 'w', encoding='utf-8') as f:
    f.write(content)

print("Forge版本G键UI已同步修改，使用与Fabric相同的布局参数")
