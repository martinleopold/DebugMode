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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;

/**
 * Model for a highlighted source code line. Will also track the line when
 * editing the attached {@link Document}.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class HighlightedLine implements DocumentListener {

    protected LineID lineID; // the line id (filename + line#)
    protected DebugEditor editor; // the view, used for highlighting lines by setting a background color
    protected Color bgColor; // the background color for highlighting lines
    protected Document doc; // the Document to use for line number tracking
    protected Position pos; // the Position acquired during line number tracking

    /**
     * Construct a {@link HighlightedLine}. Uses the current document (i.e. the
     * current tab) for tracking. TODO: don't use the current document but get
     * the right one depending on the file
     *
     * @param fileName
     * @param lineNo
     * @param bgColor
     * @param editor
     */
    public HighlightedLine(LineID lineID, Color bgColor, DebugEditor editor) {
        this.lineID = lineID;
        this.bgColor = bgColor;
        this.editor = editor;
        enableTracking(editor.currentDocument());
        editor.paintLine(this);
    }

    /**
     * Retrieve the line id of this {@link HighlightedLine}.
     *
     * @return the line id
     */
    public LineID getID() {
        // return a copy, so the line id can't be modified from outside
        // still, the copy will pass an equals() comparison with the original
        return lineID.clone();
    }

    public Color getColor() {
        return bgColor;
    }

    /**
     * Attach a {@link Document} to enable line number tracking when editing.
     * The position to track is before the first non-whitespace character on the
     * line. Edits happening before that position will cause the line number to
     * update accordingly.
     *
     * @param doc the {@link Document} to use for line number tracking
     */
    protected void enableTracking(Document doc) {
        try {
            // TODO: check if line exists
            Element line = doc.getDefaultRootElement().getElement(lineID.lineNo);
            String lineText = doc.getText(line.getStartOffset(), line.getEndOffset() - line.getStartOffset());
            // set tracking position at (=before) first non-white space character on line
            pos = doc.createPosition(line.getStartOffset() + nonWhiteSpaceOffset(lineText));
            this.doc = doc;
            doc.addDocumentListener(this);
            //System.out.println("creating position @ " + pos.getOffset());
        } catch (BadLocationException ex) {
            Logger.getLogger(LineID.class.getName()).log(Level.SEVERE, null, ex);
            pos = null;
            this.doc = null;
        }
    }

    public void close() {
        if (doc != null) {
            doc.removeDocumentListener(this);
            doc = null;
        }
    }

    protected void updatePosition() {
        if (doc != null && pos != null) {
            // track position
            int offset = pos.getOffset();

            int newLineNo = doc.getDefaultRootElement().getElementIndex(offset); // offset to lineNo

            if (lineID.lineNo != newLineNo) {
                // notify the view (so it can change line background colors)
                //editor.lineNumberChanged(this, oldLineNo);

                editor.clearLine(this);
                lineID.lineNo = newLineNo;
                editor.paintLine(this);
            }
        }
    }

    /**
     * Calculate the offset of the first non-whitespace character in a string.
     *
     * @param str the string to examine
     * @return offset of first non-whitespace character in str
     */
    protected static int nonWhiteSpaceOffset(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }
        return str.length();
    }

    /**
     * Called when the {@link Document} registered using {@link #enableTracking}
     * is edited. This happens when text is inserted or removed.
     *
     * @param de
     */
    protected void editEvent(DocumentEvent de) {
        //System.out.println("document edit @ " + de.getOffset());
        if (de.getOffset() <= pos.getOffset()) {
            updatePosition();
            //System.out.println("updating, new line no: " + lineNo);
        }
    }

    /**
     * {@link DocumentListener} callback. Called when text is inserted.
     *
     * @param de
     */
    @Override
    public void insertUpdate(DocumentEvent de) {
        editEvent(de);
    }

    /**
     * {@link DocumentListener} callback. Called when text is removed.
     *
     * @param de
     */
    @Override
    public void removeUpdate(DocumentEvent de) {
        editEvent(de);
    }

    /**
     * {@link DocumentListener} callback. Called when attributes are changed.
     * Not used.
     *
     * @param de
     */
    @Override
    public void changedUpdate(DocumentEvent de) {
        // not needed.
    }
}
