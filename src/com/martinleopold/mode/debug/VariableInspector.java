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

import com.sun.jdi.Value;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.netbeans.swing.outline.DefaultOutlineCellRenderer;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.OutlineModel;
import org.netbeans.swing.outline.RenderDataProvider;
import org.netbeans.swing.outline.RowModel;

/**
 * Variable Inspector window.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class VariableInspector extends javax.swing.JFrame {

    protected DefaultMutableTreeNode rootNode;
    protected DefaultTreeModel treeModel;
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

        initComponents();

        // setup Outline
        rootNode = new DefaultMutableTreeNode("root");
        treeModel = new DefaultTreeModel(rootNode); // model for the tree column
        OutlineModel model = DefaultOutlineModel.createOutlineModel(treeModel, new VariableRowModel(), true, "Name"); // model for all columns
        ExpansionHandler expansionHandler = new ExpansionHandler();
        model.getTreePathSupport().addTreeWillExpandListener(expansionHandler);
        model.getTreePathSupport().addTreeExpansionListener(expansionHandler);
        tree.setModel(model);
        tree.setRootVisible(false);
        tree.setRenderDataProvider(new OutlineRenderer());
        tree.setColumnHidingAllowed(false); // disable visible columns button (shows by default when right scroll bar is visible)

        // set custom renderer and editor for value column, since we are using a custom class for values (VariableNode)
        TableColumn valueColumn = tree.getColumnModel().getColumn(1);
        valueColumn.setCellRenderer(new ValueCellRenderer());
        valueColumn.setCellEditor(new ValueCellEditor());

        //System.out.println("renderer: " + tree.getDefaultRenderer(String.class).getClass());
        //System.out.println("editor: " + tree.getDefaultEditor(String.class).getClass());

        callStack = new ArrayList();
        locals = new ArrayList();
        thisFields = new ArrayList();
        declaredThisFields = new ArrayList();

        this.setTitle("Variable Inspector");

//        for (Entry<Object, Object> entry : UIManager.getDefaults().entrySet()) {
//            System.out.println(entry.getKey());
//        }
    }

    // model for a table row (excluding the tree column)
    protected class VariableRowModel implements RowModel {

        protected String[] columnNames = {"Value", "Type"};
        protected int[] editableTypes = {VariableNode.TYPE_BOOLEAN, VariableNode.TYPE_FLOAT, VariableNode.TYPE_INTEGER, VariableNode.TYPE_STRING, VariableNode.TYPE_FLOAT, VariableNode.TYPE_DOUBLE, VariableNode.TYPE_LONG, VariableNode.TYPE_SHORT, VariableNode.TYPE_CHAR};

        @Override
        public int getColumnCount() {
            if (p5mode) {
                return 1; // only show value in p5 mode
            } else {
                return 2;
            }
        }

        @Override
        public Object getValueFor(Object o, int i) {
            if (o instanceof VariableNode) {
                VariableNode var = (VariableNode) o;
                switch (i) {
                    case 0:
                        return var; // will be converted to an appropriate text by ValueCellRenderer
                    case 1:
                        return var.getTypeName();
                    default:
                        return "";
                }
            } else {
                return "";
            }
        }

        @Override
        public Class getColumnClass(int i) {
            if (i == 0) {
                return VariableNode.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(Object o, int i) {
            if (i == 0 && o instanceof VariableNode) {
                VariableNode var = (VariableNode) o;
                //System.out.println("type: " + var.getTypeName());
                for (int type : editableTypes) {
                    if (var.getType() == type) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void setValueFor(Object o, int i, Object o1) {
            VariableNode var = (VariableNode) o;
            String stringValue = (String) o1;

            Value value = null;
            try {
                switch (var.getType()) {
                    case VariableNode.TYPE_INTEGER:
                        value = dbg.vm().mirrorOf(Integer.parseInt(stringValue));
                        break;
                    case VariableNode.TYPE_BOOLEAN:
                        value = dbg.vm().mirrorOf(Boolean.parseBoolean(stringValue));
                        break;
                    case VariableNode.TYPE_FLOAT:
                        value = dbg.vm().mirrorOf(Float.parseFloat(stringValue));
                        break;
                    case VariableNode.TYPE_STRING:
                        value = dbg.vm().mirrorOf(stringValue);
                        break;
                    case VariableNode.TYPE_LONG:
                        value = dbg.vm().mirrorOf(Long.parseLong(stringValue));
                        break;
                    case VariableNode.TYPE_BYTE:
                        value = dbg.vm().mirrorOf(Byte.parseByte(stringValue));
                        break;
                    case VariableNode.TYPE_DOUBLE:
                        value = dbg.vm().mirrorOf(Double.parseDouble(stringValue));
                        break;
                    case VariableNode.TYPE_SHORT:
                        value = dbg.vm().mirrorOf(Short.parseShort(stringValue));
                        break;
                    case VariableNode.TYPE_CHAR:
                        // TODO: better char support
                        if (stringValue.length() > 0) {
                            value = dbg.vm().mirrorOf(stringValue.charAt(0));
                        }
                        break;
                }
            } catch (NumberFormatException ex) {
                Logger.getLogger(VariableRowModel.class.getName()).log(Level.INFO, "invalid value entered for {0}: {1}", new Object[]{var.getName(), stringValue});
            }
            if (value != null) {
                var.setValue(value);
                Logger.getLogger(VariableRowModel.class.getName()).log(Level.INFO, "new value set: {0}", var.getStringValue());
            }
        }

        @Override
        public String getColumnName(int i) {
            return columnNames[i];
        }
    }

    /**
     * Renderer for the tree portion of the outline component. icons, text
     * color, tooltips. TODO: better doc
     */
    protected class OutlineRenderer implements RenderDataProvider {

        protected Icon[][] icons;

        public OutlineRenderer() {
            // load icons
            icons = loadIcons("theme/var-icons.gif");
        }

        /**
         * Returns an ImageIcon, or null if the path was invalid.
         */
        protected ImageIcon[][] loadIcons(String fileName) {
            DebugMode mode = editor.mode();
            File file = mode.getContentFile(fileName);
            if (!file.exists()) {
                Logger.getLogger(OutlineRenderer.class.getName()).log(Level.SEVERE, "icon file not found: {0}", file.getAbsolutePath());
                return null;
            }
            Image allIcons = mode.loadImage(fileName);
            int cols = allIcons.getWidth(null) / ICON_SIZE;
            int rows = allIcons.getHeight(null) / ICON_SIZE;
            ImageIcon[][] iconImages = new ImageIcon[cols][rows];

            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    //Image image = createImage(ICON_SIZE, ICON_SIZE);
                    Image image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
                    Graphics g = image.getGraphics();
                    g.drawImage(allIcons, -i * ICON_SIZE, -j * ICON_SIZE, null);
                    iconImages[i][j] = new ImageIcon(image);
                }
            }
            return iconImages;
        }

        protected Icon getIcon(int type, int state) {
            if (type < 0 || type > icons.length - 1) {
                return null;
            }
            return icons[type][state];
        }

        protected VariableNode toVariableNode(Object o) {
            if (o instanceof VariableNode) {
                return (VariableNode) o;
            } else {
                return null;
            }
        }

        @Override
        public String getDisplayName(Object o) {
            return o.toString(); // VariableNode.toString() returns name; (for sorting)
//            VariableNode var = toVariableNode(o);
//            if (var != null) {
//                return var.getName();
//            } else {
//                return o.toString();
//            }
        }

        @Override
        public boolean isHtmlDisplayName(Object o) {
            return false;
        }

        @Override
        public Color getBackground(Object o) {
            return null;
        }

        @Override
        public Color getForeground(Object o) {
            if (tree.isEnabled()) {
                return null; // default
            } else {
                return Color.GRAY;
            }
        }

        @Override
        public String getTooltipText(Object o) {
            VariableNode var = toVariableNode(o);
            if (var != null) {
                return var.description();
            } else {
                return "";
            }
        }

        @Override
        public Icon getIcon(Object o) {
            VariableNode var = toVariableNode(o);
            if (var != null) {
                if (tree.isEnabled()) {
                    return getIcon(var.getType(), 0);
                } else {
                    return getIcon(var.getType(), 1);
                }
            } else {
                return null; // use standard icon //TODO: use a gray standard icon if tree is not enabled..
                //UIManager.getIcon(o);
            }
        }
    }

    // TODO: could probably extend the simpler javax.swing.table.DefaultTableCellRenderer here
    /**
     * Renderer for the value column. Uses an italic font for null values and
     * Object values ("instance of ..."). Uses a gray color when tree is not
     * enabled.
     */
    protected class ValueCellRenderer extends DefaultOutlineCellRenderer {

        public ValueCellRenderer() {
            super();
        }

        protected void setItalic(boolean on) {
            if (on) {
                setFont(new Font(getFont().getName(), Font.ITALIC, getFont().getSize()));
            } else {
                setFont(new Font(getFont().getName(), Font.PLAIN, getFont().getSize()));
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!tree.isEnabled()) {
                setForeground(Color.GRAY);
            } else {
                setForeground(Color.BLACK);
            }

            if (value instanceof VariableNode) {
                VariableNode var = (VariableNode) value;

                if (var.getValue() == null || var.getType() == VariableNode.TYPE_OBJECT) {
                    setItalic(true);
                } else {
                    setItalic(false);
                }
                value = var.getStringValue();
            }

            setValue(value);
            return c;
        }
    }

    /**
     * Editor for the value column. Will show an empty string when editing
     * String values that are null.
     */
    protected class ValueCellEditor extends DefaultCellEditor {

        public ValueCellEditor() {
            super(new JTextField());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (!(value instanceof VariableNode)) {
                return super.getTableCellEditorComponent(table, value, isSelected, row, column);
            }
            VariableNode var = (VariableNode) value;
            if (var.getType() == VariableNode.TYPE_STRING && var.getValue() == null) {
                return super.getTableCellEditorComponent(table, "", isSelected, row, column);
            } else {
                return super.getTableCellEditorComponent(table, var.getStringValue(), isSelected, row, column);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        tree = new org.netbeans.swing.outline.Outline();

        scrollPane.setViewportView(tree);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(scrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
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
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollPane;
    protected org.netbeans.swing.outline.Outline tree;
    // End of variables declaration//GEN-END:variables

    /**
     * Access the root node of the JTree.
     *
     * @return the root node
     */
    public DefaultMutableTreeNode getRootNode() {
        return rootNode;
    }

    // rebuild after this to avoid these ... dots
    public void unlock() {
        tree.setEnabled(true);
    }

    public void lock() {
        if (tree.getCellEditor() != null) {
            tree.getCellEditor().stopCellEditing(); // force quit open edit
            //tree.getCellEditor().cancelCellEditing(); // TODO: better not to use the edited value?
        }
        tree.setEnabled(false);
    }

    public void reset() {
        rootNode.removeAllChildren();
        // clear local data for good measure (in case someone rebuilds)
        callStack.clear();
        locals.clear();
        thisFields.clear();
        declaredThisFields.clear();
        expandedNodes.clear();
        // update
        treeModel.nodeStructureChanged(rootNode);
    }

    Set<TreePath> expandedNodes = new HashSet();
    TreePath expandedLast;

    protected class ExpansionHandler implements TreeWillExpandListener, TreeExpansionListener {

        @Override
        public void treeWillExpand(TreeExpansionEvent tee) throws ExpandVetoException {
            Object last = tee.getPath().getLastPathComponent();
            if (!(last instanceof VariableNode)) {
                return;
            }
            VariableNode var = (VariableNode) last;
            // load children
            if (!dbg.isPaused()) {
                throw new ExpandVetoException(tee, "Debugger busy");
            } else {
                var.removeAllChildren(); // TODO: should we only load it once?
                // TODO: don't filter in advanced mode
                //System.out.println("loading children for: " + var);
                // true means include inherited
                var.addChildren(filterNodes(dbg.getFields(var.getValue(), 0, true), new ThisFilter()));
            }
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent tee) throws ExpandVetoException {
            //throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void treeExpanded(TreeExpansionEvent tee) {
            //System.out.println("expanded: " + tee.getPath());
            //System.out.println("hash: " + tee.getPath().getLastPathComponent().hashCode());
            expandedNodes.add(tee.getPath());

//            TreePath newPath = tee.getPath();
//            if (expandedLast != null) {
//                // test each node of the path for equality
//                for (int i=0; i<expandedLast.getPathCount(); i++) {
//                    if (i<newPath.getPathCount()) {
//                        System.out.println(expandedLast.getPathComponent(i) + " =? " + newPath.getPathComponent(i) + ": " + expandedLast.getPathComponent(i).equals(newPath.getPathComponent(i)));
//                    }
//                }
//            }
//            expandedLast = newPath;
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent tee) {
            //System.out.println("collapsed: " + tee.getPath());
            expandedNodes.remove(tee.getPath());
        }
    }
    protected static final int ICON_SIZE = 16;
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
            // add all locals to root
            addAllNodes(rootNode, locals);

            // add non-inherited this fields
            addAllNodes(rootNode, filterNodes(declaredThisFields, new LocalHidesThisFilter(locals, LocalHidesThisFilter.MODE_PREFIX)));

            // add p5 builtins in a new folder
            DefaultMutableTreeNode builtins = new DefaultMutableTreeNode("Processing");
            addAllNodes(builtins, filterNodes(thisFields, new P5BuiltinsFilter()));
            rootNode.add(builtins);

            // notify tree (using model) changed a node and its children
            // http://stackoverflow.com/questions/2730851/how-to-update-jtree-elements
            // needs to be done before expanding the path!
            treeModel.nodeStructureChanged(rootNode);

            // handle node expansions
            for (TreePath path : expandedNodes) {
                System.out.println("re-expanding: " + synthesizePath(path));
                tree.expandPath(synthesizePath(path));
            }

            // TODO: this expansion causes problems when sorted and stepping
            //tree.expandPath(new TreePath(new Object[]{rootNode, builtins}));

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

        //System.out.println(tree.getCellRenderer(0, 0).getClass());
        //System.out.println(tree.getCellRenderer(0, 1).getClass());
    }

    protected TreePath synthesizePath(TreePath path) {
        if (path.getPathCount() == 0 || !rootNode.equals(path.getPathComponent(0)) ) {
            return null;
        }
        Object[] newPath = new Object[path.getPathCount()];
        newPath[0] = rootNode;
        TreeNode currentNode = rootNode;
        for (int i=0; i<path.getPathCount()-1; i++) {
            // get next node
            for (int j=0; j<currentNode.getChildCount(); j++) {
                TreeNode nextNode = currentNode.getChildAt(j);
                if (nextNode.equals(path.getPathComponent(i+1))) {
                    currentNode = nextNode;
                    newPath[i+1] = nextNode;
                }
            }
        }
        return new TreePath(newPath);
    }

    protected List<VariableNode> filterNodes(List<VariableNode> nodes, VariableNodeFilter filter) {
        List<VariableNode> filtered = new ArrayList();
        for (VariableNode node : nodes) {
            if (filter.accept(node)) {
                filtered.add(node);
            }
        }
        return filtered;
    }

    protected void addAllNodes(DefaultMutableTreeNode root, List<? extends MutableTreeNode> nodes) {
        for (MutableTreeNode node : nodes) {
            root.add(node);
        }
    }

    public interface VariableNodeFilter {

        public boolean accept(VariableNode var);
    }

    public class P5BuiltinsFilter implements VariableNodeFilter {

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

        @Override
        public boolean accept(VariableNode var) {
            return Arrays.asList(p5Builtins).contains(var.getName());
        }
    }

// filter implicit this reference
    public class ThisFilter implements VariableNodeFilter {

        @Override
        public boolean accept(VariableNode var) {
            return !var.getName().startsWith("this$");
        }
    }

    public class LocalHidesThisFilter implements VariableNodeFilter {

        public static final int MODE_HIDE = 0; // don't show hidden this fields
        public static final int MODE_PREFIX = 1; // prefix hidden this fields with "this."
        protected List<VariableNode> locals;
        protected int mode;

        public LocalHidesThisFilter(List<VariableNode> locals, int mode) {
            this.locals = locals;
            this.mode = mode;
        }

        @Override
        public boolean accept(VariableNode var) {
            // check if the same name appears in the list of locals i.e. the local hides the field
            for (VariableNode local : locals) {
                if (var.getName().equals(local.getName())) {
                    switch (mode) {
                        case MODE_PREFIX:
                            var.setName("this." + var.getName());
                            return true;
                        case MODE_HIDE:
                            return false;
                    }
                }
            }
            return true;
        }
    }
}
