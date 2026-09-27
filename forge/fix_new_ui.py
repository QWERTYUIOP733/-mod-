file = r"D:\文档\GitHub\游戏mod开发\forge\src\main\java\com\mard\pixel\forge\client\MardColorScreen.java"
with open(file, 'r', encoding='utf-8') as f:
    content = f.read()

# ========== 1. 修改 initMainPage 按钮位置 ==========
old_init = '''    private void initMainPage() {
        // 按钮区域：左侧 30% 宽度，按钮偏左且较窄
        int btnAreaW = (int) (width * 0.30);
        int btnW = Math.min(180, btnAreaW - 20);
        int btnH = 30;
        int btnX = Math.max(25, (btnAreaW - btnW) / 2 + 8);
        // 按钮区域居中偏下，确保不与顶部警告条重叠
        int centerY = Math.max(height / 2 + 20, 160);
        int gapY = 50;

        // 按钮一：颜色选取
        addRenderableWidget(Button.builder(Component.literal("颜色选取"), btn -> {
            currentPage = Page.SWATCHES;
            scrollOffset = 0;
            init();
        }).bounds(btnX, centerY - gapY / 2 - btnH / 2, btnW, btnH).build());

        // 按钮二：输入想用的色号
        addRenderableWidget(Button.builder(Component.literal("输入想用的色号"), btn -> {
            currentPage = Page.INPUT;
            init();
        }).bounds(btnX, centerY + gapY / 2 - btnH / 2, btnW, btnH).build());
    }'''

new_init = '''    private void initMainPage() {
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

content = content.replace(old_init, new_init)

# ========== 2. 修改 renderMainPage 整体布局 ==========
old_render_start = '''    private void renderMainPage(GuiGraphics g) {
        // 标题（居中偏上）
        String title = "彩色方块扩展";
        g.drawString(font, Component.literal(title), (width - font.width(title)) / 2, 40, 0xFFFFFF);

        // 在标题下方显示当前游戏模式（生存模式用红色警告）
        String modeText = "当前模式：" + getGameModeName();
        int modeColor = isSurvivalMode() ? 0xFF5555 : 0x55FF55;
        g.drawString(font, Component.literal(modeText), (width - font.width(modeText)) / 2, 58, modeColor);

        // 生存模式下显示红色警告条
        if (isSurvivalMode()) {
            String warnText = "⚠ 生存模式：仅可查看颜色，合成请使用方块染色台";
            int warnWidth = Math.min(font.width(warnText) + 24, width - 40);
            int warnX = (width - warnWidth) / 2;
            int warnY = 72;
            int warnH = 22;
            // 警告条背景（半透明外框 + 实心内框）
            g.fill(warnX, warnY, warnX + warnWidth, warnY + warnH, 0x99CC0000);
            g.fill(warnX + 1, warnY + 1, warnX + warnWidth - 1, warnY + warnH - 1, 0xFFFF4444);
            // 文字居中显示
            int textX = warnX + (warnWidth - font.width(warnText)) / 2;
            g.drawString(font, Component.literal(warnText), textX, warnY + 7, 0xFFFFFF);
        }

        // 右侧：mod 使用说明（右侧 40% 宽度）
        int infoAreaX = (int) (width * 0.55);
        int infoAreaW = (int) (width * 0.35);
        int infoY = Math.max(110, height / 2 - 90);
        int infoH = Math.min(200, height - 180);

        // 说明框背景
        g.fill(infoAreaX - 6, infoY - 6, infoAreaX + infoAreaW + 6, infoY + infoH + 6, 0xFF1a1a1a);
        g.fill(infoAreaX - 5, infoY - 5, infoAreaX + infoAreaW + 5, infoY + infoH + 5, 0xFF2a2a2a);

        g.drawString(font, Component.literal("mod 使用说明"), infoAreaX, infoY, 0xFFFFAA);'''

new_render_start = '''    private void renderMainPage(GuiGraphics g) {
        // 大面板布局参数（与initMainPage一致）
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
        g.drawString(font, Component.literal(modeText), (width - font.width(modeText)) / 2, panelY - 18, modeColor);

        // 大面板半透明背景
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC1a1a1a);
        // 面板边框
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF555555);
        g.fill(panelX, panelY + panelH - 2, panelX + panelW, panelY + panelH, 0xFF333333);
        g.fill(panelX, panelY, panelX + 2, panelY + panelH, 0xFF555555);
        g.fill(panelX + panelW - 2, panelY, panelX + panelW, panelY + panelH, 0xFF333333);

        // 生存模式警告条（面板内顶部）
        if (isSurvivalMode()) {
            String warnText = "生存模式：仅可查看颜色，合成请使用方块染色台";
            int warnWidth = Math.min(font.width(warnText) + 24, panelW - 40);
            int warnX = panelX + (panelW - warnWidth) / 2;
            int warnY = panelY + 10;
            g.fill(warnX, warnY, warnX + warnWidth, warnY + 20, 0x99CC0000);
            g.fill(warnX + 1, warnY + 1, warnX + warnWidth - 1, warnY + 19, 0xFFFF4444);
            g.drawString(font, Component.literal(warnText), warnX + (warnWidth - font.width(warnText)) / 2, warnY + 6, 0xFFFFFF);
        }

        // 右侧说明面板
        int infoPanelX = panelX + (int) (panelW * 0.50);
        int infoPanelW = (int) (panelW * 0.45);
        int infoPanelY = panelY + 20;
        int infoPanelH = panelH - 40;

        // 说明面板背景
        g.fill(infoPanelX, infoPanelY, infoPanelX + infoPanelW, infoPanelY + infoPanelH, 0xEE2a2a2a);
        // 说明面板边框
        g.fill(infoPanelX, infoPanelY, infoPanelX + infoPanelW, infoPanelY + 1, 0xFF666666);
        g.fill(infoPanelX, infoPanelY + infoPanelH - 1, infoPanelX + infoPanelW, infoPanelY + infoPanelH, 0xFF444444);
        g.fill(infoPanelX, infoPanelY, infoPanelX + 1, infoPanelY + infoPanelH, 0xFF666666);
        g.fill(infoPanelX + infoPanelW - 1, infoPanelY, infoPanelX + infoPanelW, infoPanelY + infoPanelH, 0xFF444444);

        g.drawString(font, Component.literal("mod 使用说明"), infoPanelX + 12, infoPanelY + 10, 0xFFFFAA);'''

content = content.replace(old_render_start, new_render_start)

# ========== 3. 修改说明文字的渲染位置和行高 ==========
old_lines_render = '''        int y = infoY + 15;
        int lineH = Math.max(10, (infoH - 20) / lines.length);
        for (String line : lines) {
            if (y + 10 < infoY + infoH) {
                g.drawString(font, Component.literal(line), infoAreaX, y, 0xCCCCCC);
            }
            y += lineH;
        }

        // 底部提示
        String bottomText = "彩色方块扩展 v1.2.0";
        g.drawString(font, Component.literal(bottomText),
                (width - font.width(bottomText)) / 2, height - 35, 0x888888);'''

new_lines_render = '''        int y = infoPanelY + 28;
        int lineH = 12;
        for (String line : lines) {
            if (y + 10 < infoPanelY + infoPanelH - 5) {
                g.drawString(font, Component.literal(line), infoPanelX + 12, y, 0xCCCCCC);
            }
            y += lineH;
        }

        // 版本号（面板下方）
        String bottomText = "彩色方块扩展 v1.2.0";
        g.drawString(font, Component.literal(bottomText),
                (width - font.width(bottomText)) / 2, panelY + panelH + 15, 0x888888);'''

content = content.replace(old_lines_render, new_lines_render)

with open(file, 'w', encoding='utf-8') as f:
    f.write(content)

print("Forge版本G键UI已按新布局重新设计")
print("  - 大面板半透明背景，覆盖大部分屏幕")
print("  - 标题和模式在面板上方居中")
print("  - 左侧大按钮区（45%宽度），右侧说明面板（45%宽度）")
print("  - 说明面板有独立背景和边框")
print("  - 版本号在面板下方居中")
