package org.example.hercules.utils;


import java.io.IOException;

public class D4jUtil {
    private ShellUtil shellUtil = new ShellUtil();
    public String export(String prop,String projectDir) throws IOException {
        String shellCmd = "defects4j export -p " + prop ;
        String rslt = shellUtil.runShell(shellCmd, projectDir);
        return rslt;
    }

    public String checkoutAndCompile(String pid, String bid, String projectRoot) throws IOException {
        String dstDir = pid + "_" + bid;
        String shellCmd = "defects4j checkout -p " + pid + " -v " + bid + "b -w " + projectRoot + "/" + dstDir;
        String rslt = shellUtil.runShell(shellCmd, projectRoot);
        shellUtil.runShell("defects4j compile", projectRoot + "/" + dstDir);
        return rslt;
    }
}
