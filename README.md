Debugger for the PDE (Processing Development Environment)
---------------------------------------------------------

Project Site: [http://debug.martinleopold.com](http://debug.martinleopold.com)<br />
Latest version/Repository: [https://github.com/martinleopold/DebugMode](https://github.com/martinleopold/DebugMode)<br />

The goal of this project is to develop a working prototype of a Debugger for the [Processing Development Environment](http://processing.org). It is implemented using the Mode framework of Processing 2.0. One of Processing's main strengths is its suitability for learning and teaching programming and generative design. Having an easy to use debugging feature available should facilitate teaching and understanding program flow in general as well as fixing actual bugs.<br />

This is free software, and you are welcome to redistribute it under certain conditions. See [GPLv2](http://www.gnu.org/licenses/gpl-2.0.html) for details.<br />

Supported by [Google Summer of Code 2012](http://code.google.com/soc/)

---

Current Release Info:

PROTOTYPE 7 (v0.7.2)<br />
Community release. Updated for Processing 2.0.

INSTALLATION:
* Requires Processing 2.0 ([http://www.processing.org/download](http://www.processing.org/download))
* If not already present, create a folder named "modes" inside your Sketchbook folder. (The location of the sketchbook folder is shown in Processing's Preferences dialog)
* Copy "dist/DebugMode" from the extracted .zip into the "modes" folder.
* Restart Processing.

TUTORIAL:
* Create a new sketch or open an example.
* Switch to "Debug" in the modes menu located at the top right.
* Add breakpoints to any line inside a function by putting the cursor in it and using the diamond shaped icon from the toolbar. Breakpoints show up as "<>" to the left of your code and the line will get a light grey background.
* Alternatively, you can double click inside the left-hand bar to toggle breakpoints.
* Now run your sketch. The execution should stop at the first breakpoint, highlighting its line in yellow and adding an orange arrow to the left.
* Now is a good time to open the "Inspector", click the icon with an "i" symbol. (Inspector opens...)
* The Inspector shows a snapshot of all available variables and their values at the location.
* (Hidden gem:) Double clicking any primitive value in the Inspector lets you edit it!
* To continue from a breakpoint you can either "Step" or "Continue" (more options in the "Debug" menu). Step will execute the next line and stop again, continue will just go on until another breakpoint is found, doing whatever is set to happen in your sketch.
* That's mainly it, you can list threads and local variables from the menu at any breakpoint and options for stepping-in and out (of functions) are there too.

IMPROVEMENTS/BUG FIXES:
* keyboard shortcuts (see Debug Menu)
* logfiles (located in Sketchbook/modes/debug/logs)
* debugger activity shown in status line (busy, halted)
* no longer focus inspector window when toggling it

KNOWN ISSUES:
* can't set breakpoints on some lines (method definitions, class constructors, empty lines)
* deleting a line will not remove a breakpoint set on it
* stepping into print(), println() doesn't return properly and will eventually result in exceptions

PLANNED/UPCOMING FEATURES:
* "advanced mode" for variable inspector (shows stack trace and all local variables and fields)
* handle processing types in some way (color, PImage, etc)
* properly highlight debug button (seems like a processing issue?)
