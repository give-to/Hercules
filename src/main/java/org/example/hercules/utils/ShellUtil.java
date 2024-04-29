package org.example.hercules.utils;

import fj.data.IO;

import java.io.*;
import java.nio.Buffer;
import java.util.concurrent.TimeUnit;

public class ShellUtil {
    public String runShell(String cmd, String projectDir) throws IOException {
        return runShell(cmd.split(" "), projectDir, -1l);
    }

    public String runShell(String cmd, String projectDir, long max_seconds) throws IOException {
        return runShell(cmd.split(" "), projectDir, max_seconds);
    }

    public String runShell(String[] cmd, String projectDir) throws IOException {
        return runShell(cmd, projectDir, -1l);
    }

    public String compileCheck(String cmd, String projectDir) throws IOException {
        return compileCheck(cmd.split(" "), projectDir);
    }
    public String runShell(String cmd) throws IOException {
        Process p;
        StringBuffer result = new StringBuffer();
        try {
            p = Runtime.getRuntime().exec(cmd);
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                System.out.println("run shell error");
                InputStreamReader isr = new InputStreamReader(p.getErrorStream(), "gbk");//读取
                System.out.println(isr.getEncoding());
                BufferedReader bufr = new BufferedReader(isr);//缓冲
                String ee = null;
                while ((ee = bufr.readLine()) != null) {
                    System.out.println(ee);
                }
                isr.close();
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line + "\n");
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (result.length() > 0)
                result.deleteCharAt(result.length() - 1);
            return result.toString();
        }

    }
    public String runShell(String[] cmd, String projectDir, long max_seconds) throws IOException {
        Process p;
        StringBuffer result = new StringBuffer();
        try {
            p = Runtime.getRuntime().exec(cmd, null, new File(projectDir));

            int exitCode = -1;
            if (max_seconds < 0) {
                exitCode = p.waitFor();
            } else {
                boolean normal_exit = p.waitFor(max_seconds, TimeUnit.SECONDS);
                if (!normal_exit) {
                    exitCode = -1;
                } else {
                    exitCode = 0;
                }
            }
            if (exitCode != 0) {
                System.out.println("run shell error");
                InputStreamReader isr = new InputStreamReader(p.getErrorStream(), "gbk");//读取
                System.out.println(isr.getEncoding());
                BufferedReader bufr = new BufferedReader(isr);//缓冲
                String ee = null;
                while ((ee = bufr.readLine()) != null) {
                    System.out.println(ee);
                }
                isr.close();
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line + "\n");
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (result.length() > 0)
                result.deleteCharAt(result.length() - 1);
            return result.toString();
        }

    }
    public String compileCheck(String[] cmd, String projectDir) throws IOException {
        Process p;
        StringBuffer result = new StringBuffer();
        try {
            p = Runtime.getRuntime().exec(cmd, null, new File(projectDir));
            int exitCode = -1;
            exitCode = p.waitFor();

            if (exitCode != 0) {
                System.out.println("run shell error");
                InputStreamReader isr = new InputStreamReader(p.getErrorStream(), "gbk");//读取
                System.out.println(isr.getEncoding());
                BufferedReader bufr = new BufferedReader(isr);//缓冲
                String ee = null;
                while ((ee = bufr.readLine()) != null) {
                    System.out.println(ee);
                }
                isr.close();
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line + "\n");
            }
            if (result.length() > 0)
                result.deleteCharAt(result.length() - 1);
            return result.toString();
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }
    public boolean runShellFile(String shellFileDir, String filename) {
        return runShellFile(shellFileDir, filename, -1);
    }

    public boolean runShellFile(String shellFileDir, String filename, long max_seconds) {
        ProcessBuilder pb = new ProcessBuilder("./" + filename);
        pb.directory(new File(shellFileDir));
        int runningStatus = 0;
        String s = null;
        try {
            Process p = pb.start();
            try {
                if (max_seconds < 0) {
                    runningStatus = p.waitFor();
                } else {
                    boolean normal_exit = p.waitFor(max_seconds, TimeUnit.SECONDS);
                    if (!normal_exit) {
                        runningStatus = -1;
                    } else {
                        runningStatus = 0;
                    }
                }
                if (runningStatus != 0) {
                    InputStreamReader isr = new InputStreamReader(p.getErrorStream(), "gbk");//读取
                    System.out.println(isr.getEncoding());
                    BufferedReader bufr = new BufferedReader(isr);//缓冲
                    String ee = null;
                    while ((ee = bufr.readLine()) != null) {
                        System.out.println(ee);
                    }
                    isr.close();
                }
            } catch (InterruptedException e) {
                System.out.println(e);

            }

        } catch (IOException e) {
            System.out.println(e);
        }
        if (runningStatus != 0) {
            return false;
        }
        return true;
    }

    public boolean runShellCmd(String cmd) throws IOException, InterruptedException {
        String[] cmdArray = cmd.split(" ");
        Process process = Runtime.getRuntime().exec(cmdArray);
        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = null;
        while ((line = input.readLine()) != null) {
            System.out.println(line);
        }
        input.close();
        int exitCode = process.waitFor();
        System.out.println("Exit code: " + exitCode);
        if(exitCode != 0) return false;
        return true;
    }
}
