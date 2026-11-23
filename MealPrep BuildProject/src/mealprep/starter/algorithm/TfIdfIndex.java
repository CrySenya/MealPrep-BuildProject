package mealprep.starter.algorithm;

import mealprep.starter.model.FoodItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * TfIdfIndex - the MAIN NLP ALGORITHM of the project.
 *
 * TF-IDF means "term frequency times inverse document frequency":
 *   - term frequency (TF)      : how often a word appears in one meal,
 *   - inverse document frequency (IDF) : how rare that word is in the whole menu.
 *
 * A word that appears in every meal (like "rice") gets a low IDF, so it cannot
 * dominate the ranking. A rare word (like "sinigang") gets a high IDF.
 *
 * Cosine similarity then measures the angle between the query vector and each
 * meal vector. Both vectors are normalized to length 1, so the dot product is
 * already the cosine, and the result is a score between 0.0 and 1.0.
 */
public class TfIdfIndex {

    // Worth of a word in a field. 
    private static final int SHORT_NAME_BOOST = 3;
    private static final int NAME_BOOST = 3;
    private static final int CATEGORY_BOOST = 2;
    private static final int TAG_BOOST = 1;

    /** The meals, in the same order as the vectors below. */
    private final ArrayList<FoodItem> documents = new ArrayList<>();

    /** One weight per word: how rare that word is across the whole menu. */
    private final HashMap<String, Double> idf = new HashMap<>();

    /** One normalized weight map per meal, ready for the dot product. */
    private final ArrayList<HashMap<String, Double>> vectors = new ArrayList<>();

    /**
     * Builds the index from the whole menu. Call this once after both CSV files
     * are loaded; after that, searching only reads the index.
     */
    public void build(ArrayList<FoodItem> foods) {
        // Start from a clean state so the method can be called twice safely.
        documents.clear();
        idf.clear();
        vectors.clear();

        // An empty menu cannot be indexed, so leave the index empty.
        if (foods == null || foods.isEmpty()) {
            return;
        }

        // Remember every meal in the order it was given.
        documents.addAll(foods);

        // Pass 1: count, for each word, how many meals contain it (document frequency).
        HashMap<String, Integer> documentFrequency = new HashMap<>();

        // Keep the raw weighted counts so pass 2 does not tokenize everything again.
        ArrayList<HashMap<String, Integer>> rawCounts = new ArrayList<>();

        for (FoodItem food : documents) {
            // One count map per meal.
            HashMap<String, Integer> counts = new HashMap<>();

            // The short name matters most, so it is added twice with a high boost.
            addWeighted(counts, food.getShortName(), SHORT_NAME_BOOST);

            // The full name includes the parenthetical words, such as "beef tapa".
            addWeighted(counts, food.getName(), NAME_BOOST);

            // The category is the brand for fast food ("Jollibee") and the cuisine otherwise.
            addWeighted(counts, food.getCategory(), CATEGORY_BOOST);

            // Tags are small hints such as "fried" or "popular", so they weigh least.
            addWeighted(counts, food.getTags(), TAG_BOOST);

            rawCounts.add(counts);

            // Each distinct word in this meal counts once for the document frequency.
            for (String word : counts.keySet()) {
                Integer seen = documentFrequency.get(word);
                documentFrequency.put(word, seen == null ? 1 : seen + 1);
            }
        }

        // Pass 2a: turn each document frequency into an IDF weight.
        int totalDocuments = documents.size();
        for (String word : documentFrequency.keySet()) {
            int df = documentFrequency.get(word);

            // The +1 inside the log avoids dividing by zero; the +1 outside keeps
            // common words above zero so they still add a little signal.
            double weight = Math.log((double) totalDocuments / (1 + df)) + 1.0;
            idf.put(word, weight);
        }

        // Pass 2b: multiply raw counts by IDF, then scale each meal to length 1.
        for (HashMap<String, Integer> counts : rawCounts) {
            HashMap<String, Double> vector = new HashMap<>();
            double squaredLength = 0.0;

            for (String word : counts.keySet()) {
                // TF-IDF weight for this word in this meal.
                double weight = counts.get(word) * idf.get(word);
                vector.put(word, weight);

                // Sum of squares, used for the length below.
                squaredLength += weight * weight;
            }

            // Dividing by the length makes every meal vector length 1.
            double length = Math.sqrt(squaredLength);
            if (length > 0.0) {
                for (String word : vector.keySet()) {
                    vector.put(word, vector.get(word) / length);
                }
            }

            vectors.add(vector);
        }
    }

    /**
     * Adds the words of one text field to a meal's count map.
     *
     * "boost" is how important that field is. A word that appears in both the
     * short name and the tags ends up with a bigger total count, which is what
     * we want: the name is stronger evidence than a tag.
     */
    private void addWeighted(HashMap<String, Integer> counts, String text, int boost) {
        // Reuse the normalizer so every field is split the same way.
        ArrayList<String> words = TextNormalizer.tokenize(text);
        for (String word : words) {
            Integer current = counts.get(word);
            counts.put(word, current == null ? boost : current + boost);
        }
    }

    /**
     * Scores one meal against a raw query string.
     *
     * This is a convenience wrapper; MealSearchEngine uses the token version so
     * it can fix spelling mistakes before scoring.
     */
    public double score(String query, int documentIndex) {
        return score(TextNormalizer.tokenize(query), documentIndex);
    }

    /**
     * Scores one meal against a list of query words using cosine similarity.
     *
     * Steps:
     *   1. give every query word its IDF weight,
     *   2. scale the query to length 1,
     *   3. multiply matching weights and add them up (the dot product).
     */
    public double score(ArrayList<String> queryWords, int documentIndex) {
        // A bad index means there is nothing to compare against.
        if (documentIndex < 0 || documentIndex >= vectors.size()) {
            return 0.0;
        }
        if (queryWords == null || queryWords.isEmpty()) {
            return 0.0;
        }

        // Step 1: build the query vector from words the index actually knows.
        HashMap<String, Double> queryVector = new HashMap<>();
        double squaredLength = 0.0;

        for (String word : queryWords) {
            // Unknown words carry no IDF, so they cannot help the score.
            if (!idf.containsKey(word)) {
                continue;
            }
            // Count each query word only once, even if the user typed it twice.
            if (queryVector.containsKey(word)) {
                continue;
            }
            double weight = idf.get(word);
            queryVector.put(word, weight);
            squaredLength += weight * weight;
        }

        // A query made only of unknown words matches nothing.
        if (squaredLength == 0.0) {
            return 0.0;
        }

        // Step 2: scale the query vector to length 1.
        double length = Math.sqrt(squaredLength);

        // Step 3: dot product with the already-normalized meal vector.
        double dotProduct = 0.0;
        HashMap<String, Double> documentVector = vectors.get(documentIndex);
        for (String word : queryVector.keySet()) {
            Double documentWeight = documentVector.get(word);
            if (documentWeight != null) {
                dotProduct += (queryVector.get(word) / length) * documentWeight;
            }
        }
        return dotProduct;
    }

    /** True when the index knows this word. Used before spell correction. */
    public boolean containsToken(String word) {
        return word != null && idf.containsKey(word);
    }

    /**
     * Every indexed word, sorted alphabetically.
     *
     * SpellCorrector needs the full vocabulary, and sorting makes suggestions
     * repeatable when two words are at the same distance.
     */
    public ArrayList<String> getVocabulary() {
        ArrayList<String> vocabulary = new ArrayList<>();
        for (String word : idf.keySet()) {
            // One- and two-letter words are too short to correct reliably.
            if (word.length() >= 3) {
                vocabulary.add(word);
            }
        }
        Collections.sort(vocabulary);
        return vocabulary;
    }

    /** Number of meals in the index. */
    public int size() {
        return documents.size();
    }

    /** The meal stored at one position. */
    public FoodItem getDocument(int index) {
        if (index < 0 || index >= documents.size()) {
            return null;
        }
        return documents.get(index);
    }
}
