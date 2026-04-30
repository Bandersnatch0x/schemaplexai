package com.schemaplexai.spec.util;

import com.schemaplexai.spec.dto.DiffHunk;

import java.util.ArrayList;
import java.util.List;

public class SpecDiffUtil {

    public static List<DiffHunk> diff(String textA, String textB) {
        String[] linesA = textA == null ? new String[0] : textA.split("\n", -1);
        String[] linesB = textB == null ? new String[0] : textB.split("\n", -1);

        int[][] lcs = computeLcs(linesA, linesB);
        List<DiffHunk.LineChange> changes = backtrack(linesA, linesB, lcs, linesA.length, linesB.length);

        return groupIntoHunks(changes);
    }

    private static int[][] computeLcs(String[] a, String[] b) {
        int[][] dp = new int[a.length + 1][b.length + 1];
        for (int i = 1; i <= a.length; i++) {
            for (int j = 1; j <= b.length; j++) {
                if (a[i - 1].equals(b[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp;
    }

    private static List<DiffHunk.LineChange> backtrack(String[] a, String[] b, int[][] dp, int i, int j) {
        List<DiffHunk.LineChange> changes = new ArrayList<>();
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && a[i - 1].equals(b[j - 1])) {
                changes.add(0, new DiffHunk.LineChange("UNCHANGED", a[i - 1]));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                changes.add(0, new DiffHunk.LineChange("ADDED", b[j - 1]));
                j--;
            } else if (i > 0) {
                changes.add(0, new DiffHunk.LineChange("REMOVED", a[i - 1]));
                i--;
            }
        }
        return changes;
    }

    private static List<DiffHunk> groupIntoHunks(List<DiffHunk.LineChange> changes) {
        List<DiffHunk> hunks = new ArrayList<>();
        List<DiffHunk.LineChange> current = new ArrayList<>();
        int oldLine = 1;
        int newLine = 1;
        int hunkOldStart = 1;
        int hunkNewStart = 1;

        for (int i = 0; i < changes.size(); i++) {
            DiffHunk.LineChange change = changes.get(i);
            boolean isChange = !"UNCHANGED".equals(change.getType());

            if (isChange) {
                if (current.isEmpty()) {
                    hunkOldStart = oldLine;
                    hunkNewStart = newLine;
                }
                current.add(change);
            } else {
                if (!current.isEmpty()) {
                    // Add context lines before and after
                    hunks.add(buildHunk(hunkOldStart, hunkNewStart, current));
                    current = new ArrayList<>();
                }
                oldLine++;
                newLine++;
            }

            if ("REMOVED".equals(change.getType())) {
                oldLine++;
            } else if ("ADDED".equals(change.getType())) {
                newLine++;
            }
        }

        if (!current.isEmpty()) {
            hunks.add(buildHunk(hunkOldStart, hunkNewStart, current));
        }

        return hunks;
    }

    private static DiffHunk buildHunk(int oldStart, int newStart, List<DiffHunk.LineChange> changes) {
        int oldCount = (int) changes.stream().filter(c -> !"ADDED".equals(c.getType())).count();
        int newCount = (int) changes.stream().filter(c -> !"REMOVED".equals(c.getType())).count();
        return new DiffHunk(oldStart, oldCount, newStart, newCount, new ArrayList<>(changes));
    }
}
