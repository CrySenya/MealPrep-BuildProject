package mealprep.starter.model;

/**
 * Holds the values entered on the application's user form.
 *
 * <p>This class is a model/data component rather than the place where meals
 * are calculated. The GUI creates and fills a {@code UserProfile}; later
 * components such as the calorie calculator and meal recommender read it to
 * personalize their work.</p>
 */
public class UserProfile {

    // Body information is used by the calorie calculator.
    private int age;
    private Gender gender;
    private double heightCm;
    private double weightKg;

    // Activity and goal are enums so the rest of the app receives valid choices
    // instead of having to compare free-form strings.
    private ActivityLevel activityLevel;
    private Goal goal;
    private DailyPreference dailyPreference = DailyPreference.ANY;

    // The GUI converts the allergy and dislike checkboxes into these arrays.
    private String[] allergies = new String[0];
    private String[] dislikes = new String[0];

    // These flags become dietary filters for meal recommendations.
    private boolean vegetarian;
    private boolean halal;

    // Each course can be included or skipped when the meal plan is generated.
    private boolean includeBreakfast = true;
    private boolean includeLunch = true;
    private boolean includeDinner = true;
    private boolean includeSnack = true;

    /**
     * The user's biological sex, used by the Mifflin-St Jeor calculation.
     */
    public enum Gender {
        MALE, FEMALE
    }

    /**
     * Activity choices shown by the GUI.
     *
     * <p>The multiplier is the numeric value used to turn BMR into TDEE.
     * The description is the human-readable text shown in a combo box.</p>
     */
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

        // The calculator uses this value after reading the selected enum.
        public double getMultiplier() {
            return multiplier;
        }

        // The GUI can display this instead of exposing the enum name.
        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Weight goals and their calorie adjustments.
     *
     * <p>The calorie calculator adds this adjustment to the maintenance
     * target: losing weight subtracts 500, maintaining changes nothing, and
     * gaining weight adds 500.</p>
     */
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

        // This is the numeric value consumed by the calorie calculator.
        public int getCalorieAdjustment() {
            return calorieAdjustment;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Optional preference used by the recommender when scoring food choices.
     *
     * <p>The tag is the machine-readable value matched against a food item's
     * tags, while the display name is what the GUI shows to the user.</p>
     */
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

        public String getDisplayName() {
            return displayName;
        }

        public String getTag() {
            return tag;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // Getters let calculators, recommenders, and GUI result screens read form data.
    public int getAge() {
        return age;
    }

    public Gender getGender() {
        return gender;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public Goal getGoal() {
        return goal;
    }

    public DailyPreference getDailyPreference() {
        return dailyPreference;
    }

    public String[] getAllergies() {
        return allergies;
    }

    public String[] getDislikes() {
        return dislikes;
    }

    public boolean isVegetarian() {
        return vegetarian;
    }

    public boolean isHalal() {
        return halal;
    }

    public boolean isIncludeBreakfast() {
        return includeBreakfast;
    }

    public boolean isIncludeLunch() {
        return includeLunch;
    }

    public boolean isIncludeDinner() {
        return includeDinner;
    }

    public boolean isIncludeSnack() {
        return includeSnack;
    }

    // Setters are the hand-off point where the GUI stores each form control's value.
    public void setAge(int age) {
        this.age = age;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public void setHeightCm(double heightCm) {
        this.heightCm = heightCm;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public void setActivityLevel(ActivityLevel activityLevel) {
        this.activityLevel = activityLevel;
    }

    public void setGoal(Goal goal) {
        this.goal = goal;
    }

    public void setDailyPreference(DailyPreference dailyPreference) {
        this.dailyPreference = dailyPreference;
    }

    /**
     * Stores allergy selections and treats a missing checkbox collection as empty.
     *
     * <p>An empty array keeps downstream filtering simple: the recommender can
     * always iterate over the result without first checking for {@code null}.</p>
     */
    public void setAllergies(String[] allergies) {
        this.allergies = allergies == null ? new String[0] : allergies;
    }

    /**
     * Stores dislike selections using the same null-to-empty rule as allergies.
     */
    public void setDislikes(String[] dislikes) {
        this.dislikes = dislikes == null ? new String[0] : dislikes;
    }

    public void setVegetarian(boolean vegetarian) {
        this.vegetarian = vegetarian;
    }

    public void setHalal(boolean halal) {
        this.halal = halal;
    }

    public void setIncludeBreakfast(boolean includeBreakfast) {
        this.includeBreakfast = includeBreakfast;
    }

    public void setIncludeLunch(boolean includeLunch) {
        this.includeLunch = includeLunch;
    }

    public void setIncludeDinner(boolean includeDinner) {
        this.includeDinner = includeDinner;
    }

    public void setIncludeSnack(boolean includeSnack) {
        this.includeSnack = includeSnack;
    }

    /**
     * Tells the meal-plan generator whether at least one course remains enabled.
     */
    public boolean hasAtLeastOneCourse() {
        return includeBreakfast || includeLunch || includeDinner || includeSnack;
    }

    /**
     * Performs the profile checks needed before calculation and recommendation.
     *
     * <p>This returns a boolean for callers that only need a quick gate. Use
     * {@link #getValidationError()} when the GUI needs to explain what to fix.</p>
     */
    public boolean isValid() {
        return age > 0 && age < 120
                && heightCm > 50 && heightCm < 250
                && weightKg > 20 && weightKg < 300
                && activityLevel != null
                && goal != null;
    }

    /**
     * Returns the first validation problem, or {@code null} when the profile is valid.
     */
    public String getValidationError() {
        if (age <= 0 || age >= 120) {
            return "Age must be between 1 and 119";
        }
        if (heightCm <= 50 || heightCm >= 250) {
            return "Height must be between 50 and 250 cm";
        }
        if (weightKg <= 20 || weightKg >= 300) {
            return "Weight must be between 20 and 300 kg";
        }
        if (activityLevel == null) {
            return "Please select an activity level";
        }
        if (goal == null) {
            return "Please select a goal";
        }
        return null;
    }

    /**
     * Gives logs and debugging output a compact view of the profile.
     */
    @Override
    public String toString() {
        return String.format("UserProfile[age=%d, %s, %.0fcm, %.1fkg, %s, %s]",
                age, gender, heightCm, weightKg, activityLevel, goal);
    }
}
