package mealprep.starter.model;

// Stores a food item that matched a search, along with its score and the reason it was suggested.
public class SearchResult {

    private final FoodItem food;
    private final double score;
    private final String matchedBy;

    // Creates a new SearchResult with the given food, score, and match reason.
    public SearchResult(FoodItem food, double score, String matchedBy) {
        this.food = food;
        this.score = score;
        this.matchedBy = matchedBy;
    }

    /** The meal that matched. */
    public FoodItem getFoodItem() {
        return food;
    }

    // The TF-IDF cosine score, normally between 0 and 1 that indicates how well the food matched the search terms. Higher is better.
    public double getScore() {
        return score;
    }

    /** Short text explaining the match, such as "alias" or "keywords". */
    public String getMatchedBy() {
        return matchedBy;
    }

    // Returns a string representation of the search result, including the score, food name, and match reason.
    @Override
    public String toString() {
        return String.format("%.2f  %s  [%s]", score, food.getName(), matchedBy);
    }
}
