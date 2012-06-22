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

/**
 * Describes an ID for a code line. Comprised of a file name and a (0-based)
 * line number.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class LineID {

    public String fileName; // the filename
    public int lineIdx; // the line number, 0-based

    public LineID(String fileName, int lineNo) {
        this.fileName = fileName;
        this.lineIdx = lineNo;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Test whether this {@link LineID} is equal to another object. Two
     * {@link LineID}'s are equal when both their fileName and lineNo are equal.
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
        if (this.lineIdx != other.lineIdx) {
            return false;
        }
        return true;
    }

    /**
     * Output a string representation in the form fileName:lineNo+1. Note this
     * uses a 1-based line number as is customary for human-readable line
     * numbers.
     *
     * @return the string representation of this line ID
     */
    @Override
    public String toString() {
        return fileName + ":" + (lineIdx + 1);
    }

    /**
     * Retrieve a copy of this line ID.
     *
     * @return the copy
     */
    @Override
    public LineID clone() {
        return new LineID(fileName, lineIdx);
    }
}
