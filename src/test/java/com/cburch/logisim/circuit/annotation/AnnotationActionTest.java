/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.Circuit;
import java.awt.Color;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AnnotationActionTest {

  @Test
  public void testAddAndUndo() {
    final var circ = new Circuit("TestCircuit", null, null);
    final var stroke = new FreehandStroke(List.of(10, 20), List.of(30, 40), Color.RED, 2, false);

    final var action = AnnotationAction.forAdd(circ, stroke, "Draw Stroke");
    action.doIt(null);
    assertEquals(1, circ.getAnnotations().size());
    assertTrue(circ.getAnnotations().getItems().contains(stroke));

    action.undo(null);
    assertEquals(0, circ.getAnnotations().size());

    action.doIt(null);
    assertEquals(1, circ.getAnnotations().size());
  }

  @Test
  public void testClearAndUndo() {
    final var circ = new Circuit("TestCircuit", null, null);
    final var s1 = new FreehandStroke(List.of(1, 2), List.of(3, 4), Color.RED, 2, false);
    final var s2 = new FreehandStroke(List.of(5, 6), List.of(7, 8), Color.BLUE, 2, false);
    circ.getAnnotations().add(s1);
    circ.getAnnotations().add(s2);
    assertEquals(2, circ.getAnnotations().size());

    final var action = AnnotationAction.forClear(circ);
    action.doIt(null);
    assertEquals(0, circ.getAnnotations().size());

    action.undo(null);
    assertEquals(2, circ.getAnnotations().size());
    assertTrue(circ.getAnnotations().getItems().contains(s1));
    assertTrue(circ.getAnnotations().getItems().contains(s2));
  }
}
