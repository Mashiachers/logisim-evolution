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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FreehandStroke implements AnnotationItem {
  private final int[] xPoints;
  private final int[] yPoints;
  private final int nPoints;
  private final Color color;
  private final int strokeWidth;
  private final boolean isHighlighter;
  private Bounds bounds;

  public FreehandStroke(List<Integer> xs, List<Integer> ys, Color color, int strokeWidth, boolean isHighlighter) {
    if (xs.size() != ys.size() || xs.isEmpty()) {
      throw new IllegalArgumentException("Points list must be non-empty and matching in size");
    }
    this.nPoints = xs.size();
    this.xPoints = new int[nPoints];
    this.yPoints = new int[nPoints];
    for (int i = 0; i < nPoints; i++) {
      this.xPoints[i] = xs.get(i);
      this.yPoints[i] = ys.get(i);
    }
    this.color = color;
    this.strokeWidth = Math.max(1, strokeWidth);
    this.isHighlighter = isHighlighter;
    recomputeBounds();
  }

  public FreehandStroke(int[] xs, int[] ys, int nPoints, Color color, int strokeWidth, boolean isHighlighter) {
    if (nPoints <= 0 || xs.length < nPoints || ys.length < nPoints) {
      throw new IllegalArgumentException("Invalid points array");
    }
    this.nPoints = nPoints;
    this.xPoints = Arrays.copyOf(xs, nPoints);
    this.yPoints = Arrays.copyOf(ys, nPoints);
    this.color = color;
    this.strokeWidth = Math.max(1, strokeWidth);
    this.isHighlighter = isHighlighter;
    recomputeBounds();
  }

  private void recomputeBounds() {
    int minX = xPoints[0];
    int maxX = xPoints[0];
    int minY = yPoints[0];
    int maxY = yPoints[0];
    for (int i = 1; i < nPoints; i++) {
      if (xPoints[i] < minX) minX = xPoints[i];
      if (xPoints[i] > maxX) maxX = xPoints[i];
      if (yPoints[i] < minY) minY = yPoints[i];
      if (yPoints[i] > maxY) maxY = yPoints[i];
    }
    final int pad = strokeWidth / 2 + 2;
    this.bounds = Bounds.create(minX - pad, minY - pad, (maxX - minX) + pad * 2, (maxY - minY) + pad * 2);
  }

  @Override
  public void draw(Graphics2D g2) {
    final var oldStroke = g2.getStroke();
    final var oldColor = g2.getColor();
    final var oldHints = g2.getRenderingHints();

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    Color drawColor = color;
    if (isHighlighter) {
      // Semi-transparent for highlighter
      drawColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 90);
    }
    g2.setColor(drawColor);

    g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    if (nPoints == 1) {
      final int r = Math.max(2, strokeWidth);
      g2.fillOval(xPoints[0] - r / 2, yPoints[0] - r / 2, r, r);
    } else {
      g2.drawPolyline(xPoints, yPoints, nPoints);
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
  public boolean intersects(int x, int y, int radius) {
    if (bounds == null) return false;
    // Quick rejection bounding box check
    if (x + radius < bounds.getX()
        || x - radius > bounds.getX() + bounds.getWidth()
        || y + radius > bounds.getY() + bounds.getHeight()
        || y - radius < bounds.getY()) {
      // But bounds has y coordinate from top-left, so check properly
    }
    if (x + radius < bounds.getX()
        || x - radius > bounds.getX() + bounds.getWidth()
        || y + radius < bounds.getY()
        || y - radius > bounds.getY() + bounds.getHeight()) {
      return false;
    }

    final double effectiveRadius = radius + strokeWidth / 2.0;
    final double radiusSq = effectiveRadius * effectiveRadius;

    if (nPoints == 1) {
      double dx = x - xPoints[0];
      double dy = y - yPoints[0];
      return (dx * dx + dy * dy) <= radiusSq;
    }

    for (int i = 0; i < nPoints - 1; i++) {
      if (pointToSegmentDistanceSq(x, y, xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]) <= radiusSq) {
        return true;
      }
    }
    return false;
  }

  private static double pointToSegmentDistanceSq(double px, double py, double x1, double y1, double x2, double y2) {
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
    final var elt = doc.createElement("stroke");
    elt.setAttribute("color", String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
    elt.setAttribute("width", String.valueOf(strokeWidth));
    elt.setAttribute("highlighter", String.valueOf(isHighlighter));

    final var sb = new StringBuilder();
    for (int i = 0; i < nPoints; i++) {
      if (i > 0) sb.append(' ');
      sb.append(xPoints[i]).append(',').append(yPoints[i]);
    }
    elt.setAttribute("points", sb.toString());
    return elt;
  }

  public static FreehandStroke fromXmlElement(Element elt) {
    final var colorStr = elt.getAttribute("color");
    Color color = Color.RED;
    if (colorStr != null && !colorStr.isEmpty()) {
      try {
        color = Color.decode(colorStr);
      } catch (NumberFormatException ignored) {
      }
    }
    int width = 2;
    try {
      width = Integer.parseInt(elt.getAttribute("width"));
    } catch (NumberFormatException ignored) {
    }
    boolean isHighlighter = Boolean.parseBoolean(elt.getAttribute("highlighter"));

    final var pointsStr = elt.getAttribute("points");
    final var xs = new ArrayList<Integer>();
    final var ys = new ArrayList<Integer>();
    if (pointsStr != null && !pointsStr.isEmpty()) {
      final var pairs = pointsStr.trim().split("\\s+");
      for (final var pair : pairs) {
        final var xy = pair.split(",");
        if (xy.length == 2) {
          try {
            xs.add(Integer.parseInt(xy[0]));
            ys.add(Integer.parseInt(xy[1]));
          } catch (NumberFormatException ignored) {
          }
        }
      }
    }
    if (xs.isEmpty()) return null;
    return new FreehandStroke(xs, ys, color, width, isHighlighter);
  }

  public Color getColor() {
    return color;
  }

  public int getStrokeWidth() {
    return strokeWidth;
  }

  public boolean isHighlighter() {
    return isHighlighter;
  }
}
