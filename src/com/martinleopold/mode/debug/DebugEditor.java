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
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.text.Document;
import processing.app.*;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextAreaDefaults;
import processing.core.PApplet;
import processing.mode.java.JavaEditor;

/**
 * Main View Class. Handles the editor window including tool bar and menu. Has
 * access to the Sketch. Provides line highlighting (for breakpoints and the
 * debuggers current line).
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class DebugEditor extends JavaEditor implements ActionListener {
    // important fields from superclass
    //protected Sketch sketch;
    //private JMenu fileMenu;
    //protected EditorToolbar toolbar;

    // highlighting
    public static final Color BREAKPOINT_COLOR = new Color(240, 240, 240); // the background color for highlighting lines
    public static final Color CURRENT_LINE_COLOR = new Color(255, 255, 150); // the background color for highlighting lines
    public static final String BREAKPOINT_MARKER = "<>"; // the text marker for highlighting breakpoints in the gutter
    public static final String CURRENT_LINE_MARKER = "->"; // the text marker for highlighting the current line in the gutter
    public static final Color BREAKPOINT_MARKER_COLOR = new Color(74, 84, 94); // the color of breakpoint gutter markers
    public static final Color CURRENT_LINE_MARKER_COLOR = new Color(226, 117, 0); // the color of current line gutter markers
    protected List<LineHighlight> breakpointedLines = new ArrayList(); // breakpointed lines
    protected LineHighlight currentLine; // line the debugger is currently suspended at
    protected final String breakpointMarkerComment = " //<>//"; // breakpoint marker comment
    // menus
    protected JMenu debugMenu; // the debug menu
    // debugger control
    protected JMenuItem debugMenuItem;
    protected JMenuItem continueMenuItem;
    protected JMenuItem stopMenuItem;
    // breakpoints
    protected JMenuItem toggleBreakpointMenuItem;
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
    protected JMenuItem printThreads;
    // variable inspector
    protected JMenuItem toggleVariableInspectorMenuItem;
    // references
    protected DebugMode dmode; // the mode
    protected Debugger dbg; // the debugger
    protected VariableInspector vi; // the variable inspector frame
    protected TextArea ta; // the text area

    public DebugEditor(Base base, String path, EditorState state, Mode mode) {
        super(base, path, state, mode);

        // get mode
        dmode = (DebugMode) mode;

        // init controller class
        dbg = new Debugger(this);

        // variable inspector window
        vi = new VariableInspector(this);

        // access to customized (i.e. subclassed) text area
        ta = (TextArea) textarea;

        // set action on frame close
//        addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowClosing(WindowEvent e) {
//                onWindowClosing(e);
//            }
//        });

        // set breakpoints from marker comments
        for (LineID lineID : stripBreakpointComments()) {
            //System.out.println("setting: " + lineID);
            dbg.setBreakpoint(lineID);
        }
        getSketch().setModified(false); // setting breakpoints will flag sketch as modified, so override this here
    }

//    /**
//     * Event handler called when closing the editor window. Kills the variable
//     * inspector window.
//     *
//     * @param e the event object
//     */
//    protected void onWindowClosing(WindowEvent e) {
//        // remove var.inspector
//        vi.dispose();
//        // quit running debug session
//        dbg.stopDebug();
//    }
    /**
     * Used instead of the windowClosing event handler, since it's not called on
     * mode switch. Called when closing the editor window. Stops running debug
     * sessions and kills the variable inspector window.
     */
    @Override
    public void dispose() {
        //System.out.println("window dispose");
        // quit running debug session
        dbg.stopDebug();
        // remove var.inspector
        vi.dispose();
        // original dispose
        super.dispose();
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

        toggleBreakpointMenuItem = new JMenuItem("Toggle Breakpoint");
        toggleBreakpointMenuItem.addActionListener(this);
        listBreakpointsMenuItem = new JMenuItem("List Breakpoints");
        listBreakpointsMenuItem.addActionListener(this);

        stepOverMenuItem = new JMenuItem("Step");
        stepOverMenuItem.addActionListener(this);
        stepIntoMenuItem = new JMenuItem("Step Into");
        stepIntoMenuItem.addActionListener(this);
        stepOutMenuItem = new JMenuItem("Step Out");
        stepOutMenuItem.addActionListener(this);

        printStackTraceMenuItem = new JMenuItem("Print Stack Trace");
        printStackTraceMenuItem.addActionListener(this);
        printLocalsMenuItem = new JMenuItem("Print Locals");
        printLocalsMenuItem.addActionListener(this);
        printThisMenuItem = new JMenuItem("Print Fields");
        printThisMenuItem.addActionListener(this);
        printSourceMenuItem = new JMenuItem("Print Source Location");
        printSourceMenuItem.addActionListener(this);
        printThreads = new JMenuItem("Print Threads");
        printThreads.addActionListener(this);

        toggleVariableInspectorMenuItem = new JMenuItem("Toggle Variable Inspector");
        toggleVariableInspectorMenuItem.addActionListener(this);

        debugMenu.add(debugMenuItem);
        debugMenu.add(continueMenuItem);
        debugMenu.add(stopMenuItem);
        debugMenu.addSeparator();
        debugMenu.add(toggleBreakpointMenuItem);
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
        debugMenu.add(printThreads);
        debugMenu.addSeparator();
        debugMenu.add(toggleVariableInspectorMenuItem);
        return debugMenu;
    }

    @Override
    public JMenu buildModeMenu() {
        return buildDebugMenu();
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
        } else if (source == printThreads) {
            System.out.println("# clicked print threads menu item");
            dbg.printThreads();
        } else if (source == toggleBreakpointMenuItem) {
            System.out.println("# clicked toggle breakpoint menu item");
            dbg.toggleBreakpoint();
        } else if (source == listBreakpointsMenuItem) {
            System.out.println("# clicked list breakpoints menu item");
            dbg.listBreakpoints();
        } else if (source == toggleVariableInspectorMenuItem) {
            System.out.println("# clicked toggle variable inspector menu item");
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
        if (dbg.isStarted()) {
            dbg.stopDebug();
        } else {
            super.handleStop();
        }
    }

    /**
     * Event handler called when loading another sketch in this editor. Clears
     * breakpoints of previous sketch.
     *
     * @param path
     * @return true if a sketch was opened, false if aborted
     */
    @Override
    protected boolean handleOpenInternal(String path) {
        boolean didOpen = super.handleOpenInternal(path);
        if (didOpen && dbg != null) {
            // should already been stopped (open calls handleStop)
            dbg.clearBreakpoints();
            clearBreakpointedLines(); // force clear breakpoint highlights
            variableInspector().reset(); // clear contents of variable inspector
        }
        return didOpen;
    }

    /**
     * Extract breakpointed lines from source code marker comments. This removes
     * marker comments from the editor text. Intended to be called on loading a
     * sketch, since re-setting the sketches contents after removing the markers
     * will clear all breakpoints.
     *
     * @return the list of {@link LineID}s where breakpoint marker comments were
     * removed from.
     */
    protected List<LineID> stripBreakpointComments() {
        List<LineID> bps = new ArrayList();
        // iterate over all tabs
        Sketch sketch = getSketch();
        for (int i = 0; i < sketch.getCodeCount(); i++) {
            SketchCode tab = sketch.getCode(i);
            String code = tab.getProgram();
            String lines[] = code.split("\\r?\\n"); // newlines not included
            //System.out.println(code);

            // scan code for breakpoint comments
            int lineIdx = 0;
            for (String line : lines) {
                //System.out.println(line);
                if (line.endsWith(breakpointMarkerComment)) {
                    LineID lineID = new LineID(tab.getFileName(), lineIdx);
                    bps.add(lineID);
                    //System.out.println("found breakpoint: " + lineID);
                    // got a breakpoint
                    //dbg.setBreakpoint(lineID);
                    int index = line.lastIndexOf(breakpointMarkerComment);
                    lines[lineIdx] = line.substring(0, index);
                }
                lineIdx++;
            }
            //tab.setProgram(code);
            code = PApplet.join(lines, "\n");
            setTabContents(tab.getFileName(), code);
        }
        return bps;
    }

    /**
     * Add breakpoint marker comments to the source file of a specific tab. This
     * acts on the source file on disk, not the editor text. Intended to be
     * called just after saving the sketch.
     *
     * @param tabFilename the tab file name
     */
    protected void addBreakpointComments(String tabFilename) {
        SketchCode tab = getTab(tabFilename);
        List<LineBreakpoint> bps = dbg.getBreakpoints(tab.getFileName());

        // load the source file
        File sourceFile = new File(sketch.getFolder(), tab.getFileName());
        //System.out.println("file: " + sourceFile);
        try {
            String code = base.loadFile(sourceFile);
            //System.out.println("code: " + code);
            String lines[] = code.split("\\r?\\n"); // newlines not included
            for (LineBreakpoint bp : bps) {
                //System.out.println("adding bp: " + bp.lineID());
                lines[bp.lineID().lineIdx()] += breakpointMarkerComment;
            }
            code = PApplet.join(lines, "\n");
            //System.out.println("new code: " + code);
            base.saveFile(code, sourceFile);
        } catch (IOException ex) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean handleSave(boolean immediately) {
        //System.out.println("handleSave " + immediately);

        // note modified tabs
        final List<String> modified = new ArrayList();
        for (int i = 0; i < getSketch().getCodeCount(); i++) {
            SketchCode tab = getSketch().getCode(i);
            if (tab.isModified()) {
                modified.add(tab.getFileName());
            }
        }

        boolean saved = super.handleSave(immediately);
        if (saved) {
            if (immediately) {
                for (String tabFilename : modified) {
                    addBreakpointComments(tabFilename);
                }
            } else {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        for (String tabFilename : modified) {
                            addBreakpointComments(tabFilename);
                        }
                    }
                });
            }
        }
        return saved;
    }

    @Override
    public boolean handleSaveAs() {
        //System.out.println("handleSaveAs");
        String oldName = getSketch().getCode(0).getFileName();
        //System.out.println("old name: " + oldName);
        boolean saved = super.handleSaveAs();
        if (saved) {
            // re-set breakpoints in first tab (name has changed)
            List<LineBreakpoint> bps = dbg.getBreakpoints(oldName);
            dbg.clearBreakpoints(oldName);
            String newName = getSketch().getCode(0).getFileName();
            //System.out.println("new name: " + newName);
            for (LineBreakpoint bp : bps) {
                LineID line = new LineID(newName, bp.lineID().lineIdx());
                //System.out.println("setting: " + line);
                dbg.setBreakpoint(line);
            }
            // add breakpoint marker comments to source file
            for (int i = 0; i < getSketch().getCodeCount(); i++) {
                addBreakpointComments(getSketch().getCode(i).getFileName());
            }

            // set new name of variable inspector
            vi.setTitle(getSketch().getName());
        }
        return saved;
    }

    /**
     * Set text contents of a specific tab. Updates underlying document and text
     * area. Clears Breakpoints.
     *
     * @param tabFilename the tab file name
     * @param code the text to set
     */
    protected void setTabContents(String tabFilename, String code) {
        // remove all breakpoints of this tab
        dbg.clearBreakpoints(tabFilename);

        SketchCode currentTab = getCurrentTab();

        // set code of tab
        SketchCode tab = getTab(tabFilename);
        if (tab != null) {
            tab.setProgram(code);
            // this updates document and text area
            // TODO: does this have any negative effects? (setting the doc to null)
            tab.setDocument(null);
            setCode(tab);

            // switch back to original tab
            setCode(currentTab);
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
     * Switch to a tab.
     *
     * @param tabFileName the file name identifying the tab. (as in
     * {@link SketchCode#getFileName()})
     */
    public void switchToTab(String tabFileName) {
        Sketch s = getSketch();
        for (int i = 0; i < s.getCodeCount(); i++) {
            if (tabFileName.equals(s.getCode(i).getFileName())) {
                s.setCurrentCode(i);
                break;
            }
        }
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
     * Access the mode.
     *
     * @return the mode object
     */
    public DebugMode mode() {
        return dmode;
    }

    /**
     * Access the custom text area object.
     *
     * @return the text area object
     */
    public TextArea textArea() {
        return ta;
    }

    /**
     * Access variable inspector window.
     *
     * @return the variable inspector object
     */
    public VariableInspector variableInspector() {
        return vi;
    }

    public DebugToolbar toolbar() {
        return (DebugToolbar) toolbar;
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
        return new TextArea(new PdeTextAreaDefaults(mode), this);
    }

    /**
     * Set the line to highlight as currently suspended at. Will override the
     * breakpoint color, if set. Switches to the appropriate tab and scroll to
     * the line by placing the cursor there.
     *
     * @param line the line to highlight as current suspended line
     */
    public void setCurrentLine(LineID line) {

        clearCurrentLine();
        if (line == null) {
            return; // safety, e.g. when no line mapping is found and the null line is used.
        }
        switchToTab(line.fileName());
        // scroll to line, by setting the cursor
        cursorToLineStart(line.lineIdx());
        // highlight line
        currentLine = new LineHighlight(line.lineIdx(), CURRENT_LINE_COLOR, this);
        currentLine.setMarker(CURRENT_LINE_MARKER, CURRENT_LINE_MARKER_COLOR);
        System.out.println("setting current line highlight: " + currentLine.hashCode());
    }

    /**
     * Clear the highlight for the debuggers current line.
     */
    public void clearCurrentLine() {
        if (currentLine != null) {
            currentLine.clear();
            currentLine.dispose();

            // revert to breakpoint color if any is set on this line
            for (LineHighlight hl : breakpointedLines) {
                if (hl.lineID().equals(currentLine.lineID())) {
                    hl.paint();
                    break;
                }
            }
            currentLine = null;
        }
    }

    /**
     * Add highlight for a breakpointed line.
     *
     * @param lineID the line id to highlight as breakpointed
     */
    public void addBreakpointedLine(LineID lineID) {
        LineHighlight hl = new LineHighlight(lineID, BREAKPOINT_COLOR, this);
        System.out.println("setting breakpoint line highlight: " + hl.hashCode());
        hl.setMarker(BREAKPOINT_MARKER, BREAKPOINT_MARKER_COLOR);
        breakpointedLines.add(hl);
        // repaint current line if it's on this line
        if (currentLine != null && currentLine.lineID().equals(lineID)) {
            currentLine.paint();
        }
    }

    /**
     * Add highlight for a breakpointed line on the current tab.
     *
     * @param lineIdx the line index on the current tab to highlight as
     * breakpointed
     */
    //TODO: remove and replace by {@link #addBreakpointedLine(LineID lineID)}
    public void addBreakpointedLine(int lineIdx) {
        addBreakpointedLine(getLineIDInCurrentTab(lineIdx));
    }

    /**
     * Remove a highlight for a breakpointed line. Needs to be on the current
     * tab.
     *
     * @param lineIdx the line index on the current tab to remove a breakpoint
     * highlight from
     */
    public void removeBreakpointedLine(int lineIdx) {
        LineID line = getLineIDInCurrentTab(lineIdx);
        //System.out.println("line id: " + line.fileName() + " " + line.lineIdx());
        LineHighlight foundLine = null;
        for (LineHighlight hl : breakpointedLines) {
            if (hl.lineID.equals(line)) {
                foundLine = hl;
                break;
            }
        }
        if (foundLine != null) {
            foundLine.clear();
            breakpointedLines.remove(foundLine);
            foundLine.dispose();
            // repaint current line if it's on this line
            if (currentLine != null && currentLine.lineID().equals(line)) {
                currentLine.paint();
            }
        }
    }

    /**
     * Remove all highlights for breakpointed lines.
     */
    public void clearBreakpointedLines() {
        for (LineHighlight hl : breakpointedLines) {
            hl.clear();
            hl.dispose();
        }
        breakpointedLines.clear(); // remove all breakpoints
        // fix highlights not being removed when tab names have changed due to opening a new sketch in same editor
        ta.clearLineBgColors(); // force clear all highlights
        ta.clearGutterText();

        // repaint current line
        if (currentLine != null) {
            currentLine.paint();
        }
    }

    /**
     * Retrieve a {@link LineID} object for a line on the current tab.
     *
     * @param lineIdx the line index on the current tab
     * @return the {@link LineID} object representing a line index on the
     * current tab
     */
    public LineID getLineIDInCurrentTab(int lineIdx) {
        return new LineID(getSketch().getCurrentCode().getFileName(), lineIdx);
    }

    /**
     * Retrieve line of sketch where the cursor currently resides.
     *
     * @return the current {@link LineID}
     */
    protected LineID getCurrentLineID() {
        String tab = getSketch().getCurrentCode().getFileName();
        int lineNo = getTextArea().getCaretLine();
        return new LineID(tab, lineNo);
    }

    /**
     * Check whether a {@link LineID} is on the current tab.
     *
     * @param line the {@link LineID}
     * @return true, if the {@link LineID} is on the current tab.
     */
    public boolean isInCurrentTab(LineID line) {
        return line.fileName().equals(getSketch().getCurrentCode().getFileName());
    }

    /**
     * Event handler called when switching between tabs. Loads all line
     * background colors set for the tab.
     *
     * @param code tab to switch to
     */
    @Override
    protected void setCode(SketchCode code) {
        //System.out.println("tab switch: " + code.getFileName());
        super.setCode(code); // set the new document in the textarea, etc. need to do this first

        // set line background colors for tab
        if (ta != null) { // can be null when setCode is called the first time (in constructor)
            // clear all line backgrounds
            ta.clearLineBgColors();
            // clear all gutter text
            ta.clearGutterText();
            // load appropriate line backgrounds for tab
            // first paint breakpoints
            for (LineHighlight hl : breakpointedLines) {
                if (isInCurrentTab(hl.lineID())) {
                    hl.paint();
                }
            }
            // now paint current line (if any)
            if (currentLine != null) {
                if (isInCurrentTab(currentLine.lineID())) {
                    currentLine.paint();
                }
            }
        }
        if (dbg() != null && dbg().isStarted()) {
            dbg().startTrackingLineChanges();
        }
    }

    /**
     * Get a tab by its file name.
     *
     * @param fileName the filename to search for.
     * @return the {@link SketchCode} object representing the tab, or null if
     * not found
     */
    public SketchCode getTab(String fileName) {
        Sketch s = getSketch();
        for (SketchCode c : s.getCode()) {
            if (c.getFileName().equals(fileName)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Retrieve the current tab.
     *
     * @return the {@link SketchCode} representing the current tab
     */
    public SketchCode getCurrentTab() {
        return getSketch().getCurrentCode();
    }

    /**
     * Access the currently edited document.
     *
     * @return the document object
     */
    public Document currentDocument() {
        //return ta.getDocument();
        return getCurrentTab().getDocument();
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

    /**
     * Event Handler for double clicking in the left hand gutter area.
     *
     * @param lineIdx the line (0-based) that was double clicked
     */
    public void gutterDblClicked(int lineIdx) {
        if (dbg != null) {
            dbg.toggleBreakpoint(lineIdx);
        }
    }
}
