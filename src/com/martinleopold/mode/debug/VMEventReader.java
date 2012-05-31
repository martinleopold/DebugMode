package com.martinleopold.mode.debug;

import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;

/**
 * Reader Thread for VM Events.
 * Constantly monitors a VMs EventQueue for new events and forwards them to an VMEventListener.
 * @author mlg
 */
public class VMEventReader extends Thread {
    EventQueue eventQueue;
    VMEventListener listener;

    /**
     * Construct a VMEventReader.
     * Needs to be kicked off with start() once constructed.
     * @param eventQueue The queue to read events from. Can be obtained from a VirtualMachine via eventQueue().
     * @param listener the listener to forward events to.
     */
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
