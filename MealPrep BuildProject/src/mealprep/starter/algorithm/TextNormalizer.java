package mealprep.starter.algorithm;

import java.util.ArrayList;

// This class normalizes text for searching and ranking food items. It lowercases, removes punctuation, splits into words, and drops stopwords.
public class TextNormalizer {

    // A list of common words that are not useful for searching. These are dropped from the search terms so they do not affect the ranking.
    private static final String[] STOPWORDS = {
            "a", "an", "the", "with", "and", "or", "of", "for", "plus", "to",
            "my", "i", "me", "ate", "eat", "had", "have", "some", "any",
            "order", "orders", "meal", "meals", "serving", "servings",
            "pc", "pcs", "piece", "pieces", "plate", "plates", "cup", "cups",
            "ako", "ang", "ng", "sa", "at", "is", "na"
    };

    // Private constructor to prevent instantiation. This class is a utility class with static methods only.
    private TextNormalizer() {
    }

    // Normalizes text by lowercasing, removing punctuation, and trimming whitespace.
    public static String normalize(String text) {
        // A null or blank input has no words, so return an empty string.
        if (text == null) {
            return "";
        }

        // Lowercase first so "Chickenjoy" and "chickenjoy" become identical.
        String lower = text.toLowerCase();

        // Build the cleaned text one character at a time.
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                // Letters and digits are kept as they are.
                cleaned.append(c);
            } else {
                // Punctuation and symbols become a space so words do not merge.
                cleaned.append(' ');
            }
        }

        // trim() removes the leading and trailing spaces we just added.
        return cleaned.toString().trim();
    }

    // Splits normalized text into words, dropping stopwords. Returns an empty list for null or blank input.
    public static ArrayList<String> tokenize(String text) {
        ArrayList<String> tokens = new ArrayList<>();

        // Normalize first, then split on one or more spaces.
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return tokens;
        }

        String[] parts = normalized.split("\\s+");
        for (String part : parts) {
            // Skip empty pieces caused by double spaces.
            if (part.isEmpty()) {
                continue;
            }
            // Skip stopwords so they never reach the ranking step.
            if (isStopword(part)) {
                continue;
            }
            tokens.add(part);
        }
        return tokens;
    }

    // Returns true if the token is a stopword. Null or blank tokens are not stopwords.
    public static boolean isStopword(String token) {
        if (token == null) {
            return false;
        }
        String wanted = token.trim().toLowerCase();
        for (int i = 0; i < STOPWORDS.length; i++) {
            if (STOPWORDS[i].equals(wanted)) {
                return true;
            }
        }
        return false;
    }
}
