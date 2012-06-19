package com.martinleopold.mode.debug;

import java.awt.Color;
import java.awt.event.ComponentListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;

/**
 * Customized text area. Adds support for line background colors.
 *
 * @author mlg
 */
public class TextArea extends JEditTextArea {

    public TextArea(TextAreaDefaults defaults) {
        super(defaults);

        // replace the painter:
        // first save listeners, these are package-private in JEditTextArea, so not accessible
        ComponentListener[] componentListeners = painter.getComponentListeners();
        MouseListener[] mouseListeners = painter.getMouseListeners();
        MouseMotionListener[] mouseMotionListeners = painter.getMouseMotionListeners();

        remove(painter);

        // set new painter
        painter = new TextAreaPainter(this, defaults);

        // set listeners
        for (ComponentListener cl : componentListeners) {
            painter.addComponentListener(cl);
        }

        for (MouseListener ml : mouseListeners) {
            painter.addMouseListener(ml);
        }

        for (MouseMotionListener mml : mouseMotionListeners) {
            painter.addMouseMotionListener(mml);
        }

        add(CENTER, painter);
    }
    protected Map<Integer, Color> lineColors = new HashMap(); // contains line background colors

    /**
     * Set the background color of a line.
     *
     * @param lineIdx 0-based line number
     * @param col the background color to set
     */
    public void setLineBgColor(int lineIdx, Color col) {
        lineColors.put(lineIdx, col);
        painter.invalidateLine(lineIdx);
    }

    /**
     * Clear the background color of a line.
     *
     * @param lineIdx 0-based line number
     */
    public void clearLineBgColor(int lineIdx) {
        lineColors.remove(lineIdx);
        painter.invalidateLine(lineIdx);
    }

    /**
     * Clear all line background colors.
     */
    public void clearLineBgColors() {
        for (int lineIdx : lineColors.keySet()) {
            painter.invalidateLine(lineIdx);
        }
        lineColors.clear();
    }

    /**
     * Get a lines background color.
     *
     * @param lineIdx 0-based line number
     * @return the color or null if no color was set for the specified line
     */
    public Color getLineBgColor(int lineIdx) {
        return lineColors.get(lineIdx);
    }
}
