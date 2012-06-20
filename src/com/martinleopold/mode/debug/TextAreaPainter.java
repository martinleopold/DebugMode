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
        paintLineBgColor(gfx, line, x);
        super.paintLine(gfx, tokenMarker, line, x);
    }

    /**
     * Paint the background color of a line.
     *
     * @param gfx the graphics context
     * @param line 0-based line number
     * @param x
     */
    protected void paintLineBgColor(Graphics gfx, int line, int x) {
        //System.out.println("1");
        int y = ta.lineToY(line);
        //System.out.println("2");
        y += fm.getLeading() + fm.getMaxDescent();
        //System.out.println("3");
        int height = fm.getHeight();

        // get the color
        //System.out.println("4");
        Color col = ta.getLineBgColor(line);
        //System.out.print("bg line " + line + ": ");
        // no need to paint anything
        if (col == null) {
            //System.out.println("none");
            return;
        }
        // paint line background
        //System.out.println("5");
        gfx.setColor(col);
        //System.out.println("6");
        gfx.fillRect(0, y, getWidth(), height);
    }
}
