package mealprep.starter.gui;

import mealprep.starter.model.FoodItem;
import mealprep.starter.model.MealOption;
import mealprep.starter.model.MealPlan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

// Displays the final summary of the meal plan, including the selected meals, their macros, and the total calories. It also has buttons to go back, edit inputs, start a new day, or reset the app.
public class StarterSummaryPanel extends JPanel {

    private final MealPlan plan;                                        //the meal plan that was generated and selected by the user, which contains the selected meals and their nutritional information
    private final StarterGUI parentFrame;                               //the main frame that contains this panel, so we can tell it to switch cards when the user clicks a button
    private final DecimalFormat df = new DecimalFormat("#,##0");        //formats numbers with thousands separators, e.g. 1,750

    // Lays out the header, the meal list and the footer.
    public StarterSummaryPanel(MealPlan plan, StarterGUI parent) {
        this.plan = plan;
        this.parentFrame = parent;

        setLayout(new BorderLayout(0, 12));                              //BorderLayout gaps of 12px between them
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));      //outer padding: top, left, bottom, right
        setBackground(new Color(245, 247, 250));                         //light gray background color

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMealList(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    // Day number, subtitle and the daily target.
    private JPanel buildHeader() {                                        //top section: page title on the left, target calories on the right
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);                                          //transparent background so the parent panel's background shows through

        //title at the top
        JLabel title = new JLabel("Day " + plan.getUserProfile().getDay() + " Result");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(41, 128, 185));

        //subtitle below the title
        JLabel subtitle = new JLabel("Calories and the meals you picked.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(Color.GRAY);

        //left side: title and subtitle
        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(title);
        left.add(subtitle);

        //target calories
        JLabel target = new JLabel("Target: " + df.format(plan.getTargetCalories()) + " kcal/day");
        target.setFont(new Font("SansSerif", Font.BOLD, 15));
        target.setForeground(new Color(41, 128, 185));

        header.add(left, BorderLayout.WEST);
        header.add(target, BorderLayout.EAST);
        return header;
    }

    //middle section: list of meals, each with its name, portion size, macros, and calories
    private JScrollPane buildMealList() {
        ScrollablePanel list = new ScrollablePanel();                       //a custom panel that can scroll its content
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));              //vertical layout for the meal rows
        list.setBackground(Color.WHITE);
        list.setBorder(BorderFactory.createCompoundBorder(                  //outer border: light gray line and inner padding
                BorderFactory.createLineBorder(new Color(205, 215, 230)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        for (FoodItem.MealType type : FoodItem.MealType.values()) {         //loop through each meal type (breakfast, lunch, dinner, snacks) and create a row for it
            list.add(createMealRow(type));
            list.add(Box.createVerticalStrut(6));
        }

        //wrap the list in a scroll pane so it can scroll if there are too many meals to fit on the screen
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    //one row: course name | meal details | calories
    private JPanel createMealRow(FoodItem.MealType type) {
        JPanel row = new JPanel(new BorderLayout(12, 4));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 242)),
                BorderFactory.createEmptyBorder(8, 4, 8, 4)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        //left: course name, coloured by course
        JLabel course = new JLabel(MealPlan.getSlotName(type).toUpperCase());
        course.setFont(new Font("SansSerif", Font.BOLD, 13));
        course.setForeground(colorFor(type).darker());
        course.setPreferredSize(new Dimension(110, 24));
        row.add(course, BorderLayout.WEST);

        //case 1: this course was skipped, so there is no dish to show
        if (!plan.isSlotEnabled(type)) {
            JLabel skipped = new JLabel("Skipped - its calorie share was added to the other courses.");
            skipped.setFont(new Font("SansSerif", Font.ITALIC, 12));
            skipped.setForeground(new Color(150, 80, 0));
            row.add(skipped, BorderLayout.CENTER);
            return row;
        }

        //case 2: this course is enabled, but no dish was selected (should not happen in normal use)
        MealOption picked = plan.getSelectedOption(type);
        if (picked == null) {
            JLabel none = new JLabel("No meal selected.");
            none.setFont(new Font("SansSerif", Font.ITALIC, 12));
            none.setForeground(Color.RED);
            row.add(none, BorderLayout.CENTER);
            return row;
        }

        //case 3: this course is enabled and a dish was selected
        JPanel info = new JPanel(new GridLayout(2, 1, 2, 2));
        info.setOpaque(false);

        String nameText = picked.getName();     
        if (picked.isAddedBySearch()) {
            nameText += "   [logged]";
        }
        JLabel name = new JLabel(nameText);         //meal name with portion size, e.g. "Grilled Chicken x2"
        name.setFont(new Font("SansSerif", Font.BOLD, 14));

        //serving size and scaled macros, e.g. "1 cup x2 | P:31.0g C:45.0g F:12.0g"
        JLabel detail = new JLabel(String.format("%s  |  P:%.1fg  C:%.1fg  F:%.1fg",
                picked.getServingSize(), picked.getProtein(), picked.getCarbs(), picked.getFat()));
        detail.setFont(new Font("SansSerif", Font.PLAIN, 11));
        detail.setForeground(Color.DARK_GRAY);

        info.add(name);
        info.add(detail);
        row.add(info, BorderLayout.CENTER);

        //right: calories for the meal
        JLabel calories = new JLabel(df.format(picked.getCalories()) + " kcal");
        calories.setFont(new Font("SansSerif", Font.BOLD, 14));
        calories.setForeground(new Color(41, 128, 185));
        row.add(calories, BorderLayout.EAST);

        return row;
    }

    //bottom section: totals, status message, and the three buttons
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);

        int selected = plan.getSelectedCalories();
        int difference = selected - plan.getTargetCalories();       //negative = under target
        String status = plan.getCalorieStatus();

        //4 rows stacked: totals line, macro line, status, disclaimer
        JPanel totals = new JPanel(new GridLayout(4, 1, 2, 2));
        totals.setOpaque(false);

        //%+d always prints the sign, so you get "+120" or "-80"
        //the totals line shows the target calories, the picked calories, and the difference
        JLabel totalLine = new JLabel(String.format(
                "Target: %s kcal   |   Picked: %s kcal   |   Difference: %+d kcal",
                df.format(plan.getTargetCalories()), df.format(selected), difference));
        totalLine.setFont(new Font("SansSerif", Font.BOLD, 14));

        //the macro line shows the total protein, carbs, and fat for the selected meals
        JLabel macroLine = new JLabel(String.format(
                "Protein: %.1fg   |   Carbs: %.1fg   |   Fat: %.1fg",
                plan.getSelectedProtein(), plan.getSelectedCarbs(), plan.getSelectedFat()));
        macroLine.setFont(new Font("SansSerif", Font.PLAIN, 12));
        macroLine.setForeground(Color.DARK_GRAY);

        String statusText = status;
        if (plan.countLockedCourses() > 0) {
            statusText += "   |   " + plan.countLockedCourses() + " logged meal(s)";
        }

        //the status line shows whether the picked calories are on target, over, or under
        JLabel statusLine = new JLabel(statusText);
        statusLine.setFont(new Font("SansSerif", Font.BOLD, 12));
        if (status.startsWith("On target")) {
            statusLine.setForeground(new Color(39, 174, 96));
        } else if (difference > 0) {
            statusLine.setForeground(new Color(231, 76, 60));
        } else {
            statusLine.setForeground(new Color(230, 126, 34));
        }

        //the disclaimer line shows a note about the estimates being for educational purposes only
        JLabel note = new JLabel("Estimates are for an educational project, not medical advice.");
        note.setFont(new Font("SansSerif", Font.ITALIC, 11));
        note.setForeground(Color.GRAY);

        //add the 4 rows to the totals panel, which is added to the footer panel
        totals.add(totalLine);
        totals.add(macroLine);
        totals.add(statusLine);
        totals.add(note);
        footer.add(totals, BorderLayout.NORTH);

        //the buttons panel has 3 buttons: back to options, edit inputs, and start over(new)
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        buttons.setOpaque(false);

        JButton backBtn = new JButton("Back to Options");
        backBtn.setPreferredSize(new Dimension(150, 36));
        backBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parentFrame.showResultsCard();
            }
        });

        JButton editBtn = new JButton("Edit Inputs");
        editBtn.setPreferredSize(new Dimension(120, 36));
        editBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parentFrame.showForm();
            }
        });

        JButton nextDayBtn = new JButton("Start Next Day");
        nextDayBtn.setPreferredSize(new Dimension(150, 36));
        StarterGUI.stylePrimaryButton(nextDayBtn);
        nextDayBtn.setToolTipText("Keep your inputs, hide today's meals, and plan a new day.");
        nextDayBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parentFrame.startNextDay();
            }
        });

        JButton newBtn = new JButton("Start Over");
        newBtn.setPreferredSize(new Dimension(120, 36));
        newBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parentFrame.resetForm();
            }
        });

        //add the buttons to the buttons panel, which is added to the footer panel
        buttons.add(backBtn);
        buttons.add(editBtn);
        buttons.add(nextDayBtn);
        buttons.add(newBtn);
        footer.add(buttons, BorderLayout.SOUTH);
        return footer;
    }

    //returns a background colour for each course type, so the user can quickly identify them visually
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
