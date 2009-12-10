/*
 * PanelConfigurable.java
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

import edu.cmu.sphinx.tools.gui.util.ConfigurableComponent;
import edu.cmu.sphinx.tools.gui.util.ConfigurableProperty;
import edu.cmu.sphinx.util.props.PropertyType;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;


/**
 * This is a Panel that will handle the GUI of one particular section/group
 *
 * @author Ariani
 */
public class PanelConfigurable extends javax.swing.JPanel {

    private final PanelMediator _pm;

    private static final int COMBO_NEUTRAL = 1;

    /** Creates new form PanelConfigurable */
    public PanelConfigurable(GUIMediator gm, String name, Set<ConfigurableComponent> groupset) {
        initComponents(); // create the GUI components

        _pm = new PanelMediator(name,groupset,gm,this);

        // initGUIComponents should come after the PanelMediator creation/init
        initGUIComponents();

    }

    /** there are two property modes for the 3 buttons :
     * list mode(status=true) : add and remove button
     * single mode(status=false) : change button
     * This method change "setVisible" properties of the above buttons
     */
    private void setVisibleListPropButton(boolean status){
        jButtonAdd.setVisible(status);
        jButtonRemove.setVisible(status);
        jButtonChange.setVisible(!status);
    }

    /** there are two property modes for the input :
     * component-type mode : use combo box for components
     * other native-type mode : use text area
     * This method change "setVisible" properties of them
     */
    private void setVisibleComponentInput(boolean status){
        jComboComponent.setVisible(status);
        jTextNewVal.setVisible(!status);
    }

    /**
     * change the data set
     */
    public void setPanelClassSet(Set<ConfigurableComponent> ccset){
        _pm.setGroupMap(ccset);
    }

    /* clear the Panel Detail components */
    private void setEnablePanelDetail(boolean status){

        jListInner.setEnabled(status);
        jTextNewVal.setEnabled(status);
        jComboComponent.setEnabled(status);
        jButtonChange.setEnabled(status);
        jButtonAdd.setEnabled(status);
        jButtonRemove.setEnabled(status);
        jButtonRefresh.setEnabled(status);

    }

    /* additional initialization operation for the GUI components */
    private void initGUIComponents(){

        // initialize and set up both jList components 
        // one for the class list, the other one for property list
        jListOuter.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jListInner.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jListPropVal.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jListOuter.setLayoutOrientation(JList.VERTICAL);
        jListInner.setLayoutOrientation(JList.VERTICAL);
        jListPropVal.setLayoutOrientation(JList.VERTICAL);


        DefaultListModel outerlistModel= new DefaultListModel();
        for (String s : _pm.getGroupMap().keySet()) {
            outerlistModel.addElement(s);
        }
        jListOuter.setModel(outerlistModel);
        jListInner.setModel(new DefaultListModel());
        // jListInner.setCellRenderer(new MyCellRenderer()); &&&&
        jListPropVal.setModel(new DefaultListModel());

        // set up combo box; then disable PanelDetail and right panel
        // top right panel will be enabled once there's something
        // chosen in the outer list (classname list)    

        initComboBox();
        jComboName.setEnabled(false);
        setEnablePanelDetail(false);
        jTextInnerList.setText("Property List : ");

        // this part will initialize the Panel Detail that displays info about one 
        // particular property of a class       
        setVisibleComponentInput(false);
        setVisibleListPropButton(true);

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
        jTextPropType.setText("");
        jTextClassType.setText("");
        jTextDefault.setText("");
        jTextDesc.setText("");
        ((DefaultListModel)jListPropVal.getModel()).clear();
        jTextNewVal.setText(null);
        jComboComponent.removeAllItems();
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
        for (Component component : carray) {
            if (component instanceof TextComponent) {
                ((TextComponent)component).setText("");
            } else if (component instanceof JComboBox) {
                ((JComboBox)component).removeAllItems();
            }
        }

        jListInner.clearSelection();
        // clean the jlist - not in the list of components of right panel
        ((DefaultListModel)jListInner.getModel()).clear();

        clearPanelDetail();
        initComboBox();
        setEnablePanelDetail(false);
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

    /* private method to check if there is a configuration set chosen */
    private boolean isConfigSetChosen(){
        return (jComboName.getSelectedIndex()>1);
    }

    /* private method to get the text input from user */
    private String getTextInput(){

        String newval;
        if (jTextNewVal.isVisible()){
            newval = jTextNewVal.getText();
        }
        else{ // jComboComponent is used instead
            newval = (String)jComboComponent.getSelectedItem();
            newval = newval.substring(0,(newval.indexOf('-')));
        }
        return newval;
    }

    /* private method to clear the property value list */
    private void clearPropValue(){
        ((DefaultListModel)jListPropVal.getModel()).clear();
    }

    /* private method to fill up the property value jlist with stored value from the model */
    private void initPropValue(Iterator<?> newValue){
        DefaultListModel model = (DefaultListModel)jListPropVal.getModel();
        model.clear();
        if ( newValue != null){
            for (;newValue.hasNext();){
                model.addElement(newValue.next());
            }
        }
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

        response = JOptionPane.showConfirmDialog(this, message,                "Confirm Action",JOptionPane.OK_CANCEL_OPTION);

        return response == JOptionPane.OK_OPTION;
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
        jButtonSource = new javax.swing.JButton();
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
        jLabelName1 = new javax.swing.JLabel();
        jSplitPaneInner = new javax.swing.JSplitPane();
        jInnerLeftPanel = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextInnerList = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        jListInner = new javax.swing.JList();
        jButtonRemoveProp = new javax.swing.JButton();
        jPanelDetail = new javax.swing.JPanel();
        jLabelPropName = new javax.swing.JLabel();
        jTextPropName = new javax.swing.JTextField();
        jLabelPropType = new javax.swing.JLabel();
        jTextPropType = new javax.swing.JTextField();
        jLabelClassType = new javax.swing.JLabel();
        jTextClassType = new javax.swing.JTextField();
        jLabelDefault = new javax.swing.JLabel();
        jTextDefault = new javax.swing.JTextField();
        jLabelEmpty5 = new javax.swing.JLabel();
        jLabelDesc = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextDesc = new javax.swing.JTextArea();
        jLabelEmpty6 = new javax.swing.JLabel();
        jLabelPropVal = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        jListPropVal = new javax.swing.JList();
        jLabelNewVal = new javax.swing.JLabel();
        jTextNewVal = new javax.swing.JTextField();
        jComboComponent = new javax.swing.JComboBox();
        jLabelEmpty7 = new javax.swing.JLabel();
        jButtonAdd = new javax.swing.JButton();
        jButtonRemove = new javax.swing.JButton();
        jButtonChange = new javax.swing.JButton();
        jButtonRefresh = new javax.swing.JButton();

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
            @Override
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jListOuterValueChanged(evt);
            }
        });

        jScrollPane1.setViewportView(jListOuter);

        jLeftPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jButtonSource.setText("Show Source Code");
        jButtonSource.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSourceActionPerformed(evt);
            }
        });

        jLeftPanel.add(jButtonSource, java.awt.BorderLayout.SOUTH);

        jSplitPaneOuter.setLeftComponent(jLeftPanel);

        jRightPanel.setMaximumSize(new java.awt.Dimension(650, 32767));
        jLabelEmpty4.setPreferredSize(new java.awt.Dimension(600, 10));
        jRightPanel.add(jLabelEmpty4);

        jLabelClass.setFont(new java.awt.Font("Tahoma", 1, 12));
        jLabelClass.setText("    Class name :");
        jRightPanel.add(jLabelClass);

        jTextClass.setEditable(false);
        jTextClass.setPreferredSize(new java.awt.Dimension(300, 25));
        jRightPanel.add(jTextClass);

        jLabelEmpty1.setPreferredSize(new java.awt.Dimension(600, 10));
        jRightPanel.add(jLabelEmpty1);

        jLabelName.setFont(new java.awt.Font("Tahoma", 1, 12));
        jLabelName.setText("    Configuration set  :");
        jRightPanel.add(jLabelName);

        jComboName.setOpaque(false);
        jComboName.setPreferredSize(new java.awt.Dimension(200, 25));
        jComboName.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RightPanelTopActionPerformed(evt);
            }
        });

        jRightPanel.add(jComboName);

        jButtonDel.setText("Delete");
        jButtonDel.setPreferredSize(new java.awt.Dimension(75, 25));
        jButtonDel.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RightPanelTopActionPerformed(evt);
            }
        });

        jRightPanel.add(jButtonDel);

        jLabelEmpty2.setPreferredSize(new java.awt.Dimension(400, 10));
        jRightPanel.add(jLabelEmpty2);

        jSeparator.setPreferredSize(new java.awt.Dimension(650, 20));
        jRightPanel.add(jSeparator);

        jLabelName1.setFont(new java.awt.Font("Tahoma", 1, 10));
        jLabelName1.setText("Please choose a configuration set before selecting any configurable property !");
        jRightPanel.add(jLabelName1);

        jSplitPaneInner.setDividerLocation(150);
        jSplitPaneInner.setMaximumSize(new java.awt.Dimension(350, 600));
        jSplitPaneInner.setPreferredSize(new java.awt.Dimension(600, 430));
        jInnerLeftPanel.setLayout(new java.awt.BorderLayout());

        jTextInnerList.setColumns(10);
        jTextInnerList.setEditable(false);
        jTextInnerList.setFont(new java.awt.Font("Courier", 1, 13));
        jTextInnerList.setLineWrap(true);
        jTextInnerList.setRows(1);
        jScrollPane4.setViewportView(jTextInnerList);

        jInnerLeftPanel.add(jScrollPane4, java.awt.BorderLayout.NORTH);

        jListInner.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jListInnerValueChanged(evt);
            }
        });

        jScrollPane2.setViewportView(jListInner);

        jInnerLeftPanel.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jButtonRemoveProp.setText("Remove from Set");
        jButtonRemoveProp.setToolTipText("Remove selected property from configuration set");
        jButtonRemoveProp.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRemovePropActionPerformed(evt);
            }
        });

        jInnerLeftPanel.add(jButtonRemoveProp, java.awt.BorderLayout.SOUTH);

        jSplitPaneInner.setLeftComponent(jInnerLeftPanel);

        jPanelDetail.setMaximumSize(new java.awt.Dimension(600, 600));
        jLabelPropName.setText("Property Name ");
        jPanelDetail.add(jLabelPropName);

        jTextPropName.setEditable(false);
        jTextPropName.setPreferredSize(new java.awt.Dimension(300, 25));
        jPanelDetail.add(jTextPropName);

        jLabelPropType.setText("Property Type  ");
        jPanelDetail.add(jLabelPropType);

        jTextPropType.setEditable(false);
        jTextPropType.setFont(new java.awt.Font("Tahoma", 1, 11));
        jTextPropType.setPreferredSize(new java.awt.Dimension(300, 25));
        jPanelDetail.add(jTextPropType);

        jLabelClassType.setText("      Class Type  ");
        jPanelDetail.add(jLabelClassType);

        jTextClassType.setEditable(false);
        jTextClassType.setFont(new java.awt.Font("Tahoma", 1, 11));
        jTextClassType.setPreferredSize(new java.awt.Dimension(300, 25));
        jPanelDetail.add(jTextClassType);

        jLabelDefault.setText("     Default value");
        jPanelDetail.add(jLabelDefault);

        jTextDefault.setEditable(false);
        jTextDefault.setFont(new java.awt.Font("Tahoma", 1, 11));
        jTextDefault.setPreferredSize(new java.awt.Dimension(300, 25));
        jPanelDetail.add(jTextDefault);

        jLabelEmpty5.setPreferredSize(new java.awt.Dimension(600, 10));
        jPanelDetail.add(jLabelEmpty5);

        jLabelDesc.setText("       Description        ");
        jPanelDetail.add(jLabelDesc);

        jTextDesc.setColumns(25);
        jTextDesc.setEditable(false);
        jTextDesc.setLineWrap(true);
        jTextDesc.setRows(4);
        jScrollPane3.setViewportView(jTextDesc);

        jPanelDetail.add(jScrollPane3);

        jLabelEmpty6.setPreferredSize(new java.awt.Dimension(600, 10));
        jPanelDetail.add(jLabelEmpty6);

        jLabelPropVal.setText("   Current value                   ");
        jPanelDetail.add(jLabelPropVal);

        jScrollPane7.setMinimumSize(new java.awt.Dimension(250, 23));
        jScrollPane7.setPreferredSize(new java.awt.Dimension(250, 75));
        jListPropVal.setVisibleRowCount(3);
        jScrollPane7.setViewportView(jListPropVal);

        jPanelDetail.add(jScrollPane7);

        jLabelNewVal.setText("         New value  ");
        jPanelDetail.add(jLabelNewVal);

        jTextNewVal.setPreferredSize(new java.awt.Dimension(300, 25));
        jPanelDetail.add(jTextNewVal);

        jComboComponent.setFont(new java.awt.Font("Tahoma", 1, 10));
        jComboComponent.setPreferredSize(new java.awt.Dimension(300, 25));
        jPanelDetail.add(jComboComponent);

        jLabelEmpty7.setPreferredSize(new java.awt.Dimension(600, 10));
        jPanelDetail.add(jLabelEmpty7);

        jButtonAdd.setText("Add");
        jButtonAdd.setPreferredSize(new java.awt.Dimension(200, 25));
        jButtonAdd.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddActionPerformed(evt);
            }
        });

        jPanelDetail.add(jButtonAdd);

        jButtonRemove.setText("Remove");
        jButtonRemove.setPreferredSize(new java.awt.Dimension(200, 25));
        jButtonRemove.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRemoveActionPerformed(evt);
            }
        });

        jPanelDetail.add(jButtonRemove);

        jButtonChange.setMnemonic(java.awt.event.KeyEvent.VK_C);
        jButtonChange.setText("Change Value");
        jButtonChange.setPreferredSize(new java.awt.Dimension(200, 25));
        jButtonChange.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonChangeActionPerformed(evt);
            }
        });

        jPanelDetail.add(jButtonChange);

        jButtonRefresh.setText("Refresh Property");
        jButtonRefresh.setPreferredSize(new java.awt.Dimension(250, 23));
        jButtonRefresh.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRefreshActionPerformed(evt);
            }
        });

        jPanelDetail.add(jButtonRefresh);

        jSplitPaneInner.setRightComponent(jPanelDetail);

        jRightPanel.add(jSplitPaneInner);

        jSplitPaneOuter.setRightComponent(jRightPanel);

        add(jSplitPaneOuter, java.awt.BorderLayout.CENTER);

    }// </editor-fold>//GEN-END:initComponents

    private void jButtonRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRemoveActionPerformed
        String  classname = (String)jListOuter.getSelectedValue();
        String prop = (String)jListInner.getSelectedValue();
        String setname = (String)jComboName.getSelectedItem();
        String delval = (String)jListPropVal.getSelectedValue();

        if ( delval != null && (classname != null) &&
                ( setname != null) && (prop != null) )
        {
            List<String> newlist = new ArrayList<String>();
            Object[] values = ((DefaultListModel)jListPropVal.getModel()).toArray();
            for (Object obj : values) {
                if (obj instanceof String) 
                    newlist.add ((String)obj);
            }
            if( newlist.remove(delval) ){
                try {
                    if(newlist.isEmpty()){ // list is now empty
                        removePropFromSet(); // delete this property from the set
                    }else{ // list is not empty
                        if(_pm.allowChangePropertyValue(classname,setname,prop,newlist)){
                            _pm.changePropertyValue(classname,setname,prop,newlist);
                            ((DefaultListModel)jListPropVal.getModel()).removeElement(delval);
                        }
                    }
                }catch(PanelMediatorException e){
                    displayError("Internal Error : "+e.getMessage());
                }
            }
        }
    }//GEN-LAST:event_jButtonRemoveActionPerformed

    private void jButtonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddActionPerformed
        String newval = getTextInput();

        if( newval != null ){
            newval = newval.trim();
            String  classname = (String)jListOuter.getSelectedValue();
            String prop = (String)jListInner.getSelectedValue();
            String setname = (String)jComboName.getSelectedItem();

            List<String> newlist = new ArrayList<String>();
            Object[] values = ((DefaultListModel)jListPropVal.getModel()).toArray();
            for (Object obj : values) {
                if (obj instanceof String) 
                    newlist.add ((String)obj);
            }

            if ( !newval.isEmpty() && (classname != null) &&
                    ( setname != null) && (prop != null) )
            {
                try {
                    if(_pm.allowChangePropertyValue(classname,setname,prop,newlist)){
                        _pm.changePropertyValue(classname, setname, prop, newlist);
                        ((DefaultListModel)jListPropVal.getModel()).addElement(newval);
                    }
                }catch(PanelMediatorException e){
                    displayError("Internal Error : "+e.getMessage());
                }
            }
        }
    }//GEN-LAST:event_jButtonAddActionPerformed

    private void jButtonRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRefreshActionPerformed
        updateDetails();
    }//GEN-LAST:event_jButtonRefreshActionPerformed


    private void jButtonRemovePropActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRemovePropActionPerformed
        removePropFromSet();
    }//GEN-LAST:event_jButtonRemovePropActionPerformed

    /** private method that should remove this property from the configuration set */
    private void removePropFromSet(){
        if(jListOuter.getSelectedValue() != null && jListInner.getSelectedValue() != null){
            String propname = (String)jListInner.getSelectedValue();
            if ( isConfigSetChosen() && confirmAction("Confirm delete this item?")){
                try{
                    // there is valid propertyname and config set name
                    _pm.deletePropertyFromConfigurationSet(
                        (String)jListOuter.getSelectedValue(),
                         propname,(String)jComboName.getSelectedItem());
                    clearPanelDetail();
                    jListInner.clearSelection();
                }catch(PanelMediatorException e){
                    displayError("Error :" + e.getMessage());
                }
            }
            else{
                displayError("You have to select a configuration set");
            }
        }
    }

    /**
     * private method that's invoked when the 'see source code' button is clicked
     * It will request the code from PanelMediator
     * and display it in a messagebox
     */
    private void jButtonSourceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSourceActionPerformed
        if(jListOuter.getSelectedValue() != null){
            String classname = (String)jListOuter.getSelectedValue();
            String code = _pm.getSource(classname);
            jTextAreaSource.setText(code);
            jDialogSource.setSize(800,500);
            jDialogSource.setLocationRelativeTo(null);
            jDialogSource.setVisible(true);
        }
    }//GEN-LAST:event_jButtonSourceActionPerformed

    /* private method to handle 'Change' button action */
    private void jButtonChangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonChangeActionPerformed
        String newval = getTextInput();

        if( newval != null ){
            newval = newval.trim();
            String  classname = (String)jListOuter.getSelectedValue();
            String prop = (String)jListInner.getSelectedValue();
            String setname = (String)jComboName.getSelectedItem();

            if ( !newval.isEmpty() && (classname != null) &&
                    ( setname != null) && (prop != null) ){
                try {
                    if ( _pm.allowChangePropertyValue(classname, setname, prop, newval) )
                        _pm.changePropertyValue(classname, setname, prop, newval);
                    //succesfully change the model, now update the current value
                    clearPropValue();
                    ((DefaultListModel)jListPropVal.getModel()).addElement
                            (_pm.getPropertyValue(classname,setname,prop).next());
                }catch(PanelMediatorException pme){
                    displayError("Internal Error : "+pme.getMessage());
                }
            }
        }
    }//GEN-LAST:event_jButtonChangeActionPerformed

    /* private method to handle a selection change of Inner list */
    private void jListInnerValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jListInnerValueChanged
        updateDetails();
    }//GEN-LAST:event_jListInnerValueChanged

   /* private method that updates the panel detail information based on the
    * selection in the outer list(classname) and inner list(property name), 
    * and configuration set selected in combo box
    */
    private void updateDetails(){
        //update the property info
        String prop = (String)jListInner.getSelectedValue();
        if (prop != null){
            String  classname = (String)jListOuter.getSelectedValue();
            ConfigurableProperty cp = _pm.getProperty(classname, prop);
            if(cp != null){
                jTextPropName.setText(cp.getName());

                PropertyType mytype = cp.getType();
                jTextPropType.setText((mytype==null)?null:mytype.toString());

                if (mytype == PropertyType.COMPONENT || mytype == PropertyType.COMPONENT_LIST) {
                    jLabelClassType.setVisible(true);
                    jTextClassType.setVisible(true);
                    jTextClassType.setText(cp.getClassType());
                } else {
                    jLabelClassType.setVisible(false);
                    jTextClassType.setVisible(false);
                    jTextClassType.setText("");
                }

                jTextDefault.setText(cp.getDefault());
                jTextDesc.setText(cp.getDesc());
                if( isConfigSetChosen() )
                    initPropValue(_pm.getPropertyValue(classname,
                        (String)jComboName.getSelectedItem(),prop));
                else
                    clearPropValue();

                jTextNewVal.setText(null);
                jComboComponent.removeAllItems();

                //check the type of input that is going to be needed
                // component? list? other type? or not specified?                   
                setVisibleListPropButton( _pm.isListProperty(classname,prop) );
                if( _pm.isComponentProperty(classname,prop) ){
                    // is a component property
                    setVisibleComponentInput(true);
                    // addComponent to jComboComponent
                    // System.out.println("$$$ is a component");
                    List<String> mylist = _pm.getComponentList(classname,prop);
                    if(mylist != null && !mylist.isEmpty()){
                        for (String item : mylist) {
                            jComboComponent.addItem(item);
                        }
                    }
                }else
                    setVisibleComponentInput(false);

            }
        }
    }

    /* private method to handle action performed by the Top Right GUI items */
    private void RightPanelTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RightPanelTopActionPerformed
        Object source = evt.getSource();
        if ( source instanceof javax.swing.JButton ){ // delete button is clicked
            // the first two items in combo box are not for delete
            if ( isConfigSetChosen() && confirmAction("Confirm delete this item?")){
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
            if (isConfigSetChosen()){
                setEnablePanelDetail(true);
            }

            else if (jComboName.getSelectedIndex() == 0){
                //selected item is 'create new set'               
                String s = JOptionPane.showInputDialog(this,
                         "Please enter the name of new config",
                                         "Create New Configuration Set",
                                         JOptionPane.PLAIN_MESSAGE);

                //If a string was returned, say so.               
                if (s != null && !s.isEmpty() && !s.trim().isEmpty() ) {
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
            // fill up combo with config set names
            Set<String> config = _pm.getConfigurationSet((String)jListOuter.getSelectedValue());
            if ( config != null){
                for (String configItem : config) {
                    jComboName.addItem(configItem);
                }
            }
            // fill up jList with configurable property names
            Set<String> prop = _pm.getPropertySet((String)jListOuter.getSelectedValue());
            if ( prop != null ){
                DefaultListModel innerlistModel= (DefaultListModel)jListInner.getModel();
                for (String propItem : prop) {
                    //  ListItem li = new ListItem(false, propItem );
                    innerlistModel.addElement(propItem);
                }
            }
        }
    }//GEN-LAST:event_jListOuterValueChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAdd;
    private javax.swing.JButton jButtonChange;
    private javax.swing.JButton jButtonDel;
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonRemove;
    private javax.swing.JButton jButtonRemoveProp;
    private javax.swing.JButton jButtonSource;
    private javax.swing.JComboBox jComboComponent;
    private javax.swing.JComboBox jComboName;
    private javax.swing.JDialog jDialogSource;
    private javax.swing.JPanel jInnerLeftPanel;
    private javax.swing.JLabel jLabelClass;
    private javax.swing.JLabel jLabelClassType;
    private javax.swing.JLabel jLabelDefault;
    private javax.swing.JLabel jLabelDesc;
    private javax.swing.JLabel jLabelEmpty1;
    private javax.swing.JLabel jLabelEmpty2;
    private javax.swing.JLabel jLabelEmpty4;
    private javax.swing.JLabel jLabelEmpty5;
    private javax.swing.JLabel jLabelEmpty6;
    private javax.swing.JLabel jLabelEmpty7;
    private javax.swing.JLabel jLabelName;
    private javax.swing.JLabel jLabelName1;
    private javax.swing.JLabel jLabelNewVal;
    private javax.swing.JLabel jLabelPropName;
    private javax.swing.JLabel jLabelPropType;
    private javax.swing.JLabel jLabelPropVal;
    private javax.swing.JPanel jLeftPanel;
    private javax.swing.JList jListInner;
    private javax.swing.JList jListOuter;
    private javax.swing.JList jListPropVal;
    private javax.swing.JPanel jPanelDetail;
    private javax.swing.JPanel jRightPanel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JSeparator jSeparator;
    private javax.swing.JSplitPane jSplitPaneInner;
    private javax.swing.JSplitPane jSplitPaneOuter;
    private javax.swing.JTextArea jTextAreaSource;
    private javax.swing.JTextField jTextClass;
    private javax.swing.JTextField jTextClassType;
    private javax.swing.JTextField jTextDefault;
    private javax.swing.JTextArea jTextDesc;
    private javax.swing.JTextArea jTextInnerList;
    private javax.swing.JTextField jTextNewVal;
    private javax.swing.JTextField jTextPropName;
    private javax.swing.JTextField jTextPropType;
    // End of variables declaration//GEN-END:variables

    /**
     * This private class would handle the data management for this GUI Panel
     *
     * @author Ariani
     */
    private class PanelMediator implements GUIFileActionListener {

        private final Map<String, ConfigurableComponent> _ccmap ; // ConfigurableComponent map based on component classname
        private final String _sectionName ;
        private final GUIMediator _gmediator;
        private final PanelConfigurable _panel;

        /**
         * Creates a new instance of PanelMediator
         */
        private PanelMediator(String name, Set<ConfigurableComponent> grpset, GUIMediator gmediator,
            PanelConfigurable panel) {
            _gmediator = gmediator;
            _panel = panel;
            _sectionName = name;
            _ccmap = new HashMap<String, ConfigurableComponent>();
            setGroupMap(grpset);

            _gmediator.registerPanel(this);
        }


        /**
         * from the set of classes belong to this group, create its own Map
         * also used for reset and reload of new data
         */
        private void setGroupMap(Set<ConfigurableComponent> grpset){
            _ccmap.clear();

            //create a hashmap of group members
            for (ConfigurableComponent cc : grpset) {
                _ccmap.put(cc.getName(), cc);
            }
        }

        /** This method is inherited from GUIFileActionListener
         * It is called once there is a new configuration file loaded
         * Model data will be updated automatically - only need to clear the GUI
         */
        @Override
        public void update(ConfigProperties cp) {
            _panel.clearAllDisplay();
        }

        /** This method is inherited from GUIFileActionListener
         * It is called once the configuration file is about to be written
         * Nothing needs to be done for GUI
         */
        @Override
        public void saveData(ConfigProperties cp) throws GUIOperationException {
            /* do nothing */
        }

        /**
         * This method is inherited from GUIFileActionListener
         * Method purpose is to clear all configuration data
         */
        @Override
        public void clearAll() {
            _panel.clearAllDisplay();
            System.out.println("*** clear all ");
        }

        /**
         * This method is inherited from GUIFileActionListener
         * Method purpose is to update panels after model change
         */
        @Override
        public void modelRefresh() {
            /* do nothing */
        }


        /**
         * @return the whole map that represents all information for the GUI Panel
         */
        private Map<String, ConfigurableComponent> getGroupMap(){
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
        private Set<String> getConfigurationSet(String classname){
            if (_ccmap.containsKey(classname)){
                ConfigurableComponent cc = _ccmap.get(classname);

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
                ConfigurableComponent cc = _ccmap.get(classname);
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
         * delete a property from the configuration set
         *
         * @param classname Class name
         * @param propname Property to be deleted
         * @param setname Configuration set from which the property would be deleted
         */
        private void deletePropertyFromConfigurationSet
            (String classname,String propname,String setname)
            throws PanelMediatorException
        {
             if( _ccmap.containsKey(classname) ){
                ConfigurableComponent cc = _ccmap.get(classname);
                if(cc.containsConfigurationSet(setname)){
                    cc.deleteOneConfigurationPropFromSet(setname,propname);
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
         * @param classname Name of class where the set belongs to
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
                ConfigurableComponent cc = _ccmap.get(classname);
                cc.createNewSet(name);
            }
            else
                throw new PanelMediatorException(
                        PanelMediatorException.INVALID_CLASSNAME, "Invalid class name");
        }

        /**
         * @return Set of configrable properties owned by the specified class
         */
        private Set<String> getPropertySet(String classname){
            if (_ccmap.containsKey(classname)){
                ConfigurableComponent cc = _ccmap.get(classname);
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
                ConfigurableComponent cc = _ccmap.get(classname);
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
         * check if this property requires a component with particular class type
         * @param classname
         * @param propname property name
         * @return Boolean true if it needs a component as value
         */
        private boolean isComponentProperty (String classname, String propname){
            ConfigurableProperty cp = getProperty(classname,propname);
            if(cp != null){
                PropertyType type = cp.getType();
                if (type != null && type.toString().startsWith("Component"))
                    return true;
            }
            return false;
        }

        /**
         * check if this property type is a list type
         * @param classname
         * @param propname property name
         * @return Boolean true if it is a list
         */
        private boolean isListProperty (String classname, String propname){
            ConfigurableProperty cp = getProperty(classname,propname);
            if(cp != null){
                PropertyType type = cp.getType();
                if (type == null)
                    return true; // assume it's a list if there is no type
                if (type.toString().trim().endsWith("List"))
                    return true;
            }else{
                return true;
                // assume it's a list if there is no such property
            }
            return false;
        }

        /**
         * @return property value from the specified classname, set, and property
         *         a <code>List</code> would be represented as a semi-colon separated
         *          <code>String</code>
         */
        private Iterator<?> getPropertyValue(String classname,String setName, String propname){
            if( _ccmap.containsKey(classname) ){
                ConfigurableComponent cc = _ccmap.get(classname);
                Object propval = cc.getConfigurationPropValue(setName,propname);
                if (propval != null){
                    if (propval instanceof String){
                        List<String> retlist = new ArrayList<String>();
                        retlist.add((String)propval);
                        return retlist.iterator();
                    }
                    else { // instance of List
                        return ((List<?>)propval).iterator();
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
            ConfigurableComponent cc = _ccmap.get(classname);
            cc.changeConfigurationPropValue(setName,propname,propval);
        }

        /** change the value of the property from specified class, and set
         *
         * @throws PanelMediatorException
         */
        private void changePropertyValue(String classname,String setName,
                String propname, List<String> propval) throws PanelMediatorException
        {
            ConfigurableComponent cc = _ccmap.get(classname);
            cc.changeConfigurationPropValue(setName,propname,propval);
        }



        /** check if this new property is valid
         *
         * @param classname Name of class
         * @param setName   Configuration set name
         * @param propname  Property name
         * @param propval   New property value
         * @return true if new value is valid
         */
        private boolean allowChangePropertyValue(String classname,String setName,
                String propname, Object propval) throws PanelMediatorException
        {
            if( _ccmap.containsKey(classname) ){
                ConfigurableComponent cc = _ccmap.get(classname);

                if(cc.containsConfigurationSet(setName) && cc.containsProperty(propname)){
                    if(isValidPropertyValue(classname,propname,propval)){
                        return true;
                    }
                    else{
                        throw new PanelMediatorException(
                                PanelMediatorException.INVALID_VALUE,
                                "Invalid property value");
                    }
                }
                else if (!cc.containsConfigurationSet(setName)){
                     throw new PanelMediatorException(
                         PanelMediatorException.INVALID_SETNAME,
                         "Invalid configuration set name");
                }
                else { // does not contain this property in the component
                    throw new PanelMediatorException(
                        PanelMediatorException.INVALID_PROPNAME, "Invalid property name");
                }


            } else { // does not have this class in this section
                throw new PanelMediatorException(
                        PanelMediatorException.INVALID_CLASSNAME, "Invalid class name");
            }
        } // changePropertyValue method

        /**
         * check if this value is valid for the specified property
         * @param classname Name of Class
         * @param propname Property Name
         * @param propval Value to be checked
         * @return true of it is valid
         */
        private boolean isValidPropertyValue(String classname,String propname,
                Object propval)
        {
            if( _ccmap.containsKey(classname) ){
                ConfigurableComponent cc = _ccmap.get(classname);

                if( cc.containsProperty(propname)){
                    ConfigurableProperty cp = cc.getProperty(propname);
                    return cp.getType() == null || cp.getType().isValid(propval); // no specified property is valid
                }
            }
            return false;
        }


        /**
         * this method is used to get java source code of a particular class
         * The source code is generated by ModelBuilder
         *
         * @param classname Name of java class to search
         */
        private String getSource(String classname){
           return _gmediator.getModelBuilder().getSourceCode(classname);
        }

        /**
         * this method is used to get a list of classes that meets the property
         * type restriction
         */
        private List<String> getComponentList(String classname,String prop){
            ConfigurableProperty cp = getProperty(classname,prop);
            if(cp != null){
                String classtype = cp.getClassType();
                if(classtype != null && !classtype.isEmpty()){
                    // class type is an existing class
                    // get list of clases that are / subclass of this type
                    // return mymap that contains name of class 
                    // and configuration stored for that class
                    Map<String, String> mymap = _gmediator.getModelBuilder().getclasslist(classtype);
                    if(mymap != null && !mymap.isEmpty()){
                        List<String> myreturn = new ArrayList<String>();
                        for (Map.Entry<String, String> me : mymap.entrySet()) {
                            String setname = me.getKey();//full class name
                            String fullname = me.getValue();//config set
                            int index = fullname.lastIndexOf('.');
                            String localname = fullname.substring(index + 1);
                            String packagename = fullname.substring(0, index);
                            // format the output to be "setname-classname"
                            String myitem = setname + '-' + localname + '(' + packagename + ')';
                            // System.out.println("item $$ "+myitem);                            
                            myreturn.add(myitem);
                        }
                        return myreturn;
                    }
                }
            }
            return null;
        }

    } // end inner class

    private class PanelMediatorException extends java.lang.Exception {

        private final static int INVALID_CLASSNAME = 1;
        private final static int DUPLICATE_SET_NAME = 2;
        private final static int INVALID_SETNAME = 3;
        private final static int INVALID_PROPNAME = 4;
        private final static int INVALID_VALUE = 5;

        /**
         * Creates a new instance of <code>PanelMediatorException</code> with detail message.
         */
        private PanelMediatorException(int mode, String msg) {
            super(msg);
        }
    } // end inner exception class 

}// end PanelConfigurable class

class ListItem {
      private final static Color NO_VALUE = Color.WHITE;
      private final static Color WITH_VALUE = Color.BLUE;
      private final String value;
      private final Color color;

      public ListItem(boolean isValueSet, String s) {
          if(isValueSet)
              color = WITH_VALUE;
          else
              color = NO_VALUE;
          value = s;
      }
      public Color getColor() {
          return color;
      }
      public String getValue() {
          return value;
      }
  } // end ListItem class

 class MyCellRenderer extends JLabel implements ListCellRenderer {

    public MyCellRenderer () {
        // Don't paint behind the component
        setOpaque(true);
    }

    // Set the attributes of the class and return a reference
    @Override
    public Component getListCellRendererComponent(JList list,
            Object value, // value to display
            int index,    // cell index
            boolean selected,  // is selected
            boolean hasFocus)  // cell has focus?
    {
      // Set the text and color background for rendering
      setText(((ListItem)value).getValue());
      setBackground(((ListItem)value).getColor());

      // Set a border if the list item is selected
        if (selected) {
            setBorder(BorderFactory.createLineBorder(Color.blue, 2));
        } else {
            setBorder(BorderFactory.createLineBorder(list.getBackground(), 2));
        }
        return this;
    }
 }//end MyCellRenderer
