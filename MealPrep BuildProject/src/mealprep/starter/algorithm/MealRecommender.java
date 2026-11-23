package mealprep.starter.algorithm;

import mealprep.starter.database.FoodDatabase;
import mealprep.starter.model.FoodItem;
import mealprep.starter.model.MealOption;
import mealprep.starter.model.MealPlan;
import mealprep.starter.model.UserProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chooses three meal options for each course.
 *
 * Steps for one course:
 *   1. Remove foods that match an allergy, a dislike, or a diet rule.
 *   2. Remove fast food (search only, never auto-suggested).
 *   3. Remove meals already picked earlier this session (unless that empties
 *      the course).
 *   4. Score the remaining foods by calorie fit and the mood for that course.
 *   5. Sort, then take the best three, keeping any meal the caller wants to keep.
 *
 *   Score: 2 points for a close calorie fit, 1 point for a looser fit, 1.5 points for the course mood, and small bonuses for popular or vegetarian dishes 
 *   selectOptions(..., keep, excludeIds) is shared by normal generation, per-course "New Options" and generated rebuilds.
 */
public class MealRecommender {

    //num of options to choose from for every course
    private static final int OPTIONS_PER_SLOT = 3;
        //15% tolerance for exceeding or short calories. for ex target: 600, it tolerates 510 and 690 since 15% od 600 is 90
    private static final double CALORIE_TOLERANCE = 0.15;

    private final FoodDatabase foodDatabase;

    // Stores the database that every candidate list is read from.
    public MealRecommender(FoodDatabase foodDatabase) {
        this.foodDatabase = foodDatabase;
    }

    // Generates a meal plan with three options for each enabled course.
    public MealPlan generatePlan(UserProfile profile, int targetCalories) {
        MealPlan plan = new MealPlan(profile, targetCalories);

        //checks if user wants breakfast(same as lunch, dinner, and snacks)
        plan.setSlotEnabled(FoodItem.MealType.BREAKFAST, profile.isIncludeBreakfast());
        plan.setSlotEnabled(FoodItem.MealType.LUNCH, profile.isIncludeLunch());
        plan.setSlotEnabled(FoodItem.MealType.DINNER, profile.isIncludeDinner());
        plan.setSlotEnabled(FoodItem.MealType.SNACK, profile.isIncludeSnack());

        //this sppecific block of code (MealPlan generatePlan(UserProfile profile, int targetCalories)), acts as a blueprint of things we can use later such as accessing user profile, target calories, courses, options for each course. these data will be used later to evaluate what meals are elligible .
        //checking enabled courses. a skipped course receive no options
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) {
                plan.setOptions(type, new ArrayList<>());
            } else {
                plan.setOptions(type, selectOptions(type, plan.getBudget(type), profile, null,
                        new ArrayList<Integer>()));
            }
        }
        return plan;
    }

    // Rebuilds the enabled courses after a skip or unskip, keeping any meals the user has already picked.
    public void regenerateEnabledSlots(MealPlan plan) {
        UserProfile profile = plan.getUserProfile();
        
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) {
                plan.setOptions(type, new ArrayList<>());
                continue;
            }

            MealOption keep = plan.isCourseUserPicked(type) ? plan.getSelectedOption(type) : null;
            ArrayList<MealOption> options = selectOptions(type, plan.getBudget(type), profile,
                    keep, new ArrayList<Integer>());
            plan.setOptions(type, options);
            keepSelection(options, keep);
        }
    }

    // Rebuilds one course after the user pressed "New Options", keeping any meal the user has already picked.
    public void rerollSlot(MealPlan plan, FoodItem.MealType type) {
        if (!plan.isSlotEnabled(type)) return;

        UserProfile profile = plan.getUserProfile();
        MealOption keep = plan.getSelectedOption(type);

        ArrayList<Integer> excludeIds = new ArrayList<>();
        for (MealOption option : plan.getOptions(type)) {
            if (keep == null || option.getFoodItem().getId() != keep.getFoodItem().getId()) {
                excludeIds.add(option.getFoodItem().getId());
            }
        }

        ArrayList<MealOption> options = selectOptions(type, plan.getBudget(type), profile,
                keep, excludeIds);
        plan.setOptions(type, options);
        keepSelection(options, keep);
    }

    // Restores the selected meal after the options were rebuilt.
    private void keepSelection(ArrayList<MealOption> options, MealOption keep) {
        for (MealOption option : options) {
            option.setSelected(false);
        }
        if (keep != null && options.contains(keep)) {
            keep.setSelected(true);
        } else if (!options.isEmpty()) {
            options.get(0).setSelected(true);
        }
    }

    /**
     * this block of code rebuilds options for every enabled course if the user skipped a course.
     * this is used if a user skipped a meal after the plan was already made.
     * this is important for the calorie target distribution.
     */
    public ArrayList<MealOption> selectOptions(FoodItem.MealType type, int budget,
                                               UserProfile profile, MealOption keep,
                                               ArrayList<Integer> excludeIds) {
        //storage for all elligible foods from profile                                        
        ArrayList<FoodItem> candidates = getCandidates(type, profile);
        // stores data for scored foods
        ArrayList<ScoredFood> scored = new ArrayList<>();
        for (FoodItem food : candidates) {
            if (keep != null && food.getId() == keep.getFoodItem().getId()) {
                continue;
            }
            scored.add(new ScoredFood(food,
                    scoreFood(food, budget, profile, type),
                    buildReason(food, budget, profile, type)));
        }
        // this sorts the foods by score
        scored.sort(new Comparator<ScoredFood>() {
            public int compare(ScoredFood a, ScoredFood b) {
                return Double.compare(b.score, a.score);
            }
        });

        // this block of code selects the best options for a course, keeping any meal the user has already picked. it also ensures that no two options have the same main ingredient, and that excluded foods are not included in the options. 
        ArrayList<MealOption> options = new ArrayList<>();
        ArrayList<String> usedMainTags = new ArrayList<>();

        if (keep != null) {
            options.add(keep);
            usedMainTags.add(getMainProteinTag(keep.getFoodItem()));
        }

        // First pass: one dish per main ingredient, skipping excluded foods.
        for (ScoredFood candidate : scored) {
            if (options.size() >= OPTIONS_PER_SLOT) break;
            if (excludeIds.contains(candidate.food.getId())) continue;

            String mainTag = getMainProteinTag(candidate.food);
            if (!usedMainTags.contains(mainTag)) {
                options.add(new MealOption(candidate.food, candidate.score, candidate.reason));
                usedMainTags.add(mainTag);
            }
        }

        // Second pass: fill up to three. Excluded foods are allowed here only
        // if there were not enough other choices.
        for (ScoredFood candidate : scored) {
            if (options.size() >= OPTIONS_PER_SLOT) break;
            if (!containsFood(options, candidate.food.getId())) {
                options.add(new MealOption(candidate.food, candidate.score, candidate.reason));
            }
        }

        //this controls the portions of calories  for every option
        for (MealOption option : options) {
            if (option == keep || option.isUserPicked() || option.isAddedBySearch()) {
                continue;
            }
            fitOptionToBudget(option, budget);
        }
        return options;
    }

    // ---------------------------------------------------------------
    // Filtering
    // ---------------------------------------------------------------

    /** Number of foods that pass the filters for one course. Used by the tests. */
    public int countCandidates(FoodItem.MealType type, UserProfile profile) {
        return getCandidates(type, profile).size();
    }

    // Foods of one course that pass every filter.
    private ArrayList<FoodItem> getCandidates(FoodItem.MealType type, UserProfile profile) {
        ArrayList<String> excludeTags = buildExclusionTags(profile);
        ArrayList<FoodItem> allowed = new ArrayList<>();

        for (FoodItem food : foodDatabase.getFoodsByMealType(type)) {
            // Fast food is never auto-suggested; it is only reachable through search.
            if (food.isFastFood()) {
                continue;
            }

            //excludes invalid foods by checking if it has been tagged
            boolean excluded = false;
            for (String tag : excludeTags) {
                if (food.hasTag(tag)) {
                    excluded = true;
                    break;
                }
            }
             //add valid foods
            if (!excluded) {
                allowed.add(food);
            }
        }

        // Remove meals picked on an earlier day in this session.
        List<Integer> used = profile.getRecentlyUsedFoodIds();
        if (!used.isEmpty()) {
            ArrayList<FoodItem> fresh = new ArrayList<>();
            for (FoodItem food : allowed) {
                if (!used.contains(food.getId())) {
                    fresh.add(food);
                }
            }
            // Never let a course run out of options because of the exclusion.
            if (!fresh.isEmpty()) {
                return fresh;
            }
        }
        return allowed;
    }

    // this method retrieves and fikters foods 
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

        // this block of code checks if the user is vegetarian or halal and adds the appropriate tags to the exclusion list. this is important for filtering out foods that do not meet the user's dietary restrictions.
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

    // Adds one lowercase tag once, ignoring blanks and duplicates.
    private void addTag(ArrayList<String> tags, String tag) {
        String value = tag.trim().toLowerCase();
        if (!value.isEmpty() && !tags.contains(value)) {
            tags.add(value);
        }
    }

    // Expands one allergy name into the tags it should block.
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

    // Expands one disliked food into the tags it should block.
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

    //This method determines whether a food matches the user’s selected daily mood. if gusto nila for example chicken. 
    private double scoreFood(FoodItem food, int budget, UserProfile profile,
                             FoodItem.MealType type) {
        double score = 0;

        //use calorie tolerance to score food if within 15%, receive 2.0, if within 30, receive 1.5, 0 if neither
        int difference = Math.abs(fittedCalories(food, budget) - budget);
        if (budget > 0) {
            if (difference <= budget * CALORIE_TOLERANCE) {
                score += 2.0;
            } else if (difference <= budget * 0.30) {
                score += 1.0;
            }
        }

        //checks if the food matches user daily preference, if it does, then add 1.5 to the food
        UserProfile.DailyPreference mood = profile.getCourseMood(type);
        if (mood != UserProfile.DailyPreference.ANY && matchesMood(food, mood)) {
            score += 1.5;
        }

        //basically add score depende sa preference
        if (food.hasTag("popular")) {
            score += 0.3;
        }
        if (profile.isVegetarian() && food.hasTag("vegetarian")) {
            score += 0.5;
        }
        if (profile.isHalal() && !food.hasTag("pork")) {
            score += 0.2;
        }

        return score;
    }

    /** Calories of this food at the portion that best fits the budget. */
    private int fittedCalories(FoodItem food, int budget) {
        if (budget <= 0) return food.getCalories();

        double rawPortions = (double) budget / Math.max(1, food.getCalories());
        double portions = Math.round(rawPortions * 2.0) / 2.0;
        if (portions < 1.0) portions = 1.0;
        if (portions > 3.0) portions = 3.0;

        return (int) Math.round(food.getCalories() * portions);
    }

    // True when the food fits the chosen mood for that course.
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
    }

    //This creates the explanation shown to the user.
    private String buildReason(FoodItem food, int budget, UserProfile profile,
                               FoodItem.MealType type) {
        StringBuilder reason = new StringBuilder();
        int difference = food.getCalories() - budget;

        if (budget <= 0) {
            reason.append("Fits the remaining budget");
        } else if (Math.abs(difference) <= budget * CALORIE_TOLERANCE) {
            reason.append("Good calorie fit");
        } else if (difference < 0) {
            reason.append("Light option (").append(-difference).append(" kcal under)");
        } else {
            reason.append("Hearty option (").append(difference).append(" kcal over)");
        }

        UserProfile.DailyPreference mood = profile.getCourseMood(type);
        if (mood != UserProfile.DailyPreference.ANY && matchesMood(food, mood)) {
            reason.append(" | Matches ").append(MealPlan.getSlotName(type).toLowerCase()).append(" mood");
        }

        if (food.hasTag("popular")) {
            reason.append(" | Popular choice");
        }

        return reason.toString();
    }

    //assigns each food to main-protein category. This data will be used to prevent more than one main-protein food provider
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
    }

    // ---------------------------------------------------------------
    // Portion fitting
    // ---------------------------------------------------------------

    // Scales one option to the portion that comes closest to the budget.
    private void fitOptionToBudget(MealOption option, int budget) {
        FoodItem food = option.getFoodItem();
        if (budget <= 0) return;

        //calculates  how many original servings would reach the budget.
        double rawPortions = (double) budget / Math.max(1, food.getCalories());
        double portions = Math.round(rawPortions * 2.0) / 2.0;

        //The user receives between one and three servings. This limits the portions
        if (portions < 1.0) portions = 1.0;
        if (portions > 3.0) portions = 3.0;

        // then calculates calories and macros using the new portions.
        option.setPortions(portions);

        //add reasons such as good portion fiit showm below
        if (portions != 1.0) {
            String kept = stripCalorieFitReason(option.getReason());
            int finalDifference = option.getCalories() - budget;
            String fitReason;

            //This removes the first calorie-related phrase while keeping later explanations.
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

    // Removes the first calorie-related phrase from the reason, leaving any later explanations.
    private String stripCalorieFitReason(String reason) {
        int separator = reason.indexOf(" | ");
        if (separator >= 0) {
            return reason.substring(separator + 3);
        }
        return "";
    }

    // this changes food into id so that it can only be used once
    private boolean containsFood(ArrayList<MealOption> options, int foodId) {
        for (MealOption option : options) {
            if (option.getFoodItem().getId() == foodId) {
                return true;
            }
        }
        return false;
    }

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
