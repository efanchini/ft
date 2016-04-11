/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import org.clas.tools.ExtendedFADCFitter;
import org.clas.tools.FTApplication;
import org.clas.tools.FTDetector;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas12.detector.DetectorCounter;
import org.root.attr.ColorPalette;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;

/**
 *
 * @author devita
 */
public class FTCALNoise extends FTApplication implements ActionListener {
    
    // Data Collection
    DetectorCollection<H1D> H_PED  = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NOISE = new DetectorCollection<H1D>();
    double[] detectorIDs;
    double[] pedestalMEAN;
    double[] pedestalRMS;
    double[] noiseRMS;
    
    // decoder related information
    int nProcessed = 0;
    ExtendedFADCFitter eFADCFitter = new ExtendedFADCFitter();
    
    // color palette
    ColorPalette palette = new ColorPalette();
    

    public FTCALNoise(FTDetector d) {
        super(d);
        this.setName("Noise");
        this.getCanvas().divideCanvas(2, 2);
        this.addRadioButtons("Status", "Pedestal Mean", "Pedestal RMS", "Noise");
        this.addHashTable("Status:i", "Pedestal Mean:d", "Pedestal RMS:d", "Noise:d");
        this.getTable().addConstrain(4,160.0, 240.0);
        this.getTable().addConstrain(6, 1.0, 1.5); 
        this.initCollections();
    }

    private void initCollections() {
        H_PED   = this.getData().addCollection(new H1D("Pedestal", 400, 100., 300.0),"Pedestal (fADC counts)","Counts",2);
        H_NOISE = this.getData().addCollection(new H1D("Noise", 200, 0.0, 10.0),"RMS (mV)","Counts",4); 
        pedestalMEAN    = new double[this.getDetector().getNComponents()];
        pedestalRMS     = new double[this.getDetector().getNComponents()];
        noiseRMS        = new double[this.getDetector().getNComponents()];
        System.out.println(this.getDetector().getNComponents());
        detectorIDs       = new double[this.getDetector().getNComponents()];        
        for(int i=0; i< this.getDetector().getNComponents(); i++) {
            detectorIDs[i]=this.getDetector().getIDArray()[i]; 
        }
    }
    
    public void addEvent(List<DetectorCounter> counters) {
        
        nProcessed++;
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            if(H_PED.hasEntry(0, 0, key)) {
                eFADCFitter.fit(counter.getChannels().get(0),this.getDetector().getThresholds().get(0, 0, key));
                H_PED.get(0, 0, key).fill(eFADCFitter.getPedestal());
                H_NOISE.get(0, 0, key).fill(eFADCFitter.getRMS());
            }
        }
    }

    public ExtendedFADCFitter getFitter() {
        return eFADCFitter;
    }
    
    public void setFitter(ExtendedFADCFitter efitter) {
        this.eFADCFitter = efitter;
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
        this.getCanvas().cd(0);
        this.getCanvas().draw(G_PED);
        this.getCanvas().cd(1);
        this.getCanvas().draw(G_NOISE);
        if (H_NOISE.hasEntry(0, 0, keySelect)) {
            H1D hnoise = H_NOISE.get(0, 0, keySelect);
            H1D hped   = H_PED.get(0, 0, keySelect);
            this.getCanvas().cd(2);
            this.getCanvas().draw(hped,"S");
            this.getCanvas().cd(3);
            this.getCanvas().draw(hnoise,"S");
        }
    }
        
    
    @Override
    public Color getColor(int key) {
        Color col = new Color(100, 100, 100);
        if (this.H_NOISE.hasEntry(0, 0, key)) {
            int nent = this.H_NOISE.get(0, 0, key).getEntries();
            if (nent > 0) {
                this.getTable().setValueAtAsDouble(1, this.H_PED.get(0, 0, key).getMean()  , 0, 0, key);
                this.getTable().setValueAtAsDouble(2, this.H_PED.get(0, 0, key).getRMS()   , 0, 0, key);
                this.getTable().setValueAtAsDouble(3, this.H_NOISE.get(0, 0, key).getMean(), 0, 0, key);
                if(this.getButtonSelect() == "Pedestal Mean") {
                    double cvalue = this.H_PED.get(0, 0, key).getMean();
                    col = palette.getColor3D(cvalue, 400., true);           
                }
                else if(this.getButtonSelect() == "Pedestal RMS") {
                    double cvalue = 10*this.H_PED.get(0, 0, key).getRMS();
                    col = palette.getColor3D(cvalue, 100., true);           
                }
                else if(this.getButtonSelect() == "Noise") {
                    double cvalue = this.H_NOISE.get(0, 0, key).getMean();
                    col = palette.getColor3D(cvalue, 2., true);           
                }
                else {
                    if(this.H_NOISE.get(0, 0, key).getMean()>=1. && this.H_NOISE.get(0, 0, key).getMean()<=1.5) {
                        col = new Color(0, 145, 0);
                    } else if (this.H_NOISE.get(0, 0, key).getMean() < 1.0) {
                        col = new Color(0, 0, 100);
                    } else {
                        col = new Color(255, 100, 0);
                    }
                }
            } 
        }
        return col;
    }

    
}

