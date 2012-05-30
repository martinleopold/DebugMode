/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martinleopold.mode.debug;

import com.sun.jdi.event.EventSet;

/**
 *
 * @author mlg
 */
public interface VMEventListener {
    void vmEvent(EventSet es);
}
