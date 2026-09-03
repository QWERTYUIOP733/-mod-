package com.mard.pixel.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MARD 拼豆 295 色标准色卡（dehumaker.cn 拼豆色卡对照表 / MARD 官方色号）。
 * 221 标准 A–H/M + 74 特殊 P/Q/R/T/Y/ZG，共 295 色。
 */
public final class MardPalette {

    public static final List<MardColor> COLORS = List.of(new MardColor[]{
        new MardColor("A1","#FAF5CD","A"), new MardColor("A2","#FCFED6","A"), new MardColor("A3","#FCFF92","A"),
        new MardColor("A4","#F7EC5C","A"), new MardColor("A5","#FFE44B","A"), new MardColor("A6","#FDA951","A"),
        new MardColor("A7","#FA8C4F","A"), new MardColor("A8","#F9E045","A"), new MardColor("A9","#F99C5F","A"),
        new MardColor("A10","#F47E36","A"), new MardColor("A11","#FEDB99","A"), new MardColor("A12","#FDA276","A"),
        new MardColor("A13","#FEC667","A"), new MardColor("A14","#F85842","A"), new MardColor("A15","#FBF65E","A"),
        new MardColor("A16","#FEFF97","A"), new MardColor("A17","#FDE173","A"), new MardColor("A18","#FCBF80","A"),
        new MardColor("A19","#FD7E77","A"), new MardColor("A20","#F9D66E","A"), new MardColor("A21","#FAE393","A"),
        new MardColor("A22","#EDF878","A"), new MardColor("A23","#E1C9BD","A"), new MardColor("A24","#F3F6A9","A"),
        new MardColor("A25","#FFD785","A"), new MardColor("A26","#FEC832","A"),
        new MardColor("B1","#DFF139","B"), new MardColor("B2","#64F343","B"), new MardColor("B3","#9FF685","B"),
        new MardColor("B4","#5FDF34","B"), new MardColor("B5","#39E158","B"), new MardColor("B6","#64E0A4","B"),
        new MardColor("B7","#3FAE7C","B"), new MardColor("B8","#1D9E54","B"), new MardColor("B9","#2A5037","B"),
        new MardColor("B10","#9AD1BA","B"), new MardColor("B11","#627032","B"), new MardColor("B12","#1A6E3D","B"),
        new MardColor("B13","#C8E87D","B"), new MardColor("B14","#ACE84C","B"), new MardColor("B15","#305335","B"),
        new MardColor("B16","#C0ED9C","B"), new MardColor("B17","#9EB33E","B"), new MardColor("B18","#E6ED4F","B"),
        new MardColor("B19","#26B78E","B"), new MardColor("B20","#CAEDCF","B"), new MardColor("B21","#176268","B"),
        new MardColor("B22","#0A4241","B"), new MardColor("B23","#343B1A","B"), new MardColor("B24","#E8FAA6","B"),
        new MardColor("B25","#4E846D","B"), new MardColor("B26","#907C35","B"), new MardColor("B27","#D0E0AF","B"),
        new MardColor("B28","#9EE5BB","B"), new MardColor("B29","#C6DF5F","B"), new MardColor("B30","#E3FBB1","B"),
        new MardColor("B31","#B2E694","B"), new MardColor("B32","#92AD60","B"),
        new MardColor("C1","#FFFEE4","C"), new MardColor("C2","#ABF8FE","C"), new MardColor("C3","#9EE0F8","C"),
        new MardColor("C4","#44CDFB","C"), new MardColor("C5","#06ABE3","C"), new MardColor("C6","#54A7E9","C"),
        new MardColor("C7","#3977CC","C"), new MardColor("C8","#0F52BD","C"), new MardColor("C9","#3349C3","C"),
        new MardColor("C10","#3DBBE3","C"), new MardColor("C11","#2ADED3","C"), new MardColor("C12","#1E334E","C"),
        new MardColor("C13","#CDE7FE","C"), new MardColor("C14","#D6FDFC","C"), new MardColor("C15","#21C5C4","C"),
        new MardColor("C16","#1858A2","C"), new MardColor("C17","#02D1F3","C"), new MardColor("C18","#213244","C"),
        new MardColor("C19","#188690","C"), new MardColor("C20","#1A70A9","C"), new MardColor("C21","#BEDDFC","C"),
        new MardColor("C22","#6BB1BB","C"), new MardColor("C23","#C8E2F9","C"), new MardColor("C24","#7EC5F9","C"),
        new MardColor("C25","#A9E8E0","C"), new MardColor("C26","#42ADD1","C"), new MardColor("C27","#D0DEEF","C"),
        new MardColor("C28","#BDCEED","C"), new MardColor("C29","#364A89","C"),
        new MardColor("D1","#ACB7EF","D"), new MardColor("D2","#868DD3","D"), new MardColor("D3","#3653AF","D"),
        new MardColor("D4","#162C7E","D"), new MardColor("D5","#B34EC6","D"), new MardColor("D6","#B37BDC","D"),
        new MardColor("D7","#8758A9","D"), new MardColor("D8","#E3D2FE","D"), new MardColor("D9","#D6BAF5","D"),
        new MardColor("D10","#301A49","D"), new MardColor("D11","#BCBAE2","D"), new MardColor("D12","#DC99CE","D"),
        new MardColor("D13","#B5038F","D"), new MardColor("D14","#882893","D"), new MardColor("D15","#2F1E8E","D"),
        new MardColor("D16","#E2E4F0","D"), new MardColor("D17","#C7D3F9","D"), new MardColor("D18","#9A64B8","D"),
        new MardColor("D19","#D8C2D9","D"), new MardColor("D20","#9C34AD","D"), new MardColor("D21","#940595","D"),
        new MardColor("D22","#383995","D"), new MardColor("D23","#FADBF8","D"), new MardColor("D24","#768AE1","D"),
        new MardColor("D25","#4950C2","D"), new MardColor("D26","#D6C6EB","D"),
        new MardColor("E1","#F6D4CB","E"), new MardColor("E2","#FCC1DD","E"), new MardColor("E3","#F6BDE8","E"),
        new MardColor("E4","#E9639E","E"), new MardColor("E5","#F1559F","E"), new MardColor("E6","#EC4072","E"),
        new MardColor("E7","#C63674","E"), new MardColor("E8","#FDDBE9","E"), new MardColor("E9","#E575C7","E"),
        new MardColor("E10","#D33997","E"), new MardColor("E11","#F7DAD4","E"), new MardColor("E12","#F893BF","E"),
        new MardColor("E13","#B5026A","E"), new MardColor("E14","#FAD4BF","E"), new MardColor("E15","#F5C9CA","E"),
        new MardColor("E16","#FBF4EC","E"), new MardColor("E17","#F7E3EC","E"), new MardColor("E18","#FBCBDB","E"),
        new MardColor("E19","#F6BBD1","E"), new MardColor("E20","#D7C6CE","E"), new MardColor("E21","#C09DA4","E"),
        new MardColor("E22","#B58B9F","E"), new MardColor("E23","#937D8A","E"), new MardColor("E24","#DEBEE5","E"),
        new MardColor("F1","#FF9280","F"), new MardColor("F2","#F73D48","F"), new MardColor("F3","#EF4D3E","F"),
        new MardColor("F4","#F92B40","F"), new MardColor("F5","#E30328","F"), new MardColor("F6","#913635","F"),
        new MardColor("F7","#911932","F"), new MardColor("F8","#BB0126","F"), new MardColor("F9","#E0677A","F"),
        new MardColor("F10","#874628","F"), new MardColor("F11","#6F321D","F"), new MardColor("F12","#F8516D","F"),
        new MardColor("F13","#F45C45","F"), new MardColor("F14","#FCADB2","F"), new MardColor("F15","#D50527","F"),
        new MardColor("F16","#F8C0A9","F"), new MardColor("F17","#E89B7D","F"), new MardColor("F18","#D07E4A","F"),
        new MardColor("F19","#BE454A","F"), new MardColor("F20","#C69495","F"), new MardColor("F21","#F2BBC6","F"),
        new MardColor("F22","#F7C3D0","F"), new MardColor("F23","#EC806D","F"), new MardColor("F24","#E09DAF","F"),
        new MardColor("F25","#E84854","F"),
        new MardColor("G1","#FFE4D3","G"), new MardColor("G2","#FCC6AC","G"), new MardColor("G3","#F1C4A5","G"),
        new MardColor("G4","#DCB387","G"), new MardColor("G5","#E7B34E","G"), new MardColor("G6","#F3A014","G"),
        new MardColor("G7","#98503A","G"), new MardColor("G8","#4B2B1C","G"), new MardColor("G9","#E4B685","G"),
        new MardColor("G10","#DA8C42","G"), new MardColor("G11","#DAC898","G"), new MardColor("G12","#FEC993","G"),
        new MardColor("G13","#B2714B","G"), new MardColor("G14","#8B684C","G"), new MardColor("G15","#F6F8E3","G"),
        new MardColor("G16","#F2D8C1","G"), new MardColor("G17","#79544E","G"), new MardColor("G18","#FFE4D6","G"),
        new MardColor("G19","#DD7D41","G"), new MardColor("G20","#A5452F","G"), new MardColor("G21","#B38561","G"),
        new MardColor("H1","#FBFBFB","H"), new MardColor("H2","#FFFFFF","H"), new MardColor("H3","#B4B4B4","H"),
        new MardColor("H4","#878787","H"), new MardColor("H5","#464648","H"), new MardColor("H6","#2C2C2C","H"),
        new MardColor("H7","#010101","H"), new MardColor("H8","#E7D6DC","H"), new MardColor("H9","#EFEDEE","H"),
        new MardColor("H10","#ECEAEB","H"), new MardColor("H11","#CDCDCD","H"), new MardColor("H12","#FDF6EE","H"),
        new MardColor("H13","#F4EFD1","H"), new MardColor("H14","#CED7D4","H"), new MardColor("H15","#98A6A6","H"),
        new MardColor("H16","#1B1213","H"), new MardColor("H17","#F0EEEF","H"), new MardColor("H18","#FCFFF8","H"),
        new MardColor("H19","#F2EEE5","H"), new MardColor("H20","#96A09F","H"), new MardColor("H21","#F8FBE6","H"),
        new MardColor("H22","#CACADA","H"), new MardColor("H23","#9B9C94","H"),
        new MardColor("M1","#BBC6B6","M"), new MardColor("M2","#909994","M"), new MardColor("M3","#697E80","M"),
        new MardColor("M4","#E0D4BC","M"), new MardColor("M5","#D0CBAE","M"), new MardColor("M6","#B0AA86","M"),
        new MardColor("M7","#B0A796","M"), new MardColor("M8","#AE8082","M"), new MardColor("M9","#A88764","M"),
        new MardColor("M10","#C6B2BB","M"), new MardColor("M11","#9D7693","M"), new MardColor("M12","#644B51","M"),
        new MardColor("M13","#C79266","M"), new MardColor("M14","#C37463","M"), new MardColor("M15","#747D7A","M"),
        new MardColor("P1","#F9F9F9","P"), new MardColor("P2","#ABABAB","P"), new MardColor("P3","#B6DBAF","P"),
        new MardColor("P4","#FEA2A3","P"), new MardColor("P5","#EB903F","P"), new MardColor("P6","#63CEA2","P"),
        new MardColor("P7","#E79273","P"), new MardColor("P8","#ECDB59","P"), new MardColor("P9","#DBD9DA","P"),
        new MardColor("P10","#DBC7EA","P"), new MardColor("P11","#F1E9D4","P"), new MardColor("P12","#E9EDEE","P"),
        new MardColor("P13","#ADCBF1","P"), new MardColor("P14","#337BAD","P"), new MardColor("P15","#668575","P"),
        new MardColor("P16","#FDC24E","P"), new MardColor("P17","#FDA42E","P"), new MardColor("P18","#FEBDA7","P"),
        new MardColor("P19","#FFDEE9","P"), new MardColor("P20","#FCBFD1","P"), new MardColor("P21","#E8BEC2","P"),
        new MardColor("P22","#DFAAA4","P"), new MardColor("P23","#A3656A","P"),
        new MardColor("Q1","#F2A5E8","Q"), new MardColor("Q2","#E9EC91","Q"), new MardColor("Q3","#FFFF00","Q"),
        new MardColor("Q4","#FFEBFA","Q"), new MardColor("Q5","#76CEDE","Q"),
        new MardColor("R1","#D40E1F","R"), new MardColor("R2","#F13484","R"), new MardColor("R3","#FB852B","R"),
        new MardColor("R4","#F8ED33","R"), new MardColor("R5","#32C958","R"), new MardColor("R6","#1EBA93","R"),
        new MardColor("R7","#1D779C","R"), new MardColor("R8","#1960C8","R"), new MardColor("R9","#945AB1","R"),
        new MardColor("R10","#F8DA54","R"), new MardColor("R11","#FCECF7","R"), new MardColor("R12","#D8D4D3","R"),
        new MardColor("R13","#56534E","R"), new MardColor("R14","#A3E7DC","R"), new MardColor("R15","#78CEE7","R"),
        new MardColor("R16","#3FCDCE","R"), new MardColor("R17","#4E8379","R"), new MardColor("R18","#7DCA9C","R"),
        new MardColor("R19","#C8E664","R"), new MardColor("R20","#E3CCBA","R"), new MardColor("R21","#A17140","R"),
        new MardColor("R22","#6B372C","R"), new MardColor("R23","#F6BB6F","R"), new MardColor("R24","#F3C6C0","R"),
        new MardColor("R25","#C76A62","R"), new MardColor("R26","#D093BC","R"), new MardColor("R27","#E58EAE","R"),
        new MardColor("R28","#9F85CF","R"),
        new MardColor("T1","#FCFDFF","T"),
        new MardColor("Y1","#FF6FB7","Y"), new MardColor("Y2","#FDB583","Y"), new MardColor("Y3","#D8FCA4","Y"),
        new MardColor("Y4","#91DAFB","Y"), new MardColor("Y5","#E987EA","Y"), new MardColor("Y6","#F7D4B8","Y"),
        new MardColor("Y7","#F1FA7D","Y"), new MardColor("Y8","#5EE88C","Y"), new MardColor("Y9","#F8F5FE","Y"),
        new MardColor("ZG1","#DAABB3","ZG"), new MardColor("ZG2","#D6AA87","ZG"), new MardColor("ZG3","#C1BD8D","ZG"),
        new MardColor("ZG4","#96B69F","ZG"), new MardColor("ZG5","#849DC6","ZG"), new MardColor("ZG6","#94BFE2","ZG"),
        new MardColor("ZG7","#E2A9D2","ZG"), new MardColor("ZG8","#AB91C0","ZG")
    });

    public static final Map<String, MardColor> BY_CODE;
    public static final List<String> SERIES_ORDER = List.of(
        "A","B","C","D","E","F","G","H","M","P","Q","R","T","Y","ZG");

    static {
        Map<String, MardColor> m = new HashMap<>();
        for (MardColor c : COLORS) m.put(c.code().toUpperCase(), c);
        BY_CODE = Collections.unmodifiableMap(m);
    }

    public static MardColor byCode(String code) {
        return code == null ? null : BY_CODE.get(code.trim().toUpperCase());
    }

    public static String blockName(String code) {
        MardColor c = byCode(code);
        return c == null ? null : c.blockName();
    }

    public static MardColor nearest(int rgb) {
        MardColor best = null;
        double bestD = Double.MAX_VALUE;
        for (MardColor c : COLORS) {
            double d = ColorMath.deltaE2000(rgb, c.rgb());
            if (d < bestD) { bestD = d; best = c; }
        }
        return best;
    }

    public static List<String> seriesList() {
        List<String> out = new ArrayList<>();
        for (String s : SERIES_ORDER) if (hasSeries(s)) out.add(s);
        return out;
    }

    public static boolean hasSeries(String s) {
        for (MardColor c : COLORS) if (c.series().equals(s)) return true;
        return false;
    }

    private MardPalette() {}
}
