package mealprep.starter;

import mealprep.starter.algorithm.CalorieCalculator;
import mealprep.starter.algorithm.MealPlanGenerator;
import mealprep.starter.algorithm.MealRecommender;
import mealprep.starter.algorithm.SearchEvaluator;
import mealprep.starter.algorithm.SpellCorrector;
import mealprep.starter.algorithm.TextNormalizer;
import mealprep.starter.algorithm.TfIdfIndex;
import mealprep.starter.database.FoodDatabase;
import mealprep.starter.model.FoodItem;
import mealprep.starter.model.MealOption;
import mealprep.starter.model.MealPlan;
import mealprep.starter.model.SearchResult;
import mealprep.starter.model.UserProfile;

import java.util.ArrayList;

/**
 * Console entry point and self-tests for the Starter Edition.
 *
 * NLP ADDITIONS:
 *   - tests for the tokenizer, edit distance, spell correction and TF-IDF,
 *   - the baseline-versus-NLP evaluation table.
 *
 * Run this before the GUI:
 *   java -cp out mealprep.starter.StarterApp
 */
public class StarterApp {

    private static int passed = 0;
    private static int failed = 0;

    // Runs the self-tests and the evaluation report, then prints the pass/fail total.
    public static void main(String[] args) {
        System.out.println("=======================================");
        System.out.println("    MEALPREP STARTER - NLP SELF-TESTS");
        System.out.println("=======================================\n");

        FoodDatabase foodDatabase = new FoodDatabase();
        if (!foodDatabase.loadFromCSV("data/philippine_foods.csv")) {
            System.err.println("Failed to load data/philippine_foods.csv");
            System.err.println("Run this program from the starter folder.");
            return;
        }
        foodDatabase.appendFromCSV("data/fastfood_foods.csv");
        System.out.println(foodDatabase + "\n");

        runSelfTests(foodDatabase);

        // The measured comparison of the old search against the NLP search.
        SearchEvaluator.printReport(foodDatabase, "data/search_eval.csv");

        System.out.println("=======================================");
        System.out.println("SELF-TESTS: " + passed + " passed, " + failed + " failed");
        System.out.println("=======================================");
    }

    // Runs every check in order and prints PASS or FAIL for each one.
    private static void runSelfTests(FoodDatabase foodDatabase) {
        System.out.println("--- SELF-TESTS ---");

        UserProfile user = createTestUser();

        // 1. Database
        check("database has at least 50 home-cooked foods", foodDatabase.size() >= 50);
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            check(type + " has foods", foodDatabase.size(type) > 0);
        }
        check("fast-food file added more foods", foodDatabase.countFastFoods() >= 20);

        // 2. Calorie formulas
        check("female BMR = 1279",
                Math.abs(CalorieCalculator.calculateBMR(user) - 1279) < 0.01);
        check("target is rounded to 50",
                CalorieCalculator.calculateTargetCalories(user) % 50 == 0);
        check("BMI = 21.5",
                Math.abs(CalorieCalculator.calculateBMI(user) - 21.5) < 0.01);

        // 3. Skip-aware budgets
        MealPlan budgetPlan = new MealPlan(user, 2000);
        check("all-course budgets sum to 2000", budgetSum(budgetPlan) == 2000);

        budgetPlan.setSlotEnabled(FoodItem.MealType.BREAKFAST, false);
        check("skipped-course budgets still sum to 2000", budgetSum(budgetPlan) == 2000);
        check("skipped breakfast budget is 0", budgetPlan.getBreakfastBudget() == 0);
        check("lunch budget grows after skipping breakfast", budgetPlan.getLunchBudget() > 700);

        // 4. Exact tag matching
        FoodItem bangus = foodDatabase.getFoodById(3);
        check("bangus has the milkfish tag", bangus != null && bangus.hasTag("milkfish"));
        check("dairy 'milk' does not match 'milkfish'",
                bangus != null && !bangus.hasTag("milk"));

        // 5. Three options per course
        MealPlanGenerator generator = new MealPlanGenerator(foodDatabase);
        MealPlan normal = generator.generatePlan(createTestUser());

        check("at most 3 options per course", maxOptions(normal) <= 3);
        check("at least 1 option per enabled course", minOptions(normal) >= 1);
        check("selected total is above zero", normal.getSelectedCalories() > 0);
        check("every enabled course has a selected meal", everyEnabledCourseHasPick(normal));
        check("portions stay between 1.0 and 3.0", portionsInRange(normal));

        // 6. Filters
        UserProfile dairy = createTestUser();
        dairy.setAllergies(new String[]{"dairy"});
        MealPlan dairyPlan = generator.generatePlan(dairy);
        check("dairy allergy removes milk, cheese, cream and butter",
                noTags(dairyPlan, new String[]{"milk", "cheese", "cream", "butter"}));

        UserProfile vegetarian = createTestUser();
        vegetarian.setVegetarian(true);
        MealPlan vegetarianPlan = generator.generatePlan(vegetarian);
        check("vegetarian plan contains no meat",
                noTags(vegetarianPlan, new String[]{"pork", "ground-pork", "beef", "oxtail",
                        "chicken", "duck", "fish", "milkfish", "tilapia", "salmon",
                        "seafood", "shrimp", "crab", "squid"}));

        UserProfile noPork = createTestUser();
        noPork.setDislikes(new String[]{"pork"});
        MealPlan noPorkPlan = generator.generatePlan(noPork);
        check("pork dislike removes pork dishes",
                noTags(noPorkPlan, new String[]{"pork", "ground-pork"}));

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

        // ---------------------------------------------------------------
        // ADDED FEATURE TESTS
        // ---------------------------------------------------------------

        // 9. Moods are stored per course and applied from the meal planner page.
        MealPlan moodPlan = generator.generatePlan(createTestUser());
        generator.applyCourseMood(moodPlan, FoodItem.MealType.LUNCH,
                UserProfile.DailyPreference.CHICKEN);

        check("planner mood is stored on the profile",
                moodPlan.getUserProfile().getCourseMood(FoodItem.MealType.LUNCH)
                        == UserProfile.DailyPreference.CHICKEN);
        check("planner mood rebuilds that course",
                planHasTag(moodPlan, FoodItem.MealType.LUNCH, "chicken"));
        check("planner mood leaves other courses alone",
                moodPlan.getUserProfile().getCourseMood(FoodItem.MealType.DINNER)
                        == UserProfile.DailyPreference.ANY);

        // 10. Fast food is search-only, never suggested.
        MealPlan noFastFoodPlan = generator.generatePlan(createTestUser());
        check("fast food is never auto-suggested", noFastFoodPlan.countFastFoodOptions() == 0);

        MealRecommender recommender = generator.getMealRecommender();
        int lunchCandidates = recommender.countCandidates(FoodItem.MealType.LUNCH, createTestUser());
        check("fast-food rows are filtered out of candidates",
                lunchCandidates < generator.getFoodDatabase().size(FoodItem.MealType.LUNCH));

        // 11. Search still finds fast food, now through the ranked NLP search.
        check("ranked search finds Jollibee meals",
                foodDatabase.searchRanked("jollibee", FoodItem.MealType.LUNCH).size() >= 2);
        check("ranked search by exact name finds Chickenjoy",
                !foodDatabase.searchRanked("chickenjoy 1pc", FoodItem.MealType.LUNCH).isEmpty());

        // 12. Per-course re-roll keeps the pick and changes the others.
        UserProfile rerollUser = createTestUser();
        MealPlan rerollPlan = generator.generatePlan(rerollUser);
        MealOption kept = rerollPlan.getOptions(FoodItem.MealType.LUNCH).get(0);
        kept.setSelected(true);
        kept.setUserPicked(true);
        int keptId = kept.getFoodItem().getId();
        ArrayList<Integer> beforeIds = optionIds(rerollPlan, FoodItem.MealType.LUNCH);

        generator.rerollSlot(rerollPlan, FoodItem.MealType.LUNCH);

        MealOption afterKeep = rerollPlan.getSelectedOption(FoodItem.MealType.LUNCH);
        ArrayList<Integer> afterIds = optionIds(rerollPlan, FoodItem.MealType.LUNCH);
        check("re-roll keeps the saved meal", afterKeep != null
                && afterKeep.getFoodItem().getId() == keptId);
        check("re-roll changes the other options", changedOtherOptions(beforeIds, afterIds, keptId));

        // 13. Logging a searched meal keeps its real calories.
        UserProfile loggedUser = createTestUser();
        MealPlan loggedPlan = generator.generatePlan(loggedUser);
        FoodItem bigMac = foodDatabase.getFoodById(920);
        MealOption logged = generator.addSearchOption(loggedPlan,
                FoodItem.MealType.LUNCH, bigMac);
        check("logged meal keeps real calories (no rescaling)",
                logged.getPortions() == 1.0 && logged.getCalories() == bigMac.getCalories());
        check("logged meal is marked and locked",
                logged.isAddedBySearch() && loggedPlan.isCourseLocked(FoodItem.MealType.LUNCH));
        check("a logged course still shows three options",
                loggedPlan.getOptions(FoodItem.MealType.LUNCH).size() == 3);

        // 14. Refit the other courses to the leftover calories.
        UserProfile refitUser = createTestUser();
        MealPlan refitPlan = generator.generatePlan(refitUser);
        FoodItem bigLunch = foodDatabase.getFoodById(902); // Chickenjoy 2pc, 990 kcal
        generator.addSearchOption(refitPlan, FoodItem.MealType.LUNCH, bigLunch);
        generator.fitRemainingToCourses(refitPlan);

        int leftover = refitPlan.getTargetCalories() - bigLunch.getCalories();
        int freeBudgets = refitPlan.getBreakfastBudget()
                + refitPlan.getDinnerBudget() + refitPlan.getSnackBudget();
        check("logged course keeps its own meal", refitPlan.getSelectedOption(FoodItem.MealType.LUNCH) != null
                && refitPlan.getSelectedOption(FoodItem.MealType.LUNCH)
                        .getFoodItem().getId() == 902);
        check("remaining courses share the leftover calories",
                Math.abs(freeBudgets - leftover) <= 3);
        check("a big logged lunch shrinks the dinner budget",
                refitPlan.getDinnerBudget() < 525);

        // 15. Start Next Day hides today's picks.
        UserProfile dayUser = createTestUser();
        MealPlan dayOne = generator.generatePlan(dayUser);
        dayOne.getOptions(FoodItem.MealType.BREAKFAST).get(0).setSelected(true);
        ArrayList<Integer> dayOneIds = selectedIds(dayOne);

        MealPlan dayTwo = generator.recordPicksAndAdvance(dayOne);
        check("next day keeps the same profile", dayTwo.getUserProfile() == dayUser);
        check("next day increments the day counter", dayTwo.getUserProfile().getDay() == 2);
        check("next day remembers the meals already eaten",
                dayTwo.getUserProfile().getRecentlyUsedFoodIds().containsAll(dayOneIds));
        check("next day does not suggest meals already eaten",
                !planContainsAny(dayTwo, dayOneIds));

        // ---------------------------------------------------------------
        // NLP FEATURE TESTS
        // ---------------------------------------------------------------

        // 16. Text normalization and tokenization.
        ArrayList<String> withStopwords = TextNormalizer.tokenize("chicken with rice");
        check("tokenizer removes stopwords", withStopwords.size() == 2
                && withStopwords.contains("chicken") && withStopwords.contains("rice"));
        check("tokenizer lowercases and strips punctuation",
                TextNormalizer.tokenize("Chickenjoy, 1pc!").get(0).equals("chickenjoy"));

        // 17. Damerau-Levenshtein edit distance.
        check("edit distance kitten -> sitting is 3",
                SpellCorrector.editDistance("kitten", "sitting") == 3);
        check("edit distance counts a swapped pair as 1",
                SpellCorrector.editDistance("ca", "ac") == 1);
        check("edit distance of identical words is 0",
                SpellCorrector.editDistance("adobo", "adobo") == 0);

        // 18. Spelling correction.
        check("corrector suggests sinigang for sinigng",
                "sinigang".equals(foodDatabase.suggestQuery("sinigng")));

        // 19. TF-IDF cosine ranking on a small controlled index.
        ArrayList<FoodItem> tinyFoods = new ArrayList<>();
        tinyFoods.add(new FoodItem(1, "Alpha Chicken", "Test", "lunch",
                500, 10, 10, 10, "1 plate", "chicken,rice"));
        tinyFoods.add(new FoodItem(2, "Beta Fish", "Test", "lunch",
                400, 10, 10, 10, "1 plate", "fish"));
        TfIdfIndex tinyIndex = new TfIdfIndex();
        tinyIndex.build(tinyFoods);

        double matchingScore = tinyIndex.score(TextNormalizer.tokenize("alpha chicken"), 0);
        double unrelatedScore = tinyIndex.score(TextNormalizer.tokenize("alpha chicken"), 1);
        check("tf-idf gives the matching meal a high cosine score", matchingScore > 0.9);
        check("tf-idf gives an unrelated meal a near-zero score", unrelatedScore < 0.1);

        // 20. Alias dictionary priority.
        check("alias chickenjoy puts food 901 first",
                foodDatabase.searchRanked("chickenjoy", FoodItem.MealType.LUNCH).get(0)
                        .getFoodItem().getId() == 901);

        // 21. Alias expansion for a nickname.
        ArrayList<SearchResult> tapsi =
                foodDatabase.searchRanked("tapsi", FoodItem.MealType.BREAKFAST);
        check("alias tapsi finds Tapsilog first",
                !tapsi.isEmpty() && tapsi.get(0).getFoodItem().getId() == 1);

        // 22. Typo tolerance end to end.
        ArrayList<SearchResult> typo = foodDatabase.searchRanked("sinigng", null);
        check("typo sinigng still finds a Sinigang dish",
                !typo.isEmpty()
                        && typo.get(0).getFoodItem().getName().toLowerCase().contains("sinigang"));

        // 23. Word order, which the baseline cannot handle.
        boolean baselineMissesWordOrder = foodDatabase
                .searchFoodsBaseline("rice chicken", FoodItem.MealType.LUNCH).isEmpty();
        boolean rankedHandlesWordOrder = !foodDatabase
                .searchRanked("rice chicken", FoodItem.MealType.LUNCH).isEmpty();
        check("ranked search handles swapped words that the baseline misses",
                baselineMissesWordOrder && rankedHandlesWordOrder);

        // 24. The course filter still applies to ranked results.
        ArrayList<SearchResult> lunchOnly =
                foodDatabase.searchRanked("rice", FoodItem.MealType.LUNCH);
        boolean allLunch = !lunchOnly.isEmpty();
        for (SearchResult result : lunchOnly) {
            if (result.getFoodItem().getMealType() != FoodItem.MealType.LUNCH) {
                allLunch = false;
            }
        }
        check("ranked search keeps the course filter", allLunch);

        // 25. Evaluation: the NLP version must not lose to the baseline.
        ArrayList<SearchEvaluator.QueryRow> evalRows =
                SearchEvaluator.loadQueries("data/search_eval.csv");
        double baselineTop1 = SearchEvaluator.precisionAtK(foodDatabase, evalRows, 1, false);
        double rankedTop1 = SearchEvaluator.precisionAtK(foodDatabase, evalRows, 1, true);
        check("NLP precision@1 is at least the baseline ("
                        + String.format("%.2f", rankedTop1) + " vs "
                        + String.format("%.2f", baselineTop1) + ")",
                !evalRows.isEmpty() && rankedTop1 >= baselineTop1);

        System.out.println();
    }

    // Builds the sample profile that every test uses.
    private static UserProfile createTestUser() {
        UserProfile profile = new UserProfile();
        profile.setAge(22);
        profile.setGender(UserProfile.Gender.FEMALE);
        profile.setHeightCm(160);
        profile.setWeightKg(55);
        profile.setActivityLevel(UserProfile.ActivityLevel.LIGHT);
        profile.setGoal(UserProfile.Goal.MAINTAIN);
        profile.setAllergies(new String[]{"peanuts"});
        profile.setDislikes(new String[]{"bitter"});
        profile.setVegetarian(false);
        profile.setHalal(false);
        return profile;
    }

    // Adds up the four course budgets.
    private static int budgetSum(MealPlan plan) {
        int sum = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            sum += plan.getBudget(type);
        }
        return sum;
    }

    // Largest number of options offered by any course.
    private static int maxOptions(MealPlan plan) {
        int max = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (plan.getOptions(type).size() > max) {
                max = plan.getOptions(type).size();
            }
        }
        return max;
    }

    // Fewest options among the courses that are switched on.
    private static int minOptions(MealPlan plan) {
        int min = Integer.MAX_VALUE;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) continue;
            int size = plan.getOptions(type).size();
            if (size < min) min = size;
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    // Counts switched-on courses that actually received options.
    private static int countEnabledOptions(MealPlan plan) {
        int count = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (plan.isSlotEnabled(type) && !plan.getOptions(type).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    // True when each switched-on course has one selected meal.
    private static boolean everyEnabledCourseHasPick(MealPlan plan) {
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (plan.isSlotEnabled(type) && plan.getSelectedOption(type) == null) {
                return false;
            }
        }
        return true;
    }

    // True when every portion is between 1x and 3x.
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

    // True when no offered meal carries any of the given tags.
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

    // True when every option explains why it was suggested.
    private static boolean everyOptionHasReason(MealPlan plan) {
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            for (MealOption option : plan.getOptions(type)) {
                if (option.getReason() == null || option.getReason().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    // Food ids of the options shown for one course.
    private static ArrayList<Integer> optionIds(MealPlan plan, FoodItem.MealType type) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (MealOption option : plan.getOptions(type)) {
            ids.add(option.getFoodItem().getId());
        }
        return ids;
    }

    // Food ids of the meals picked across all courses.
    private static ArrayList<Integer> selectedIds(MealPlan plan) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            MealOption picked = plan.getSelectedOption(type);
            if (picked != null) {
                ids.add(picked.getFoodItem().getId());
            }
        }
        return ids;
    }

    // True when a new option appeared that is not the kept meal.
    private static boolean changedOtherOptions(ArrayList<Integer> before,
                                               ArrayList<Integer> after, int keptId) {
        for (Integer id : after) {
            if (id != keptId && !before.contains(id)) {
                return true;
            }
        }
        return false;
    }

    // True when one course offers a meal with the given tag.
    private static boolean planHasTag(MealPlan plan, FoodItem.MealType type, String tag) {
        for (MealOption option : plan.getOptions(type)) {
            if (option.getFoodItem().hasTag(tag)) {
                return true;
            }
        }
        return false;
    }

    // True when any offered meal has one of the given food ids.
    private static boolean planContainsAny(MealPlan plan, ArrayList<Integer> ids) {
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            for (MealOption option : plan.getOptions(type)) {
                if (ids.contains(option.getFoodItem().getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Records one test result and prints its PASS or FAIL line.
    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS: " + label);
        } else {
            failed++;
            System.out.println("  FAIL: " + label);
        }
    }
}
