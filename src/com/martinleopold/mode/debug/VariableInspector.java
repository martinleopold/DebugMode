/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package com.martinleopold.mode.debug;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;

/**
 * Variable Inspector window.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class VariableInspector extends javax.swing.JFrame implements TreeWillExpandListener {

    protected DefaultMutableTreeNode rootNode;
//    protected DefaultMutableTreeNode callStackNode;
//    protected DefaultMutableTreeNode localsNode;
//    protected DefaultMutableTreeNode thisNode;
//    protected DefaultMutableTreeNode nonInheritedThisNode;
    protected List<DefaultMutableTreeNode> callStack;
    protected List<VariableNode> locals;
    protected List<VariableNode> thisFields;
    protected List<VariableNode> declaredThisFields;
    protected DebugEditor editor;
    protected Debugger dbg;

    /**
     * Creates new form NewJFrame
     */
    public VariableInspector(DebugEditor editor) {
        this.editor = editor;
        this.dbg = editor.dbg();
        initComponents(); // creates the root node

        // setup jTree
        tree.setRootVisible(false);
        tree.addTreeWillExpandListener(this);
        TreeRenderer tcr = new TreeRenderer();
        tree.setCellRenderer(tcr);
        ToolTipManager.sharedInstance().registerComponent(tree);

//        callStackNode = new DefaultMutableTreeNode();
//        localsNode = new DefaultMutableTreeNode();
//        thisNode = new DefaultMutableTreeNode();
//        nonInheritedThisNode = new DefaultMutableTreeNode();

        callStack = new ArrayList();
        locals = new ArrayList();
        thisFields = new ArrayList();
        declaredThisFields = new ArrayList();

//        rootNode.add(callStackNode);
//        rootNode.add(localsNode);
//        rootNode.add(thisNode);

        this.setTitle("Variable Inspector");

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        rootNode = new DefaultMutableTreeNode();
        tree = new javax.swing.JTree(rootNode);

        jScrollPane1.setViewportView(tree);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String args[]) {
//        /*
//         * Set the Nimbus look and feel
//         */
//        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
//        /*
//         * If Nimbus (introduced in Java SE 6) is not available, stay with the
//         * default look and feel. For details see
//         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
//         */
//        try {
//            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
//        } catch (ClassNotFoundException ex) {
//            java.util.logging.Logger.getLogger(VariableInspector.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(VariableInspector.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(VariableInspector.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(VariableInspector.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /*
//         * Create and display the form
//         */
//        run(new VariableInspector());
//    }
    protected static void run(final VariableInspector vi) {
        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                vi.setVisible(true);
            }
        });
    }

    /**
     * Access the JTree.
     *
     * @return the {@link JTree} object
     */
    public JTree getTree() {
        return tree;
    }

    /**
     * Access the root node of the JTree.
     *
     * @return the root node
     */
    public DefaultMutableTreeNode getRootNode() {
        return rootNode;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTree tree;
    // End of variables declaration//GEN-END:variables

    @Override
    public void treeWillExpand(TreeExpansionEvent tee) throws ExpandVetoException {
        //System.out.println("tree expansion: " + tee.getPath());
        Object last = tee.getPath().getLastPathComponent();
        if (!(last instanceof VariableNode)) {
            return;
        }
        VariableNode var = (VariableNode) last;
        // load children
        if (!dbg.isPaused()) {
            throw new ExpandVetoException(tee, "Debugger busy");
        } else {
            //System.out.println("loading children for: " + var);
            var.addChildren(dbg.getFields(var.getValue(), 0, true));
        }
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent tee) throws ExpandVetoException {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * TODO: docs
     */
    protected class TreeRenderer extends DefaultTreeCellRenderer {

        protected Icon[] icons;

        public TreeRenderer() {
            // load icons
            icons = new Icon[]{
                loadIcon("theme/var-Object.gif"),
                loadIcon("theme/var-array.gif"),
                loadIcon("theme/var-int.gif"),
                loadIcon("theme/var-float.gif"),
                loadIcon("theme/var-boolean.gif"),
                loadIcon("theme/var-char.gif"),
                loadIcon("theme/var-String.gif"),
                loadIcon("theme/var-long.gif"),
                loadIcon("theme/var-double.gif")
            };
        }

        protected Icon getIcon(int type) {
            if (type < 0 || type > icons.length - 1) {
                return null;
            }
            return icons[type];
        }

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {

            super.getTreeCellRendererComponent(
                    tree, value, sel,
                    expanded, leaf, row,
                    hasFocus);

            if (value instanceof VariableNode) {
                VariableNode node = (VariableNode) value;
                String typeName = node.getTypeName();
                if (typeName != null) {
                    setToolTipText(typeName);
                }

                Icon icon = getIcon(node.getType());
                if (icon != null) {
                    setIcon(icon);
                }
            } else {
                setToolTipText("");
            }

            return this;
        }

        /**
         * Returns an ImageIcon, or null if the path was invalid.
         */
        protected ImageIcon loadIcon(String fileName) {
            DebugMode mode = editor.mode();
            File file = mode.getContentFile(fileName);
            if (!file.exists()) {
                Logger.getLogger(TreeRenderer.class.getName()).log(Level.SEVERE, "icon file not found: {0}", file.getAbsolutePath());
                return null;
            }
            //if (img == null) System.out.println("couldn't load image: " + fileName);
            return new ImageIcon(file.getAbsolutePath());
        }
    }
    protected boolean p5mode = true;

    public void setAdvancedMode() {
        p5mode = false;
    }

    public void setP5Mode() {
        p5mode = true;
    }

    public void toggleMode() {
        if (p5mode) {
            setAdvancedMode();
        } else {
            setP5Mode();
        }
    }

    public void updateCallStack(List<DefaultMutableTreeNode> nodes, String title) {
        callStack = nodes;
    }

    public void updateLocals(List<VariableNode> nodes, String title) {
        locals = nodes;
    }

    public void updateThisFields(List<VariableNode> nodes, String title) {
        thisFields = nodes;
    }

    public void updateDeclaredThisFields(List<VariableNode> nodes, String title) {
        declaredThisFields = nodes;
    }

    public void rebuild() {
        rootNode.removeAllChildren();
        if (p5mode) {
            // filter
            P5BuiltinsFilter filter = new P5BuiltinsFilter();

            // add all locals to root
            for (VariableNode var : locals) {
                rootNode.add(var);
            }

            // add non-inherited this fields
            for (VariableNode var : declaredThisFields) {
                rootNode.add(var);
            }

            // add p5 builtins
            for (VariableNode var : thisFields) {
                if (filter.accept(var)) {
                    rootNode.add(var);
                }
            }
            //System.out.println("shown fields: " + rootNode.getChildCount());
        } else {
            // TODO: implement advanced mode here
//            rootNode.add(callStackNode);
//            rootNode.add(localsNode);
//            rootNode.add(thisNode);
            // expand top level nodes
            // needs to happen after nodeStructureChanged
//            tree.expandPath(new TreePath(new Object[]{rootNode, callStackNode}));
//            tree.expandPath(new TreePath(new Object[]{rootNode, localsNode}));
//            tree.expandPath(new TreePath(new Object[]{rootNode, thisNode}));
        }

        // notify tree (using model)
        //http://stackoverflow.com/questions/2730851/how-to-update-jtree-elements
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.nodeStructureChanged(rootNode);
    }

    public class P5BuiltinsFilter {

        protected String[] p5Builtins = {
            "focused",
            "frameCount",
            "frameRate",
            "height",
            "online",
            "screen",
            "width",
            "mouseX",
            "mouseY",
            "pmouseX",
            "pmouseY",
            "key",
            "keyCode",
            "keyPressed"
        };

        public boolean accept(VariableNode var) {
            return Arrays.asList(p5Builtins).contains(var.name);
        }
    }
}
