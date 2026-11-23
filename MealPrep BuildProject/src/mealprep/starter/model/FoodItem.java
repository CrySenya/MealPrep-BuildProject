package mealprep.starter.model;

public class FoodItem {
    // attributes
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

    // meal types constants
    public enum MealType {
        BREAKFAST, LUNCH, DINNER, SNACK
    }

    // constructor
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

    // String to MealType (LUNCH is default)
    private MealType parseMealType(String text) {
        try {
            return MealType.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MealType.LUNCH;
        }
    }

    // getters
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

    // True when this row came from the fast-food CSV. 
    public boolean isFastFood() {
        return hasTag("fastfood");
    }

    // Restaurant name for fast food, or the category for home-cooked food.
    public String getBrand() {
        return category == null ? "" : category;
    }

    // Returns the short name of the food item, which is the part before any parentheses.
    // For example, "Chicken Sandwich (Grilled)" becomes "Chicken Sandwich".
    public String getShortName() {
        if (name == null) {
            return "";
        }
        int openParen = name.indexOf('(');
        if (openParen > 0) {
            return name.substring(0, openParen).trim();
        }
        return name.trim();
    }

    // Returns a string that contains the short name, full name, brand, and tags, all lowercased and separated by spaces. This is used for searching.
    public String getSearchText() {
        String shortName = getShortName();
        String fullName = name == null ? "" : name;
        String brand = category == null ? "" : category;
        String tagText = tags == null ? "" : tags;
        return shortName + " " + fullName + " " + brand + " " + tagText;
    }

    // Exact tag match. We compare whole tags so that "milk" does not match the tag "milkfish"
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

    // get String of this object or class
    @Override
    public String toString() {
        return String.format("%s (%s) - %d kcal | P:%.1f C:%.1f F:%.1f",
                name, servingSize, calories, protein, carbs, fat);
    }
}