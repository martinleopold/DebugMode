package com.martinleopold.mode.debug;

import java.awt.Color;
import java.awt.Graphics;
import processing.app.syntax.TextAreaDefaults;
import processing.app.syntax.TokenMarker;

/**
 * Customized line painter. Adds support for background colors.
 *
 * @author mlg
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
