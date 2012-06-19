package com.martinleopold.mode.debug;

import java.io.File;
import processing.app.Base;
import processing.app.EditorState;
import processing.mode.java.JavaMode;

/**
 * Debug Mode for Processing. Built on top of JavaMode.
 *
 * @author mlg
 */
public class DebugMode extends JavaMode {

    // important inherited fields:
    // protected Base base;
    public DebugMode(Base base, File folder) {
        super(base, folder);
        // output version from manifest file
        Package p = DebugMode.class.getPackage();
        System.out.println(p.getImplementationTitle() + " (v" + p.getImplementationVersion() + ")");
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
