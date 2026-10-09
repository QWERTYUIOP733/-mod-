# 全面对比Forge版本MardPalette.java与共享mard_295.json的颜色
import json
import re

# 读取共享JSON配置
with open(r"D:\文档\GitHub\游戏mod开发\colors\mard_295.json", 'r', encoding='utf-8') as f:
    json_data = json.load(f)

json_colors = {}
for color in json_data['colors']:
    code = color['code']
    hex_value = color['hex'].upper()
    json_colors[code] = hex_value

print(f"共享JSON配置颜色数量: {len(json_colors)}")

# 读取Forge版本MardPalette.java
with open(r"D:\文档\GitHub\游戏mod开发\shared\java\com\mard\pixel\common\MardPalette.java", 'r', encoding='utf-8') as f:
    java_content = f.read()

# 提取颜色定义: new MardColor("A1","#FAF4C8","A")
java_colors = {}
pattern = r'new MardColor\("([A-M]\d+)"\s*,\s*"#([0-9A-Fa-f]{6})"\s*,\s*"([A-M])"\)'
matches = re.findall(pattern, java_content)

for match in matches:
    code = match[0]
    hex_value = '#' + match[1].upper()
    java_colors[code] = hex_value

print(f"Forge Java颜色定义数量: {len(java_colors)}")

# 对比
differences = []
missing_in_java = []
missing_in_json = []

for code, hex_value in json_colors.items():
    if code not in java_colors:
        missing_in_java.append(code)
    elif java_colors[code] != hex_value:
        differences.append({
            'code': code,
            'json': hex_value,
            'java': java_colors[code]
        })

for code in java_colors.keys():
    if code not in json_colors:
        missing_in_json.append(code)

print(f"\n=== 对比结果 ===")
print(f"颜色不一致: {len(differences)} 处")
print(f"Forge缺少: {len(missing_in_java)} 个")
print(f"JSON缺少: {len(missing_in_json)} 个")

if differences:
    print(f"\n=== 颜色不一致详情 ===")
    for diff in differences:
        print(f"  {diff['code']}: JSON={diff['json']}, Forge={diff['java']}")

if missing_in_java:
    print(f"\n=== Forge缺少的颜色 ===")
    print(f"  {', '.join(missing_in_java)}")

if missing_in_json:
    print(f"\n=== JSON缺少的颜色 ===")
    print(f"  {', '.join(missing_in_json)}")

if not differences and not missing_in_java and not missing_in_json:
    print(f"\n✅ 两个版本的221个颜色完全一致！")
    
    # 显示前10个颜色作为样本
    print(f"\n=== 前10个颜色样本 ===")
    for i, (code, hex_value) in enumerate(list(json_colors.items())[:10]):
        print(f"  {code}: {hex_value}")
