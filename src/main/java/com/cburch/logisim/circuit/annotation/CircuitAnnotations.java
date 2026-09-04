/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.annotation;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Bounds;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CircuitAnnotations {
  private final Circuit circuit;
  private final List<AnnotationItem> items = new CopyOnWriteArrayList<>();
  private boolean visible = true;

  public CircuitAnnotations(Circuit circuit) {
    this.circuit = circuit;
  }

  public Circuit getCircuit() {
    return circuit;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    if (this.visible != visible) {
      this.visible = visible;
      fireChanged();
    }
  }

  public List<AnnotationItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  public int size() {
    return items.size();
  }

  public void add(AnnotationItem item) {
    if (item != null) {
      items.add(item);
      fireChanged();
    }
  }

  public void addAll(Collection<? extends AnnotationItem> toAdd) {
    if (toAdd != null && !toAdd.isEmpty()) {
      items.addAll(toAdd);
      fireChanged();
    }
  }

  public void remove(AnnotationItem item) {
    if (item != null && items.remove(item)) {
      fireChanged();
    }
  }

  public void removeAll(Collection<? extends AnnotationItem> toRemove) {
    if (toRemove != null && !toRemove.isEmpty()) {
      if (items.removeAll(toRemove)) {
        fireChanged();
      }
    }
  }

  public void clear() {
    if (!items.isEmpty()) {
      items.clear();
      fireChanged();
    }
  }

  public List<AnnotationItem> findIntersects(int x, int y, int radius) {
    final var hit = new ArrayList<AnnotationItem>();
    for (final var item : items) {
      if (item.intersects(x, y, radius)) {
        hit.add(item);
      }
    }
    return hit;
  }

  public Bounds getBounds() {
    if (items.isEmpty() || !visible) return Bounds.EMPTY_BOUNDS;
    Bounds bds = Bounds.EMPTY_BOUNDS;
    for (final var item : items) {
      final var itemBounds = item.getBounds();
      if (itemBounds != null && itemBounds != Bounds.EMPTY_BOUNDS) {
        bds = (bds == Bounds.EMPTY_BOUNDS) ? itemBounds : bds.add(itemBounds);
      }
    }
    return bds;
  }

  public void draw(Graphics2D g2) {
    if (!visible || items.isEmpty()) return;
    for (final var item : items) {
      item.draw(g2);
    }
  }

  private void fireChanged() {
    // Notify circuit listeners of change if circuit is present
    if (circuit != null) {
      // circuit.fireEvent(CircuitEvent.ACTION_INVALIDATE, null) will be called if needed
    }
  }
}
