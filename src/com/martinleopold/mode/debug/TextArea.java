/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martinleopold.mode.debug;

import java.awt.Color;
import java.awt.event.ComponentListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;

/**
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

    //protected Map<Position, Color> positionColors = new HashMap();

    /**
     *
     * @param lineIdx 0-based line number
     * @param col
     * @return
     */
//    public Position setLineBgColor(int lineIdx, Color col) {
//        try {
//            // offset of line number
//            int offset = getLineStartOffset(lineIdx);
//            Position pos = document.createPosition(offset);
//            System.out.println("lineIdx: " + lineIdx + " offset: " + offset + " position: " + pos);
//            //lineColors.put(lineIdx, col);
//            positionColors.put(pos, col);
//            painter.invalidateLine(lineIdx);
//            return pos;
//        } catch (BadLocationException ex) {
//            Logger.getLogger(TextArea.class.getName()).log(Level.SEVERE, null, ex);
//            return null;
//        }
//    }

//    public void clearLineBgColor(Position p) {
//        int idx = getLineOfOffset(p.getOffset());
//        painter.invalidateLine(idx);
//        positionColors.remove(p);
//    }
//
//    public void clearLineBgColors() {
//        // invalidate all colored lines
//        for (Position p : positionColors.keySet()) {
//            int idx = getLineOfOffset(p.getOffset());
//            painter.invalidateLine(idx);
//        }
//        positionColors.clear();
//    }
//
//    public void clearLineBgColor(int lineIdx) {
//        Set<Position> positions = positionColors.keySet();
//        for (Position p : positions) {
//            int idx = getLineOfOffset(p.getOffset());
//            if (idx == lineIdx) {
//                positions.remove(p); // keySet is backed by the map, removing also affects map
//                painter.invalidateLine(idx);
//            }
//        }
//    }

    /**
     *
     * @param idx
     * @return null if no color saved for the specified line
     */
//    public Color getLineBgColor(int lineIdx) {
//        // TODO: re-calc only on edits (insert, remove)
//        // re-calculate line numbers from positions
//        Map<Integer, Color> lineColors = new HashMap();
//        for (Position p : positionColors.keySet()) {
//            int idx = getLineOfOffset(p.getOffset());
//            lineColors.put(idx, positionColors.get(p));
//        }
//
//        return lineColors.get(lineIdx);
//    }

    protected Map<Integer, Color> lineColors = new HashMap();
    public void setLineBgColor(int lineIdx, Color col) {
        lineColors.put(lineIdx, col);
        painter.invalidateLine(lineIdx);
    }

    public void clearLineBgColor(int lineIdx) {
        lineColors.remove(lineIdx);
        painter.invalidateLine(lineIdx);
    }

    public void clearLineBgColors() {
        for (int lineIdx : lineColors.keySet()) {
                painter.invalidateLine(lineIdx);
        }
        lineColors.clear();
    }

    public Color getLineBgColor(int lineIdx) {
        return lineColors.get(lineIdx);
    }
}
