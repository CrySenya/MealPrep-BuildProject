package mealprep.starter.algorithm;

import java.util.ArrayList;
import java.util.Collections;

/**
 * SpellCorrector - step 2 of the NLP search pipeline.
 *
 * It uses the Damerau-Levenshtein distance, which counts the smallest number of
 * single-character edits needed to turn one word into another:
 *   - insert a letter      ("chiken"  -> "chicken")
 *   - delete a letter      ("chickeen" -> "chicken")
 *   - substitute a letter  ("chickin" -> "chicken")
 *   - swap two neighbours  ("hcicken" -> "chicken")
 *
 * The fourth rule is what separates Damerau-Levenshtein from the plain
 * Levenshtein distance, and it is exactly the mistake people make when typing.
 */
public class SpellCorrector {

    /** Words shorter than this are not corrected; there are too many candidates. */
    private static final int MIN_WORD_LENGTH = 3;

    /** Every known word, sorted so suggestions are repeatable. */
    private final ArrayList<String> vocabulary = new ArrayList<>();

    /**
     * Stores the vocabulary that suggestions are chosen from.
     *
     * MealSearchEngine passes TfIdfIndex.getVocabulary(), so the corrector can
     * only suggest words that actually exist in the menu.
     */
    public void build(ArrayList<String> words) {
        vocabulary.clear();
        if (words == null) {
            return;
        }
        vocabulary.addAll(words);
        Collections.sort(vocabulary);
    }

    /**
     * Returns the closest known word, or null when nothing is close enough.
     *
     * A wrong suggestion is worse than no suggestion, so the allowed distance
     * depends on the word length: one edit for short words, two for longer ones.
     */
    public String suggest(String word) {
        // Nothing to correct if there is no word or no vocabulary.
        if (word == null || vocabulary.isEmpty()) {
            return null;
        }

        String lower = word.toLowerCase();

        // Very short words are skipped; "pi" could be many different things.
        if (lower.length() < MIN_WORD_LENGTH) {
            return null;
        }

        // Longer words tolerate two edits, short ones only one.
        int allowedDistance = lower.length() <= 4 ? 1 : 2;

        String bestWord = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String candidate : vocabulary) {
            // Words with very different lengths cannot be within the allowed distance.
            if (Math.abs(candidate.length() - lower.length()) > allowedDistance) {
                continue;
            }

            int distance = editDistance(lower, candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestWord = candidate;
            }
        }

        // Only return a suggestion when it is close enough to be believable.
        if (bestDistance <= allowedDistance) {
            return bestWord;
        }
        return null;
    }

    /**
     * Damerau-Levenshtein distance between two words.
     *
     * The method fills a table where cell [i][j] holds the distance between the
     * first i letters of "a" and the first j letters of "b". The answer is the
     * bottom-right cell.
     *
     * Example: editDistance("kitten", "sitting") = 3
     */
    public static int editDistance(String a, String b) {
        // Two empty strings are already equal.
        if (a == null || a.isEmpty()) {
            return b == null ? 0 : b.length();
        }
        if (b == null || b.isEmpty()) {
            return a.length();
        }

        int rows = a.length() + 1;
        int columns = b.length() + 1;

        // The table starts at zero in every cell.
        int[][] distance = new int[rows][columns];

        // Turning the first i letters into nothing costs i deletions.
        for (int i = 0; i < rows; i++) {
            distance[i][0] = i;
        }

        // Turning nothing into the first j letters costs j insertions.
        for (int j = 0; j < columns; j++) {
            distance[0][j] = j;
        }

        // Fill the rest of the table, one cell at a time.
        for (int i = 1; i < rows; i++) {
            for (int j = 1; j < columns; j++) {
                // Substitution costs 0 when the letters match, otherwise 1.
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;

                // Take the cheapest of delete, insert and substitute.
                int delete = distance[i - 1][j] + 1;
                int insert = distance[i][j - 1] + 1;
                int substitute = distance[i - 1][j - 1] + cost;

                int best = Math.min(delete, Math.min(insert, substitute));

                // Damerau rule: two neighbouring letters swapped cost 1, not 2.
                if (i > 1 && j > 1
                        && a.charAt(i - 1) == b.charAt(j - 2)
                        && a.charAt(i - 2) == b.charAt(j - 1)) {
                    best = Math.min(best, distance[i - 2][j - 2] + 1);
                }

                distance[i][j] = best;
            }
        }

        // The bottom-right cell is the distance between the whole words.
        return distance[rows - 1][columns - 1];
    }
}
