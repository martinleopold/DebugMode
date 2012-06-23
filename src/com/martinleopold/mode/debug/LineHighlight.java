/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.martinleopold.mode.debug;

import java.awt.Color;
import javax.swing.text.Document;

/**
 * Model/Controller for a highlighted source code line. Will also track the line
 * when editing the attached {@link Document}.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class LineHighlight extends LineID {
    protected DebugEditor editor; // the view, used for highlighting lines by setting a background color
    protected Color bgColor; // the background color for highlighting lines


    /**
     * Create a {@link LineHighlight} on the current tab.
     *
     * @param lineIdx the line index on the current tab to highlight
     * @param bgColor the background color used for highlighting
     * @param editor the {@link DebugEditor}
     */
    protected LineHighlight(int lineIdx, Color bgColor, DebugEditor editor) {
        super(editor.getSketch().getCurrentCode().getFileName(), lineIdx);
        this.bgColor = bgColor;
        this.editor = editor;
        startTracking(editor.currentDocument());
        editor.paintLine(this);
    }

    public static LineHighlight create(int lineIdx, Color bgColor, DebugEditor editor) {
        LineID spr = LineID.create(editor.getSketch().getCurrentCode().getFileName(), lineIdx);

        this.bgColor = bgColor;
        this.editor = editor;
    }

//    /**
//     * Retrieve the line id of this {@link LineHighlight}.
//     *
//     * @return the line id
//     */
//    public LineID getID() {
//        // return a copy, so the line id can't be modified from outside
//        // still, the copy will pass an equals() comparison with the original
//        return lineID.clone();
//    }

    /**
     * Retrieve the color for highlighting this line.
     *
     * @return the highlight color.
     */
    public Color getColor() {
        return bgColor;
    }

    public boolean isOnLine(LineID testLine) {
        return equals(testLine);
    }

    @Override
    protected void lineChanged(int oldIdx, int newIdx) {
        lineIdx = oldIdx;
        editor.clearLine(this);
        lineIdx = newIdx;
        editor.paintLine(this);
    }
}
