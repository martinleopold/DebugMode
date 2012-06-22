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

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.request.BreakpointRequest;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class LineBreakpoint  {
    protected Debugger dbg;
    protected DebugEditor editor;
    LineID line;
    BreakpointRequest bpr;

    public LineBreakpoint(LineID line, Debugger dbg) {
        this.line = line;
        this.dbg = dbg;

        dbg.editor().addBreakpointedLine(line.lineIdx);
        if (dbg.isPaused()) { // in a paused debug session
            // immediately activate the breakpoint
            attach();
        }
    }

    public void attach() {
        // find line in java space
        LineID javaLine = dbg.lineMapping().get(line);
        if (javaLine == null) {
            System.out.println("Couldn't find line " + line + " in the java code");
            return;
        }
        try {
            List<Location> locations = dbg.mainClass().locationsOfLine(javaLine.lineIdx + 1);
            if (locations.isEmpty()) {
                System.out.println("no location found for line " + line + " -> " + javaLine);
                return;
            }
            // use first found location
            bpr = dbg.vm().eventRequestManager().createBreakpointRequest(locations.get(0));
            bpr.enable();
            System.out.println(line + " -> " + javaLine);
        } catch (AbsentInformationException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void detach() {
        if (bpr != null) {
            dbg.vm().eventRequestManager().deleteEventRequest(bpr);
            bpr = null;
        }
    }

    public void enable() {

    }

    public void disable() {

    }
}
