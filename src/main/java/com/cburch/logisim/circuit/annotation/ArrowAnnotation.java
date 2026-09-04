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
import java.awt.Polygon;
import java.awt.RenderingHints;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ArrowAnnotation implements AnnotationItem {
  private final int x1;
  private final int y1;
  private final int x2;
  private final int y2;
  private final Color color;
  private final int strokeWidth;
  private final Bounds bounds;

  public ArrowAnnotation(int x1, int y1, int x2, int y2, Color color, int strokeWidth) {
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
    this.color = color;
    this.strokeWidth = Math.max(1, strokeWidth);

    final int minX = Math.min(x1, x2);
    final int minY = Math.min(y1, y2);
    final int maxX = Math.max(x1, x2);
    final int maxY = Math.max(y1, y2);
    final int pad = Math.max(14, this.strokeWidth * 4);
    this.bounds = Bounds.create(minX - pad, minY - pad, (maxX - minX) + pad * 2, (maxY - minY) + pad * 2);
  }

  @Override
  public void draw(Graphics2D g2) {
    final var oldStroke = g2.getStroke();
    final var oldColor = g2.getColor();
    final var oldHints = g2.getRenderingHints();

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(color);
    g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    final double dx = x2 - x1;
    final double dy = y2 - y1;
    final double dist = Math.sqrt(dx * dx + dy * dy);

    if (dist < 2) {
      g2.drawLine(x1, y1, x2, y2);
      g2.setStroke(oldStroke);
      g2.setColor(oldColor);
      g2.setRenderingHints(oldHints);
      return;
    }

    final double arrowHeadLen = Math.min(dist * 0.7, Math.max(12.0, strokeWidth * 3.5));
    final double arrowAngle = Math.PI / 6.0; // 30 degrees
    final double theta = Math.atan2(dy, dx);

    // Draw main line (stop slightly before x2, y2 so the stroke doesn't poke out of arrowhead)
    final double shorten = Math.min(dist, arrowHeadLen * 0.5);
    final int lineEndX = (int) Math.round(x2 - shorten * Math.cos(theta));
    final int lineEndY = (int) Math.round(y2 - shorten * Math.sin(theta));
    g2.drawLine(x1, y1, lineEndX, lineEndY);

    // Arrowhead points
    final int leftX = (int) Math.round(x2 - arrowHeadLen * Math.cos(theta - arrowAngle));
    final int leftY = (int) Math.round(y2 - arrowHeadLen * Math.sin(theta - arrowAngle));
    final int rightX = (int) Math.round(x2 - arrowHeadLen * Math.cos(theta + arrowAngle));
    final int rightY = (int) Math.round(y2 - arrowHeadLen * Math.sin(theta + arrowAngle));

    final var headPoly = new Polygon(new int[] {x2, leftX, rightX}, new int[] {y2, leftY, rightY}, 3);
    g2.fill(headPoly);

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
    final double rEff = radius + strokeWidth / 2.0;
    final double rSq = rEff * rEff;
    if (distToSegmentSq(px, py, x1, y1, x2, y2) <= rSq) return true;

    // Check hit near head
    final double headHitRadius = Math.max(radius + 6.0, strokeWidth * 2.0);
    final double dx = px - x2;
    final double dy = py - y2;
    return (dx * dx + dy * dy) <= headHitRadius * headHitRadius;
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
    final var elt = doc.createElement("arrow");
    elt.setAttribute("x1", String.valueOf(x1));
    elt.setAttribute("y1", String.valueOf(y1));
    elt.setAttribute("x2", String.valueOf(x2));
    elt.setAttribute("y2", String.valueOf(y2));
    elt.setAttribute("color", String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
    elt.setAttribute("width", String.valueOf(strokeWidth));
    return elt;
  }

  public static ArrowAnnotation fromXmlElement(Element elt) {
    int x1 = Integer.parseInt(elt.getAttribute("x1"));
    int y1 = Integer.parseInt(elt.getAttribute("y1"));
    int x2 = Integer.parseInt(elt.getAttribute("x2"));
    int y2 = Integer.parseInt(elt.getAttribute("y2"));
    Color color = Color.RED;
    final var colorStr = elt.getAttribute("color");
    if (colorStr != null && !colorStr.isEmpty()) {
      try {
        color = Color.decode(colorStr);
      } catch (NumberFormatException ignored) {
        // keep default color
      }
    }
    int width = 2;
    try {
      width = Integer.parseInt(elt.getAttribute("width"));
    } catch (NumberFormatException ignored) {
      // keep default width
    }
    return new ArrowAnnotation(x1, y1, x2, y2, color, width);
  }

  public int getX1() {
    return x1;
  }

  public int getY1() {
    return y1;
  }

  public int getX2() {
    return x2;
  }

  public int getY2() {
    return y2;
  }

  public Color getColor() {
    return color;
  }

  public int getStrokeWidth() {
    return strokeWidth;
  }
}
