package com.martinleopold.mode.debug;

import com.sun.jdi.event.EventSet;

/**
 * Interface for VM callbacks.
 * @author mlg
 */
public interface VMEventListener {
    /**
     * Receive an event from the VM. Events are sent in batches.
     * See documentation of EventSet for more information.
     * @param es Set of events
     */
    void vmEvent(EventSet es);
}
