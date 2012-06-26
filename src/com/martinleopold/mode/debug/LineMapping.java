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

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import processing.app.Sketch;
import processing.app.SketchCode;

/**
 * Utility class for mapping line numbers between "sketch space" (i.e. like in
 * the editor) and "java space" (i.e. after preprocessing).
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class LineMapping {

    static void addLineNumbers(SketchCode tab) {
        String[] lines = tab.getProgram().split("\n", -1); // -1 includes empty strings
        String newProgram = "";
        int i = 1;
        for (String line : lines) {
            if (i > 1) {
                newProgram += "\n";
            }
            newProgram += line + " // " + tab.getFileName() + ":" + i;
            i++;
        }
        tab.setProgram(newProgram);
    }

    static void addLineNumbers(Sketch sketch) {
        for (SketchCode tab : sketch.getCode()) {
            addLineNumbers(tab);
        }
    }

    static void removeLineNumbers(SketchCode tab) {
        String[] lines = tab.getProgram().split("\n", -1);
        String newProgram = "";
        int i = 1;
        for (String line : lines) {
            if (i > 1) {
                newProgram += "\n";
            }
            newProgram += line.substring(0, line.lastIndexOf(" // "));
            i++;
        }
        tab.setProgram(newProgram);
    }

    static void removeLineNumbers(Sketch sketch) {
        for (SketchCode tab : sketch.getCode()) {
            removeLineNumbers(tab);
        }
    }

    static String sketchToString(Sketch sketch) {
        String output = "";
        for (SketchCode tab : sketch.getCode()) {
            output += tab.getProgram();
        }
        return output;
    }

    static Map<LineID, LineID> generateMapping(String srcFilePath) {
        // open the src
        // scan for comments //filename:number

        Map<LineID, LineID> map = new HashMap();
        File f = new File(srcFilePath);
        // enter srcfilee:currentlineno -> filename:number into mapping
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line;

            int i = 1; // current line number
            while ((line = r.readLine()) != null) {
                // find annotation in current line. //tabname:lineno
                int idx = line.lastIndexOf(" // "); // index of last comment
                if (idx > -1) {
                    line = line.substring(idx + 4); // only keep comment
                    // check the pattern
                    if (line.matches(".*:[0-9]*")) {
                        String[] parts = line.split(":");
                        LineID sketchLine = LineID.create(parts[0], Integer.parseInt(parts[1]));
                        LineID javaLine = LineID.create(f.getName(), i);
                        // enter bi-directional mapping
                        map.put(sketchLine, javaLine);
                        map.put(javaLine, sketchLine);
                    }
                }
                i++;
            }

            r.close();
        } catch (FileNotFoundException ex) {
            System.err.println(ex);
            return null;
        } catch (IOException ex) {
            System.err.println(ex);
            return null;
        }
        return map;
    }
}
