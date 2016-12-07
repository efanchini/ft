/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.viewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.accessibility.AccessibleContext;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;


import org.clas.ftcal.FTCALGamma;
import org.clas.ftcal.FTCALDetector;
import org.clas.ftcal.FTCALSimulatedData;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.basic.IDetectorModule;
import org.jlab.clas12.basic.IDetectorProcessor;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.clasrec.main.DetectorEventProcessorPane;
import org.jlab.data.io.DataEvent;
import org.jlab.evio.clas12.EvioDataEvent;
import org.jlab.evio.clas12.EvioSource;
import org.root.attr.ColorPalette;


/**
 *
 * @author gavalian
 */
public class FTGammaViewerModule implements IDetectorProcessor, IDetectorModule, ActionListener {
    
    
    FTCALGamma moduleFTCAL=new FTCALGamma();   
    DetectorEventProcessorPane evPane = new DetectorEventProcessorPane();
   

    EventDecoder decoder = new EventDecoder();
    int nProcessed = 0;
    FTCALDetector viewFTCAL = new FTCALDetector("FTCAL");            
    FTCALSimulatedData simulation = new FTCALSimulatedData(viewFTCAL);
    
    // ColorPalette class defines colors 
    ColorPalette palette = new ColorPalette();
 
    JPanel detectorPanel = null;
    JPanel FTCALPanel = null;

    public FTGammaViewerModule() {
        
        moduleFTCAL.setDecoder(decoder);
        
        this.initRawDataDecoder();
        
        this.initDetector();
        this.initApps();
        this.initHistograms();
        
        this.evPane.addProcessor(this);
        
        for(int i=0; i<this.evPane.getComponents().length;i++){
          AccessibleContext p = this.evPane.getComponent(i).getAccessibleContext();
          if(this.evPane.getComponent(i).getAccessibleContext().getAccessibleName()=="File"){
             JButton  buttonOpen = new JButton("Evio FileS");
             buttonOpen.addActionListener(this);
             this.evPane.add(buttonOpen);
             this.evPane.repaint(i);
          }
        }
        
        /*Graphics starts here*/
        this.detectorPanel = new JPanel();
        this.detectorPanel.setLayout(new BorderLayout());
        
        this.FTCALPanel = new JPanel(new BorderLayout());
        moduleFTCAL.setDetectorPanel(this.FTCALPanel);        
        moduleFTCAL.initPanel();
        
        // filling main panel with tabs for different FT subdetectors and event handling panel
        this.detectorPanel.add(this.FTCALPanel, BorderLayout.CENTER);
        this.detectorPanel.add(this.evPane, BorderLayout.PAGE_END);
       
        
    }
    
    private void initDetector() {        
        moduleFTCAL.initDetector();
    }

    private void initApps() {
        moduleFTCAL.initApps();
    }

    private void initRawDataDecoder() {
        moduleFTCAL.initDecoder();
    }

    private void initHistograms() {
    }

    private void resetHistograms() {
        moduleFTCAL.resetHistograms();
    }

    
    public void processEvent(DataEvent de) {
        EvioDataEvent event = (EvioDataEvent) de;
        if(event.hasBank("FTCAL::dgtz")){
            simulation.eventBankDecoder(event,"FTCAL::dgtz");
            DetectorCollection<Double> adc = this.simulation.getSimAdc();
            DetectorCollection<Double> tdc = this.simulation.getSimTdc();
            moduleFTCAL.processDecodedSimEvent(adc,tdc);  
        }
        else{
        decoder.decode(event);
        moduleFTCAL.processDecodedEvent();  
        }
        nProcessed++;
        if((nProcessed%5000)==0)System.out.println("Decoded Event: "+nProcessed);
    }

        
    public String getName() {
        return "FTGammaViewerModule";
    }

    public String getAuthor() {
        return "Fanchini";
    }

    public DetectorType getType() {
        return DetectorType.FTCAL;
    }

    public String getDescription() {
        return "FT Gamma Display";
    }

    public JPanel getDetectorPanel() {
        return this.detectorPanel;
    }

    public static void main(String[] args) {
        FTGammaViewerModule module = new FTGammaViewerModule();
        JFrame frame = new JFrame();
        frame.add(module.getDetectorPanel());
        frame.pack();
        frame.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        System.out.println("FTViewer ACTION = " + e.getActionCommand());
        if(e.getActionCommand().compareTo("Evio FileS")==0) FileList();
    }

    private void FileList(){
        EvioSource reader = new EvioSource();
        
        JFileChooser fc = new JFileChooser();
        File file = new File("./");
        int returnValue = fc.showOpenDialog(null);
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setAcceptAllFileFilterUsed(false);
        
        int nf=0;
        String ff ="";
        if (returnValue == JFileChooser.APPROVE_OPTION) {
           //System.out.println("Erica: HELP: "+fc.getSelectedFile().getAbsolutePath()+"  "+fc.getCurrentDirectory().getPath());
            File[] folders = new File(fc.getCurrentDirectory().getPath()).listFiles();
            for (File fd : folders) {
                 //System.out.println("Erica: HELP: "+fd.isDirectory()+" "+fd.getAbsolutePath());
                if(fd.isFile()){
                    if(nf==0)ff=fd.getName();
                    // File type chose based on the first file selected
                    if((ff.contains(".evio") && !fd.getName().contains(".evio")) || (ff.contains(".hipo") && !fd.getName().contains(".hipo")))continue;
                   //System.out.println("Erica: file: "+fd.getName());
                    //System.out.println("Erica: basta dir: "+fd.getAbsolutePath());
                   reader.open(fd);
                   Integer current = reader.getCurrentIndex();
                   Integer nevents = reader.getSize();
                    System.out.println("FILE: "+nf+" "+fd.getName()+" N.EVENTS: " + nevents.toString() + "  CURRENT : " + current.toString());
                   for(int k=0; k<nevents; k++){
                   //System.out.println("ERICA EVENTS "+reader.hasEvent());
                   if(reader.hasEvent()){
                       DataEvent event = reader.getNextEvent();
                        processEvent(event);
                        }
                   }
                   nf++;
                }
            }
        }   
    }
    
    
    public void detectorSelected(DetectorDescriptor dd) {
        //To change body of generated methods, choose Tools | Templates.
    }
  
    
  

}
