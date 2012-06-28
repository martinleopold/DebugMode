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
    protected Map<LineID, LineID> lineMap = new HashMap(); // maps source lines from "sketch-space" to "java-space" and vice-versa
    protected List<LineBreakpoint> breakpoints = new ArrayList();
    protected StepRequest requestedStep; // the step request we are currently in, or null if not in a step

    /**
     * Construct a Debugger object.
     *
     * @param editor The Editor that will act as primary view
     */
    public Debugger(DebugEditor editor) {
        this.editor = editor;
    }

    /**
     * Access the VM.
     *
     * @return the virtual machine object or null if not available.
     */
    public VirtualMachine vm() {
        if (runtime != null) {
            return runtime.vm();
        } else {
            return null;
        }
    }

    // TODO: fix this
    public ReferenceType mainClass() {
        return mainClass;
    }

    public Map<LineID, LineID> lineMapping() {
        return lineMap;
    }

    /**
     * Access the editor associated with this debugger.
     *
     * @return the editor object
     */
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

        // load edits into sketch obj, etc...
        editor.prepareRun();

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
        LineID line = editor.getCurrentLineID();
        breakpoints.add(new LineBreakpoint(line.lineIdx, this));
        System.out.println("set breakpoint on line " + line);
        System.out.println("note: breakpoints on method declarations will not work, use first line of method instead");
        //System.out.println("note: changes take effect after (re)starting the debug session");
    }

    /**
     * Remove a breakpoint from the current line (if set).
     */
    public synchronized void removeBreakpoint() {
        // do nothing if we are kinda busy
        if (isBusy()) {
            return;
        }

        LineBreakpoint bp = breakpointOnLine(editor.getCurrentLineID());
        if (bp != null) {
            bp.remove();
            breakpoints.remove(bp);
            System.out.println("removed breakpoint " + bp);
        }
    }

    /**
     * Remove all breakpoints.
     */
    public synchronized void clearBreakpoints() {
        //TODO: handle busy-ness correctly
        if (isBusy()) {
            System.out.println("busy");
            return;
        }

        for (LineBreakpoint bp : breakpoints) {
            bp.remove();
        }
        breakpoints.clear();
    }

    /**
     * Get the breakpoint on a certain line, if set.
     *
     * @param line the line to get the breakpoint from
     * @return the breakpoint, or null if no breakpoint is set on the specified
     * line.
     */
    protected LineBreakpoint breakpointOnLine(LineID line) {
        for (LineBreakpoint bp : breakpoints) {
            if (bp.isOnLine(line)) {
                return bp;
            }
        }
        return null;
    }

    /**
     * Toggle the breakpoint on the current line.
     */
    public synchronized void toggleBreakpoint() {
        LineID line = editor.getCurrentLineID();

        LineBreakpoint bp = breakpointOnLine(line);
        if (bp == null) {
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
            for (LineBreakpoint bp : breakpoints) {
                System.out.println(bp);
            }
        }
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

//                if (breakpoints.isEmpty()) {
//                    System.out.println("using auto brekapoints (no manual breakpoints set)");
//                    System.out.println("setting breakpoint on setup()");
//                    Location setupLocation = rt.methodsByName("setup").get(0).location();
//                    BreakpointRequest setupBp =
//                            runtime.vm().eventRequestManager().createBreakpointRequest(setupLocation);
//                    setupBp.enable();
//
//                    System.out.println("setting breakpoint on draw()");
//                    Location drawLocation = rt.methodsByName("draw").get(0).location();
//                    BreakpointRequest drawBp =
//                            runtime.vm().eventRequestManager().createBreakpointRequest(drawLocation);
//                    drawBp.enable();
//                } else {
//                    System.out.println("setting breakpoints:");
//                    for (LineBreakpoint bp : breakpoints) {
//                        bp.attach();
//                    }
//                }

                if (!breakpoints.isEmpty()) {
                    System.out.println("setting breakpoints:");
                    for (LineBreakpoint bp : breakpoints) {
                        bp.attach();
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

                // fix canvas update issue
                // TODO: is this a good solution?
                resumeOtherThreads(currentThread);

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

    public synchronized boolean isBusy() {
        return isStarted() && !isPaused();
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

    // TODO: doc
    protected void resumeOtherThreads(ThreadReference t) {
        if (!isStarted()) {
            return;
        }
        for (ThreadReference other : vm().allThreads()) {
            if (!other.equals(t) && other.isSuspended()) {
                other.resume();
            }
        }
    }

    // TODO: doc
    public synchronized void printThreads() {
        if (!isPaused()) {
            return;
        }
        System.out.println("threads:");
        for (ThreadReference t : vm().allThreads()) {
            printThread(t);
        }
    }

    protected void printThread(ThreadReference t) {
        System.out.println(t.name());
        System.out.println("   is suspended: " + t.isSuspended());
        System.out.println("   is at breakpoint: " + t.isAtBreakpoint());
        System.out.println("   status: " + threadStatusToString(t.status()));
    }

    protected String threadStatusToString(int status) {
        switch (status) {
            case ThreadReference.THREAD_STATUS_MONITOR:
                return "THREAD_STATUS_MONITOR";
            case ThreadReference.THREAD_STATUS_NOT_STARTED:
                return "THREAD_STATUS_NOT_STARTED";
            case ThreadReference.THREAD_STATUS_RUNNING:
                return "THREAD_STATUS_RUNNING";
            case ThreadReference.THREAD_STATUS_SLEEPING:
                return "THREAD_STATUS_SLEEPING";
            case ThreadReference.THREAD_STATUS_UNKNOWN:
                return "THREAD_STATUS_UNKNOWN";
            case ThreadReference.THREAD_STATUS_WAIT:
                return "THREAD_STATUS_WAIT";
            case ThreadReference.THREAD_STATUS_ZOMBIE:
                return "THREAD_STATUS_ZOMBIE";
            default:
                return "";
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
                DefaultMutableTreeNode localVarsNode = new DefaultMutableTreeNode("Locals at " + currentLocation(t));
                for (VariableNode var : getLocals(t, 0)) {
                    localVarsNode.add(var);
                }
                rootNode.add(localVarsNode);

                // this fields
                DefaultMutableTreeNode thisNode = new DefaultMutableTreeNode("Class " + thisName(t));
                for (VariableNode var : getThisFields(t, 0)) {
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

    // TODO: doc
    protected String thisName(ThreadReference t) {
        try {
            if (!t.isSuspended() || t.frameCount() == 0) {
                return "";
            }
            return t.frame(0).thisObject().referenceType().name();
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }

    // TODO: doc
    protected String currentLocation(ThreadReference t) {
        try {
            if (!t.isSuspended() || t.frameCount() == 0) {
                return "";
            }
            return locationToString(t.frame(0).location());
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }

    // TODO: doc
    // class.method:translated_line_noa
    protected String locationToString(Location l) {
        LineID line = locationToLineID(l);
        int lineNumber;
        if (line != null) {
            lineNumber = line.lineIdx() + 1;
        } else {
            lineNumber = l.lineNumber();
        }
        return l.declaringType().name() + "." + l.method().name() + ":" + lineNumber;
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
                    VariableNode var = new VariableNode(lv.name(), lv.typeName(), val);
                    var.addChildren(getFields(val, depth - 1));
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
        try {
            if (t.frameCount() > 0) {
                StackFrame sf = t.frame(0);
                ObjectReference thisObj = sf.thisObject();
                return getFields(thisObj, depth);
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList();
    }

//    /**
//     * Recursively resolve a field for use in a {@link JTree}. Uses an object
//     * reference as environment. Used by {@link #getLocals} and
//     * {@link #getThisFields}.
//     *
//     * @param field the field to resolve
//     * @param obj the object reference used as environment (must contain the
//     * field to resolve)
//     * @param depth the current depth (in the recursive call)
//     * @param maxDepth the depth to stop recursion at
//     * @return the resolved field
//     */
//    protected VariableNode getFieldRecursive(Field field, ObjectReference obj, int depth, int maxDepth) {
//        // resolve the field to a value using the provided object reference
//        Value val = obj.getValue(field);
//        VariableNode var = new VariableNode(field.name(), field.typeName(), val == null ? "null" : val.toString());
//        //System.out.println("field: " + var);
//
//        if (val != null && depth < maxDepth) {
//            // add all child fields (if field represents an object)
//            if (val instanceof ObjectReference) {
//                ObjectReference env = (ObjectReference) val;
//                //add all children
//                for (Field f : env.referenceType().visibleFields()) {
//                    //System.out.print(String.format(String.format("%%0%dd", depth + 1), 0).replace("0", "  "));
//                    var.addChild(getFieldRecursive(f, env, depth + 1, maxDepth));
//                }
//            }
//        }
//        return var;
//    }
    // TODO: doc
    protected List<VariableNode> getFields(Value value, int depth, int maxDepth) {
        // remember: Value <- ObjectReference, ArrayReference
        List<VariableNode> fields = new ArrayList();
        if (value instanceof ObjectReference) {
            ObjectReference obj = (ObjectReference) value;
            // get the fields of this object
            for (Field field : obj.referenceType().visibleFields()) {
                Value val = obj.getValue(field); // get the value, may be null
                VariableNode var = new VariableNode(field.name(), field.typeName(), val);
                // recursively add children
                if (val != null && depth < maxDepth) {
                    var.addChildren(getFields(val, depth + 1, maxDepth));
                }
                fields.add(var);
            }
        }
        return fields;
    }

    // TODO: doc
    // maxDepth: 0 .. only the fields.
    protected List<VariableNode> getFields(Value value, int maxDepth) {
        return getFields(value, 0, maxDepth);
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
                stack.add(new DefaultMutableTreeNode(locationToString(f.location())));
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
            LineID sketchLine = lineMap.get(LineID.create(l.sourceName(), l.lineNumber()));
            editor.clearSelection();
            if (sketchLine != null) {
                int lineIdx = sketchLine.lineIdx(); // 0-based line number
                String tab = sketchLine.fileName();
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

    /**
     * Translate a java source location to a sketch line id.
     *
     * @param l the location to translate
     * @return the corresponding line id, or null if not found
     */
    protected LineID locationToLineID(Location l) {
        try {
            return lineMap.get(LineID.create(l.sourceName(), l.lineNumber() - 1));
        } catch (AbsentInformationException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
