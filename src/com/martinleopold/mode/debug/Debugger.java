package com.martinleopold.mode.debug;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import processing.mode.java.JavaBuild;

/**
 * Main controller class for debugging mode.
 * Mainly works with DebugEditor as the corresponding "view".
 * Uses DebugRunner to launch a VM.
 *
 * @author mlg
 */
public class Debugger implements VMEventListener {

    DebugEditor editor; // editor window, acting as main view
    DebugRunner runtime; // the runtime, contains debuggee VM

    /**
     * Construct a Debugger object.
     * @param editor The Editor that will act as primary view
     */
    Debugger(DebugEditor editor) {
        this.editor = editor;
    }

    /**
     * Start a debugging session.
     * Builds the sketch and launches a VM to run it. VM starts suspended.
     * Should produce a VMStartEvent.
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
     * End debugging session.
     * Stops and disconnects VM. Should produce VMDisconnectEvent.
     */
    void stopDebug() {
        if (runtime != null) {
            System.out.println("closing runtime");
            runtime.close();
            runtime = null;
        }
    }

    /**
     * Resume paused debugging session.
     * Resumes VM.
     */
    void continueDebug() {
        if (isConnected()) {
            runtime.vm().resume();
        }
    }

    /**
     * Callback for VM events.
     * Will be called from another thread. (VMEventReader)
     *
     * @param es Incoming set of events from VM
     */
    @Override
    public void vmEvent(EventSet es) {
        for (Event e : es) {
            System.out.println("VM Event: " + e.toString());
        }
    }

    /**
     * Checks whether the debugger is connected to a debuggee VM.
     * i.e. a debugging session is running.
     * @return true if connected to debuggee VM
     */
    boolean isConnected() {
        return runtime != null && runtime.vm() != null;
    }
}
