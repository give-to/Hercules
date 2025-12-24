package org.example.hercules;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.tbar.TBarFixer;
import org.example.hercules.findContext.ReachingDefinitionFinder;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.jdt.tree.Tree;
import org.example.hercules.utils.D4jUtil;
import org.example.hercules.utils.GenerateStringFromTree;
import edu.lu.uni.serval.tbar.utils.SuspiciousCodeParser;
import org.example.hercules.utils.ShellUtil;
import org.example.hercules.utils.SortPatches;
import org.example.hercules.utils.VariableAssignmentAnalyzer;
import tree.LblTree;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import edu.lu.uni.serval.tbar.TBarAPI;

import static java.io.File.separator;

public class Main_NFL {
    private static String logFile = "";
    private static String projectRoot = "";
    private static String catenaD4jHome = "";
    private static String FLResultRoot = "";
    private static String bugsListFile = "";
    private static int MAX_FL_RANK = 200;
    private static int MAX_SIBLINGS_RANK = Integer.MAX_VALUE;
    private static int MAX_GROUP_PATCHES = 300;
    private static double threshold = 0.8;
    private static long parseTimeout = 1800; //second
    private static long generationTimeout = 9000; //second
    public static List<Integer> getContextLines(String targetDir, String susJavaClass, int buggyLine, String susMethod) {
        return ReachingDefinitionFinder.findContext(targetDir, susJavaClass, buggyLine, susMethod);
    }
    public static List<Integer> getContextLines(String srcfilePath, int buggyLine) throws FileNotFoundException {
        return VariableAssignmentAnalyzer.getContextAssignment(srcfilePath, buggyLine);
    }
    public static String readFile(String fileName) {
        String jsonStr = "";
        try {
            File jsonFile = new File(fileName);
            FileReader fileReader = new FileReader(jsonFile);

            Reader reader = new InputStreamReader(new FileInputStream(jsonFile), "utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static List<Patch> readPatches(String projectId) throws IOException {
        List<Patch> rst = new ArrayList<>();
        String[] eles = projectId.split("_");
        String projectName = eles[0];
        String bid = eles[1];
//        String cid = eles[2];
//        String patchPath = catenaD4jHome + "/projects/"+projectName+"/"+bid+"/"+cid+".src.patch";
        String patchPath = "tempJson" + separator + projectName + separator + bid + separator + "1.src.patch";
        String s = readFile(patchPath);
        JSONObject jobj = JSON.parseObject(s);

        JSONArray patches = jobj.getJSONArray("patch");
        for (int i = 0; i < patches.size(); i++) {
            String fileName = (String) patches.getJSONObject(i).get("file_name");
            List<Integer> lineNum = new ArrayList<>();
            if (patches.getJSONObject(i).containsKey("from_line_no")) {
                lineNum.add((Integer) patches.getJSONObject(i).get("from_line_no"));
            } else {
                int lineInsert = (Integer) patches.getJSONObject(i).get("next_line_no");
                lineNum = findAppropriateLine(projectName, bid, projectRoot + separator + projectId + separator + fileName.replace("/", separator), lineInsert);
            }
            rst.add(new Patch(projectId, fileName, lineNum));
        }
        return rst;
    }

    public static int findLastStatementLine(String filePath, int curLine) throws IOException {
        // 转换成List<String>, 要注意java.lang.OutOfMemoryError: Java heap space
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        lines.add(0, null);
        int lastLine = curLine - 1;
        while (lastLine >= 1) {
            String lineStr = lines.get(lastLine).replace(" ", "");
            if (lineStr.startsWith("/*") || lineStr.startsWith("*") || lineStr.startsWith("*/") || lineStr.startsWith("//") || lineStr.equals(""))
                lastLine--;
            else
                break;
        }
        if (lines.get(lastLine).replace("\n", "").endsWith("{"))
            return -1;
        return lastLine;
    }

    public static List<Integer> findAppropriateLine(String projectName, String bid, String filePath, int lineNum) throws IOException {

        // 转换成List<String>, 要注意java.lang.OutOfMemoryError: Java heap space
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        lines.add(0, null);
        int nextLine = lineNum;
        if (lines.get(lineNum).replace(" ", "").equals("")) {
            nextLine = lineNum + 1;
            while (nextLine < lines.size()) {
                String lineStr = lines.get(nextLine).replace(" ", "");
                if (lineStr.startsWith("/*") || lineStr.startsWith("*") || lineStr.startsWith("*/") || lineStr.startsWith("//") || lineStr.equals(""))
                    nextLine++;
                else
                    break;
            }
        }
        int lastLine = lineNum - 1;
        while (lastLine >= 1) {
            String lineStr = lines.get(lastLine).replace(" ", "");
            if (lineStr.startsWith("/*") || lineStr.startsWith("*") || lineStr.startsWith("*/") || lineStr.startsWith("//") || lineStr.equals(""))
                lastLine--;
            else
                break;
        }
        // Read Ochiai File
        String rankingFile = FLResultRoot + separator + projectName + separator + projectName + "_" + bid + separator + "gzoltar" + separator + "ranking";
        String className = filePath.substring(filePath.lastIndexOf(separator) + 1, filePath.length() - 5);
        Scanner sc = new Scanner(new FileReader(rankingFile));
        while (sc.hasNextLine()) {  //按行读取字符串
            String flLine = sc.nextLine();
            if (flLine.contains("$" + className + "$") && flLine.contains(":" + lastLine + ";")) {
                List<Integer> rst = new ArrayList<>();
                rst.add(lastLine);
                return rst;
            }
            if (flLine.contains("$" + className + "$") && flLine.contains(":" + nextLine + ";")) {
                List<Integer> rst = new ArrayList<>();
                rst.add(nextLine);
                return rst;
            }
        }
        List<Integer> rst = new ArrayList<>();
//        rst.add(lastLine);
        rst.add(nextLine);
        return rst;
    }

    public static List<Pair<String, Integer>> parseFLResult(String filePath) {
        List<Pair<String, Integer>> rst = new ArrayList<>();
        String[] ranking = readFile(filePath).split("\n");
        for (int i = 0; i < ranking.length; i++) {
            String line = ranking[i];
            int rindex_jing = line.lastIndexOf("#");
            String susMethod = line.substring(rindex_jing + 1, line.indexOf("("));
            int sedindex_dollar = line.indexOf("$", line.indexOf("$") + 1);
            if (sedindex_dollar > 0 && sedindex_dollar < rindex_jing) {
                rindex_jing = sedindex_dollar;
            }

            String java_class = line.substring(0, rindex_jing);
            java_class = java_class.replace("$", ".");

            String line_num = line.substring(line.lastIndexOf(":") + 1, line.lastIndexOf(";"));
            String score = line.substring(line.lastIndexOf(";") + 1);
            // if (Float.parseFloat(score) > 0) {
                rst.add(new Pair<>(java_class + "@" + susMethod, Integer.parseInt(line_num)));
            // }
        }
        return rst;
    }

    public static boolean applyPatch(Patch patch) throws IOException {
        String buggyFilePath = patch.getBuggyFilePath();
        String currentPatch = patch.getPatchStr();
        currentPatch = currentPatch.replace("$", "\\$");
        currentPatch = currentPatch.replace("\"", "\\\"");
        currentPatch = "\"" + currentPatch + "\"";
        String buggyLine = patch.getBuggyLine().get(0) + "";
        boolean exitCode = applyPatch(buggyFilePath, currentPatch, buggyLine, patch.getProjectId());
        return exitCode;
    }

    public static boolean applyPatch(String buggyFilePath, String currentPatch, String buggyLine, String projectId) throws IOException {
        String applyPatch = "python3 apply_patches.py " + "0" + " " + buggyFilePath + " " + currentPatch + " " + buggyLine;
        String shellFile = projectId + "_apply_patches.sh";
        String applyPatchesShell = "scripts/" + shellFile;
        writeFile(applyPatch, applyPatchesShell);
        ShellUtil shellUtil = new ShellUtil();
        shellUtil.runShell("chmod +x " + shellFile, "scripts");
        boolean exitCode = shellUtil.runShellFile("scripts", shellFile);
        return exitCode;
    }

    public static List<List<Patch>> findSimilarFL(List<Patch> candidatePatch) {
        // find similar FL
        List<List<Patch>> similarGroups = new ArrayList<>();
        Set<Patch> triedPatch = new HashSet<>();
        for (int i = 0; i < Math.min(MAX_FL_RANK, candidatePatch.size()); i++) {
            Patch currentPatch = candidatePatch.get(i);
            if (triedPatch.contains(currentPatch)) continue;

            List<Patch> tmpSimilarGroup = new ArrayList<>();
            triedPatch.add(currentPatch);
            tmpSimilarGroup.add(currentPatch);
            for (int j = i + 1; j < Math.min(candidatePatch.size(), MAX_SIBLINGS_RANK); j++) {
                Patch comparedPatch = candidatePatch.get(j);
                if (triedPatch.contains(comparedPatch)) continue;

                LblTree tree1 = currentPatch.getLblTree().get(currentPatch.getLblTree().size() - 1);
                LblTree tree2 = comparedPatch.getLblTree().get(comparedPatch.getLblTree().size() - 1);
                double result = -1;
                double similarity = 0;
                try {
                    result = new HerculesEditDist(true).nonNormalizedTreeDist(tree1, tree2);
                }catch (OutOfMemoryError e){
                    System.out.println(e);
                }
                if(result > -1){
                    similarity = 1 - result / Math.max(tree1.getNodeCount() - 1, tree2.getNodeCount() - 1);
                }
//                System.out.println(similarity);
//                output += i + " and " + j + ":" + similarity + "=" + result + " / max(" + tree1.getNodeCount() + ", " + tree2.getNodeCount() + ")\n";
                if (similarity >= threshold) {
                    tmpSimilarGroup.add(comparedPatch);
                    triedPatch.add(comparedPatch);
                }
            }
//            if (tmpSimilarGroup.size() > 1)
            similarGroups.add(tmpSimilarGroup);
        }
        return similarGroups;
    }

    public static void backupPatchSrc(Patch patch) throws IOException {
        String buggyFilePath = patch.getBuggyFilePath();
        File bakFile = new File(buggyFilePath + ".bak");
        if (!bakFile.exists()) {
            writeFile(readFile(buggyFilePath), buggyFilePath + ".bak");
        }
//            copyFile(new File(buggyFilePath), new File(buggyFilePath + ".bak"));
    }

    public static void restorePatchSrc(Patch patch) throws IOException {
        String buggyFilePath = patch.getBuggyFilePath();
        writeFile(readFile(buggyFilePath + ".bak"), buggyFilePath);
    }

    public static int getGenPatchLine(String genPatch) {
        if (genPatch.startsWith("delete"))
            return 0;
        if (genPatch.startsWith("insert-before$") || genPatch.startsWith("insert-after$") || genPatch.startsWith("replace$") || genPatch.startsWith("wrap$"))
            return 0;
        if (genPatch.startsWith("replace") && !genPatch.substring(0, genPatch.indexOf("$")).contains(","))
            return 0;
        int sign1 = genPatch.indexOf(":");
        int sign2 = -1;
        if (genPatch.contains("$"))
            sign2 = genPatch.indexOf("$");
        else
            sign2 = genPatch.indexOf(",");
        if (genPatch.substring(sign1 + 1, sign2).contains(",")) {
            sign2 = genPatch.indexOf(",");
        }
        return Integer.parseInt(genPatch.substring(sign1 + 1, sign2));
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ShellUtil shellUtil = new ShellUtil();
        D4jUtil d4jUtil = new D4jUtil();

        projectRoot = args[0];
        catenaD4jHome = args[1];
        FLResultRoot = args[2];
        bugsListFile = args[3];
	logFile = args[4];
        String[] allProjects = readFile(bugsListFile).split(System.getProperty("line.separator"));
        for (int index = 0; index < allProjects.length; index++) {
            String output = "";
            String projectId = allProjects[index];
            SimpleDateFormat sbf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
// patch generation
            String start_time = sbf.format(new Date());
            Long start_second = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
            System.out.println(projectId + "start: " + start_time);
            String projectDir = projectRoot + separator + projectId;
            String srcDir = d4jUtil.export("dir.src.classes", projectDir);
            String targetDir = d4jUtil.export("dir.bin.classes", projectDir);
            String targetPath = projectDir + separator + targetDir;

            appendFile(logFile, "=========================================\n");
            appendFile(logFile, projectId + ":\n");
            appendFile(logFile, "start at: " + sbf.format(new Date()) + "\n");
            // Read Ochiai ranking
            String[] eles = projectId.split("_");
            String projectName = eles[0];
            String bid = eles[1];
            String currentFLPath = FLResultRoot + separator + projectId + separator + "ochiai.ranking.txt";
            List<Pair<String, Integer>> flRanking = parseFLResult(currentFLPath);
            // require all candidatePatch FL
//            appendFile(logFile, "Parse FL start at: " + sbf.format(new Date()) + "\n");
            List<Patch> candidatePatch = new ArrayList<>();
            for (int i = 0; i < Math.min(flRanking.size(), MAX_SIBLINGS_RANK) && LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8")) - start_second < parseTimeout; i++) {
                Pair<String, Integer> oneFL = flRanking.get(i);

                String buggyClassAndMethod = oneFL.getFirst();
                String buggyClass = buggyClassAndMethod.substring(0, buggyClassAndMethod.indexOf("@"));
                String susMethod = buggyClassAndMethod.substring(buggyClassAndMethod.indexOf("@"));
                int buggyLine = oneFL.getSecond();
                List<Integer> susLine = new ArrayList<>();
                susLine.add(buggyLine);
                Patch tmpPatch = new Patch(projectId, buggyClass, susLine, susMethod);
                tmpPatch.setSrcDir(srcDir);
                tmpPatch.setTargetDir(targetDir);
                SuspiciousCodeParser scp = new SuspiciousCodeParser();
                for (int lineIndex = 0; lineIndex < tmpPatch.getBuggyLine().size(); lineIndex++) {
                    String srcfilePath = projectDir + separator + srcDir + separator + tmpPatch.getBuggyClass().replace(".", separator) + ".java";
                    tmpPatch.setBuggyFilePath(srcfilePath);
                    try {
                        scp.parseSuspiciousCode(new File(srcfilePath), tmpPatch.getBuggyLine().get(lineIndex));
                    } catch (Throwable e) {
                        String errorMsg = "Parsing FL has error at: " + tmpPatch.getBuggyFilePath() + "@" + tmpPatch.getBuggyLine().get(0) + "\n";
                        appendFile(logFile, errorMsg);
                        continue;
                    }
//                    if(scp.unit.getProblems().length > 0){
//                        continue;
//                    }
                }
                List<Pair<ITree, String>> suspiciousCodePairs = scp.getSuspiciousCode();
                String tmpPatchNodeTreeStr = "";
                for (int j = 0; j < suspiciousCodePairs.size(); j++) {
                    tmpPatch.addTree((Tree) suspiciousCodePairs.get(j).firstElement);
                    String targetStr = GenerateStringFromTree.generateString((Tree) suspiciousCodePairs.get(j).firstElement);
                    tmpPatchNodeTreeStr += "{" + targetStr + "}";
                }
                tmpPatchNodeTreeStr = "{" + "-1###rootKind###rootName" + tmpPatchNodeTreeStr + "}";
                LblTree tree = HerculesLblTree.fromString(tmpPatchNodeTreeStr);
                tmpPatch.addLblTree(tree);
                candidatePatch.add(tmpPatch);
            }
            appendFile(logFile, "Parse FL end at: " + sbf.format(new Date()) + "\n");
            // find similar FL
            List<List<Patch>> similarGroups = findSimilarFL(candidatePatch);
            appendFile(logFile, "find similar FL end at: " + sbf.format(new Date()) + "\n");
//            appendFile(logFile, "origin group num: " + similarGroups.size() + "\n");

            // find similar reaching definition context
            List<List<Patch>> newSimilarGroups = new ArrayList<>();
            for (int i = 0; i < similarGroups.size(); i++) {
                List<Patch> currentGroup = similarGroups.get(i);

                for (int j = 0; j < currentGroup.size(); j++) {
                    Patch tmpPatch = currentGroup.get(j);
                    String buggyClass = tmpPatch.getBuggyClass();
                    List<Integer> contextLines = new ArrayList<>();
                    try {
//                        contextLines = getContextLines(targetPath, buggyClass, tmpPatch.getBuggyLine().get(0), tmpPatch.getSusMethod());
                        String srcfilePath = projectDir + separator + srcDir + separator + tmpPatch.getBuggyClass().replace(".", separator) + ".java";
                        contextLines = getContextLines(srcfilePath, tmpPatch.getBuggyLine().get(0));
                    } catch (Exception e) {
                        String errStr = projectId + " : " + tmpPatch.getBuggyClass() + "@" + tmpPatch.getBuggyLine().get(0) + " getContext error!";
                        System.out.println(errStr);
                        appendFile(logFile, "There exists error when get contexts of " + buggyClass + "@" + tmpPatch.getBuggyLine().get(0) + "\n");
                        contextLines.addAll(tmpPatch.getBuggyLine());
                    }
                    contextLines.sort(Comparator.naturalOrder());

                    if (contextLines.size() <= 1) {
                        int lastLine = Math.max(0, tmpPatch.getBuggyLine().get(0) - 1);
                        try {
                            lastLine = findLastStatementLine(tmpPatch.getBuggyFilePath(), tmpPatch.getBuggyLine().get(0));
                        } catch (Exception e) {
                            appendFile(logFile, "Find Last Line Number Error! " + e + " " + tmpPatch.getBuggyFilePath() + "@" + tmpPatch.getBuggyLine().get(0) + "\n");
                        }
                        if(lastLine >= 0)
                            contextLines.add(0, lastLine);
                    }

                    tmpPatch.addContextLines(contextLines);

                    if(contextLines.size()<=1){
                        continue;
                    }
                    SuspiciousCodeParser scp = new SuspiciousCodeParser();
                    for (Integer susLine : contextLines) {
                        String srcfilePath = projectDir + separator + srcDir + separator + tmpPatch.getBuggyClass().replace(".", separator) + ".java";
                        try {
                            scp.parseSuspiciousCode(new File(srcfilePath), susLine);
                        } catch (Throwable e) {
                            appendFile(logFile, "Error when parsing Context of " + tmpPatch.getBuggyFilePath() + "@" + tmpPatch.getBuggyLine().get(0) + "=>" + susLine + "\n");
                        }
//                        if(scp.unit.getProblems().length > 0){
//                            continue;
//                        }
                    }
                    List<Pair<ITree, String>> suspiciousCodePairs = scp.getSuspiciousCode();
                    String contextTreeString = "";
                    for (int contextIndex = 0; contextIndex < suspiciousCodePairs.size(); contextIndex++) {
                        tmpPatch.addTree((Tree) suspiciousCodePairs.get(contextIndex).firstElement);
                        String targetStr = GenerateStringFromTree.generateString((Tree) suspiciousCodePairs.get(contextIndex).firstElement);
                        contextTreeString += "{" + targetStr + "}";
                    }
                    contextTreeString = "{" + "-1###rootKind###rootName" + contextTreeString + "}";
                    LblTree tree = HerculesLblTree.fromString(contextTreeString);
                    tmpPatch.addLblTree(tree);
                }
                // find similar context
                List<List<Patch>> everyNewGroups = findSimilarFL(currentGroup);
                for (int j = 0; j < everyNewGroups.size(); j++) {
                    newSimilarGroups.add(everyNewGroups.get(j));
                }
            }
//            System.out.println(newSimilarGroups);
            appendFile(logFile, "find similar FL(with context) end at: " + sbf.format(new Date()) + "\n");
            appendFile(logFile, "new group num: " + newSimilarGroups.size() + "\n");
	    /*
	
	    for (int groupIndex = 0; groupIndex < newSimilarGroups.size(); groupIndex++) {
                List<Patch> currentGroup = newSimilarGroups.get(groupIndex);
                for (int i = 0; i < currentGroup.size(); i++) {
                    Patch memberPatch = currentGroup.get(i);
                    String relativeBuggyFilePath = memberPatch.getSrcDir() + separator + memberPatch.getBuggyClass().replace(".", separator) + ".java";
                    String susFileAndLine = relativeBuggyFilePath + ":" + memberPatch.getBuggyLine().get(0);
                    appendFile("groups" + separator + projectId + separator + "group_" + groupIndex + separator + "0.txt", susFileAndLine + "\n");
                }
            }
	    
	    if(newSimilarGroups.size() >= 0){
	    	continue;
	    }
	    */
	
            for (int groupIndex = 0; groupIndex < newSimilarGroups.size() && LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8")) - start_second < generationTimeout; groupIndex++) {

                List<Patch> currentGroup = newSimilarGroups.get(groupIndex);

                Patch firstPatch = currentGroup.get(0);
                // Generate patchStr of firstPatch
                int buggyline = firstPatch.getBuggyLine().get(0);
                String buggyProjectPath = projectRoot + separator + projectId + separator + firstPatch.getSrcDir();
                String buggyFilePath = firstPatch.getBuggyFilePath();
//                appendFile(logFile, "group_" + groupIndex + " start generating patches at " + sbf.format(new Date()) + "\n");
                String results = "";
                TBarFixer.results = "";
                try {
                    results = TBarAPI.runTBarPerfect("code", buggyline, buggyFilePath, buggyProjectPath);
                }catch(Exception e){
                    System.out.println(e);
                    appendFile(logFile, "group_" + groupIndex + " generating patches error! " + sbf.format(new Date()) + "\n");
                    continue;
                }
//                appendFile(logFile, "group_" + groupIndex + " end generating patches at " + sbf.format(new Date()) + "\n");
                String debugInfo = "The patches generated by TBarAPI of " + firstPatch.getBuggyClass() + "@" + firstPatch.getBuggyLine().get(0) + "as follows:\n";
//                appendFile(logFile, debugInfo);
//                System.out.println(debugInfo);
//                appendFile(logFile, results);
//                appendFile(logFile, "\n<- add a \\n\n");
//                System.out.println(results);

                String[] genPatches = results.split("\n");
//                List<String> patchedSrcList = new ArrayList<>();
                System.out.println("group_" + groupIndex + "'s " + firstPatch.getBuggyFilePath() + "@" + firstPatch.getBuggyLine().get(0) + "is extending\n");
//                appendFile(logFile, "group_" + groupIndex + "'s " + firstPatch.getBuggyFilePath() + "@" + firstPatch.getBuggyLine().get(0) + "is extending\n");
                for (int j = 0; j < Math.min(MAX_GROUP_PATCHES, genPatches.length); j++) {
                    String currentGenPatch = genPatches[j];
                    if (currentGenPatch.isEmpty() || currentGenPatch.replace(" ", "").replace("\n", "").isEmpty())
                        continue;

                    firstPatch.setPatchStr(currentGenPatch);

                    // Back Up Src Files
                    backupPatchSrc(firstPatch);
                    //apply genPatch
                    boolean exitCode = applyPatch(firstPatch);
                    if (!exitCode) {
                        restorePatchSrc(firstPatch);
                        continue;
                    }

                    SuspiciousCodeParser scp = new SuspiciousCodeParser();
//                    appendFile(logFile, firstPatch.getBuggyFilePath() + "@" + firstPatch.getBuggyLine().get(0) + "@" + currentGenPatch + " is extending\n");
                    System.out.println(firstPatch.getBuggyFilePath() + "@" + firstPatch.getBuggyLine().get(0) + "@" + currentGenPatch + " is extending");
//                    System.out.println(getGenPatchLine(currentGenPatch));
//                    appendFile(logFile, projectId + "parsing patched AST start at " + sbf.format(new Date()) + "\n");
                    try {
                        scp.parseSuspiciousCode(new File(buggyFilePath), firstPatch.getBuggyLine().get(0) + getGenPatchLine(currentGenPatch));
                    } catch (Throwable e) {
                        String errorMsg = "There exists error when parsing patched src.\n";
//                        System.out.println(errorMsg);
                        System.out.println("The extending patch is: " + firstPatch.getBuggyFilePath() + "@" + firstPatch.getBuggyLine().get(0) + "@" + currentGenPatch + "\n");
//                        appendFile(logFile, errorMsg);
                        System.out.println(e);
                        restorePatchSrc(firstPatch);
//                        appendFile(logFile, "parsing patched AST end at " + sbf.format(new Date()) + "\n");
                        continue;
                    }
                    if (scp.getSuspiciousCode().size() < 1) {
//                        appendFile(logFile, "get empty patched node\n");
                        restorePatchSrc(firstPatch);
//                        appendFile(logFile, "parsing patched AST end at " + sbf.format(new Date()) + "\n");
                        continue;
                    }
//                    appendFile(logFile, "parsing patched AST end at " + sbf.format(new Date()) + "\n");
                    ITree fixedPatchNode = scp.getSuspiciousCode().get(0).getFirst();
//                    appendFile(logFile, "extending start at " + sbf.format(new Date()) + "\n");
                    for (int option = 0; option < 1; option++) {
                        if (option == 0) {

                            for (int k = 1; k < currentGroup.size(); k++) {
                                Patch waitExtendPatch = currentGroup.get(k);
                                String extendedPatch = "";
                                if (currentGenPatch.startsWith("move") || currentGenPatch.startsWith("delete")) {
                                    extendedPatch = PatchExtend.patchExtend_strategy_1(firstPatch.getTree().get(0), waitExtendPatch.getTree().get(0), fixedPatchNode, currentGenPatch);

                                } else {
                                    try {
                                        extendedPatch = PatchExtend.patchExtend(firstPatch.getTree().get(0), waitExtendPatch.getTree().get(0), fixedPatchNode, currentGenPatch);
                                    } catch (Exception e) {
                                        extendedPatch = PatchExtend.patchExtend_strategy_1(firstPatch.getTree().get(0), waitExtendPatch.getTree().get(0), fixedPatchNode, currentGenPatch);
                                    }

                                }
                                waitExtendPatch.setPatchStr(extendedPatch);
//                                backupPatchSrc(waitExtendPatch);
//                                applyPatch(waitExtendPatch);
                            }
                        } else {
                            //extend patch
                            for (int k = 1; k < currentGroup.size(); k++) {
                                Patch waitExtendPatch = currentGroup.get(k);
                                String extendedPatch = PatchExtend.patchExtend_strategy_1(firstPatch.getTree().get(0), waitExtendPatch.getTree().get(0), fixedPatchNode, currentGenPatch);
                                waitExtendPatch.setPatchStr(extendedPatch);
//                                backupPatchSrc(waitExtendPatch);
//                                applyPatch(waitExtendPatch);
                            }
                        }
                        try {
                            for (int i = 0; i < currentGroup.size(); i++) {
                                Patch memberPatch = currentGroup.get(i);
//                                String bgyFilePath = memberPatch.getBuggyFilePath();
                                String relativeBuggyFilePath = memberPatch.getSrcDir() + separator + memberPatch.getBuggyClass().replace(".", separator) + ".java";
                                String susFileAndLine = relativeBuggyFilePath + ":" + memberPatch.getBuggyLine().get(0);
                                appendFile("patches" + separator + projectId + separator + "group_" + groupIndex + separator + j + ".txt", susFileAndLine + ":" + memberPatch.getPatchStr() + "\n");
                            }
                        } catch (Exception e) {
                            System.out.println("compile failed......");
                        } finally {
//                            appendFile(logFile, "extending end at " + sbf.format(new Date()) + "\n");
                            restorePatchSrc(firstPatch);
                        }
                    }

                }
            }
            String end_time = sbf.format(new Date());
            System.out.println(projectId + "end: " + end_time);
            Long end_second = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
            Long spend_second = end_second - start_second;
            writeFile(spend_second + "s", "patches" + separator + projectId + separator + "time_info_gen.txt");
            appendFile(logFile, "All Generation end at: " + end_time + "\n");

//            sort all patches
            Long sort_start_time = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
            List<PatchFile> allPatchList = new ArrayList<>();
            String patchesRoot = "patches";

            String[] groupList = new File(patchesRoot + separator + projectId).list();
            for (int i = 0; i < groupList.length; i++) {

                File groupDir = new File(patchesRoot + separator + projectId + separator + groupList[i]);
                if (groupDir.isDirectory()) {
                    String[] patchFileList = groupDir.list();
                    for (int j = 0; j < patchFileList.length; j++) {
                        String patchFilePath = groupDir.getPath() + separator + patchFileList[j];
                        PatchFile patch = new PatchFile(groupList[i], patchFileList[j]);
                        String[] patchFileContent = readFile(patchFilePath).split("\\r?\\n");
                        List<Patch> content = new ArrayList<>();
                        for (int k = 0; k < patchFileContent.length; k++) {
                            content.add(new Patch(projectRoot + separator + projectId + separator + patchFileContent[k]));
                        }
                        patch.setPatch(content);
                        allPatchList.add(patch);
                    }
                }
            }
//            appendFile(logFile, projectId + " Sort Patches start at " + sbf.format(new Date()) + "\n");
            try {
                SortPatches.sortPatches(allPatchList);
            } catch (Exception e) {
                System.out.println(e);
                System.out.println("sort error");
            }
            appendFile(logFile, projectId + " Sort Patches end at " + sbf.format(new Date()) + "\n");



            //write sort result
            String rankFile = patchesRoot + separator + projectId + separator + "rank.txt";
            File rank = new File(rankFile);
            if(rank.exists()){
                rank.delete();
            }
            for (int i = 0; i < allPatchList.size(); i++) {
                PatchFile patchFile = allPatchList.get(i);
                appendFile(rankFile, patchFile.getGroupId() + separator + patchFile.getPatchId() + "\n");
            }

            Long sort_end_time = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
            Long generation_time = Long.parseLong(readFile("patches" + separator + projectId + separator + "time_info_gen.txt").replace("s",""));
            Long sort_time = sort_end_time - sort_start_time;
            Long sum_time = generation_time + sort_time;
            writeFile(sum_time+"s","patches" + separator + projectId + separator + "time_info.txt");

            //validation
//            appendFile(logFile, "Validation Patches start at " + sbf.format(new Date()) + "\n");
//            System.out.println(projectId + " Start validation");
//            String validateCmd = "python3 hercules_valid_v2.py " + projectRoot;
//            shellUtil.runShell(validateCmd, ".");
//            System.out.println(projectId + " End Validation");
//            appendFile(logFile, "Validation Patches end at " + sbf.format(new Date()) + "\n");


        }

    }

    public static void appendFile(String filePath, String content) throws IOException {
        File file = new File(filePath);
        File fileParent = file.getParentFile();
        if (fileParent != null && !fileParent.exists()) {
            fileParent.mkdirs();
        }
        //如果文件不存在，创建文件
        if (!file.exists())
            file.createNewFile();
        BufferedWriter out = new BufferedWriter(new FileWriter(filePath, true));
        out.write(content);
        out.close();
        //创建FileWriter对象
//        FileWriter writer = new FileWriter(file);
//向文件中写入内容
//        writer.write(content);
//        writer.flush();
//        writer.close();
    }

    public static void writeFile(String contend, String filePath) throws IOException {
        File file = new File(filePath);
        File fileParent = file.getParentFile();
        if (!fileParent.exists()) {
            fileParent.mkdirs();
        }
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        bw.write(contend);
        bw.flush();
        bw.close();
    }


}
