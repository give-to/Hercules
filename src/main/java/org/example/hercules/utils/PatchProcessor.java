package org.example.hercules.utils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PatchProcessor {
    public static String getCode(String pat) {
        return pat.substring(pat.indexOf('$') + 1);
    }

    public static String preprocess(String pat) {
        if (pat.startsWith("insert-before$")) {
            return pat.replaceFirst("insert-before\\$", "insert-before:0\\$");
        } else if (pat.startsWith("insert-after$")) {
            return pat.replaceFirst("insert-after\\$", "insert-after:0\\$");
        } else if (pat.startsWith("replace$")) {
            return pat.replaceFirst("replace\\$", "replace:0,1\\$");
        } else if (pat.startsWith("wrap$")) {
            return pat.replaceFirst("wrap\\$", "wrap:0,1\\$");
        } else if (pat.equals("delete")) {
            return "delete:0,1";
        } else {
            String[] test_tks = pat.split(":");
            test_tks[1] = pat.substring(pat.indexOf(':') + 1);
            if (!test_tks[0].equals("delete") && !test_tks[0].equals("replace") && !test_tks[0].equals("wrap")) {
                return pat;
            } else if (test_tks[0].equals("delete") && !test_tks[1].contains(",")) {
                return test_tks[0] + ":0," + test_tks[1];
            } else if (!test_tks[1].substring(0, test_tks[1].contains("$")?test_tks[1].indexOf('$'):test_tks[1].length()).contains(",")) {
                return test_tks[0] + ":0," + test_tks[1];
            } else {
                return pat;
            }
        }
    }

    public static String[] tokenize(String pat) {
        pat = preprocess(pat);
        String oper = pat.split(":")[0];
        if (!oper.equals("delete") && !oper.equals("move-before") && !oper.equals("move-after")) {
            String code = getCode(pat);
            String[] posStr = pat.substring(pat.indexOf(':') + 1, pat.indexOf('$')).split(",");
            int[] pos = new int[posStr.length];
            for (int i = 0; i < posStr.length; i++) {
                pos[i] = Integer.parseInt(posStr[i]);
            }
            return new String[]{oper, joinIntArray(pos, ","), code};
        } else {
            String[] posStr = pat.split(":")[1].split(",");
            int[] pos = new int[posStr.length];
            for (int i = 0; i < posStr.length; i++) {
                pos[i] = Integer.parseInt(posStr[i]);
            }
            return new String[]{oper, joinIntArray(pos, ",")};
        }
    }
    public static String joinIntArray(int[] arr, String separator) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < arr.length; i++) {
            result.append(arr[i]);

            // Append the separator after each element except the last one
            if (i < arr.length - 1) {
                result.append(separator);
            }
        }

        return result.toString();
    }

    public static int getTargetLine(int line, String pat) {
        return line + Integer.parseInt(tokenize(pat)[1].split(",")[0]);
    }

    public static List<String> toSrcList(List<Object> src) {
        List<String> res = new ArrayList<>();
        for (Object code : src) {
            if (code instanceof String) {
                res.add((String) code);
            } else {
                res.add(((String[]) code)[0] + ((String[]) code)[1] + ((String[]) code)[2]);
            }
        }
        return res;
    }

    public static String toSrc(List<Object> src) {
        StringBuilder res = new StringBuilder();
        for (Object code : src) {
            if (code instanceof String) {
                res.append((String) code);
            } else {
                res.append(((String[]) code)[0]).append(((String[]) code)[1]).append(((String[]) code)[2]);
            }
        }
        return res.toString();
    }

    public static String toFixedLineSrc(List<Object> src) {
        int len0 = src.size();
        List<String> srclist = toSrcList(src);
        srclist.replaceAll(s -> s.replace("\n", ""));
        srclist.replaceAll(s -> s + "\n");
        assert srclist.size() == len0;
        StringBuilder result = new StringBuilder();
        for (String s : srclist) {
            result.append(s);
        }
        return result.toString();
    }

    public static List<Object> convertStringListToObjectList(List<String> stringList) {
        List<Object> objectList = new ArrayList<>();

        for (String str : stringList) {
            objectList.add((Object) str); // Add each string as an Object
        }

        return objectList;
    }

    public static List<Object> ins_bf(List<Object> src, int line, int bias, String code) {
        int cp = line - 1 + bias;
        if (src.get(cp) instanceof String[]) {
            String[] srcCode = (String[]) src.get(cp);
            srcCode[0] = srcCode[0] + " " + code;
        } else {
            src.set(cp, new String[]{code, (String) src.get(cp), ""});
        }
        return src;
    }

    public static List<Object> ins_af(List<Object> src, int line, int bias, String code) {
        int cp = line - 1 + bias;
        if (src.get(cp) instanceof String[]) {
            String[] srcCode = (String[]) src.get(cp);
            srcCode[2] = code + " " + srcCode[2];
        } else {
            src.set(cp, new String[]{"", (String) src.get(cp), code});
        }
        return src;
    }

    public static List<Object> ins_ba(List<Object> src, int line, int[] bias, String code1, String code2) {
        src = ins_af(ins_bf(src, line, bias[0], code1), line, bias[1], code2);
        return src;
    }

    public static List<Object> del_(List<Object> src, int line, int[] bias) {
        int i = line - 1 + bias[0];
        while (i != line + bias[0] + bias[1] - 1) {
            if (src.get(i) instanceof String[]) {
                String[] srcCode = (String[]) src.get(i);
                srcCode[1] = "";
            } else {
                src.set(i, new String[]{"", "", ""});
            }
            i++;
        }
        return src;
    }

    public static List<Object> rep(List<Object> src, int line, int[] bias, String code) {
        int i = line - 1 + bias[0];
        if (src.get(i) instanceof String[]) {
            String[] srcCode = (String[]) src.get(i);
            srcCode[1] = code;
        } else {
            src.set(i, new String[]{"", code, ""});
        }
        i++;
        while (i != line + bias[0] + bias[1] - 1) {
            if (src.get(i) instanceof String[]) {
                String[] srcCode = (String[]) src.get(i);
                srcCode[1] = "";
            } else {
                src.set(i, new String[]{"", "", ""});
            }
            i++;
        }
        return src;
    }

    public static List<Object> wrap(List<Object> src, int line, int[] bias, String code) {
        src = ins_bf(src, line, bias[0], code);
        src = ins_af(src, line, bias[0] + bias[1] - 1, "}");
        return src;
    }

    public static List<Object> mv_b(List<Object> src, int line, int[] bias) {
        StringBuilder code = new StringBuilder();
        for (int i = line - 1 + bias[0]; i < line - 1 + bias[0] + bias[1]; i++) {
            if (src.get(i) instanceof String[]) {
		    String strArray[] = (String[])(src.get(i));
		    code.append(strArray[0]).append(strArray[1]).append(strArray[2]); 
            } else {
		    code.append(src.get(i));
            }
            src.set(i, "");
        }
        src = ins_bf(src, line, bias[2], code.toString());
        return src;
    }

    public static List<Object> mv_a(List<Object> src, int line, int[] bias) {
        StringBuilder code = new StringBuilder();
        for (int i = line - 1 + bias[0]; i < line - 1 + bias[0] + bias[1]; i++) {
            if (src.get(i) instanceof String[]) {
		    String strArray[] = (String[])(src.get(i));
		    code.append(strArray[0]).append(strArray[1]).append(strArray[2]); 
            } else {
		    code.append(src.get(i));
            }
            src.set(i, "");
        }
        src = ins_af(src, line, bias[2], code.toString());
        return src;
    }

    public static List<Object> applyPatch(List<Object> src, String pat, int line) {
        String[] tp = tokenize(pat);
        String op = tp[0];
	String[] posStr = tp[1].split(",");
        int[] pos = new int[posStr.length];
        for (int i = 0; i < posStr.length; i++) {
            pos[i] = Integer.parseInt(posStr[i]);
        }
        if (op.equals("insert-before")) {
            return ins_bf(src, line, pos[0], tp[2]);
        } else if (op.equals("insert-after")) {
            return ins_af(src, line, pos[0], tp[2]);
        } else if (op.equals("insert-before-after")) {
            String[] code = tp[2].split("<#>");
            src = ins_ba(src, line, pos, code[0], code[1]);
            return src;
        } else if (op.equals("delete")) {
            return del_(src, line, pos);
        } else if (op.equals("replace")) {
            return rep(src, line, pos, tp[2]);
        } else if (op.equals("wrap")) {
            return wrap(src, line, pos, tp[2]);
        } else if (op.equals("move-before")) {
            return mv_b(src, line, pos);
        } else if (op.equals("move-after")) {
            return mv_a(src, line, pos);
        } else {
            System.out.println(op + ", unchanged");
            return src;
        }
    }


    public static void main(String[] args) throws IOException {
        String srcPath = "src.txt";
        String patPath = "patch.txt";
        int line = 0;
        String outPath = "out";
        String oriPath = "original.txt";
    }
}


