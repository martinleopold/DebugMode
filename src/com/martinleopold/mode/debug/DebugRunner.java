/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martinleopold.mode.debug;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import java.io.IOException;
import java.util.Map;
import processing.app.*;
import processing.core.PApplet;
import processing.mode.java.JavaBuild;

/**
 *
 * @author mlg
 */
public class DebugRunner extends processing.mode.java.runner.Runner {

    public DebugRunner(JavaBuild build, RunnerListener listener) throws SketchException {
        super(build, listener);
    }

    @Override
    // blocks until finished, but doesn't have to
    public void launch(boolean presenting) {
        //this.presenting = presenting;

        String[] machineParamList = getMachineParams();
        String[] sketchParamList = getSketchParams();

        System.out.println("launching VM");
        vm = launchVirtualMachine(machineParamList, sketchParamList);
        System.out.println("VM launched");
        if (vm != null) {
            // this redirects streams (out, err) and resumes vm
            //generateTrace(null);

            // create listener thread that outputs VM events
            System.out.println("starting event listener");
            VMEventListener eventThread = new VMEventListener(vm.eventQueue());
            eventThread.start();

            // start vm execution
            System.out.println("VM resume");
            vm.resume();
            // block till vm death (when event thread dies)
            try {
                eventThread.join();
            } catch (InterruptedException e) {
                // don't interrupt
            }
            System.out.println("VM done");
        }
    }

    @Override
    protected VirtualMachine launchVirtualMachine(String[] vmParams,
            String[] classParams) {
        //vm = launchTarget(sb.toString());
        LaunchingConnector connector = (LaunchingConnector) findConnector("com.sun.jdi.RawCommandLineLaunch");
        //PApplet.println(connector);  // gets the defaults

        //Map arguments = connectorArguments(connector, mainArgs);
        Map arguments = connector.defaultArguments();

        System.out.println("connector default args:");
        for (Object arg : arguments.keySet()) {
            System.out.println("   " + (String) arg);
        }

        Connector.Argument commandArg =
                (Connector.Argument) arguments.get("command");
        // Using localhost instead of 127.0.0.1 sometimes causes a
        // "Transport Error 202" error message when trying to run.
        // http://dev.processing.org/bugs/show_bug.cgi?id=895
        String addr = "127.0.0.1:" + (8000 + (int) (Math.random() * 1000));
        //String addr = "localhost:" + (8000 + (int) (Math.random() * 1000));
        //String addr = "" + (8000 + (int) (Math.random() * 1000));

        String commandArgs =
                "java -Xrunjdwp:transport=dt_socket,address=" + addr + ",suspend=y ";
        if (Base.isWindows()) {
            commandArgs =
                    "java -Xrunjdwp:transport=dt_shmem,address=" + addr + ",suspend=y ";
        } else if (Base.isMacOS()) {
            commandArgs =
                    "java -d" + Base.getNativeBits() + //Preferences.get("run.options.bits") +
                    " -Xrunjdwp:transport=dt_socket,address=" + addr + ",suspend=y ";
        }

        for (int i = 0; i < vmParams.length; i++) {
            commandArgs = addArgument(commandArgs, vmParams[i], ' ');
        }
        if (classParams != null) {
            for (int i = 0; i < classParams.length; i++) {
                commandArgs = addArgument(commandArgs, classParams[i], ' ');
            }
        }
        commandArg.setValue(commandArgs);

        Connector.Argument addressArg =
                (Connector.Argument) arguments.get("address");
        addressArg.setValue(addr);

        //PApplet.println(connector);  // prints the current
        //com.sun.tools.jdi.AbstractLauncher al;
        //com.sun.tools.jdi.RawCommandLineLauncher rcll;

        //System.out.println(PApplet.javaVersion);
        // http://java.sun.com/j2se/1.5.0/docs/guide/jpda/conninv.html#sunlaunch
        try {
            return connector.launch(arguments);
        } catch (IOException exc) {
            throw new Error("Unable to launch target VM: " + exc);
        } catch (IllegalConnectorArgumentsException exc) {
            throw new Error("Internal error: " + exc);
        } catch (VMStartException exc) {
            Process p = exc.process();
            //System.out.println(p);
            String[] errorStrings = PApplet.loadStrings(p.getErrorStream());
            /*
             * String[] inputStrings =
             */ PApplet.loadStrings(p.getInputStream());

            if (errorStrings != null && errorStrings.length > 1) {
                if (errorStrings[0].indexOf("Invalid maximum heap size") != -1) {
                    Base.showWarning("Way Too High",
                            "Please lower the value for \u201Cmaximum available memory\u201D in the\n"
                            + "Preferences window. For more information, read Help \u2192 Troubleshooting.",
                            exc);
                } else {
                    PApplet.println(errorStrings);
                    System.err.println("Using startup command:");
                    PApplet.println(arguments);
                }
            } else {
                exc.printStackTrace();
                System.err.println("Could not run the sketch (Target VM failed to initialize).");
                if (Preferences.getBoolean("run.options.memory")) {
                    // Only mention this if they've even altered the memory setup
                    System.err.println("Make sure that you haven't set the maximum available memory too high.");
                }
                System.err.println("For more information, read revisions.txt and Help \u2192 Troubleshooting.");
            }
            // changing this to separate editor and listener [091124]
            //if (editor != null) {
            listener.statusError("Could not run the sketch.");
            //}
            return null;
        }
    }

    private static boolean hasWhitespace(String string) {
        int length = string.length();
        for (int i = 0; i < length; i++) {
            if (Character.isWhitespace(string.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String addArgument(String string, String argument, char sep) {
        if (hasWhitespace(argument) || argument.indexOf(',') != -1) {
            // Quotes were stripped out for this argument, add 'em back.
            StringBuffer buffer = new StringBuffer(string);
            buffer.append('"');
            for (int i = 0; i < argument.length(); i++) {
                char c = argument.charAt(i);
                if (c == '"') {
                    buffer.append('\\');
                }
                buffer.append(c);
            }
            buffer.append('\"');
            buffer.append(sep);
            return buffer.toString();
        } else {
            return string + argument + String.valueOf(sep);
        }
    }
}
