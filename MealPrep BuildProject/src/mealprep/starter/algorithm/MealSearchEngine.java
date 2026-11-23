package mealprep.starter.algorithm;

import mealprep.starter.model.FoodItem;
import mealprep.starter.model.SearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * MealSearchEngine - the conductor of the NLP search.
 *
 * It runs the three algorithms in order:
 *   1. TextNormalizer  : clean the text into words,
 *   2. SpellCorrector  : repair a word that is not in the menu,
 *   3. FoodAliases     : expand nicknames such as "tapsi",
 *   4. TfIdfIndex      : rank every meal with TF-IDF cosine similarity.
 *
 * Step 4 is the main algorithm. Steps 1 to 3 exist to give step 4 clean input.
 */
public class MealSearchEngine {

    /** Points added when a meal is found through the alias dictionary. */
    private static final double ALIAS_BONUS = 0.5;

    /** The TF-IDF ranking model. */
    private final TfIdfIndex index = new TfIdfIndex();

    /** The typo repair model. */
    private final SpellCorrector corrector = new SpellCorrector();

    /** The nickname dictionary. */
    private final FoodAliases aliases = new FoodAliases();

    /** The meals being searched, in index order. */
    private final ArrayList<FoodItem> foods = new ArrayList<>();

    /** True after build() has finished, so search() knows the index is usable. */
    private boolean ready = false;

    /**
     * Builds every part of the pipeline. Call once after the CSV files load.
     *
     * The alias file is optional. Passing null simply skips it.
     */
    public void build(ArrayList<FoodItem> allFoods, String aliasFilePath) {
        foods.clear();
        if (allFoods != null) {
            foods.addAll(allFoods);
        }

        // The index must be built first, because the corrector needs its vocabulary.
        index.build(foods);

        // The corrector may only suggest words that exist in the menu.
        corrector.build(index.getVocabulary());

        // Aliases are optional, so a false return value is not an error.
        if (aliasFilePath != null) {
            aliases.load(aliasFilePath);
        }

        ready = true;
    }

    /** True when build() has run. Used by the self-tests. */
    public boolean isReady() {
        return ready;
    }

    /**
     * Returns every matching meal for one course, best match first.
     *
     * A null course means "search all courses". The method never returns null;
     * an empty list means nothing matched.
     */
    public ArrayList<SearchResult> search(String query, FoodItem.MealType type) {
        ArrayList<SearchResult> results = new ArrayList<>();
        if (!ready || query == null) {
            return results;
        }

        // Step 1: clean the text into words.
        ArrayList<String> originalWords = TextNormalizer.tokenize(query);
        if (originalWords.isEmpty()) {
            return results;
        }

        // Step 2: repair words the menu does not know.
        ArrayList<String> searchWords = new ArrayList<>();
        String correctionNote = null;

        for (String word : originalWords) {
            // A known word, or a known nickname, is used exactly as typed.
            if (index.containsToken(word) || !aliases.lookupToken(word).isEmpty()) {
                searchWords.add(word);
                continue;
            }

            // An unknown word is replaced by the closest menu word, if any.
            String suggestion = corrector.suggest(word);
            if (suggestion == null) {
                // No close word: drop it rather than let it pollute the score.
                continue;
            }
            searchWords.add(suggestion);

            // Keep the first repair so the GUI can explain what happened.
            if (correctionNote == null) {
                correctionNote = word + " -> " + suggestion;
            }
        }

        // If every word was dropped, fall back to the original words.
        if (searchWords.isEmpty()) {
            searchWords.addAll(originalWords);
        }

        // Step 3: collect alias hits from both the typed words and the repaired words.
        ArrayList<Integer> aliasIds = aliases.lookupTokens(originalWords);
        ArrayList<Integer> repairedAliasIds = aliases.lookupTokens(searchWords);
        for (Integer id : repairedAliasIds) {
            if (!aliasIds.contains(id)) {
                aliasIds.add(id);
            }
        }

        // Step 4: score every meal in the requested course.
        for (int i = 0; i < index.size(); i++) {
            FoodItem food = index.getDocument(i);
            if (food == null) {
                continue;
            }

            // Keep only the course the user is working on.
            if (type != null && food.getMealType() != type) {
                continue;
            }

            // The main algorithm: TF-IDF cosine similarity.
            double score = index.score(searchWords, i);

            // A dictionary hit is strong evidence, so it adds a fixed bonus.
            boolean aliasHit = aliasIds.contains(food.getId());
            if (aliasHit) {
                score += ALIAS_BONUS;
            }

            // A score of zero means the meal shares no words with the query.
            if (score <= 0.0) {
                continue;
            }

            // Record why this meal matched, for display and for debugging.
            String matchedBy;
            if (aliasHit && correctionNote != null) {
                matchedBy = "alias; corrected " + correctionNote;
            } else if (aliasHit) {
                matchedBy = "alias";
            } else if (correctionNote != null) {
                matchedBy = "corrected " + correctionNote;
            } else {
                matchedBy = "keywords";
            }

            results.add(new SearchResult(food, score, matchedBy));
        }

        // Sort from the highest score to the lowest.
        Collections.sort(results, new Comparator<SearchResult>() {
            public int compare(SearchResult first, SearchResult second) {
                if (first.getScore() > second.getScore()) {
                    return -1;
                }
                if (first.getScore() < second.getScore()) {
                    return 1;
                }
                // Equal scores: sort by name so the order never changes.
                return first.getFoodItem().getName().compareTo(second.getFoodItem().getName());
            }
        });

        return results;
    }

    /**
     * Returns a corrected word for the query, or null when nothing is misspelled.
     *
     * The GUI uses this for the "Did you mean ...?" message.
     */
    public String suggestQuery(String query) {
        ArrayList<String> words = TextNormalizer.tokenize(query);
        for (String word : words) {
            if (index.containsToken(word)) {
                continue;
            }
            String suggestion = corrector.suggest(word);
            if (suggestion != null) {
                return suggestion;
            }
        }
        return null;
    }

    /** Food ids that the alias dictionary knows for one word. Used by the tests. */
    public ArrayList<Integer> lookupAlias(String word) {
        return aliases.lookupToken(word);
    }

    /** Number of aliases loaded. */
    public int aliasCount() {
        return aliases.size();
    }

    /** Number of meals in the index. */
    public int documentCount() {
        return index.size();
    }
}
