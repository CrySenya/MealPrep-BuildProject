package mealprep.starter.algorithm;

import mealprep.starter.database.FoodDatabase;
import mealprep.starter.model.FoodItem;
import mealprep.starter.model.SearchResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * SearchEvaluator - the experiment that proves the NLP layer helps.
 *
 * It reads data/search_eval.csv, which contains queries with the correct
 * answer, and measures two versions of the search:
 *
 *   baseline : the old substring method (FoodDatabase.searchFoodsBaseline)
 *   ranked   : the new NLP pipeline (FoodDatabase.searchRanked)
 *
 * Three standard measurements are reported:
 *   Precision@1 : was the correct meal first?
 *   Precision@3 : was the correct meal in the top three?
 *   MRR         : mean reciprocal rank - 1.0 for first place, 0.5 for second,
 *                 0.33 for third, and 0 when the meal is missing.
 *
 * This is the part that turns a feature into a result.
 */
public class SearchEvaluator {

    /**
     * One test row: what the user typed, and the food id that should be found.
     */
    public static class QueryRow {

        private final String query;
        private final int expectedId;

        // Stores one labelled query: the text and the food id it should find.
        public QueryRow(String query, int expectedId) {
            this.query = query;
            this.expectedId = expectedId;
        }

        // The text that was typed for this test row.
        public String getQuery() {
            return query;
        }

        // The food id this query should return.
        public int getExpectedId() {
            return expectedId;
        }
    }

    /**
     * Reads search_eval.csv. Format: query,expected_id, one row per line.
     *
     * The query may contain commas only if it is quoted, which the parser below
     * handles by removing the quotes.
     */
    public static ArrayList<QueryRow> loadQueries(String filePath) {
        ArrayList<QueryRow> rows = new ArrayList<>();

        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("Warning: evaluation file not found: " + filePath);
            return rows;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // Split only on the last comma: the query may contain commas.
                int lastComma = line.lastIndexOf(',');
                if (lastComma <= 0) {
                    continue;
                }

                String query = line.substring(0, lastComma).trim().replace("\"", "");
                String idText = line.substring(lastComma + 1).trim();

                int expectedId;
                try {
                    expectedId = Integer.parseInt(idText);
                } catch (NumberFormatException e) {
                    continue;
                }

                rows.add(new QueryRow(query, expectedId));
            }

        } catch (IOException e) {
            System.err.println("Warning: could not read evaluation file: " + e.getMessage());
        }

        return rows;
    }

    /**
     * Precision@k for one version of the search.
     *
     * "ranked" is true for the NLP pipeline and false for the old baseline.
     * The result is the fraction of queries where the expected meal appeared in
     * the first k answers.
     */
    public static double precisionAtK(FoodDatabase database, ArrayList<QueryRow> rows,
                                      int k, boolean ranked) {
        if (rows == null || rows.isEmpty()) {
            return 0.0;
        }

        int hits = 0;
        for (QueryRow row : rows) {
            ArrayList<Integer> topIds = topIds(database, row.getQuery(), k, ranked);
            if (topIds.contains(row.getExpectedId())) {
                hits++;
            }
        }
        return (double) hits / rows.size();
    }

    /**
     * Mean reciprocal rank for one version of the search.
     *
     * The first correct answer earns 1/1, the second earns 1/2, and so on.
     * A query with no correct answer earns 0.
     */
    public static double meanReciprocalRank(FoodDatabase database, ArrayList<QueryRow> rows,
                                            boolean ranked) {
        if (rows == null || rows.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (QueryRow row : rows) {
            ArrayList<Integer> allIds = topIds(database, row.getQuery(), Integer.MAX_VALUE, ranked);
            for (int i = 0; i < allIds.size(); i++) {
                if (allIds.get(i) == row.getExpectedId()) {
                    total += 1.0 / (i + 1);
                    break;
                }
            }
        }
        return total / rows.size();
    }

    /**
     * Collects the ids of the first "limit" results from either search version.
     */
    private static ArrayList<Integer> topIds(FoodDatabase database, String query,
                                             int limit, boolean ranked) {
        ArrayList<Integer> ids = new ArrayList<>();

        if (ranked) {
            ArrayList<SearchResult> results = database.searchRanked(query, null);
            for (int i = 0; i < results.size() && i < limit; i++) {
                ids.add(results.get(i).getFoodItem().getId());
            }
        } else {
            ArrayList<FoodItem> results = database.searchFoodsBaseline(query, null);
            for (int i = 0; i < results.size() && i < limit; i++) {
                ids.add(results.get(i).getId());
            }
        }
        return ids;
    }

    /**
     * Prints the full comparison table. Called from StarterApp.
     */
    public static void printReport(FoodDatabase database, String filePath) {
        ArrayList<QueryRow> rows = loadQueries(filePath);

        System.out.println("--- SEARCH EVALUATION: BASELINE VS NLP ---");
        if (rows.isEmpty()) {
            System.out.println("No evaluation rows. Is " + filePath + " present?");
            System.out.println();
            return;
        }

        System.out.println("Queries: " + rows.size());
        System.out.printf("%-16s %-12s %-12s%n", "Measure", "Baseline", "NLP");
        System.out.printf("%-16s %-12.3f %-12.3f%n", "Precision@1",
                precisionAtK(database, rows, 1, false),
                precisionAtK(database, rows, 1, true));
        System.out.printf("%-16s %-12.3f %-12.3f%n", "Precision@3",
                precisionAtK(database, rows, 3, false),
                precisionAtK(database, rows, 3, true));
        System.out.printf("%-16s %-12.3f %-12.3f%n", "MRR",
                meanReciprocalRank(database, rows, false),
                meanReciprocalRank(database, rows, true));
        System.out.println();
    }
}
