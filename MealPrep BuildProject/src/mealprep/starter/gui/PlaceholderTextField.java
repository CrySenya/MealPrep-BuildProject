package mealprep.starter.gui;

import javax.swing.JTextField;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * A text field that paints gray hint text while it is empty.
 * Swing has no built-in placeholder, so we draw it ourselves.
 */
public class PlaceholderTextField extends JTextField {

    private String placeholder = "";

    // Empty constructor, required because Swing expects a no-argument one.
    public PlaceholderTextField() {
        super();
    }

    // Creates a field with the given number of columns.
    public PlaceholderTextField(int columns) {
        super(columns);
    }

    // Sets the gray hint text and repaints so it appears at once.
    public void setPlaceholder(String text) {
        this.placeholder = text == null ? "" : text;
        repaint();
    }

    // Returns the gray hint text.
    public String getPlaceholder() {
        return placeholder;
    }

    // Draws the hint text while empty, then lets Swing paint the field itself.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (placeholder.isEmpty() || getText().length() > 0) {
            return;
        }
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(160, 168, 178));
        g2.setFont(getFont());

        Insets insets = getInsets();
        FontMetrics fm = g2.getFontMetrics();
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(placeholder, insets.left + 4, y);
        g2.dispose();
    }
}
