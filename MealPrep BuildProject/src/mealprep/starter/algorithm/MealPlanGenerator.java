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

    private final FoodDatabase foodDatabase; //receives the food database from the main class and passes it to the recommender
    private final MealRecommender recommender;

    public MealPlanGenerator(FoodDatabase foodDatabase) { //constructor that takes in the food database and initializes the recommender with it
        this.foodDatabase = foodDatabase;
        this.recommender = new MealRecommender(foodDatabase);
    }

    public int getTargetCalories(UserProfile profile) { //returns the target calories for the user profile by calling the CalorieCalculator
        return CalorieCalculator.calculateTargetCalories(profile);
    }

    //main entry point: target -> build plan -> pick defaults
    public MealPlan generatePlan(UserProfile profile) { 
        int target = CalorieCalculator.calculateTargetCalories(profile); //calculate the target calories for the user profile
        MealPlan plan = recommender.generatePlan(profile, target);       //generate a meal plan using the recommender
        autoSelectFirstOptions(plan);                                    //automatically select the first option in each course
        return plan;
    }

    /** Rebuild the enabled courses after a skip or unskip. */
    public void regenerateEnabledSlots(MealPlan plan) {
        recommender.regenerateEnabledSlots(plan); //rebuild the enabled courses in the meal plan using the recommender
        autoSelectFirstOptions(plan); //automatically select the first option in each course after rebuilding the enabled courses as default
    }

    //clear all selections and select the first option in each course as default as the best recommendation for the user
    private void autoSelectFirstOptions(MealPlan plan) { 
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            ArrayList<MealOption> options = plan.getOptions(type);
            for (MealOption option : options) {                     //clear all selections in the meal plan
                option.setSelected(false);
            }
            if (plan.isSlotEnabled(type) && !options.isEmpty()) {   //if the course is enabled and has options, select the first option as default
                options.get(0).setSelected(true);         //the first option is selected as the best recommendation for the user
            }
        }
    }

    //builds a readable text version of the plan; it returns the text, it does not print it
    public String describePlan(MealPlan plan) {
        StringBuilder text = new StringBuilder();
        text.append("Target: ").append(plan.getTargetCalories()).append(" kcal/day\n\n"); //daily target calories for the user profile

        for (FoodItem.MealType type : FoodItem.MealType.values()) { //loop through each meal type (breakfast, lunch, dinner, snacks) and build a description of the meal plan
            String name = MealPlan.getSlotName(type).toUpperCase();

            if (!plan.isSlotEnabled(type)) {                        //if the course is skipped, add a message to the description and skip to the next course
                text.append("--- ").append(name).append(": SKIPPED ---\n\n");
                continue;
            }

            //course heading with its budget after skipped courses are redistributed
            text.append("--- ").append(name)
                    .append(" (Budget: ").append(plan.getBudget(type))
                    .append(" kcal) ---\n");

            ArrayList<MealOption> options = plan.getOptions(type); //get the options for the current course
            if (options.isEmpty()) {                               //if there are no options for the current course, add a message to the description
                text.append("  No options available (check the filters)\n");
            } else {                                               //if there are options for the current course, loop through them and add them to the description
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
        text.append("Status: ").append(plan.getCalorieStatus()).append("\n");
        return text.toString();
    }

    //accessors
    public FoodDatabase getFoodDatabase() { return foodDatabase; }
    public MealRecommender getMealRecommender() { return recommender; }
}