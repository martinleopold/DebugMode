package com.martinleopold.mode.debug;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import java.util.logging.Level;
import java.util.logging.Logger;
import processing.app.Sketch;
import processing.mode.java.JavaBuild;

/**
 * Main controller class for debugging mode. Mainly works with DebugEditor as
 * the corresponding "view". Uses DebugRunner to launch a VM.
 *
 * @author mlg
 */
public class Debugger implements VMEventListener {

    DebugEditor editor; // editor window, acting as main view
    DebugRunner runtime; // the runtime, contains debuggee VM
    boolean started = false; // debuggee vm has started, VMStartEvent received
    ThreadReference initialThread; // initial thread of debuggee vm
    String mainClassName;

    /**
     * Construct a Debugger object.
     *
     * @param editor The Editor that will act as primary view
     */
    Debugger(DebugEditor editor) {
        this.editor = editor;
    }

    /**
     * Start a debugging session. Builds the sketch and launches a VM to run it.
     * VM starts suspended. Should produce a VMStartEvent.
     */
    void startDebug() {
        stopDebug(); // stop any running sessions

        try {
            Sketch sketch = editor.getSketch();
            JavaBuild build = new JavaBuild(sketch);

            System.out.println("building sketch: " + sketch.getName());
            String appletClassName = build.build();
            System.out.println("class: " + appletClassName);
            // folder with assembled/preprocessed src
            System.out.println("build src: " + build.getSrcFolder().getPath());
            // folder with compiled code (.class files)
            System.out.println("build bin: " + build.getBinFolder().getPath());

            if (appletClassName != null) {
                mainClassName = appletClassName;
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


                // test setting a breakpoint on setup()
                //Location
                //vm.eventRequestManager().createBreakpointRequest(null);
            }
        } catch (Exception e) {
            editor.statusError(e);
        }

        //return null;
    }

    /**
     * End debugging session. Stops and disconnects VM. Should produce
     * VMDisconnectEvent.
     */
    void stopDebug() {
        if (runtime != null) {
            System.out.println("closing runtime");
            runtime.close();
            runtime = null;
        }
        started = false;
    }

    /**
     * Resume paused debugging session. Resumes VM.
     */
    void continueDebug() {
        if (isConnected()) {
            runtime.vm().resume();
        }
    }

    /**
     * Callback for VM events. Will be called from another thread.
     * (VMEventReader)
     *
     * @param es Incoming set of events from VM
     */
    @Override
    public void vmEvent(EventSet es) {
        for (Event e : es) {
            System.out.println("VM Event: " + e.toString());

            if (e instanceof VMStartEvent) {
                initialThread = ((VMStartEvent) e).thread();
                started = true;

                printStackTrace(initialThread);

                // ref.type of the thread.
                /*
                 * ReferenceType rt = initialThread.referenceType();
                 * System.out.println("ref.type: " + rt);
                 * System.out.println("name: " + rt.name()); try {
                 * System.out.println("sourceName: " + rt.sourceName()); } catch
                 * (AbsentInformationException ex) {
                 * System.out.println("sourceName: unknown"); }
                 *
                 * // get the threads run method for (Method m :
                 * rt.methodsByName("run")) { System.out.println(m.toString());
                 * }
                 */

                /*
                 * for (ReferenceType rt : runtime.vm().allClasses()) {
                 * System.out.println(rt); }
                 *
                 */

                /*
                 * List<ReferenceType> mainClasses =
                 * runtime.vm().classesByName(mainClassName); if
                 * (mainClasses.size() == 1) { ReferenceType mainClass =
                 * mainClasses.get(0); System.out.println("ref.type: " +
                 * mainClass.toString()); System.out.println("name: " +
                 * mainClass.name()); try { System.out.println("sourceName: " +
                 * mainClass.sourceName()); } catch (AbsentInformationException
                 * ex) { System.out.println("sourceName: unknown"); } for
                 * (Method m : mainClass.methods()) {
                 * System.out.println(m.toString()); } }
                 */

                ClassPrepareRequest mainClassPrepare = runtime.vm().eventRequestManager().createClassPrepareRequest();
                mainClassPrepare.addClassFilter(mainClassName);
                mainClassPrepare.enable();
                runtime.vm().resume();
            } else if (e instanceof ClassPrepareEvent) {
                ReferenceType rt = ((ClassPrepareEvent) e).referenceType();
                debugPrint(rt);

                System.out.println("setting breakpoint on setup()");
                // get setup() location
                Location setupLocation = rt.methodsByName("setup").get(0).location();
                BreakpointRequest setupBp = runtime.vm().eventRequestManager().createBreakpointRequest(setupLocation);
                setupBp.enable();
                runtime.vm().resume();
            } else if (e instanceof BreakpointEvent) {
                System.out.println("stack trace:");
                printStackTrace(((BreakpointEvent) e).thread());
            }
        }
    }

    /**
     * Checks whether the debugger is connected to a debuggee VM. i.e. a
     * debugging session is running. VMStartEvent has been received.
     *
     * @return true if connected to debuggee VM
     */
    boolean isConnected() {
        return started && runtime != null && runtime.vm() != null;
    }

    /**
     * Print call stack trace of a thread.
     * Only works on suspended threads.
     * @param t suspended thread to print stack trace of
     */
    void printStackTrace(ThreadReference t) {
        try {
            System.out.println("thread: " + t.name());
            int i = 0;
            for (StackFrame f : t.frames()) {
                Location l = f.location();
                System.out.println(i++ + ": " + f.toString() + " @ " + l);
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Print debug info about a ReferenceType.
     * Prints class name, source file name, lists methods.
     * @param rt the reference type to print out
     */
    private void debugPrint(ReferenceType rt) {
        System.out.println("ref.type: " + rt);
        System.out.println("name: " + rt.name());
        try {
            System.out.println("sourceName: " + rt.sourceName());
        } catch (AbsentInformationException ex) {
            System.out.println("sourceName: unknown");
        }
        System.out.println("methods:");
        for (Method m : rt.methods()) {
            System.out.println(m.toString());
        }
        // get the threads run method for (Method m :
    }
}
