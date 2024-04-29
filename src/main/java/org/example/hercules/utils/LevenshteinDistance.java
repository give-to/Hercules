package org.example.hercules.utils;

public class LevenshteinDistance {

    public static int calculateDistance(String s1, String s2) {
        int[][] distanceMatrix = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            distanceMatrix[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            distanceMatrix[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int substitutionCost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                distanceMatrix[i][j] = Math.min(Math.min(distanceMatrix[i - 1][j] + 1, distanceMatrix[i][j - 1] + 1),
                        distanceMatrix[i - 1][j - 1] + substitutionCost);
            }
        }

        return distanceMatrix[s1.length()][s2.length()];
    }
}
