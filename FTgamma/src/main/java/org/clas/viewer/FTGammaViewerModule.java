/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.viewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import org.jlab.evio.clas12.EvioDataBank;
import org.jlab.evio.clas12.EvioDataEvent;
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
       
    }

    public void detectorSelected(DetectorDescriptor dd) {
        //To change body of generated methods, choose Tools | Templates.
    }
  
    
  

}
