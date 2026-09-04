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
import java.awt.Graphics2D;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface AnnotationItem {
  /**
   * Render this annotation item.
   *
   * @param g2 The Graphics2D context (already scaled to model coordinates).
   */
  void draw(Graphics2D g2);

  /**
   * Returns the bounding box of this annotation.
   *
   * @return Bounds of the item.
   */
  Bounds getBounds();

  /**
   * Tests if an eraser circle centered at (x, y) with the given radius touches this item.
   *
   * @param x Circle center X.
   * @param y Circle center Y.
   * @param radius Eraser hit-radius.
   * @return True if hit.
   */
  boolean intersects(int x, int y, int radius);

  /**
   * Serialize this annotation item to an XML element.
   *
   * @param doc The DOM document.
   * @return The created XML element.
   */
  Element toXmlElement(Document doc);
}
