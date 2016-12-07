/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.containers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author gavalian
 */
public class FTHashTableViewer extends JPanel {
    
    JTable  table = new JTable();    
    private List<IFTHashTableListener>    listeners = new ArrayList<IFTHashTableListener>();
    FTHashTable   hashTable = null;
    
    public FTHashTableViewer(FTHashTable ht){
        super();
        this.hashTable = ht;
        this.setPreferredSize(new Dimension(600,600));
        this.setLayout(new BorderLayout());
        table.setModel(ht);
        HashTableCellRenderer renderer = new HashTableCellRenderer(ht);
        //renderer.addConstrain(2, 5, 10);
        table.setDefaultRenderer(Object.class, renderer);
        
        JScrollPane  scroll = new JScrollPane(table);
        //scroll.add(table);

        this.add(scroll,BorderLayout.CENTER);
    }
    
    public void addListener(IFTHashTableListener lt){
        this.listeners.add(lt);
    }
    
    
    private void initInterfaces(){
        this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){

            public void valueChanged(ListSelectionEvent e) {
                //System.out.println("Selection has changed " + getSelected().toString());
                if(listeners.size()>0){
                    int row   =  table.getSelectedRow();
                    int nindex = hashTable.getIndexCount();
                    int[] index = new int[nindex]; 
                }
            }
        });
    }
    
    public JTable getTable() {
        return this.table;
    } 
        
    public class HashTableCellRenderer extends DefaultTableCellRenderer {
    
    FTHashTable  table = null;
    
    
    public HashTableCellRenderer(FTHashTable t){
        this.table = t;
    }
        
    @Override
    public Component getTableCellRendererComponent
        (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component c = super.getTableCellRendererComponent
                                  (table, value, isSelected, hasFocus, row, column);
            
            if(isSelected==true){
                c.setBackground(new Color(20,20,255));                
                return c;
            }
            
            if(row%2==0){
                c.setBackground(new Color(240,255,240));
            } else {
                c.setBackground(new Color(240,240,255));
            }
            
            if(this.table.isValid(row, column)==false){
                c.setBackground(new Color(255,200,200));
            }

            return c;
        }
        
        
}
    
    public static void main(String[] args){
        JFrame frame = new JFrame();
        frame.setSize(600, 600);
        FTHashTable  table = new FTHashTable(3,"a:i","b:i","c:d","r:d");
        table.addConstrain(2,  5.0, 10.0);
        table.addConstrain(5, 10.0, 20.0);
        /*
        table.addRowAsDouble(new String[]{"21","7","1","0.5","0.1","0.6"});
        table.addRowAsDouble(new String[]{"22","8","2","0.6","0.2","0.7"});
        table.addRowAsDouble(new String[]{"23","9","3","0.7","0.3","0.8"});
        table.addRowAsDouble(new String[]{"24","10","4","0.8","0.4","0.9"});
        */
        table.readFile("/Users/gavalian/Work/Software/Release-8.0/COATJAVA/coatjava/EC.table");
        table.show();
        FTHashTableViewer canvas = new FTHashTableViewer(table);
        frame.add(canvas);
        frame.pack();
        frame.setVisible(true);
    }
}
