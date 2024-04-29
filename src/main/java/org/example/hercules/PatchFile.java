package org.example.hercules;

import java.util.List;

public class PatchFile {
    private String groupId;
    private String patchId;
    private List<Patch> content;
    private double odsScore;

    public PatchFile(){}

    public PatchFile(String groupId, String patchId) {
        this.groupId = groupId;
        this.patchId = patchId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getPatchId() {
        return patchId;
    }

    public void setPatchId(String patchId) {
        this.patchId = patchId;
    }

    public List<Patch> getPatch() {
        return content;
    }

    public void setPatch(List<Patch> content) {
        this.content = content;
    }

    public double getOdsScore() {
        return odsScore;
    }

    public void setOdsScore(double odsScore) {
        this.odsScore = odsScore;
    }
}
