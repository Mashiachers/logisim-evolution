/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.annotation.ArrowAnnotation;
import com.cburch.logisim.circuit.annotation.FreehandStroke;
import com.cburch.logisim.circuit.annotation.RectAnnotation;
import com.cburch.logisim.circuit.annotation.TextNoteAnnotation;
import java.awt.Color;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

public class AnnotationXmlTest {

  @Test
  public void testAnnotationSerializationAndDeserialization() throws Exception {
    final var docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    final var doc = docBuilder.newDocument();

    final var circ = new Circuit("TestCircuit", null, null);
    final var annotations = circ.getAnnotations();

    // 1. FreehandStroke
    final var stroke = new FreehandStroke(
        List.of(10, 20, 30),
        List.of(15, 25, 35),
        Color.RED,
        3,
        false);
    annotations.add(stroke);

    // 2. Highlighter stroke
    final var highlighter = new FreehandStroke(
        List.of(100, 110),
        List.of(200, 210),
        Color.YELLOW,
        14,
        true);
    annotations.add(highlighter);

    // 3. RectAnnotation
    final var rect = new RectAnnotation(
        50, 60, 150, 120,
        Color.BLUE, 2,
        new Color(0, 0, 255, 40),
        true);
    annotations.add(rect);

    // 4. ArrowAnnotation
    final var arrow = new ArrowAnnotation(200, 300, 250, 350, Color.GREEN, 4);
    annotations.add(arrow);

    // 5. TextNoteAnnotation
    final var note = new TextNoteAnnotation(400, 500, "Hello Logisim Note", Color.BLACK, TextNoteAnnotation.DEFAULT_NOTE_BG);
    annotations.add(note);

    assertEquals(5, annotations.size());

    // Serialize to XML
    final var xmlElement = AnnotationXml.toXmlElement(doc, annotations);
    assertNotNull(xmlElement);
    assertEquals("annotations", xmlElement.getTagName());
    assertTrue(xmlElement.getChildNodes().getLength() >= 5);

    // Deserialize into a new circuit
    final var restoredCircuit = new Circuit("RestoredCircuit", null, null);
    AnnotationXml.loadIntoCircuit(restoredCircuit, xmlElement);

    final var restoredList = restoredCircuit.getAnnotations().getItems();
    assertEquals(5, restoredList.size());

    // Verify Stroke
    final var s0 = (FreehandStroke) restoredList.get(0);
    assertEquals(Color.RED, s0.getColor());
    assertEquals(3, s0.getStrokeWidth());
    assertFalse(s0.isHighlighter());

    // Verify Highlighter
    final var s1 = (FreehandStroke) restoredList.get(1);
    assertEquals(Color.YELLOW, s1.getColor());
    assertEquals(14, s1.getStrokeWidth());
    assertTrue(s1.isHighlighter());

    // Verify Rect
    final var r = (RectAnnotation) restoredList.get(2);
    assertEquals(50, r.getX());
    assertEquals(60, r.getY());
    assertEquals(100, r.getWidth());
    assertEquals(60, r.getHeight());
    assertEquals(Color.BLUE, r.getStrokeColor());
    assertEquals(2, r.getStrokeWidth());
    assertTrue(r.isRounded());

    // Verify Arrow
    final var a = (ArrowAnnotation) restoredList.get(3);
    assertEquals(200, a.getX1());
    assertEquals(300, a.getY1());
    assertEquals(250, a.getX2());
    assertEquals(350, a.getY2());
    assertEquals(Color.GREEN, a.getColor());
    assertEquals(4, a.getStrokeWidth());

    // Verify Note
    final var n = (TextNoteAnnotation) restoredList.get(4);
    assertEquals(400, n.getX());
    assertEquals(500, n.getY());
    assertEquals("Hello Logisim Note", n.getText());
    assertEquals(Color.BLACK, n.getTextColor());
  }
}
