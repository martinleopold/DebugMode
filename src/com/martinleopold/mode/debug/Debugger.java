package com.martinleopold.mode.debug;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;
import java.io.*;
import java.util.List;
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
    //ThreadReference initialThread; // initial thread of debuggee vm
    ThreadReference lastThread; // thread the last breakpoint or step occured in
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
                    runtime.vm().eventRequestManager().createStepRequest(lastThread, StepRequest.STEP_LINE, stepDepth);
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

    void printStackTrace() {
        if (isConnected()) {
            printStackTrace(lastThread);
        }
    }

    void printLocals() {
        if (isConnected()) {
            printLocalVariables(lastThread);
        }
    }

    void printThis() {
        if (isConnected()) {
            printThis(lastThread);
        }
    }

    void printSource() {
        if (isConnected()) {
            printLocation(lastThread);
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
            System.out.println("*** VM Event: " + e.toString());

            if (e instanceof VMStartEvent) {
                //initialThread = ((VMStartEvent) e).thread();
                ThreadReference t = ((VMStartEvent) e).thread();
                started = true;

                printStackTrace(t);

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

                // break on main class load
                ClassPrepareRequest mainClassPrepare = runtime.vm().eventRequestManager().createClassPrepareRequest();
                mainClassPrepare.addClassFilter(mainClassName);
                mainClassPrepare.enable();
                runtime.vm().resume();
            } else if (e instanceof ClassPrepareEvent) {
                ReferenceType rt = ((ClassPrepareEvent) e).referenceType();
                printType(rt);

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
                BreakpointEvent be = (BreakpointEvent)e;
                lastThread = be.thread(); // save this thread
                BreakpointRequest br = (BreakpointRequest)be.request();

                printLocation(lastThread);
            } else if (e instanceof StepEvent) {
                StepEvent se = (StepEvent)e;
                lastThread = se.thread();

                printLocation(lastThread);

                // delete the steprequest that triggered this step so new ones can be placed (only one per thread)
                EventRequestManager mgr = runtime.vm().eventRequestManager();
                mgr.deleteEventRequest(se.request());
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
            System.out.println("stack trace for thread " + t.name() + ":");
            int i = 0;
            for (StackFrame f : t.frames()) {
                Location l = f.location();
                System.out.println(i++ + ": " + f.toString());
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
                List<LocalVariable> locals = sf.visibleVariables();
                if (locals.size() == 0) {
                    System.out.println("no local variables");
                    return;
                }
                for (LocalVariable lv : locals) {
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
     *
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
                System.out.println("fields in this (" + type.name() + "):");
                for (Field f : type.visibleFields()) {
                    System.out.println(f.typeName() + " " + f.name() + " = " + thisObject.getValue(f));
                }
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void printLocation(ThreadReference t) {
        try {
            if (t.frameCount() == 0) {
                System.out.println("call stack empty");
            } else {
                Location l = t.frame(0).location(); // current stack frame location
                printLocation(l);
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void printLocation(Location l) {
        try {
            //System.out.println(l.sourceName() + ":" + l.lineNumber());
            System.out.println("in method " + l.method() + ":");
            System.out.println(getSourceLine(l.sourcePath(), l.lineNumber(), 2));

        } catch (AbsentInformationException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Read a line from the given file in the builds src folder.
     * 1-based i.e. first line has line no. 1
     *
     * @param filePath
     * @param lineNo
     * @return
     */
    String getSourceLine(String filePath, int lineNo, int radius) {
        if (lineNo == -1) {
            System.err.println("invalid line number: " + lineNo);
            return "";
        }
        //System.out.println("getting line: " + lineNo);
        File f = new File(srcPath + File.separator + filePath);
        String output = "";
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            int i = 1;
            //String line = "";
            while (i <= lineNo+radius) {
                String line = r.readLine(); // line no. i
                if (line == null) break; // end of file
                if (i >= lineNo-radius) {
                    if (i > lineNo-radius) output += "\n"; // add newlines before all lines but the first
                    output += f.getName() + ":" + i + (i==lineNo ? " =>  " : "     ") + line;
                }
                i++;
            }
            r.close();
            return output;
        } catch (FileNotFoundException ex) {
            //System.err.println(ex);
            return f.getName() + ":" + lineNo;
        } catch (IOException ex) {
            System.err.println(ex);
            return "";
        }

    }

    /**
     * Print info about a ReferenceType. Prints class name, source file
     * name, lists methods.
     *
     * @param rt the reference type to print out
     */
    private void printType(ReferenceType rt) {
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
