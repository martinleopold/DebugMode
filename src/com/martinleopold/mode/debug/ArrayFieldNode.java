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

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.Value;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class ArrayFieldNode extends VariableNode {

    protected ArrayReference array;
    protected int index;

    public ArrayFieldNode(String name, String type, Value value, ArrayReference array, int index) {
        super(name, type, value);
        this.array = array;
    }

    @Override
    public void setValue(Value value) {
        try {
            array.setValue(index, value);
        } catch (InvalidTypeException ex) {
            Logger.getLogger(ArrayFieldNode.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotLoadedException ex) {
            Logger.getLogger(ArrayFieldNode.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.value = value;
    }
}
