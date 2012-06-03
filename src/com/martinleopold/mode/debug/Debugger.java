package com.martinleopold.mode.debug;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import processing.app.Sketch;

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
    ThreadReference lastBpThread; // thread the last breakpoint occured in
    String mainClassName; // name of the main class that's currently being debugged
    String srcPath; // path to the src folder of the current build

    // for debugging
    DebugBuild build = null;

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
                build = new DebugBuild(sketch);

                System.out.println("building sketch: " + sketch.getName());
                mainClassName = build.build(false);
                System.out.println("class: " + mainClassName);
                // folder with assembled/preprocessed src
                srcPath = build.getSrcFolder().getPath();
                System.out.println("build src: " + srcPath);
                // folder with compiled code (.class files)
                System.out.println("build bin: " + build.getBinFolder().getPath());

            if (mainClassName != null) {
                System.out.println("init debuggee runtime");
                runtime = new DebugRunner(build, editor);
                System.out.println("launching debuggee runtime");
                VirtualMachine vm = runtime.launch(); // non-blocking
                if (vm == null) {
                    System.out.println("error 37: launch failed");
                }

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
            build = null;
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

    void step(int stepDepth) {
        if (isConnected()) {
            StepRequest sr =
                    runtime.vm().eventRequestManager().createStepRequest(lastBpThread, StepRequest.STEP_LINE, stepDepth);
            sr.addCountFilter(1); // valid for one step only
            sr.enable();
            runtime.vm().resume();
        }
    }

    void stepOver() {
        step(StepRequest.STEP_OVER);
    }

    void stepInto() {
        step(StepRequest.STEP_INTO);
    }

    void stepOut() {
        step(StepRequest.STEP_OUT);
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
            System.out.println("*** VM Event: " + e.toString());

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
                Location setupLocation = rt.methodsByName("setup").get(0).location();
                BreakpointRequest setupBp = runtime.vm().eventRequestManager().createBreakpointRequest(setupLocation);
                setupBp.enable();

                System.out.println("setting breakpoint on draw()");
                Location drawLocation = rt.methodsByName("draw").get(0).location();
                BreakpointRequest drawBp = runtime.vm().eventRequestManager().createBreakpointRequest(drawLocation);
                drawBp.enable();

                runtime.vm().resume();
            } else if (e instanceof BreakpointEvent) {
                ThreadReference t = ((BreakpointEvent) e).thread();
                lastBpThread = t;
                System.out.println("stack trace:");
                printStackTrace(t);
                System.out.println("source location:");
                printLocation(t);
                System.out.println("local variables:");
                printLocalVariables(t);
                /*
                 * System.out.println("this:"); printThis(t);
                 *
                 */
            } else if (e instanceof StepEvent) {
                StepEvent se = (StepEvent) e;
                ThreadReference t = se.thread();
                printLocation(se.location());
                System.out.println("local variables:");
                printLocalVariables(t);

                // delete all steprequests so new ones can be placed (only one per thread)
                // todo: per thread
                EventRequestManager mgr = runtime.vm().eventRequestManager();
                mgr.deleteEventRequests(mgr.stepRequests());
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
     * Print call stack trace of a thread. Only works on suspended threads.
     *
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
     * Print local variables on a suspended thread. Takes the topmost stack
     * frame and lists all local variables and their values.
     *
     * @param t suspended thread
     */
    void printLocalVariables(ThreadReference t) {
        try {
            if (t.frameCount() == 0) {
                System.out.println("call stack empty");
            } else {
                StackFrame sf = t.frame(0);
                for (LocalVariable lv : sf.visibleVariables()) {
                    System.out.println(lv.typeName() + " " + lv.name() + " = " + sf.getValue(lv));
                }
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (AbsentInformationException ex) {
            System.out.println("local variable information not available");
        }
    }

    /**
     * todo: seems to kill VMEventReader thread
     *
     * @param t
     */
    void printThis(ThreadReference t) {
        try {
            if (t.frameCount() == 0) {
                System.out.println("call stack empty");
            } else {
                StackFrame sf = t.frame(0);
                ObjectReference thisObject = sf.thisObject();
                ReferenceType type = thisObject.referenceType();
                System.out.println("this type: " + type.name());
                for (Field f : type.allFields()) {
                    Value v = thisObject.getValue(f);
                    System.out.println(f.typeName() + " " + f.name() + " = " + v.toString());
                }
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void printLocation(Location l) {
        try {
            //System.out.println(l.sourceName() + ":" + l.lineNumber());
            System.out.println(getSourceLine(l.sourcePath(), l.lineNumber()));

        } catch (AbsentInformationException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Read a line from the given file in the builds src folder (1-based i.e.
     * first line is no. 1)
     *
     * @param filePath
     * @param lineNo
     * @return
     */
    String getSourceLine(String filePath, int lineNo) {
        if (lineNo == -1) {
            return "";
        }
        File f = new File(srcPath + File.separator + filePath);
        try {

            BufferedReader r = new BufferedReader(new FileReader(f));
            int i = 0;
            String line = "";
            while (i++ < lineNo) {
                line = r.readLine();
                //System.out.println("reading line: " + line);
            }
            r.close();
            return f.getName() + ":" + lineNo + " | " + line;
        } catch (FileNotFoundException ex) {
            //System.err.println(ex);
            return f.getName() + ":" + lineNo;
        } catch (IOException ex) {
            System.err.println(ex);
            return "";
        }

    }

    void printLocation(ThreadReference t) {
        try {
            if (t.frameCount() == 0) {
                System.out.println("call stack empty");
            } else {
                StackFrame sf = t.frame(0);
                printLocation(sf.location());
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Print debug info about a ReferenceType. Prints class name, source file
     * name, lists methods.
     *
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
    }
}
