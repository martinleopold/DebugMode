package com.martinleopold.mode.debug;

import java.io.File;
import processing.app.*;
import processing.mode.java.JavaBuild;
import processing.mode.java.JavaMode;
import processing.mode.java.runner.Runner;

/**
 * Mode Template for extending Java mode in Processing IDE 2.0a5 or later.
 *
 */
public class DebugMode extends JavaMode {
    public DebugMode(Base base, File folder) {
        super(base, folder);
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

    @Override
    public Runner handleRun(Sketch sketch, RunnerListener listener) throws SketchException {
        JavaBuild build = new JavaBuild(sketch);

        System.out.println("building sketch");
        String appletClassName = build.build();

        if (appletClassName != null) {
            final Runner runtime = new DebugRunner(build, listener);
            runtime.launch(false);
            return runtime;

            /*
            // launch runner in new thread
            new Thread(new Runnable() {

                @Override
                public void run() {
                    runtime.launch(false);  // this blocks until finished
                }
            }).start();
            return runtime;
            */
        }
        return null;
    }




    /**
     * Create a new editor associated with this mode.
     */
    /*
    @Override
    public Editor createEditor(Base base, String path, EditorState state) {
        return null;
    }
    */

    /**
     * Returns the default extension for this editor setup.
     */
    /*
    @Override
    public String getDefaultExtension() {
        return null;
    }
    */

    /**
     * Returns a String[] array of proper extensions.
     */
    /*
    @Override
    public String[] getExtensions() {
        return null;
    }
    */

    /**
     * Get array of file/directory names that needn't be copied during "Save
     * As".
     */
    /*
    @Override
    public String[] getIgnorable() {
        return null;
    }
    */
}
