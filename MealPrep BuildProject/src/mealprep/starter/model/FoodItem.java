package mealprep.starter.model;

/**
 * One food from the CSV file.
 *
 * This class is the same idea as the reference FoodItem, but the Starter
 * Edition keeps only the fields it needs.
 */
public class FoodItem {

    private int id;
    private String name;
    private String category;
    private MealType mealType;
    private int calories;
    private double protein;
    private double carbs;
    private double fat;
    private String servingSize;
    private String tags;

    public enum MealType {
        BREAKFAST, LUNCH, DINNER, SNACK
    }

    public FoodItem(int id, String name, String category, String mealTypeText,
                    int calories, double protein, double carbs, double fat,
                    String servingSize, String tags) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.mealType = parseMealType(mealTypeText);
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.servingSize = servingSize;
        this.tags = tags.toLowerCase();
    }

    private MealType parseMealType(String text) {
        try {
            return MealType.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MealType.LUNCH;
        }
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public MealType getMealType() { return mealType; }
    public int getCalories() { return calories; }
    public double getProtein() { return protein; }
    public double getCarbs() { return carbs; }
    public double getFat() { return fat; }
    public String getServingSize() { return servingSize; }
    public String getTags() { return tags; }

    /**
     * Exact tag match. We compare whole tags so that "milk" does not match
     * the tag "milkfish".
     */
    public boolean hasTag(String tag) {
        if (tag == null || tags == null) return false;

        String wanted = tag.trim().toLowerCase();
        if (wanted.isEmpty()) return false;

        String[] parts = tags.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].trim().equals(wanted)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %d kcal | P:%.1f C:%.1f F:%.1f",
                name, servingSize, calories, protein, carbs, fat);
    }
}
