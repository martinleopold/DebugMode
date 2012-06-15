package com.martinleopold.mode.debug;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;
import java.io.*;
import java.util.ArrayList;
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
 * @author mlg
 */
public class Debugger implements VMEventListener {

    protected DebugEditor editor; // editor window, acting as main view
    protected DebugRunner runtime; // the runtime, contains debuggee VM
    protected boolean started = false; // debuggee vm has started, VMStartEvent received, main class loaded
    //ThreadReference initialThread; // initial thread of debuggee vm
    protected ThreadReference lastThread; // thread the last breakpoint or step occured in
    protected String mainClassName; // name of the main class that's currently being debugged
    protected ReferenceType mainClass;
    protected String srcPath; // path to the src folder of the current build
    protected DebugBuild build; // todo: might not need to be global
    protected Map<LineID, LineID> lineMap; // maps source lines from "sketch-space" to "java-space" and vice-versa
    protected List<LineID> breakpoints = new ArrayList(); // list of breakpoints in "sketch-space"

    /**
     * Construct a Debugger object.
     *
     * @param editor The Editor that will act as primary view
     */
    public Debugger(DebugEditor editor) {
        this.editor = editor;
    }

    /**
     * Start a debugging session. Builds the sketch and launches a VM to run it.
     * VM starts suspended. Should produce a VMStartEvent.
     */
    public void startDebug() {
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
    public void stopDebug() {
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
    public void continueDebug() {
        editor.clearSelection();
        if (isConnected()) {
            runtime.vm().resume();
        } else {
            startDebug();
        }
    }

    /**
     * Step through source code lines.
     *
     * @param stepDepth the step depth ({@link StepRequest}{@code .STEP_OVER}, {@link StepRequest}{@code .STEP_INTO}
     * or {@link StepRequest}{@code .STEP_OUT})
     */
    protected void step(int stepDepth) {
        if (isConnected()) {
            StepRequest sr =
                    runtime.vm().eventRequestManager().createStepRequest(lastThread, StepRequest.STEP_LINE, stepDepth);
            sr.addCountFilter(1); // valid for one step only
            sr.enable();
            runtime.vm().resume();
        }
    }

    /**
     * Step over current statement.s
     */
    public void stepOver() {
        step(StepRequest.STEP_OVER);
    }

    /**
     * Step into current statement.
     */
    public void stepInto() {
        step(StepRequest.STEP_INTO);
    }

    /**
     * Step out of current function.
     */
    public void stepOut() {
        step(StepRequest.STEP_OUT);
    }

    /**
     * Print the current stack trace.
     */
    public void printStackTrace() {
        if (isConnected()) {
            printStackTrace(lastThread);
        }
    }

    /**
     * Print local variables. Outputs type, name and value of each variable.
     */
    public void printLocals() {
        if (isConnected()) {
            printLocalVariables(lastThread);
        }
    }

    /**
     * Print fields of current {@code this}-object. Outputs type, name and value
     * of each field.
     */
    public void printThis() {
        if (isConnected()) {
            printThis(lastThread);
        }
    }

    /**
     * Print a source code snippet of the current location.
     */
    public void printSource() {
        if (isConnected()) {
            printSourceLocation(lastThread);
        }
    }

    /**
     * Set a breakpoint on the current line.
     */
    public void setBreakpoint() {
        LineID line = getCurrentLineID();
        line.enableTracking(editor.currentDocument());
        line.setView(editor);
        breakpoints.add(line);
        //editor.setLineBgColor(line, new Color(255, 170, 170));
        System.out.println("setting breakpoint on line " + line);
        System.out.println("note: breakpoints on method declarations will not work, use first line of method instead");
        System.out.println("note: changes take effect after (re)starting the debug session");
    }

    /**
     * Remove a breakpoint from the current line (if set).
     */
    public void removeBreakpoint() {
        LineID line = getCurrentLineID();
        line.disableTracking();
        editor.clearLineBgColor(line);
        if (breakpoints.contains(line)) {
            breakpoints.remove(line);
            System.out.println("removed breakpoint " + line);
            System.out.println("note: changes take effect after (re)starting the debug session");
        } else {
            System.out.println("no breakpoint on line " + line);
        }
    }

    /**
     * Print a list of currently set breakpoints.
     */
    public void listBreakpoints() {
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
     * Retrieve line of sketch where the cursor currently resides.
     *
     * @return
     */
    protected LineID getCurrentLineID() {
        Sketch s = editor.getSketch();
        String tab = s.getCurrentCode().getFileName();
        int lineNo = editor.getTextArea().getCaretLine() + 1;

        // check if there already is a breakpoint on the current line
        LineID newID = new LineID(tab, lineNo);
        if (breakpoints.contains(newID)) {
            return breakpoints.get(breakpoints.indexOf(newID));
        } else {
            return newID;
        }
    }

    /**
     * Callback for VM events. Will be called from another thread.
     * ({@link VMEventReader})
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

                /*
                 * System.out.println("setting breakpoint on setup()"); Location
                 * setupLocation = rt.methodsByName("setup").get(0).location();
                 * BreakpointRequest setupBp =
                 * runtime.vm().eventRequestManager().createBreakpointRequest(setupLocation);
                 * setupBp.enable();
                 *
                 */

                /*
                 * System.out.println("setting breakpoint on draw()"); Location
                 * drawLocation = rt.methodsByName("draw").get(0).location();
                 * BreakpointRequest drawBp =
                 * runtime.vm().eventRequestManager().createBreakpointRequest(drawLocation);
                 * drawBp.enable();
                 *
                 */

                System.out.println("setting breakpoints:");
                if (breakpoints.isEmpty()) {
                    System.out.println("no breakpoints set");
                }
                for (LineID sketchLine : breakpoints) {
                    setBreakpoint(sketchLine);
                }
                started = true;
                runtime.vm().resume();
            } else if (e instanceof BreakpointEvent) {
                BreakpointEvent be = (BreakpointEvent) e;
                lastThread = be.thread(); // save this thread
                BreakpointRequest br = (BreakpointRequest) be.request();

                printSourceLocation(lastThread);
                selectSourceLocation(be.location());
                updateVariableInspector(lastThread);
            } else if (e instanceof StepEvent) {
                StepEvent se = (StepEvent) e;
                lastThread = se.thread();

                printSourceLocation(lastThread);
                selectSourceLocation(se.location());
                updateVariableInspector(lastThread);

                // delete the steprequest that triggered this step so new ones can be placed (only one per thread)
                EventRequestManager mgr = runtime.vm().eventRequestManager();
                mgr.deleteEventRequest(se.request());
            } else if (e instanceof VMDisconnectEvent) {
                started = false;
            }
        }
    }

    /**
     * Checks whether the debugger is connected to a debuggee VM. i.e. a
     * debugging session is running. VMStartEvent has been received.
     *
     * @return true if connected to debuggee VM
     */
    public boolean isConnected() {
        return started && runtime != null && runtime.vm() != null;
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
                StackFrame sf = t.frame(0);

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
                tree.expandPath(new TreePath(new Object[]{rootNode, localVarsNode}));
                tree.expandPath(new TreePath(new Object[]{rootNode, thisNode}));

                //tree.repaint();
                //vi.repaint();
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

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

    protected List<VariableNode> getThisFields(ThreadReference t, int depth) {
        //System.out.println("getting this");
        List<VariableNode> vars = new ArrayList();
        try {
            if (t.frameCount() > 0) {
                StackFrame sf = t.frame(0);
                ObjectReference thisObj = sf.thisObject();
                //System.out.println("type: " + thisObj.referenceType().name());
                for (Field f : thisObj.referenceType().visibleFields()) {
                    //System.out.println("recursively adding field: " + f.name());
                    vars.add(getFieldRecursive(f, thisObj, 0, depth));
                }
            }
        } catch (IncompatibleThreadStateException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
        return vars;
    }

    // recursively resolve a field using an object reference as environment
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
     * @return
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
                int lineNo = sketchLine.lineNo - 1; // 0-based line number
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
                editor.setSelection(editor.getLineStartOffset(lineNo), editor.getLineStopOffset(lineNo));
            }
        } catch (AbsentInformationException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Set a breakpoint on a sketch line.
     *
     * @param sketchLine specifies the line to set a breakpoint on
     */
    protected void setBreakpoint(LineID sketchLine) {
        // find line in java space
        LineID javaLine = lineMap.get(sketchLine);
        if (javaLine == null) {
            System.out.println("Couldn't find line " + sketchLine + " in the java code");
            return;
        }
        try {
            List<Location> locations = mainClass.locationsOfLine(javaLine.lineNo);
            if (locations.isEmpty()) {
                System.out.println("no location found for line " + sketchLine + " -> " + javaLine);
                return;
            }
            for (Location l : locations) {
                BreakpointRequest bpr = runtime.vm().eventRequestManager().createBreakpointRequest(l);
                bpr.enable();
                System.out.println(sketchLine + " -> " + javaLine);
            }
        } catch (AbsentInformationException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
