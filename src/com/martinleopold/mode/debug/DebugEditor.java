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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.text.Document;
import processing.app.*;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextAreaDefaults;
import processing.mode.java.JavaEditor;

/**
 * Main View Class. Handles the editor window incl. toolbar and menu. Has access
 * to the Sketch.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class DebugEditor extends JavaEditor implements ActionListener {

    public static final Color BREAKPOINT_COLOR = new Color(255, 170, 170); // the background color for highlighting elines
    public static final Color CURRENT_LINE_COLOR = new Color(255, 255, 0); // the background color for highlighting lines
    // important fields from superclass
    //protected Sketch sketch;
    //private JMenu fileMenu;
    //protected EditorToolbar toolbar;
    protected JMenu debugMenu;
    // debugger control
    protected JMenuItem debugMenuItem;
    protected JMenuItem continueMenuItem;
    protected JMenuItem stopMenuItem;
    // breakpoints
    protected JMenuItem setBreakpointMenuItem;
    protected JMenuItem removeBreakpointMenuItem;
    protected JMenuItem listBreakpointsMenuItem;
    // stepping
    protected JMenuItem stepOverMenuItem;
    protected JMenuItem stepIntoMenuItem;
    protected JMenuItem stepOutMenuItem;
    // info
    protected JMenuItem printStackTraceMenuItem;
    protected JMenuItem printLocalsMenuItem;
    protected JMenuItem printThisMenuItem;
    protected JMenuItem printSourceMenuItem;
    // variable inspector
    protected JMenuItem toggleVariableInspectorMenuItem;
    protected DebugMode dmode;
    protected Debugger dbg;
    protected VariableInspector vi;
    protected TextArea ta;

    public DebugEditor(Base base, String path, EditorState state, Mode mode) {
        super(base, path, state, mode);

        // add debug menu to editor frame
        JMenuBar menuBar = getJMenuBar();
        menuBar.add(buildDebugMenu());
        dmode = (DebugMode) mode;

        // init controller class
        dbg = new Debugger(this);

        // variable inspector window
        vi = new VariableInspector();

        // access to customized (i.e. subclassed) text area
        ta = (TextArea) textarea;

        // set action on frame close
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                onWindowClosing(e);
            }
        });
    }

    /**
     * Event handler called when closing the editor window. Kills the variable
     * inspector window.
     *
     * @param e the event object
     */
    protected void onWindowClosing(WindowEvent e) {
        System.out.println("closing window");
        // remove var.inspector
        vi.dispose();
    }

    /**
     * Creates the debug menu. Includes ActionListeners for the menu items.
     * Intended for adding to the menu bar.
     *
     * @return The debug menu
     */
    protected JMenu buildDebugMenu() {
        debugMenu = new JMenu("Debug");

        debugMenuItem = new JMenuItem("Debug");
        debugMenuItem.addActionListener(this);
        continueMenuItem = new JMenuItem("Continue");
        continueMenuItem.addActionListener(this);
        stopMenuItem = new JMenuItem("Stop");
        stopMenuItem.addActionListener(this);

        setBreakpointMenuItem = new JMenuItem("Set Breakpoint");
        setBreakpointMenuItem.addActionListener(this);
        removeBreakpointMenuItem = new JMenuItem("Remove Breakpoint");
        removeBreakpointMenuItem.addActionListener(this);
        listBreakpointsMenuItem = new JMenuItem("List Breakpoints");
        listBreakpointsMenuItem.addActionListener(this);

        stepOverMenuItem = new JMenuItem("Step Over");
        stepOverMenuItem.addActionListener(this);
        stepIntoMenuItem = new JMenuItem("Step Into");
        stepIntoMenuItem.addActionListener(this);
        stepOutMenuItem = new JMenuItem("Step Out");
        stepOutMenuItem.addActionListener(this);

        printStackTraceMenuItem = new JMenuItem("Print Stack trace");
        printStackTraceMenuItem.addActionListener(this);
        printLocalsMenuItem = new JMenuItem("Print Locals");
        printLocalsMenuItem.addActionListener(this);
        printThisMenuItem = new JMenuItem("Print this fields");
        printThisMenuItem.addActionListener(this);
        printSourceMenuItem = new JMenuItem("Print Source Location");
        printSourceMenuItem.addActionListener(this);

        toggleVariableInspectorMenuItem = new JMenuItem("Show/Hide Variable Inspector");
        toggleVariableInspectorMenuItem.addActionListener(this);

        debugMenu.add(debugMenuItem);
        debugMenu.add(continueMenuItem);
        debugMenu.add(stopMenuItem);
        debugMenu.addSeparator();
        debugMenu.add(setBreakpointMenuItem);
        debugMenu.add(removeBreakpointMenuItem);
        debugMenu.add(listBreakpointsMenuItem);
        debugMenu.addSeparator();
        debugMenu.add(stepOverMenuItem);
        debugMenu.add(stepIntoMenuItem);
        debugMenu.add(stepOutMenuItem);
        debugMenu.addSeparator();
        debugMenu.add(printStackTraceMenuItem);
        debugMenu.add(printLocalsMenuItem);
        debugMenu.add(printThisMenuItem);
        debugMenu.add(printSourceMenuItem);
        debugMenu.addSeparator();
        debugMenu.add(toggleVariableInspectorMenuItem);
        return debugMenu;
    }

    /**
     * Callback for menu items. Implementation of Swing ActionListener.
     *
     * @param ae Action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        //System.out.println("ActionEvent: " + ae.toString());

        JMenuItem source = (JMenuItem) ae.getSource();
        if (source == debugMenuItem) {
            System.out.println("# clicked debug menu item");
            //dmode.handleDebug(sketch, this);
            dbg.startDebug();
        } else if (source == stopMenuItem) {
            System.out.println("# clicked stop menu item");
            //dmode.handleDebug(sketch, this);
            dbg.stopDebug();
        } else if (source == continueMenuItem) {
            System.out.println("# clicked continue menu item");
            //dmode.handleDebug(sketch, this);
            dbg.continueDebug();
        } else if (source == stepOverMenuItem) {
            System.out.println("# clicked step over menu item");
            dbg.stepOver();
        } else if (source == stepIntoMenuItem) {
            System.out.println("# clicked step into menu item");
            dbg.stepInto();
        } else if (source == stepOutMenuItem) {
            System.out.println("# clicked step out menu item");
            dbg.stepOut();
        } else if (source == printStackTraceMenuItem) {
            System.out.println("# clicked print stack trace menu item");
            dbg.printStackTrace();
        } else if (source == printLocalsMenuItem) {
            System.out.println("# clicked print locals menu item");
            dbg.printLocals();
        } else if (source == printThisMenuItem) {
            System.out.println("# clicked print this menu item");
            dbg.printThis();
        } else if (source == printSourceMenuItem) {
            System.out.println("# clicked print source menu item");
            dbg.printSource();
        } else if (source == setBreakpointMenuItem) {
            System.out.println("# clicked set breakpoint menu item");
            dbg.setBreakpoint();
        } else if (source == removeBreakpointMenuItem) {
            System.out.println("# clicked remove breakpoint menu item");
            dbg.removeBreakpoint();
        } else if (source == listBreakpointsMenuItem) {
            System.out.println("# clicked list breakpoints menu item");
            dbg.listBreakpoints();
        } else if (source == toggleVariableInspectorMenuItem) {
            System.out.println("# clicked show/hide variable inspector menu item");
            toggleVariableInspector();
        }
    }

//    @Override
//    public void handleRun() {
//        dbg.continueDebug();
//    }
    /**
     * Event handler called when hitting the stop button. Stops a running debug
     * session or performs standard stop action if not currently debugging.
     */
    @Override
    public void handleStop() {
        if (dbg.isConnected()) {
            dbg.stopDebug();
        } else {
            super.handleStop();
        }
    }

    /**
     * Clear the console.
     */
    public void clearConsole() {
        console.clear();
    }

    /**
     * Clear current text selection.
     */
    public void clearSelection() {
        setSelection(getCaretOffset(), getCaretOffset());
    }

    /**
     * Select a line in the current tab.
     *
     * @param lineIdx 0-based line number
     */
    public void selectLine(int lineIdx) {
        setSelection(getLineStartOffset(lineIdx), getLineStopOffset(lineIdx));
    }

    /**
     * Set the cursor to the start of a line.
     *
     * @param lineIdx 0-based line number
     */
    public void cursorToLineStart(int lineIdx) {
        setSelection(getLineStartOffset(lineIdx), getLineStartOffset(lineIdx));
    }

    /**
     * Set the cursor to the end of a line.
     *
     * @param lineIdx 0-based line number
     */
    public void cursorToLineEnd(int lineIdx) {
        setSelection(getLineStopOffset(lineIdx), getLineStopOffset(lineIdx));
    }

    /**
     * Access variable inspector window.
     *
     * @return the variable inspector object
     */
    public VariableInspector variableInspector() {
        return vi;
    }

    /**
     * Access the debugger.
     *
     * @return the debugger controller object
     */
    public Debugger dbg() {
        return dbg;
    }

    /**
     * Show the variable inspector window.
     */
    public void showVariableInspector() {
        vi.setVisible(true);
    }

    /**
     * Set visibility of the variable inspector window.
     *
     * @param visible true to set the variable inspector visible, false for
     * invisible.
     */
    public void showVariableInspector(boolean visible) {
        vi.setVisible(visible);
    }

    /**
     * Hide the variable inspector window.
     */
    public void hideVariableInspector() {
        vi.setVisible(true);
    }

    /**
     * Toggle visibility of the variable inspector window.
     */
    public void toggleVariableInspector() {
        vi.setVisible(!vi.isVisible());
    }

    /**
     * Text area factory method. Instantiates the customized TextArea.
     *
     * @return the customized text area object
     */
    @Override
    protected JEditTextArea createTextArea() {
        //System.out.println("overriding creation of text area");
        return new TextArea(new PdeTextAreaDefaults(mode));
    }
    protected Map<LineID, List<Color>> lineColors = new HashMap(); // holds background colors for lines

    /**
     * Set background color of a sketch line.
     *
     * @param l identifies sketch line to colorize
     * @param c the color
     */
    public void setLineBgColor(LineID l, Color c) {
        // push, overwriting the color for this line
        List<Color> colors = lineColors.get(l);
        if (colors == null) {
            colors = new ArrayList();
            colors.add(c);
            lineColors.put(l, colors);
        } else {
            colors.add(c);
        }
        ta.setLineBgColor(l.lineNo - 1, c);
    }

    /**
     * Clear background color of a sketch line.
     *
     * @param l identifies sketch line to clear
     */
    public void clearLineBgColor(LineID l) {
        // remove last and restore previous color
        List<Color> colors = lineColors.get(l);
        if (colors != null) {
            if (colors.size() > 0) {
                colors.remove(colors.size() - 1);
            }
            if (colors.size() > 0) {
                ta.setLineBgColor(l.lineNo - 1, colors.get(colors.size() - 1));
            } else {
                // no more colors for this line
                ta.clearLineBgColor(l.lineNo - 1);
                lineColors.remove(l); // remove the whole map entry, since the list is empty anyway
            }
        }
    }

    /**
     * Event handler called when switching between tabs. Loads all line
     * background colors set for the tab.
     *
     * @param code tab to switch to
     */
    @Override
    protected void setCode(SketchCode code) {
        //System.out.println("tab switch");
        super.setCode(code); // set the new document in the textarea, etc. need to do this first

        // set line background colors for tab
        if (ta != null) { // can be null when setCode is called the first time (in constructor)
            // clear all line backgrounds
            ta.clearLineBgColors();
            // load appropriate line backgrounds for tab
            for (Map.Entry<LineID, List<Color>> e : lineColors.entrySet()) {
                LineID l = e.getKey();
                List<Color> colors = e.getValue();
                if (colors.size() > 0) {
                    if (l.fileName.equals(code.getFileName())) {
                        ta.setLineBgColor(l.lineNo - 1, colors.get(colors.size() - 1));
                    }
                }
            }
        }
    }

    /**
     * Access the currently edited document.
     *
     * @return the document object
     */
    public Document currentDocument() {
        return ta.getDocument();
    }

    /**
     * Factory method for the editor toolbar. Instantiates the customized
     * toolbar.
     *
     * @return the toolbar
     */
    @Override
    public EditorToolbar createToolbar() {
        return new DebugToolbar(this, base);
    }
}
