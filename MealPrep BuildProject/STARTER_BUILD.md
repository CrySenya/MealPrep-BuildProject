# MealPrep - Starter Edition (Complete Code)

This is the **smallest edition** of MealPrep. It keeps the same single input page and the same visual style as the reference, but removes the features that make the full app long.

| Feature | Full / Beginner edition | Starter Edition |
|---|---|---|
| Single input page | Yes | Yes |
| Body, activity, goal, mood | Yes | Yes |
| Allergies | Checkboxes | Checkboxes |
| Dislikes | `JList` multi-select | **Checkboxes, multi-select** |
| Meal options per course | 3 | 3 |
| Skip a course | Yes | Yes |
| Calorie result after picking | Summary bar | **Dedicated summary page** |
| Meal history | Yes | **No** |
| Export | Yes | **No** |
| Repair / regenerate buttons | Yes | **No** |
| Side dishes | Yes | **No** |
| Calculator-only mode | Yes | Yes |

> The Starter Edition is **not** a different algorithm. It uses the same Mifflin-St Jeor calculator, the same 25/35/30/10 course percentages, the same skip redistribution, and the same idea of scoring foods and taking the top three. It simply removes features and uses one dish per option.

## Screenshots

These are real screenshots of the Starter Edition in `starter/preview/`.

![Single input page](starter/preview/01-single-form.png)

*The same single input page. Dislikes are checkboxes, so several can be ticked.*

![Results with breakfast skipped](starter/preview/03-results-skipped-breakfast.png)

*Three options per course and a skip checkbox per course.*

![Final summary](starter/preview/04-result-summary.png)

*The final summary: target, picked calories, difference, macros, and every meal picked.*

## How to use this guide

1. Create a new folder, for example `MealPrep Starter`.
2. Create `src/mealprep/starter/{model,database,algorithm,gui}` and a `data` folder.
3. Copy `data/philippine_foods.csv` from the reference project. Do not retype 90 rows.
4. Type the files in the order below. Run `.\build.bat` after each phase.
5. Stop at Phase 9. The console must print `SELF-TESTS: 26 passed, 0 failed` before you start the GUI.
6. Run `.\run-gui.bat` and walk through the form, the options, the skip checkbox, and the summary page.

PowerShell note: run scripts with `.\build.bat`, not `build.bat`.

## Folder layout

~~~text
MealPrep Starter/
  build.bat
  run-console.bat
  run-gui.bat
  data/
    philippine_foods.csv
  src/mealprep/starter/
    StarterApp.java
    model/
    database/
    algorithm/
    gui/
~~~

## Build order

| Phase | File | Covers |
|---|---|---|
| 1 | `src/mealprep/starter/model/FoodItem.java` | FoodItem - one dish |
| 2 | `src/mealprep/starter/database/FoodDatabase.java` | FoodDatabase - load and filter the CSV |
| 3 | `src/mealprep/starter/model/UserProfile.java` | UserProfile - all form data |
| 4 | `src/mealprep/starter/algorithm/CalorieCalculator.java` | CalorieCalculator - BMR, TDEE, target, BMI |
| 5 | `src/mealprep/starter/model/MealOption.java` | MealOption - one recommended dish |
| 6 | `src/mealprep/starter/model/MealPlan.java` | MealPlan - budgets, skipped courses, totals |
| 7 | `src/mealprep/starter/algorithm/MealRecommender.java` | MealRecommender - filter, score, pick 3 |
| 8 | `src/mealprep/starter/algorithm/MealPlanGenerator.java` | MealPlanGenerator - put the pieces together |
| 9 | `src/mealprep/starter/StarterApp.java` | StarterApp - console entry point and 26 tests |
| 10 | `src/mealprep/starter/gui/ScrollablePanel.java` | ScrollablePanel - a width-aware panel |
| 11 | `src/mealprep/starter/gui/StarterGUI.java` | StarterGUI - one input page |
| 12 | `src/mealprep/starter/gui/StarterResultsPanel.java` | StarterResultsPanel - 3 options per course and skip |
| 13 | `src/mealprep/starter/gui/StarterSummaryPanel.java` | StarterSummaryPanel - calories and meals picked |

---

## Phase 1. FoodItem - one dish

**File:** `src/mealprep/starter/model/FoodItem.java`

A `FoodItem` is one row of the CSV: id, name, category, meal type, calories, protein, carbs, fat, serving size, and tags.

`MealType` is an `enum`. It prevents spelling mistakes such as `"breakfst"` and lets the compiler check every course.

`hasTag` compares whole comma-separated tags. That is why the dairy tag `milk` does not match the fish tag `milkfish`.

Concepts: class, fields, constructor, `enum`, `String.split`, `for` loop.

Complete code:

~~~java
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
~~~

---

## Phase 2. FoodDatabase - load and filter the CSV

**File:** `src/mealprep/starter/database/FoodDatabase.java`

The Starter Edition keeps all 90 foods in one `ArrayList` and filters with ordinary loops. There is no `HashMap` index because the data set is small.

`parseCSVLine` handles quoted fields. A plain `line.split(",")` would break names such as `"Tapsilog (Beef Tapa + Sinangag + Itlog)"`.

`getFoodsByMealType` returns a new list with only breakfast, lunch, dinner, or snack foods.

Concepts: `ArrayList`, `BufferedReader`, `try-with-resources`, loops, `StringBuilder`.

Complete code:

~~~java
package mealprep.starter.database;

import mealprep.starter.model.FoodItem;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Loads the Philippine food CSV.
 *
 * The Starter Edition keeps all foods in one ArrayList and filters with
 * ordinary loops. That is slower than an index, but there are only 90 rows
 * and the code is much easier to follow.
 */
public class FoodDatabase {

    private ArrayList<FoodItem> allFoods = new ArrayList<>();

    /**
     * Reads data/philippine_foods.csv.
     * The first line is the header. Quoted fields may contain commas.
     */
    public boolean loadFromCSV(String filePath) {
        allFoods.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                try {
                    FoodItem food = parseCSVLine(line);
                    if (food != null) {
                        allFoods.add(food);
                    }
                } catch (Exception e) {
                    System.err.println("Warning: could not read line " + lineNumber
                            + ": " + e.getMessage());
                }
            }

            System.out.println("Loaded " + allFoods.size()
                    + " food items from " + filePath);
            return true;

        } catch (FileNotFoundException e) {
            System.err.println("Error: food file not found: " + filePath);
            return false;
        } catch (IOException e) {
            System.err.println("Error reading food file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Splits one CSV line, respecting quoted fields.
     * A plain split(",") would break names such as "Tapsilog (Beef Tapa...)".
     */
    private FoodItem parseCSVLine(String line) {
        ArrayList<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());

        if (fields.size() < 10) {
            throw new IllegalArgumentException("expected 10 fields, got " + fields.size());
        }

        int id = Integer.parseInt(fields.get(0));
        String name = fields.get(1).replace("\"", "");
        String category = fields.get(2).replace("\"", "");
        String mealType = fields.get(3).replace("\"", "");
        int calories = Integer.parseInt(fields.get(4));
        double protein = Double.parseDouble(fields.get(5));
        double carbs = Double.parseDouble(fields.get(6));
        double fat = Double.parseDouble(fields.get(7));
        String servingSize = fields.get(8).replace("\"", "");
        String tags = fields.get(9).replace("\"", "");

        return new FoodItem(id, name, category, mealType, calories,
                protein, carbs, fat, servingSize, tags);
    }

    public ArrayList<FoodItem> getAllFoods() {
        return new ArrayList<>(allFoods);
    }

    public ArrayList<FoodItem> getFoodsByMealType(FoodItem.MealType type) {
        ArrayList<FoodItem> result = new ArrayList<>();
        for (FoodItem food : allFoods) {
            if (food.getMealType() == type) {
                result.add(food);
            }
        }
        return result;
    }

    public FoodItem getFoodById(int id) {
        for (FoodItem food : allFoods) {
            if (food.getId() == id) {
                return food;
            }
        }
        return null;
    }

    public int size() {
        return allFoods.size();
    }

    public int size(FoodItem.MealType type) {
        return getFoodsByMealType(type).size();
    }

    @Override
    public String toString() {
        return String.format("FoodDatabase[total=%d, breakfast=%d, lunch=%d, dinner=%d, snacks=%d]",
                allFoods.size(),
                size(FoodItem.MealType.BREAKFAST),
                size(FoodItem.MealType.LUNCH),
                size(FoodItem.MealType.DINNER),
                size(FoodItem.MealType.SNACK));
    }
}
~~~

---

## Phase 3. UserProfile - all form data

**File:** `src/mealprep/starter/model/UserProfile.java`

This class holds age, gender, height, weight, activity, goal, mood, allergies, dislikes, diet rules, and the courses to include.

Allergies and dislikes are plain `String[]` arrays. The GUI builds them from two rows of checkboxes.

The enums carry display text and multipliers, so the GUI can show `Light (exercise 1-3 days/week)` while the calculator uses `1.375`.

There is no `allowRecentRepeats` field because the Starter Edition has no history.

Concepts: enums with fields, arrays, getters and setters, validation with `if`.

Complete code:

~~~java
package mealprep.starter.model;

/**
 * Everything the user enters on the single input page.
 *
 * The Starter Edition stores allergies and dislikes as String arrays.
 * The GUI fills them from multi-select checkboxes.
 */
public class UserProfile {

    private int age;
    private Gender gender;
    private double heightCm;
    private double weightKg;
    private ActivityLevel activityLevel;
    private Goal goal;
    private DailyPreference dailyPreference = DailyPreference.ANY;

    private String[] allergies = new String[0];
    private String[] dislikes = new String[0];
    private boolean vegetarian;
    private boolean halal;

    private boolean includeBreakfast = true;
    private boolean includeLunch = true;
    private boolean includeDinner = true;
    private boolean includeSnack = true;

    public enum Gender {
        MALE, FEMALE
    }

    public enum ActivityLevel {
        SEDENTARY(1.2, "Sedentary (little/no exercise)"),
        LIGHT(1.375, "Light (exercise 1-3 days/week)"),
        MODERATE(1.55, "Moderate (exercise 3-5 days/week)"),
        ACTIVE(1.725, "Active (exercise 6-7 days/week)"),
        VERY_ACTIVE(1.9, "Very Active (physical job/2x training)");

        private final double multiplier;
        private final String description;

        ActivityLevel(double multiplier, String description) {
            this.multiplier = multiplier;
            this.description = description;
        }

        public double getMultiplier() { return multiplier; }
        public String getDescription() { return description; }

        @Override
        public String toString() { return description; }
    }

    public enum Goal {
        LOSE(-500, "Lose Weight (-0.5 kg/week)"),
        MAINTAIN(0, "Maintain Weight"),
        GAIN(500, "Gain Weight (+0.5 kg/week)");

        private final int calorieAdjustment;
        private final String description;

        Goal(int calorieAdjustment, String description) {
            this.calorieAdjustment = calorieAdjustment;
            this.description = description;
        }

        public int getCalorieAdjustment() { return calorieAdjustment; }
        public String getDescription() { return description; }

        @Override
        public String toString() { return description; }
    }

    public enum DailyPreference {
        ANY("Any / Surprise Me", "any"),
        CHICKEN("Chicken / Poultry", "chicken"),
        PORK("Pork", "pork"),
        BEEF("Beef", "beef"),
        FISH("Fish / Seafood", "fish"),
        VEGETABLES("Vegetables / Vegetarian", "vegetables"),
        SOUP("Soup / Broth-based", "soup"),
        NOODLES("Noodles / Pasta", "noodles"),
        RICE_MEALS("Rice Meals / Silog", "rice"),
        GRILLED("Grilled / Barbecue", "grilled");

        private final String displayName;
        private final String tag;

        DailyPreference(String displayName, String tag) {
            this.displayName = displayName;
            this.tag = tag;
        }

        public String getDisplayName() { return displayName; }
        public String getTag() { return tag; }

        @Override
        public String toString() { return displayName; }
    }

    public int getAge() { return age; }
    public Gender getGender() { return gender; }
    public double getHeightCm() { return heightCm; }
    public double getWeightKg() { return weightKg; }
    public ActivityLevel getActivityLevel() { return activityLevel; }
    public Goal getGoal() { return goal; }
    public DailyPreference getDailyPreference() { return dailyPreference; }
    public String[] getAllergies() { return allergies; }
    public String[] getDislikes() { return dislikes; }
    public boolean isVegetarian() { return vegetarian; }
    public boolean isHalal() { return halal; }

    public boolean isIncludeBreakfast() { return includeBreakfast; }
    public boolean isIncludeLunch() { return includeLunch; }
    public boolean isIncludeDinner() { return includeDinner; }
    public boolean isIncludeSnack() { return includeSnack; }

    public void setAge(int age) { this.age = age; }
    public void setGender(Gender gender) { this.gender = gender; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
    public void setActivityLevel(ActivityLevel activityLevel) { this.activityLevel = activityLevel; }
    public void setGoal(Goal goal) { this.goal = goal; }
    public void setDailyPreference(DailyPreference dailyPreference) { this.dailyPreference = dailyPreference; }
    public void setAllergies(String[] allergies) { this.allergies = allergies == null ? new String[0] : allergies; }
    public void setDislikes(String[] dislikes) { this.dislikes = dislikes == null ? new String[0] : dislikes; }
    public void setVegetarian(boolean vegetarian) { this.vegetarian = vegetarian; }
    public void setHalal(boolean halal) { this.halal = halal; }

    public void setIncludeBreakfast(boolean includeBreakfast) { this.includeBreakfast = includeBreakfast; }
    public void setIncludeLunch(boolean includeLunch) { this.includeLunch = includeLunch; }
    public void setIncludeDinner(boolean includeDinner) { this.includeDinner = includeDinner; }
    public void setIncludeSnack(boolean includeSnack) { this.includeSnack = includeSnack; }

    public boolean hasAtLeastOneCourse() {
        return includeBreakfast || includeLunch || includeDinner || includeSnack;
    }

    public boolean isValid() {
        return age > 0 && age < 120
                && heightCm > 50 && heightCm < 250
                && weightKg > 20 && weightKg < 300
                && activityLevel != null
                && goal != null;
    }

    public String getValidationError() {
        if (age <= 0 || age >= 120) return "Age must be between 1 and 119";
        if (heightCm <= 50 || heightCm >= 250) return "Height must be between 50 and 250 cm";
        if (weightKg <= 20 || weightKg >= 300) return "Weight must be between 20 and 300 kg";
        if (activityLevel == null) return "Please select an activity level";
        if (goal == null) return "Please select a goal";
        return null;
    }

    @Override
    public String toString() {
        return String.format("UserProfile[age=%d, %s, %.0fcm, %.1fkg, %s, %s]",
                age, gender, heightCm, weightKg, activityLevel, goal);
    }
}
~~~

---

## Phase 4. CalorieCalculator - BMR, TDEE, target, BMI

**File:** `src/mealprep/starter/algorithm/CalorieCalculator.java`

BMR uses the Mifflin-St Jeor equation. TDEE multiplies BMR by the activity level.

The goal adds or subtracts 500 kcal. A safety floor prevents very low targets, and the result is rounded to the nearest 50.

`getSafetyWarning` returns `null` when the target looks reasonable.

Concepts: static methods, `Math`, `String.format`, constants.

Complete code:

~~~java
package mealprep.starter.algorithm;

import mealprep.starter.model.UserProfile;

/**
 * Calorie formulas.
 *
 * BMR  : Mifflin-St Jeor equation
 * TDEE : BMR x activity multiplier
 * Target: TDEE + goal adjustment, with a safety floor, rounded to 50
 *
 * Everything is static, so the console tests and the GUI call the same code.
 */
public class CalorieCalculator {

    public static double calculateBMR(UserProfile profile) {
        double weight = profile.getWeightKg();
        double height = profile.getHeightCm();
        int age = profile.getAge();

        if (profile.getGender() == UserProfile.Gender.MALE) {
            return 10 * weight + 6.25 * height - 5 * age + 5;
        } else {
            return 10 * weight + 6.25 * height - 5 * age - 161;
        }
    }

    public static double calculateTDEE(UserProfile profile) {
        return calculateBMR(profile) * profile.getActivityLevel().getMultiplier();
    }

    public static int calculateTargetCalories(UserProfile profile) {
        double target = calculateTDEE(profile) + profile.getGoal().getCalorieAdjustment();

        // A school project should not suggest an extremely low intake.
        int floor = profile.getGender() == UserProfile.Gender.MALE ? 1500 : 1200;
        if (target < floor) {
            target = floor;
        }

        return (int) Math.round(target / 50.0) * 50;
    }

    public static double calculateBMI(UserProfile profile) {
        double heightM = profile.getHeightCm() / 100.0;
        double bmi = profile.getWeightKg() / (heightM * heightM);
        return Math.round(bmi * 10.0) / 10.0;
    }

    public static String getBMICategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Overweight";
        return "Obese";
    }

    public static int getProteinTarget(int targetCalories) {
        return (int) Math.round(targetCalories * 0.25 / 4.0);
    }

    public static int getCarbsTarget(int targetCalories) {
        return (int) Math.round(targetCalories * 0.45 / 4.0);
    }

    public static int getFatTarget(int targetCalories) {
        return (int) Math.round(targetCalories * 0.30 / 9.0);
    }

    /**
     * Returns a warning when the target is outside a reasonable range,
     * or null when it looks fine.
     */
    public static String getSafetyWarning(UserProfile profile, int targetCalories) {
        double bmr = calculateBMR(profile);
        double maxSafe = bmr * 2.5;

        if (targetCalories < bmr) {
            return "Target is below BMR. This is not safe without medical supervision.";
        }
        if (targetCalories > maxSafe) {
            return String.format("Target is very high. Reasonable maximum: %.0f kcal.", maxSafe);
        }
        return null;
    }
}
~~~

---

## Phase 5. MealOption - one recommended dish

**File:** `src/mealprep/starter/model/MealOption.java`

A `MealOption` wraps a `FoodItem` and adds the number of servings, a score, a reason, and a selected flag.

There is no side dish in this edition. The only adjustment is the number of servings, scaled between 1.0 and 3.0.

`getCalories`, `getProtein`, `getCarbs`, and `getFat` multiply the food values by the portions.

Concepts: composition, methods, `Math.round`, `String.format`.

Complete code:

~~~java
package mealprep.starter.model;

/**
 * One recommended meal option.
 *
 * Unlike the full edition, a Starter option is always one dish.
 * The only adjustment is the number of servings, which is scaled to fit
 * the course calorie budget.
 */
public class MealOption {

    private final FoodItem foodItem;
    private double portions = 1.0;
    private double score;
    private boolean selected;
    private String reason;

    public MealOption(FoodItem foodItem, double score, String reason) {
        this.foodItem = foodItem;
        this.score = score;
        this.reason = reason;
    }

    public void setPortions(double portions) {
        if (portions < 1.0) portions = 1.0;
        if (portions > 3.0) portions = 3.0;
        this.portions = portions;
    }

    public double getPortions() { return portions; }

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

    public String getName() {
        if (portions == 1.0) {
            return foodItem.getName();
        }
        return foodItem.getName() + " x" + formatPortion(portions);
    }

    public String getServingSize() {
        if (portions == 1.0) {
            return foodItem.getServingSize();
        }
        return foodItem.getServingSize() + " x" + formatPortion(portions);
    }

    public static String formatPortion(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }

    public FoodItem getFoodItem() { return foodItem; }
    public double getScore() { return score; }
    public boolean isSelected() { return selected; }
    public String getReason() { return reason; }

    public void setSelected(boolean selected) { this.selected = selected; }
    public void setScore(double score) { this.score = score; }
    public void setReason(String reason) { this.reason = reason; }

    public FoodItem.MealType getMealType() {
        return foodItem.getMealType();
    }

    @Override
    public String toString() {
        return String.format("%s (%d kcal) - %s", getName(), getCalories(), reason);
    }
}
~~~

---

## Phase 6. MealPlan - budgets, skipped courses, totals

**File:** `src/mealprep/starter/model/MealPlan.java`

`MealPlan` stores the target, the four option lists, the four enabled flags, and the user profile.

`getBudget` is the important method. If a course is skipped, its percentage is removed from the denominator and the remaining courses share the target.

Example with target 1750 and breakfast skipped: breakfast 0, lunch 817, dinner 700, snacks 233.

`getSelectedOption` finds the user pick. `getSelectedCalories` and the macro methods add up only the enabled courses.

Concepts: arrays indexed by `enum.ordinal()`, loops, percentage normalization.

Complete code:

~~~java
package mealprep.starter.model;

import java.util.ArrayList;

/**
 * The four course budgets and the three options offered for each course.
 *
 * If a course is skipped, its percentage is removed from the total and the
 * remaining courses share the target calories. Example with target 1750:
 *   all courses      -> breakfast 438, lunch 613, dinner 525, snacks 175
 *   breakfast skipped -> breakfast 0, lunch 817, dinner 700, snacks 233
 */
public class MealPlan {

    public static final double BREAKFAST_PCT = 0.25;
    public static final double LUNCH_PCT = 0.35;
    public static final double DINNER_PCT = 0.30;
    public static final double SNACK_PCT = 0.10;

    private int targetCalories;
    private UserProfile userProfile;

    private ArrayList<MealOption> breakfastOptions = new ArrayList<>();
    private ArrayList<MealOption> lunchOptions = new ArrayList<>();
    private ArrayList<MealOption> dinnerOptions = new ArrayList<>();
    private ArrayList<MealOption> snackOptions = new ArrayList<>();

    private boolean[] enabled = {true, true, true, true};

    public MealPlan(UserProfile userProfile, int targetCalories) {
        this.userProfile = userProfile;
        this.targetCalories = targetCalories;
    }

    public static String getSlotName(FoodItem.MealType type) {
        switch (type) {
            case BREAKFAST: return "Breakfast";
            case LUNCH:     return "Lunch";
            case DINNER:    return "Dinner";
            case SNACK:     return "Snacks";
            default:        return "Meal";
        }
    }

    public static double getBasePercentage(FoodItem.MealType type) {
        switch (type) {
            case BREAKFAST: return BREAKFAST_PCT;
            case LUNCH:     return LUNCH_PCT;
            case DINNER:    return DINNER_PCT;
            case SNACK:     return SNACK_PCT;
            default:        return 0;
        }
    }

    public boolean isSlotEnabled(FoodItem.MealType type) {
        return enabled[type.ordinal()];
    }

    public void setSlotEnabled(FoodItem.MealType type, boolean value) {
        enabled[type.ordinal()] = value;
    }

    public int getEnabledSlotCount() {
        int count = 0;
        for (int i = 0; i < enabled.length; i++) {
            if (enabled[i]) count++;
        }
        return count;
    }

    /**
     * Budget for one course after skipped courses are redistributed.
     * Returns 0 when the course is skipped.
     */
    public int getBudget(FoodItem.MealType type) {
        if (!isSlotEnabled(type)) return 0;

        double enabledPercentage = 0;
        for (FoodItem.MealType t : FoodItem.MealType.values()) {
            if (isSlotEnabled(t)) {
                enabledPercentage += getBasePercentage(t);
            }
        }
        if (enabledPercentage <= 0) return 0;

        return (int) Math.round(targetCalories * getBasePercentage(type) / enabledPercentage);
    }

    public int getBreakfastBudget() { return getBudget(FoodItem.MealType.BREAKFAST); }
    public int getLunchBudget()     { return getBudget(FoodItem.MealType.LUNCH); }
    public int getDinnerBudget()    { return getBudget(FoodItem.MealType.DINNER); }
    public int getSnackBudget()     { return getBudget(FoodItem.MealType.SNACK); }

    public ArrayList<MealOption> getBreakfastOptions() { return breakfastOptions; }
    public ArrayList<MealOption> getLunchOptions()     { return lunchOptions; }
    public ArrayList<MealOption> getDinnerOptions()    { return dinnerOptions; }
    public ArrayList<MealOption> getSnackOptions()     { return snackOptions; }

    public ArrayList<MealOption> getOptions(FoodItem.MealType type) {
        switch (type) {
            case BREAKFAST: return breakfastOptions;
            case LUNCH:     return lunchOptions;
            case DINNER:    return dinnerOptions;
            case SNACK:     return snackOptions;
            default:        return new ArrayList<>();
        }
    }

    public void setOptions(FoodItem.MealType type, ArrayList<MealOption> options) {
        switch (type) {
            case BREAKFAST: breakfastOptions = options; break;
            case LUNCH:     lunchOptions = options; break;
            case DINNER:    dinnerOptions = options; break;
            case SNACK:     snackOptions = options; break;
        }
    }

    /** The option the user picked for one course, or null. */
    public MealOption getSelectedOption(FoodItem.MealType type) {
        for (MealOption option : getOptions(type)) {
            if (option.isSelected()) {
                return option;
            }
        }
        return null;
    }

    public boolean isComplete() {
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (isSlotEnabled(type) && getOptions(type).isEmpty()) {
                return false;
            }
        }
        return getEnabledSlotCount() > 0;
    }

    public int getTargetCalories() { return targetCalories; }
    public UserProfile getUserProfile() { return userProfile; }

    public void setTargetCalories(int targetCalories) { this.targetCalories = targetCalories; }

    public int getSelectedCalories() {
        int total = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!isSlotEnabled(type)) continue;
            MealOption picked = getSelectedOption(type);
            if (picked != null) {
                total += picked.getCalories();
            }
        }
        return total;
    }

    public double getSelectedProtein() { return getSelectedMacro("protein"); }
    public double getSelectedCarbs()   { return getSelectedMacro("carbs"); }
    public double getSelectedFat()     { return getSelectedMacro("fat"); }

    private double getSelectedMacro(String macro) {
        double total = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!isSlotEnabled(type)) continue;
            MealOption picked = getSelectedOption(type);
            if (picked == null) continue;

            if (macro.equals("protein")) {
                total += picked.getProtein();
            } else if (macro.equals("carbs")) {
                total += picked.getCarbs();
            } else {
                total += picked.getFat();
            }
        }
        return total;
    }

    public String getCalorieStatus() {
        int selected = getSelectedCalories();
        if (selected == 0) {
            return "No meals selected yet";
        }

        int difference = selected - targetCalories;
        if (Math.abs(difference) <= targetCalories * 0.10) {
            return "On target (+/-10%)";
        }
        if (difference > 0) {
            return "+" + difference + " kcal over target";
        }
        return (-difference) + " kcal under target";
    }

    @Override
    public String toString() {
        return String.format("MealPlan[target=%d kcal, selected=%d kcal, enabled=%d/4]",
                targetCalories, getSelectedCalories(), getEnabledSlotCount());
    }
}
~~~

---

## Phase 7. MealRecommender - filter, score, pick 3

**File:** `src/mealprep/starter/algorithm/MealRecommender.java`

This is the heart of the Starter Edition. For one course it filters candidates, scores them, sorts them, and takes up to three.

Hard filters: allergies, dislikes, vegetarian, and halal. Related tags are added, so a dairy allergy also removes `milk`, `cheese`, `cream`, and `butter`.

Score: 2 points for a close calorie fit, 1 point for a looser fit, 1.5 points for today's mood, and small bonuses for popular or vegetarian dishes.

The sort uses an anonymous `Comparator` class. The first pass prefers different main ingredients; the second pass fills to three.

Portions are scaled to the course budget. The reason text is rebuilt so it matches the final servings.

There is no history, no repair step, and no side dish in this edition.

Concepts: `ArrayList`, `Comparator`, loops, scoring, nested classes.

Complete code:

~~~java
package mealprep.starter.algorithm;

import mealprep.starter.database.FoodDatabase;
import mealprep.starter.model.FoodItem;
import mealprep.starter.model.MealOption;
import mealprep.starter.model.MealPlan;
import mealprep.starter.model.UserProfile;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Chooses three meal options for each course.
 *
 * Steps for one course:
 *   1. Remove foods that match an allergy, a dislike, or a diet rule.
 *   2. Score the remaining foods by calorie fit and today's mood.
 *   3. Sort the scores from highest to lowest.
 *   4. Take the best three, preferring different main ingredients.
 *   5. Scale the servings so the option fits the course budget.
 *
 * There is no history, no repair step, and no side dish in this edition.
 */
public class MealRecommender {

    private static final int OPTIONS_PER_SLOT = 3;
    //num of options to choose from for every course
    private static final double CALORIE_TOLERANCE = 0.15;
    //15% tolerance for exceeding or short calories. for ex target: 600, it tolerates 510 and 690 since 15% od 600 is 90

    private final FoodDatabase foodDatabase;

    public MealRecommender(FoodDatabase foodDatabase) {
        this.foodDatabase = foodDatabase;
    }

    public MealPlan generatePlan(UserProfile profile, int targetCalories) {
        MealPlan plan = new MealPlan(profile, targetCalories);

        plan.setSlotEnabled(FoodItem.MealType.BREAKFAST, profile.isIncludeBreakfast()); //checks if user wants breakfast(same as lunch, dinner, and snacks)
        plan.setSlotEnabled(FoodItem.MealType.LUNCH, profile.isIncludeLunch());
        plan.setSlotEnabled(FoodItem.MealType.DINNER, profile.isIncludeDinner());
        plan.setSlotEnabled(FoodItem.MealType.SNACK, profile.isIncludeSnack());

        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) {
                plan.setOptions(type, new ArrayList<>());
            } else {
                plan.setOptions(type, selectOptions(type, plan.getBudget(type), profile));
            }
        } //checking enabled courses. a skipped course receive no options
        return plan; //returns complete plan for each enabled courses
    } //this sppecific block of code (MealPlan generatePlan(UserProfile profile, int targetCalories)), acts as a blueprint of things we can use later such as accessing user profile, target calories, courses, options for each course. these data will be used later to evaluate what meals are elligible .
 
    /** Rebuild every enabled course. Used after the user skips a course. */
    public void regenerateEnabledSlots(MealPlan plan) {
        UserProfile profile = plan.getUserProfile();

        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) {
                plan.setOptions(type, new ArrayList<>());
            } else {
                plan.setOptions(type, selectOptions(type, plan.getBudget(type), profile));
            }
        }
    } //this block of code rebuilds options for every enabled course if the user skipped a course. this is used if a user skipped a meal after the plan was already made. this is important for the calorie target distribution.

    public ArrayList<MealOption> selectOptions(FoodItem.MealType type, int budget,
                                               UserProfile profile) {
        ArrayList<FoodItem> candidates = getCandidates(type, profile);
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        } //storage for all elligible foods from profile

        ArrayList<ScoredFood> scored = new ArrayList<>();
        for (FoodItem food : candidates) {
            double score = scoreFood(food, budget, profile);
            String reason = buildReason(food, budget, profile);
            scored.add(new ScoredFood(food, score, reason));
        } // stores data for scored foods

        scored.sort(new Comparator<ScoredFood>() {
            public int compare(ScoredFood a, ScoredFood b) {
                return Double.compare(b.score, a.score);
            } // this sorts the foods by score
        });

        ArrayList<MealOption> options = new ArrayList<>();
        ArrayList<String> usedMainTags = new ArrayList<>();

        // First pass: one dish per main ingredient (pork, chicken, fish, ...).
        for (ScoredFood candidate : scored) {
            if (options.size() >= OPTIONS_PER_SLOT) break;

            String mainTag = getMainProteinTag(candidate.food);
            if (!usedMainTags.contains(mainTag)) {
                options.add(new MealOption(candidate.food, candidate.score, candidate.reason));
                usedMainTags.add(mainTag);
            }
        } //prevents having multiple main protein provider

        // Second pass: fill up to three if there were not enough different tags.
        for (ScoredFood candidate : scored) {
            if (options.size() >= OPTIONS_PER_SLOT) break;
            if (!containsFood(options, candidate.food.getId())) {
                options.add(new MealOption(candidate.food, candidate.score, candidate.reason));
            }
        }// fill remaining spaces

        for (MealOption option : options) {
            fitOptionToBudget(option, budget);
        }
        return options; //this controls the portions of calories  for every option
    }

    // ---------------------------------------------------------------
    // Filtering
    // ---------------------------------------------------------------

    private ArrayList<FoodItem> getCandidates(FoodItem.MealType type, UserProfile profile) {
        ArrayList<String> excludeTags = buildExclusionTags(profile);
        ArrayList<FoodItem> result = new ArrayList<>();

        for (FoodItem food : foodDatabase.getFoodsByMealType(type)) {
            boolean excluded = false;
            for (String tag : excludeTags) {
                if (food.hasTag(tag)) {
                    excluded = true;
                    break; //excludes invalid foods by checking if it has been tagged
                }
            }
            if (!excluded) {
                result.add(food);
            } //add valid foods
        }
        return result;
    } // this method retrieves and fikters foods 

    private ArrayList<String> buildExclusionTags(UserProfile profile) {
        ArrayList<String> tags = new ArrayList<>();

        for (String allergy : profile.getAllergies()) {
            String value = allergy.toLowerCase();
            addTag(tags, value);
            addRelatedAllergyTags(tags, value);
        }

        for (String dislike : profile.getDislikes()) {
            String value = dislike.toLowerCase();
            addTag(tags, value);
            addRelatedDislikeTags(tags, value);
        }

        if (profile.isVegetarian()) {
            addTag(tags, "pork");
            addTag(tags, "ground-pork");
            addTag(tags, "beef");
            addTag(tags, "oxtail");
            addTag(tags, "chicken");
            addTag(tags, "duck");
            addTag(tags, "fish");
            addTag(tags, "milkfish");
            addTag(tags, "tilapia");
            addTag(tags, "salmon");
            addTag(tags, "seafood");
            addTag(tags, "shrimp");
            addTag(tags, "crab");
            addTag(tags, "squid");
            addTag(tags, "meat");
        }

        if (profile.isHalal()) {
            addTag(tags, "pork");
            addTag(tags, "ground-pork");
        }

        return tags;
    }

    private void addTag(ArrayList<String> tags, String tag) {
        String value = tag.trim().toLowerCase();
        if (!value.isEmpty() && !tags.contains(value)) {
            tags.add(value);
        }
    }

    private void addRelatedAllergyTags(ArrayList<String> tags, String allergy) {
        switch (allergy) {
            case "peanuts":
            case "peanut":
                addTag(tags, "peanut");
                addTag(tags, "nut");
                break;
            case "shellfish":
                addTag(tags, "shrimp");
                addTag(tags, "crab");
                addTag(tags, "squid");
                addTag(tags, "shellfish");
                addTag(tags, "seafood");
                break;
            case "dairy":
                addTag(tags, "milk");
                addTag(tags, "cheese");
                addTag(tags, "cream");
                addTag(tags, "butter");
                break;
            case "gluten":
                addTag(tags, "wheat");
                addTag(tags, "flour");
                addTag(tags, "bread");
                addTag(tags, "noodles");
                addTag(tags, "pasta");
                addTag(tags, "pastry");
                break;
            case "soy":
                addTag(tags, "soy");
                addTag(tags, "soya");
                addTag(tags, "tofu");
                break;
            case "eggs":
            case "egg":
                addTag(tags, "egg");
                addTag(tags, "salted-egg");
                break;
        }
    }

    private void addRelatedDislikeTags(ArrayList<String> tags, String dislike) {
        switch (dislike) {
            case "fish":
                addTag(tags, "fish");
                addTag(tags, "milkfish");
                addTag(tags, "tilapia");
                addTag(tags, "salmon");
                break;
            case "pork":
                addTag(tags, "pork");
                addTag(tags, "ground-pork");
                break;
            case "beef":
                addTag(tags, "beef");
                addTag(tags, "oxtail");
                break;
            case "fried":
                addTag(tags, "fried");
                addTag(tags, "pan-fried");
                break;
            case "bitter":
                addTag(tags, "bitter-melon");
                break;
            case "organ meat":
            case "organ":
                addTag(tags, "offal");
                addTag(tags, "liver");
                break;
        }
    }

    // ---------------------------------------------------------------
    // Scoring
    // ---------------------------------------------------------------

    private double scoreFood(FoodItem food, int budget, UserProfile profile) {
        double score = 0;

        int difference = Math.abs(food.getCalories() - budget);
        if (difference <= budget * CALORIE_TOLERANCE) {
            score += 2.0;
        } else if (difference <= budget * 0.30) {
            score += 1.0;
        } //use calorie tolerance to score food if within 15%, receive 2.0, if within 30, receive 1.5, 0 if neither

        if (profile.getDailyPreference() != UserProfile.DailyPreference.ANY
                && matchesMood(food, profile.getDailyPreference())) {
            score += 1.5;
        } //checks if the food matches user daily preference, if it does, then add 1.5 to the food

        if (food.hasTag("popular")) {
            score += 0.3;
        }
        if (profile.isVegetarian() && food.hasTag("vegetarian")) {
            score += 0.5;
        }
        if (profile.isHalal() && !food.hasTag("pork")) {
            score += 0.2;
        } //basically add score depende sa preference

        return score;
    }

    private boolean matchesMood(FoodItem food, UserProfile.DailyPreference preference) {
        switch (preference) {
            case ANY:
                return true;
            case CHICKEN:
                return food.hasTag("chicken") || food.hasTag("duck");
            case PORK:
                return food.hasTag("pork") || food.hasTag("ground-pork");
            case BEEF:
                return food.hasTag("beef") || food.hasTag("oxtail");
            case FISH:
                return food.hasTag("fish") || food.hasTag("milkfish")
                        || food.hasTag("tilapia") || food.hasTag("salmon")
                        || food.hasTag("shrimp") || food.hasTag("crab")
                        || food.hasTag("squid");
            case VEGETABLES:
                return food.hasTag("vegetables") || food.hasTag("vegetarian");
            case SOUP:
                return food.hasTag("soup") || food.hasTag("porridge");
            case NOODLES:
                return food.hasTag("noodles") || food.hasTag("rice-noodles");
            case RICE_MEALS:
                return food.hasTag("rice");
            case GRILLED:
                return food.hasTag("grilled") || food.hasTag("bbq");
            default:
                return food.hasTag(preference.getTag());
        }
    }  //This method determines whether a food matches the user’s selected daily mood. if gusto nila for example chicken

    private String buildReason(FoodItem food, int budget, UserProfile profile) {
        StringBuilder reason = new StringBuilder();
        int difference = food.getCalories() - budget;

        if (Math.abs(difference) <= budget * CALORIE_TOLERANCE) {
            reason.append("Good calorie fit");
        } else if (difference < 0) {
            reason.append("Light option (").append(-difference).append(" kcal under)");
        } else {
            reason.append("Hearty option (").append(difference).append(" kcal over)");
        }

        if (profile.getDailyPreference() != UserProfile.DailyPreference.ANY
                && matchesMood(food, profile.getDailyPreference())) {
            reason.append(" | Matches mood");
        }

        if (food.hasTag("popular")) {
            reason.append(" | Popular choice");
        }

        return reason.toString();
    } //This creates the explanation shown to the user.

    private String getMainProteinTag(FoodItem food) {
        if (food.hasTag("pork") || food.hasTag("ground-pork")) return "pork";
        if (food.hasTag("beef") || food.hasTag("oxtail")) return "beef";
        if (food.hasTag("chicken") || food.hasTag("duck")) return "chicken";
        if (food.hasTag("fish") || food.hasTag("milkfish")
                || food.hasTag("tilapia") || food.hasTag("salmon")) return "fish";
        if (food.hasTag("shrimp") || food.hasTag("crab") || food.hasTag("squid")) return "seafood";
        if (food.hasTag("egg") || food.hasTag("salted-egg")) return "egg";
        if (food.hasTag("tofu") || food.hasTag("soy")) return "tofu";
        if (food.hasTag("beans")) return "beans";
        return "other";
    } //assigns each food to main-protein category. This data will be used to prevent more than one main-protein food provider

    // ---------------------------------------------------------------
    // Portion fitting
    // ---------------------------------------------------------------

    private void fitOptionToBudget(MealOption option, int budget) {
        FoodItem food = option.getFoodItem();
        double rawPortions = (double) budget / Math.max(1, food.getCalories()); //calculates  how many original servings would reach the budget.
        double portions = Math.round(rawPortions * 2.0) / 2.0;

        if (portions < 1.0) portions = 1.0;
        if (portions > 3.0) portions = 3.0;
        //The user receives between one and three servings. This limits the portions

        option.setPortions(portions); // then calculates calories and macros using the new portions.

        if (portions != 1.0) {
            // The first part of the old reason was based on one serving.
            // Replace it with a phrase that matches the scaled portion.
            String kept = stripCalorieFitReason(option.getReason()); //keeps  the reason from before eg Matches moof, popular choice,etc
            int finalDifference = option.getCalories() - budget;
            String fitReason; //add reasons such as good portion fiit showm below

            if (Math.abs(finalDifference) <= budget * CALORIE_TOLERANCE) {
                fitReason = "Good portion fit";
            } else if (finalDifference < 0) {
                fitReason = "Light portion (" + (-finalDifference) + " kcal under)";
            } else {
                fitReason = "Hearty portion (" + finalDifference + " kcal over)";
            }

            String reason = fitReason;
            if (!kept.isEmpty()) {
                reason = reason + " | " + kept;
            }
            option.setReason(reason + " | x" + MealOption.formatPortion(portions)
                    + " serving");
        }
    }

    /** Drops the one-serving calorie phrase, keeping mood/popularity notes. */
    private String stripCalorieFitReason(String reason) {
        int separator = reason.indexOf(" | ");
        if (separator >= 0) {
            return reason.substring(separator + 3);
        }
        return "";
    } //This removes the first calorie-related phrase while keeping later explanations.

    private boolean containsFood(ArrayList<MealOption> options, int foodId) {
        for (MealOption option : options) {
            if (option.getFoodItem().getId() == foodId) {
                return true;
            }
        }
        return false;
    } // this changes food into id so that it can oonly be used once

    /** Small helper that holds one food and its score before sorting. */
    private static class ScoredFood {
        FoodItem food;
        double score;
        String reason;

        ScoredFood(FoodItem food, double score, String reason) {
            this.food = food;
            this.score = score;
            this.reason = reason;
        }
    }
}
~~~

---

## Phase 8. MealPlanGenerator - put the pieces together

**File:** `src/mealprep/starter/algorithm/MealPlanGenerator.java`

The generator creates the plan, asks the recommender for options, and selects the first option in each enabled course.

When the user skips a course, `regenerateEnabledSlots` rebuilds the remaining courses with their new budgets.

`describePlan` prints a plain-text plan for the console app.

Concepts: composition, delegation, methods that return objects.

Complete code:

~~~java
package mealprep.starter.algorithm;

import mealprep.starter.database.FoodDatabase;
import mealprep.starter.model.FoodItem;
import mealprep.starter.model.MealOption;
import mealprep.starter.model.MealPlan;
import mealprep.starter.model.UserProfile;

import java.util.ArrayList;

/**
 * Puts the calculator and the recommender together.
 *
 * The generator is small on purpose: it creates the plan, selects the first
 * option in each course, and can rebuild the plan when a course is skipped.
 */
public class MealPlanGenerator {

    private final FoodDatabase foodDatabase;
    private final MealRecommender recommender;

    public MealPlanGenerator(FoodDatabase foodDatabase) {
        this.foodDatabase = foodDatabase;
        this.recommender = new MealRecommender(foodDatabase);
    }

    public int getTargetCalories(UserProfile profile) {
        return CalorieCalculator.calculateTargetCalories(profile);
    }

    public MealPlan generatePlan(UserProfile profile) {
        int target = CalorieCalculator.calculateTargetCalories(profile);
        MealPlan plan = recommender.generatePlan(profile, target);
        autoSelectFirstOptions(plan);
        return plan;
    }

    /** Rebuild the enabled courses after a skip or unskip. */
    public void regenerateEnabledSlots(MealPlan plan) {
        recommender.regenerateEnabledSlots(plan);
        autoSelectFirstOptions(plan);
    }

    private void autoSelectFirstOptions(MealPlan plan) {
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            ArrayList<MealOption> options = plan.getOptions(type);
            for (MealOption option : options) {
                option.setSelected(false);
            }
            if (plan.isSlotEnabled(type) && !options.isEmpty()) {
                options.get(0).setSelected(true);
            }
        }
    }

    /** Plain-text plan used by the console app and the self-tests. */
    public String describePlan(MealPlan plan) {
        StringBuilder text = new StringBuilder();
        text.append("Target: ").append(plan.getTargetCalories()).append(" kcal/day\n\n");

        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            String name = MealPlan.getSlotName(type).toUpperCase();

            if (!plan.isSlotEnabled(type)) {
                text.append("--- ").append(name).append(": SKIPPED ---\n\n");
                continue;
            }

            text.append("--- ").append(name)
                    .append(" (Budget: ").append(plan.getBudget(type))
                    .append(" kcal) ---\n");

            ArrayList<MealOption> options = plan.getOptions(type);
            if (options.isEmpty()) {
                text.append("  No options available (check the filters)\n");
            } else {
                for (int i = 0; i < options.size(); i++) {
                    MealOption option = options.get(i);
                    text.append(option.isSelected() ? ">> " : "   ");
                    text.append(i + 1).append(". ").append(option.getName())
                            .append(" (").append(option.getCalories()).append(" kcal)\n");
                    text.append("      ").append(option.getReason()).append("\n");
                }
            }
            text.append("\n");
        }

        text.append("Selected total: ").append(plan.getSelectedCalories()).append(" kcal\n");
        text.append("Status: ").append(plan.getCalorieStatus()).append("\n");
        return text.toString();
    }

    public FoodDatabase getFoodDatabase() { return foodDatabase; }
    public MealRecommender getMealRecommender() { return recommender; }
}
~~~

---

## Phase 9. StarterApp - console entry point and 26 tests

**File:** `src/mealprep/starter/StarterApp.java`

Run this first. It loads the CSV, checks the formulas, checks the filters, checks the skip budgets, and prints two demo plans.

Do not start the GUI until this prints `SELF-TESTS: 26 passed, 0 failed`.

The tests cover exact tag matching, three options per course, dairy and vegetarian filters, pork dislike, and all 15 course combinations.

Concepts: `main`, static helper methods, arrays, loops, formatted output.

Complete code:

~~~java
package mealprep.starter;

import mealprep.starter.algorithm.CalorieCalculator;
import mealprep.starter.algorithm.MealPlanGenerator;
import mealprep.starter.database.FoodDatabase;
import mealprep.starter.model.FoodItem;
import mealprep.starter.model.MealOption;
import mealprep.starter.model.MealPlan;
import mealprep.starter.model.UserProfile;

/**
 * Console entry point and self-tests for the Starter Edition.
 *
 * Run this before the GUI:
 *   java -cp out mealprep.starter.StarterApp
 */
public class StarterApp { //this phase runs the program first and it acts as a checker if any there are problems in database, calculator, etc. before proceeding with the GUI

    private static int passed = 0;
    private static int failed = 0;
    //this declarations store number of succesful and failed test

    public static void main(String[] args) {
        System.out.println("=======================================");
        System.out.println("     MEALPREP STARTER - SELF-TESTS");
        System.out.println("=======================================\n");

        FoodDatabase foodDatabase = new FoodDatabase();
        if (!foodDatabase.loadFromCSV("data/philippine_foods.csv")) {
            System.err.println("Failed to load data/philippine_foods.csv");
            System.err.println("Run this program from the starter folder.");
            return;
        } //this if statement checcks wether the database is loading.

        //The return statement is important. It prevents the application from running tests when the required food data is missing. If the database is working then proceed
        System.out.println(foodDatabase + "\n");

        runSelfTests(foodDatabase); //now this runs the 26 tests

        MealPlanGenerator generator = new MealPlanGenerator(foodDatabase);

        System.out.println("\n--- DEMO: THREE OPTIONS PER COURSE ---");
        UserProfile user = createTestUser();
        MealPlan plan = generator.generatePlan(user);
        System.out.println(generator.describePlan(plan));
        //This creates a standard test user, generates a complete meal plan, and prints it. only a demo

        System.out.println("--- DEMO: SKIP BREAKFAST ---");
        UserProfile skipUser = createTestUser();
        skipUser.setIncludeBreakfast(false);
        MealPlan skipPlan = generator.generatePlan(skipUser);
        System.out.println("Target: " + skipPlan.getTargetCalories() + " kcal");
        System.out.println("Breakfast budget: " + skipPlan.getBreakfastBudget() + " kcal (skipped)");
        System.out.println("Lunch budget:     " + skipPlan.getLunchBudget() + " kcal");
        System.out.println("Dinner budget:    " + skipPlan.getDinnerBudget() + " kcal");
        System.out.println("Snacks budget:    " + skipPlan.getSnackBudget() + " kcal");
        System.out.println("Selected total:   " + skipPlan.getSelectedCalories() + " kcal\n");
        System.out.println(generator.describePlan(skipPlan));

        //this demo is a demo user who skip courses

        System.out.println("=======================================");
        System.out.println("SELF-TESTS: " + passed + " passed, " + failed + " failed");
        System.out.println("=======================================");
        //this prints the counted self test results the passed and failed. below are the basis of the score
    }

    private static void runSelfTests(FoodDatabase foodDatabase) {
        System.out.println("--- SELF-TESTS ---");

        UserProfile user = createTestUser();

        // 1. Database 
        //This checks two database requirements:
        //The database contains at least 50 foods.
        //Every meal type contains at least one food.
        check("database has at least 50 foods", foodDatabase.size() >= 50);
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            check(type + " has foods", foodDatabase.size(type) > 0);
        }

        // 2. Calorie formulas
        //this use a demo user which tests the calculations for calorie plan. Basically checking the CalorieCalculatpr
        check("female BMR = 1279",
                Math.abs(CalorieCalculator.calculateBMR(user) - 1279) < 0.01);
        check("target is rounded to 50",
                CalorieCalculator.calculateTargetCalories(user) % 50 == 0);
        check("BMI = 21.5",
                Math.abs(CalorieCalculator.calculateBMI(user) - 21.5) < 0.01);

        // 3. Skip-aware budgets
        //this naman uses the dummy to check if the budget calculator works if a user skips a meal
        MealPlan budgetPlan = new MealPlan(user, 2000);
        check("all-course budgets sum to 2000", budgetSum(budgetPlan) == 2000);

        budgetPlan.setSlotEnabled(FoodItem.MealType.BREAKFAST, false);
        check("skipped-course budgets still sum to 2000", budgetSum(budgetPlan) == 2000);
        check("skipped breakfast budget is 0", budgetPlan.getBreakfastBudget() == 0);
        check("lunch budget grows after skipping breakfast", budgetPlan.getLunchBudget() > 700);

        // 4. Exact tag matching
        //the purpose of this is to checck wether FoodItem.hasTag() is working or yeah like it uses exact matching like for example tag should exactly be milkfish and will not be tagged if system scans through "milk" only
        FoodItem bangus = foodDatabase.getFoodById(3);
        check("bangus has the milkfish tag", bangus != null && bangus.hasTag("milkfish"));
        check("dairy 'milk' does not match 'milkfish'",
                bangus != null && !bangus.hasTag("milk"));

        // 5. Three options per course
        //this naman checks output requirements
        //No course has more than three options.
        //Every enabled course has at least one option.
        //The selected total is greater than zero.
        //Every enabled course has one automatically selected meal.
        //Portions stay between 1.0 and 3.0.
        MealPlanGenerator generator = new MealPlanGenerator(foodDatabase);
        MealPlan normal = generator.generatePlan(createTestUser());

        check("at most 3 options per course", maxOptions(normal) <= 3);
        check("at least 1 option per enabled course", minOptions(normal) >= 1);
        check("selected total is above zero", normal.getSelectedCalories() > 0);
        check("every enabled course has a selected meal", everyEnabledCourseHasPick(normal));
        check("portions stay between 1.0 and 3.0", portionsInRange(normal));

        // 6. Filters
        //this test is crucial for checking unsuitable foods.
        UserProfile dairy = createTestUser();
        dairy.setAllergies(new String[]{"dairy"});
        MealPlan dairyPlan = generator.generatePlan(dairy);
        check("dairy allergy removes milk, cheese, cream and butter",
                noTags(dairyPlan, new String[]{"milk", "cheese", "cream", "butter"}));
        //checking for allergies
        

        UserProfile vegetarian = createTestUser();
        vegetarian.setVegetarian(true);
        MealPlan vegetarianPlan = generator.generatePlan(vegetarian);
        check("vegetarian plan contains no meat",
                noTags(vegetarianPlan, new String[]{"pork", "ground-pork", "beef", "oxtail",
                        "chicken", "duck", "fish", "milkfish", "tilapia", "salmon",
                        "seafood", "shrimp", "crab", "squid"}));
        //removing foods unsuitable for vegetarians

        UserProfile noPork = createTestUser();
        noPork.setDislikes(new String[]{"pork"});
        MealPlan noPorkPlan = generator.generatePlan(noPork);
        check("pork dislike removes pork dishes",
                noTags(noPorkPlan, new String[]{"pork", "ground-pork"}));
        //if user doesnt want posk

        // 7. Skipping a course
        UserProfile skipUser = createTestUser();
        skipUser.setIncludeBreakfast(false);
        MealPlan skipPlan = generator.generatePlan(skipUser);

        check("skip breakfast clears breakfast options",
                skipPlan.getBreakfastOptions().isEmpty());
        check("skip breakfast gives budgets 817 / 700 / 233",
                skipPlan.getLunchBudget() == 817
                        && skipPlan.getDinnerBudget() == 700
                        && skipPlan.getSnackBudget() == 233);
        //this section verifies the calculation of calorie target  distribution if the user skips a course

        // 8. Every non-empty combination of courses still gets options.
        boolean allCombinationsOk = true;
        for (int mask = 1; mask < 16; mask++) {
            UserProfile combination = createTestUser();
            combination.setIncludeBreakfast((mask & 1) != 0);
            combination.setIncludeLunch((mask & 2) != 0);
            combination.setIncludeDinner((mask & 4) != 0);
            combination.setIncludeSnack((mask & 8) != 0);

            MealPlan combinationPlan = generator.generatePlan(combination);
            if (countEnabledOptions(combinationPlan) != combinationPlan.getEnabledSlotCount()) {
                allCombinationsOk = false;
                System.out.println("    (combination failed: mask " + mask + ")");
            }
        }
        check("all 15 course combinations produce options", allCombinationsOk);
        check("every option has a reason", everyOptionHasReason(normal));

        System.out.println();
    } //this checks all possible combinations including skipping courses, etc. nstead of manually writing 15 separate test cases, one loop checks every possible combination.

        //It protects against edge-case errors such as:

        //A specific course producing no options
        //A zero budget accidentally being used
        //Disabled courses still appearing
        //Incorrect percentage normalization
        //Recommender failures that only happen for one course combination
        //If a combination fails, the mask is printed so the developer can reproduce it.

    private static UserProfile createTestUser() {
        UserProfile profile = new UserProfile();
        profile.setAge(22);
        profile.setGender(UserProfile.Gender.FEMALE);
        profile.setHeightCm(160);
        profile.setWeightKg(55);
        profile.setActivityLevel(UserProfile.ActivityLevel.LIGHT);
        profile.setGoal(UserProfile.Goal.MAINTAIN);
        profile.setDailyPreference(UserProfile.DailyPreference.ANY);
        profile.setAllergies(new String[]{"peanuts"});
        profile.setDislikes(new String[]{"bitter"});
        profile.setVegetarian(false);
        profile.setHalal(false);
        return profile;
    } //this is a test profile

    private static int budgetSum(MealPlan plan) {
        int sum = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            sum += plan.getBudget(type);
        }
        return sum;
    }

    private static int maxOptions(MealPlan plan) {
        int max = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (plan.getOptions(type).size() > max) {
                max = plan.getOptions(type).size();
            }
        }
        return max;
    }

    private static int minOptions(MealPlan plan) {
        int min = Integer.MAX_VALUE;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) continue;
            int size = plan.getOptions(type).size();
            if (size < min) min = size;
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private static int countEnabledOptions(MealPlan plan) {
        int count = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (plan.isSlotEnabled(type) && !plan.getOptions(type).isEmpty()) {
                count++;
            }
        }
        return count;
    } //These helper methods inspect different properties of a generated MealPlan such as budgetSum, maxOptions, minOptions, countEnabledOptions

    private static boolean everyEnabledCourseHasPick(MealPlan plan) {
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (plan.isSlotEnabled(type) && plan.getSelectedOption(type) == null) {
                return false;
            }
        }
        return true;
    }

    private static boolean portionsInRange(MealPlan plan) {
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            for (MealOption option : plan.getOptions(type)) {
                if (option.getPortions() < 1.0 || option.getPortions() > 3.0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean noTags(MealPlan plan, String[] tags) {
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            for (MealOption option : plan.getOptions(type)) {
                FoodItem food = option.getFoodItem();
                for (String tag : tags) {
                    if (food.hasTag(tag)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean everyOptionHasReason(MealPlan plan) {
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            for (MealOption option : plan.getOptions(type)) {
                if (option.getReason() == null || option.getReason().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    } //These methods test the result from the user’s point of view.

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS: " + label);
        } else {
            failed++;
            System.out.println("  FAIL: " + label);
        }
    }
} //this counts the passed tests and failed tests
~~~

---

## Phase 10. ScrollablePanel - a width-aware panel

**File:** `src/mealprep/starter/gui/ScrollablePanel.java`

A 2x2 grid inside a `JScrollPane` keeps its wide preferred width unless the panel implements `Scrollable`.

`getScrollableTracksViewportWidth` returns `true`, so the grid follows the window width and the right column is not clipped.

Concepts: interface, `implements`, five interface methods, `Dimension`, `Rectangle`.

Complete code:

~~~java
package mealprep.starter.gui;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * A panel that follows the width of its JScrollPane.
 *
 * Without this, a 2x2 grid keeps its wide preferred width and the right
 * column is clipped when the window is small.
 */
public class ScrollablePanel extends JPanel implements Scrollable {

    public ScrollablePanel() {
        super();
    }

    public ScrollablePanel(LayoutManager layout) {
        super(layout);
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 160;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
~~~

---

## Phase 11. StarterGUI - one input page

**File:** `src/mealprep/starter/gui/StarterGUI.java`

The window has three cards: the single input form, the results page, and the final summary page.

The form is a `GridBagLayout` inside a `JScrollPane`. Reset and Generate sit in a fixed bottom bar, so they are always visible.

Dislikes are checkboxes in a 2x5 grid, exactly like allergies. `getProfileFromForm` loops over `dislikeChecks` and adds every ticked value.

Calculator-only is a radio toggle. `updateMealSectionVisibility` hides the food section and changes the button to `Calculate Calories`.

All listeners are anonymous `ActionListener` classes. There are no lambdas.

Concepts: `JFrame`, `JPanel`, `CardLayout`, `GridBagLayout`, `JScrollPane`, `JRadioButton`, `JComboBox`, `JCheckBox`, `ActionListener`.

Complete code:

~~~java
package mealprep.starter.gui;

import mealprep.starter.algorithm.CalorieCalculator;
import mealprep.starter.algorithm.MealPlanGenerator;
import mealprep.starter.database.FoodDatabase;
import mealprep.starter.model.MealPlan;
import mealprep.starter.model.UserProfile;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Starter Edition window.
 *
 * One scrollable input page, one results page with three options per course,
 * and one summary page with the calorie result and the meals the user picked.
 */
public class StarterGUI extends JFrame {

    private FoodDatabase foodDatabase;
    private MealPlanGenerator generator;
    private UserProfile currentProfile;
    private MealPlan currentPlan;

    private static final String CARD_FORM = "form";
    private static final String CARD_RESULTS = "results";
    private static final String CARD_SUMMARY = "summary";

    private JPanel cardPanel;
    private CardLayout cardLayout;
    private JPanel formPanel;
    private JPanel formContent;
    private JPanel mealSection;
    private JPanel resultsCard;
    private JPanel summaryCard;

    private JTextField ageField, heightField, weightField;
    private JComboBox<UserProfile.Gender> genderCombo;
    private JComboBox<UserProfile.ActivityLevel> activityCombo;
    private JComboBox<UserProfile.Goal> goalCombo;
    private JComboBox<UserProfile.DailyPreference> dailyPrefCombo;
    private JCheckBox[] allergyChecks;
    private JCheckBox[] dislikeChecks;
    private JCheckBox vegetarianCheck, halalCheck;
    private JCheckBox includeBreakfastCheck, includeLunchCheck, includeDinnerCheck, includeSnackCheck;
    private JRadioButton calculatorOnlyRadio, fullPlanRadio;
    private JLabel caloriePreviewLabel;
    private JButton generateBtn;

    private boolean calculatorOnlyMode = false;

    public StarterGUI() {
        super("MealPrep Starter - Philippine Meal Planner");
        initializeDatabase();
        initializeUI();
    }

    private void initializeDatabase() {
        foodDatabase = new FoodDatabase();
        boolean loaded = foodDatabase.loadFromCSV("data/philippine_foods.csv");

        if (!loaded) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load the food database.\nCheck data/philippine_foods.csv.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        generator = new MealPlanGenerator(foodDatabase);
        System.out.println("GUI: " + foodDatabase);
    }

    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 780);
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createTitlePanel(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        formPanel = createFormPanel();
        cardPanel.add(formPanel, CARD_FORM);

        resultsCard = createPlaceholderResultsPanel();
        cardPanel.add(resultsCard, CARD_RESULTS);

        summaryCard = new JPanel();
        cardPanel.add(summaryCard, CARD_SUMMARY);

        add(cardPanel, BorderLayout.CENTER);
        updateCaloriePreview();
        showForm();
    }

    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(41, 128, 185));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("MealPrep");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Philippine Calorie Calculator & Meal Planner - Starter Edition");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(new Color(200, 230, 255));

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(title);
        left.add(subtitle);

        JLabel icon = new JLabel("🍚");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 48));

        panel.add(left, BorderLayout.WEST);
        panel.add(icon, BorderLayout.EAST);
        return panel;
    }

    // ---------------------------------------------------------------
    // The single input page
    // ---------------------------------------------------------------

    private JPanel createFormPanel() {
        formContent = new JPanel(new GridBagLayout());
        formContent.setBorder(BorderFactory.createEmptyBorder(12, 24, 18, 24));
        formContent.setBackground(new Color(245, 247, 250));

        GridBagConstraints outer = new GridBagConstraints();
        outer.gridx = 0;
        outer.weightx = 1;
        outer.fill = GridBagConstraints.HORIZONTAL;
        outer.insets = new Insets(0, 0, 7, 0);

        outer.gridy = 0;
        formContent.add(createModeRow(), outer);
        outer.gridy = 1;
        formContent.add(createBodySection(), outer);
        outer.gridy = 2;
        formContent.add(createActivitySection(), outer);
        outer.gridy = 3;
        mealSection = createMealSection();
        formContent.add(mealSection, outer);

        GridBagConstraints spacer = new GridBagConstraints();
        spacer.gridx = 0;
        spacer.gridy = 4;
        spacer.weighty = 1;
        spacer.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        formContent.add(filler, spacer);

        JScrollPane scroll = new JScrollPane(formContent);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel actions = createFormButtons();
        actions.setBackground(new Color(245, 247, 250));
        actions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 222, 232)),
                BorderFactory.createEmptyBorder(10, 24, 12, 24)));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scroll, BorderLayout.CENTER);
        wrapper.add(actions, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel createModeRow() {
        calculatorOnlyRadio = new JRadioButton("Calorie Calculator Only");
        fullPlanRadio = new JRadioButton("Full Meal Plan");
        calculatorOnlyRadio.setMargin(new Insets(0, 0, 0, 8));
        fullPlanRadio.setMargin(new Insets(0, 0, 0, 8));
        fullPlanRadio.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(calculatorOnlyRadio);
        group.add(fullPlanRadio);

        JLabel label = new JLabel("Mode:");
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setPreferredSize(new Dimension(120, 24));

        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        modes.setOpaque(false);
        modes.add(label);
        modes.add(calculatorOnlyRadio);
        modes.add(fullPlanRadio);

        ActionListener modeListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateMealSectionVisibility();
            }
        };
        calculatorOnlyRadio.addActionListener(modeListener);
        fullPlanRadio.addActionListener(modeListener);

        return modes;
    }

    private JPanel createBodySection() {
        JPanel section = createSection("1. Body Information");

        addLabel(section, "Age:", 0, 0);
        ageField = new JTextField("22", 6);
        addControl(section, ageField, 1, 0, 1);

        addLabel(section, "Gender:", 2, 0);
        genderCombo = new JComboBox<>(UserProfile.Gender.values());
        genderCombo.setSelectedItem(UserProfile.Gender.FEMALE);
        addControl(section, genderCombo, 3, 0, 1);

        addLabel(section, "Height (cm):", 0, 1);
        heightField = new JTextField("160", 6);
        addControl(section, heightField, 1, 1, 1);

        addLabel(section, "Weight (kg):", 2, 1);
        weightField = new JTextField("55", 6);
        addControl(section, weightField, 3, 1, 1);

        return section;
    }

    private JPanel createActivitySection() {
        JPanel section = createSection("2. Activity & Goal");

        addLabel(section, "Activity Level:", 0, 0);
        activityCombo = new JComboBox<>(UserProfile.ActivityLevel.values());
        activityCombo.setSelectedItem(UserProfile.ActivityLevel.LIGHT);
        activityCombo.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof UserProfile.ActivityLevel) {
                    value = ((UserProfile.ActivityLevel) value).getDescription();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        addControl(section, activityCombo, 1, 0, 1);

        addLabel(section, "Goal:", 2, 0);
        goalCombo = new JComboBox<>(UserProfile.Goal.values());
        goalCombo.setSelectedItem(UserProfile.Goal.MAINTAIN);
        goalCombo.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof UserProfile.Goal) {
                    value = ((UserProfile.Goal) value).getDescription();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        addControl(section, goalCombo, 3, 0, 1);

        caloriePreviewLabel = new JLabel("Fill the body fields to see the calorie estimate.");
        caloriePreviewLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        caloriePreviewLabel.setForeground(Color.GRAY);
        GridBagConstraints c = sectionConstraint(1, 1, 3);
        section.add(caloriePreviewLabel, c);

        ActionListener previewListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateCaloriePreview();
            }
        };
        ageField.addActionListener(previewListener);
        heightField.addActionListener(previewListener);
        weightField.addActionListener(previewListener);
        genderCombo.addActionListener(previewListener);
        activityCombo.addActionListener(previewListener);
        goalCombo.addActionListener(previewListener);

        return section;
    }

    private JPanel createMealSection() {
        JPanel section = createSection("3. Food Preferences");

        addLabel(section, "Today's mood:", 0, 0);
        dailyPrefCombo = new JComboBox<>(UserProfile.DailyPreference.values());
        dailyPrefCombo.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof UserProfile.DailyPreference) {
                    value = ((UserProfile.DailyPreference) value).getDisplayName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        addControl(section, dailyPrefCombo, 1, 0, 3);

        addLabel(section, "Allergies:", 0, 1);
        String[] allergies = {"Peanuts", "Shellfish", "Dairy", "Gluten", "Soy", "Eggs"};
        allergyChecks = new JCheckBox[allergies.length];
        JPanel allergyPanel = new JPanel(new GridLayout(1, 6, 6, 0));
        allergyPanel.setOpaque(false);
        for (int i = 0; i < allergies.length; i++) {
            allergyChecks[i] = new JCheckBox(allergies[i]);
            allergyChecks[i].setOpaque(false);
            allergyPanel.add(allergyChecks[i]);
        }
        addControl(section, allergyPanel, 1, 1, 3);

        // Dislikes are checkboxes, exactly like allergies. The user can tick
        // as many categories as they want.
        addLabel(section, "Dislikes:", 0, 2);
        String[] dislikes = {"Pork", "Beef", "Chicken", "Fish", "Spicy",
                             "Fried", "Bitter", "Sweet", "Sour", "Organ Meat"};
        dislikeChecks = new JCheckBox[dislikes.length];
        JPanel dislikePanel = new JPanel(new GridLayout(2, 5, 6, 0));
        dislikePanel.setOpaque(false);
        for (int i = 0; i < dislikes.length; i++) {
            dislikeChecks[i] = new JCheckBox(dislikes[i]);
            dislikeChecks[i].setOpaque(false);
            dislikePanel.add(dislikeChecks[i]);
        }
        addControl(section, dislikePanel, 1, 2, 3);

        addLabel(section, "Diet:", 0, 3);
        JPanel dietPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        dietPanel.setOpaque(false);
        vegetarianCheck = new JCheckBox("Vegetarian");
        halalCheck = new JCheckBox("Halal");
        vegetarianCheck.setOpaque(false);
        halalCheck.setOpaque(false);
        dietPanel.add(vegetarianCheck);
        dietPanel.add(halalCheck);
        addControl(section, dietPanel, 1, 3, 1);

        addLabel(section, "Courses today:", 2, 3);
        JPanel coursePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        coursePanel.setOpaque(false);
        includeBreakfastCheck = new JCheckBox("Breakfast", true);
        includeLunchCheck = new JCheckBox("Lunch", true);
        includeDinnerCheck = new JCheckBox("Dinner", true);
        includeSnackCheck = new JCheckBox("Snacks", true);

        JCheckBox[] courseBoxes = {includeBreakfastCheck, includeLunchCheck,
                includeDinnerCheck, includeSnackCheck};
        for (int i = 0; i < courseBoxes.length; i++) {
            courseBoxes[i].setOpaque(false);
            coursePanel.add(courseBoxes[i]);
        }
        addControl(section, coursePanel, 3, 3, 1);

        return section;
    }

    private JPanel createFormButtons() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        JButton resetBtn = new JButton("Reset");
        resetBtn.setPreferredSize(new Dimension(100, 38));
        resetBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetForm();
            }
        });

        generateBtn = new JButton("Generate Plan");
        generateBtn.setPreferredSize(new Dimension(170, 38));
        stylePrimaryButton(generateBtn);
        generateBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                generateAndShowResults();
            }
        });

        actions.add(resetBtn);
        actions.add(generateBtn);
        return actions;
    }

    private JPanel createSection(String title) {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBackground(new Color(252, 253, 255));
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(205, 215, 230)),
                        title, TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 13), new Color(41, 128, 185)),
                BorderFactory.createEmptyBorder(3, 6, 5, 6)));
        return section;
    }

    private GridBagConstraints sectionConstraint(int x, int y, int width) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = width;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(3, 5, 3, 5);
        return c;
    }

    private void addLabel(JPanel section, String text, int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 5);

        JLabel label = new JLabel(text);
        label.setPreferredSize(new Dimension(120, 24));
        section.add(label, c);
    }

    private void addControl(JPanel section, JComponent component, int x, int y, int width) {
        GridBagConstraints c = sectionConstraint(x, y, width);
        c.weightx = 1;
        section.add(component, c);
    }

    private void updateMealSectionVisibility() {
        boolean fullPlan = !calculatorOnlyRadio.isSelected();

        if (mealSection != null) {
            mealSection.setVisible(fullPlan);
        }
        if (generateBtn != null) {
            generateBtn.setText(fullPlan ? "Generate Plan" : "Calculate Calories");
        }
        if (formContent != null) {
            formContent.revalidate();
            formContent.repaint();
        }
    }

    private void updateCaloriePreview() {
        if (caloriePreviewLabel == null) return;

        try {
            UserProfile temp = getProfileFromForm();
            if (temp.isValid()) {
                int target = CalorieCalculator.calculateTargetCalories(temp);
                int bmr = (int) Math.round(CalorieCalculator.calculateBMR(temp));
                int tdee = (int) Math.round(CalorieCalculator.calculateTDEE(temp));

                caloriePreviewLabel.setText(String.format(
                        "<html>Estimated: <b>%d kcal/day</b>&nbsp; (BMR: %d, TDEE: %d)</html>",
                        target, bmr, tdee));
                caloriePreviewLabel.setForeground(new Color(41, 128, 185));
            } else {
                caloriePreviewLabel.setText("Fill all fields to see the estimate.");
                caloriePreviewLabel.setForeground(Color.GRAY);
            }
        } catch (Exception e) {
            caloriePreviewLabel.setText("Enter valid numbers.");
            caloriePreviewLabel.setForeground(Color.RED);
        }
    }

    // ---------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------

    private JPanel createPlaceholderResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Results will appear here", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 16));
        label.setForeground(Color.GRAY);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private void showResults(JPanel panel) {
        cardPanel.remove(resultsCard);
        resultsCard = panel;
        cardPanel.add(resultsCard, CARD_RESULTS);
        cardLayout.show(cardPanel, CARD_RESULTS);
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    void showForm() {
        cardLayout.show(cardPanel, CARD_FORM);
        formPanel.revalidate();
        formPanel.repaint();
    }

    void showResultsCard() {
        cardLayout.show(cardPanel, CARD_RESULTS);
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    void showSummary() {
        if (currentPlan == null) return;

        cardPanel.remove(summaryCard);
        summaryCard = new StarterSummaryPanel(currentPlan, this);
        cardPanel.add(summaryCard, CARD_SUMMARY);
        cardLayout.show(cardPanel, CARD_SUMMARY);
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    void resetForm() {
        ageField.setText("22");
        heightField.setText("160");
        weightField.setText("55");
        genderCombo.setSelectedItem(UserProfile.Gender.FEMALE);
        activityCombo.setSelectedItem(UserProfile.ActivityLevel.LIGHT);
        goalCombo.setSelectedItem(UserProfile.Goal.MAINTAIN);
        dailyPrefCombo.setSelectedItem(UserProfile.DailyPreference.ANY);

        for (int i = 0; i < allergyChecks.length; i++) {
            allergyChecks[i].setSelected(false);
        }
        for (int i = 0; i < dislikeChecks.length; i++) {
            dislikeChecks[i].setSelected(false);
        }

        vegetarianCheck.setSelected(false);
        halalCheck.setSelected(false);
        includeBreakfastCheck.setSelected(true);
        includeLunchCheck.setSelected(true);
        includeDinnerCheck.setSelected(true);
        includeSnackCheck.setSelected(true);

        fullPlanRadio.setSelected(true);
        calculatorOnlyMode = false;
        currentPlan = null;

        updateMealSectionVisibility();
        updateCaloriePreview();
        showForm();
    }

    // ---------------------------------------------------------------
    // Validation and generation
    // ---------------------------------------------------------------

    private boolean validateForm() {
        try {
            int age = Integer.parseInt(ageField.getText().trim());
            double height = Double.parseDouble(heightField.getText().trim());
            double weight = Double.parseDouble(weightField.getText().trim());

            if (age < 1 || age >= 120 || height <= 50 || height >= 250
                    || weight <= 20 || weight >= 300) {
                showError("Please enter a valid age (1-119), height (50-250 cm) and weight (20-300 kg).");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for age, height and weight.");
            return false;
        }

        if (activityCombo.getSelectedItem() == null || goalCombo.getSelectedItem() == null) {
            showError("Please select an activity level and a goal.");
            return false;
        }

        if (!calculatorOnlyMode) {
            if (dailyPrefCombo.getSelectedItem() == null) {
                showError("Please select today's food mood.");
                return false;
            }
            if (!includeBreakfastCheck.isSelected() && !includeLunchCheck.isSelected()
                    && !includeDinnerCheck.isSelected() && !includeSnackCheck.isSelected()) {
                showError("Please include at least one course.");
                return false;
            }
        }
        return true;
    }

    private void generateAndShowResults() {
        calculatorOnlyMode = calculatorOnlyRadio.isSelected();
        if (!validateForm()) return;

        currentProfile = getProfileFromForm();
        String error = currentProfile.getValidationError();
        if (error != null) {
            showError(error);
            return;
        }

        if (calculatorOnlyMode) {
            showResultsCalculatorOnly(currentProfile);
        } else {
            currentPlan = generator.generatePlan(currentProfile);
            showResults(new StarterResultsPanel(currentPlan, generator, this));
        }
    }

    private UserProfile getProfileFromForm() {
        UserProfile profile = new UserProfile();
        profile.setAge(Integer.parseInt(ageField.getText().trim()));
        profile.setGender((UserProfile.Gender) genderCombo.getSelectedItem());
        profile.setHeightCm(Double.parseDouble(heightField.getText().trim()));
        profile.setWeightKg(Double.parseDouble(weightField.getText().trim()));
        profile.setActivityLevel((UserProfile.ActivityLevel) activityCombo.getSelectedItem());
        profile.setGoal((UserProfile.Goal) goalCombo.getSelectedItem());
        profile.setDailyPreference((UserProfile.DailyPreference) dailyPrefCombo.getSelectedItem());

        ArrayList<String> allergies = new ArrayList<>();
        for (int i = 0; i < allergyChecks.length; i++) {
            if (allergyChecks[i].isSelected()) {
                allergies.add(allergyChecks[i].getText().toLowerCase());
            }
        }
        profile.setAllergies(allergies.toArray(new String[0]));

        // Every ticked dislike is added. This is the multi-select behaviour
        // that used to be a JList in the reference.
        ArrayList<String> dislikes = new ArrayList<>();
        for (int i = 0; i < dislikeChecks.length; i++) {
            if (dislikeChecks[i].isSelected()) {
                dislikes.add(dislikeChecks[i].getText().toLowerCase());
            }
        }
        profile.setDislikes(dislikes.toArray(new String[0]));

        profile.setVegetarian(vegetarianCheck.isSelected());
        profile.setHalal(halalCheck.isSelected());

        profile.setIncludeBreakfast(includeBreakfastCheck.isSelected());
        profile.setIncludeLunch(includeLunchCheck.isSelected());
        profile.setIncludeDinner(includeDinnerCheck.isSelected());
        profile.setIncludeSnack(includeSnackCheck.isSelected());

        return profile;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showResultsCalculatorOnly(UserProfile profile) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 24, 15, 24));
        panel.setBackground(new Color(245, 247, 250));

        int bmr = (int) Math.round(CalorieCalculator.calculateBMR(profile));
        int tdee = (int) Math.round(CalorieCalculator.calculateTDEE(profile));
        int target = CalorieCalculator.calculateTargetCalories(profile);
        double bmi = CalorieCalculator.calculateBMI(profile);
        String warning = CalorieCalculator.getSafetyWarning(profile, target);

        String text = "CALORIE RESULT\n\n"
                + "BMR: " + bmr + " kcal/day\n"
                + "TDEE: " + tdee + " kcal/day (x"
                + String.format("%.2f", profile.getActivityLevel().getMultiplier()) + ")\n"
                + "Goal: " + profile.getGoal().getDescription() + "\n"
                + "Target: " + target + " kcal/day\n\n"
                + "Suggested macros: Protein " + CalorieCalculator.getProteinTarget(target)
                + "g | Carbs " + CalorieCalculator.getCarbsTarget(target)
                + "g | Fat " + CalorieCalculator.getFatTarget(target) + "g\n"
                + "BMI: " + bmi + " (" + CalorieCalculator.getBMICategory(bmi) + ")\n"
                + "Safety check: " + (warning == null ? "OK" : "Warning - " + warning)
                + "\n\nChoose Full Meal Plan to get 3 meal options per course.";

        JTextArea textArea = new JTextArea(text);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setEditable(false);

        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        JButton editBtn = new JButton("Edit Inputs");
        editBtn.setPreferredSize(new Dimension(140, 36));
        editBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showForm();
            }
        });

        JButton newBtn = new JButton("New Calculation");
        newBtn.setPreferredSize(new Dimension(160, 36));
        stylePrimaryButton(newBtn);
        newBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetForm();
            }
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        south.setOpaque(false);
        south.add(editBtn);
        south.add(newBtn);
        panel.add(south, BorderLayout.SOUTH);

        showResults(panel);
    }

    /**
     * Flat blue primary button. BasicButtonUI keeps the background color on
     * every platform, unlike the default Windows look and feel.
     */
    static void stylePrimaryButton(JButton button) {
        button.setBackground(new Color(41, 128, 185));
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }
                new StarterGUI().setVisible(true);
            }
        });
    }
}
~~~

---

## Phase 12. StarterResultsPanel - 3 options per course and skip

**File:** `src/mealprep/starter/gui/StarterResultsPanel.java`

The results page shows a summary bar and a 2x2 grid of course cards. Each card has a skip checkbox, a budget, and up to three option cards.

Skipping a course calls `toggleSlot`. The plan redistributes the calories and `generator.regenerateEnabledSlots` rebuilds the remaining courses.

The user cannot skip every course. The last enabled course stays on.

Selecting an option clears the other options in that course and marks the chosen one.

The bottom bar has only `Edit Inputs` and `Show My Result`. There is no history, export, or regenerate button.

Concepts: `JPanel`, `BoxLayout`, `GridLayout`, `JScrollPane`, `JCheckBox`, `JButton`, anonymous listeners.

Complete code:

~~~java
package mealprep.starter.gui;

import mealprep.starter.algorithm.MealPlanGenerator;
import mealprep.starter.model.FoodItem;
import mealprep.starter.model.MealOption;
import mealprep.starter.model.MealPlan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Results page: three options per course and a skip checkbox per course.
 *
 * There is no history, export, repair, or side dish in this edition.
 * The only action is "Show My Result", which opens the summary page.
 */
public class StarterResultsPanel extends JPanel {

    private final MealPlan plan;
    private final MealPlanGenerator generator;
    private final StarterGUI parentFrame;
    private final DecimalFormat df = new DecimalFormat("#,##0");

    private JLabel summaryLabel;
    private JLabel macroLabel;
    private JLabel statusLabel;

    public StarterResultsPanel(MealPlan plan, MealPlanGenerator generator, StarterGUI parent) {
        this.plan = plan;
        this.generator = generator;
        this.parentFrame = parent;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        add(buildTopBar(), BorderLayout.NORTH);

        ScrollablePanel slotsPanel = new ScrollablePanel(new GridLayout(2, 2, 15, 15));
        slotsPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            slotsPanel.add(wrapInScroll(new MealSlotPanel(type)));
        }

        JScrollPane scrollPane = new JScrollPane(slotsPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        add(buildBottomBar(), BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        topBar.setBackground(new Color(245, 248, 255));

        summaryLabel = new JLabel();
        summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        macroLabel = new JLabel();
        macroLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        macroLabel.setForeground(Color.GRAY);

        statusLabel = new JLabel();
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(summaryLabel);
        left.add(macroLabel);

        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        topBar.add(left, BorderLayout.CENTER);
        topBar.add(statusLabel, BorderLayout.SOUTH);

        updateSummary();
        return topBar;
    }

    private JPanel buildBottomBar() {
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)));

        JButton editBtn = new JButton("Edit Inputs");
        editBtn.setPreferredSize(new Dimension(150, 36));
        editBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parentFrame.showForm();
            }
        });

        JButton resultBtn = new JButton("Show My Result");
        resultBtn.setPreferredSize(new Dimension(170, 36));
        StarterGUI.stylePrimaryButton(resultBtn);
        resultBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showSummary();
            }
        });

        bottomBar.add(editBtn);
        bottomBar.add(resultBtn);
        return bottomBar;
    }

    private JScrollPane wrapInScroll(JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ---------------------------------------------------------------
    // Refresh / totals
    // ---------------------------------------------------------------

    private void refreshAll() {
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    private void updateSummary() {
        if (summaryLabel == null) return;

        summaryLabel.setText(String.format("Daily Target: %s kcal   |   Selected: %s kcal",
                df.format(plan.getTargetCalories()), df.format(plan.getSelectedCalories())));

        macroLabel.setText(String.format("Protein: %.1fg  |  Carbs: %.1fg  |  Fat: %.1fg",
                plan.getSelectedProtein(), plan.getSelectedCarbs(), plan.getSelectedFat()));

        String skipped = "";
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) {
                skipped += (skipped.isEmpty() ? "" : ", ") + MealPlan.getSlotName(type);
            }
        }
        if (!skipped.isEmpty()) {
            macroLabel.setText(macroLabel.getText() + "   |   Skipped: " + skipped);
        }

        String status = plan.getCalorieStatus();
        if (status.startsWith("On target")) {
            statusLabel.setText("<html><font color='#27ae60'>" + status + "</font></html>");
        } else if (status.startsWith("+")) {
            statusLabel.setText("<html><font color='#e74c3c'>" + status + "</font></html>");
        } else {
            statusLabel.setText("<html><font color='#e67e22'>" + status + "</font></html>");
        }
    }

    // ---------------------------------------------------------------
    // Course switching and option selection
    // ---------------------------------------------------------------

    private void toggleSlot(FoodItem.MealType type, boolean include) {
        if (!include && plan.getEnabledSlotCount() == 1) {
            JOptionPane.showMessageDialog(this,
                    "At least one course must stay enabled.",
                    "Cannot skip everything", JOptionPane.WARNING_MESSAGE);
            refreshAll();
            return;
        }

        plan.setSlotEnabled(type, include);
        generator.regenerateEnabledSlots(plan);
        refreshAll();
    }

    private void selectOption(FoodItem.MealType type, MealOption chosen) {
        ArrayList<MealOption> options = plan.getOptions(type);
        for (MealOption option : options) {
            option.setSelected(false);
        }
        chosen.setSelected(true);
        refreshAll();
    }

    private void showSummary() {
        int picks = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (plan.isSlotEnabled(type) && plan.getSelectedOption(type) != null) {
                picks++;
            }
        }

        if (picks == 0) {
            JOptionPane.showMessageDialog(this,
                    "Pick at least one meal first.",
                    "Nothing selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        parentFrame.showSummary();
    }

    // ---------------------------------------------------------------
    // One course card
    // ---------------------------------------------------------------

    private class MealSlotPanel extends ScrollablePanel {

        private final FoodItem.MealType type;

        MealSlotPanel(FoodItem.MealType type) {
            this.type = type;
            boolean enabled = plan.isSlotEnabled(type);
            int budget = plan.getBudget(type);

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(colorFor(type));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(colorFor(type).darker(), 2),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);

            JCheckBox includeBox = new JCheckBox(
                    iconFor(type) + " " + MealPlan.getSlotName(type).toUpperCase(), enabled);
            includeBox.setFont(new Font("SansSerif", Font.BOLD, 13));
            includeBox.setOpaque(false);
            includeBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    toggleSlot(type, includeBox.isSelected());
                }
            });

            JLabel budgetLabel = new JLabel(
                    enabled ? "Budget: " + df.format(budget) + " kcal" : "SKIPPED");
            budgetLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            budgetLabel.setForeground(enabled ? Color.DARK_GRAY : new Color(150, 80, 0));

            header.add(includeBox, BorderLayout.WEST);
            header.add(budgetLabel, BorderLayout.EAST);
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            add(header);
            add(Box.createVerticalStrut(8));

            if (!enabled) {
                JLabel note = new JLabel("<html><i>This course is skipped. Its calorie share<br>"
                        + "was added to the other courses.</i></html>");
                note.setFont(new Font("SansSerif", Font.PLAIN, 11));
                note.setForeground(new Color(120, 90, 40));
                note.setAlignmentX(Component.LEFT_ALIGNMENT);
                add(note);
                return;
            }

            ArrayList<MealOption> options = plan.getOptions(type);
            if (options.isEmpty()) {
                JLabel empty = new JLabel("No options available (check the filters)");
                empty.setFont(new Font("SansSerif", Font.ITALIC, 12));
                empty.setForeground(Color.RED);
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                add(empty);
            } else {
                for (int i = 0; i < options.size(); i++) {
                    add(createOptionCard(options.get(i)));
                    if (i < options.size() - 1) {
                        add(Box.createVerticalStrut(8));
                    }
                }
            }
        }

        private JPanel createOptionCard(MealOption option) {
            JPanel card = new JPanel(new BorderLayout(10, 5));
            card.setBackground(option.isSelected() ? new Color(200, 240, 200) : Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(option.isSelected()
                            ? new Color(100, 200, 100) : new Color(200, 200, 200), 2),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

            JPanel left = new JPanel(new GridLayout(3, 1, 2, 2));
            left.setOpaque(false);

            JLabel nameLabel = new JLabel(option.getName());
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            nameLabel.setToolTipText(option.getName());

            JLabel calorieLabel = new JLabel(String.format(
                    "%s kcal  |  P:%.1fg  C:%.1fg  F:%.1fg  |  %s",
                    df.format(option.getCalories()), option.getProtein(), option.getCarbs(),
                    option.getFat(), option.getServingSize()));
            calorieLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            calorieLabel.setForeground(Color.DARK_GRAY);

            JLabel reasonLabel = new JLabel(option.getReason());
            reasonLabel.setFont(new Font("SansSerif", Font.ITALIC, 10));
            reasonLabel.setForeground(new Color(100, 100, 100));
            reasonLabel.setToolTipText(option.getReason());

            left.add(nameLabel);
            left.add(calorieLabel);
            left.add(reasonLabel);

            JButton selectBtn = new JButton(option.isSelected() ? "Selected" : "Select");
            selectBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
            StarterGUI.stylePrimaryButton(selectBtn);
            if (option.isSelected()) {
                selectBtn.setBackground(new Color(46, 204, 113));
            }
            selectBtn.setPreferredSize(new Dimension(90, 30));
            selectBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectOption(type, option);
                }
            });

            card.add(left, BorderLayout.CENTER);
            card.add(selectBtn, BorderLayout.EAST);
            return card;
        }

        private Color colorFor(FoodItem.MealType type) {
            switch (type) {
                case BREAKFAST: return new Color(255, 243, 224);
                case LUNCH:     return new Color(232, 245, 233);
                case DINNER:    return new Color(239, 235, 255);
                case SNACK:     return new Color(255, 248, 230);
                default:        return Color.WHITE;
            }
        }

        private String iconFor(FoodItem.MealType type) {
            switch (type) {
                case BREAKFAST: return "\u2600";
                case LUNCH:     return "\u25D1";
                case DINNER:    return "\u263E";
                case SNACK:     return "\u25CF";
                default:        return "";
            }
        }
    }
}
~~~

---

## Phase 13. StarterSummaryPanel - calories and meals picked

**File:** `src/mealprep/starter/gui/StarterSummaryPanel.java`

This is the final answer screen. It lists every course, the selected dish, the serving size, and the calories.

Skipped courses are shown clearly, with a note that their calorie share moved to the other courses.

The footer shows target, picked, difference, macros, and a status such as `On target (+/-10%)`.

`Back to Options` returns to the results page without losing the picks. `Edit Inputs` returns to the filled form. `Start Over` resets everything.

Concepts: `BorderLayout`, `BoxLayout`, `JLabel`, `DecimalFormat`, anonymous listeners.

Complete code:

~~~java
package mealprep.starter.gui;

import mealprep.starter.model.FoodItem;
import mealprep.starter.model.MealOption;
import mealprep.starter.model.MealPlan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

/**
 * Final summary page.
 *
 * This is the answer to "show me the calories and the meals I picked".
 * It lists every course, the selected dish, and the final total.
 */
public class StarterSummaryPanel extends JPanel {

    private final MealPlan plan;
    private final StarterGUI parentFrame;
    private final DecimalFormat df = new DecimalFormat("#,##0");

    public StarterSummaryPanel(MealPlan plan, StarterGUI parent) {
        this.plan = plan;
        this.parentFrame = parent;

        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        setBackground(new Color(245, 247, 250));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMealList(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Your Daily Result");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(41, 128, 185));

        JLabel subtitle = new JLabel("Calories and the meals you picked.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(Color.GRAY);

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(title);
        left.add(subtitle);

        JLabel target = new JLabel("Target: " + df.format(plan.getTargetCalories()) + " kcal/day");
        target.setFont(new Font("SansSerif", Font.BOLD, 15));
        target.setForeground(new Color(41, 128, 185));

        header.add(left, BorderLayout.WEST);
        header.add(target, BorderLayout.EAST);
        return header;
    }

    private JScrollPane buildMealList() {
        ScrollablePanel list = new ScrollablePanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        list.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 215, 230)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            list.add(createMealRow(type));
            list.add(Box.createVerticalStrut(6));
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JPanel createMealRow(FoodItem.MealType type) {
        JPanel row = new JPanel(new BorderLayout(12, 4));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 242)),
                BorderFactory.createEmptyBorder(8, 4, 8, 4)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel course = new JLabel(MealPlan.getSlotName(type).toUpperCase());
        course.setFont(new Font("SansSerif", Font.BOLD, 13));
        course.setForeground(colorFor(type).darker());
        course.setPreferredSize(new Dimension(110, 24));
        row.add(course, BorderLayout.WEST);

        if (!plan.isSlotEnabled(type)) {
            JLabel skipped = new JLabel("Skipped - its calorie share was added to the other courses.");
            skipped.setFont(new Font("SansSerif", Font.ITALIC, 12));
            skipped.setForeground(new Color(150, 80, 0));
            row.add(skipped, BorderLayout.CENTER);
            return row;
        }

        MealOption picked = plan.getSelectedOption(type);
        if (picked == null) {
            JLabel none = new JLabel("No meal selected.");
            none.setFont(new Font("SansSerif", Font.ITALIC, 12));
            none.setForeground(Color.RED);
            row.add(none, BorderLayout.CENTER);
            return row;
        }

        JPanel info = new JPanel(new GridLayout(2, 1, 2, 2));
        info.setOpaque(false);

        JLabel name = new JLabel(picked.getName());
        name.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel detail = new JLabel(String.format("%s  |  P:%.1fg  C:%.1fg  F:%.1fg",
                picked.getServingSize(), picked.getProtein(), picked.getCarbs(), picked.getFat()));
        detail.setFont(new Font("SansSerif", Font.PLAIN, 11));
        detail.setForeground(Color.DARK_GRAY);

        info.add(name);
        info.add(detail);
        row.add(info, BorderLayout.CENTER);

        JLabel calories = new JLabel(df.format(picked.getCalories()) + " kcal");
        calories.setFont(new Font("SansSerif", Font.BOLD, 14));
        calories.setForeground(new Color(41, 128, 185));
        row.add(calories, BorderLayout.EAST);

        return row;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);

        int selected = plan.getSelectedCalories();
        int difference = selected - plan.getTargetCalories();
        String status = plan.getCalorieStatus();

        JPanel totals = new JPanel(new GridLayout(4, 1, 2, 2));
        totals.setOpaque(false);

        JLabel totalLine = new JLabel(String.format(
                "Target: %s kcal   |   Picked: %s kcal   |   Difference: %+d kcal",
                df.format(plan.getTargetCalories()), df.format(selected), difference));
        totalLine.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel macroLine = new JLabel(String.format(
                "Protein: %.1fg   |   Carbs: %.1fg   |   Fat: %.1fg",
                plan.getSelectedProtein(), plan.getSelectedCarbs(), plan.getSelectedFat()));
        macroLine.setFont(new Font("SansSerif", Font.PLAIN, 12));
        macroLine.setForeground(Color.DARK_GRAY);

        JLabel statusLine = new JLabel(status);
        statusLine.setFont(new Font("SansSerif", Font.BOLD, 12));
        if (status.startsWith("On target")) {
            statusLine.setForeground(new Color(39, 174, 96));
        } else if (difference > 0) {
            statusLine.setForeground(new Color(231, 76, 60));
        } else {
            statusLine.setForeground(new Color(230, 126, 34));
        }

        JLabel note = new JLabel("Estimates are for an educational project, not medical advice.");
        note.setFont(new Font("SansSerif", Font.ITALIC, 11));
        note.setForeground(Color.GRAY);

        totals.add(totalLine);
        totals.add(macroLine);
        totals.add(statusLine);
        totals.add(note);
        footer.add(totals, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        buttons.setOpaque(false);

        JButton backBtn = new JButton("Back to Options");
        backBtn.setPreferredSize(new Dimension(150, 36));
        backBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parentFrame.showResultsCard();
            }
        });

        JButton editBtn = new JButton("Edit Inputs");
        editBtn.setPreferredSize(new Dimension(130, 36));
        editBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parentFrame.showForm();
            }
        });

        JButton newBtn = new JButton("Start Over");
        newBtn.setPreferredSize(new Dimension(130, 36));
        StarterGUI.stylePrimaryButton(newBtn);
        newBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parentFrame.resetForm();
            }
        });

        buttons.add(backBtn);
        buttons.add(editBtn);
        buttons.add(newBtn);
        footer.add(buttons, BorderLayout.SOUTH);
        return footer;
    }

    private Color colorFor(FoodItem.MealType type) {
        switch (type) {
            case BREAKFAST: return new Color(255, 243, 224);
            case LUNCH:     return new Color(232, 245, 233);
            case DINNER:    return new Color(239, 235, 255);
            case SNACK:     return new Color(255, 248, 230);
            default:        return Color.WHITE;
        }
    }
}
~~~

---

## Phase 14. Build and run

### `build.bat`

~~~bat
@echo off
REM Build every Java file under src\ into out\
setlocal
if not exist out mkdir out

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$files = Get-ChildItem -Recurse -Path 'src' -Filter '*.java' | ForEach-Object { $_.FullName }; " ^
  "& javac -encoding UTF-8 -d out $files; exit $LASTEXITCODE"
set RESULT=%ERRORLEVEL%

if %RESULT% neq 0 (
    echo.
    echo BUILD FAILED - read the errors above.
    exit /b %RESULT%
)

echo.
echo BUILD OK. Next:
echo   run-console.bat   - self-tests + console demo
echo   run-gui.bat       - Swing application
endlocal
~~~

### `run-console.bat`

~~~bat
@echo off
if not exist out\mealprep\starter\StarterApp.class (
    echo Classes not found. Run build.bat first.
    exit /b 1
)
java -cp out mealprep.starter.StarterApp
~~~

### `run-gui.bat`

~~~bat
@echo off
if not exist out\mealprep\starter\gui\StarterGUI.class (
    echo Classes not found. Run build.bat first.
    exit /b 1
)
java -cp out mealprep.starter.gui.StarterGUI
~~~

Run from the Starter project root:

~~~powershell
.\build.bat
.\run-console.bat
.\run-gui.bat
~~~

Expected console ending:

~~~text
SELF-TESTS: 26 passed, 0 failed
~~~

Expected sample numbers for the default profile (22, female, 160 cm, 55 kg, light activity, maintain):

~~~text
BMR:    1279 kcal/day
TDEE:   1759 kcal/day
Target: 1750 kcal/day
BMI:    21.5 (Normal)
All courses:     breakfast 438 | lunch 613 | dinner 525 | snacks 175
Skip breakfast:  breakfast 0   | lunch 817 | dinner 700 | snacks 233
~~~

## What to say in the defense

1. The app has one input page, one options page, and one summary page.
2. The calculator uses Mifflin-St Jeor, an activity multiplier, a goal adjustment, a safety floor, and rounding to 50.
3. Skipping a course does not delete calories. `MealPlan.getBudget` removes that course from the percentage total and shares the target among the remaining courses.
4. The recommender filters first, scores second, sorts third, and takes the top three. It prefers different main ingredients.
5. Allergies and dislikes are hard filters. Dislikes are checkboxes, so the user can pick several at once.
6. The Starter Edition has no history, export, repair, or side dishes. The final summary shows the calorie total and the exact meals picked.

## Common problems

| Problem | Fix |
|---|---|
| `File not found: data/philippine_foods.csv` | Run the program from the Starter project root, not from `src`. |
| `javac` is not recognized | Add the JDK `bin` folder to `PATH`, or run from a terminal where `javac -version` works. |
| The right column of the results grid is clipped | Keep `ScrollablePanel`; it makes the 2x2 grid follow the window width. |
| The form is too tall | It is scrollable. Do not remove the `JScrollPane`. |
| A course has fewer than 3 options | The filters removed too many foods. Clear an allergy or dislike and generate again. |

*Estimates are for an educational project, not medical advice.*
