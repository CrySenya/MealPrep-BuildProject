package mealprep.starter.gui;

import mealprep.starter.algorithm.MealPlanGenerator;
import mealprep.starter.model.FoodItem;
import mealprep.starter.model.MealOption;
import mealprep.starter.model.MealPlan;
import mealprep.starter.model.UserProfile;
import mealprep.starter.model.SearchResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;

// The results page. It shows the generated meals, their calories and macros, and lets the user pick or search for different meals.
public class StarterResultsPanel extends JPanel {

    private final MealPlan plan;
    private final MealPlanGenerator generator;
    private final StarterGUI parentFrame;
    private final DecimalFormat df = new DecimalFormat("#,##0");

    private JLabel summaryLabel;
    private JLabel macroLabel;
    private JLabel statusLabel;

    // Stores the plan and generator, then builds the page.
    public StarterResultsPanel(MealPlan plan, MealPlanGenerator generator, StarterGUI parent) {
        this.plan = plan;
        this.generator = generator;
        this.parentFrame = parent;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    // Top bar, the two rows of course cards, and the bottom bar.
    private void buildUI() {
        add(buildTopBar(), BorderLayout.NORTH);

        // Two rows of two courses. The cards are added directly, with no inner
        // scroll panes, so the outer scroll reaches the true bottom.
        ScrollablePanel rows = new ScrollablePanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        FoodItem.MealType[] types = FoodItem.MealType.values();
        for (int r = 0; r < types.length / 2; r++) {
            JPanel row = new JPanel(new GridLayout(1, 2, 15, 15));
            row.setOpaque(false);
            for (int c = 0; c < 2; c++) {
                row.add(new MealSlotPanel(types[r * 2 + c]));
            }
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            rows.add(row);
            if (r < types.length / 2 - 1) {
                rows.add(Box.createVerticalStrut(15));
            }
        }

        JScrollPane scrollPane = new JScrollPane(rows);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        add(buildBottomBar(), BorderLayout.SOUTH);
    }

    // Day, target, selected and remaining calories plus the macro line.
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

    // Edit Inputs and Show My Result buttons.
    private JPanel buildBottomBar() {
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)));

        JButton editBtn = new JButton("Edit Inputs");
        editBtn.setPreferredSize(new Dimension(140, 36));
        editBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parentFrame.showForm();
            }
        });

        JButton resultBtn = new JButton("Show My Result");
        resultBtn.setPreferredSize(new Dimension(160, 36));
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

    // ---------------------------------------------------------------
    // Refresh / totals
    // ---------------------------------------------------------------

    // Rebuilds the whole page after the plan changed. cannot become stale on screen.
    private void refreshAll() {
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    // Refreshes the totals and colours the status line.
    private void updateSummary() {
        if (summaryLabel == null) return;

        int remaining = plan.getRemainingCalories();
        String remainingText = remaining < 0
                ? "-" + df.format(-remaining)
                : df.format(remaining);

        summaryLabel.setText(String.format(
                "Day %d   |   Daily Target: %s kcal   |   Selected: %s kcal   |   Remaining: %s kcal",
                plan.getUserProfile().getDay(), df.format(plan.getTargetCalories()),
                df.format(plan.getSelectedCalories()), remainingText));

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

        int logged = plan.countLockedCourses();
        if (logged > 0) {
            macroLabel.setText(macroLabel.getText() + "   |   Logged meals: " + logged);
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
    // Course actions
    // ---------------------------------------------------------------

    // Includes or skips one course and rebuilds the affected courses.
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

    // Marks one option as the pick and remembers the user chose it.
    private void selectOption(FoodItem.MealType type, MealOption chosen) {
        // A course may have only one selected option; other courses are
        // independent and can each have their own selection.
        ArrayList<MealOption> options = plan.getOptions(type);
        for (MealOption option : options) {
            option.setSelected(false);
        }
        chosen.setSelected(true);
        chosen.setUserPicked(true);
        refreshAll();
    }

    // Opens the summary page when at least one meal is picked.
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
        private final PlaceholderTextField searchField = new PlaceholderTextField(18);

        MealSlotPanel(FoodItem.MealType type) {
            this.type = type;
            boolean enabled = plan.isSlotEnabled(type);
            int budget = plan.getBudget(type);

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(colorFor(type));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(colorFor(type).darker(), 2),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            add(buildTitleRow(enabled, budget));
            add(Box.createVerticalStrut(6));

            if (!enabled) {
                JLabel note = new JLabel("<html><i>This course is skipped. Its calorie share<br>"
                        + "was added to the other courses.</i></html>");
                note.setFont(new Font("SansSerif", Font.PLAIN, 11));
                note.setForeground(new Color(120, 90, 40));
                note.setAlignmentX(Component.LEFT_ALIGNMENT);
                add(note);
                return;
            }

            add(buildMoodRow());
            add(Box.createVerticalStrut(6));
            add(buildSearchRow());
            add(Box.createVerticalStrut(8));

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

            add(Box.createVerticalStrut(8));
            add(buildNewOptionsRow());
        }

        // Course name checkbox and its calorie budget.
        private JPanel buildTitleRow(boolean enabled, int budget) {
            JCheckBox includeBox = new JCheckBox(
                    iconFor(type) + " " + MealPlan.getSlotName(type).toUpperCase(), enabled);
            includeBox.setFont(new Font("SansSerif", Font.BOLD, 13));
            includeBox.setOpaque(false);
            includeBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    toggleSlot(type, includeBox.isSelected());
                }
            });

            JLabel budgetLabel = new JLabel(enabled ? df.format(budget) + " kcal" : "SKIPPED");
            budgetLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            budgetLabel.setForeground(enabled ? Color.DARK_GRAY : new Color(150, 80, 0));

            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            row.add(includeBox, BorderLayout.WEST);
            row.add(budgetLabel, BorderLayout.EAST);
            return row;
        }

        // The per-course mood dropdown.
        private JPanel buildMoodRow() {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

            JLabel label = new JLabel("Mood:");
            label.setFont(new Font("SansSerif", Font.PLAIN, 11));
            label.setForeground(Color.GRAY);

            final JComboBox<UserProfile.DailyPreference> moodCombo =
                    new JComboBox<>(UserProfile.DailyPreference.values());
            moodCombo.setRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList<?> list, Object value,
                        int index, boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof UserProfile.DailyPreference) {
                        value = ((UserProfile.DailyPreference) value).getDisplayName();
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            moodCombo.setPreferredSize(new Dimension(190, 24));
            moodCombo.setSelectedItem(plan.getUserProfile().getCourseMood(type));
            moodCombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    UserProfile.DailyPreference pref =
                            (UserProfile.DailyPreference) moodCombo.getSelectedItem();
                    if (pref == null) return;
                    generator.applyCourseMood(plan, type, pref);
                    refreshAll();
                }
            });

            row.add(label);
            row.add(moodCombo);
            return row;
        }

        // The search field and its Search button.
        private JPanel buildSearchRow() {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            searchField.setPlaceholder("Search for a Meal");
            searchField.setToolTipText("Search any meal, including fast food, and log it.");
            searchField.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    openSearchDialog();
                }
            });

            JButton findBtn = new JButton("Search");
            findBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
            findBtn.setMargin(new Insets(1, 10, 1, 10));
            findBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    openSearchDialog();
                }
            });

            row.add(searchField);
            row.add(findBtn);
            return row;
        }

        // The per-course New Options button.
        private JPanel buildNewOptionsRow() {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            JButton newOptions = new JButton("New Options");
            newOptions.setFont(new Font("SansSerif", Font.PLAIN, 11));
            newOptions.setMargin(new Insets(2, 10, 2, 10));
            newOptions.setToolTipText("Keep the picked meal and suggest different options.");
            newOptions.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    generator.rerollSlot(plan, type);
                    refreshAll();
                }
            });

            row.add(newOptions);
            return row;
        }

        // Runs the ranked search and adds the chosen meal to this course.
        private void openSearchDialog() {
            String query = searchField.getText().trim();
            if (query.isEmpty()) {
                JOptionPane.showMessageDialog(StarterResultsPanel.this,
                        "Type a meal or brand name first.",
                        "Search", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // ranked search instead of the old substring search.
            ArrayList<SearchResult> matches =
                    generator.getFoodDatabase().searchRanked(query, type);

            if (matches.isEmpty()) {
                // offer a corrected word when one is close.
                String suggestion = generator.getFoodDatabase().suggestQuery(query);
                String message = "No " + MealPlan.getSlotName(type).toLowerCase()
                        + " match \"" + query + "\".";
                if (suggestion != null && !suggestion.isEmpty()) {
                    message = message + " Did you mean \"" + suggestion + "\"?";
                }
                JOptionPane.showMessageDialog(StarterResultsPanel.this,
                        message, "Search", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Each row is a SearchResult, so the score can be shown next to the name.
            DefaultListModel<SearchResult> model = new DefaultListModel<>();
            for (SearchResult result : matches) {
                model.addElement(result);
            }
            JList<SearchResult> list = new JList<>(model);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setVisibleRowCount(8);
            list.setSelectedIndex(0);

            // a small renderer that prints "name (score 0.87)".
            list.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList<?> list, Object value,
                        int index, boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof SearchResult) {
                        SearchResult result = (SearchResult) value;
                        value = result.getFoodItem().getName()
                                + "   (score " + String.format("%.2f", result.getScore()) + ")";
                    }
                    return super.getListCellRendererComponent(list, value, index,
                            isSelected, cellHasFocus);
                }
            });

            int choice = JOptionPane.showConfirmDialog(StarterResultsPanel.this,
                    new JScrollPane(list),
                    "Add to " + MealPlan.getSlotName(type) + " (" + matches.size() + " matches)",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (choice != JOptionPane.OK_OPTION) return;

            SearchResult chosen = list.getSelectedValue();
            if (chosen == null) return;

            generator.addSearchOption(plan, type, chosen.getFoodItem());
            generator.fitRemainingToCourses(plan);
            searchField.setText("");
            refreshAll();
        }

        // One meal card with its calories, macros, reason and Select button.
        private JPanel createOptionCard(MealOption option) {
            JPanel card = new JPanel(new BorderLayout(10, 5));
            card.setBackground(option.isSelected() ? new Color(200, 240, 200) : Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(option.isSelected()
                            ? new Color(100, 200, 100) : new Color(200, 200, 200), 2),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel left = new JPanel(new GridLayout(3, 1, 2, 2));
            left.setOpaque(false);

            JPanel nameRow = new JPanel(new BorderLayout(6, 0));
            nameRow.setOpaque(false);

            JLabel nameLabel = new JLabel(option.getName());
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            nameLabel.setToolTipText(option.getName());
            nameRow.add(nameLabel, BorderLayout.CENTER);

            if (option.isFastFood()) {
                JLabel brandTag = new JLabel("\uD83C\uDF54 " + option.getFoodItem().getBrand());
                brandTag.setFont(new Font("SansSerif", Font.BOLD, 10));
                brandTag.setForeground(new Color(192, 57, 43));
                nameRow.add(brandTag, BorderLayout.EAST);
            }

            JLabel calorieLabel = new JLabel(String.format(
                    "%s kcal  |  P:%.1fg  C:%.1fg  F:%.1fg  |  %s",
                    df.format(option.getCalories()), option.getProtein(), option.getCarbs(),
                    option.getFat(), option.getServingSize()));
            calorieLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            calorieLabel.setForeground(Color.DARK_GRAY);

            String reason = option.getReason();
            if (option.isAddedBySearch()) {
                reason = "Logged meal - real calories kept";
            }
            JLabel reasonLabel = new JLabel(reason);
            reasonLabel.setFont(new Font("SansSerif", Font.ITALIC, 10));
            reasonLabel.setForeground(option.isAddedBySearch()
                    ? new Color(41, 128, 185) : new Color(100, 100, 100));
            reasonLabel.setToolTipText(reason);

            left.add(nameRow);
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

        // Background colour for one course card.
        private Color colorFor(FoodItem.MealType type) {
            switch (type) {
                case BREAKFAST: return new Color(255, 243, 224);
                case LUNCH:     return new Color(232, 245, 233);
                case DINNER:    return new Color(239, 235, 255);
                case SNACK:     return new Color(255, 248, 230);
                default:        return Color.WHITE;
            }
        }

        // Small sun, plate, moon or snack icon for one course.
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
