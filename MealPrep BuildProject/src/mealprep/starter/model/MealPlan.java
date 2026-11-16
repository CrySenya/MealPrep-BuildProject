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
