/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.awt.GridLayout;
import javax.swing.SpringLayout;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.root.func.F1D;
import org.root.group.SpringUtilities;
import org.root.histogram.H1D;

/**
 *
 * @author fanchini
 */


public class CustomizeFit {
    
    public CustomizeFit(){}
    public F1D newfct;
  
    private F1D fct;
    private H1D hist;
    private ArrayList<Double> pars = new ArrayList<Double>();
    private ArrayList<Double> err_pars = new ArrayList<Double>();
    private double[] range =new double[2];
    
    
    public void FitPanel(H1D h, F1D f, String opt){
        this.hist=h;
        this.fct=f;
        this.newfct=f;
        this.pars.clear();
        this.err_pars.clear();
        int npar = f.getNParams();
        String f_name = f.getName(); 
        this.newfct.setName("New_"+f_name);
        
        CustomPanel panel = new CustomPanel();

        int result = JOptionPane.showConfirmDialog(null, panel, 
                        "Adjust Fit", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            for(int i=0; i<npar; i++){   
                if(panel.params[i].getText().isEmpty()){
                    this.pars.add(f.getParameter(i));
                    this.err_pars.add(f.parameter(i).error());
                }
                else {
                    this.pars.add(Double.parseDouble(panel.params[i].getText()));
                }
            }
            if(!panel.minRange.getText().isEmpty())this.range[0] = Double.parseDouble(panel.minRange.getText());
            else this.range[0] = f.getMin();
            if(!panel.maxRange.getText().isEmpty())this.range[1] = Double.parseDouble(panel.maxRange.getText());
            else this.range[1] = f.getMax();
            
            refit(opt); 
        }       
    }

    public void refit(String opt){
        for(int i=0; i<this.pars.size(); i++){
            this.newfct.setParameter(i, this.pars.get(i));
        }
        this.newfct.setRange(range[0], range[1]);
        this.hist.fit(this.newfct,opt+"R");
        for(int i=0; i<this.pars.size(); i++){
            this.err_pars.add(this.newfct.parameter(i).error());
        }
        this.newfct.setLineColor(3);
        
    }
    
    public String draw(){
        this.FitPanel(this.hist, this.fct,"");
        String panel="";
        return panel;
    }
	
    private class CustomPanel extends JPanel {
        
	JTextField minRange = new JTextField(3);
	JTextField maxRange = new JTextField(3);
	JTextField[] params = new JTextField[10];
        
	private CustomPanel(){
            this.setLayout(new SpringLayout());
            int npar = fct.getNParams();
           
            for (int i = 0; i < npar; i++) {  
                params[i] = new JTextField(3);
                JLabel l = new JLabel("Par"+i, JLabel.TRAILING);
                this.add(l);
                this.add(params[i]);
            }
            this.add(new JLabel("Minimum"));
            this.add(minRange);
            this.add(new JLabel("Maximum"));
            this.add(maxRange);
            
            //Lay out the panel.
            SpringUtilities.makeCompactGrid(this,
                                        npar+2, 2, //rows, cols
                                        6, 6,        //initX, initY
                                        6, 6);       //xPad, yPad
	}
    }    
   

    public void clear(){
        this.pars.clear();
        this.range[0]=0;
        this.range[1]=0;
        this.hist.reset();  
    }


    
    
}