/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.annotation;

import com.cburch.logisim.data.Bounds;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TextNoteAnnotation implements AnnotationItem {
  private final int x;
  private final int y;
  private int width;
  private int height;
  private String text;
  private final Color textColor;
  private final Color bgColor;
  private final Color borderColor;
  private final Font font;
  private Bounds bounds;

  public static final Color DEFAULT_NOTE_BG = new Color(255, 249, 196, 230); // Soft sticky yellow
  public static final Color DEFAULT_NOTE_BORDER = new Color(230, 210, 140, 240);

  public TextNoteAnnotation(int x, int y, String text, Color textColor, Color bgColor) {
    this(x, y, 0, 0, text, textColor, bgColor, null);
  }

  public TextNoteAnnotation(
      int x, int y, int width, int height,
      String text, Color textColor, Color bgColor, Font font) {
    this.x = x;
    this.y = y;
    this.text = text != null ? text : "";
    this.textColor = textColor != null ? textColor : Color.BLACK;
    this.bgColor = bgColor != null ? bgColor : DEFAULT_NOTE_BG;
    this.borderColor = DEFAULT_NOTE_BORDER;
    this.font = font != null ? font : new Font("SansSerif", Font.PLAIN, 13);
    this.width = width > 0 ? width : 120;
    this.height = height > 0 ? height : 60;
    this.bounds = Bounds.create(x, y, this.width + 4, this.height + 4);
  }

  @Override
  public void draw(Graphics2D g2) {
    final var oldFont = g2.getFont();
    final var oldColor = g2.getColor();
    final var oldHints = g2.getRenderingHints();

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Compute dimensions based on text if not explicitly set
    g2.setFont(font);
    final var fm = g2.getFontMetrics();
    final var lines = text.split("\n", -1);
    int textMaxW = 40;
    for (final var line : lines) {
      textMaxW = Math.max(textMaxW, fm.stringWidth(line));
    }
    final int lineH = fm.getHeight();
    final int autoH = Math.max(36, lines.length * lineH + 16);
    final int autoW = Math.max(width, textMaxW + 20);
    this.width = autoW;
    this.height = autoH;
    this.bounds = Bounds.create(x, y, width + 4, height + 4);

    // Drop shadow
    g2.setColor(new Color(0, 0, 0, 30));
    g2.fillRoundRect(x + 2, y + 2, width, height, 8, 8);

    // Background card
    if (bgColor != null) {
      g2.setColor(bgColor);
      g2.fillRoundRect(x, y, width, height, 8, 8);
    }

    // Border
    if (borderColor != null) {
      g2.setColor(borderColor);
      g2.setStroke(new BasicStroke(1.2f));
      g2.drawRoundRect(x, y, width, height, 8, 8);
    }

    // Text lines
    g2.setColor(textColor);
    int ty = y + 8 + fm.getAscent();
    for (final var line : lines) {
      g2.drawString(line, x + 10, ty);
      ty += lineH;
    }

    g2.setFont(oldFont);
    g2.setColor(oldColor);
    g2.setRenderingHints(oldHints);
  }

  @Override
  public Bounds getBounds() {
    return bounds;
  }

  @Override
  public boolean intersects(int px, int py, int radius) {
    final int rx1 = x;
    final int ry1 = y;
    final int rx2 = x + width;
    final int ry2 = y + height;
    final int nearestX = Math.max(rx1, Math.min(px, rx2));
    final int nearestY = Math.max(ry1, Math.min(py, ry2));
    final int dx = px - nearestX;
    final int dy = py - nearestY;
    return (dx * dx + dy * dy) <= radius * radius;
  }

  @Override
  public Element toXmlElement(Document doc) {
    final var elt = doc.createElement("note");
    elt.setAttribute("x", String.valueOf(x));
    elt.setAttribute("y", String.valueOf(y));
    elt.setAttribute("w", String.valueOf(width));
    elt.setAttribute("h", String.valueOf(height));
    elt.setAttribute("text", text);
    if (textColor != null) {
      elt.setAttribute("color", String.format("#%02x%02x%02x", textColor.getRed(), textColor.getGreen(), textColor.getBlue()));
    }
    if (bgColor != null) {
      elt.setAttribute("bg", String.format("#%02x%02x%02x%02x", bgColor.getAlpha(), bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue()));
    }
    return elt;
  }

  public static TextNoteAnnotation fromXmlElement(Element elt) {
    int x = Integer.parseInt(elt.getAttribute("x"));
    int y = Integer.parseInt(elt.getAttribute("y"));
    int w = 120;
    try {
      w = Integer.parseInt(elt.getAttribute("w"));
    } catch (NumberFormatException ignored) {
      // keep default
    }
    int h = 60;
    try {
      h = Integer.parseInt(elt.getAttribute("h"));
    } catch (NumberFormatException ignored) {
      // keep default
    }
    String text = elt.getAttribute("text");
    Color textColor = Color.BLACK;
    final var colorStr = elt.getAttribute("color");
    if (colorStr != null && !colorStr.isEmpty()) {
      try {
        textColor = Color.decode(colorStr);
      } catch (NumberFormatException ignored) {
        // keep default
      }
    }
    Color bg = DEFAULT_NOTE_BG;
    final var bgStr = elt.getAttribute("bg");
    if (bgStr != null && !bgStr.isEmpty()) {
      try {
        if (bgStr.startsWith("#") && bgStr.length() == 9) {
          int argb = (int) Long.parseLong(bgStr.substring(1), 16);
          bg = new Color(argb, true);
        } else {
          bg = Color.decode(bgStr);
        }
      } catch (NumberFormatException ignored) {
        // keep default
      }
    }
    return new TextNoteAnnotation(x, y, w, h, text, textColor, bg, null);
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text != null ? text : "";
  }

  public Color getTextColor() {
    return textColor;
  }

  public Color getBgColor() {
    return bgColor;
  }
}
