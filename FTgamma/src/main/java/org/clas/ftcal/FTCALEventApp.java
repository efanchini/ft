/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import java.awt.Color;
import java.util.List;
import org.clas.tools.FTApplication;
import org.clas.tools.FTDetector;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas12.detector.DetectorCounter;
import org.root.attr.ColorPalette;
import org.root.histogram.H1D;

/**
 *
 * @author devita
 */
public class FTCALEventApp extends FTApplication {

    // Data Collection
    DetectorCollection<H1D> H_WAVE   = new DetectorCollection<H1D>();
    H1D H_WMAX         = null;
    H1D H_TCROSS       = null;
    
    // decoder related information
    int nProcessed = 0;
    Boolean realflag=true;
    
       // analysis realted info //
    double nsPerSample=4;
    double LSB = 0.4884;
    double clusterThr = 50.0;// Vertical selection
    double singleChThr = 20.0;// Single channel selection
    double signalThr =0.0;
    double simSignalThr=0.0;// Threshold used for simulated events in pC
    double startTime =124.4;
    double charge2e = 15.3/6.005; //MeV
    
    
    public FTCALEventApp(FTDetector d) {
        super(d);
        this.setName("Event Viewer");
        this.addCanvas("Event");
        this.initCollections();
    }

    private void initCollections() {
        H_WAVE   = this.getData().addCollection(new H1D("Wave", 1000, 0.0, 5000.0),"Energy (MeV)","Counts",5,"H_WAVE");
        H_WMAX   = new H1D("WMAX", this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_TCROSS = new H1D("TCROSS", this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
    }
    
    public void addEvent(List<DetectorCounter> counters) {   
        H_WMAX.reset();
        H_TCROSS.reset();
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            if(H_WAVE.hasEntry(0, 0, key)) {
                H_WAVE.get(0, 0, key).reset();
                short pulse[] = counter.getChannels().get(0).getPulse();
                this.getFitter().fit(counter.getChannels().get(0),this.getDetector().getThresholds().get(0, 0, key));                
                for (int i = 0; i < Math.min(pulse.length, H_WAVE.get(0, 0, key).getAxis().getNBins()); i++) {
                    H_WAVE.get(0, 0, key).fill(i, pulse[i]);
                }
                H_WMAX.fill(key,this.getFitter().getWave_Max()-this.getFitter().getPedestal());
                H_TCROSS.fill(key,this.getFitter().getTime(3));
            }
        }
    }
    
    
    
    
    public void addSimEvent(DetectorCollection<Double> adc) { 
        realflag=false;
        H_WMAX.reset();
        double charge=0.0;
        for(int key : adc.getComponents(0, 0)) {
            if(H_WAVE.hasEntry(0, 0, key)) {
                charge=(adc.get(0, 0, key)*LSB*nsPerSample/50.0)*this.charge2e;
                H_WAVE.get(0, 0, key).fill(charge);
                H_WMAX.fill(key, charge);
            }
        }
    }
       
    public void updateCanvas(int keySelect) {
        this.getCanvas(0).draw(H_WAVE.get(0, 0, keySelect));
    }
    
     @Override
    public void resetCollections() {
        H_WMAX.reset();
        for (int component : this.getDetector().getDetectorComponents()) {
            H_WAVE.get(0, 0, component).reset();
        }
    }
    
    @Override
    public Color getColor(int key) {
        Color col = new Color(100, 100, 100);
        ColorPalette palette = new ColorPalette();
        if(this.realflag){
            if(H_WMAX.getBinContent(key)>this.getDetector().getThresholds().get(0, 0, key)) {
                if(H_TCROSS.getBinContent(key)>0) {
                   col = new Color(140, 0, 200);
                }
                else {
                  col = new Color(200, 0, 200);
             }
            }else col = new Color(100, 100, 100);
           }
        else{
            if(this.H_WAVE.get(0, 0, key).getEntries()>0){
                col = palette.getColor3D(H_WMAX.getBinContent(key), 12000, true);
            }
            else col = new Color(100, 100, 100);
        }
        return col;
    }
  
   
    
}
