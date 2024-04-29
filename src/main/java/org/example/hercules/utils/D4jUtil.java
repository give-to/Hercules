package org.example.hercules.utils;


import java.io.IOException;

public class D4jUtil {
    private ShellUtil shellUtil = new ShellUtil();
    public String export(String prop,String projectDir) throws IOException {
        String shellCmd = "defects4j export -p " + prop ;
        String rslt = shellUtil.runShell(shellCmd, projectDir);
        return rslt;
    }
}
