/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.annotation;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.annotation.AnnotationAction;
import com.cburch.logisim.circuit.annotation.AnnotationItem;
import com.cburch.logisim.circuit.annotation.ArrowAnnotation;
import com.cburch.logisim.circuit.annotation.FreehandStroke;
import com.cburch.logisim.circuit.annotation.RectAnnotation;
import com.cburch.logisim.circuit.annotation.TextNoteAnnotation;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.tools.Tool;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AnnotationTool extends Tool {
  public static final String _ID = "Annotation Tool";

  public enum Mode {
    PEN("Freehand Pen"),
    HIGHLIGHTER("Highlighter"),
    RECTANGLE("Rectangle"),
    ARROW("Arrow"),
    TEXT_NOTE("Text Note"),
    ERASER("Eraser");

    private final String displayName;

    Mode(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  private Mode mode = Mode.PEN;
  private Color color = new Color(220, 20, 60); // Crimson red
  private int strokeWidth = 3;
  private boolean fillRect = true;
  private int eraserRadius = 12;

  // Dragging state
  private boolean dragging = false;
  private int startX;
  private int startY;
  private int curX;
  private int curY;
  private final List<Integer> dragXs = new ArrayList<>();
  private final List<Integer> dragYs = new ArrayList<>();
  private final Set<AnnotationItem> erasedInCurrentDrag = new LinkedHashSet<>();

  public AnnotationTool() {}

  @Override
  public String getName() {
    return _ID;
  }

  @Override
  public String getDisplayName() {
    return com.cburch.logisim.tools.Strings.S.get("annotationTool") + " (" + mode.getDisplayName() + ")";
  }

  @Override
  public String getDescription() {
    return com.cburch.logisim.tools.Strings.S.get("annotationToolDesc");
  }

  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    if (mode != null) {
      this.mode = mode;
    }
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    if (color != null) {
      this.color = color;
    }
  }

  public int getStrokeWidth() {
    return strokeWidth;
  }

  public void setStrokeWidth(int strokeWidth) {
    this.strokeWidth = Math.max(1, strokeWidth);
  }

  public boolean isFillRect() {
    return fillRect;
  }

  public void setFillRect(boolean fillRect) {
    this.fillRect = fillRect;
  }

  @Override
  public Cursor getCursor() {
    if (mode == Mode.ERASER) {
      return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }
    return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
  }

  @Override
  public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
    if (e.getButton() != MouseEvent.BUTTON1) return;
    final var circ = canvas.getCircuit();
    if (circ == null) return;

    startX = e.getX();
    startY = e.getY();
    curX = startX;
    curY = startY;
    dragging = true;

    if (mode == Mode.PEN || mode == Mode.HIGHLIGHTER) {
      dragXs.clear();
      dragYs.clear();
      dragXs.add(startX);
      dragYs.add(startY);
      canvas.repaint();
    } else if (mode == Mode.ERASER) {
      erasedInCurrentDrag.clear();
      eraseAt(circ, startX, startY);
      canvas.repaint();
    } else if (mode == Mode.TEXT_NOTE) {
      dragging = false;
      promptAndCreateNote(canvas, circ, startX, startY);
    }
  }

  @Override
  public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
    if (!dragging) return;
    curX = e.getX();
    curY = e.getY();

    final var circ = canvas.getCircuit();
    if (circ == null) return;

    if (mode == Mode.PEN || mode == Mode.HIGHLIGHTER) {
      // Add point if distance moved is at least 2 pixels to keep stroke smooth without redundant points
      if (dragXs.isEmpty()) {
        dragXs.add(curX);
        dragYs.add(curY);
      } else {
        int lastX = dragXs.get(dragXs.size() - 1);
        int lastY = dragYs.get(dragYs.size() - 1);
        int dx = curX - lastX;
        int dy = curY - lastY;
        if (dx * dx + dy * dy >= 4) {
          dragXs.add(curX);
          dragYs.add(curY);
        }
      }
      canvas.repaint();
    } else if (mode == Mode.ERASER) {
      eraseAt(circ, curX, curY);
      canvas.repaint();
    } else {
      canvas.repaint();
    }
  }

  @Override
  public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
    if (!dragging) return;
    dragging = false;
    curX = e.getX();
    curY = e.getY();

    final var circ = canvas.getCircuit();
    final var proj = canvas.getProject();
    if (circ == null || proj == null) return;

    switch (mode) {
      case PEN, HIGHLIGHTER -> {
        if (!dragXs.isEmpty()) {
          dragXs.add(curX);
          dragYs.add(curY);
          int width = (mode == Mode.HIGHLIGHTER) ? Math.max(12, strokeWidth * 3) : strokeWidth;
          Color c = (mode == Mode.HIGHLIGHTER && color.equals(new Color(220, 20, 60)))
              ? new Color(255, 235, 59) // Default yellow for highlighter
              : color;
          final var stroke = new FreehandStroke(dragXs, dragYs, c, width, mode == Mode.HIGHLIGHTER);
          proj.doAction(AnnotationAction.forAdd(circ, stroke, "Draw " + mode.getDisplayName()));
        }
        dragXs.clear();
        dragYs.clear();
      }
      case RECTANGLE -> {
        if (Math.abs(curX - startX) > 3 || Math.abs(curY - startY) > 3) {
          Color fill = fillRect
              ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 35) // 14% alpha fill
              : null;
          final var rect = new RectAnnotation(startX, startY, curX, curY, color, strokeWidth, fill, true);
          proj.doAction(AnnotationAction.forAdd(circ, rect, "Add Box Annotation"));
        }
      }
      case ARROW -> {
        if (Math.abs(curX - startX) > 4 || Math.abs(curY - startY) > 4) {
          final var arrow = new ArrowAnnotation(startX, startY, curX, curY, color, strokeWidth);
          proj.doAction(AnnotationAction.forAdd(circ, arrow, "Add Arrow Annotation"));
        }
      }
      case ERASER -> {
        if (!erasedInCurrentDrag.isEmpty()) {
          // Restore items temporarily so the action can cleanly perform & register in undo history
          circ.getAnnotations().addAll(erasedInCurrentDrag);
          proj.doAction(AnnotationAction.forRemove(circ, erasedInCurrentDrag, "Erase Annotations"));
          erasedInCurrentDrag.clear();
        }
      }
      default -> {
      }
    }
    canvas.repaint();
  }

  @Override
  public void mouseMoved(Canvas canvas, Graphics g, MouseEvent e) {
    curX = e.getX();
    curY = e.getY();
    if (mode == Mode.ERASER) {
      canvas.repaint();
    }
  }

  private void eraseAt(Circuit circ, int x, int y) {
    final var hits = circ.getAnnotations().findIntersects(x, y, eraserRadius);
    if (!hits.isEmpty()) {
      erasedInCurrentDrag.addAll(hits);
      circ.getAnnotations().removeAll(hits);
    }
  }

  private void promptAndCreateNote(Canvas canvas, Circuit circ, int x, int y) {
    final var input = OptionPane.showInputDialog(
        canvas,
        "Enter note content:",
        "New Note Annotation",
        OptionPane.PLAIN_MESSAGE);
    if (input != null && !input.trim().isEmpty()) {
      final var note = new TextNoteAnnotation(x, y, input.trim(), Color.BLACK, TextNoteAnnotation.DEFAULT_NOTE_BG);
      canvas.getProject().doAction(AnnotationAction.forAdd(circ, note, "Add Note Annotation"));
      canvas.repaint();
    }
  }

  @Override
  public void draw(Canvas canvas, ComponentDrawContext context) {
    final var g = context.getGraphics();
    if (!(g instanceof Graphics2D g2)) return;

    final var oldStroke = g2.getStroke();
    final var oldColor = g2.getColor();
    final var oldHints = g2.getRenderingHints();

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    if (mode == Mode.ERASER) {
      // Draw eraser radius circle indicator
      g2.setColor(new Color(150, 150, 150, 80));
      g2.fillOval(curX - eraserRadius, curY - eraserRadius, eraserRadius * 2, eraserRadius * 2);
      g2.setColor(new Color(80, 80, 80, 180));
      g2.setStroke(new BasicStroke(1.2f));
      g2.drawOval(curX - eraserRadius, curY - eraserRadius, eraserRadius * 2, eraserRadius * 2);
    }

    if (!dragging) {
      g2.setStroke(oldStroke);
      g2.setColor(oldColor);
      g2.setRenderingHints(oldHints);
      return;
    }

    switch (mode) {
      case PEN, HIGHLIGHTER -> {
        if (!dragXs.isEmpty()) {
          int n = dragXs.size();
          int[] xs = new int[n];
          int[] ys = new int[n];
          for (int i = 0; i < n; i++) {
            xs[i] = dragXs.get(i);
            ys[i] = dragYs.get(i);
          }
          int width = (mode == Mode.HIGHLIGHTER) ? Math.max(12, strokeWidth * 3) : strokeWidth;
          Color c = (mode == Mode.HIGHLIGHTER && color.equals(new Color(220, 20, 60)))
              ? new Color(255, 235, 59, 90)
              : (mode == Mode.HIGHLIGHTER ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 90) : color);
          g2.setColor(c);
          g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
          if (n == 1) {
            g2.fillOval(xs[0] - width / 2, ys[0] - width / 2, width, width);
          } else {
            g2.drawPolyline(xs, ys, n);
          }
        }
      }
      case RECTANGLE -> {
        int rx = Math.min(startX, curX);
        int ry = Math.min(startY, curY);
        int rw = Math.max(1, Math.abs(curX - startX));
        int rh = Math.max(1, Math.abs(curY - startY));
        if (fillRect) {
          g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 35));
          g2.fillRoundRect(rx, ry, rw, rh, 12, 12);
        }
        g2.setColor(color);
        // Dashed stroke during preview
        final var dash = new float[] {6.0f, 4.0f};
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f));
        g2.drawRoundRect(rx, ry, rw, rh, 12, 12);
      }
      case ARROW -> {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(startX, startY, curX, curY);
        double dx = curX - startX;
        double dy = curY - startY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > 4) {
          double arrowHeadLen = Math.min(dist * 0.7, Math.max(12.0, strokeWidth * 3.5));
          double theta = Math.atan2(dy, dx);
          double arrowAngle = Math.PI / 6.0;
          int lx = (int) Math.round(curX - arrowHeadLen * Math.cos(theta - arrowAngle));
          int ly = (int) Math.round(curY - arrowHeadLen * Math.sin(theta - arrowAngle));
          int rx = (int) Math.round(curX - arrowHeadLen * Math.cos(theta + arrowAngle));
          int ry = (int) Math.round(curY - arrowHeadLen * Math.sin(theta + arrowAngle));
          g2.fill(new Polygon(new int[] {curX, lx, rx}, new int[] {curY, ly, ry}, 3));
        }
      }
      default -> {
      }
    }

    g2.setStroke(oldStroke);
    g2.setColor(oldColor);
    g2.setRenderingHints(oldHints);
  }

  @Override
  public void paintIcon(ComponentDrawContext c, int x, int y) {
    final var g = c.getGraphics();
    if (g == null) return;
    final var gCopy = g.create();
    if (gCopy instanceof Graphics2D g2) {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      // Draw modern pen icon (tilted pen)
      g2.setColor(new Color(220, 50, 40));
      g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g2.drawLine(x + 3, y + 13, x + 11, y + 5);
      g2.setColor(new Color(60, 60, 60));
      g2.setStroke(new BasicStroke(1.5f));
      g2.drawLine(x + 2, y + 14, x + 3, y + 13);
      g2.setColor(new Color(240, 180, 0));
      g2.fillOval(x + 10, y + 3, 4, 4);
    }
    gCopy.dispose();
  }
}
