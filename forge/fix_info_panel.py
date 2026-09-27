# 修复Forge版本说明面板显示问题
file = r"D:\文档\GitHub\游戏mod开发\forge\src\main\java\com\mard\pixel\forge\client\MardColorScreen.java"
with open(file, 'r', encoding='utf-8') as f:
    content = f.read()

# 先修复之前PowerShell错误替换的内容
content = content.replace('int infoPanelY = panelY + 10;`n        int infoPanelH = panelH - 20;', 
                          'int infoPanelY = panelY + 10;\n        int infoPanelH = panelH - 20;')

content = content.replace('int y = infoPanelY + 20;`n        int lineH = 9;`n        for (String line : lines) {`n            if (y + 9 < infoPanelY + infoPanelH - 2) {`n                g.drawString(font, Component.literal(line), infoPanelX + 8, y, 0xCCCCCC);',
                          'int y = infoPanelY + 20;\n        int lineH = 9;\n        for (String line : lines) {\n            if (y + 9 < infoPanelY + infoPanelH - 2) {\n                g.drawString(font, Component.literal(line), infoPanelX + 8, y, 0xCCCCCC);')

# 确保说明面板参数正确
old_info = '''        // 右侧说明面板
        int infoPanelX = panelX + (int) (panelW * 0.50);
        int infoPanelW = (int) (panelW * 0.45);
        int infoPanelY = panelY + 15;
        int infoPanelH = panelH - 30;'''
new_info = '''        // 右侧说明面板
        int infoPanelX = panelX + (int) (panelW * 0.50);
        int infoPanelW = (int) (panelW * 0.45);
        int infoPanelY = panelY + 10;
        int infoPanelH = panelH - 20;'''
content = content.replace(old_info, new_info)

# 确保说明标题位置正确
old_title = 'g.drawString(font, Component.literal("mod 使用说明"), infoPanelX + 10, infoPanelY + 8, 0xFFFFAA);'
new_title = 'g.drawString(font, Component.literal("mod 使用说明"), infoPanelX + 8, infoPanelY + 6, 0xFFFFAA);'
content = content.replace(old_title, new_title)

# 确保说明文字渲染正确
old_lines = '''        int y = infoPanelY + 24;
        int lineH = 11;
        for (String line : lines) {
            if (y + 10 < infoPanelY + infoPanelH - 3) {
                g.drawString(font, Component.literal(line), infoPanelX + 10, y, 0xCCCCCC);
            }
            y += lineH;
        }'''
new_lines = '''        int y = infoPanelY + 20;
        int lineH = 9;
        for (String line : lines) {
            if (y + 9 < infoPanelY + infoPanelH - 2) {
                g.drawString(font, Component.literal(line), infoPanelX + 8, y, 0xCCCCCC);
            }
            y += lineH;
        }'''
content = content.replace(old_lines, new_lines)

with open(file, 'w', encoding='utf-8') as f:
    f.write(content)

print("Forge版本说明面板已修复")
# 验证
with open(file, 'r', encoding='utf-8') as f:
    c = f.read()
    if 'infoPanelY = panelY + 10' in c:
        print("  ✓ infoPanelY正确")
    if 'lineH = 9' in c:
        print("  ✓ lineH正确")
    if '`n' not in c:
        print("  ✓ 无错误的转义字符")
    else:
        print("  ✗ 仍有错误的转义字符！")
