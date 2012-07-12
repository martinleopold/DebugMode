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

import java.awt.Image;
import java.awt.event.MouseEvent;
import processing.app.Base;
import processing.app.Editor;
import processing.mode.java.JavaToolbar;

/**
 * Custom toolbar for the editor window.
 * TODO: explain id and index
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class DebugToolbar extends JavaToolbar {
    /*
     static protected final int RUN    = 0;
     static protected final int STOP   = 1;

     static protected final int NEW    = 2;
     static protected final int OPEN   = 3;
     static protected final int SAVE   = 4;
     static protected final int EXPORT = 5;
     */

    // preserve original button id's
    static protected final int DEBUG = RUN;
    static protected final int CONTINUE = 101;
    static protected final int STEP = 102;
    //static protected final int STOP = 1;
    static protected final int TOGGLE_BREAKPOINT = 103;
    static protected final int TOGGLE_VAR_INSPECTOR = 104;
    //static protected final int NEW = 6;
    //static protected final int OPEN = 7;
    //static protected final int SAVE = 8;
    //static protected final int EXPORT = 9;

    public DebugToolbar(Editor editor, Base base) {
        super(editor, base);
    }
    static protected final int[] buttonSequence = {
        DEBUG, CONTINUE, STEP, STOP, TOGGLE_BREAKPOINT, TOGGLE_VAR_INSPECTOR, NEW, OPEN, SAVE, EXPORT
    };

    protected int translateButton(int button) {
        switch (button) {
            case JavaToolbar.RUN:
                return DEBUG;
            case JavaToolbar.STOP:
                return STOP;
            case JavaToolbar.NEW:
                return NEW;
            case JavaToolbar.OPEN:
                return OPEN;
            case JavaToolbar.SAVE:
                return SAVE;
            case JavaToolbar.EXPORT:
                return EXPORT;
            default:
                return button;
        }
    }

    /**
     * Initialize buttons. Loads images and adds the buttons to the toolbar.
     */
    @Override
    public void init() {
        Image[][] images = loadImages();
        for (int idx = 0; idx < buttonSequence.length; idx++) {
            int id = buttonId(idx);
            addButton(getTitle(id, false), getTitle(id, true), images[idx], id == NEW || id == TOGGLE_BREAKPOINT);
        }
    }

    /**
     * Get the title for a toolbar button. Displayed in the toolbar when
     * hovering over a button.
     *
     * @param id id of the toolbar button
     * @param shift true if shift is pressed
     * @return the title
     */
    public static String getTitle(int id, boolean shift) {
        switch (id) {
//            case RUN:
//                return JavaToolbar.getTitle(JavaToolbar.RUN, shift);
            case STOP:
                return JavaToolbar.getTitle(JavaToolbar.STOP, shift);
            case NEW:
                return JavaToolbar.getTitle(JavaToolbar.NEW, shift);
            case OPEN:
                return JavaToolbar.getTitle(JavaToolbar.OPEN, shift);
            case SAVE:
                return JavaToolbar.getTitle(JavaToolbar.SAVE, shift);
            case EXPORT:
                return JavaToolbar.getTitle(JavaToolbar.EXPORT, shift);
            case DEBUG:
                if (shift) {
                    return "Run";
                } else {
                    return "Debug";
                }
            case CONTINUE:
                return "Continue";
            case TOGGLE_BREAKPOINT:
                return "Toggle Breakpoint";
            case STEP:
                if (shift) {
                    return "Step Into";
                } else {
                    return "Step";
                }
            case TOGGLE_VAR_INSPECTOR:
                return "Variable Inspector";
        }
        return null;
    }

    /**
     * Event handler called when a toolbar button is clicked.
     *
     * @param e the mouse event
     * @param idx index of the toolbar button clicked
     */
    @Override
    public void handlePressed(MouseEvent e, int idx) {
        boolean shift = e.isShiftDown();
        DebugEditor deditor = (DebugEditor) editor;
        int id = buttonId(idx);

        switch (id) {
//            case RUN:
//                super.handlePressed(e, JavaToolbar.RUN);
//                break;
            case STOP:
                super.handlePressed(e, JavaToolbar.STOP);
                break;
            case NEW:
                super.handlePressed(e, JavaToolbar.NEW);
                break;
            case OPEN:
                super.handlePressed(e, JavaToolbar.OPEN);
                break;
            case SAVE:
                super.handlePressed(e, JavaToolbar.SAVE);
                break;
            case EXPORT:
                super.handlePressed(e, JavaToolbar.EXPORT);
                break;
            case DEBUG:
                if (shift) {
                    deditor.handleRun();
                } else {
                    deditor.dbg.startDebug();
                }
                break;
            case CONTINUE:
                deditor.dbg.continueDebug();
                break;
            case TOGGLE_BREAKPOINT:
                deditor.dbg.toggleBreakpoint();
                break;
            case STEP:
                if (shift) {
                    deditor.dbg.stepInto();
                } else {
                    deditor.dbg.stepOver();
                }
                break;
//            case STEP_INTO:
//                deditor.dbg.stepInto();
//                break;
//            case STEP_OUT:
//                deditor.dbg.stepOut();
//                break;
            case TOGGLE_VAR_INSPECTOR:
                deditor.toggleVariableInspector();
                break;
        }
    }

    @Override
    public void activate(int id) {
        //System.out.println("activate: " + id);
        super.activate(buttonIndex(id));
    }

    @Override
    public void deactivate(int id) {
        //System.out.println("deactivate: " + id);
        super.deactivate(buttonIndex(id));
    }

    // get button position (index) from it's id (RUN, DEBUG, CONTINUE, STEP, ...)
    protected int buttonIndex(int buttonId) {
        for (int i=0; i<buttonSequence.length; i++) {
            if (buttonSequence[i] == buttonId) {
                return i;
            }
        }
        return -1;
    }

    // get the button id from its position (index)
    protected int buttonId(int buttonIdx) {
        return buttonSequence[buttonIdx];
    }
}
