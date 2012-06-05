package com.martinleopold.mode.debug;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import processing.app.Base;
import processing.app.EditorState;
import processing.app.EditorToolbar;
import processing.app.Mode;
import processing.mode.java.JavaEditor;

/**
 * Main View Class.
 * Handles the editor window incl. toolbar and menu. Has access to the Sketch.
 * @author mlg
 */
public class DebugEditor extends JavaEditor implements ActionListener {

    // important fields from superclass
    //protected Sketch sketch;
    //private JMenu fileMenu;
    //protected EditorToolbar toolbar;

    JMenu debugMenu;
    // debugger control
    JMenuItem debugMenuItem;
    JMenuItem continueMenuItem;
    JMenuItem stopMenuItem;
    // breakpoints
    JMenuItem setBreakpointMenuItem;
    JMenuItem removeBreakpointMenuItem;
    JMenuItem listBreakpointsMenuItem;
    // stepping
    JMenuItem stepOverMenuItem;
    JMenuItem stepIntoMenuItem;
    JMenuItem stepOutMenuItem;
    // info
    JMenuItem printStackTraceMenuItem;
    JMenuItem printLocalsMenuItem;
    JMenuItem printThisMenuItem;
    JMenuItem printSourceMenuItem;

    DebugMode dmode;
    Debugger dbg;

    DebugEditor(Base base, String path, EditorState state, Mode mode) {
        super(base, path, state, mode);

        // add debug menu to editor frame
        JMenuBar menuBar = getJMenuBar();
        menuBar.add(buildDebugMenu());
        dmode = (DebugMode)mode;

        // init controller class
        dbg = new Debugger(this);
    }

    /**
     * Creates the debug menu.
     * Includes ActionListeners for the menu items. Intended for adding to the menu bar.
     * @return The debug menu
     */
    JMenu buildDebugMenu() {
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
        return debugMenu;
    }

    /**
     * Callback for menu items.
     * IMplementation of Swing ActionListener.
     * @param ae Action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        //System.out.println("ActionEvent: " + ae.toString());

        JMenuItem source = (JMenuItem)ae.getSource();
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
        }
    }

    @Override
    public void handleRun() {
        dbg.continueDebug();
    }

    @Override
    public void handleStop() {
        dbg.stopDebug();
    }

    public void clearConsole() {
        console.clear();
    }
}
