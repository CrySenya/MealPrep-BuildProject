/* 
 * (rerollSlot) is the per-course "New Options": keep the picked meal, replace the rest.
 * (applyCourseMood) changes one course mood and rebuilds only that course.
 * (addSearchOption) logs a meal the user already ate at its real calories, and fitRemainingToCourses rebuilds the other courses from the leftover.
 * (recordPicksAndAdvance) is "Start Next Day": remember today's picks, hide them for the session, and build a new day.
 * (describePlan) prints a plain-text plan for the console app.
 */
package mealprep.starter.algorithm;

import mealprep.starter.database.FoodDatabase;
import mealprep.starter.model.FoodItem;
import mealprep.starter.model.MealOption;
import mealprep.starter.model.MealPlan;
import mealprep.starter.model.UserProfile;

import java.util.ArrayList;

// Generates a meal plan for a user profile, using the FoodDatabase and MealRecommender.
public class MealPlanGenerator {

    private static final int OPTIONS_PER_SLOT = 3;

    //receives the food database from the main class and passes it to the recommender
    private final FoodDatabase foodDatabase;    
    private final MealRecommender recommender;

    //constructor that takes in the food database and initializes the recommender with it
    public MealPlanGenerator(FoodDatabase foodDatabase) {
        this.foodDatabase = foodDatabase;
        this.recommender = new MealRecommender(foodDatabase);
    }

    //returns the target calories for the user profile by calling the CalorieCalculator
    public int getTargetCalories(UserProfile profile) {
        return CalorieCalculator.calculateTargetCalories(profile);
    }

    //main entry point: target -> build plan -> pick defaults
    public MealPlan generatePlan(UserProfile profile) {
        int target = CalorieCalculator.calculateTargetCalories(profile);
        MealPlan plan = recommender.generatePlan(profile, target);
        autoSelectFirstOptions(plan);
        return plan;
    }

    // Rebuilds the plan's enabled courses, keeping the user's picks and any locked courses.
    public void regenerateEnabledSlots(MealPlan plan) {
        plan.clearBudgetOverrides();
        recommender.regenerateEnabledSlots(plan);       //rebuild the enabled courses in the meal plan using the recommender
        autoSelectFirstOptions(plan);                   //automatically select the first option in each course after rebuilding the enabled courses as default
    }

    /** Per-course "New Options": keep the picked meal, replace the rest. */
    public void rerollSlot(MealPlan plan, FoodItem.MealType type) {
        recommender.rerollSlot(plan, type);
        autoSelectFirstOptions(plan);
    }

    /**
     * Changes the mood for one course and rebuilds that course's options.
     * Used by the mood dropdown on the meal planner page.
     */
    public void applyCourseMood(MealPlan plan, FoodItem.MealType type,
                                UserProfile.DailyPreference preference) {
        plan.getUserProfile().setCourseMood(type, preference);
        if (!plan.isSlotEnabled(type)) return;

        ArrayList<MealOption> options = recommender.selectOptions(type, plan.getBudget(type),
                plan.getUserProfile(), null, new ArrayList<Integer>());
        plan.setOptions(type, options);
        autoSelectFirstOptions(plan);
    }

    // Fits the courses that have no logged meal to the calories left over.
    // Called right after the user logs a real meal in a course.
    public MealOption addSearchOption(MealPlan plan, FoodItem.MealType type, FoodItem food) {
        ArrayList<MealOption> options = plan.getOptions(type);

        MealOption chosen = null;
        for (MealOption option : options) {
            if (option.getFoodItem().getId() == food.getId()) {
                chosen = option;
                break;
            }
        }

        if (chosen == null) {
            chosen = new MealOption(food, 0, "You logged this meal");
            chosen.setAddedBySearch(true);
            options.add(0, chosen);
        } else {
            chosen.setAddedBySearch(true);
        }

        // Keep the card at three options: the logged meal plus the best two.
        while (options.size() > OPTIONS_PER_SLOT) {
            options.remove(options.size() - 1);
        }
        plan.setOptions(type, options);

        for (MealOption option : options) {
            option.setSelected(false);
        }
        chosen.setSelected(true);
        chosen.setUserPicked(true);
        return chosen;
    }

    // Fits the courses that have no logged meal to the calories left over.
    public void fitRemainingToCourses(MealPlan plan) {
        plan.clearBudgetOverrides();

        UserProfile profile = plan.getUserProfile();
        int fixedTotal = 0;
        double lockedPercentage = 0;

        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) continue;
            if (plan.isCourseLocked(type)) {
                fixedTotal += plan.getSelectedOption(type).getCalories();
                lockedPercentage += MealPlan.getBasePercentage(type);
            }
        }

        double freePercentage = plan.getEnabledPercentage() - lockedPercentage;
        int remaining = Math.max(0, plan.getTargetCalories() - fixedTotal);

        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) continue;
            if (plan.isCourseLocked(type)) continue;

            int budget = freePercentage <= 0 ? 0
                    : (int) Math.round(remaining * MealPlan.getBasePercentage(type) / freePercentage);
            plan.setBudgetOverride(type, budget);
        }

        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) continue;
            if (plan.isCourseLocked(type)) continue;

            MealOption keep = plan.isCourseUserPicked(type) ? plan.getSelectedOption(type) : null;
            ArrayList<MealOption> options = recommender.selectOptions(type, plan.getBudget(type),
                    profile, keep, new ArrayList<Integer>());
            plan.setOptions(type, options);

            MealOption selected = keep;
            for (MealOption option : options) {
                option.setSelected(false);
            }
            if (selected != null && options.contains(selected)) {
                selected.setSelected(true);
            } else if (!options.isEmpty()) {
                options.get(0).setSelected(true);
            }
        }
    }

    // Records the user's picks for the day, increments the day counter, and generates a new plan for the next day.
    public MealPlan recordPicksAndAdvance(MealPlan plan) {
        UserProfile profile = plan.getUserProfile();

        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) continue;
            MealOption picked = plan.getSelectedOption(type);
            if (picked != null) {
                profile.addRecentlyUsedFoodId(picked.getFoodItem().getId());
            }
        }

        profile.incrementDay();
        return generatePlan(profile);
    }

    //clear all selections and select the first option in each course as default as the best recommendation for the user
    private void autoSelectFirstOptions(MealPlan plan) {
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) continue;                // Skip disabled courses
            if (plan.getSelectedOption(type) != null) continue;     // Skip courses that already have a selected option

            // Automatically select the first option in each course as default
            ArrayList<MealOption> options = plan.getOptions(type);
            if (!options.isEmpty()) {
                options.get(0).setSelected(true);
            }
        }
    }

    //builds a readable text version of the plan; it returns the text, it does not print it
    public String describePlan(MealPlan plan) {
        StringBuilder text = new StringBuilder();
        text.append("Day ").append(plan.getUserProfile().getDay()).append("\n");
        text.append("Target: ").append(plan.getTargetCalories()).append(" kcal/day\n\n");   //daily target calories for the user profile

        //loop through each meal type (breakfast, lunch, dinner, snacks) and build a description of the meal plan
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            String name = MealPlan.getSlotName(type).toUpperCase();

            //if the course is skipped, add a message to the description and skip to the next course
            if (!plan.isSlotEnabled(type)) {
                text.append("--- ").append(name).append(": SKIPPED ---\n\n");
                continue;
            }

            //course heading with its budget after skipped courses are redistributed
            text.append("--- ").append(name)
                    .append(" (Budget: ").append(plan.getBudget(type))
                    .append(" kcal) ---\n");

            ArrayList<MealOption> options = plan.getOptions(type);                              //get the options for the current course
            if (options.isEmpty()) {                                                            //if there are no options for the current course, add a message to the description
                text.append("  No options available (check the filters)\n");
            } else {                                                                            //if there are options for the current course, loop through them and add them to the description
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

        //footer: total calories of the picks and how close they are to target
        text.append("Selected total: ").append(plan.getSelectedCalories()).append(" kcal\n");
        text.append("Remaining: ").append(plan.getRemainingCalories()).append(" kcal\n");
        text.append("Status: ").append(plan.getCalorieStatus()).append("\n");
        return text.toString();
    }

    //accessors
    public FoodDatabase getFoodDatabase() { return foodDatabase; }
    public MealRecommender getMealRecommender() { return recommender; }
}
