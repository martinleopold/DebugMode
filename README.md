Debugger for the PDE (Processing Development Environment)
---------------------------------------------------------

[https://github.com/martinleopold/DebugMode](https://github.com/martinleopold/DebugMode)<br />
[http://www.processing.org](http://www.processing.org)

The goal of this project is to develop a working prototype of a tool for debugging Processing sketches. One of Processings main strengths is its suitability for learning and teaching programming and generative design. Having an easy to use debugging feature available should facilitate teaching and understanding program flow in general as well as fixing actual bugs.
Features include the ability to set breakpoints on specific lines and subsequent stepping through the code while examining variable values. Ideally breakpoints can be set by interacting with the main PDE window and are indicated by highlighting the appropriate lines. A similar indication needs to be used for the current statement when execution is halted.<br />

This is free software, and you are welcome to redistribute it under certain conditions. See [GPLv2](http://www.gnu.org/licenses/gpl-2.0.html) for details.<br />

Supported by [Google Summer of Code 2012](http://code.google.com/soc/)

---

Current Release Info:

PROTOTYPE 6.
GSOC release
Processing SVN rev. 10007

INSTALLATION:
* Needs a current version of Processing built from SVN. Last tested with rev. 10007: [http://code.google.com/p/processing](http://code.google.com/p/processing)
* If not present, create a folder named "modes" inside your Sketchbook folder. The location of the sketchbook folder is shown in Processing's Preferences dialog.
* Copy the folder "dist/DebugMode" into the "modes" folder.
* Restart Processing.

TUTORIAL:
* Create a new sketch or open an example.
* Switch to "Debug" in the modes menu to the right.
* Add breakpoints at any line inside a function by putting the cursor in it and using the diamond shaped icon from the toolbar. Breakpoints show up as "<>" to the left of your code and the line will get a light grey background.
* Alternatively, you can double click inside the left-hand bar to toggle breakpoints.
* Now run your sketch. The execution should stop at the first breakpoint highlighting it's line in yellow and adding an orange arrow to the left.
* Here is a good time to open the "Inspector", click the icon with an "i" symbol. (Inspector opens …).
* The Inspector shows a snapshot of all available variables and their values at the current breakpoint.
* (Hidden gem:) double clicking any primitive value in the Inspector lets you edit it!
* To continue from a breakpoint you can either "step" or "continue" (more options in the menu). Step will execute the next line and stop again, continue will just go on until another breakpoint is found or whatever is set to happen in your sketch.
* That's mainly it, you can list threads and local variables from the menu at any breakpoint and options for stepping-in and out (of functions) are there too.

IMPROVEMENTS/BUG FIXES:
* lost dependency on core hack
* breakpoints are saved with the sketch
* node expansions are maintained in the variable inspector
* toggle breakpoints by double clicking in the left-hand breakpoint bar
* reworked toolbar icons
* updated javadocs
* fixed exceptions that occurred on variable inspector updates (EDT problems)

KNOWN ISSUES:
* can't set breakpoints on some lines (method definitions, class constructors, empty lines)
* deleting a line will not remove a breakpoint set on it
* stepping into print(), println() doesn't return properly and will eventually result in exceptions

PLANNED/UPCOMING FEATURES:
* "advanced mode" for variable inspector (shows stack trace and all local variables and fields)
* handle processing types in some way (color, PImage, etc)
* keyboard shortcuts
* properly highlight debug button (seems like a processing issue?)
* revised logging (log files)
