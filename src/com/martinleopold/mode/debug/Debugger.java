/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martinleopold.mode.debug;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import processing.mode.java.JavaBuild;

/**
 * Main Controller class for debugging mode
 * mainly works with DebugEditor as the corresponding "view"
 * uses DebugRunner to launch a VM
 *
 * @author mlg
 */
public class Debugger implements VMEventListener {

    DebugEditor editor; // editor window, acting as main view
    DebugRunner runtime; // the runtime, contains debuggee VM

    Debugger(DebugEditor view) {
        this.editor = view;
    }

    /**
     * start debugging session
     */
    void startDebug() {
        stopDebug(); // stop any running sessions

        try {
            JavaBuild build = new JavaBuild(editor.getSketch());

            System.out.println("building sketch");
            String appletClassName = build.build();

            if (appletClassName != null) {
                System.out.println("init debuggee runtime");
                runtime = new DebugRunner(build, editor);
                System.out.println("launching debuggee runtime");
                VirtualMachine vm = runtime.launch(); // non-blocking

                // start receiving vm events
                VMEventReader eventThread = new VMEventReader(vm.eventQueue(), this);
                eventThread.start();


                //return runtime;

                /*
                 * // launch runner in new thread new Thread(new Runnable() {
                 *
                 * @Override public void run() { runtime.launch(false); // this
                 * blocks until finished } }).start(); return runtime;
                 */
            }
        } catch (Exception e) {
            editor.statusError(e);
        }

        //return null;
    }

    /**
     * end debugging session
     */
    void stopDebug() {
        if (runtime != null) {
            System.out.println("closing runtime");
            runtime.close();
            runtime = null;
        }
    }

    /**
     * resume paused debugging session
     */
    void continueDebug() {
        if (runtime != null && runtime.vm() != null) {
            runtime.vm().resume();
        }
    }

    /**
     * callback for vm events will be called from another thread
     *
     * @param es incoming set of events from vm
     */
    @Override
    public void vmEvent(EventSet es) {
        for (Event e : es) {
            System.out.println("VM Event: " + e.toString());
        }
    }
}
