/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martinleopold.mode.debug;

import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;

/**
 *
 * @author mlg
 */
public class VMEventReader extends Thread {
    EventQueue eventQueue;
    VMEventListener listener;

    public VMEventReader(EventQueue eventQueue, VMEventListener listener) {
        this.eventQueue = eventQueue;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            while (true) {
                EventSet eventSet = eventQueue.remove();
                listener.vmEvent(eventSet);
                /*
                for (Event e : eventSet) {
                    System.out.println("VM Event: " + e.toString());
                }
                */
            }
        } catch (Exception e) {
            System.out.println("VMEventReader quit: " + e.toString());
        }
    }
}
