/*
 * Copyright (C) 2015 Martin Leopold <m@martinleopold.com>
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

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.BreakpointRequest;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Model/Controller of a line breakpoint. Can be set before or while debugging.
 * Adds a highlight using the debuggers view ({@link DebugEditor}).
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class LineBreakpoint implements ClassLoadListener {

    protected Debugger dbg; // the debugger
    protected LineID line, javaLine; // the line this breakpoint is set on in sketch space and java space.
    protected Location location; // the location of this line in the corresponding class/type. needed to set the breakpoint
    protected BreakpointRequest bpr; // the request on the VM's event request manager
    protected ReferenceType theClass; // the class containing this breakpoint, null when not yet loaded

    /**
     * Create a {@link LineBreakpoint}. If in a debug session, will try to
     * immediately set the breakpoint. If not in a debug session or the
     * corresponding class is not yet loaded the breakpoint will activate on
     * class load.
     *
     * @param line the line id to create the breakpoint on
     * @param dbg the {@link Debugger}
     */
    public LineBreakpoint(LineID line, Debugger dbg) {
        this.line = line;
        line.startTracking(dbg.editor().getTab(line.fileName()).getDocument());
        this.dbg = dbg;
        tryClass(dbg.getClass(className())); // the class might already be loaded
        set(); // activate the breakpoint (show highlight, attach if debugger is running)
    }

    /**
     * Create a {@link LineBreakpoint} on a line in the current tab.
     *
     * @param lineIdx the line index of the current tab to create the breakpoint
     * on
     * @param dbg the {@link Debugger}
     */
    // TODO: remove and replace by {@link #LineBreakpoint(LineID line, Debugger dbg)}
    public LineBreakpoint(int lineIdx, Debugger dbg) {
        this(dbg.editor().getLineIDInCurrentTab(lineIdx), dbg);
    }

    /**
     * Get the line id this breakpoint is on.
     *
     * @return the line id
     */
    public LineID lineID() {
        return line;
    }

    /**
     * Test if this breakpoint is on a certain line.
     *
     * @param testLine the line id to test
     * @return true if this breakpoint is on the given line
     */
    public boolean isOnLine(LineID testLine) {
        return line.equals(testLine);
    }

    /**
     * Attach this breakpoint to the VM. Creates and enables a
     * {@link BreakpointRequest}. VM needs to be paused.
     */
    protected void attach() {
        if (!dbg.isPaused()) {
            Logger.getLogger(LineBreakpoint.class.getName()).log(Level.WARNING, "can't attach breakpoint, debugger not paused");
            return;
        }

        if (theClass == null) {
            Logger.getLogger(LineBreakpoint.class.getName()).log(Level.WARNING, "can't attach breakpoint, class not loaded: {0}", className());
            return;
        }

        if (location == null ) {
            Logger.getLogger(LineBreakpoint.class.getName()).log(Level.WARNING, "no location found for line {0} -> {1}", new Object[]{line, javaLine});
        }

        bpr = dbg.vm().eventRequestManager().createBreakpointRequest(location);
        bpr.enable();
        Logger.getLogger(LineBreakpoint.class.getName()).log(Level.INFO, "attached breakpoint to {0} -> {1}", new Object[]{line, javaLine});
    }

    /**
     * Detach this breakpoint from the VM. Deletes the
     * {@link BreakpointRequest}.
     */
    protected void detach() {
        if (bpr != null) {
            dbg.vm().eventRequestManager().deleteEventRequest(bpr);
            bpr = null;
        }
    }

    /**
     * Set this breakpoint. Adds the line highlight. If Debugger is paused also
     * attaches the breakpoint by calling {@link #attach()}.
     */
    protected void set() {
        if (theClass == null) { // class not yet loaded, need to listen for class loads
            dbg.addClassLoadListener(this); 
        }
        dbg.editor().addBreakpointedLine(line);
        if (theClass != null && dbg.isPaused()) { // class is loaded
            attach();// immediately activate the breakpoint
        }
        if (dbg.editor().isInCurrentTab(line)) {
            dbg.editor().getSketch().setModified(true);
        }
    }

    /**
     * Remove this breakpoint. Clears the highlight and detaches the breakpoint
     * if the debugger is paused.
     */
    public void remove() {
        dbg.removeClassLoadListener(this);
        //System.out.println("removing " + line.lineIdx());
        dbg.editor().removeBreakpointedLine(line.lineIdx());
        if (dbg.isPaused()) {
            // immediately remove the breakpoint
            detach();
        }
        line.stopTracking();
        if (dbg.editor().isInCurrentTab(line)) {
            dbg.editor().getSketch().setModified(true);
        }
    }

//    public void enable() {
//    }
//
//    public void disable() {
//    }
    @Override
    public String toString() {
        return line.toString();
    }

    /**
     * Get the name of the class this breakpoint belongs to. Needed for fetching
     * the right location to create a breakpoint request.
     *
     * @return the class name
     */
    protected String className() {
        if (line.fileName().endsWith(".pde")) {
            // standard tab
            ReferenceType mainClass = dbg.getMainClass();
            if (mainClass == null) {
                return null;
            }
            return dbg.getMainClass().name();
        }

        if (line.fileName().endsWith(".java")) {
            // pure java tab
            return line.fileName().substring(0, line.fileName().lastIndexOf(".java"));
        }

        return null;
    }

    /**
     * Try to find this lines location in a reference type. This is primarily used to find the right nested class. e.g. MainClass$Nested1. location and theClass instance variables are set on sucess.
     * 
     * @param  theClass class to try
     * @return true if the right class/location was found, otherwise false
     */
    protected boolean tryClass(ReferenceType theClass) {
        if (theClass == null) return false;
        javaLine = dbg.sketchToJavaLine(line); // find line in java space
        if (javaLine == null) {
            Logger.getLogger(LineBreakpoint.class.getName()).log(Level.WARNING, "couldn't find line {0} in the java code", line);
        }
        try {
            List<Location> locations = theClass.locationsOfLine(javaLine.lineIdx() + 1);
            if (!locations.isEmpty()) {
                this.location = locations.get(0); // use first location found
                this.theClass = theClass;
                return true; 
            }
        } catch (AbsentInformationException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false; // location not found in class
    }

    /**
     * Event handler called when a class is loaded in the debugger. Causes the
     * breakpoint to be attached, if its class was loaded.
     *
     * @param theClass the class that was just loaded.
     */
    @Override
    public void classLoaded(ReferenceType theClass) {
        // check if our class is being loaded
        if ( theClass.name().startsWith(className()) && tryClass(theClass) ) { // this includes nested classes e.g. MySketch$NestedClass
            //dbg.removeClassLoadListener(this); // we found it, no need to check any more classes as they are loaded
            attach();
        }
    }
}
