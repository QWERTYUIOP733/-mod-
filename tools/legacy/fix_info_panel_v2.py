# 优化两个版本的说明面板，确保按钮二等全部内容可见
import os

def fix_fabric():
    file = r"D:\文档\GitHub\游戏mod开发\fabric\src\main\java\com\mard\pixel\fabric\ColorPaletteScreen.java"
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 减小行高到8，减小起始位置
    old_lines = '''        int y = infoPanelY + 20;
        int lineH = 9;
        for (String line : lines) {
            if (y + 9 < infoPanelY + infoPanelH - 2) {
                g.drawString(font, line, infoPanelX + 8, y, 0xCCCCCC);
            }
            y += lineH;
        }'''
    
    new_lines = '''        int y = infoPanelY + 18;
        int lineH = 8;
        for (String line : lines) {
            if (y + 8 < infoPanelY + infoPanelH - 2) {
                g.drawString(font, line, infoPanelX + 8, y, 0xCCCCCC);
            }
            y += lineH;
        }'''
    
    content = content.replace(old_lines, new_lines)
    
    # 减小说明标题和内容的间距
    old_title = 'g.drawString(font, "mod 使用说明", infoPanelX + 8, infoPanelY + 6, 0xFFFFAA);'
    new_title = 'g.drawString(font, "mod 使用说明", infoPanelX + 8, infoPanelY + 5, 0xFFFFAA);'
    content = content.replace(old_title, new_title)
    
    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fabric版本说明面板已优化（行高8）")

def fix_forge():
    file = r"D:\文档\GitHub\游戏mod开发\forge\src\main\java\com\mard\pixel\forge\client\MardColorScreen.java"
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 减小行高到8，减小起始位置
    old_lines = '''        int y = infoPanelY + 20;
        int lineH = 9;
        for (String line : lines) {
            if (y + 9 < infoPanelY + infoPanelH - 2) {
                g.drawString(font, Component.literal(line), infoPanelX + 8, y, 0xCCCCCC);
            }
            y += lineH;
        }'''
    
    new_lines = '''        int y = infoPanelY + 18;
        int lineH = 8;
        for (String line : lines) {
            if (y + 8 < infoPanelY + infoPanelH - 2) {
                g.drawString(font, Component.literal(line), infoPanelX + 8, y, 0xCCCCCC);
            }
            y += lineH;
        }'''
    
    content = content.replace(old_lines, new_lines)
    
    # 减小说明标题和内容的间距
    old_title = 'g.drawString(font, Component.literal("mod 使用说明"), infoPanelX + 8, infoPanelY + 6, 0xFFFFAA);'
    new_title = 'g.drawString(font, Component.literal("mod 使用说明"), infoPanelX + 8, infoPanelY + 5, 0xFFFFAA);'
    content = content.replace(old_title, new_title)
    
    # 保存为无BOM的UTF-8
    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Forge版本说明面板已优化（行高8）")

fix_fabric()
fix_forge()
print("\n两个版本都已优化，按钮二等全部内容应该能完整显示")
