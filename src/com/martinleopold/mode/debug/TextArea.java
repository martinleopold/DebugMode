/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package com.martinleopold.mode.debug;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;

/**
 * Customized text area. Adds support for line background colors.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class TextArea extends JEditTextArea {

    protected MouseListener[] mouseListeners; // cached mouselisteners, these are wrapped by MouseHandler
    protected DebugEditor editor;

    public TextArea(TextAreaDefaults defaults, DebugEditor editor) {
        super(defaults);

        this.editor = editor;

        // replace the painter:
        // first save listeners, these are package-private in JEditTextArea, so not accessible
        ComponentListener[] componentListeners = painter.getComponentListeners();
        mouseListeners = painter.getMouseListeners();
        MouseMotionListener[] mouseMotionListeners = painter.getMouseMotionListeners();

        remove(painter);

        // set new painter
        painter = new TextAreaPainter(this, defaults);

        // set listeners
        for (ComponentListener cl : componentListeners) {
            painter.addComponentListener(cl);
        }

//        for (MouseListener ml : mouseListeners) {
//            painter.addMouseListener(ml);
//        }

        for (MouseMotionListener mml : mouseMotionListeners) {
            painter.addMouseMotionListener(mml);
        }

        MouseHandler mouseHandler = new MouseHandler();
        painter.addMouseListener(mouseHandler);
        painter.addMouseMotionListener(mouseHandler);

        add(CENTER, painter);
    }
    // TODO: docs
    protected int gutterChars = 2; // # characters
    protected int gutterMargins = 3; // px, space added left and right
    protected Color gutterBgColor = new Color(252, 252, 252);
    protected Color gutterLineColor = new Color(233, 233, 233);
    protected Map<Integer, String> gutterText = new HashMap();
    protected Map<Integer, Color> gutterTextColors = new HashMap();

    protected int gutterWidth() {
        return gutterChars * painter.getFontMetrics().getMaxAdvance() + 2 * gutterMargins;
    }

    protected int gutterBorder() {
        return gutterMargins;
    }

    public void setGutterText(int lineIdx, String text) {
        gutterText.put(lineIdx, text);
        painter.invalidateLine(lineIdx);
    }

    public void setGutterText(int lineIdx, String text, Color textColor) {
        gutterTextColors.put(lineIdx, textColor);
        setGutterText(lineIdx, text);
    }

    public void clearGutterText(int lineIdx) {
        gutterText.remove(lineIdx);
        painter.invalidateLine(lineIdx);
    }

    public void clearGutterText() {
        for (int lineIdx : gutterText.keySet()) {
            painter.invalidateLine(lineIdx);
        }
        gutterText.clear();
    }

    public String getGutterText(int lineIdx) {
        return gutterText.get(lineIdx);
    }

    public Color getGutterTextColor(int lineIdx) {
        return gutterTextColors.get(lineIdx);
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

    @Override
    public int _offsetToX(int line, int offset) {
        return super._offsetToX(line, offset) + gutterWidth();
    }

    @Override
    public int xToOffset(int line, int x) {
        return super.xToOffset(line, x - gutterWidth());
    }

    protected class MouseHandler implements MouseListener, MouseMotionListener {

        protected int lastX;

        @Override
        public void mouseClicked(MouseEvent me) {
            for (MouseListener ml : mouseListeners) {
                ml.mouseClicked(me);
            }
        }

        @Override
        public void mousePressed(MouseEvent me) {
            // check if this happened in the gutter area
            if (me.getX() < gutterWidth()) {
                if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() == 2) {
                    int line = me.getY() / painter.getFontMetrics().getHeight() + firstLine;
                    if (line >= 0 && line <= getLineCount() - 1) {
                        editor.gutterDblClicked(line);
                    }
                }
            } else {
                // invoke standard listeners
                for (MouseListener ml : mouseListeners) {
                    ml.mousePressed(me);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            for (MouseListener ml : mouseListeners) {
                ml.mouseReleased(me);
            }
        }

        @Override
        public void mouseEntered(MouseEvent me) {
            for (MouseListener ml : mouseListeners) {
                ml.mouseEntered(me);
            }
        }

        @Override
        public void mouseExited(MouseEvent me) {
            for (MouseListener ml : mouseListeners) {
                ml.mouseExited(me);
            }
        }

        @Override
        public void mouseDragged(MouseEvent me) {
            // nop
        }

        @Override
        public void mouseMoved(MouseEvent me) {
            //System.out.println("moved");
            if (me.getX() < gutterWidth()) {
                if (lastX >= gutterWidth()) {
                    painter.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            } else {
                if (lastX < gutterWidth()) {
                    painter.setCursor(new Cursor(Cursor.TEXT_CURSOR));
                }
            }
            lastX = me.getX();
        }
    }
}
