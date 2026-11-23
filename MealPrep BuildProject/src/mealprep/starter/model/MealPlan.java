package mealprep.starter.model;

/*

getBudget is the important method. If a course is skipped, its percentage is removed from the denominator and the remaining courses share the target.

budgetOverride lets a course fit the calories left after the user logs a meal. getRemainingCalories is target minus picked.

isCourseLocked is true when the picked meal was logged through search, so "fit the leftovers" leaves that course alone.
*/

import java.util.ArrayList;

// Represents a daily meal plan with options for breakfast, lunch, dinner and snacks.
// If a course is skipped, its percentage is removed from the total and the remaining courses share the target calories.
public class MealPlan {
    // percentage of each meals
    public static final double BREAKFAST_PCT = 0.25;
    public static final double LUNCH_PCT = 0.35;
    public static final double DINNER_PCT = 0.30;
    public static final double SNACK_PCT = 0.10;

    // attributes
    private int targetCalories;
    private UserProfile userProfile;

    // options for each meals
    private ArrayList<MealOption> breakfastOptions = new ArrayList<>();
    private ArrayList<MealOption> lunchOptions = new ArrayList<>();
    private ArrayList<MealOption> dinnerOptions = new ArrayList<>();
    private ArrayList<MealOption> snackOptions = new ArrayList<>();

    // which meals is enabled
    private boolean[] enabled = {true, true, true, true};

    // Per-course budget set by "fit the leftovers to the other courses".
    private Integer[] budgetOverride = new Integer[4];

    // Constructor that stores the profile and the daily calorie target.
    public MealPlan(UserProfile userProfile, int targetCalories) {
        this.userProfile = userProfile;
        this.targetCalories = targetCalories;
    }

    // turn MealType to String
    public static String getSlotName(FoodItem.MealType type) {
        switch (type) {
            case BREAKFAST: return "Breakfast";
            case LUNCH:     return "Lunch";
            case DINNER:    return "Dinner";
            case SNACK:     return "Snacks";
            default:        return "Meal";
        }
    }

    // get base percentage of each meal
    public static double getBasePercentage(FoodItem.MealType type) {
        switch (type) {
            case BREAKFAST: return BREAKFAST_PCT;
            case LUNCH:     return LUNCH_PCT;
            case DINNER:    return DINNER_PCT;
            case SNACK:     return SNACK_PCT;
            default:        return 0;
        }
    }

     // get and set slots
    public boolean isSlotEnabled(FoodItem.MealType type) {
        return enabled[type.ordinal()];
    }

    public void setSlotEnabled(FoodItem.MealType type, boolean value) {
        enabled[type.ordinal()] = value;
    }

    // get number of slots that are enabled (true)
    public int getEnabledSlotCount() {
        int count = 0;
        for (int i = 0; i < enabled.length; i++) {
            if (enabled[i]) count++;
        }
        return count;
    }

    // Total share of the day used by the switched-on courses.
    public double getEnabledPercentage() {
        double total = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (isSlotEnabled(type)) {
                total += getBasePercentage(type);
            }
        }
        return total;
    }

    // Forces one course to a fixed calorie budget.
    public void setBudgetOverride(FoodItem.MealType type, int value) {
        budgetOverride[type.ordinal()] = Math.max(0, value);
    }

    // Removes every forced budget.
    public void clearBudgetOverrides() {
        for (int i = 0; i < budgetOverride.length; i++) {
            budgetOverride[i] = null;
        }
    }

    // True when this course has a forced budget.
    public boolean hasBudgetOverride(FoodItem.MealType type) {
        return budgetOverride[type.ordinal()] != null;
    }

    // Budget for one course after skipped courses are redistributed.
    // Returns 0 when the course is skipped.
    public int getBudget(FoodItem.MealType type) {
        if (!isSlotEnabled(type)) return 0;

        if (budgetOverride[type.ordinal()] != null) {
            return budgetOverride[type.ordinal()];
        }

        double enabledPercentage = getEnabledPercentage();
        if (enabledPercentage <= 0) return 0;

        return (int) Math.round(targetCalories * getBasePercentage(type) / enabledPercentage);
    }

    // get budget for each meal
    public int getBreakfastBudget() { return getBudget(FoodItem.MealType.BREAKFAST); }
    public int getLunchBudget()     { return getBudget(FoodItem.MealType.LUNCH); }
    public int getDinnerBudget()    { return getBudget(FoodItem.MealType.DINNER); }
    public int getSnackBudget()     { return getBudget(FoodItem.MealType.SNACK); }

    // get options for each meal
    public ArrayList<MealOption> getBreakfastOptions() { return breakfastOptions; }
    public ArrayList<MealOption> getLunchOptions()     { return lunchOptions; }
    public ArrayList<MealOption> getDinnerOptions()    { return dinnerOptions; }
    public ArrayList<MealOption> getSnackOptions()     { return snackOptions; }

    // get options for a specific meal type
    public ArrayList<MealOption> getOptions(FoodItem.MealType type) {
        switch (type) {
            case BREAKFAST: return breakfastOptions;
            case LUNCH:     return lunchOptions;
            case DINNER:    return dinnerOptions;
            case SNACK:     return snackOptions;
            default:        return new ArrayList<>();
        }
    }

    // set meal options
    public void setOptions(FoodItem.MealType type, ArrayList<MealOption> options) {
        switch (type) {
            case BREAKFAST: breakfastOptions = options; break;
            case LUNCH:     lunchOptions = options; break;
            case DINNER:    dinnerOptions = options; break;
            case SNACK:     snackOptions = options; break;
        }
    }

    // The option the user picked for one course, or null.
    public MealOption getSelectedOption(FoodItem.MealType type) {
        for (MealOption option : getOptions(type)) {
            if (option.isSelected()) {
                return option;
            }
        }
        return null;
    }

    // The picked option was searched for (a real meal the user already ate).
    public boolean isCourseLocked(FoodItem.MealType type) {
        MealOption picked = getSelectedOption(type);
        return picked != null && picked.isAddedBySearch();
    }

    // The picked option was chosen by the user (used to preserve picks).
    public boolean isCourseUserPicked(FoodItem.MealType type) {
        MealOption picked = getSelectedOption(type);
        return picked != null && picked.isUserPicked();
    }

    // How many courses are locked by a logged meal.
    public int countLockedCourses() {
        int count = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (isSlotEnabled(type) && isCourseLocked(type)) {
                count++;
            }
        }
        return count;
    }

    // How many offered options are fast food.
    public int countFastFoodOptions() {
        int count = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            for (MealOption option : getOptions(type)) {
                if (option.isFastFood()) {
                    count++;
                }
            }
        }
        return count;
    }

    // check if plan is complete
    public boolean isComplete() {
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (isSlotEnabled(type) && getOptions(type).isEmpty()) {
                return false;
            }
        }
        return getEnabledSlotCount() > 0;
    }

    // getter and setter of TargetCalories, and getter for UserProfile
    public int getTargetCalories() { return targetCalories; }
    public UserProfile getUserProfile() { return userProfile; }
    public void setTargetCalories(int targetCalories) { this.targetCalories = targetCalories; }

    // get the calories of the selected option
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

    // Remaining calories to reach the target after the selected meals.
    public int getRemainingCalories() {
        return targetCalories - getSelectedCalories();
    }

    // get the value for each macronutrients of the option
    public double getSelectedProtein() { return getSelectedMacro("protein"); }
    public double getSelectedCarbs()   { return getSelectedMacro("carbs"); }
    public double getSelectedFat()     { return getSelectedMacro("fat"); }

    // get the macronutrient value from the option
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

    // compares the selected calories to the target calories
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

     // get String of the object
    @Override
    public String toString() {
        return String.format("MealPlan[target=%d kcal, selected=%d kcal, enabled=%d/4]",
                targetCalories, getSelectedCalories(), getEnabledSlotCount());
    }
}
