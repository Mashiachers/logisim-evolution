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
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AnnotationAction extends Action {
  private final Circuit circuit;
  private final String name;
  private final List<AnnotationItem> toAdd;
  private final List<AnnotationItem> toRemove;

  public AnnotationAction(
      Circuit circuit, String name,
      Collection<AnnotationItem> toAdd,
      Collection<AnnotationItem> toRemove) {
    this.circuit = circuit;
    this.name = name != null ? name : "Annotation";
    this.toAdd = toAdd != null ? new ArrayList<>(toAdd) : Collections.emptyList();
    this.toRemove = toRemove != null ? new ArrayList<>(toRemove) : Collections.emptyList();
  }

  public static AnnotationAction forAdd(Circuit circuit, AnnotationItem item, String name) {
    return new AnnotationAction(circuit, name, Collections.singletonList(item), Collections.emptyList());
  }

  public static AnnotationAction forRemove(Circuit circuit, Collection<AnnotationItem> items, String name) {
    return new AnnotationAction(circuit, name, Collections.emptyList(), items);
  }

  public static AnnotationAction forClear(Circuit circuit) {
    return new AnnotationAction(circuit, "Clear Annotations", Collections.emptyList(), circuit.getAnnotations().getItems());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void doIt(Project proj) {
    final var annotations = circuit.getAnnotations();
    if (!toRemove.isEmpty()) {
      annotations.removeAll(toRemove);
    }
    if (!toAdd.isEmpty()) {
      annotations.addAll(toAdd);
    }
    if (proj != null && proj.getFrame() != null && proj.getFrame().getCanvas() != null) {
      proj.getFrame().getCanvas().repaint();
    }
  }

  @Override
  public void undo(Project proj) {
    final var annotations = circuit.getAnnotations();
    if (!toAdd.isEmpty()) {
      annotations.removeAll(toAdd);
    }
    if (!toRemove.isEmpty()) {
      annotations.addAll(toRemove);
    }
    if (proj != null && proj.getFrame() != null && proj.getFrame().getCanvas() != null) {
      proj.getFrame().getCanvas().repaint();
    }
  }
}
