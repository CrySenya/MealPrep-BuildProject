/* Skipping a course calls (toggleSlot). The plan redistributes the calories and generator.
* (regenerateEnabledSlots) rebuilds the remaining courses.
 * Changing the mood calls (generator.applyCourseMood) and rebuilds that course. 
 * New Options calls (generator.rerollSlot) and keeps the picked meal.
 * The search bar calls (FoodDatabase.searchFoods). 
 * Adding a result calls (addSearchOption) and then (fitRemainingToCourses), so the other courses use the leftover calories.
 */
package mealprep.starter.gui;

import mealprep.starter.algorithm.CalorieCalculator;
import mealprep.starter.algorithm.MealPlanGenerator;
import mealprep.starter.database.FoodDatabase;
import mealprep.starter.model.FoodItem;
import mealprep.starter.model.MealPlan;
import mealprep.starter.model.UserProfile;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

// The main GUI window for the MealPrep Starter Edition. It has three cards: the input form, the results page, and the summary page.
public class StarterGUI extends JFrame {
    // The food database and the meal plan generator.
    private FoodDatabase foodDatabase;
    private MealPlanGenerator generator;
    private UserProfile currentProfile;
    private MealPlan currentPlan;

    // Card names for the three pages.
    private static final String CARD_FORM = "form";
    private static final String CARD_RESULTS = "results";
    private static final String CARD_SUMMARY = "summary";

    // The main panel that holds the three cards, and the individual cards.
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private JPanel formPanel;
    private JPanel formContent;
    private JPanel mealSection;
    private JPanel resultsCard;
    private JPanel summaryCard;

    // Form input widgets
    private PlaceholderTextField ageField, heightField, weightField;
    private JComboBox<UserProfile.Gender> genderCombo;
    private JComboBox<UserProfile.ActivityLevel> activityCombo;
    private JComboBox<UserProfile.Goal> goalCombo;
    private JCheckBox[] allergyChecks;
    private JCheckBox[] dislikeChecks;
    private JCheckBox vegetarianCheck;
    private JCheckBox includeBreakfastCheck, includeLunchCheck, includeDinnerCheck, includeSnackCheck;
    private JRadioButton calculatorOnlyRadio, fullPlanRadio;
    private JLabel caloriePreviewLabel;
    private JButton generateBtn;

    // Whether the user has selected "Calorie Calculator Only" mode. If true, the meal plan section is hidden and the results page only shows the calorie estimate.
    private boolean calculatorOnlyMode = false;

    // Window title, then the database and the widgets.
    public StarterGUI() {
        super("PlanABite");
        initializeDatabase();
        initializeUI();
    }

    // Loads both CSV files and creates the plan generator.
    private void initializeDatabase() {
        foodDatabase = new FoodDatabase();
        boolean loaded = foodDatabase.loadFromCSV("data/philippine_foods.csv");
        if (loaded) {
            foodDatabase.appendFromCSV("data/fastfood_foods.csv");
        }

        if (!loaded) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load the food database.\nCheck data/philippine_foods.csv.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        generator = new MealPlanGenerator(foodDatabase);
        System.out.println("GUI: " + foodDatabase);
    }

    // Builds the three cards and shows the input form.
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

    // Blue title bar with the app name and the plate icon.
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(41, 128, 185));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("PlanABite");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        
        JLabel subtitle = new JLabel("Calculate Your Calories and Plan Your Meals");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(new Color(200, 230, 255));

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(title);
        left.add(subtitle);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel icon = new JLabel("\uD83C\uDF5A");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 42));
        right.add(icon);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // ---------------------------------------------------------------
    // The single input page
    // ---------------------------------------------------------------

    // Builds the scrollable input page with its button row.
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

    // The calculator-only and full-plan radio buttons.
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

    // Section 1: age, gender, height and weight.
    private JPanel createBodySection() {
        JPanel section = createSection("1. Body Information");

        addLabel(section, "Age:", 0, 0);
        ageField = new PlaceholderTextField(6);
        ageField.setPlaceholder("Enter age");
        addControl(section, ageField, 1, 0, 1);

        addLabel(section, "Gender:", 2, 0);
        genderCombo = new JComboBox<>(UserProfile.Gender.values());
        genderCombo.setSelectedItem(UserProfile.Gender.FEMALE);
        addControl(section, genderCombo, 3, 0, 1);

        addLabel(section, "Height (cm):", 0, 1);
        heightField = new PlaceholderTextField(6);
        heightField.setPlaceholder("Enter height (cm)");
        addControl(section, heightField, 1, 1, 1);

        addLabel(section, "Weight (kg):", 2, 1);
        weightField = new PlaceholderTextField(6);
        weightField.setPlaceholder("Enter weight (kg)");
        addControl(section, weightField, 3, 1, 1);

        return section;
    }

    // Section 2: activity level, goal and the live calorie estimate.
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

    // Section 3: allergies, dislikes, diet and the courses to include.
    private JPanel createMealSection() {
        JPanel section = createSection("3. Food Preferences");

        // Course moods live on the meal planner page now, so this section starts
        // with allergies.
        addLabel(section, "Allergies:", 0, 0);
        String[] allergies = {"Peanuts", "Shellfish", "Dairy", "Gluten", "Soy", "Eggs"};
        allergyChecks = new JCheckBox[allergies.length];
        JPanel allergyPanel = new JPanel(new GridLayout(1, 6, 6, 0));
        allergyPanel.setOpaque(false);
        for (int i = 0; i < allergies.length; i++) {
            allergyChecks[i] = new JCheckBox(allergies[i]);
            allergyChecks[i].setOpaque(false);
            allergyPanel.add(allergyChecks[i]);
        }
        addControl(section, allergyPanel, 1, 0, 3);

        addLabel(section, "Dislikes:", 0, 1);
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
        addControl(section, dislikePanel, 1, 1, 3);

        addLabel(section, "Diet:", 0, 2);
        JPanel dietPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        dietPanel.setOpaque(false);
        vegetarianCheck = new JCheckBox("Vegetarian");
        vegetarianCheck.setOpaque(false);
        dietPanel.add(vegetarianCheck);
        addControl(section, dietPanel, 1, 2, 3);

        addLabel(section, "Courses today:", 0, 3);
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
        addControl(section, coursePanel, 1, 3, 3);

        return section;
    }

    // Reset and Generate buttons at the bottom of the form.
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

    // A titled, bordered box used for one form section.
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

    // GridBag constraints for one cell inside a section.
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

    // Adds a fixed-width label at one grid position.
    private void addLabel(JPanel section, String text, int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 5);

        JLabel label = new JLabel(text);
        label.setPreferredSize(new Dimension(110, 24));
        section.add(label, c);
    }

    // Adds an input widget next to its label.
    private void addControl(JPanel section, JComponent component, int x, int y, int width) {
        GridBagConstraints c = sectionConstraint(x, y, width);
        c.weightx = 1;
        section.add(component, c);
    }

    // Hides the food section in calculator-only mode and renames the button.
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

    // Recalculates the estimate line while the user types.
    private void updateCaloriePreview() {
        if (caloriePreviewLabel == null) return;

        if (ageField.getText().trim().isEmpty()
                || heightField.getText().trim().isEmpty()
                || weightField.getText().trim().isEmpty()) {
            caloriePreviewLabel.setText("Fill the body fields to see the calorie estimate.");
            caloriePreviewLabel.setForeground(Color.GRAY);
            return;
        }

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
                caloriePreviewLabel.setText("Enter valid body values to see the estimate.");
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

    // Empty results card shown before the first plan exists.
    private JPanel createPlaceholderResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Results will appear here", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 16));
        label.setForeground(Color.GRAY);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    // Swaps in a new results page and displays it.
    private void showResults(JPanel panel) {
        cardPanel.remove(resultsCard);
        resultsCard = panel;
        cardPanel.add(resultsCard, CARD_RESULTS);
        cardLayout.show(cardPanel, CARD_RESULTS);
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    // Goes back to the input page.
    void showForm() {
        cardLayout.show(cardPanel, CARD_FORM);
        formPanel.revalidate();
        formPanel.repaint();
    }

    // Returns from the summary to the planner page.
    void showResultsCard() {
        cardLayout.show(cardPanel, CARD_RESULTS);
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    // Builds the summary page from the current plan and shows it.
    void showSummary() {
        if (currentPlan == null) return;

        cardPanel.remove(summaryCard);
        summaryCard = new StarterSummaryPanel(currentPlan, this);
        cardPanel.add(summaryCard, CARD_SUMMARY);
        cardLayout.show(cardPanel, CARD_SUMMARY);
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    /** "Start Next Day": keep the inputs, hide today's picks, show a new plan. */
    void startNextDay() {
        if (currentPlan == null) return;
        currentPlan = generator.recordPicksAndAdvance(currentPlan);
        showResults(new StarterResultsPanel(currentPlan, generator, this));
    }

    // Clears every input and returns to a blank form.
    void resetForm() {
        ageField.setText("");
        heightField.setText("");
        weightField.setText("");
        genderCombo.setSelectedItem(UserProfile.Gender.FEMALE);
        activityCombo.setSelectedItem(UserProfile.ActivityLevel.LIGHT);
        goalCombo.setSelectedItem(UserProfile.Goal.MAINTAIN);

        for (int i = 0; i < allergyChecks.length; i++) {
            allergyChecks[i].setSelected(false);
        }
        for (int i = 0; i < dislikeChecks.length; i++) {
            dislikeChecks[i].setSelected(false);
        }

        vegetarianCheck.setSelected(false);
        includeBreakfastCheck.setSelected(true);
        includeLunchCheck.setSelected(true);
        includeDinnerCheck.setSelected(true);
        includeSnackCheck.setSelected(true);

        fullPlanRadio.setSelected(true);
        calculatorOnlyMode = false;
        currentPlan = null;
        currentProfile = null;

        updateMealSectionVisibility();
        updateCaloriePreview();
        showForm();
    }

    // ---------------------------------------------------------------
    // Validation and generation
    // ---------------------------------------------------------------

    // Checks the typed numbers and the course checkboxes before generating.
    private boolean validateForm() {
        if (ageField.getText().trim().isEmpty()
                || heightField.getText().trim().isEmpty()
                || weightField.getText().trim().isEmpty()) {
            showError("Please enter your age, height and weight.");
            return false;
        }

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
            if (!includeBreakfastCheck.isSelected() && !includeLunchCheck.isSelected()
                    && !includeDinnerCheck.isSelected() && !includeSnackCheck.isSelected()) {
                showError("Please include at least one course.");
                return false;
            }
        }
        return true;
    }

    // Validates the form, then shows either the calorie card or the planner.
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

    // Copies every widget value into a new UserProfile.
    private UserProfile getProfileFromForm() {
        UserProfile profile = new UserProfile();
        profile.setAge(Integer.parseInt(ageField.getText().trim()));
        profile.setGender((UserProfile.Gender) genderCombo.getSelectedItem());
        profile.setHeightCm(Double.parseDouble(heightField.getText().trim()));
        profile.setWeightKg(Double.parseDouble(weightField.getText().trim()));
        profile.setActivityLevel((UserProfile.ActivityLevel) activityCombo.getSelectedItem());
        profile.setGoal((UserProfile.Goal) goalCombo.getSelectedItem());

        ArrayList<String> allergies = new ArrayList<>();
        for (int i = 0; i < allergyChecks.length; i++) {
            if (allergyChecks[i].isSelected()) {
                allergies.add(allergyChecks[i].getText().toLowerCase());
            }
        }
        profile.setAllergies(allergies.toArray(new String[0]));

        ArrayList<String> dislikes = new ArrayList<>();
        for (int i = 0; i < dislikeChecks.length; i++) {
            if (dislikeChecks[i].isSelected()) {
                dislikes.add(dislikeChecks[i].getText().toLowerCase());
            }
        }
        profile.setDislikes(dislikes.toArray(new String[0]));

        profile.setVegetarian(vegetarianCheck.isSelected());
        profile.setHalal(false);

        profile.setIncludeBreakfast(includeBreakfastCheck.isSelected());
        profile.setIncludeLunch(includeLunchCheck.isSelected());
        profile.setIncludeDinner(includeDinnerCheck.isSelected());
        profile.setIncludeSnack(includeSnackCheck.isSelected());

        return profile;
    }

    // Shows one error dialog.
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    // Text card with BMR, TDEE, target, macros, BMI and the safety check.
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

    // Starts the window on the Swing event thread.
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
