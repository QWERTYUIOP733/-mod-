package com.mard.pixel.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MARD 拼豆标准色卡（MARD官方基础色卡 2026修订版）。
 * 221 标准色 A–H/M。
 */
public final class MardPalette {

    public static final List<MardColor> COLORS = List.of(new MardColor[]{
        new MardColor("A1","#FAF4C8","A"), new MardColor("A2","#FFFFD5","A"), new MardColor("A3","#FEFF8B","A"),
        new MardColor("A4","#FBED56","A"), new MardColor("A5","#F4D738","A"), new MardColor("A6","#FEAC4C","A"),
        new MardColor("A7","#FE8B4C","A"), new MardColor("A8","#FFDA45","A"), new MardColor("A9","#FF995B","A"),
        new MardColor("A10","#F77C31","A"), new MardColor("A11","#FFDD99","A"), new MardColor("A12","#FE9F72","A"),
        new MardColor("A13","#FFC365","A"), new MardColor("A14","#FD543D","A"), new MardColor("A15","#FFF365","A"),
        new MardColor("A16","#FFFF9F","A"), new MardColor("A17","#FFE36E","A"), new MardColor("A18","#FEBE7D","A"),
        new MardColor("A19","#FD7C72","A"), new MardColor("A20","#FFD568","A"), new MardColor("A21","#FFE395","A"),
        new MardColor("A22","#F4F57D","A"), new MardColor("A23","#E6C9B7","A"), new MardColor("A24","#F7F8A2","A"),
        new MardColor("A25","#FFD67D","A"), new MardColor("A26","#FFC830","A"),
        new MardColor("B1","#E6EE31","B"), new MardColor("B2","#63F347","B"), new MardColor("B3","#9EF780","B"),
        new MardColor("B4","#5DE035","B"), new MardColor("B5","#35E352","B"), new MardColor("B6","#65E2A6","B"),
        new MardColor("B7","#3DAF80","B"), new MardColor("B8","#1C9C4F","B"), new MardColor("B9","#27523A","B"),
        new MardColor("B10","#95D3C2","B"), new MardColor("B11","#5D722A","B"), new MardColor("B12","#166F41","B"),
        new MardColor("B13","#CAEB7B","B"), new MardColor("B14","#ADE946","B"), new MardColor("B15","#2E5132","B"),
        new MardColor("B16","#C5ED9C","B"), new MardColor("B17","#9BB13A","B"), new MardColor("B18","#E6EE49","B"),
        new MardColor("B19","#24B88C","B"), new MardColor("B20","#C2F0CC","B"), new MardColor("B21","#156A6B","B"),
        new MardColor("B22","#0B3C43","B"), new MardColor("B23","#303A21","B"), new MardColor("B24","#EEFCA5","B"),
        new MardColor("B25","#4E846D","B"), new MardColor("B26","#8D7A35","B"), new MardColor("B27","#CCE1AF","B"),
        new MardColor("B28","#9EE5B9","B"), new MardColor("B29","#C5E254","B"), new MardColor("B30","#E2FCB1","B"),
        new MardColor("B31","#B0E792","B"), new MardColor("B32","#9CAB5A","B"),
        new MardColor("C1","#E8FFE7","C"), new MardColor("C2","#A9F9FC","C"), new MardColor("C3","#A0E2FB","C"),
        new MardColor("C4","#41CCFF","C"), new MardColor("C5","#01ACEB","C"), new MardColor("C6","#50AAF0","C"),
        new MardColor("C7","#3677D2","C"), new MardColor("C8","#0F54C0","C"), new MardColor("C9","#324BCA","C"),
        new MardColor("C10","#3EBCE2","C"), new MardColor("C11","#28DDDE","C"), new MardColor("C12","#1C334D","C"),
        new MardColor("C13","#CDE8FF","C"), new MardColor("C14","#D5FDFF","C"), new MardColor("C15","#22C4C6","C"),
        new MardColor("C16","#1557A8","C"), new MardColor("C17","#04D1F6","C"), new MardColor("C18","#1D3344","C"),
        new MardColor("C19","#1887A2","C"), new MardColor("C20","#176DAF","C"), new MardColor("C21","#BEDDFF","C"),
        new MardColor("C22","#67B4BE","C"), new MardColor("C23","#C8E2FF","C"), new MardColor("C24","#7CC4FF","C"),
        new MardColor("C25","#A9E5E5","C"), new MardColor("C26","#3CAED8","C"), new MardColor("C27","#D3DFFA","C"),
        new MardColor("C28","#BBCFED","C"), new MardColor("C29","#34488E","C"),
        new MardColor("D1","#AEB4F2","D"), new MardColor("D2","#858EDD","D"), new MardColor("D3","#2F54AF","D"),
        new MardColor("D4","#182A84","D"), new MardColor("D5","#B843C5","D"), new MardColor("D6","#AC7BDE","D"),
        new MardColor("D7","#8854B3","D"), new MardColor("D8","#E2D3FF","D"), new MardColor("D9","#D5B9F8","D"),
        new MardColor("D10","#361851","D"), new MardColor("D11","#B9BAE1","D"), new MardColor("D12","#DE9AD4","D"),
        new MardColor("D13","#B90095","D"), new MardColor("D14","#8B279B","D"), new MardColor("D15","#2F1F90","D"),
        new MardColor("D16","#E3E1EE","D"), new MardColor("D17","#C4D4F6","D"), new MardColor("D18","#A45EC7","D"),
        new MardColor("D19","#D8C3D7","D"), new MardColor("D20","#9C32B2","D"), new MardColor("D21","#9A009B","D"),
        new MardColor("D22","#333A95","D"), new MardColor("D23","#EBDAFC","D"), new MardColor("D24","#7786E5","D"),
        new MardColor("D25","#494FC7","D"), new MardColor("D26","#DFC2F8","D"),
        new MardColor("E1","#FDD3CC","E"), new MardColor("E2","#FEC0DF","E"), new MardColor("E3","#FFB7E7","E"),
        new MardColor("E4","#E8649E","E"), new MardColor("E5","#F551A2","E"), new MardColor("E6","#F13D74","E"),
        new MardColor("E7","#C63478","E"), new MardColor("E8","#FFDBE9","E"), new MardColor("E9","#E970CC","E"),
        new MardColor("E10","#D33793","E"), new MardColor("E11","#FCDDD2","E"), new MardColor("E12","#F78FC3","E"),
        new MardColor("E13","#B5006D","E"), new MardColor("E14","#FFD1BA","E"), new MardColor("E15","#F8C7C9","E"),
        new MardColor("E16","#FFF3EB","E"), new MardColor("E17","#FFE2EA","E"), new MardColor("E18","#FFC7DB","E"),
        new MardColor("E19","#FEBAD5","E"), new MardColor("E20","#D8C7D1","E"), new MardColor("E21","#BD9DA1","E"),
        new MardColor("E22","#B785A1","E"), new MardColor("E23","#937A8D","E"), new MardColor("E24","#E1BCE8","E"),
        new MardColor("F1","#FD957B","F"), new MardColor("F2","#FC3D46","F"), new MardColor("F3","#F74941","F"),
        new MardColor("F4","#FC283C","F"), new MardColor("F5","#E7002F","F"), new MardColor("F6","#943630","F"),
        new MardColor("F7","#971937","F"), new MardColor("F8","#BC0028","F"), new MardColor("F9","#E2677A","F"),
        new MardColor("F10","#8A4526","F"), new MardColor("F11","#5A2121","F"), new MardColor("F12","#FD4E6A","F"),
        new MardColor("F13","#F35744","F"), new MardColor("F14","#FFA9AD","F"), new MardColor("F15","#D30022","F"),
        new MardColor("F16","#FEC2A6","F"), new MardColor("F17","#E69C79","F"), new MardColor("F18","#D37C46","F"),
        new MardColor("F19","#C1444A","F"), new MardColor("F20","#CD9391","F"), new MardColor("F21","#F7B4C6","F"),
        new MardColor("F22","#FDC0D0","F"), new MardColor("F23","#F67E66","F"), new MardColor("F24","#E698AA","F"),
        new MardColor("F25","#E54B4F","F"),
        new MardColor("G1","#FFE2CE","G"), new MardColor("G2","#FFC4AA","G"), new MardColor("G3","#F4C3A5","G"),
        new MardColor("G4","#E1B383","G"), new MardColor("G5","#EDB045","G"), new MardColor("G6","#E99C17","G"),
        new MardColor("G7","#9D5B3E","G"), new MardColor("G8","#753832","G"), new MardColor("G9","#E6B483","G"),
        new MardColor("G10","#D98C39","G"), new MardColor("G11","#E0C593","G"), new MardColor("G12","#FFC890","G"),
        new MardColor("G13","#B7714A","G"), new MardColor("G14","#8D614C","G"), new MardColor("G15","#FCF9E0","G"),
        new MardColor("G16","#F2D9BA","G"), new MardColor("G17","#78524B","G"), new MardColor("G18","#FFE4CC","G"),
        new MardColor("G19","#E07935","G"), new MardColor("G20","#A94023","G"), new MardColor("G21","#B88558","G"),
        new MardColor("H1","#FDFBFF","H"), new MardColor("H2","#FEFFFF","H"), new MardColor("H3","#B6B1BA","H"),
        new MardColor("H4","#89858C","H"), new MardColor("H5","#48464E","H"), new MardColor("H6","#2F2B2F","H"),
        new MardColor("H7","#000000","H"), new MardColor("H8","#E7D6DB","H"), new MardColor("H9","#EDEDED","H"),
        new MardColor("H10","#EEE9EA","H"), new MardColor("H11","#CECDD5","H"), new MardColor("H12","#FFF5ED","H"),
        new MardColor("H13","#F5ECD2","H"), new MardColor("H14","#CFD7D3","H"), new MardColor("H15","#98A6A8","H"),
        new MardColor("H16","#1D1414","H"), new MardColor("H17","#F1EDED","H"), new MardColor("H18","#FFFDF0","H"),
        new MardColor("H19","#F6EFE2","H"), new MardColor("H20","#949FA3","H"), new MardColor("H21","#FFFBE1","H"),
        new MardColor("H22","#CACAD4","H"), new MardColor("H23","#9A9D94","H"),
        new MardColor("M1","#BCC6B8","M"), new MardColor("M2","#8AA386","M"), new MardColor("M3","#697D80","M"),
        new MardColor("M4","#E3D2BC","M"), new MardColor("M5","#D0CCAA","M"), new MardColor("M6","#B0A782","M"),
        new MardColor("M7","#B4A497","M"), new MardColor("M8","#B38281","M"), new MardColor("M9","#A58767","M"),
        new MardColor("M10","#C5B2BC","M"), new MardColor("M11","#9F7594","M"), new MardColor("M12","#644749","M"),
        new MardColor("M13","#D19066","M"), new MardColor("M14","#C77362","M"), new MardColor("M15","#757D78","M")
    });

    public static final Map<String, MardColor> BY_CODE;
    public static final List<String> SERIES_ORDER = List.of(
        "A","B","C","D","E","F","G","H","M");

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
