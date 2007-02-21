/*
 * PanelDecoder.java
 *
 * Created on December 11, 2006, 3:01 PM
 * 
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui;

import edu.cmu.sphinx.tools.gui.util.ModelBuilder;
import edu.cmu.sphinx.tools.gui.util.ConfigurableComponent;
import edu.cmu.sphinx.tools.gui.util.ConfigurableProperty;

import java.util.Iterator;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.awt.Component;
import java.awt.TextComponent;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultComboBoxModel;

/**
 * This is a Panel that will handle the GUI of one particular section/group
 *
 * @author Ariani  
 */
public class PanelDecoder extends javax.swing.JPanel {
        
    private PanelMediator _pm;
    
    private static final int COMBO_NEUTRAL = 1;
    
    /** Creates new form PanelDecoder */
    public PanelDecoder(GUIMediator gm, String name, Set groupset) {         
        initComponents(); // create the GUI components
           
        _pm = new PanelMediator(name,groupset,gm,this);
        
        // initGUIComponents should come after the PanelMediator creation/init
        initGUIComponents(); 
        
    }
    
    /** 
     * change the data set
     */
    public void setPanelClassSet(Set ccset){
        _pm.setGroupMap(ccset);
    }
    
    /* clear the Panel Detail components */
    private void setEnablePanelDetail(boolean status){        
        jTextNewVal.setEnabled(status);
        jButtonChange.setEnabled(status);
    }
    
    /* additional initialization operation for the GUI components */
    private void initGUIComponents(){
         
        // initialize and set up both jList components 
        // one for the class list, the other one for property list
        jListOuter.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jListInner.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jListOuter.setLayoutOrientation(JList.VERTICAL);
        jListInner.setLayoutOrientation(JList.VERTICAL);
        
        DefaultListModel outerlistModel= new DefaultListModel();
        for ( Iterator it = _pm.getGroupMap().keySet().iterator(); it.hasNext();){
            outerlistModel.addElement(it.next());
        }        
        jListOuter.setModel(outerlistModel);                  
        jListInner.setModel(new DefaultListModel());
      
        // set up combo box; then disable PanelDetail and right panel
        // top right panel will be enabled once there's something
        // chosen in the outer list        
        initComboBox();
        jComboName.setEnabled(false);
        setEnablePanelDetail(false);                    
    }
    
    /**
     * reset and re-initialize the combo box
     */
    private void initComboBox(){
        if ( jComboName.getItemCount() == 0 ){
            // set up the jcombobox items, set item at the info         
            String[] comboinit = {"<Create new set>","Choose configuration set"};
            jComboName.insertItemAt(comboinit[0],0);
            jComboName.insertItemAt(comboinit[1],1);
            jComboName.setSelectedIndex(COMBO_NEUTRAL);
        }
    }
    
    /**
     * clear all the property details from the inner panel
     */
    private void clearPanelDetail(){
        jTextPropName.setText("");
        jTextDefault.setText("");
        jTextDesc.setText("");
        jTextPropVal.setText("");
        jTextNewVal.setText("");
    }
    
    /**
     * clear all selection and any configuration info displayed on the GUI
     */
    private void clearAllDisplay(){
        clearRightPanel();
        jListOuter.clearSelection();
    }
    
    /**
     * clear all selection of the jlist, 
     * clear all information from the textboxes
     * but the jlist items and the combo box items are still maintained
     */
    private void resetInnerSplitPanel(){
        jListInner.clearSelection(); // set to select nothing
        clearPanelDetail();
        setEnablePanelDetail(false);
    }
    
    /**
     * clear all data from the right outer panel;
     * delete all list items and text box values; reset combo box items 
     */
    private void clearRightPanel(){
        
        Component[] carray = jRightPanel.getComponents();
        // the array will contain the upper top right panel components only
        for( int i = 0; i < carray.length; i++){
           if (carray[i] instanceof java.awt.TextComponent ){
               ((TextComponent)carray[i]).setText("");
           }  
           else if (carray[i] instanceof javax.swing.JComboBox){
               ((JComboBox)carray[i]).removeAllItems();               
           }
        }
        
        jListInner.clearSelection();
        // clean the jlist - not in the list of components of right panel
        ((DefaultListModel)jListInner.getModel()).clear();        
      
        clearPanelDetail();
        initComboBox();
        setEnablePanelDetail(false);
    }

    /**
     * private helper function to display the error to user
     */
    private void displayError(String message) {
        JOptionPane.showMessageDialog(this,message, 
                        _pm.getname(), JOptionPane.ERROR_MESSAGE);
    }
    
    /** 
     * re-confirm a user action
     */
   private boolean confirmAction(String message){ 
        int response;

        response = JOptionPane.showConfirmDialog(this, message, 
                "Confirm Action",JOptionPane.OK_CANCEL_OPTION);
        if ( response == JOptionPane.OK_OPTION){
            return true;
        }
        else
            return false;
   }

    /*
     * This function is used to check the name of the new set
     * it should only contains alphanumeric '_' or '-'
     */
    private boolean checkSetName(String s){
       
        final char[] chars = s.toCharArray();
        for (int x = 0; x < chars.length; x++) {      
            final char c = chars[x];
            if ((c >= 'a') && (c <= 'z')) continue; // lowercase
            if ((c >= 'A') && (c <= 'Z')) continue; // uppercase
            // numeric allowed for 2nd char onwards
            if ((x >= 1) && (c >= '0') && (c <= '9')) continue; 
            if ((x >= 1) && (c == '_')) continue; // underscore after 2nd char 
            if ((x >= 1) && (c == '-')) continue; // dash for 2nd char onwards
            return false;
        }  
        return true;
        
    }
    
    /** 
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jDialogSource = new javax.swing.JDialog();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTextAreaSource = new javax.swing.JTextArea();
        jSplitPaneOuter = new javax.swing.JSplitPane();
        jLeftPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListOuter = new javax.swing.JList();
        jButtonSouce = new javax.swing.JButton();
        jRightPanel = new javax.swing.JPanel();
        jLabelEmpty4 = new javax.swing.JLabel();
        jLabelClass = new javax.swing.JLabel();
        jTextClass = new javax.swing.JTextField();
        jLabelEmpty1 = new javax.swing.JLabel();
        jLabelName = new javax.swing.JLabel();
        jComboName = new javax.swing.JComboBox();
        jButtonDel = new javax.swing.JButton();
        jLabelEmpty2 = new javax.swing.JLabel();
        jSeparator = new javax.swing.JSeparator();
        jSplitPaneInner = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jListInner = new javax.swing.JList();
        jPanelDetail = new javax.swing.JPanel();
        jLabelEmpty6 = new javax.swing.JLabel();
        jLabelPropName = new javax.swing.JLabel();
        jTextPropName = new javax.swing.JTextField();
        jLabelDefault = new javax.swing.JLabel();
        jTextDefault = new javax.swing.JTextField();
        jLabelEmpty5 = new javax.swing.JLabel();
        jLabelDesc = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextDesc = new javax.swing.JTextArea();
        jLabelPropVal = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextPropVal = new javax.swing.JTextArea();
        jLabelNewVal = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextNewVal = new javax.swing.JTextArea();
        jLabelEmpty3 = new javax.swing.JLabel();
        jButtonChange = new javax.swing.JButton();

        jDialogSource.setTitle("Source Code");
        jDialogSource.setAlwaysOnTop(true);
        jDialogSource.setModal(true);
        jDialogSource.setName("dialogSource");
        jTextAreaSource.setMargin(new java.awt.Insets(20, 20, 20, 20));
        jScrollPane6.setViewportView(jTextAreaSource);

        jDialogSource.getContentPane().add(jScrollPane6, java.awt.BorderLayout.CENTER);

        setLayout(new java.awt.BorderLayout());

        jSplitPaneOuter.setDividerLocation(250);
        jLeftPanel.setLayout(new java.awt.BorderLayout());

        jListOuter.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jListOuterValueChanged(evt);
            }
        });

        jScrollPane1.setViewportView(jListOuter);

        jLeftPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jButtonSouce.setText("Show Source Code");
        jButtonSouce.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSouceActionPerformed(evt);
            }
        });

        jLeftPanel.add(jButtonSouce, java.awt.BorderLayout.SOUTH);

        jSplitPaneOuter.setLeftComponent(jLeftPanel);

        jRightPanel.setMaximumSize(new java.awt.Dimension(350, 32767));
        jLabelEmpty4.setPreferredSize(new java.awt.Dimension(400, 10));
        jRightPanel.add(jLabelEmpty4);

        jLabelClass.setFont(new java.awt.Font("Tahoma", 1, 12));
        jLabelClass.setText("      Class name :");
        jRightPanel.add(jLabelClass);

        jTextClass.setEditable(false);
        jTextClass.setPreferredSize(new java.awt.Dimension(200, 25));
        jRightPanel.add(jTextClass);

        jLabelEmpty1.setPreferredSize(new java.awt.Dimension(400, 10));
        jRightPanel.add(jLabelEmpty1);

        jLabelName.setFont(new java.awt.Font("Tahoma", 1, 12));
        jLabelName.setText("          Configuration list  :");
        jRightPanel.add(jLabelName);

        jComboName.setOpaque(false);
        jComboName.setPreferredSize(new java.awt.Dimension(175, 25));
        jComboName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RightPanelTopActionPerformed(evt);
            }
        });

        jRightPanel.add(jComboName);

        jButtonDel.setText("Delete");
        jButtonDel.setPreferredSize(new java.awt.Dimension(75, 25));
        jButtonDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RightPanelTopActionPerformed(evt);
            }
        });

        jRightPanel.add(jButtonDel);

        jLabelEmpty2.setPreferredSize(new java.awt.Dimension(400, 10));
        jRightPanel.add(jLabelEmpty2);

        jSeparator.setPreferredSize(new java.awt.Dimension(500, 20));
        jRightPanel.add(jSeparator);

        jSplitPaneInner.setDividerLocation(150);
        jSplitPaneInner.setMaximumSize(new java.awt.Dimension(350, 600));
        jSplitPaneInner.setPreferredSize(new java.awt.Dimension(450, 450));
        jListInner.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jListInnerValueChanged(evt);
            }
        });

        jScrollPane2.setViewportView(jListInner);

        jSplitPaneInner.setLeftComponent(jScrollPane2);

        jPanelDetail.setMaximumSize(new java.awt.Dimension(200, 600));
        jLabelEmpty6.setPreferredSize(new java.awt.Dimension(400, 10));
        jPanelDetail.add(jLabelEmpty6);

        jLabelPropName.setText("Property Name");
        jPanelDetail.add(jLabelPropName);

        jTextPropName.setEditable(false);
        jTextPropName.setPreferredSize(new java.awt.Dimension(150, 25));
        jPanelDetail.add(jTextPropName);

        jLabelDefault.setText("    Default value");
        jPanelDetail.add(jLabelDefault);

        jTextDefault.setEditable(false);
        jTextDefault.setFont(new java.awt.Font("Tahoma", 1, 11));
        jTextDefault.setPreferredSize(new java.awt.Dimension(150, 25));
        jPanelDetail.add(jTextDefault);

        jLabelEmpty5.setPreferredSize(new java.awt.Dimension(400, 10));
        jPanelDetail.add(jLabelEmpty5);

        jLabelDesc.setText("  Description");
        jPanelDetail.add(jLabelDesc);

        jTextDesc.setColumns(22);
        jTextDesc.setEditable(false);
        jTextDesc.setLineWrap(true);
        jTextDesc.setRows(3);
        jScrollPane3.setViewportView(jTextDesc);

        jPanelDetail.add(jScrollPane3);

        jLabelPropVal.setText("Current value");
        jPanelDetail.add(jLabelPropVal);

        jTextPropVal.setColumns(30);
        jTextPropVal.setEditable(false);
        jTextPropVal.setFont(new java.awt.Font("Courier", 1, 13));
        jTextPropVal.setLineWrap(true);
        jTextPropVal.setRows(3);
        jScrollPane4.setViewportView(jTextPropVal);

        jPanelDetail.add(jScrollPane4);

        jLabelNewVal.setText("New value");
        jPanelDetail.add(jLabelNewVal);

        jTextNewVal.setColumns(22);
        jTextNewVal.setLineWrap(true);
        jTextNewVal.setRows(3);
        jScrollPane5.setViewportView(jTextNewVal);

        jPanelDetail.add(jScrollPane5);

        jLabelEmpty3.setPreferredSize(new java.awt.Dimension(150, 25));
        jPanelDetail.add(jLabelEmpty3);

        jButtonChange.setMnemonic(java.awt.event.KeyEvent.VK_C);
        jButtonChange.setText("Change");
        jButtonChange.setPreferredSize(new java.awt.Dimension(150, 25));
        jButtonChange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonChangeActionPerformed(evt);
            }
        });

        jPanelDetail.add(jButtonChange);

        jSplitPaneInner.setRightComponent(jPanelDetail);

        jRightPanel.add(jSplitPaneInner);

        jSplitPaneOuter.setRightComponent(jRightPanel);

        add(jSplitPaneOuter, java.awt.BorderLayout.CENTER);

    }// </editor-fold>//GEN-END:initComponents

    /**
     * private method that's invoked when the 'see source code' button is clicked 
     * It will request the code from PanelMediator
     * and display it in a messagebox
     */
    private void jButtonSouceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSouceActionPerformed
        if(jListOuter.getSelectedValue() != null){
            String classname = (String)jListOuter.getSelectedValue();
            String code = _pm.getSource(classname);
            jTextAreaSource.setText(code);
            jDialogSource.setSize(800,500);
            jDialogSource.setLocationRelativeTo(null);
            jDialogSource.setVisible(true);
        }
    }//GEN-LAST:event_jButtonSouceActionPerformed

    /* private method to handle 'Change' button action */
    private void jButtonChangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonChangeActionPerformed
        String newval = jTextNewVal.getText(); 
        if( newval != null ){
            newval = newval.trim();
            String  classname = (String)jListOuter.getSelectedValue();
            String prop = (String)jListInner.getSelectedValue();
            String setname = (String)jComboName.getSelectedItem();
            
            if ( !newval.equalsIgnoreCase("") && (classname != null) &&
                    ( setname != null) && (prop != null) ){
                try {
                    _pm.changePropertyValue(classname, setname, prop, newval);
                    //succesfully change the model, now update the current value
                    jTextPropVal.setText(_pm.getPropertyValue(classname,setname,prop));
                }catch(PanelMediatorException pme){
                    displayError("Internal Error : "+pme.getMessage());
                }
            }
        }
    }//GEN-LAST:event_jButtonChangeActionPerformed

    /* private method to handle a selection change of Inner list */
    private void jListInnerValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jListInnerValueChanged
        //update the property info
        String prop = (String)jListInner.getSelectedValue();
        if (prop != null){
            String  classname = (String)jListOuter.getSelectedValue();
            ConfigurableProperty cp = _pm.getProperty(classname, prop);
            if(cp != null){
                jTextPropName.setText(cp.getName());                
                jTextDefault.setText(cp.getDefault());
                jTextDesc.setText(cp.getDesc());
                jTextPropVal.setText(_pm.getPropertyValue(classname,
                        (String)jComboName.getSelectedItem(),prop));
                jTextNewVal.setText("");
            }
        }
    }//GEN-LAST:event_jListInnerValueChanged

    /* private method to handle action performed by the Top Right GUI items */
    private void RightPanelTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RightPanelTopActionPerformed
        Object source = evt.getSource();
        if ( source instanceof javax.swing.JButton ){ // delete button is clicked     
            // the first two items in combo box are not for delete
            if ( jComboName.getSelectedIndex()>1 && 
                    confirmAction("Confirm delete this item?")){
                try {
                    // delete the set from the model
                    _pm.deleteConfigurationSet((String)jListOuter.getSelectedValue(),
                            (String)jComboName.getSelectedItem());
                    jComboName.removeItemAt(jComboName.getSelectedIndex());
                    jComboName.setSelectedIndex(COMBO_NEUTRAL); // set it to the natural index
                }catch(PanelMediatorException pme){
                    displayError("Internal Error : "+ pme.getMessage());
                }
            }
       } else if (source instanceof javax.swing.JComboBox){  
            // if the combo box changes  
            resetInnerSplitPanel();
            
            if (jComboName.getSelectedIndex() > 1){                              
                setEnablePanelDetail(true);
            }     

            else if (jComboName.getSelectedIndex() == 0){ 
                //selected item is 'create new set'               
                String s = (String)JOptionPane.showInputDialog(this,
                         "Please enter the name of new config",
                                         "Create New Configuration Set",
                                         JOptionPane.PLAIN_MESSAGE);

                //If a string was returned, say so.               
                if ((s != null) && (s.length() > 0) && 
                        !s.trim().equalsIgnoreCase("") ) {
                    s = s.trim();  
                    if ( checkSetName(s) ){ // is it a valid name?
                        
                        // create a new set in the model
                        try {
                            _pm.createNewConfigurationSet(s,                         
                                (String)jListOuter.getSelectedValue()); 
                            
                            // update the combo box
                            jComboName.addItem(s.trim());
                            // select last item           
                            jComboName.setSelectedIndex(jComboName.getItemCount()-1); 
                            setEnablePanelDetail(true);
                        } catch (PanelMediatorException pme){
                            displayError("Internal error " + pme.getMessage());
                            jComboName.setSelectedIndex(COMBO_NEUTRAL);
                        }
                    } else { // invalid name
                        displayError("The new set name is not valid\n"+
                                "Set name must start with an alphabet "+
                                "and contain only A-Z; a-z; 0-9; -; _");
                        jComboName.setSelectedIndex(COMBO_NEUTRAL);
                    }                    
                }
            } 
            
       }
    }//GEN-LAST:event_RightPanelTopActionPerformed

    /* private method to handle a selection change of Inner list */
    private void jListOuterValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jListOuterValueChanged
        // when there is a change in the selected item of outer list,
        // clear the right panel 
        // disable the detail panel until there is something selected in the combo box
        // put the class name on label
        // load the configuration sets into combo box
        // load the list of properties for this class
        clearRightPanel();
        String classname = (String)jListOuter.getSelectedValue();
        if ( classname != null){
            jComboName.setEnabled(true);
            jTextClass.setText(classname.substring(classname.lastIndexOf('.')+1));
            Set config = _pm.getConfigurationSet((String)jListOuter.getSelectedValue());
            if ( config != null){
                for ( Iterator it = config.iterator();it.hasNext();){
                    jComboName.addItem((String)it.next());
                }
            }
            Set prop = _pm.getPropertySet((String)jListOuter.getSelectedValue());
            if ( prop != null ){
                DefaultListModel innerlistModel= (DefaultListModel)jListInner.getModel();
                for ( Iterator it = prop.iterator();it.hasNext();){
                    innerlistModel.addElement(it.next());
                }
            }
        }
        
    }//GEN-LAST:event_jListOuterValueChanged
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonChange;
    private javax.swing.JButton jButtonDel;
    private javax.swing.JButton jButtonSouce;
    private javax.swing.JComboBox jComboName;
    private javax.swing.JDialog jDialogSource;
    private javax.swing.JLabel jLabelClass;
    private javax.swing.JLabel jLabelDefault;
    private javax.swing.JLabel jLabelDesc;
    private javax.swing.JLabel jLabelEmpty1;
    private javax.swing.JLabel jLabelEmpty2;
    private javax.swing.JLabel jLabelEmpty3;
    private javax.swing.JLabel jLabelEmpty4;
    private javax.swing.JLabel jLabelEmpty5;
    private javax.swing.JLabel jLabelEmpty6;
    private javax.swing.JLabel jLabelName;
    private javax.swing.JLabel jLabelNewVal;
    private javax.swing.JLabel jLabelPropName;
    private javax.swing.JLabel jLabelPropVal;
    private javax.swing.JPanel jLeftPanel;
    private javax.swing.JList jListInner;
    private javax.swing.JList jListOuter;
    private javax.swing.JPanel jPanelDetail;
    private javax.swing.JPanel jRightPanel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSeparator jSeparator;
    private javax.swing.JSplitPane jSplitPaneInner;
    private javax.swing.JSplitPane jSplitPaneOuter;
    private javax.swing.JTextArea jTextAreaSource;
    private javax.swing.JTextField jTextClass;
    private javax.swing.JTextField jTextDefault;
    private javax.swing.JTextArea jTextDesc;
    private javax.swing.JTextArea jTextNewVal;
    private javax.swing.JTextField jTextPropName;
    private javax.swing.JTextArea jTextPropVal;
    // End of variables declaration//GEN-END:variables
    
    /**
     * This private class would handle the data management for this GUI Panel 
     *
     * @author Ariani
     */
    private class PanelMediator implements GUIFileActionListener {

        private Map _ccmap ; // ConfigurableComponent map based on component classname
        private String _sectionName ;
        private GUIMediator _gmediator;
        private PanelDecoder _panel;
        
        /** 
         * Creates a new instance of PanelMediator 
         */
        private PanelMediator(String name, Set grpset, GUIMediator gmediator,
            PanelDecoder panel) {
            _gmediator = gmediator;
            _panel = panel;
            _sectionName = name;            
            _ccmap = new HashMap();               
            setGroupMap(grpset);
      
            _gmediator.registerPanel(this);
        }
                      
        
        /**
         * from the set of classes belong to this group, create its own Map
         * also used for reset and reload of new data
         */
        private void setGroupMap(Set grpset){
            _ccmap.clear();
            
            //create a hashmap of group members
            for( Iterator it = grpset.iterator(); it.hasNext();){
                ConfigurableComponent cc = 
                        (ConfigurableComponent)it.next();
                _ccmap.put(cc.getName(),cc);
            }
        }
        
        /** This method is inherited from GUIFileActionListener
         * It is called once there is a new configuration file loaded
         * Model data will be updated automatically - only need to clear the GUI
         */
        public void update(ConfigProperties cp) {
            _panel.clearAllDisplay();
        }
        
        /** This method is inherited from GUIFileActionListener
         * It is called once the configuration file is about to be written
         * Nothing needs to be done for GUI
         */
        public void saveData(ConfigProperties cp) throws GUIOperationException {
            /* do nothing */
        }

        /**
         * This method is inherited from GUIFileActionListener
         * Method purpose is to clear all configuration data
         */
        public void clearAll() {
            _panel.clearAllDisplay();
            System.out.println("*** clear all ");
        }
        
        /**
         * This method is inherited from GUIFileActionListener
         * Method purpose is to update panels after model change
         */
        public void modelRefresh() {
            /* do nothing */
        }
        
        
        /**
         * @return the whole map that represents all information for the GUI Panel
         */
        private Map getGroupMap(){
            return _ccmap;
        }

        /**
         * @return Name of this section/group
         */
        private String getname(){
            return _sectionName;
        }

        /**
         * @return the <code>Set</code> collection of defined configuration sets
         * for the specified classname
         */
        private Set getConfigurationSet(String classname){
            if (_ccmap.containsKey(classname)){
                ConfigurableComponent cc = 
                        (ConfigurableComponent)_ccmap.get(classname);
                
                if ( cc.getConfigurationPropMap() != null && 
                        !cc.getConfigurationPropMap().isEmpty() )
                    return cc.getConfigurationPropMap().keySet();
                else 
                    return null;
            }
            else
                return null;
        }
        
        /** 
         * delete a particular configuration set from a specified classname
         * 
         * @param classname Class that the set belongs to
         * @param setname the Configuration set name
         * @throws PanelMediatorException
         */
        private void deleteConfigurationSet (String classname, String setname)
            throws PanelMediatorException
        {
            if( _ccmap.containsKey(classname) ){
                ConfigurableComponent cc = (ConfigurableComponent)_ccmap.get(classname);
                if(cc.containsConfigurationSet(setname)){
                    cc.deleteConfigurationProp(setname);
                }
                else 
                    throw new PanelMediatorException(
                            PanelMediatorException.INVALID_SETNAME,
                            "The configuration set name is invalid");
            }
            else
                throw new PanelMediatorException(PanelMediatorException.INVALID_CLASSNAME,
                        "The classname is invalid");
        }
        
        /**
         * create a new configuration set, with the complete set of properties
         * and set to default values
         *
         * @param name Name of new configuration set
         * @param name of class where the set belongs to
         * @throws PanelMediatorException
         */
        private void createNewConfigurationSet (String name, String classname)
            throws PanelMediatorException
        {
            //check if there is a duplicate in the model for this name
            //note that names are case-sensitive
            if ( _gmediator.getModelBuilder().checkDuplicateConfigurationSet(name) ){
                throw new PanelMediatorException(
                        PanelMediatorException.DUPLICATE_SET_NAME, 
                        "This name has been used for another configuration set\n" + 
                        "Please choose a different name");
            }
            
            //create the new configuration set
            if ( _ccmap.containsKey(classname) ){
                ConfigurableComponent cc = 
                        (ConfigurableComponent)_ccmap.get(classname);
                cc.createNewSet(name);                
            }
            else
                throw new PanelMediatorException(
                        PanelMediatorException.INVALID_CLASSNAME, "Invalid class name");
        }
        
        /**
         * @return Set of configrable properties owned by the specified class
         */
        private Set getPropertySet(String classname){
            if (_ccmap.containsKey(classname)){
                ConfigurableComponent cc = 
                        (ConfigurableComponent)_ccmap.get(classname);
                if ( cc.getPropertyMap() != null && !cc.getPropertyMap().isEmpty() )
                     return cc.getPropertyMap().keySet(); 
                else 
                    return null;
            }
            else return null;
        }
        
        /** 
         * @return <code>ConfigurableProperty</code> with information about this property
         */
        private ConfigurableProperty getProperty(String classname,String propname){
            if( _ccmap.containsKey(classname) ){
                ConfigurableComponent cc = 
                        (ConfigurableComponent)_ccmap.get(classname);
                if (cc.containsProperty(propname)){
                    return cc.getProperty(propname);
                }
                else
                    return null;
            }
            else
                return null;
        }
        
        /** 
         * @return property value from the specified classname, set, and property
         *         a <code>List</code> would be represented as a semi-colon separated 
         *          <code>String</code>
         */
        private String getPropertyValue(String classname,String setName,String propname){
            if( _ccmap.containsKey(classname) ){
                ConfigurableComponent cc = 
                        (ConfigurableComponent)_ccmap.get(classname);
                Object propval = cc.getConfigurationPropValue(setName,propname);
                if (propval != null){
                    if (propval instanceof String)
                        return (String)propval;
                    else { // instance of List
                        String retval = new String();
                        for( Iterator it = ((List)propval).iterator(); it.hasNext();) {
                            retval = retval.concat((String)it.next()+";\n");
                        }
                        return retval;                       
                    }  
                }
                else
                    return null;
            }
            else
                return null;
        }
        
        /** change the value of the property from specified class, and set
         * 
         * @throws PanelMediatorException
         */
        private void changePropertyValue(String classname,String setName,
                String propname, String propval) throws PanelMediatorException
        {
            System.out.println("enters change vlaue method ***** ");
            
            if( _ccmap.containsKey(classname) ){
                ConfigurableComponent cc = 
                        (ConfigurableComponent)_ccmap.get(classname);
                                
                if(cc.containsConfigurationSet(setName)  && cc.containsProperty(propname)){
                    if(propval.contains(";")){ // it is a list
                        List myList = new ArrayList();
                        String[] temp = propval.split(";");
                        for ( int i = 0; i<temp.length;i++){
                            if(temp[i] != null && !temp[i].trim().equalsIgnoreCase(""))
                                myList.add(temp[i].trim());
                        }
                        cc.changeConfigurationPropValue(setName,propname,myList);
                    }
                    else{
                        cc.changeConfigurationPropValue(setName,propname,propval);
                    }
                }
                else if (!cc.containsConfigurationSet(setName)){
                     throw new PanelMediatorException(
                         PanelMediatorException.INVALID_SETNAME,
                         "Invalid configuration set name");
                }
                else {
                    throw new PanelMediatorException(
                        PanelMediatorException.INVALID_PROPNAME, "Invalid property name");
                }
                    
                              
            } else {
                throw new PanelMediatorException(
                        PanelMediatorException.INVALID_CLASSNAME, "Invalid class name");
            }
         
        } // changePropertyValue method
        
        /** 
         * this method is used to get java source code of a particular class
         * The source code is generated by ModelBuilder
         *
         * @param classname Name of java class to search
         */
        private String getSource(String classname){
           return _gmediator.getModelBuilder().getSourceCode(classname);
        }


    } // end inner class

    private class PanelMediatorException extends java.lang.Exception {
        private final static int INVALID_CLASSNAME = 1;
        private final static int DUPLICATE_SET_NAME = 2;
        private final static int INVALID_SETNAME = 3;
        private final static int INVALID_PROPNAME = 3;
        
        private int _mode;
        
        /**
         * Creates a new instance of <code>PanelMediatorException</code> with detail message.
         */
        private PanelMediatorException(int mode, String msg) {
            super(msg);
            _mode = mode;
        }
    } // end inner exception class 
    
}// end class
