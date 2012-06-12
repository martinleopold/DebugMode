/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martinleopold.mode.debug;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;
import processing.app.syntax.TokenMarker;

/**
 *
 * @author mlg
 */
public class TextAreaPainter extends processing.app.syntax.TextAreaPainter {

    // we need the subclassed textarea
    protected TextArea ta;
    public TextAreaPainter(TextArea textArea, TextAreaDefaults defaults) {
        super(textArea, defaults);
        ta = (TextArea) textArea;
    }

    @Override
    protected void paintLine(Graphics gfx, TokenMarker tokenMarker,
            int line, int x) {
        paintLineColor(gfx, line, x);
        super.paintLine(gfx, tokenMarker, line, x);
    }

    protected void paintLineColor(Graphics gfx, int line, int x) {
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
        gfx.fillRect(0,y,getWidth(),height);
    }
}
