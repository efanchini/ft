/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.clas.containers.FTHashTable;

/**
 *
 * @author devita
 */
public class FTApplication implements ActionListener {

    private String       appName    = null;
    private FTDetector   detector   = null;
    private FTDataSet    dataSet    = null;
    private NoGridCanvas canvas     = null;
    private JPanel       radioPane  = new JPanel();
    private FTHashTable  table      = new FTHashTable();
    private String       buttonSelect;

    
    public FTApplication(FTDetector d) {
        this.detector = d;
        this.dataSet  = new FTDataSet(d);
        this.canvas   = new NoGridCanvas();
    }
  
    public FTApplication(FTDetector d, String name) {
        this.appName  = name;
        this.detector = d;
        this.dataSet  = new FTDataSet(d);
        this.canvas   = new NoGridCanvas();
    }
  
    public FTApplication(FTDetector d, String name, String... buttons) {
        this.appName  = name;
        this.detector = d;
        this.dataSet  = new FTDataSet(d);
        this.addRadioButtons(buttons);
        this.addHashTable(buttons);
    } 
    
    public FTDetector getDetector() {
        return this.detector;
    }
    
    public String getName() {
        return this.appName;
    }
    
    public void setName(String name) {
        this.appName=name;
    }
    
    public void setCanvas(NoGridCanvas c) {
        this.canvas = c;
    }
    
    public NoGridCanvas getCanvas() {
        return this.canvas;
    }
    
    public void setRadioPane(JPanel radioPane) {
        this.radioPane = radioPane;
    }

    public JPanel getRadioPane() {
        return radioPane;
    }
    
    public String getButtonSelect() {
        return buttonSelect;
    }
    
    public FTDataSet getData() {
        return dataSet;
    }
    
    public final void addRadioButtons(String... buttons){
        this.radioPane.setLayout(new FlowLayout());
    	ButtonGroup bG = new ButtonGroup();
    	for(String item : buttons){
            System.out.println(item);
            JRadioButton b = new JRadioButton(item);
            if(bG.getButtonCount()==0) b.setSelected(true);
            b.addActionListener(this);
            this.radioPane.add(b); bG.add(b);
    	}
    }   
    
    public final void addHashTable(String... list) {
        table = new FTHashTable(3,list);
        double[] values = new double[list.length];
        System.out.println(list.length);
        for(int i=0; i<list.length; i++) values[i]=-1;
        for (int component : this.detector.getDetectorComponents()) {
            table.addRow(values,0,0,component);           
        }
    }
    
    public FTHashTable getTable() {
        return this.table;
    }

    public Color getColor(int key) {
        Color col = new Color(100, 100, 100);
        return col;
    }
        
    public void actionPerformed(ActionEvent e) {
        System.out.println(this.getName() + " application radio button set to: " + e.getActionCommand());
        buttonSelect=e.getActionCommand();
    }
        
}
