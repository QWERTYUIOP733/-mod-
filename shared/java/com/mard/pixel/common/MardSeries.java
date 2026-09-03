package com.mard.pixel.common;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MardSeries {

    public static final Map<String, String> NAMES;
    public static final String[] ORDER = {"A","B","C","D","E","F","G","H","M","P","Q","R","T","Y","ZG"};

    static {
        NAMES = new LinkedHashMap<>();
        NAMES.put("A", "黄/浅橙");
        NAMES.put("B", "绿/黄绿");
        NAMES.put("C", "蓝/浅蓝");
        NAMES.put("D", "紫/蓝紫");
        NAMES.put("E", "红/粉/肤色");
        NAMES.put("F", "橙/红");
        NAMES.put("G", "棕/肤/土");
        NAMES.put("H", "灰/白/黑");
        NAMES.put("M", "莫兰迪/中性");
        NAMES.put("P", "珠光");
        NAMES.put("Q", "温变");
        NAMES.put("R", "果冻");
        NAMES.put("T", "透明");
        NAMES.put("Y", "夜光");
        NAMES.put("ZG", "光变");
    }

    public static String name(String series) {
        String n = NAMES.get(series == null ? "" : series.toUpperCase());
        return n == null ? (series == null ? "" : series) : n;
    }

    private MardSeries() {}
}
