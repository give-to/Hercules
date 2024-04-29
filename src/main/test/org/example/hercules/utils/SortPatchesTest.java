package org.example.hercules.utils;

import org.example.hercules.Patch;
import org.example.hercules.PatchFile;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.io.File.separator;

public class SortPatchesTest {
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

    @Test
    public void sortPatches() throws IOException {
        List<PatchFile> patchList = new ArrayList<>();
        String patchesRoot = "tmp_patches";
        String projectId = "Math_35";
        String[] groupList = new File(patchesRoot + separator + projectId).list();
        for (int i = 0; i < groupList.length; i++) {

            File groupDir = new File(patchesRoot + separator + projectId + separator + groupList[i] + separator + "patches");
            if (groupDir.isDirectory()) {
                String[] patchFileList = groupDir.list();
                for (int j = 0; j < patchFileList.length; j++) {
                    String patchFilePath = groupDir.getPath() + separator + patchFileList[j];
                    PatchFile patch = new PatchFile(groupList[i], patchFileList[j]);
                    String[] patchFileContent = readFile(patchFilePath).split(System.getProperty("line.separator"));
                    List<Patch> content = new ArrayList<>();
                    for (int k = 0; k < patchFileContent.length; k++) {
                        content.add(new Patch(patchFileContent[k]));
                    }
                    patch.setPatch(content);
                    patchList.add(patch);
                }
            }
        }

        SortPatches.sortPatches(patchList);

    }
}
