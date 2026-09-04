/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.annotation.ArrowAnnotation;
import com.cburch.logisim.circuit.annotation.CircuitAnnotations;
import com.cburch.logisim.circuit.annotation.FreehandStroke;
import com.cburch.logisim.circuit.annotation.RectAnnotation;
import com.cburch.logisim.circuit.annotation.TextNoteAnnotation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AnnotationXml {
  private AnnotationXml() {}

  public static Element toXmlElement(Document doc, CircuitAnnotations annotations) {
    if (annotations == null || annotations.isEmpty()) return null;

    final var root = doc.createElement("annotations");
    root.setAttribute("visible", String.valueOf(annotations.isVisible()));

    for (final var item : annotations.getItems()) {
      final var elt = item.toXmlElement(doc);
      if (elt != null) {
        root.appendChild(elt);
      }
    }
    return root;
  }

  public static void loadIntoCircuit(Circuit circuit, Element annotationsElement) {
    if (circuit == null || annotationsElement == null) return;
    final var annotations = circuit.getAnnotations();
    if (annotationsElement.hasAttribute("visible")) {
      annotations.setVisible(Boolean.parseBoolean(annotationsElement.getAttribute("visible")));
    }

    for (final var child : XmlIterator.forChildElements(annotationsElement)) {
      final var tag = child.getTagName();
      try {
        switch (tag) {
          case "stroke" -> {
            final var stroke = FreehandStroke.fromXmlElement(child);
            if (stroke != null) annotations.add(stroke);
          }
          case "rect" -> {
            final var rect = RectAnnotation.fromXmlElement(child);
            if (rect != null) annotations.add(rect);
          }
          case "arrow" -> {
            final var arrow = ArrowAnnotation.fromXmlElement(child);
            if (arrow != null) annotations.add(arrow);
          }
          case "note" -> {
            final var note = TextNoteAnnotation.fromXmlElement(child);
            if (note != null) annotations.add(note);
          }
        }
      } catch (Exception ignored) {
        // Skip corrupted individual annotation without failing the whole circuit load
      }
    }
  }
}
