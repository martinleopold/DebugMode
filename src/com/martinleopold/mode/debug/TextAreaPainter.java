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
import java.awt.Graphics;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;
import processing.app.syntax.TextAreaDefaults;
import processing.app.syntax.TokenMarker;

/**
 * Customized line painter. Adds support for background colors.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class TextAreaPainter extends processing.app.syntax.TextAreaPainter {

    protected TextArea ta; // we need the subclassed textarea

    public TextAreaPainter(TextArea textArea, TextAreaDefaults defaults) {
        super(textArea, defaults);
        ta = (TextArea) textArea;
    }

    /**
     * Paint a line.
     *
     * @param gfx the graphics context
     * @param tokenMarker
     * @param line 0-based line number
     * @param x
     */
    @Override
    protected void paintLine(Graphics gfx, TokenMarker tokenMarker,
            int line, int x) {

        // paint gutter
        paintGutterBg(gfx, line, x);

        paintLineBgColor(gfx, line, x + ta.gutterWidth());

        paintGutterLine(gfx, line, x);

        // paint gutter symbol
        paintGutterText(gfx, line, x);

        super.paintLine(gfx, tokenMarker, line, x + ta.gutterWidth());
    }

    // TODO: docs
    protected void paintGutterBg(Graphics gfx, int line, int x) {
        gfx.setColor(ta.gutterBgColor);
        int y = ta.lineToY(line) + fm.getLeading() + fm.getMaxDescent();
        gfx.fillRect(0, y, ta.gutterWidth(), fm.getHeight());
    }

    protected void paintGutterLine(Graphics gfx, int line, int x) {
        int y = ta.lineToY(line) + fm.getLeading() + fm.getMaxDescent();
        gfx.setColor(ta.gutterLineColor);
        gfx.drawLine(ta.gutterWidth(), y, ta.gutterWidth(), y + fm.getHeight());
    }

    protected void paintGutterText(Graphics gfx, int line, int x) {
        String text = ta.getGutterText(line);
        if (text == null) {
            return;
        }

        gfx.setFont(getFont());
        Color textColor = ta.getGutterTextColor(line);
        if (textColor == null) {
            gfx.setColor(getForeground());
        } else {
            gfx.setColor(textColor);
        }
        int y = ta.lineToY(line) + fm.getHeight();

        //int len = text.length() > ta.gutterChars ? ta.gutterChars : text.length();
        Utilities.drawTabbedText(new Segment(text.toCharArray(), 0, text.length()), ta.gutterBorder(), y, gfx, this, 0);
        Utilities.drawTabbedText(new Segment(text.toCharArray(), 0, text.length()), ta.gutterBorder() + 1, y, gfx, this, 0);
        Utilities.drawTabbedText(new Segment(text.toCharArray(), 0, text.length()), ta.gutterBorder(), y + 1, gfx, this, 0);
        Utilities.drawTabbedText(new Segment(text.toCharArray(), 0, text.length()), ta.gutterBorder() + 1, y + 1, gfx, this, 0);

        // Draw characters via input method.
//    if (compositionTextPainter != null && compositionTextPainter.hasComposedTextLayout()) {
//      compositionTextPainter.draw(gfx, lineHighlightColor);
//    }
    }

    /**
     * Paint the background color of a line.
     *
     * @param gfx the graphics context
     * @param line 0-based line number
     * @param x
     */
    protected void paintLineBgColor(Graphics gfx, int line, int x) {
        int y = ta.lineToY(line);
        y += fm.getLeading() + fm.getMaxDescent();
        int height = fm.getHeight();

        // get the color
        Color col = ta.getLineBgColor(line);
        //System.out.print("bg line " + line + ": ");
        // no need to paint anything
        if (col == null) {
            //System.out.println("none");
            return;
        }
        // paint line background
        gfx.setColor(col);
        gfx.fillRect(0, y, getWidth(), height);
    }
}
