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
    
    public FTCALEventApp(FTDetector d) {
        super(d);
        this.setName("Event Viewer");
        this.addCanvas("Event");
        this.initCollections();
    }

    private void initCollections() {
        H_WAVE   = this.getData().addCollection(new H1D("Wave", 300, 0.0, 600.0),"fADC Sample","fADC Counts",5,"H_WAVE");
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
//                 //for run609 //
//                if(key==9 || key==10 || key==31 || key==32 ||key==53 ||key==54 ||key==75 ||key==76 ||key==97 ||key==98 ||
//                    key==118 ||  key==119 || key==120 ||  key==140 ||key==141){
//                    this.getFitter().fitException(counter.getChannels().get(0), 4, 24, 60, 120);
//                }
//                else this.getFitter().fit(counter.getChannels().get(0),this.getDetector().getThresholds().get(0, 0, key));

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
        for(int key : adc.getComponents(0, 0)) {
            if(H_WAVE.hasEntry(0, 0, key)) {
                H_WAVE.get(0, 0, key).fill(adc.get(0, 0, key));
                H_WMAX.fill(key,1);
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
        if(this.realflag){
            if(H_WMAX.getBinContent(key)>this.getDetector().getThresholds().get(0, 0, key)) {
                if(H_TCROSS.getBinContent(key)>0) {
                   col = new Color(140, 0, 200);
                }
                else {
                  col = new Color(200, 0, 200);
             }
            }
           }
        else{
            //System.out.println("EVT "+this.realflag+"  "+key+"  "+H_WMAX.getBinContent(key));
            if(H_WMAX.getBinContent(key)>0)col = new Color(200, 0, 200);
            //else col = new Color(200, 0, 200);
        }
        return col;
    }
    
    
}
