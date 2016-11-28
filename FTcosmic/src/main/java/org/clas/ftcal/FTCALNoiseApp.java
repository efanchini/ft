/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.List;
import org.clas.tools.FTApplication;
import org.clas.tools.FTDetector;
import org.clas.tools.HipoFile;
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
    double[] noiseMEAN;
    double[] noiseRMS;
    
    // decoder related information
    int nProcessed = 0;

    public FTCALNoiseApp(FTDetector d) {
        super(d);
        this.setName("Noise");
        this.addCanvas("Noise");
        this.getCanvas("Noise").divideCanvas(2, 2);
        this.addFields("Status", "Pedestal Mean", "Pedestal RMS", "Noise", "Noise RMS");
        this.getParameter(0).setRanges(0.0,0.0,1.0,1.0);
        this.getParameter(1).setRanges(100.,300.,1.0,400.0);
        this.getParameter(2).setRanges(0.0,10.0,10.0,10.0);
        //this.getParameter(3).setRanges(1.0,1.5,1.0,2.0);//old preamps
        //this.getParameter(3).setRanges(1.0,1.05,1.0,2.0);//new preamps
        this.getParameter(3).setRanges(3.0,500.0,1.0,2.0);//Noise parameters //
        this.initCollections();
    }

    private void initCollections() {
        H_PED   = this.getData().addCollection(new H1D("Pedestal", 150, 0.0, 350.0),"Pedestal (fADC counts)","Counts",2,"H_PED");
        H_NOISE = this.getData().addCollection(new H1D("Noise", 150, 0.0, 350.0),"RMS (mV)","Counts",4,"H_NOISE"); 
        //OLD real cosmic data //
//        H_PED   = this.getData().addCollection(new H1D("Pedestal", 400, 100.0, 300.0),"Pedestal (fADC counts)","Counts",2,"H_PED");
//        H_NOISE = this.getData().addCollection(new H1D("Noise", 200, 0.0, 10.0),"RMS (mV)","Counts",4,"H_NOISE"); 
        pedestalMEAN    = new double[this.getDetector().getNComponents()];
        pedestalRMS     = new double[this.getDetector().getNComponents()];
        noiseRMS        = new double[this.getDetector().getNComponents()];
        noiseMEAN       = new double[this.getDetector().getNComponents()];
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
//                //for run609 //
//                if(key==9 || key==10 || key==31 || key==32 ||key==53 ||key==54 ||key==75 ||key==76 ||key==97 ||key==98 ||
//                    key==118 ||  key==119 || key==120 ||  key==140 ||key==141){
//                    this.getFitter().fitException(counter.getChannels().get(0), 4, 24, 60, 120);
//                }
//                else this.getFitter().fit(counter.getChannels().get(0),this.getDetector().getThresholds().get(0, 0, key));
                this.getFitter().fit(counter.getChannels().get(0),this.getDetector().getThresholds().get(0, 0, key)); //normale
                H_PED.get(0,0,key).fill(this.getFitter().getPedestal());
                H_NOISE.get(0, 0, key).fill(this.getFitter().getRMS());
            }
        }
    }
    
        public void addSimEvent(DetectorCollection<Double> adc) { 
        nProcessed++;
        for(int key : adc.getComponents(0, 0)) {
            if(adc.hasEntry(0, 0, key)){
             H_PED.get(0,0,key).fill(adc.get(0, 0, key));
             H_NOISE.get(0, 0, key).fill(adc.get(0, 0, key));
            }
        }
       
    }
    
    public void updateCanvas(int keySelect) {
        int ipointer=0;
        for(int key : this.getDetector().getDetectorComponents()) {
            pedestalMEAN[ipointer] = H_PED.get(0,0,key).getMean();
            pedestalRMS[ipointer]  = H_PED.get(0,0,key).getRMS();
            noiseMEAN[ipointer]    = H_NOISE.get(0, 0, key).getMean();
            noiseRMS[ipointer]     = H_NOISE.get(0, 0, key).getRMS();
            ipointer++;
        }
        GraphErrors  G_PED = new GraphErrors(detectorIDs,pedestalRMS);
        G_PED.setTitle(" "); //  title
        G_PED.setXTitle("Crystal ID"); // X axis title
        G_PED.setYTitle("Pedestal RMS (mV)");   // Y axis title
        G_PED.setMarkerColor(2); // color from 0-9 for given palette
        G_PED.setMarkerSize(5); // size in points on the screen
        G_PED.setMarkerStyle(1); // Style can be 1 or 2
        GraphErrors  G_NOISE = new GraphErrors(detectorIDs,noiseMEAN);
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
    
    public void saveToFile(HipoFile histofile) {
        histofile.addToMap("Noise_histo",    this.H_NOISE);
        histofile.addToMap("Pedestal_histo", this.H_PED);
    }
        
    @Override
    public double getFieldValue(int index, int key) {
        double value = -1;
        if(this.getDetector().hasComponent(key)) {
            switch (index) {
                case 0: 
                {
                    //System.out.println("ERICA NOISE: "+key+"  "+this.H_NOISE.get(0, 0, key).getMean()+"  "+this.getParameter(3).getParMin()+" "+this.getParameter(3).getParMax());
                    if     (this.getParameter(3).isValid(this.H_NOISE.get(0, 0, key).getMean())){
                        value=0;
                    }//ok
                    else if(this.getParameter(3).isLow(this.H_NOISE.get(0, 0, key).getMean()) || this.H_NOISE.get(0, 0, key).getEntries()==0){
                        value=3;
                    }//dead
                    //else if(this.getParameter(3).isLow(this.H_NOISE.get(0, 0, key).getMean()) || this.H_NOISE.get(0, 0, key).getEntries()==0)   value=3;//dead
                    else if(this.getParameter(3).isHigh(this.H_NOISE.get(0, 0, key).getMean())){
                        value=1;
                    }//noisy
                    else   {
                        value=5;
                    }//other issue
                    break;
                }
                case 1: value = this.H_PED.get(0, 0, key).getMean(); 
                    break;
                case 2: value = this.H_PED.get(0, 0, key).getRMS();
                    break;
                case 3: value = this.H_NOISE.get(0, 0, key).getMean();
                    break;
                 case 4: value = this.H_NOISE.get(0, 0, key).getRMS();
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
                    int color =0;
                    col = this.getSelectedParameter().getColor(color, true);
                    if(this.getFieldValue(this.getButtonIndex(),key)==3){
                        color=-1;  
                        col = this.getSelectedParameter().getColor(color, true);
                    }
                    else if(this.getFieldValue(this.getButtonIndex(),key)==1){
                        color=1;
                        col = this.getSelectedParameter().getColor(color, true);
                    }
                    else if(this.getFieldValue(this.getButtonIndex(),key)==5){
                        col = new Color(255,255,0);
                    }
                }
            } 
        }
        return col;
    }
    
}

