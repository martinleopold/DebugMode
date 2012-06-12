/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martinleopold.mode.debug;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;

/**
 * Describes an ID for a code line. Comprised of a file name and a line number.
 * Allows tracking the line when editing by attaching to a Document.
 *
 * @author mlg
 */
public class LineID implements DocumentListener {

    public String fileName;
    public int lineNo;

    public LineID(String fileName, int lineNo) {
        this.fileName = fileName;
        this.lineNo = lineNo;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

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

    protected Document doc;
    protected Position pos;
    /**
     * Attach a Document to enable line number tracking when editing.
     * @param doc
     */
    public void enableTracking(Document doc) {
        try {
            int offset = doc.getDefaultRootElement().getElement(lineNo-1).getStartOffset(); // todo check if line exists
            pos = doc.createPosition(offset);
            this.doc = doc;
            doc.addDocumentListener(this);
            System.out.println("creating position @ " + pos.getOffset());
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
                if (editor != null) {
                    editor.clearLineBgColor(new LineID(fileName, lineNo));
                    editor.setLineBgColor(new LineID(fileName, newLineNo), BGCOLOR);
                }
                lineNo = newLineNo;
            }
        }
    }

    public static final Color BGCOLOR = new Color(255, 170, 170);
    protected DebugEditor editor;
    public void setView(DebugEditor editor) {
        this.editor = editor;
        editor.setLineBgColor(this, BGCOLOR);
    }

  // todo: use this to track from the first non-whitespace position in the line
//      public int getLineStartNonWhiteSpaceOffset(int line)
//  {
//    int offset = getLineStartOffset(line);
//    int length = getLineLength(line);
//    String str = getText(offset, length);
//
//    for(int i = 0; i < str.length(); i++) {
//      if(!Character.isWhitespace(str.charAt(i))) {
//        return offset + i;
//      }
//    }
//    return offset + length;
//  }

    protected void editEvent(DocumentEvent de) {
        System.out.println("document edit @ " + de.getOffset());
        if (de.getOffset() < pos.getOffset()) {
            updatePosition();
            System.out.println("updating, new line no: " + lineNo);
        }
    }

    @Override
    public void insertUpdate(DocumentEvent de) {
        editEvent(de);
    }

    @Override
    public void removeUpdate(DocumentEvent de) {
        editEvent(de);
    }

    @Override
    public void changedUpdate(DocumentEvent de) {
        // not needed.
    }
}
