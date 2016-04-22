/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import java.awt.Color;
import static java.lang.Math.max;
import java.util.List;
import org.clas.tools.CustomizeFit;
import org.clas.tools.FTApplication;
import org.clas.tools.FTDetector;
import org.clas.tools.HipoFile;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas12.detector.DetectorCounter;
import org.root.attr.ColorPalette;
import org.root.func.F1D;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;

/**
 *
 * @author devita
 */
public class FTCALCosmicApp extends FTApplication {

    // Data Collections
    DetectorCollection<H1D> H_fADC          = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_fADC   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_CHARGE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_VMAX   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_TCROSS = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_THALF  = new DetectorCollection<H1D>();
    DetectorCollection<F1D> F_ChargeLandau  = new DetectorCollection<F1D>();
    DetectorCollection<F1D> F_TimeGauss     = new DetectorCollection<F1D>();
    H1D hfADC          = null;
    H1D H_fADC_N       = null;
    H1D H_COSMIC_N     = null;
    H1D H_COSMIC_MEAN  = null;
    H1D H_COSMIC_SIGMA = null;
    H1D H_COSMIC_CHI2  = null;
    H1D H_TIME_MEAN  = null;
    H1D H_TIME_SIGMA = null;
    double[] detectorIDs;
    double[] timeCROSS;
    double[] timeHALF;

    
    // decoder related information
    int nProcessed = 0;
    
    // analysis realted info
    double nsPerSample=4;
    double LSB = 0.4884;
    int ncry_cosmic = 4;        // number of crystals above threshold in a column for cosmics selection

    CustomizeFit cfit = new CustomizeFit();
    
    public FTCALCosmicApp(FTDetector d) {
        super(d);
        this.setName("Energy");
        this.addCanvas("Energy");
        this.addCanvas("Time");
        this.getCanvas("Energy").divideCanvas(2, 2);
        this.getCanvas("Time").divideCanvas(2, 2);
        this.addFields("Occupancy", "<E>", "\u03C3(E)", "\u03C7\u00B2(E)", "<T>", "\u03C3(T)");
        this.getParameter(0).setRanges(0.,1000000.,1.,1000000.);
        this.getParameter(1).setRanges(5.,25.,10.,50.);
        this.getParameter(2).setRanges(0.,10.,10.,10.);
        this.getParameter(3).setRanges(0.,2.,10.,5.);
        this.getParameter(4).setRanges(0.,50.,10.,50.);
        this.getParameter(5).setRanges(0.,5.,10.,5.);
        this.initCollections();
    }

   
    private void initCollections() {
        H_fADC          = this.getData().addCollection(new H1D("fADC", 100, 0.0, 100.0),"H_fADC");
        H_COSMIC_fADC   = this.getData().addCollection(new H1D("fADC", 100, 0.0, 100.0),"fADC Sample","fADC Counts",3,"H_COSMIC_fADC");
        H_COSMIC_CHARGE = this.getData().addCollection(new H1D("Charge", 80, 0.0, 40.0),"Charge (pC)","Counts",2,"H_COSMIC_CHARGE");
        H_COSMIC_VMAX   = this.getData().addCollection(new H1D("VMax",   80, 0.0, 40.0), "Amplitude (mV)", "Counts", 2,"H_COSMIC_VMAX");
        H_COSMIC_TCROSS = this.getData().addCollection(new H1D("T_TRIG", 80, -20.0, 60.0), "Time (ns)", "Counts", 5, "H_COSMIC_TCROSS");
        H_COSMIC_THALF  = this.getData().addCollection(new H1D("T_HALF", 80, -20.0, 60.0), "Time (ns)", "Counts", 5,"H_COSMIC_THALF");
        F_ChargeLandau  = this.getData().addCollection(new F1D("landau", 0.0, 40.0),"Landau","F_ChargeLandau");
        F_TimeGauss     = this.getData().addCollection(new F1D("gaus", -20.0, 60.0),"Time","F_ChargeLandau");
        H_fADC_N       = new H1D("fADC"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_COSMIC_N     = new H1D("EVENT" , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_COSMIC_MEAN  = new H1D("MEAN"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_COSMIC_SIGMA = new H1D("SIGMA" , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_COSMIC_CHI2  = new H1D("CHI2"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_TIME_MEAN  = new H1D("TIME MEAN"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_TIME_SIGMA = new H1D("TIME SIGMA" , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        timeCROSS       = new double[this.getDetector().getNComponents()];
        timeHALF        = new double[this.getDetector().getNComponents()];
        detectorIDs     = new double[this.getDetector().getNComponents()];        
        for(int i=0; i< this.getDetector().getNComponents(); i++) {
            detectorIDs[i]=this.getDetector().getIDArray()[i]; 
        }
        
    }

    @Override
    public void resetCollections() {
        for (int component : this.getDetector().getDetectorComponents()) {
            H_fADC.get(0, 0, component).reset();
            H_COSMIC_fADC.get(0, 0, component).reset();
            H_COSMIC_CHARGE.get(0, 0, component).reset();
            H_COSMIC_VMAX.get(0, 0, component).reset();
            H_COSMIC_TCROSS.get(0, 0, component).reset();
            H_COSMIC_THALF.get(0, 0, component).reset();            
        }
        H_fADC_N.reset();
        H_COSMIC_N.reset();
        H_COSMIC_MEAN.reset();
        H_COSMIC_SIGMA.reset();
        H_COSMIC_CHI2.reset();
        H_TIME_MEAN.reset();
        H_TIME_SIGMA.reset();
    }
    
    private void initLandauFitPar(int key) {
        H1D hcosmic = H_COSMIC_CHARGE.get(0,0,key);
        double mlMin=hcosmic.getAxis().min();
        double mlMax=hcosmic.getAxis().max();
        mlMin=1.0;
        F1D ff;
        if(hcosmic.getBinContent(0)==0){
            ff = new F1D("landau",     mlMin, mlMax);
            ff.setName("Landau_"+key);
            F_ChargeLandau.add(0, 0, key, ff);
        }
        else{
            ff = new F1D("landau+exp", mlMin, mlMax);
            ff.setName("Landau_"+key);
            F_ChargeLandau.add(0, 0, key, ff);
        }
          
        if(hcosmic.getBinContent(0)<20) {//Changed from 10
            F_ChargeLandau.get(0, 0, key).setParameter(0, hcosmic.getBinContent(hcosmic.getMaximumBin()));
        }
        else {
            F_ChargeLandau.get(0, 0, key).setParameter(0, 20);//Changed from 10
        }
        F_ChargeLandau.get(0, 0, key).setParLimits(0, 0.0, 10000000.); 
        F_ChargeLandau.get(0, 0, key).setParameter(1,hcosmic.getMean());
        F_ChargeLandau.get(0, 0, key).setParLimits(1, 4.0, 30.);//Changed from 5-30        
        F_ChargeLandau.get(0, 0, key).setParameter(2,1.6);//Changed from 2
        F_ChargeLandau.get(0, 0, key).setParLimits(2, 0.3, 5);//Changed from 0.5-10
        if(hcosmic.getBinContent(0)!=0) {
            F_ChargeLandau.get(0, 0, key).setParameter(3,hcosmic.getBinContent(0));
            F_ChargeLandau.get(0, 0, key).setParLimits(3,  0.0, 10000000.); 
            F_ChargeLandau.get(0, 0, key).setParameter(4, -0.3);//Changed from -0.2
            F_ChargeLandau.get(0, 0, key).setParLimits(4, -3, 0.0); //Changed from -10-0
        }
        
    }
    
    private void fitLandau(int key) {
        H1D hcosmic = H_COSMIC_CHARGE.get(0,0,key);
        String name = F_ChargeLandau.get(0, 0, key).getName();
        if(name.indexOf("New_")==-1)initLandauFitPar(key);
        hcosmic.fit(F_ChargeLandau.get(0, 0, key),"LQ");
        this.updateChargeFitResults(key);
    }
    

    private void initTimeGaussFitPar(int key, H1D htime) {
        double hAmp  = htime.getBinContent(htime.getMaximumBin());
        double hMean = htime.getAxis().getBinCenter(htime.getMaximumBin());
        double hRMS  = htime.getRMS();        
        double rangeMin = hMean - 3*hRMS; 
        double rangeMax = hMean + 3*hRMS;     
        F_TimeGauss.add(0, 0, key, new F1D("gaus", rangeMin, rangeMax));
        F_TimeGauss.get(0, 0, key).setName("Gaus_"+key);
        F_TimeGauss.get(0, 0, key).setParameter(0, hAmp);
        F_TimeGauss.get(0, 0, key).setParLimits(0, hAmp*0.8, hAmp*1.2);
        F_TimeGauss.get(0, 0, key).setParameter(1, hMean);       
        F_TimeGauss.get(0, 0, key).setParameter(2, 2.);
        F_TimeGauss.get(0, 0, key).setParLimits(2, 0.2, 10);
    }    

    private void fitTime(int key) {
        H1D htime = H_COSMIC_THALF.get(0,0,key);
        String name =F_TimeGauss.get(0, 0, key).getName();
        if(name.indexOf("New_")==-1)initTimeGaussFitPar(key,htime);
        htime.fit(F_TimeGauss.get(0, 0, key),"NQ");
        this.updateTimeFitResults(key);
    }
    
    @Override
    public void fitCollections() {
        for(int key : H_COSMIC_CHARGE.getComponents(0, 0)) {
            if(H_COSMIC_CHARGE.get(0, 0, key).getEntries()>100) {
                //System.out.println("Fitting charge histos for component: " + key);
                this.fitLandau(key);
            }
            //else System.out.println("Skipping charge fit of component: " + key + 
            //                        ", only " + H_COSMIC_CHARGE.get(0, 0, key).getEntries() + " events");
            if(H_COSMIC_THALF.get(0,0,key).getEntries()>0) {        
                this.fitTime(key);
            }   
        }
        if(     this.getCanvasSelect() == "Energy") this.fitBook(H_COSMIC_CHARGE,F_ChargeLandau);
        else if(this.getCanvasSelect() == "Time")   this.fitBook(H_COSMIC_THALF,F_TimeGauss);
        
    }
    
     public void fitFastCollections() {
        for(int key : H_COSMIC_CHARGE.getComponents(0, 0)) {
            if(H_COSMIC_CHARGE.get(0, 0, key).getEntries()>0 && F_ChargeLandau.get(0, 0, key).getName()=="f1") {
                this.fitLandau(key);
            }
            if(H_COSMIC_THALF.get(0,0,key).getEntries()>0 && F_TimeGauss.get(0, 0, key).getName()=="f1") {   
                this.fitTime(key);
            }   
        }
        
    }
      
    private void updateChargeFitResults(int key){
        H_COSMIC_MEAN.setBinContent(key, F_ChargeLandau.get(0, 0, key).getParameter(1));
        H_COSMIC_SIGMA.setBinContent(key, F_ChargeLandau.get(0, 0, key).getParameter(2));
        H_COSMIC_CHI2.setBinContent(key, F_ChargeLandau.get(0, 0, key).getChiSquare(H_COSMIC_CHARGE.get(0,0,key),"NR")
                                        /F_ChargeLandau.get(0, 0, key).getNDF(H_COSMIC_CHARGE.get(0,0,key).getDataSet()));
    }

    private void updateTimeFitResults(int key){
        H_TIME_MEAN.setBinContent(key, F_TimeGauss.get(0, 0, key).getParameter(1));
        H_TIME_SIGMA.setBinContent(key, F_TimeGauss.get(0, 0, key).getParameter(2));
    }

    @Override
    public void customizeFit(int key){     
        if(this.getCanvasSelect() == "Energy") {
            cfit.FitPanel(H_COSMIC_CHARGE.get(0,0,key), F_ChargeLandau.get(0,0,key),"LQ");
            this.getCanvas("Energy").update();
            this.updateChargeFitResults(key);
            
        }
        else if(this.getCanvasSelect() == "Time") {
            cfit.FitPanel(H_COSMIC_THALF.get(0,0,key), F_TimeGauss.get(0,0,key),"NQ");
            this.getCanvas("Time").update();            
            this.updateTimeFitResults(key);
        }
    }

    public void addEvent(List<DetectorCounter> counters) { 
        nProcessed++;
        H1D H_WMAX = new H1D("WMAX",this.getDetector().getComponentMaxCount(),0.,this.getDetector().getComponentMaxCount());
        H_WMAX.reset();
        double tPMTCross=0;
        double tPMTHalf=0;
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            if(this.getDetector().hasComponent(key)) {
                this.getFitter().fit(counter.getChannels().get(0),this.getDetector().getThresholds().get(0, 0, key));                
                H_fADC_N.fill(key);
                short pulse[] = counter.getChannels().get(0).getPulse();
                for (int i = 0; i < Math.min(pulse.length, H_fADC.get(0, 0, key).getAxis().getNBins()); i++) {
                    H_fADC.get(0, 0, key).fill(i, pulse[i] - this.getFitter().getPedestal() + 10.0);
                }
                H_WMAX.fill(key,this.getFitter().getWave_Max()-this.getFitter().getPedestal());
                if(key==501) {      // top long PMT
                    tPMTCross = this.getFitter().getTime(3);
                    tPMTHalf  = this.getFitter().getTime(7);
                }    
            }
        }
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            if(this.getDetector().hasComponent(key)) {
                int ix  = this.getDetector().getIX(key);
                int iy  = this.getDetector().getIY(key);
                int nCrystalInColumn = 0;
                this.getFitter().fit(counter.getChannels().get(0),this.getDetector().getThresholds().get(0, 0, key));
                int i1=(int) max(0,iy-ncry_cosmic);    // allowing for +/- to cope with dead channels
                int i2=iy+ncry_cosmic;
                for(int i=i1; i<=i2; i++) {
                    int component = this.getDetector().getComponent(ix, i);
                    if(i!=iy && this.getDetector().hasComponent(component)) {
                        if(H_WMAX.getBinContent(component)>this.getDetector().getThresholds().get(0, 0, component) ) nCrystalInColumn++;                    
                    }
                }
                if(nCrystalInColumn>=ncry_cosmic) {
                    short pulse[] = counter.getChannels().get(0).getPulse();
                    H_COSMIC_N.fill(key);
                    for (int i = 0; i < Math.min(pulse.length, H_COSMIC_fADC.get(0, 0, key).getAxis().getNBins()); i++) {
                        H_COSMIC_fADC.get(0, 0, key).fill(i, pulse[i]-this.getFitter().getPedestal() + 10.0);                
                    }
                    double charge=(counter.getChannels().get(0).getADC().get(0)*LSB*nsPerSample/50);
                    H_COSMIC_CHARGE.get(0, 0, key).fill(charge);
                    H_COSMIC_VMAX.get(0, 0, key).fill((this.getFitter().getWave_Max()-this.getFitter().getPedestal())*LSB);
                    H_COSMIC_TCROSS.get(0, 0, key).fill(this.getFitter().getTime(3)-tPMTCross);
                    H_COSMIC_THALF.get(0, 0, key).fill(this.getFitter().getTime(7)-tPMTHalf);                
                }
            }
        }
    }
    
    public void updateCanvas(int keySelect) {
        // Energy - Cosmics for now
        this.getCanvas("Energy").cd(0);
        if (H_fADC.hasEntry(0, 0, keySelect)) {
            hfADC = H_fADC.get(0, 0, keySelect).histClone(" ");
            hfADC.normalize(H_fADC_N.getBinContent(keySelect));
            hfADC.setFillColor(3);
            hfADC.setXTitle("fADC Sample");
            hfADC.setYTitle("fADC Counts");
            this.getCanvas("Energy").draw(hfADC);               
        }
        this.getCanvas("Energy").cd(1);
        if (H_COSMIC_fADC.hasEntry(0, 0, keySelect)) {
            hfADC = H_COSMIC_fADC.get(0, 0, keySelect).histClone(" ");
            hfADC.normalize(H_COSMIC_N.getBinContent(keySelect));
            hfADC.setFillColor(3);
            hfADC.setXTitle("fADC Sample");
            hfADC.setYTitle("fADC Counts");
            this.getCanvas("Energy").draw(hfADC);               
        }
        this.getCanvas("Energy").cd(2);
        if(H_COSMIC_VMAX.hasEntry(0, 0, keySelect)) {
            H1D hcosmic = H_COSMIC_VMAX.get(0,0,keySelect);
            this.getCanvas("Energy").draw(hcosmic,"S");
        }
        this.getCanvas("Energy").cd(3);
        if(H_COSMIC_CHARGE.hasEntry(0, 0, keySelect)) {
            H1D hcosmic = H_COSMIC_CHARGE.get(0,0,keySelect);
            this.getCanvas("Energy").draw(hcosmic,"S");
            if(hcosmic.getEntries()>0) {
                fitLandau(keySelect);
                this.getCanvas("Energy").draw(F_ChargeLandau.get(0, 0, keySelect),"sameS");
            }
        }     
         // Time
        int ipointer = 0;
        for(int key : this.getDetector().getDetectorComponents()) {
            timeCROSS[ipointer]    = H_COSMIC_TCROSS.get(0, 0, key).getMean();
            timeHALF[ipointer]     = H_COSMIC_THALF.get(0, 0, key).getMean();
            ipointer++;
        }
        GraphErrors  G_TCROSS = new GraphErrors(detectorIDs,timeCROSS);
        G_TCROSS.setTitle(" "); //  title
        G_TCROSS.setXTitle("Crystal ID"); // X axis title
        G_TCROSS.setYTitle("Trigger Time (ns)");   // Y axis title
        G_TCROSS.setMarkerColor(5); // color from 0-9 for given palette
        G_TCROSS.setMarkerSize(5);  // size in points on the screen
        G_TCROSS.setMarkerStyle(1); // Style can be 1 or 2
        GraphErrors  G_THALF = new GraphErrors(detectorIDs,timeHALF);
        G_THALF.setTitle(" "); //  title
        G_THALF.setXTitle("Crystal ID"); // X axis title
        G_THALF.setYTitle("Mode-7 Time (ns)");   // Y axis title
        G_THALF.setMarkerColor(5); // color from 0-9 for given palette
        G_THALF.setMarkerSize(5);  // size in points on the screen
        G_THALF.setMarkerStyle(1); // Style can be 1 or 2
        this.getCanvas("Time").cd(0);
        this.getCanvas("Time").draw(G_TCROSS);
        this.getCanvas("Time").cd(1);
        this.getCanvas("Time").draw(G_THALF);
        this.getCanvas("Time").cd(2);
        if(H_COSMIC_TCROSS.hasEntry(0, 0, keySelect)) {
            H1D htime = H_COSMIC_TCROSS.get(0, 0, keySelect);
            this.getCanvas("Time").draw(htime,"S");
        }
        this.getCanvas("Time").cd(3);
        if(H_COSMIC_THALF.hasEntry(0, 0, keySelect)) {
            H1D htime = H_COSMIC_THALF.get(0, 0, keySelect);
            this.getCanvas("Time").draw(htime,"S");
            if(htime.getEntries()>0) {
                fitTime(keySelect);
                this.getCanvas("Time").draw(F_TimeGauss.get(0, 0, keySelect),"sameS");
            }
        }
    }
    
    @Override
    public double getFieldValue(int index, int key) {
        double value = -1;
        if(this.getDetector().hasComponent(key)) {
            switch (index) {
                case 0: value = this.H_COSMIC_CHARGE.get(0, 0, key).getEntries();
                    break;
                case 1: value = this.H_COSMIC_MEAN.getBinContent(key); 
                    break;
                case 2: value = this.H_COSMIC_SIGMA.getBinContent(key);
                    break;
                case 3: value = this.H_COSMIC_CHI2.getBinContent(key);
                    break;
                case 4: value = this.H_TIME_MEAN.getBinContent(key); 
                    break;
                case 5: value = this.H_TIME_SIGMA.getBinContent(key);
                    break;
                default: value =-1;
                    break;
            }
        }
        return value;
    }
    
    @Override
    public Color getColor(int key) {
        ColorPalette palette = new ColorPalette();
        Color col = new Color(100, 100, 100);
        if (this.getDetector().hasComponent(key)) {
            int nent = this.H_COSMIC_CHARGE.get(0, 0, key).getEntries();
            if (nent > 0) {
                if(this.getButtonIndex()!=0) {
                    col = this.getSelectedParameter().getColor(this.getFieldValue(this.getButtonIndex(),key), false);
                }
                else {
                    col = palette.getColor3D(nent, nProcessed, true);
                }
            } 
        }
        return col;
    }
    
    @Override
    public void saveToFile(String hipoFileName) {
        HipoFile histofile = new HipoFile(hipoFileName);
        histofile.addToMap("Energy_histo", this.H_COSMIC_CHARGE);
        histofile.addToMap("Energy_fct", this.F_ChargeLandau);
        histofile.addToMap("Noise_histo", this.H_COSMIC_THALF);
        histofile.addToMap("Noise_fct", this.F_TimeGauss);
        histofile.writeHipoFile(hipoFileName);
        //histofile.browsFile(hipoFileName);
        
        
    }
}
