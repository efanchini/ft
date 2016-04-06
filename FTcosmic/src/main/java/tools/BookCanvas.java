/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import org.root.attr.TStyle;
import org.root.base.IDataSet;
import org.root.func.F1D;
import org.root.func.RandomFunc;
import org.root.histogram.H1D;
import org.root.basic.EmbeddedCanvas;


/**
 *
 * @author louiseclark
 */


public class BookCanvas extends JPanel implements ActionListener {
    
        private List<IDataSet> container = new ArrayList<IDataSet>();
        private List<String>   options   = new ArrayList<String>();
        private EmbeddedCanvas canvas    = new EmbeddedCanvas();
        private int            nDivisionsX = 1;
        private int            nDivisionsY = 1;
        private int            currentPosition = 0;
        private boolean backward = false; 
        JComboBox comboDivide = null;
        
        
    public BookCanvas(){
        initCanvas();
    }

    
    public void initCanvas() {
        // event canvas
        this.canvas.setGridX(false);
        this.canvas.setGridY(false);
        this.canvas.setAxisFontSize(10);
        this.canvas.setTitleFontSize(16);
        this.canvas.setAxisTitleFontSize(14);
        this.canvas.setStatBoxFontSize(8);
    }
    
    
    public BookCanvas(int dx, int dy){
        super();
      
        TStyle.setFrameFillColor(250, 250, 255);
        this.setLayout(new BorderLayout());
        
        this.nDivisionsX = dx;
        this.nDivisionsY = dy;
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        
        JButton  buttonPrev = new JButton("< Previous");
        JButton  buttonNext = new JButton("Next >");
        JButton  buttonSave = new JButton("Save to File");
        buttonNext.addActionListener(this);
        buttonPrev.addActionListener(this);
        buttonSave.addActionListener(this);
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(buttonPrev);
        buttonPanel.add(buttonNext);
        buttonPanel.add(buttonSave);
        canvas.divide(this.nDivisionsX, this.nDivisionsY);
        
    
        JPanel canvasPane = new JPanel();
        canvasPane.setLayout(new BorderLayout());
        canvasPane.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        canvasPane.add(canvas,BorderLayout.CENTER);

        this.add(canvasPane,BorderLayout.CENTER);
        this.add(buttonPanel,BorderLayout.PAGE_END);
    }

    public void add(IDataSet ds, String opt){
        this.container.add(ds);
        this.options.add(opt);
    }

    
    public void drawNextBack(boolean back){
        int npads   = this.nDivisionsX*this.nDivisionsY;
        int counter = 0;
        int elements =1;
        if(back){
            for(int i=(this.currentPosition); i<this.container.size(); i++){
               if(this.options.get(i).contains("same"))elements++;
               else break;
            }
            this.currentPosition-=(2*elements*npads);
            if(this.currentPosition>=this.container.size() || this.currentPosition<=0){
                
                this.currentPosition = this.container.size()-(2*elements*npads);
            }        
        }
        else{
            if(this.currentPosition>=this.container.size()){
            this.currentPosition = 0;
            }
        }
        
        canvas.divide(this.nDivisionsX,this.nDivisionsY);
        canvas.cd(counter);
        this.initCanvas();
            
        while(this.currentPosition<this.container.size()&&counter<npads){
            IDataSet ds = this.container.get(this.currentPosition);
            String   op = this.options.get(this.currentPosition);            
            if(op.contains("same")==false){
//                System.out.println(" (    ) "  + this.currentPosition + 
//                        " on pad " + counter+"   "+ds.getName());
                canvas.cd(counter);
                canvas.draw(ds, "");
                this.initCanvas();
                counter++;
            } else {
//                System.out.println(" Drawing Position (same)"  + this.currentPosition + 
//                        " on pad " + counter+"   "+ds.getName());
                canvas.draw(ds, "same");
            }
            this.currentPosition++;
        }
    }
   
    public void reset(){
        this.currentPosition = 0;
        
    }
    

    public void actionPerformed(ActionEvent e) {
        System.out.println("action " + e.getActionCommand());
        if(e.getActionCommand().compareTo("Next >")==0){
            this.drawNextBack(backward);
        }
        
        if(e.getActionCommand().compareTo("< Previous")==0){
           this.backward = true;
           this.drawNextBack(backward);
        }
        
        if(e.getActionCommand().compareTo("comboBoxChanged")==0){
            String selection = (String) this.comboDivide.getSelectedItem();
            System.out.println("Changed to " + selection);
            if(selection.compareTo("1x1")==0){
                this.nDivisionsX = 1;
                this.nDivisionsY = 1;
                this.reset();
            }
            
            if(selection.compareTo("2x2")==0){
                this.nDivisionsX = 2;
                this.nDivisionsY = 2;
                this.reset();
            }
        }
        if (e.getActionCommand().compareTo("Save to File") == 0) {
            this.saveToFile();
           
        }
    }
    
     private void saveToFile() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("File.PNG"));
        int returnValue = fc.showSaveDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            String outputFileName = fc.getSelectedFile().getAbsolutePath();
            this.canvas.save(outputFileName);
            System.out.println("Saving calibration results to: " + outputFileName);
        }
    }  
    
    
}


