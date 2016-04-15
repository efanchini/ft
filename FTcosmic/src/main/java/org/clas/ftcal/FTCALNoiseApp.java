/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.List;
import org.clas.tools.ExtendedFADCFitter;
import org.clas.tools.FTApplication;
import org.clas.tools.FTDetector;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas12.detector.DetectorCounter;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;

/**
 *
 * @author devita
 */
public class FTCALNoiseApp extends FTApplication implements ActionListener {
    
    // Data Collection
    DetectorCollection<H1D> H_PED   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NOISE = new DetectorCollection<H1D>();
    double[] detectorIDs;
    double[] pedestalMEAN;
    double[] pedestalRMS;
    double[] noiseRMS;
    
    // decoder related information
    int nProcessed = 0;

    public FTCALNoiseApp(FTDetector d) {
        super(d);
        this.setName("Noise");
        this.addCanvas("Noise");
        this.getCanvas("Noise").divideCanvas(2, 2);
        this.addFields("Status", "Pedestal Mean", "Pedestal RMS", "Noise");
        this.getParameter(0).setRanges(0.,0.,1.,1.);
        this.getParameter(1).setRanges(100.,300.,1.,400.);
        this.getParameter(2).setRanges(0.,10.,10.,10.);
        this.getParameter(3).setRanges(1.,1.5,1.,2.);
        this.initCollections();
    }

    private void initCollections() {
        H_PED   = this.getData().addCollection(new H1D("Pedestal", 400, 100., 300.0),"Pedestal (fADC counts)","Counts",2);
        H_NOISE = this.getData().addCollection(new H1D("Noise", 200, 0.0, 10.0),"RMS (mV)","Counts",4); 
        pedestalMEAN    = new double[this.getDetector().getNComponents()];
        pedestalRMS     = new double[this.getDetector().getNComponents()];
        noiseRMS        = new double[this.getDetector().getNComponents()];
        detectorIDs     = new double[this.getDetector().getNComponents()];        
        for(int i=0; i< this.getDetector().getNComponents(); i++) {
            detectorIDs[i]=this.getDetector().getIDArray()[i]; 
        }
    }
    
    @Override
    public void resetCollections() {
        for (int component : this.getDetector().getDetectorComponents()) {
            H_PED.get(0, 0, component).reset();
            H_NOISE.get(0, 0, component).reset();
        }
    }
    
    public void addEvent(List<DetectorCounter> counters) {        
        nProcessed++;
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            if(H_PED.hasEntry(0, 0, key)) {
                this.getFitter().fit(counter.getChannels().get(0),this.getDetector().getThresholds().get(0, 0, key));
                H_PED.get(0,0,key).fill(this.getFitter().getPedestal());
                H_NOISE.get(0, 0, key).fill(this.getFitter().getRMS());
            }
        }
    }
    
    public void updateCanvas(int keySelect) {
        int ipointer=0;
        for(int key : this.getDetector().getDetectorComponents()) {
            pedestalMEAN[ipointer] = H_PED.get(0,0,key).getMean();
            pedestalRMS[ipointer]  = H_PED.get(0,0,key).getRMS();
            noiseRMS[ipointer]     = H_NOISE.get(0, 0, key).getMean();
            ipointer++;
        }
        GraphErrors  G_PED = new GraphErrors(detectorIDs,pedestalRMS);
        G_PED.setTitle(" "); //  title
        G_PED.setXTitle("Crystal ID"); // X axis title
        G_PED.setYTitle("Pedestal RMS (mV)");   // Y axis title
        G_PED.setMarkerColor(2); // color from 0-9 for given palette
        G_PED.setMarkerSize(5); // size in points on the screen
        G_PED.setMarkerStyle(1); // Style can be 1 or 2
        GraphErrors  G_NOISE = new GraphErrors(detectorIDs,noiseRMS);
        G_NOISE.setTitle(" "); //  title
        G_NOISE.setXTitle("Crystal ID"); // X axis title
        G_NOISE.setYTitle("Noise (mV)");   // Y axis title
        G_NOISE.setMarkerColor(4); // color from 0-9 for given palette
        G_NOISE.setMarkerSize(5); // size in points on the screen
        G_NOISE.setMarkerStyle(1); // Style can be 1 or 2
        this.getCanvas(0).cd(0);
        this.getCanvas(0).draw(G_PED);
        this.getCanvas(0).cd(1);
        this.getCanvas(0).draw(G_NOISE);
        if (H_NOISE.hasEntry(0, 0, keySelect)) {
            H1D hnoise = H_NOISE.get(0, 0, keySelect);
            H1D hped   = H_PED.get(0, 0, keySelect);
            this.getCanvas(0).cd(2);
            this.getCanvas(0).draw(hped,"S");
            this.getCanvas(0).cd(3);
            this.getCanvas(0).draw(hnoise,"S");
        }
    }
        
    @Override
    public double getFieldValue(int index, int key) {
        double value = -1;
        if(this.getDetector().hasComponent(key)) {
            switch (index) {
                case 0: 
                {
                    if     (this.getParameter(3).isValid(this.H_NOISE.get(0, 0, key).getMean())) value=0;
                    else if(this.getParameter(3).isLow(this.H_NOISE.get(0, 0, key).getMean()))   value=-1;
                    else    value=1;
                    break;
                }
                case 1: value = this.H_PED.get(0, 0, key).getMean(); 
                    break;
                case 2: value = this.H_PED.get(0, 0, key).getRMS();
                    break;
                case 3: value = this.H_NOISE.get(0, 0, key).getMean();
                    break;
                default: value =-1;
                    break;
            }
        }
        return value;
    }
    
    
    @Override
    public Color getColor(int key) {
        Color col = new Color(100, 100, 100);
        if (this.H_NOISE.hasEntry(0, 0, key)) {
            int nent = this.H_NOISE.get(0, 0, key).getEntries();
            if (nent > 0) {
                if(this.getButtonIndex()!=0) 
                    col = this.getSelectedParameter().getColor(this.getFieldValue(this.getButtonIndex(),key), false);
                else {
                    col = this.getSelectedParameter().getColor(this.getFieldValue(this.getButtonIndex(),key), true);
                }
            } 
        }
        return col;
    }
    
}
