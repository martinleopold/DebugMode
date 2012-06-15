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
 * Describes an ID for a code line. Comprised of a file name and a (1-based)
 * line number. Allows tracking the line when editing by attaching to a
 * Document.<p>TODO: split the tracking and highlighting features into a subclass
 * "LineBreakpoint"
 *
 * @author mlg
 */
public class LineID implements DocumentListener {

    public String fileName; // the filename
    public int lineNo; // the line number, 1-based

    public LineID(String fileName, int lineNo) {
        this.fileName = fileName;
        this.lineNo = lineNo;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Test whether this {@link LineID} is equal to another object. Two {@link LineID}'s are equal when both their fileName and lineNo are equal.
     *
     * @param obj the object to test for equality
     * @return {@code true} if equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LineID other = (LineID) obj;
        if ((this.fileName == null) ? (other.fileName != null) : !this.fileName.equals(other.fileName)) {
            return false;
        }
        if (this.lineNo != other.lineNo) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return fileName + ":" + lineNo;
    }
    protected Document doc; // the Document to use for line number tracking
    protected Position pos; // the Position acquired during line number tracking

    /**
     * Attach a {@link Document} to enable line number tracking when editing. The
     * position to track is before the first non-whitespace character on the
     * line. Edits happening before that position will cause the line number to
     * update accordingly.
     *
     * @param doc the {@link Document} to use for line number tracking
     */
    public void enableTracking(Document doc) {
        try {
            // todo check if line exists
            Element line = doc.getDefaultRootElement().getElement(lineNo - 1);
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

    public void disableTracking() {
        if (doc != null) {
            doc.removeDocumentListener(this);
            doc = null;
        }
    }

    protected void updatePosition() {
        if (doc != null && pos != null) {
            // track position
            int offset = pos.getOffset();
            // offset to lineNo
            int newLineNo = doc.getDefaultRootElement().getElementIndex(offset) + 1;
            if (newLineNo != lineNo) {
                // update the view (line background colors in edior)
                if (editor != null) {
                    editor.clearLineBgColor(new LineID(fileName, lineNo)); // clear old line background
                    editor.setLineBgColor(new LineID(fileName, newLineNo), bgColor); // set new line background
                }
                lineNo = newLineNo;
            }
        }
    }
    protected DebugEditor editor; // the view, used for highlighting lines by setting a background color
    protected Color bgColor; // the background color for highlighting lines

    /**
     * Set a view for visually representing this line using a colored
     * background.
     *
     * @param editor the {@link DebugEditor} to use as view
     */
    public void setView(DebugEditor editor, Color bgColor) {
        this.editor = editor;
        this.bgColor = bgColor;
        editor.setLineBgColor(this, bgColor);
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
     * Called when the {@link Document} registered using {@link enableTracking()} is edited.
     * This happens when text is inserted or removed.
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
     * {@link DocumentListener} callback. Called when attributes are changed. Not used.
     *
     * @param de
     */
    @Override
    public void changedUpdate(DocumentEvent de) {
        // not needed.
    }
}
