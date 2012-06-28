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

import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * Model for a variable in the variable inspector. Has a type and name and
 * optionally a value. Can have sub-variables (as is the case for objects, and
 * arrays).
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class VariableNode implements MutableTreeNode {

    protected String type;
    protected String name;
    protected Value value;
    List<MutableTreeNode> children = new ArrayList();
    MutableTreeNode parent;

    public VariableNode(String name) {
        this.name = name;
        this.type = null;
        this.value = null;
    }

    public VariableNode(String name, String type) {
        this(name);
        this.type = type;
    }

    public VariableNode(String name, String type, Value value) {
        this(name, type);
        setValue(value);
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    public void addChild(VariableNode c) {
        children.add(c);
        c.setParent(this);
    }

    public void addChildren(List<VariableNode> children) {
        for (VariableNode child : children) {
            addChild(child);
        }
    }

    @Override
    public TreeNode getChildAt(int i) {
        return children.get(i);
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public int getIndex(TreeNode tn) {
        return children.indexOf(tn);
    }

    @Override
    public boolean getAllowsChildren() {
        return ((Value) value) instanceof ObjectReference;
    }

    /**
     * This controls the default icon and disclosure triangle.
     *
     * @return true, will show "folder" icon and disclosure triangle.
     */
    @Override
    public boolean isLeaf() {
        //return children.size() == 0;
        return !getAllowsChildren();
    }

    @Override
    public Enumeration children() {
        return Collections.enumeration(children);
    }

    @Override
    public String toString() {
        String str = name;
        if (type != null) {
            str += " (" + type + ")";
        }
        if (value != null) {
            str += ": " + value.toString();
        } else {
            str += ": " + "null";
        }
        return str;
    }

    @Override
    public void insert(MutableTreeNode mtn, int i) {
        children.add(i, this);
    }

    @Override
    public void remove(int i) {
        children.remove(i);
    }

    @Override
    public void remove(MutableTreeNode mtn) {
        children.remove(mtn);
        mtn.setParent(null);
    }

    @Override
    public void setUserObject(Object o) {
        if (o instanceof Value) {
            setValue((Value) o);
        }
    }

    @Override
    public void removeFromParent() {
        parent.remove(this);
        this.parent = null;
    }

    @Override
    public void setParent(MutableTreeNode mtn) {
        parent = mtn;
    }
}
