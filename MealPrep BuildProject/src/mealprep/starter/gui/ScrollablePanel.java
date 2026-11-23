package mealprep.starter.gui;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * A reusable panel for content displayed inside a {@code JScrollPane}.
 *
 * <p>In the MealPrep app, this lets form or meal-plan content keep its
 * normal height while matching the available window width. That prevents
 * the right side of a wide grid layout from being clipped when the window
 * is resized.</p>
 */
public class ScrollablePanel extends JPanel implements Scrollable {

    // Creates an empty panel using Swing's default layout manager.
    public ScrollablePanel() {
        super();
    }

    // Creates a panel using the layout manager supplied by the caller. @param layout the layout manager used to arrange this panel's children
    public ScrollablePanel(LayoutManager layout) {
        super(layout);
    }

    // Supplies the initial size that the scroll pane should use for this panel. @return this panel's preferred size
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    // Controls the smallest movement when the user scrolls by an arrow or key.
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    // Controls the movement for a page-style scroll.
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 160;
    }

    // Makes the panel stretch to the scroll pane's available width. <p>This is the key behavior for the app: grid columns follow the window width instead of keeping a wider preferred width that could be clipped.</p>
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    // Allows the panel to grow taller than the visible viewport when needed.
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
