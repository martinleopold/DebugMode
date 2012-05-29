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
public class VMEventListener extends Thread {
    EventQueue eventQueue;

    public VMEventListener(EventQueue eventQueue) {
        this.eventQueue = eventQueue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                EventSet eventSet = eventQueue.remove();
                for (Event e : eventSet) {
                    System.out.println("VM Event: " + e.toString());
                }
            }
        } catch (Exception e) {
            System.out.println("listener quit: " + e.toString());
        }
    }
}
