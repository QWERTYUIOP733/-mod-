# 为所有Java文件添加MIT版权声明头部
import os

copyright_header = '''/*
 * Copyright (c) 2026 Color Blocks Extension
 * SPDX-License-Identifier: MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

'''

def add_copyright(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 检查是否已有版权声明
    if 'Copyright (c) 2026 Color Blocks Extension' in content:
        print(f"  已有版权声明: {os.path.basename(file_path)}")
        return
    
    # 移除BOM
    if content.startswith('\ufeff'):
        content = content[1:]
    
    # 添加版权声明
    new_content = copyright_header + content
    
    # 保存为无BOM的UTF-8
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print(f"  已添加版权声明: {os.path.basename(file_path)}")

# 处理Forge版本
print("=== Forge版本 ===")
forge_dir = r"D:\文档\GitHub\游戏mod开发\forge\src\main\java"
for root, dirs, files in os.walk(forge_dir):
    for file in files:
        if file.endswith('.java'):
            add_copyright(os.path.join(root, file))

# 处理Fabric版本
print("\n=== Fabric版本 ===")
fabric_dir = r"D:\文档\GitHub\游戏mod开发\fabric\src\main\java"
for root, dirs, files in os.walk(fabric_dir):
    for file in files:
        if file.endswith('.java'):
            add_copyright(os.path.join(root, file))

print("\n所有Java文件版权声明处理完成")
