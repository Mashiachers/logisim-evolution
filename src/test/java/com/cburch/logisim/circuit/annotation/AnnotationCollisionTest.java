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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.Circuit;
import java.awt.Color;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AnnotationCollisionTest {

  @Test
  public void testStrokeIntersection() {
    final var stroke = new FreehandStroke(
        List.of(100, 150, 200),
        List.of(100, 100, 100),
        Color.RED,
        4,
        false);

    // Hit on segment
    assertTrue(stroke.intersects(125, 100, 5));
    // Hit within radius
    assertTrue(stroke.intersects(125, 105, 5));
    // Miss far away
    assertFalse(stroke.intersects(125, 150, 5));
    assertFalse(stroke.intersects(50, 100, 5));
  }

  @Test
  public void testRectIntersection() {
    // Hollow rect
    final var hollowRect = new RectAnnotation(100, 100, 200, 200, Color.BLACK, 2, null, false);
    // Hit border
    assertTrue(hollowRect.intersects(100, 150, 5));
    assertTrue(hollowRect.intersects(150, 100, 5));
    // Center of hollow rect should NOT hit if radius is small
    assertFalse(hollowRect.intersects(150, 150, 5));

    // Filled rect
    final var filledRect = new RectAnnotation(100, 100, 200, 200, Color.BLACK, 2, new Color(255, 0, 0, 50), false);
    // Center of filled rect SHOULD hit
    assertTrue(filledRect.intersects(150, 150, 5));
    // Far outside should not hit
    assertFalse(filledRect.intersects(300, 300, 5));
  }

  @Test
  public void testArrowIntersection() {
    final var arrow = new ArrowAnnotation(50, 50, 150, 50, Color.BLUE, 2);
    // Hit along the body
    assertTrue(arrow.intersects(100, 50, 5));
    // Hit near the arrowhead
    assertTrue(arrow.intersects(148, 52, 5));
    // Far outside
    assertFalse(arrow.intersects(100, 100, 5));
  }

  @Test
  public void testFindIntersectsAndRemoval() {
    final var circ = new Circuit("TestCircuit", null, null);
    final var annots = circ.getAnnotations();

    final var stroke1 = new FreehandStroke(List.of(10, 20), List.of(10, 20), Color.BLACK, 2, false);
    final var stroke2 = new FreehandStroke(List.of(200, 210), List.of(200, 210), Color.RED, 2, false);
    annots.add(stroke1);
    annots.add(stroke2);

    assertEquals(2, annots.size());

    // Eraser near stroke1
    final var hits = annots.findIntersects(15, 15, 10);
    assertEquals(1, hits.size());
    assertTrue(hits.contains(stroke1));

    // Remove hits
    annots.removeAll(hits);
    assertEquals(1, annots.size());
    assertEquals(stroke2, annots.getItems().get(0));
  }
}
