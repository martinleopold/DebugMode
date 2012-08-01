Debugger for the PDE (Processing Development Environment)
---------------------------------------------------------

[www.processing.org](http://www.processing.org)

The goal of this project is to develop a working prototype of a tool for debugging Processing sketches. One of Processings main strengths is its suitability for learning and teaching programming and generative design. Having an easy to use debugging feature available should facilitate teaching and understanding program flow in general as well as fixing actual bugs.
Features include the ability to set breakpoints on specific lines and subsequent stepping through the code while examining variable values. Ideally breakpoints can be set by interacting with the main PDE window and are indicated by highlighting the appropriate lines. A similar indication needs to be used for the current statement when execution is halted.<br />

This is free software, and you are welcome to redistribute it under certain conditions. See [GPLv2](http://www.gnu.org/licenses/gpl-2.0.html) for details.<br />

Supported by [Google Summer of Code 2012](http://code.google.com/soc/)

---

Current Release Info:

PROTOTYPE 5.
Internal testing release.
Processing SVN rev. 9810

IMPROVEMENTS, BUG FIXES:
* fixed setting values in variable inspector (arrays, null)
* added support for core libraries (pdf, opengl, …)

PLANNED/UPCOMING FEATURES:
* variable inspector maintains node expansions and scroll position
* some toolbar icon changes (debug, toggle breakpoint, variable inspector)
* "advanced mode" for variable inspector (shows stack trace and all local variables and fields)
* handle processing types in some way (color, PImage, etc)
* keyboard shortcuts
* toggling breakpoints by clicking in the left-hand column
* breakpoints are saved with a sketch
* debug button doesn't highlight properly (seems like a processing issue?)

KNOWN ISSUES:
* can't set breakpoints on some lines (method definitions, class constructors, empty lines)
* deleting a line will not remove a breakpoint set on it
* stepping into print(), println() doesn't return properly and will eventually result in exceptions

INSTALLATION:
Put the dist/DebugMode folder into the modes folder of your sketchbook.
This version depends on a (slightly) hacked version of processing.app.Editor, which is contained in dist/pde.jar. Replace the pde.jar in your Processing installation to make it work.

Note: As of Processing SVN rev. 9943 the hacked version of processing.app.Editor is no longer needed.