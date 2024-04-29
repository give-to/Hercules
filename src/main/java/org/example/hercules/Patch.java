package org.example.hercules;

import edu.lu.uni.serval.jdt.tree.Tree;
import tree.LblTree;

import java.util.ArrayList;
import java.util.List;

public class Patch {
    private String projectId;
    private String srcDir;
    private String targetDir;
    private List<Integer> buggyLine;
    private String buggyClass;
    private String susMethod;

    private String buggyFilePath;

    private String patchStr;
    private List<Tree> tree = new ArrayList<>();

    private List<LblTree> lblTree = new ArrayList<>();

    private List<List<Integer>> contextLines = new ArrayList<>();

    public Patch(String allInfo){
        String[] eles = allInfo.split(":",3);
        this.buggyFilePath = eles[0];
        List<Integer> patch = new ArrayList<>();
        patch.add(Integer.parseInt(eles[1]));
        this.buggyLine = patch;
        this.patchStr = eles[2];
    }
    public Patch(String projectId, String buggyClass, List<Integer> buggyLine){
        this.projectId = projectId;
        this.buggyClass = buggyClass;
        this.buggyLine = buggyLine;
    }

    public Patch(String projectId, String buggyClass, List<Integer> buggyLine, String susMethod){
        this.projectId = projectId;
        this.buggyClass = buggyClass;
        this.buggyLine = buggyLine;
        this.susMethod = susMethod;
    }

    public String getPatchStr() {
        return patchStr;
    }

    public void setPatchStr(String patchStr) {
        this.patchStr = patchStr;
    }

    public String getBuggyFilePath() {
        return buggyFilePath;
    }

    public void setBuggyFilePath(String buggyFilePath) {
        this.buggyFilePath = buggyFilePath;
    }

    public String getSusMethod() {
        return susMethod;
    }

    public void setSusMethod(String susMethod) {
        this.susMethod = susMethod;
    }

    public String getBuggyClass() {
        return buggyClass;
    }

    public void setBuggyClass(String buggyClass) {
        this.buggyClass = buggyClass;
    }

    public List<Tree> getTree() {
        return tree;
    }

    public void addTree(Tree tree) {
        this.tree.add(tree);
    }

    public List<LblTree> getLblTree() {
        return lblTree;
    }

    public void addLblTree(LblTree lblTree) {
        this.lblTree.add(lblTree);
    }

    public List<List<Integer>> getContextLines() {
        return contextLines;
    }

    public void addContextLines(List<Integer> contextLines) {
        this.contextLines.add(contextLines);
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getSrcDir() {
        return srcDir;
    }

    public void setSrcDir(String srcDir) {
        this.srcDir = srcDir;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public List<Integer> getBuggyLine() {
        return buggyLine;
    }

    public void setBuggyLine(List<Integer> buggyLine) {
        this.buggyLine = buggyLine;
    }

    @Override
    public String toString() {
        return "Patch{" +
                "\nbuggyLine=" + buggyLine +
                "\ncontextLines=" + contextLines +
                "\nprojectId='" + projectId + '\'' +
                "\nlblTree=" + lblTree +
                '}';
    }
}
