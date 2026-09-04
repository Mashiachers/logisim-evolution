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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RectAnnotation implements AnnotationItem {
  private final int x;
  private final int y;
  private final int width;
  private final int height;
  private final Color strokeColor;
  private final int strokeWidth;
  private final Color fillColor;
  private final boolean isRounded;
  private final Bounds bounds;

  public RectAnnotation(
      int x1, int y1, int x2, int y2,
      Color strokeColor, int strokeWidth,
      Color fillColor, boolean isRounded) {
    this.x = Math.min(x1, x2);
    this.y = Math.min(y1, y2);
    this.width = Math.max(1, Math.abs(x2 - x1));
    this.height = Math.max(1, Math.abs(y2 - y1));
    this.strokeColor = strokeColor;
    this.strokeWidth = Math.max(1, strokeWidth);
    this.fillColor = fillColor;
    this.isRounded = isRounded;

    final int pad = this.strokeWidth / 2 + 1;
    this.bounds = Bounds.create(x - pad, y - pad, width + pad * 2, height + pad * 2);
  }

  public RectAnnotation(
      int x, int y, int width, int height,
      Color strokeColor, int strokeWidth,
      Color fillColor, boolean isRounded, boolean exactCoords) {
    this.x = x;
    this.y = y;
    this.width = Math.max(1, width);
    this.height = Math.max(1, height);
    this.strokeColor = strokeColor;
    this.strokeWidth = Math.max(1, strokeWidth);
    this.fillColor = fillColor;
    this.isRounded = isRounded;

    final int pad = this.strokeWidth / 2 + 1;
    this.bounds = Bounds.create(x - pad, y - pad, width + pad * 2, height + pad * 2);
  }

  @Override
  public void draw(Graphics2D g2) {
    final var oldStroke = g2.getStroke();
    final var oldColor = g2.getColor();
    final var oldHints = g2.getRenderingHints();

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    final int arc = isRounded ? 12 : 0;

    if (fillColor != null && fillColor.getAlpha() > 0) {
      g2.setColor(fillColor);
      if (isRounded) {
        g2.fillRoundRect(x, y, width, height, arc, arc);
      } else {
        g2.fillRect(x, y, width, height);
      }
    }

    if (strokeColor != null && strokeWidth > 0) {
      g2.setColor(strokeColor);
      g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      if (isRounded) {
        g2.drawRoundRect(x, y, width, height, arc, arc);
      } else {
        g2.drawRect(x, y, width, height);
      }
    }

    g2.setStroke(oldStroke);
    g2.setColor(oldColor);
    g2.setRenderingHints(oldHints);
  }

  @Override
  public Bounds getBounds() {
    return bounds;
  }

  @Override
  public boolean intersects(int px, int py, int radius) {
    // Check if circle touches or is inside the rectangle
    final int rx1 = x;
    final int ry1 = y;
    final int rx2 = x + width;
    final int ry2 = y + height;

    if (fillColor != null && fillColor.getAlpha() > 0) {
      // If filled, hitting inside or boundary is an intersection
      final int nearestX = Math.max(rx1, Math.min(px, rx2));
      final int nearestY = Math.max(ry1, Math.min(py, ry2));
      final int dx = px - nearestX;
      final int dy = py - nearestY;
      return (dx * dx + dy * dy) <= radius * radius;
    } else {
      // Hollow rectangle: check distance to 4 segments
      final double rEff = radius + strokeWidth / 2.0;
      final double rSq = rEff * rEff;
      if (distToSegmentSq(px, py, rx1, ry1, rx2, ry1) <= rSq) return true;
      if (distToSegmentSq(px, py, rx2, ry1, rx2, ry2) <= rSq) return true;
      if (distToSegmentSq(px, py, rx2, ry2, rx1, ry2) <= rSq) return true;
      if (distToSegmentSq(px, py, rx1, ry2, rx1, ry1) <= rSq) return true;
      return false;
    }
  }

  private static double distToSegmentSq(double px, double py, double x1, double y1, double x2, double y2) {
    double dx = x2 - x1;
    double dy = y2 - y1;
    if (dx == 0 && dy == 0) {
      double diffX = px - x1;
      double diffY = py - y1;
      return diffX * diffX + diffY * diffY;
    }
    double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
    if (t < 0) {
      double diffX = px - x1;
      double diffY = py - y1;
      return diffX * diffX + diffY * diffY;
    } else if (t > 1) {
      double diffX = px - x2;
      double diffY = py - y2;
      return diffX * diffX + diffY * diffY;
    } else {
      double projX = x1 + t * dx;
      double projY = y1 + t * dy;
      double diffX = px - projX;
      double diffY = py - projY;
      return diffX * diffX + diffY * diffY;
    }
  }

  @Override
  public Element toXmlElement(Document doc) {
    final var elt = doc.createElement("rect");
    elt.setAttribute("x", String.valueOf(x));
    elt.setAttribute("y", String.valueOf(y));
    elt.setAttribute("w", String.valueOf(width));
    elt.setAttribute("h", String.valueOf(height));
    if (strokeColor != null) {
      elt.setAttribute("stroke", String.format("#%02x%02x%02x", strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue()));
    }
    elt.setAttribute("width", String.valueOf(strokeWidth));
    if (fillColor != null) {
      elt.setAttribute("fill", String.format("#%02x%02x%02x%02x", fillColor.getAlpha(), fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue()));
    }
    elt.setAttribute("rounded", String.valueOf(isRounded));
    return elt;
  }

  public static RectAnnotation fromXmlElement(Element elt) {
    int x = Integer.parseInt(elt.getAttribute("x"));
    int y = Integer.parseInt(elt.getAttribute("y"));
    int w = Integer.parseInt(elt.getAttribute("w"));
    int h = Integer.parseInt(elt.getAttribute("h"));
    Color stroke = Color.RED;
    final var strokeStr = elt.getAttribute("stroke");
    if (strokeStr != null && !strokeStr.isEmpty()) {
      try {
        stroke = Color.decode(strokeStr);
      } catch (NumberFormatException ignored) {
        // keep default stroke
      }
    }
    int width = 2;
    try {
      width = Integer.parseInt(elt.getAttribute("width"));
    } catch (NumberFormatException ignored) {
      // keep default width
    }

    Color fill = null;
    final var fillStr = elt.getAttribute("fill");
    if (fillStr != null && !fillStr.isEmpty()) {
      try {
        if (fillStr.startsWith("#") && fillStr.length() == 9) {
          int argb = (int) Long.parseLong(fillStr.substring(1), 16);
          fill = new Color(argb, true);
        } else {
          fill = Color.decode(fillStr);
        }
      } catch (NumberFormatException ignored) {
        // keep null fill
      }
    }
    boolean rounded = Boolean.parseBoolean(elt.getAttribute("rounded"));
    return new RectAnnotation(x, y, w, h, stroke, width, fill, rounded, true);
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

  public Color getStrokeColor() {
    return strokeColor;
  }

  public int getStrokeWidth() {
    return strokeWidth;
  }

  public Color getFillColor() {
    return fillColor;
  }

  public boolean isRounded() {
    return isRounded;
  }
}
