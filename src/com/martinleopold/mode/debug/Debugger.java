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

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import processing.app.Sketch;

/**
 * Main controller class for debugging mode. Mainly works with DebugEditor as
 * the corresponding "view". Uses DebugRunner to launch a VM.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class Debugger implements VMEventListener {

    protected DebugEditor editor; // editor window, acting as main view
    protected DebugRunner runtime; // the runtime, contains debuggee VM
    protected boolean started = false; // debuggee vm has started, VMStartEvent received, main class loaded
    protected boolean paused = false; // currently paused at breakpoint or step
    //ThreadReference initialThread; // initial thread of debuggee vm
    protected ThreadReference currentThread; // thread the last breakpoint or step occured in
    protected String mainClassName; // name of the main class that's currently being debugged
    protected ReferenceType mainClass;
    protected String srcPath; // path to the src folder of the current build
    protected DebugBuild build; // todo: might not need to be global
    protected Map<LineID, LineID> lineMap; // maps source lines from "sketch-space" to "java-space" and vice-versa
    protected List<LineID> breakpoints = new ArrayList(); // list of breakpoints in "sketch-space"
    protected Map<LineID, BreakpointRequest> breakpointRequests = new HashMap(); // list of breakpoints in "sketch-space"
    //protected List<LineBreakpoint> breakpoints = new ArrayList();

    protected StepRequest requestedStep; // the step request we are currently in, or null if not in a step

    /**
     * Construct a Debugger object.
     *
     * @param editor The Editor that will act as primary view
     */
    public Debugger(DebugEditor editor) {
        this.editor = editor;
    }

    public VirtualMachine vm() {
        if (runtime != null) {
            return runtime.vm();
        } else {
            return null;
        }
    }

    public ReferenceType mainClass() {
        return mainClass;
    }

    public Map<LineID, LineID> lineMapping() {
        return lineMap;
    }

    public DebugEditor editor() {
        return editor;
    }

    /**
     * Start a debugging session. Builds the sketch and launches a VM to run it.
     * VM starts suspended. Should produce a VMStartEvent.
     */
    public synchronized void startDebug() {
        stopDebug(); // stop any running sessions

        // clear console
        editor.clearConsole();

        try {
            Sketch sketch = editor.getSketch();
            build = new DebugBuild(sketch);

            System.out.println("building sketch: " + sketch.getName());
            LineMapping.addLineNumbers(sketch); // annotate
            mainClassName = build.build(false);
            LineMapping.removeLineNumbers(sketch); // annotate

            System.out.println("class: " + mainClassName);
            // folder with assembled/preprocessed src
            srcPath = build.getSrcFolder().getPath();
            System.out.println("build src: " + srcPath);
            // folder with compiled code (.class files)
            System.out.println("build bin: " + build.getBinFolder().getPath());

            if (mainClassName != null) {
                // generate the source line mapping
                System.out.println("generating source line mapping (sketch <-> java)");
                lineMap = LineMapping.generateMapping(srcPath + File.separator + mainClassName + ".java");
                /*
                 * for (Entry<LineID, LineID> entry : lineMap.entrySet()) {
                 * System.out.println(entry.getKey() + " -> " +
                 * entry.getValue()); }
                 */

                System.out.println("launching debuggee runtime");
                runtime = new DebugRunner(build, editor);
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
    public synchronized void stopDebug() {
        if (runtime != null) {
            System.out.println("closing runtime");
            runtime.close();
            runtime = null;
            build = null;
            // need to clear highlight here because, VMDisconnectedEvent seems to be unreliable. TODO: likely synchronization problem
            //clearHighlight();
            editor.clearCurrentLine();
        }
        started = false;
    }

    /**
     * Resume paused debugging session. Resumes VM.
     */
    public synchronized void continueDebug() {
        //editor.clearSelection();
        //clearHighlight();
        editor.clearCurrentLine();
        if (!isStarted()) {
            startDebug();
        } else if (isPaused()) {
            runtime.vm().resume();
            paused = false;
        }
    }

    /**
     * Step through source code lines.
     *
     * @param stepDepth the step depth ({@link StepRequest}{@code .STEP_OVER},
     * {@link StepRequest}{@code .STEP_INTO} or
     * {@link StepRequest}{@code .STEP_OUT})
     */
    protected void step(int stepDepth) {
        if (isPaused()) {
            // use global to mark that there is a step request pending
            requestedStep =
                    runtime.vm().eventRequestManager().createStepRequest(currentThread, StepRequest.STEP_LINE, stepDepth);
            requestedStep.addCountFilter(1); // valid for one step only
            requestedStep.enable();
            paused = false;
            runtime.vm().resume();
        }
    }

    /**
     * Step over current statement.
     */
    public synchronized void stepOver() {
        step(StepRequest.STEP_OVER);
    }

    /**
     * Step into current statement.
     */
    public synchronized void stepInto() {
        step(StepRequest.STEP_INTO);
    }

    /**
     * Step out of current function.
     */
    public synchronized void stepOut() {
        step(StepRequest.STEP_OUT);
    }

    /**
     * Print the current stack trace.
     */
    public synchronized void printStackTrace() {
        if (isStarted()) {
            printStackTrace(currentThread);
        }
    }

    /**
     * Print local variables. Outputs type, name and value of each variable.
     */
    public synchronized void printLocals() {
        if (isStarted()) {
            printLocalVariables(currentThread);
        }
    }

    /**
     * Print fields of current {@code this}-object. Outputs type, name and value
     * of each field.
     */
    public synchronized void printThis() {
        if (isStarted()) {
            printThis(currentThread);
        }
    }

    /**
     * Print a source code snippet of the current location.
     */
    public synchronized void printSource() {
        if (isStarted()) {
            printSourceLocation(currentThread);
        }
    }

    /**
     * Set a breakpoint on the current line.
     */
    public synchronized void setBreakpoint() {
        // do nothing if we are kinda busy
        if (isStarted() && !isPaused()) {
            return;
        }
        LineID line = getCurrentLineID();
        editor.addBreakpointedLine(line.lineIdx);
        breakpoints.add(line); // add to bp list
        if (isPaused()) { // in a paused debug session
            // immediately activate the breakpoint
            setBreakpoint(line);
        }
        //editor.setLineBgColor(line, new Color(255, 170, 170));
        System.out.println("set breakpoint on line " + line);
        System.out.println("note: breakpoints on method declarations will not work, use first line of method instead");
        //System.out.println("note: changes take effect after (re)starting the debug session");
    }

    /**
     * Remove a breakpoint from the current line (if set).
     */
    public synchronized void removeBreakpoint() {
        // do nothing if we are kinda busy
        if (isStarted() && !isPaused()) {
            return;
        }
        LineID line = getCurrentLineID();
        editor.removeBreakpointedLine(line.lineIdx);
        if (breakpoints.contains(line)) {
            if (isPaused()) {
                // immediately remove the breakpoint
                removeBreakpoint(line);
            }
            breakpoints.remove(line);
            System.out.println("removed breakpoint " + line);
            //System.out.println("note: changes take effect after (re)starting the debug session");
        } else {
            System.out.println("no breakpoint on line " + line);
        }
    }

    /**
     * Toggle the breakpoint on the current line.
     */
    public synchronized void toggleBreakpoint() {
        LineID line = getCurrentLineID();
        if (!breakpoints.contains(line)) {
            setBreakpoint();
        } else {
            removeBreakpoint();
        }
    }

    /**
     * Print a list of currently set breakpoints.
     */
    public synchronized void listBreakpoints() {
        if (breakpoints.isEmpty()) {
            System.out.println("no breakpoints");
        } else {
            System.out.println("line breakpoints:");
            for (LineID line : breakpoints) {
                System.out.println(line);
            }
        }
    }

    /**
     * Retrieve line of sketch where the cursor currently resides. TODO: maybe
     * move to editor?
     *
     * @return the current {@link LineID}
     */
    protected LineID getCurrentLineID() {
        String tab = editor.getSketch().getCurrentCode().getFileName();
        int lineNo = editor.getTextArea().getCaretLine();
        return new LineID(tab, lineNo);
    }

    /**
     * Callback for VM events. Will be called from another thread.
     * ({@link VMEventReader})
     *
     * @param es Incoming set of events from VM
     */
    @Override
    public synchronized void vmEvent(EventSet es) {
        for (Event e : es) {
            System.out.println("*** VM Event: " + e.toString());
            if (e instanceof VMStartEvent) {
                //initialThread = ((VMStartEvent) e).thread();
                ThreadReference t = ((VMStartEvent) e).thread();
                //printStackTrace(t);

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
                System.out.println("requesting event on class load: " + mainClassName);
                ClassPrepareRequest mainClassPrepare = runtime.vm().eventRequestManager().createClassPrepareRequest();
                mainClassPrepare.addClassFilter(mainClassName);
                mainClassPrepare.enable();
                runtime.vm().resume();
            } else if (e instanceof ClassPrepareEvent) {
                ClassPrepareEvent ce = (ClassPrepareEvent) e;
                ReferenceType rt = ce.referenceType();
                //printType(rt);
                mainClass = rt;

                if (breakpoints.isEmpty()) {
                    System.out.println("using auto brekapoints (no manual breakpoints set)");
                    System.out.println("setting breakpoint on setup()");
                    Location setupLocation = rt.methodsByName("setup").get(0).location();
                    BreakpointRequest setupBp =
                            runtime.vm().eventRequestManager().createBreakpointRequest(setupLocation);
                    setupBp.enable();

                    System.out.println("setting breakpoint on draw()");
                    Location drawLocation = rt.methodsByName("draw").get(0).location();
                    BreakpointRequest drawBp =
                            runtime.vm().eventRequestManager().createBreakpointRequest(drawLocation);
                    drawBp.enable();
                } else {
                    System.out.println("setting breakpoints:");
                    for (LineID sketchLine : breakpoints) {
                        setBreakpoint(sketchLine);
                    }
                }
                started = true;
                paused = false;
                runtime.vm().resume();
            } else if (e instanceof BreakpointEvent) {
                BreakpointEvent be = (BreakpointEvent) e;
                currentThread = be.thread(); // save this thread
                BreakpointRequest br = (BreakpointRequest) be.request();

                printSourceLocation(currentThread);
                editor.setCurrentLine(locationToLineID(be.location()));
                updateVariableInspector(currentThread);

                // hit a breakpoint during a step, need to cancel the step.
                if (requestedStep != null) {
                    runtime.vm().eventRequestManager().deleteEventRequest(requestedStep);
                    requestedStep = null;
                }

                paused = true;
            } else if (e instanceof StepEvent) {
                StepEvent se = (StepEvent) e;
                currentThread = se.thread();

                printSourceLocation(currentThread);
                editor.setCurrentLine(locationToLineID(se.location()));
                updateVariableInspector(currentThread);

                // delete the steprequest that triggered this step so new ones can be placed (only one per thread)
                EventRequestManager mgr = runtime.vm().eventRequestManager();
                mgr.deleteEventRequest(se.request());
                requestedStep = null; // mark that there is no step request pending
                paused = true;
            } else if (e instanceof VMDisconnectEvent) {
                started = false;
                // clear line highlight
                editor.clearCurrentLine();
            } else if (e instanceof VMDeathEvent) {
                started = false;
            }
        }
    }

    /**
     * Check whether a debugging session is running. i.e. the debugger is
     * connected to a debuggee VM, VMStartEvent has been received and main class
     * is loaded.
     *
     * @return true if the debugger is started.
     */
    public synchronized boolean isStarted() {
        return started && runtime != null && runtime.vm() != null;
    }

    /**
     * Check whether the debugger is paused. i.e. it is currently suspended at a
     * breakpoint or step
     *
     * @return true if the debugger is paused, false otherwise or if not started
     * ({@link #isStarted()})
     */
    public synchronized boolean isPaused() {
        return isStarted() && paused && currentThread != null && currentThread.isSuspended();
    }

    /**
     * Print call stack trace of a thread. Only works on suspended threads.
     *
     * @param t suspended thread to print stack trace of
     */
    protected void printStackTrace(ThreadReference t) {
        if (!t.isSuspended()) {
            return;
        }
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
    protected void printLocalVariables(ThreadReference t) {
        if (!t.isSuspended()) {
            return;
        }
        try {
            if (t.frameCount() == 0) {
                System.out.println("call stack empty");
            } else {
                StackFrame sf = t.frame(0);
                List<LocalVariable> locals = sf.visibleVariables();
                if (locals.isEmpty()) {
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
     * Update variable inspector window. Displays local variables and this
     * fields.
     *
     * @param t suspended thread to retrieve locals and this
     */
    protected void updateVariableInspector(ThreadReference t) {
        if (!t.isSuspended()) {
            return;
        }
        VariableInspector vi = editor.variableInspector();
        JTree tree = vi.getTree();
        DefaultMutableTreeNode rootNode = vi.getRootNode();
        rootNode.removeAllChildren(); // clear tree

        try {
            if (t.frameCount() == 0) {
                System.out.println("call stack empty");
            } else {
                // stack trace
                DefaultMutableTreeNode callStackNode = new DefaultMutableTreeNode("Call stack");
                for (DefaultMutableTreeNode node : getStackTrace(t)) {
                    callStackNode.add(node);
                }
                rootNode.add(callStackNode);

                // local variables
                DefaultMutableTreeNode localVarsNode = new DefaultMutableTreeNode("Locals");
                for (VariableNode var : getLocals(t, 1)) {
                    localVarsNode.add(var);
                }
                rootNode.add(localVarsNode);

                // this fields
                DefaultMutableTreeNode thisNode = new DefaultMutableTreeNode("this");
                for (VariableNode var : getThisFields(t, 1)) {
                    thisNode.add(var);
                }
                rootNode.add(thisNode);

                // notify tree (using model)
                //http://stackoverflow.com/questions/2730851/how-to-update-jtree-elements
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.nodeStructureChanged(rootNode);
                // expand top level nodes
                // needs to happen after nodeStructureChanged
                tree.expandPath(new TreePath(new Object[]{rootNode, callStackNode}));
                tree.expandPath(new TreePath(new Object[]{rootNode, localVarsNode}));
                tree.expandPath(new TreePath(new Object[]{rootNode, thisNode}));

                //tree.repaint();
                //vi.repaint();
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Compile a list of current locals usable for insertion into a
     * {@link JTree}. Recursively resolves object references.
     *
     * @param t the suspended thread to get locals for
     * @param depth how deep to resolve nested object references
     * @return the list of current locals
     */
    protected List<VariableNode> getLocals(ThreadReference t, int depth) {
        //System.out.println("getting locals");
        List<VariableNode> vars = new ArrayList();
        try {
            if (t.frameCount() > 0) {
                StackFrame sf = t.frame(0);
                for (LocalVariable lv : sf.visibleVariables()) {
                    //System.out.println("local var: " + lv.name());
                    Value val = sf.getValue(lv);
                    VariableNode var = new VariableNode(lv.name(), lv.typeName(), val == null ? "null" : val);
                    if (val != null) {
                        try {
                            // is this local var an object?
                            if (val instanceof ObjectReference) {
                                ObjectReference env = (ObjectReference) val;
                                for (Field f : ((ReferenceType) lv.type()).visibleFields()) {
                                    var.addChild(getFieldRecursive(f, env, 1, depth));
                                }
                            }
                        } catch (ClassNotLoadedException ex) {
                            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    vars.add(var);
                }
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (AbsentInformationException ex) {
            System.out.println("local variable information not available");
        }
        return vars;
    }

    /**
     * Compile a list of fields in the current this object usable for insertion
     * into a {@link JTree}. Recursively resolves object references.
     *
     * @param t the suspended thread to get locals for
     * @param depth how deep to resolve nested object references
     * @return the list of fields in the current this object
     */
    protected List<VariableNode> getThisFields(ThreadReference t, int depth) {
        //System.out.println("getting this");
        List<VariableNode> vars = new ArrayList();
        try {
            if (t.frameCount() > 0) {
                StackFrame sf = t.frame(0);
                ObjectReference thisObj = sf.thisObject();
                if (thisObj != null) { // will be null in native or static methods
                    //System.out.println("type: " + thisObj.referenceType().name());
                    for (Field f : thisObj.referenceType().visibleFields()) {
                        if (f == null) {
                            System.out.println("field is null!");
                        }
                        //System.out.println("recursively adding field: " + f.name());
                        vars.add(getFieldRecursive(f, thisObj, 0, depth));
                    }
                }
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
        return vars;
    }

    /**
     * Recursively resolve a field for use in a {@link JTree}. Uses an object
     * reference as environment. Used by {@link #getLocals} and
     * {@link #getThisFields}.
     *
     * @param field the field to resolve
     * @param obj the object reference used as environment (must contain the
     * field to resolve)
     * @param depth the current depth (in the recursive call)
     * @param maxDepth the depth to stop recursion at
     * @return the resolved field
     */
    protected VariableNode getFieldRecursive(Field field, ObjectReference obj, int depth, int maxDepth) {
        // resolve the field to a value using the provided object reference
        Value val = obj.getValue(field);
        VariableNode var = new VariableNode(field.name(), field.typeName(), val == null ? "null" : val.toString());
        //System.out.println("field: " + var);

        if (val != null && depth < maxDepth) {
            // add all child fields (if field represents an object)
            if (val instanceof ObjectReference) {
                ObjectReference env = (ObjectReference) val;
                //add all children
                for (Field f : env.referenceType().visibleFields()) {
                    //System.out.print(String.format(String.format("%%0%dd", depth + 1), 0).replace("0", "  "));
                    var.addChild(getFieldRecursive(f, env, depth + 1, maxDepth));
                }
            }
        }
        return var;
    }

    /**
     * Get the current call stack trace usable for insertion into a
     * {@link JTree}.
     *
     * @param t the suspended thread to retrieve the call stack from
     * @return call stack as list of {@link DefaultMutableTreeNode}s
     */
    protected List<DefaultMutableTreeNode> getStackTrace(ThreadReference t) {
        List<DefaultMutableTreeNode> stack = new ArrayList();
        try {
            int i = 0;
            for (StackFrame f : t.frames()) {
                Location l = f.location();
                int lineNo = l.lineNumber();
                try {
                    // line number translation
                    LineID sketchLine = lineMap.get(new LineID(l.sourceName(), l.lineNumber()));
                    if (sketchLine != null) {
                        lineNo = sketchLine.lineIdx + 1;
                    }
                } catch (AbsentInformationException ex) {
                    Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
                }
                stack.add(new DefaultMutableTreeNode(l.declaringType().name() + "." + l.method().name() + ":" + lineNo));
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
        return stack;
    }

    /**
     * Print visible fields of current "this" object on a suspended thread.
     * Prints type, name and value.
     *
     * @param t suspended thread
     */
    protected void printThis(ThreadReference t) {
        if (!t.isSuspended()) {
            return;
        }
        try {
            if (t.frameCount() == 0) {
                System.out.println("call stack empty");
            } else {
                StackFrame sf = t.frame(0);
                ObjectReference thisObject = sf.thisObject();
                if (this != null) {
                    ReferenceType type = thisObject.referenceType();
                    System.out.println("fields in this (" + type.name() + "):");
                    for (Field f : type.visibleFields()) {
                        System.out.println(f.typeName() + " " + f.name() + " = " + thisObject.getValue(f));
                    }
                } else {
                    System.out.println("can't get this (in native or static method)");
                }
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Print source code snippet of current location in a suspended thread.
     *
     * @param t suspended thread
     */
    protected void printSourceLocation(ThreadReference t) {
        try {
            if (t.frameCount() == 0) {
                System.out.println("call stack empty");
            } else {
                Location l = t.frame(0).location(); // current stack frame location
                printSourceLocation(l);
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Print source code snippet.
     *
     * @param l {@link Location} object to print source code for
     */
    protected void printSourceLocation(Location l) {
        try {
            //System.out.println(l.sourceName() + ":" + l.lineNumber());
            System.out.println("in method " + l.method() + ":");
            System.out.println(getSourceLine(l.sourcePath(), l.lineNumber(), 2));

        } catch (AbsentInformationException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Read a line from the given file in the builds src folder. 1-based i.e.
     * first line has line no. 1
     *
     * @param filePath
     * @param lineNo
     * @return the requested source line
     */
    protected String getSourceLine(String filePath, int lineNo, int radius) {
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
            while (i <= lineNo + radius) {
                String line = r.readLine(); // line no. i
                if (line == null) {
                    break; // end of file
                }
                if (i >= lineNo - radius) {
                    if (i > lineNo - radius) {
                        output += "\n"; // add newlines before all lines but the first
                    }
                    output += f.getName() + ":" + i + (i == lineNo ? " =>  " : "     ") + line;
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
     * Print info about a ReferenceType. Prints class name, source file name,
     * lists methods.
     *
     * @param rt the reference type to print out
     */
    protected void printType(ReferenceType rt) {
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

    /**
     * Mark a line in the editor by selecting it. Switches to appropriate tab.
     *
     * @param l {@link Location} object that describes source location to
     * select.
     */
    protected void selectSourceLocation(Location l) {
        try {
            LineID sketchLine = lineMap.get(new LineID(l.sourceName(), l.lineNumber()));
            editor.clearSelection();
            if (sketchLine != null) {
                int lineIdx = sketchLine.lineIdx; // 0-based line number
                String tab = sketchLine.fileName;
                System.out.println("sketch line: " + sketchLine);

                // switch to tab
                Sketch s = editor.getSketch();
                for (int i = 0; i < s.getCodeCount(); i++) {
                    if (tab.equals(s.getCode(i).getFileName())) {
                        s.setCurrentCode(i);
                        break;
                    }
                }
                // select line
                editor.selectLine(lineIdx);
            }
        } catch (AbsentInformationException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected LineID locationToLineID(Location l) {
        try {
            return lineMap.get(new LineID(l.sourceName(), l.lineNumber() - 1));
        } catch (AbsentInformationException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * Set a breakpoint on a sketch line.
     *
     * @param sketchLine specifies the line to set the breakpoint on
     */
    protected void setBreakpoint(LineID sketchLine) {
        // find line in java space
        LineID javaLine = lineMap.get(sketchLine);
        if (javaLine == null) {
            System.out.println("Couldn't find line " + sketchLine + " in the java code");
            return;
        }
        try {
            List<Location> locations = mainClass.locationsOfLine(javaLine.lineIdx + 1);
            if (locations.isEmpty()) {
                System.out.println("no location found for line " + sketchLine + " -> " + javaLine);
                return;
            }
            // use first found location
            BreakpointRequest bpr = runtime.vm().eventRequestManager().createBreakpointRequest(locations.get(0));
            bpr.enable();
            breakpointRequests.put(sketchLine, bpr);
            System.out.println(sketchLine + " -> " + javaLine);
        } catch (AbsentInformationException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Remove a breakpoint from a sketch line. Deletes the breakpoint request
     * from the event request manager.
     *
     * @param sketchLine identifies the line to remove the breakpoint from
     */
    protected void removeBreakpoint(LineID sketchLine) {
        if (breakpointRequests.containsKey(sketchLine)) {
            BreakpointRequest bpr = breakpointRequests.get(sketchLine);
            runtime.vm().eventRequestManager().deleteEventRequest(bpr);
            breakpointRequests.remove(sketchLine);
        }
    }
}
