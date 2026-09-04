package com.mard.pixel.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * MARD 扩展特殊效果色块方块。
 * 支持6种特殊效果：珠光、温变、果冻透明、透明闪粉、夜光、光变。
 */
public class MardEffectBlock extends MardBlock {

    /** 效果类型枚举 */
    public enum EffectType {
        PEARL("珠光"),          // 珠光：光泽效果
        THERMOCHROMIC("温变"),  // 温变：温度变化时变色
        JELLY("果冻透明"),       // 果冻：半透明质感
        GLITTER("透明闪粉"),     // 闪粉：半透明带细闪
        GLOW("夜光"),            // 夜光：黑暗中发光
        PHOTOCHROMIC("光变");    // 光变：紫外线照射后显色

        private final String name;
        EffectType(String name) { this.name = name; }
        public String getName() { return name; }

        public static EffectType fromString(String s) {
            if (s == null || s.isEmpty()) return null;
            for (EffectType t : values()) {
                if (t.name.equals(s) || t.name().equalsIgnoreCase(s)) return t;
            }
            // 处理 "光变(照射后)" 这种带括号的
            if (s.contains("光变")) return PHOTOCHROMIC;
            return null;
        }
    }

    private final EffectType effectType;
    private final String effectName;
    private final String nameCn;

    public MardEffectBlock(String code, int rgb, String effect, String nameCn) {
        super(code, rgb);
        this.effectType = EffectType.fromString(effect);
        this.effectName = effect;
        this.nameCn = nameCn;
    }

    public EffectType getEffectType() { return effectType; }
    public String getEffectName() { return effectName; }
    public String getNameCn() { return nameCn; }

    /** 是否为夜光效果 */
    public boolean isGlow() { return effectType == EffectType.GLOW; }

    /** 是否为半透明效果（果冻/闪粉） */
    public boolean isTransparent() {
        return effectType == EffectType.JELLY || effectType == EffectType.GLITTER;
    }

    /** 是否为动态颜色效果（温变/光变） */
    public boolean isDynamicColor() {
        return effectType == EffectType.THERMOCHROMIC || effectType == EffectType.PHOTOCHROMIC;
    }

    /** 是否为珠光效果 */
    public boolean isPearl() { return effectType == EffectType.PEARL; }

    /** 是否为闪粉效果 */
    public boolean isGlitter() { return effectType == EffectType.GLITTER; }

    /**
     * 夜光方块的光照等级（0-15）。
     * 夜光色在黑暗中发出微光。
     */
    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (isGlow()) {
            return 8; // 夜光方块发出8级光（约半亮度）
        }
        return super.getLightEmission(state, level, pos);
    }

    /**
     * 获取方块的地图颜色。
     */
    @Override
    public MapColor getMapColor(BlockState state) {
        return MapColor.NONE;
    }

    /**
     * 半透明方块的渲染提示。
     * 果冻和闪粉方块使用半透明渲染。
     */
    public float getTransparency() {
        if (effectType == EffectType.JELLY) return 0.65f; // 果冻约65%不透明度
        if (effectType == EffectType.GLITTER) return 0.5f; // 闪粉约50%不透明度
        return 1.0f;
    }
}
