package mealprep.starter.algorithm;

import mealprep.starter.model.UserProfile;

/**
 * Calorie formulas.
 *
 * BMR  : Mifflin-St Jeor equation
 * TDEE : BMR x activity multiplier
 * Target: TDEE + goal adjustment, with a safety floor, rounded to 50
 */
public class CalorieCalculator {

    // BMR = Basal Metabolic Rate, the calories burned at rest.
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

    // TDEE = Total Daily Energy Expenditure, the calories burned in a day.
    public static double calculateTDEE(UserProfile profile) {
        return calculateBMR(profile) * profile.getActivityLevel().getMultiplier();
    }

    // Target calories = TDEE + goal adjustment, rounded to the nearest 50.
    public static int calculateTargetCalories(UserProfile profile) {
        double target = calculateTDEE(profile) + profile.getGoal().getCalorieAdjustment();

        // Safety floor: 1500 for men, 1200 for women. Round to nearest 50. should not suggest an extremely low intake.
        int floor = profile.getGender() == UserProfile.Gender.MALE ? 1500 : 1200;
        if (target < floor) {
            target = floor;
        }

        return (int) Math.round(target / 50.0) * 50;
    }

    // Returns body mass index from height and weight, rounded to one decimal.
    public static double calculateBMI(UserProfile profile) {
        double heightM = profile.getHeightCm() / 100.0;
        double bmi = profile.getWeightKg() / (heightM * heightM);
        return Math.round(bmi * 10.0) / 10.0;
    }

    // Standard BMI band: underweight, normal, overweight or obese.
    public static String getBMICategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Overweight";
        return "Obese";
    }

    // Grams of protein from 25% of the target calories (4 kcal per gram).
    public static int getProteinTarget(int targetCalories) {
        return (int) Math.round(targetCalories * 0.25 / 4.0);
    }

    // Grams of carbs from 45% of the target calories (4 kcal per gram).
    public static int getCarbsTarget(int targetCalories) {
        return (int) Math.round(targetCalories * 0.45 / 4.0);
    }

    // Grams of fat from 30% of the target calories (9 kcal per gram).
    public static int getFatTarget(int targetCalories) {
        return (int) Math.round(targetCalories * 0.30 / 9.0);
    }

    // Returns a warning message if the target calories are outside safe limits, or null if safe.
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
