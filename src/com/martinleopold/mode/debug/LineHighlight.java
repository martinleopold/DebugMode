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

/**
 * Model/Controller for a highlighted source code line.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class LineHighlight implements LineListener {

    protected DebugEditor editor; // the view, used for highlighting lines by setting a background color
    protected Color bgColor; // the background color for highlighting lines
    protected LineID lineID;
    protected String marker;
    protected Color markerColor;

    /**
     * Create a {@link LineHighlight} on the current tab.
     *
     * @param lineIdx the line index on the current tab to highlight
     * @param bgColor the background color used for highlighting
     * @param editor the {@link DebugEditor}
     */
    public LineHighlight(int lineIdx, Color bgColor, DebugEditor editor) {
        lineID = editor.getLineIDInCurrentTab(lineIdx);
        this.bgColor = bgColor;
        this.editor = editor;
        lineID.addListener(this);
        lineID.startTracking(editor.currentDocument()); // todo: overwrite a previous doc?
        paint();
    }

    public void setMarker(String marker) {
        this.marker = marker;
        paint();
    }

    public void setMarker(String marker, Color markerColor) {
        this.markerColor = markerColor;
        setMarker(marker);
    }

    /**
     * Retrieve the line id of this {@link LineHighlight}.
     *
     * @return the line id
     */
    public LineID lineID() {
        return lineID;
    }

    /**
     * Retrieve the color for highlighting this line.
     *
     * @return the highlight color.
     */
    public Color getColor() {
        return bgColor;
    }

    /**
     * Test if this highlight is on a certain line.
     *
     * @param testLine the line to test
     * @return true if this highlight is on the given line
     */
    public boolean isOnLine(LineID testLine) {
        return lineID.equals(testLine);
    }

    /**
     * Event handler for line number changes (due to editing). Will remove the
     * highlight from the old line number and repaint it at the new location.
     *
     * @param line the line that has changed
     * @param oldLineIdx the old line index (0-based)
     * @param newLineIdx the new line index (0-based)
     */
    @Override
    public void lineChanged(LineID line, int oldLineIdx, int newLineIdx) {
        // clear old line
        if (editor.isInCurrentTab(new LineID(line.fileName(), oldLineIdx))) {
            editor.textArea().clearLineBgColor(oldLineIdx);
            editor.textArea().clearGutterText(oldLineIdx);
        }
        // paint new line
        paint();
    }

    /**
     * Notify this line highlight that it is no linger used.
     *
     */
    public void dispose() {
        lineID.removeListener(this);
        lineID.stopTracking();
    }

    /**
     * (Re-)paint this line highlight. Needs to be on the current tab
     * (obviously).
     */
    public void paint() {
        if (editor.isInCurrentTab(lineID)) {
            editor.textArea().setLineBgColor(lineID.lineIdx(), bgColor);
            if (marker != null) {
                if (markerColor != null) {
                    editor.textArea().setGutterText(lineID.lineIdx(), marker, markerColor);
                } else {
                    editor.textArea().setGutterText(lineID.lineIdx(), marker);
                }
            }
        }
    }

/**
 * Clear this line highlight. Needs to be on the current tab (obviously).
 */
public void clear() {
        if (editor.isInCurrentTab(lineID)) {
            editor.textArea().clearLineBgColor(lineID.lineIdx());
            editor.textArea().clearGutterText(lineID.lineIdx());
        }
    }
}
