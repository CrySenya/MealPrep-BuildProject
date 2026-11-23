package mealprep.starter.model;

// Stores a food item and its score, portions, and reason for recommendation.
public class MealOption {

    //attributes
    private final FoodItem foodItem;
    private double portions = 1.0;      // servings, starts at one
    private double score;               // recommendation quality score
    private boolean selected;           // true when this is the meal currently picked for its course
    private boolean userPicked;         // true when the user pressed Select on this meal
    private boolean addedBySearch;      // whether the user has selected this option
    private String reason;              // reason for recommendation

    // Stores the food, its score and the reason it was suggested.
    public MealOption(FoodItem foodItem, double score, String reason) {
        this.foodItem = foodItem;
        this.score = score;
        this.reason = reason;
    }

    // Sets the number of servings
    public void setPortions(double portions) {
        if (portions < 1.0) portions = 1.0;     //minimum of 1 portion
        if (portions > 3.0) portions = 3.0;     //maximum of 3 portions
        this.portions = portions;
    }

    // How many servings this option represents.
    public double getPortions() { return portions; }

    //Each getter asks the wrapped food for its base value, then multiplies by portions.
    public int getCalories() {
        return (int) Math.round(foodItem.getCalories() * portions);
    }

    
    public double getProtein() {
        return foodItem.getProtein() * portions;
    }

    
    public double getCarbs() {
        return foodItem.getCarbs() * portions;
    }

    
    public double getFat() {
        return foodItem.getFat() * portions;
    }

    //the name of the meal and portion size, formatted for display
    public String getName() {
        if (portions == 1.0) {
            return foodItem.getName();
        }
        return foodItem.getName() + " x" + formatPortion(portions);
    }

    //the serving size of the meal and portion size, formatted for display
    public String getServingSize() {
        if (portions == 1.0) {
            return foodItem.getServingSize();
        }
        return foodItem.getServingSize() + " x" + formatPortion(portions);
    }

    //drops the .0 decimal, unless like 1.5, in which case it keeps one decimal place
    public static String formatPortion(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }

    //accessors 
    public FoodItem getFoodItem() { return foodItem; }
    public double getScore() { return score; }
    public boolean isSelected() { return selected; }
    public boolean isUserPicked() { return userPicked; }
    public boolean isAddedBySearch() { return addedBySearch; }
    public boolean isFastFood() { return foodItem.isFastFood(); }
    public String getReason() { return reason; }

    public void setSelected(boolean selected) { this.selected = selected; }
    public void setUserPicked(boolean userPicked) { this.userPicked = userPicked; }
    public void setAddedBySearch(boolean addedBySearch) { this.addedBySearch = addedBySearch; }
    public void setScore(double score) { this.score = score; }
    public void setReason(String reason) { this.reason = reason; }

    //returns the meal type of the food item (e.g., breakfast, lunch, dinner)
    public FoodItem.MealType getMealType() {
        return foodItem.getMealType();
    }

    //printable representation of the meal option, including calories and reason for recommendation
    @Override
    public String toString() {
        return String.format("%s (%d kcal) - %s", getName(), getCalories(), reason);
    }
}
