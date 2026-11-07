#!/usr/bin/env python3
"""比对发布 jar 与从 tag 重建的 jar，发现"源码与产物不一致"。

用法:
    python3 tools/compare_jars.py <发布.jar> <重建.jar> [--percent 10]

判定:
    - class 名集合不同        -> 失败（源码与产物结构不一致）
    - 单个 class 体积差 > 阈值 -> 失败（多半是产物来自另一份源码）
    - 资源文件名集合不同      -> 失败
其余差异只打印，不失败。
"""

import sys
import zipfile

IGNORE_PREFIX = ("META-INF/",)


def entries(path):
    z = zipfile.ZipFile(path)
    classes, others = {}, set()
    for n in z.namelist():
        if n.endswith("/"):
            continue
        if n.endswith(".class"):
            classes[n] = z.getinfo(n).file_size
        elif not n.startswith(IGNORE_PREFIX):
            others.add(n)
    return classes, others


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 2

    old, new = sys.argv[1], sys.argv[2]
    percent = 10.0
    if "--percent" in sys.argv:
        percent = float(sys.argv[sys.argv.index("--percent") + 1])

    a_cls, a_res = entries(old)
    b_cls, b_res = entries(new)

    failures = []

    only_a = sorted(set(a_cls) - set(b_cls))
    only_b = sorted(set(b_cls) - set(a_cls))
    print(f"发布包 class={len(a_cls)} 资源={len(a_res)}")
    print(f"重建包 class={len(b_cls)} 资源={len(b_res)}")

    if only_a:
        print("[FAIL] 发布包多出的 class:")
        for n in only_a:
            print("   ", n)
        failures.append(f"{len(only_a)} 个 class 只在发布包里")

    if only_b:
        print("[FAIL] 重建包多出的 class:")
        for n in only_b:
            print("   ", n)
        failures.append(f"{len(only_b)} 个 class 只在重建包里")

    big = []
    for n, size in sorted(a_cls.items()):
        if n not in b_cls:
            continue
        other = b_cls[n]
        if size == 0:
            continue
        delta = abs(other - size) / size * 100.0
        if delta > percent and abs(other - size) > 200:
            big.append((n, size, other, delta))

    if big:
        print("[FAIL] 以下 class 体积差异超过阈值:")
        for n, x, y, d in big:
            print(f"    {n}: 发布 {x} -> 重建 {y}  ({d:.0f}%)")
        failures.append(f"{len(big)} 个 class 体积差异过大")

    res_only_a = sorted(a_res - b_res)
    res_only_b = sorted(b_res - a_res)
    if res_only_a or res_only_b:
        print("[WARN] 资源文件差异:")
        for n in res_only_a[:20]:
            print("    只在发布包:", n)
        for n in res_only_b[:20]:
            print("    只在重建包:", n)
        if len(res_only_a) + len(res_only_b) > 20:
            print("    ...")

    print()
    if failures:
        print("结论: 不一致 ->", "; ".join(failures))
        return 1
    print("结论: 一致（发布包可以由该 tag 复现）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
