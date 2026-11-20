package mealprep.starter.gui;

import mealprep.starter.algorithm.MealPlanGenerator;
import mealprep.starter.model.FoodItem;
import mealprep.starter.model.MealOption;
import mealprep.starter.model.MealPlan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Results page: three options per course and a skip checkbox per course.
 *
 * There is no history, export, repair, or side dish in this edition.
 * The only action is "Show My Result", which opens the summary page.
 *
 * This panel is the Phase 12 bridge between the meal-plan model and the
 * Swing screen: it redraws itself after skips or selections so the display
 * always reflects the current plan.
 */
public class StarterResultsPanel extends JPanel {

    private final MealPlan plan;
    private final MealPlanGenerator generator;
    private final StarterGUI parentFrame;
    private final DecimalFormat df = new DecimalFormat("#,##0");

    private JLabel summaryLabel;
    private JLabel macroLabel;
    private JLabel statusLabel;

    public StarterResultsPanel(MealPlan plan, MealPlanGenerator generator, StarterGUI parent) {
        this.plan = plan;
        this.generator = generator;
        this.parentFrame = parent;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        add(buildTopBar(), BorderLayout.NORTH);

        ScrollablePanel slotsPanel = new ScrollablePanel(new GridLayout(2, 2, 15, 15));
        slotsPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            slotsPanel.add(wrapInScroll(new MealSlotPanel(type)));
        }

        JScrollPane scrollPane = new JScrollPane(slotsPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        add(buildBottomBar(), BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        topBar.setBackground(new Color(245, 248, 255));

        summaryLabel = new JLabel();
        summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        macroLabel = new JLabel();
        macroLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        macroLabel.setForeground(Color.GRAY);

        statusLabel = new JLabel();
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(summaryLabel);
        left.add(macroLabel);

        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        topBar.add(left, BorderLayout.CENTER);
        topBar.add(statusLabel, BorderLayout.SOUTH);

        updateSummary();
        return topBar;
    }

    private JPanel buildBottomBar() {
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)));

        JButton editBtn = new JButton("Edit Inputs");
        editBtn.setPreferredSize(new Dimension(150, 36));
        editBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parentFrame.showForm();
            }
        });

        JButton resultBtn = new JButton("Show My Result");
        resultBtn.setPreferredSize(new Dimension(170, 36));
        StarterGUI.stylePrimaryButton(resultBtn);
        resultBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showSummary();
            }
        });

        bottomBar.add(editBtn);
        bottomBar.add(resultBtn);
        return bottomBar;
    }

    private JScrollPane wrapInScroll(JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ---------------------------------------------------------------
    // Refresh / totals
    // ---------------------------------------------------------------

    private void refreshAll() {
        // Rebuild every card after the model changes so budgets and selections
        // cannot become stale on screen.
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    private void updateSummary() {
        if (summaryLabel == null) return;

        summaryLabel.setText(String.format("Daily Target: %s kcal   |   Selected: %s kcal",
                df.format(plan.getTargetCalories()), df.format(plan.getSelectedCalories())));

        macroLabel.setText(String.format("Protein: %.1fg  |  Carbs: %.1fg  |  Fat: %.1fg",
                plan.getSelectedProtein(), plan.getSelectedCarbs(), plan.getSelectedFat()));

        String skipped = "";
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (!plan.isSlotEnabled(type)) {
                skipped += (skipped.isEmpty() ? "" : ", ") + MealPlan.getSlotName(type);
            }
        }
        if (!skipped.isEmpty()) {
            macroLabel.setText(macroLabel.getText() + "   |   Skipped: " + skipped);
        }

        String status = plan.getCalorieStatus();
        if (status.startsWith("On target")) {
            statusLabel.setText("<html><font color='#27ae60'>" + status + "</font></html>");
        } else if (status.startsWith("+")) {
            statusLabel.setText("<html><font color='#e74c3c'>" + status + "</font></html>");
        } else {
            statusLabel.setText("<html><font color='#e67e22'>" + status + "</font></html>");
        }
    }

    // ---------------------------------------------------------------
    // Course switching and option selection
    // ---------------------------------------------------------------

    private void toggleSlot(FoodItem.MealType type, boolean include) {
        // Keep one course enabled so the user always has a usable plan.
        if (!include && plan.getEnabledSlotCount() == 1) {
            JOptionPane.showMessageDialog(this,
                    "At least one course must stay enabled.",
                    "Cannot skip everything", JOptionPane.WARNING_MESSAGE);
            refreshAll();
            return;
        }

        plan.setSlotEnabled(type, include);
        generator.regenerateEnabledSlots(plan);
        refreshAll();
    }

    private void selectOption(FoodItem.MealType type, MealOption chosen) {
        // A course may have only one selected option; other courses are
        // independent and can each have their own selection.
        ArrayList<MealOption> options = plan.getOptions(type);
        for (MealOption option : options) {
            option.setSelected(false);
        }
        chosen.setSelected(true);
        refreshAll();
    }

    private void showSummary() {
        int picks = 0;
        for (FoodItem.MealType type : FoodItem.MealType.values()) {
            if (plan.isSlotEnabled(type) && plan.getSelectedOption(type) != null) {
                picks++;
            }
        }

        if (picks == 0) {
            JOptionPane.showMessageDialog(this,
                    "Pick at least one meal first.",
                    "Nothing selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        parentFrame.showSummary();
    }

    // ---------------------------------------------------------------
    // One course card
    // ---------------------------------------------------------------

    private class MealSlotPanel extends ScrollablePanel {

        private final FoodItem.MealType type;

        MealSlotPanel(FoodItem.MealType type) {
            this.type = type;
            boolean enabled = plan.isSlotEnabled(type);
            int budget = plan.getBudget(type);

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(colorFor(type));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(colorFor(type).darker(), 2),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            // Each course card owns its skip checkbox, budget, and options.
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);

            JCheckBox includeBox = new JCheckBox(
                    iconFor(type) + " " + MealPlan.getSlotName(type).toUpperCase(), enabled);
            includeBox.setFont(new Font("SansSerif", Font.BOLD, 13));
            includeBox.setOpaque(false);
            includeBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    toggleSlot(type, includeBox.isSelected());
                }
            });

            JLabel budgetLabel = new JLabel(
                    enabled ? "Budget: " + df.format(budget) + " kcal" : "SKIPPED");
            budgetLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            budgetLabel.setForeground(enabled ? Color.DARK_GRAY : new Color(150, 80, 0));

            header.add(includeBox, BorderLayout.WEST);
            header.add(budgetLabel, BorderLayout.EAST);
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            add(header);
            add(Box.createVerticalStrut(8));

            if (!enabled) {
                JLabel note = new JLabel("<html><i>This course is skipped. Its calorie share<br>"
                        + "was added to the other courses.</i></html>");
                note.setFont(new Font("SansSerif", Font.PLAIN, 11));
                note.setForeground(new Color(120, 90, 40));
                note.setAlignmentX(Component.LEFT_ALIGNMENT);
                add(note);
                return;
            }

            ArrayList<MealOption> options = plan.getOptions(type);
            if (options.isEmpty()) {
                JLabel empty = new JLabel("No options available (check the filters)");
                empty.setFont(new Font("SansSerif", Font.ITALIC, 12));
                empty.setForeground(Color.RED);
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                add(empty);
            } else {
                for (int i = 0; i < options.size(); i++) {
                    add(createOptionCard(options.get(i)));
                    if (i < options.size() - 1) {
                        add(Box.createVerticalStrut(8));
                    }
                }
            }
        }

        private JPanel createOptionCard(MealOption option) {
            JPanel card = new JPanel(new BorderLayout(10, 5));
            card.setBackground(option.isSelected() ? new Color(200, 240, 200) : Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(option.isSelected()
                            ? new Color(100, 200, 100) : new Color(200, 200, 200), 2),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

            JPanel left = new JPanel(new GridLayout(3, 1, 2, 2));
            left.setOpaque(false);

            JLabel nameLabel = new JLabel(option.getName());
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            nameLabel.setToolTipText(option.getName());

            JLabel calorieLabel = new JLabel(String.format(
                    "%s kcal  |  P:%.1fg  C:%.1fg  F:%.1fg  |  %s",
                    df.format(option.getCalories()), option.getProtein(), option.getCarbs(),
                    option.getFat(), option.getServingSize()));
            calorieLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            calorieLabel.setForeground(Color.DARK_GRAY);

            JLabel reasonLabel = new JLabel(option.getReason());
            reasonLabel.setFont(new Font("SansSerif", Font.ITALIC, 10));
            reasonLabel.setForeground(new Color(100, 100, 100));
            reasonLabel.setToolTipText(option.getReason());

            left.add(nameLabel);
            left.add(calorieLabel);
            left.add(reasonLabel);

            JButton selectBtn = new JButton(option.isSelected() ? "Selected" : "Select");
            selectBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
            StarterGUI.stylePrimaryButton(selectBtn);
            if (option.isSelected()) {
                selectBtn.setBackground(new Color(46, 204, 113));
            }
            selectBtn.setPreferredSize(new Dimension(90, 30));
            selectBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectOption(type, option);
                }
            });

            card.add(left, BorderLayout.CENTER);
            card.add(selectBtn, BorderLayout.EAST);
            return card;
        }

        private Color colorFor(FoodItem.MealType type) {
            switch (type) {
                case BREAKFAST: return new Color(255, 243, 224);
                case LUNCH:     return new Color(232, 245, 233);
                case DINNER:    return new Color(239, 235, 255);
                case SNACK:     return new Color(255, 248, 230);
                default:        return Color.WHITE;
            }
        }

        private String iconFor(FoodItem.MealType type) {
            switch (type) {
                case BREAKFAST: return "\u2600";
                case LUNCH:     return "\u25D1";
                case DINNER:    return "\u263E";
                case SNACK:     return "\u25CF";
                default:        return "";
            }
        }
    }
}