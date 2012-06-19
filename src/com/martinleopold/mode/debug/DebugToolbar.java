package com.martinleopold.mode.debug;

import java.awt.Image;
import java.awt.event.MouseEvent;
import processing.app.Base;
import processing.app.Editor;
import processing.mode.java.JavaToolbar;

/**
 * Custom toolbar for the editor window.
 *
 * @author mlg
 */
public class DebugToolbar extends JavaToolbar {

    static protected final int RUN = 0;
    static protected final int DEBUG = 1;
    static protected final int CONTINUE = 2;
    static protected final int STOP = 3;
    static protected final int TOGGLE_BREAKPOINT = 4;
    static protected final int STEP_OVER = 5;
    static protected final int STEP_INTO = 6;
    static protected final int STEP_OUT = 7;
    static protected final int TOGGLE_VAR_INSPECTOR = 8;
    static protected final int NEW = 9;
    static protected final int OPEN = 10;
    static protected final int SAVE = 11;
    static protected final int EXPORT = 12;

    public DebugToolbar(Editor editor, Base base) {
        super(editor, base);
    }

    /**
     * Initialize buttons. Loads images and adds the buttons to the toolbar.
     */
    @Override
    public void init() {
        Image[][] images = loadImages();
        for (int i = 0; i < 13; i++) {
            addButton(getTitle(i, false), getTitle(i, true), images[i], i == NEW);
        }
    }

    /**
     * Get the title for a toolbar button. Displayed in the toolbar when
     * hovering over a button.
     *
     * @param index index of the toolbar button
     * @param shift true if shift is pressed
     * @return the title
     */
    public static String getTitle(int index, boolean shift) {
        switch (index) {
            case RUN:
                return JavaToolbar.getTitle(JavaToolbar.RUN, shift);
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
                return "Debug";
            case CONTINUE:
                return "Continue";
            case TOGGLE_BREAKPOINT:
                return "Toggle Breakpoint";
            case STEP_OVER:
                return "Step";
            case STEP_INTO:
                return "Step into";
            case STEP_OUT:
                return "Step out";
            case TOGGLE_VAR_INSPECTOR:
                return "Toggle Variable Inspector";
        }
        return null;
    }

    /**
     * Event handler called when a toolbar button is clicked.
     *
     * @param e the mouse event
     * @param sel index of the toolbar button clicked
     */
    @Override
    public void handlePressed(MouseEvent e, int sel) {
        boolean shift = e.isShiftDown();
        DebugEditor deditor = (DebugEditor) editor;

        switch (sel) {
            case RUN:
                super.handlePressed(e, JavaToolbar.RUN);
                break;
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
                deditor.dbg.startDebug();
                break;
            case CONTINUE:
                deditor.dbg.continueDebug();
                break;
            case TOGGLE_BREAKPOINT:
                deditor.dbg.toggleBreakpoint();
                break;
            case STEP_OVER:
                deditor.dbg.stepOver();
                break;
            case STEP_INTO:
                deditor.dbg.stepInto();
                break;
            case STEP_OUT:
                deditor.dbg.stepOut();
                break;
            case TOGGLE_VAR_INSPECTOR:
                deditor.toggleVariableInspector();
                break;
        }
    }
}
