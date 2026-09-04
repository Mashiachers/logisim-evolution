/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.annotation.AnnotationAction;
import com.cburch.logisim.tools.annotation.AnnotationTool;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

public class AnnotationToolbarPanel extends JPanel {
  private final Canvas canvas;
  private final AnnotationTool tool;

  private final List<JToggleButton> modeButtons = new ArrayList<>();
  private final List<ColorDotButton> colorButtons = new ArrayList<>();
  private final List<JToggleButton> widthButtons = new ArrayList<>();
  private final JButton visibilityButton;

  // Preset colors
  private static final Color[] PALETTE = {
    new Color(220, 20, 60),  // Crimson Red
    new Color(30, 144, 255), // Dodger Blue
    new Color(46, 139, 87),  // Sea Green
    new Color(255, 193, 7),  // Amber Yellow
    new Color(156, 39, 176), // Purple
    new Color(33, 33, 33)    // Dark Gray / Black
  };

  public AnnotationToolbarPanel(Canvas canvas, AnnotationTool tool) {
    this.canvas = canvas;
    this.tool = tool;

    setOpaque(false);
    setLayout(new FlowLayout(FlowLayout.LEFT, 4, 3));
    setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

    // Mode buttons
    addModeButton("Pen", AnnotationTool.Mode.PEN, "Freehand Pen (自由画笔)");
    addModeButton("Highlight", AnnotationTool.Mode.HIGHLIGHTER, "Highlighter (荧光高亮笔)");
    addModeButton("Box", AnnotationTool.Mode.RECTANGLE, "Rectangle Box (矩形框标注)");
    addModeButton("Arrow", AnnotationTool.Mode.ARROW, "Arrow Line (箭头指示线)");
    addModeButton("Note", AnnotationTool.Mode.TEXT_NOTE, "Sticky Note (便签文字)");
    addModeButton("Eraser", AnnotationTool.Mode.ERASER, "Stroke Eraser (触碰橡皮擦)");

    add(createSeparator());

    // Color palette
    for (final var c : PALETTE) {
      final var dot = new ColorDotButton(c);
      colorButtons.add(dot);
      add(dot);
    }

    final var customColorBtn = new JButton("+");
    customColorBtn.setToolTipText("Custom Color (自定义颜色)");
    customColorBtn.setFocusable(false);
    customColorBtn.setMargin(new java.awt.Insets(1, 4, 1, 4));
    customColorBtn.addActionListener(e -> {
      final var chosen = JColorChooser.showDialog(this, "Choose Annotation Color", tool.getColor());
      if (chosen != null) {
        tool.setColor(chosen);
        updateState();
        canvas.repaint();
      }
    });
    add(customColorBtn);

    add(createSeparator());

    // Width buttons
    addWidthButton("1x", 2, "Thin Stroke (细)");
    addWidthButton("2x", 4, "Medium Stroke (中)");
    addWidthButton("3x", 8, "Thick Stroke (粗)");

    add(createSeparator());

    // Toggle visibility button
    visibilityButton = new JButton("Eye");
    visibilityButton.setToolTipText("Toggle Annotation Visibility (显示/隐藏标注图层)");
    visibilityButton.setFocusable(false);
    visibilityButton.addActionListener(e -> {
      final var circ = canvas.getCircuit();
      if (circ != null) {
        final var annot = circ.getAnnotations();
        annot.setVisible(!annot.isVisible());
        updateVisibilityText();
        canvas.repaint();
      }
    });
    add(visibilityButton);

    // Clear all button
    final var clearBtn = new JButton("Clear");
    clearBtn.setToolTipText("Clear All Annotations in Current Circuit (清空当前电路标注)");
    clearBtn.setFocusable(false);
    clearBtn.addActionListener(e -> {
      final var circ = canvas.getCircuit();
      final var proj = canvas.getProject();
      if (circ != null && proj != null && !circ.getAnnotations().isEmpty()) {
        proj.doAction(AnnotationAction.forClear(circ));
        canvas.repaint();
      }
    });
    add(clearBtn);

    updateState();
  }

  private void addModeButton(String text, AnnotationTool.Mode mode, String tooltip) {
    final var btn = new JToggleButton(text);
    btn.setToolTipText(tooltip);
    btn.setFocusable(false);
    btn.setMargin(new java.awt.Insets(2, 6, 2, 6));
    btn.addActionListener(e -> {
      tool.setMode(mode);
      updateState();
      canvas.repaint();
    });
    modeButtons.add(btn);
    add(btn);
  }

  private void addWidthButton(String text, int width, String tooltip) {
    final var btn = new JToggleButton(text);
    btn.setToolTipText(tooltip);
    btn.setFocusable(false);
    btn.setMargin(new java.awt.Insets(2, 4, 2, 4));
    btn.addActionListener(e -> {
      tool.setStrokeWidth(width);
      updateState();
      canvas.repaint();
    });
    widthButtons.add(btn);
    add(btn);
  }

  private JPanel createSeparator() {
    final var sep = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        g.setColor(new Color(180, 180, 180, 150));
        g.drawLine(2, 4, 2, getHeight() - 4);
      }
    };
    sep.setPreferredSize(new Dimension(5, 22));
    sep.setOpaque(false);
    return sep;
  }

  public void updateState() {
    // Update mode buttons
    final var curMode = tool.getMode();
    final var modes = AnnotationTool.Mode.values();
    for (int i = 0; i < modeButtons.size() && i < modes.length; i++) {
      modeButtons.get(i).setSelected(modes[i] == curMode);
    }

    // Update color dots
    final var curColor = tool.getColor();
    for (final var dot : colorButtons) {
      dot.setSelected(dot.color.equals(curColor));
    }

    // Update width buttons
    final int curW = tool.getStrokeWidth();
    if (widthButtons.size() >= 3) {
      widthButtons.get(0).setSelected(curW <= 2);
      widthButtons.get(1).setSelected(curW > 2 && curW <= 5);
      widthButtons.get(2).setSelected(curW > 5);
    }

    updateVisibilityText();
    repaint();
  }

  private void updateVisibilityText() {
    final var circ = canvas.getCircuit();
    if (circ != null) {
      visibilityButton.setText(circ.getAnnotations().isVisible() ? "Hide" : "Show");
    }
  }

  public void updateLocation() {
    final var pane = canvas.getCanvasPane();
    if (pane == null) return;
    final var viewRect = pane.getViewport().getViewRect();
    final var pref = getPreferredSize();
    final int pad = 12;
    final int x = viewRect.x + viewRect.width - pref.width - pad;
    final int y = viewRect.y + pad;
    setBounds(Math.max(viewRect.x + pad, x), Math.max(viewRect.y + pad, y), pref.width, pref.height);
  }

  @Override
  protected void paintComponent(Graphics g) {
    final var g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Semi-transparent frosted card background
    g2.setColor(new Color(255, 255, 255, 235));
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

    // Card border & subtle shadow
    g2.setColor(new Color(180, 180, 180, 180));
    g2.setStroke(new BasicStroke(1.2f));
    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

    g2.dispose();
    super.paintComponent(g);
  }

  private class ColorDotButton extends JPanel {
    private final Color color;
    private boolean selected = false;

    ColorDotButton(Color color) {
      this.color = color;
      setPreferredSize(new Dimension(20, 20));
      setOpaque(false);
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      setToolTipText("Color: #" + Integer.toHexString(color.getRGB()).substring(2).toUpperCase());
      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          tool.setColor(color);
          updateState();
          canvas.repaint();
        }
      });
    }

    void setSelected(boolean sel) {
      this.selected = sel;
      repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
      final var g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int w = getWidth();
      int h = getHeight();

      // Outer highlight circle if selected
      if (selected) {
        g2.setColor(new Color(60, 60, 60, 200));
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawOval(1, 1, w - 3, h - 3);
      }

      // Fill color circle
      g2.setColor(color);
      int inset = selected ? 3 : 2;
      g2.fillOval(inset, inset, w - inset * 2, h - inset * 2);

      g2.setColor(new Color(0, 0, 0, 40));
      g2.setStroke(new BasicStroke(1.0f));
      g2.drawOval(inset, inset, w - inset * 2, h - inset * 2);

      g2.dispose();
    }
  }
}
