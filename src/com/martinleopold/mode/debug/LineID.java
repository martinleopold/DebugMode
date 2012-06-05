/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martinleopold.mode.debug;

/**
 * Describes an ID for a code line. Comprised of a file name and a line number.
 *
 * @author mlg
 */
public class LineID {

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
}
