package com.railway.view;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public final class Theme {
  public static final Color NAVY = new Color(17, 35, 57),
      TEAL = new Color(0, 119, 113),
      BG = new Color(244, 247, 250),
      MUTED = new Color(99, 114, 132);

  private Theme() {}

  public static void install() {
    FlatLightLaf.setup();
    UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
    UIManager.put("Button.arc", 12);
    UIManager.put("Component.arc", 10);
    UIManager.put("TextComponent.arc", 10);
    UIManager.put("Component.focusColor", TEAL);
    UIManager.put("Table.rowHeight", 42);
    UIManager.put("Table.selectionBackground", new Color(215, 239, 236));
    UIManager.put("Table.selectionForeground", NAVY);
    UIManager.put("Panel.background", BG);
    UIManager.put("ScrollPane.border", new LineBorder(new Color(224, 230, 236)));
  }

  public static JLabel label(String text, int size, Color color) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("Segoe UI", Font.PLAIN, size));
    label.setForeground(color);
    return label;
  }

  public static JButton button(String text, boolean primary) {
    JButton b = new JButton(text);
    b.setMargin(new Insets(11, 18, 11, 18));
    b.setFocusPainted(true);
    if (primary) {
      b.setBackground(TEAL);
      b.setForeground(Color.WHITE);
    }
    return b;
  }

  public static JPanel panel(LayoutManager layout) {
    JPanel p = new JPanel(layout);
    p.setOpaque(false);
    return p;
  }

  public static JPanel card() {
    JPanel p = new JPanel(new BorderLayout(12, 12));
    p.setBackground(Color.WHITE);
    p.setBorder(
        new CompoundBorder(
            new LineBorder(new Color(224, 230, 236)), new EmptyBorder(22, 24, 22, 24)));
    return p;
  }
}
