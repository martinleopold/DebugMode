/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martinleopold.mode.debug;

import java.io.File;
import processing.app.Sketch;
import processing.app.SketchException;
import processing.mode.java.JavaBuild;

/**
 *
 * @author mlg
 */
public class DebugBuild extends JavaBuild {

    public DebugBuild(Sketch sketch) {
        super(sketch);
    }

    /**
     * Preprocess and compile sketch.
     * Copied from processing.mode.java.JavaBuild, just changed compiler.
     * @param srcFolder
     * @param binFolder
     * @param sizeWarning
     * @return
     * @throws SketchException
     */
    @Override
    public String build(File srcFolder, File binFolder, boolean sizeWarning) throws SketchException {
        this.srcFolder = srcFolder;
        this.binFolder = binFolder;

//    Base.openFolder(srcFolder);
//    Base.openFolder(binFolder);

        // run the preprocessor
        String classNameFound = preprocess(srcFolder, sizeWarning);

        // compile the program. errors will happen as a RunnerException
        // that will bubble up to whomever called build().
//    Compiler compiler = new Compiler(this);
//    String bootClasses = System.getProperty("sun.boot.class.path");
//    if (compiler.compile(this, srcFolder, binFolder, primaryClassName, getClassPath(), bootClasses)) {
        if (Compiler.compile(this)) { // use compiler with debug info enabled (-g switch flicked)
            sketchClassName = classNameFound;
            return classNameFound;
        }
        return null;
    }
}
