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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import processing.app.Base;
import processing.app.EditorState;
import processing.mode.java.JavaMode;

/**
 * Debug Mode for Processing. Built on top of JavaMode.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class DebugMode extends JavaMode {

    public static final boolean VERBOSE_LOGGING = true;

    // important inherited fields:
    // protected Base base;
    public DebugMode(Base base, File folder) {
        super(base, folder);
        // output version from manifest file
        Package p = DebugMode.class.getPackage();
        System.out.println(p.getImplementationTitle() + " (v" + p.getImplementationVersion() + ")");

        // set logging level
        Logger logger = Logger.getLogger("");
        //Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); // doesn't work on os x
        if (VERBOSE_LOGGING) {
            logger.setLevel(Level.ALL);
        } else {
            logger.setLevel(Level.WARNING);
        }
    }

    /**
     * Return the pretty/printable/menu name for this mode. This is separate
     * from the single word name of the folder that contains this mode. It could
     * even have spaces, though that might result in sheer madness or total
     * mayhem.
     */
    @Override
    public String getTitle() {
        return "Debug";
    }

    /**
     * Create a new editor associated with this mode.
     */
    @Override
    public processing.app.Editor createEditor(Base base, String path, EditorState state) {
        return new DebugEditor(base, path, state, this);
    }
    /**
     * Returns the default extension for this editor setup.
     */
    /*
     * @Override public String getDefaultExtension() { return null; }
     */
    /**
     * Returns a String[] array of proper extensions.
     */
    /*
     * @Override public String[] getExtensions() { return null; }
     */
    /**
     * Get array of file/directory names that needn't be copied during "Save
     * As".
     */
    /*
     * @Override public String[] getIgnorable() { return null; }
     */
}
