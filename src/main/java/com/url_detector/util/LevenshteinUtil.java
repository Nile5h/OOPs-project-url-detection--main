package com.url_detector.util;

public final class LevenshteinUtil {

    private LevenshteinUtil() {
        // utility class — no instances
    }

    public static int computeDistance(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";

        int m = a.length();
        int n = b.length();

        // Ensure b is the shorter string to minimise memory
        if (m < n) {
            String tmp = a; a = b; b = tmp;
            int t = m; m = n; n = t;
        }

        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];

        // Initialise first row: cost of deleting all chars of b
        for (int j = 0; j <= n; j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            for (int j = 1; j <= n; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(
                    Math.min(prev[j] + 1,       // deletion
                             curr[j - 1] + 1),  // insertion
                    prev[j - 1] + cost           // substitution
                );
            }
            int[] swap = prev; prev = curr; curr = swap;
        }

        return prev[n];
    }
}
