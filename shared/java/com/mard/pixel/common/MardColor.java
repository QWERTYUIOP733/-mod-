/*
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

package com.mard.pixel.common;

/**
 * 单个 标准色号。
 * @param code   色号，如 "A1" / "ZG8"
 * @param hex    #RRGGBB
 * @param series 所属系列，如 "A" / "ZG"
 * @param rgb    0xRRGGBB
 * @param effect 特殊效果类型，如 "珠光" / "温变" / "果冻透明" / "透明闪粉" / "夜光" / "光变(照射后)"，基础色为 ""
 * @param nameCn 中文颜色名，如 "珠光白"，基础色为 ""
 */
public record MardColor(String code, String hex, String series, int rgb, String effect, String nameCn) {

    /** 便捷构造：仅给 code/hex/series，rgb 自动解析，effect和nameCn为空。 */
    public MardColor(String code, String hex, String series) {
        this(code, hex, series, parseRgb(hex), "", "");
    }

    /** 便捷构造：给 code/hex/series/effect/nameCn，rgb 自动解析。 */
    public MardColor(String code, String hex, String series, String effect, String nameCn) {
        this(code, hex, series, parseRgb(hex), effect, nameCn);
    }

    private static int parseRgb(String h) {
        String s = h.startsWith("#") ? h.substring(1) : h;
        return (int) Long.parseLong(s, 16);
    }

    public int r() { return (rgb >> 16) & 0xFF; }
    public int g() { return (rgb >> 8) & 0xFF; }
    public int b() { return rgb & 0xFF; }

    /** 物品/方块注册名后缀：色号小写，如 mard_a1 */
    public String blockName() { return "mard_" + code.toLowerCase(); }
}
