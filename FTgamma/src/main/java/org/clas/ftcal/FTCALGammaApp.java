/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import java.awt.Color;
import java.io.IOException;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;
import org.clas.tools.CustomizeFit;
import org.clas.tools.FTApplication;
import org.clas.tools.FTDetector;
import org.clas.tools.FitParametersFile;
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
public class FTCALGammaApp extends FTApplication {

    // Data Collections
    DetectorCollection<H1D>    H_SIM_fADC   = new DetectorCollection<H1D>();
    DetectorCollection<H1D>    H_SIM_CHARGE = new DetectorCollection<H1D>();
    DetectorCollection<H1D>    H_SIM_VMAX   = new DetectorCollection<H1D>();
    DetectorCollection<H1D>    H_SIM_TCROSS = new DetectorCollection<H1D>();
    DetectorCollection<H1D>    H_SIM_THALF  = new DetectorCollection<H1D>();
    DetectorCollection<F1D>    F_ChargeLandau  = new DetectorCollection<F1D>();
    DetectorCollection<F1D>    F_TimeGauss     = new DetectorCollection<F1D>();
    DetectorCollection<Double> fvalues         = new DetectorCollection<Double>();//da togliere
    
    H1D hfADC          = null;
    H1D H_fADC_N       = null;
    H1D H_SIM_N        = null;
    H1D H_SIM_MEAN     = null;
    H1D H_SIM_SIGMA    = null;
    H1D H_SIM_CHI2     = null;
    H1D H_TIME_MEAN    = null;
    H1D H_TIME_SIGMA   = null;
    H1D ht_mean        = null;
    F1D F_time_mean    = null; 
    H1D H_fADC         = null;
    
    double[] detectorIDs;
    double[] timeCROSS;
    double[] timeHALF;
    
    
    String compcanvas="";
    private int SelectionType=1;// da cambiare
    Boolean simflag=false;
    
    // decoder related information
    int nProcessed = 0;
    
    // analysis realted info
    double nsPerSample=4;
    double LSB = 0.4884;
    double clusterThr = 50.0;// Vertical selection
    double singleChThr = 0.00;// Single channel selection MeV
    double signalThr =0.0;
    double simSignalThr=0.00;// Threshold used for simulated events in MeV
    double startTime   = 124.25;//ns
    double ftcalDistance =1.898; //meters
    double timeshift =0;// ns
    double crystal_size = 15;//mm
    double charge2e = 15.3/6.005; //MeV
    double crystal_length = 200;//mm                                                                                            
    double shower_depth = 45;                                                                                                   
    double light_speed = 150; //cm/ns     
    CustomizeFit cfit = new CustomizeFit();
    FTCalGammaSelection gammasel;
    private ArrayList selOpt;
    
    public FTCALGammaApp(FTDetector d) throws IOException {
        super(d);
        this.setName("Energy");
        this.addCanvas("Energy");
        this.addCanvas("Time");
        this.getCanvas("Energy").divideCanvas(2, 2);
        this.getCanvas("Time").divideCanvas(2, 2);
        this.addFields("Occupancy", "<E>", "\u03C3(E)", "\u03C7\u00B2(E)", "<T>", "\u03C3(T)","Threshold");
        this.getParameter(0).setRanges(0,1000000,1,1000000);   //  occuopancy
        this.getParameter(1).setRanges(0.5,12000.0,10.0,11000.0);//  Range Energy //da sistemare
        this.getParameter(2).setRanges(0.0,1000.0,10.0,1000.0);    //  Sigma energy // da sistemare
        this.getParameter(3).setRanges(0.0,2.0,10.0,5.0);      //  Chi quadro //da sistemare
        this.getParameter(4).setRanges(-0.0,1.2,1.0,1.2);      //  <T>
        this.getParameter(5).setRanges(0.0,5.0,10.0,5.0);      //  RMS<T>
        this.getParameter(6).setRanges(0.0,2000.0,1.0,2000.0);  //  threshold
        this.initCollections();
    }

    private void selInfo(){
       switch (SelectionType) {
            case 0:
                // Single Signal Above Threshold
                System.out.println("Cosmic event selection set: Single signal above threshold => ThrCh: "+this.singleChThr+" MeV");
                this.signalThr = this.singleChThr*50.0/(this.LSB*this.nsPerSample*charge2e);
                break;
           case 1:
                // Cosmic selection for simulated events in integral mode
                System.out.println("Cosmic event selection set with simulated events: Single signal above threshold => ThrCh: "+this.simSignalThr+" MeV");
                this.simSignalThr = this.simSignalThr*50.0/(this.LSB*this.nsPerSample*charge2e);              
                break;                
       }
    }
    
    public void LoadSelection(ArrayList arl) {
        this.selOpt=arl;
        this.SelectionType = (Integer)this.selOpt.get(0);
        switch (SelectionType) {
            case 0:
                // Single Signal Above Threshold
                this.singleChThr = (Double)this.selOpt.get(1);
                break;
            case 1:
                // Single Signal Above Threshold for simulated events        
                this.simSignalThr = (Double)this.selOpt.get(1);
                break;                
        } 
   }
    
  public Object getDefaultSelPar(int seltype, int npar) {
        Object objout= null;
        switch (seltype) {
            case 0:
                // Single Signal Above Threshold
                if(npar==1)objout=this.singleChThr;
                else objout=-1;
                break;
            case 1:
                // Single Signal Above Threshold simulated events
                if(npar==1)objout=this.simSignalThr;
                else objout=-1;
                break;               
        }
        return objout;
       
   }
    private void initCollections() throws IOException {

        H_fADC          = new H1D("fADC", 120, 0.0, 12.0);// Histo with Max energy evt
        H_SIM_fADC      = this.getData().addCollection(new H1D("fADC", 100, 0.0, 100.0),"fADC Sample","fADC Counts",3,"H_SIM_fADC");
        ht_mean         = new H1D("H_TimeMean",512,-10.0,15.0);
        F_time_mean     = new F1D("gaus",-10.0, 30.0);
        H_SIM_CHARGE    = this.getData().addCollection(new H1D("Charge", 200, 0.0, 2000.0),"Energy (MeV)","Counts",2,"H_SIM_CHARGE");
        H_SIM_VMAX      = this.getData().addCollection(new H1D("VMax",   120, 0.0, 12.0), "Energy (GeV)", "Counts", 2,"H_SIM_VMAX");
        F_ChargeLandau  = this.getData().addCollection(new F1D("landau", 0.0, 300.0),"Landau","F_ChargeLandau");
        
        if(this.SelectionType==1){//Simulated data
            H_SIM_TCROSS  = this.getData().addCollection(new H1D("T_TRIG",330, -30.0, 80.0), "Time (ns)", "Counts", 5, "H_SIM_TCROSS");
            H_SIM_THALF   = this.getData().addCollection(new H1D("T_HALF", 270, -15.0, 15.0), "Time (ns)", "Counts", 5,"H_SIM_THALF");
         
        }
        else{
           H_SIM_TCROSS = this.getData().addCollection(new H1D("T_TRIG", 220, -20.0, 200.0), "Time (ns)", "Counts", 5, "H_SIM_TCROSS");
           H_SIM_THALF  = this.getData().addCollection(new H1D("T_HALF", 220, -20.0, 200.0), "Time (ns)", "Counts", 5,"H_SIM_THALF");
      
        }
        F_TimeGauss     = this.getData().addCollection(new F1D("gaus", -10.0,400.0),"Time","F_TimeGaus");
        H_fADC_N        = new H1D("fADC"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_SIM_N         = new H1D("EVENT" , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_SIM_MEAN      = new H1D("MEAN"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_SIM_SIGMA     = new H1D("SIGMA" , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_SIM_CHI2      = new H1D("CHI2"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_TIME_MEAN     = new H1D("TIME MEAN"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_TIME_SIGMA    = new H1D("TIME SIGMA" , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        timeCROSS       = new double[this.getDetector().getNComponents()];
        timeHALF        = new double[this.getDetector().getNComponents()];
        detectorIDs     = new double[this.getDetector().getNComponents()]; 
        
        for(int i=0; i< this.getDetector().getNComponents(); i++) {
            detectorIDs[i]=this.getDetector().getIDArray()[i]; 
        }
        gammasel = new FTCalGammaSelection(this.getDetector());
        
        //test
        //FitParametersFile ff = new FitParametersFile(); 
        //ff.readCCDBFiles();
    }

    public void reloadCollections(){
        H_SIM_CHARGE.clear();
        H_SIM_VMAX.clear();     
        H_SIM_CHARGE    = this.getData().addCollection(new H1D("Charge", 200, 0.0, 2000.0),"Energy (MeV)","Counts",2,"H_SIM_CHARGE");
        H_SIM_VMAX      = this.getData().addCollection(new H1D("VMax",   120, 0.0, 12.0), "Energy (GeV)", "Counts", 2,"H_SIM_VMAX");
        F_ChargeLandau  = this.getData().addCollection(new F1D("landau", 0.0, 60.0),"Landau","F_ChargeLandau");
    }
    
    
    @Override
    public void resetCollections() {
        for (int component : this.getDetector().getDetectorComponents()) {
            H_SIM_fADC.get(0, 0, component).reset();
            H_SIM_CHARGE.get(0, 0, component).reset();
            H_SIM_VMAX.get(0, 0, component).reset();
            H_SIM_TCROSS.get(0, 0, component).reset();
            H_SIM_THALF.get(0, 0, component).reset();       
        }
        H_fADC.reset();
        H_fADC_N.reset();
        H_SIM_N.reset();
        H_SIM_MEAN.reset();
        H_SIM_SIGMA.reset();
        H_SIM_CHI2.reset();
        H_TIME_MEAN.reset();
        H_TIME_SIGMA.reset();
        this.F_ChargeLandau.clear();
        this.F_TimeGauss.clear();
        this.ht_mean.reset();
    }
    
    
    public void initLandauFitPar(int key) {
        // Initialize functiojn for cosmic runs in vertical position and with high thresholds //
        H1D hcharge = H_SIM_CHARGE.get(0, 0,key);
        int firstbin = 0;
        int filledbin=0;
        double binmax = hcharge.getXaxis().getNBins();
        int nbins=0;
        double meanv=0.0;
        int startfit =0;
        for(int i=0; i<binmax; i++){
            if(hcharge.getBinContent(i)!=0 && filledbin==0){
               firstbin=i;
               if((i+20)<=hcharge.getXaxis().getNBins())binmax=i+20;
               filledbin++; 
            }
            if(firstbin!=0 && i>=firstbin){
                meanv+=hcharge.getBinContent(i);
                nbins++;
            }
        }
            meanv=meanv/nbins;
            for(int i=firstbin; i<binmax; i++){
                if(hcharge.getBinContent(i)>meanv){
                    startfit=i;
                    break;
                }
            }     
        double mlMin=hcharge.getAxis().getBin(startfit);
        double mlMax=hcharge.getAxis().max();
        F1D ff;
        
        ff = new F1D("landau+exp", mlMin, mlMax);
        ff.setName("Landau_"+key);
        this.F_ChargeLandau.add(0, 0, key, ff);
        F_ChargeLandau.get(0, 0, key).setParameter(0, hcharge.getBinContent(hcharge.getMaximumBin()));
        F_ChargeLandau.get(0, 0, key).setParLimits(0, 0.0, 10000000.); 
        F_ChargeLandau.get(0, 0, key).setParameter(1,hcharge.getMean());
        F_ChargeLandau.get(0, 0, key).setParLimits(1, startfit, 30.0);//Changed from 5-30        
        F_ChargeLandau.get(0, 0, key).setParameter(2, 2.0);//Changed from 2
        F_ChargeLandau.get(0, 0, key).setParLimits(2, 0.3, 5.0);//Changed from 0.5-10
        F_ChargeLandau.get(0, 0, key).setParameter(3,mlMin);
        F_ChargeLandau.get(0, 0, key).setParLimits(3,mlMin-2.0, 10000000.0); 
        F_ChargeLandau.get(0, 0, key).setParameter(4, -0.1);//Changed from -0.2
        F_ChargeLandau.get(0, 0, key).setParLimits(4, -1.5, 0.0); //Changed from -10-0
    }
    
    private void initLandauFitParHorizonthal(int key) {
        H1D hcharge = H_SIM_CHARGE.get(0, 0,key);
        double mlMin=hcharge.getAxis().min();
        double mlMax=hcharge.getAxis().max();
        mlMin=2.0;
        F1D ff;
        if(hcharge.getBinContent(0)==0){
            ff = new F1D("landau",     mlMin, mlMax);
            ff.setName("Landau_"+key);
            F_ChargeLandau.add(0, 0, key, ff);
        }
        else{
            ff = new F1D("landau+exp", mlMin, mlMax);
            ff.setName("Landau_"+key);
            F_ChargeLandau.add(0, 0, key, ff);
        }
          
        if(hcharge.getBinContent(0)<20) {//Changed from 10
            F_ChargeLandau.get(0, 0, key).setParameter(0, hcharge.getBinContent(hcharge.getMaximumBin()));
        }
        else {
            F_ChargeLandau.get(0, 0, key).setParameter(0, 20);//Changed from 10
        }
        F_ChargeLandau.get(0, 0, key).setParLimits(0, 0.0, 10000000.0); 
        F_ChargeLandau.get(0, 0, key).setParameter(1,hcharge.getMean());
        F_ChargeLandau.get(0, 0, key).setParLimits(1, 4.0, 30.0);//Changed from 5-30        
        F_ChargeLandau.get(0, 0, key).setParameter(2,1.6);//Changed from 2
        F_ChargeLandau.get(0, 0, key).setParLimits(2, 0.3, 5);//Changed from 0.5-10
        if(hcharge.getBinContent(0)!=0) {
            F_ChargeLandau.get(0, 0, key).setParameter(3,hcharge.getBinContent(0));
            F_ChargeLandau.get(0, 0, key).setParLimits(3,  0.0, 10000000.); 
            F_ChargeLandau.get(0, 0, key).setParameter(4, -0.3);//Changed from -0.2
            F_ChargeLandau.get(0, 0, key).setParLimits(4, -3, 0.0); //Changed from -10-0
        }
        
    }
    
    private void fitLandau(int key) {
        H1D hcharge = H_SIM_CHARGE.get(0, 0,key);
        String name = F_ChargeLandau.get(0, 0, key).getName();
        if(name.indexOf("New_")==-1){
            if(this.SelectionType==1)initLandauFitParHorizonthal(key);
            else initLandauFitPar(key);  
        }
        hcharge.fit(F_ChargeLandau.get(0, 0, key),"NQ");
        this.updateChargeFitResults(key);
        
    }
    

    private void initTimeGaussFitPar(int key) {
        H1D htime = H_SIM_THALF.get(0, 0,key);
        double hAmp  = htime.getBinContent(htime.getMaximumBin());
        double hMean = htime.getAxis().getBinCenter(htime.getMaximumBin());
        double hRMS  = 2; //ns
        
        double rangeMin = (hMean - (0.8*hRMS)); 
        double rangeMax = (hMean + (0.2*hRMS));     
        F_TimeGauss.add(0, 0, key, new F1D("gaus", rangeMin, rangeMax));
        F_TimeGauss.get(0, 0, key).setName("Gaus_"+key);
        F_TimeGauss.get(0, 0, key).setParameter(0, hAmp);
        F_TimeGauss.get(0, 0, key).setParLimits(0, hAmp*0.8, hAmp*1.2);
        F_TimeGauss.get(0, 0, key).setParameter(1, hMean);
        Double pm = (hMean*3.)/100.0;
        F_TimeGauss.get(0, 0, key).setParLimits(1, hMean-pm, hMean+(pm));
        F_TimeGauss.get(0, 0, key).setParameter(2, 0.2);
        F_TimeGauss.get(0, 0, key).setParLimits(2, 0.1*hRMS, 0.8*hRMS);
    }    

    private void fitTime(int key) {
        H1D htime = H_SIM_THALF.get(0, 0,key);
        String name =F_TimeGauss.get(0, 0, key).getName();
        if(name.indexOf("New_")==-1)initTimeGaussFitPar(key);
        htime.fit(F_TimeGauss.get(0, 0, key),"LQ");
        this.updateTimeFitResults(key);
    }
    
    @Override
    public void fitCollections() {
        for(int key : H_SIM_CHARGE.getComponents(0, 0)) {
            if(H_SIM_CHARGE.get(0, 0, key).getEntries()>100) {
                //this.fitLandau(key);
            }
            if(H_SIM_THALF.get(0, 0, key).getEntries()>0) {    
              this.fitTime(key);
            } 
        }     
        if(this.getCanvasSelect() == "Energy" && this.compcanvas!="Comparison") this.fitBook(H_SIM_CHARGE,F_ChargeLandau);
        else if(this.getCanvasSelect() == "Time")   this.fitBook(H_SIM_THALF,F_TimeGauss);
    }
    
     public void fitFastCollections() {
        for(int key : H_SIM_CHARGE.getComponents(0, 0)) {
            if(H_SIM_CHARGE.get(0, 0, key).getEntries()>0 && F_ChargeLandau.get(0, 0, key).getName()=="f1") {
                //this.fitLandau(key);
            }
            if(H_SIM_THALF.get(0, 0,key).getEntries()>0 && F_TimeGauss.get(0, 0, key).getName()=="f1") {   
                this.fitTime(key);
            }   
        }
    }
     
    public void fitFastCollections(DetectorCollection<H1D> histo) {
        //DetectorCollection<F1D> fct = null;
        Boolean fitcharge= true;
        for(int key : histo.getComponents(0, 0)) {
            if(histo.get(0, 0, key).getEntries()>100) {
                //if(fitcharge)this.fitLandau(key);
                //else 
                    this.fitTime(key);
               
            }  
        }
    }
        public void fitCollections(String canvas) {
            this.compcanvas = canvas;
            fitCollections();
        }
    
      
    private void updateChargeFitResults(int key){
        H_SIM_MEAN.setBinContent(key, F_ChargeLandau.get(0, 0, key).getParameter(1));
        H_SIM_SIGMA.setBinContent(key, F_ChargeLandau.get(0, 0, key).getParameter(2));
        H_SIM_CHI2.setBinContent(key, F_ChargeLandau.get(0, 0, key).getChiSquare(H_SIM_CHARGE.get(0, 0,key),"LR")
                                        /F_ChargeLandau.get(0, 0, key).getNDF(H_SIM_CHARGE.get(0, 0,key).getDataSet()));
    }

    private void updateTimeFitResults(int key){
        H_TIME_MEAN.setBinContent(key, F_TimeGauss.get(0, 0, key).getParameter(1));
        H_TIME_SIGMA.setBinContent(key, F_TimeGauss.get(0, 0, key).getParameter(2));
    }
    
    private H1D updateSimTimeFitResults(DetectorCollection<H1D> hist, DetectorCollection<F1D> fct){ 
        H1D ht = new H1D("Ht",512,-10.0,15.0);
        for(int key: hist.getComponents(0, 0)){
            if(hist.get(0, 0, key).getEntries()>20){
                this.fitTime(key);
                ht.fill(fct.get(0, 0, key).getParameter(1));
            }
        }
        this.ht_mean = ht;
        return ht;
    }
    

    @Override
    public void customizeFit(int key){ 
        //System.out.println("customizeFit: "+this.getCanvasSelect());
        if(this.getCanvasSelect() == "Energy") {
            cfit.FitPanel(H_SIM_CHARGE.get(0, 0,key), F_ChargeLandau.get(0, 0,key),"LQ");
            this.getCanvas("Energy").update();
            this.updateChargeFitResults(key); 
        }
        else if(this.getCanvasSelect() == "Time") {
            cfit.FitPanel(H_SIM_THALF.get(0, 0,key), F_TimeGauss.get(0, 0,key),"LQ");
            cfit.FitPanel(this.ht_mean, this.F_time_mean,"LQ");
            this.getCanvas("Time").update();            
            this.updateTimeFitResults(key);
        }
        else if(this.getCanvasSelect() == "Comparison") {
            cfit.FitPanel(H_SIM_CHARGE.get(0, 0,key), F_ChargeLandau.get(0, 0,key),"LQ");
            this.getCanvas("Comparison").update();
            this.updateChargeFitResults(key);
        }
    }
  

    public void addEvent(List<DetectorCounter> counters) { 
        nProcessed++;
        if(nProcessed==1)selInfo();
        H1D H_WMAX = new H1D("WMAX",this.getDetector().getComponentMaxCount(),0.,this.getDetector().getComponentMaxCount());
        H_WMAX.reset();
        double tPMTCross=0;
        double tPMTHalf=0;
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            if(this.getDetector().hasComponent(key)) {
                this.getFitter().fit(counter.getChannels().get(0),this.singleChThr);
                           
                H_fADC_N.fill(key);
                short pulse[] = counter.getChannels().get(0).getPulse();
                H_WMAX.fill(key,this.getFitter().getWave_Max()-this.getFitter().getPedestal());
                if(key==501) {      // top long PMT
                    tPMTCross = this.getFitter().getTime(3);
                    tPMTHalf  = this.getFitter().getTime(7);
                }    
            }
        }
        
      double timecor = 0.0;
      double charge  = 0.0;
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            if(this.getDetector().hasComponent(key)) {
                Boolean sel = false;
                if(SelectionType==1)sel = this.gammasel.SignalAboveThrSel(counter, key, H_WMAX, this.getFitter(), this.singleChThr);
                if(sel==true){
                    
                    short pulse[] = counter.getChannels().get(0).getPulse();
                    H_SIM_N.fill(key);
                    for (int i = 0; i < Math.min(pulse.length, H_SIM_fADC.get(0, 0, key).getAxis().getNBins()); i++) {
                        H_SIM_fADC.get(0, 0, key).fill(i, pulse[i]-this.getFitter().getPedestal() + 10.0);                
                    }
                    charge=((counter.getChannels().get(0).getADC().get(0)*LSB*nsPerSample/50)*this.charge2e);
                    H_SIM_CHARGE.get(0, 0, key).fill(charge);
                    H_SIM_VMAX.get(0, 0, key).fill((this.getFitter().getWave_Max()-this.getFitter().getPedestal())*LSB);
                    H_SIM_TCROSS.get(0, 0, key).fill(this.getFitter().getTime(3)-tPMTCross);
                    H_SIM_THALF.get(0, 0, key).fill(this.getFitter().getTime(7)-tPMTHalf);      
                  
                }  
            }
         }
       
    }

    private double timecorrection(double time, int key){
        double timec =0.0;
        double gamma=1.0;
        double radius = sqrt(pow(this.getDetector().getIdX(key)-0.5,2.0)+pow(this.getDetector().getIdY(key)-0.5,2.0))*this.crystal_size*1.0E-3;//meters
        double distcor = sqrt(pow(this.ftcalDistance+shower_depth/1000.,2)+pow(radius,2));
        double tof = (distcor/(gamma*3.0)*1.0E1); //ns
        timec = (((time*nsPerSample)/100.0) -(this.startTime + (crystal_length-shower_depth)/light_speed + tof))-this.timeshift;
        return timec;
    }
    
    
    public void addSimEvent(DetectorCollection<Double> adc, DetectorCollection<Double> tdc) { 
        nProcessed++;
        simflag=true;
        //if(nProcessed==1)selInfo();// Da rimettere pannello soglie
   
    Boolean sel=false;
    double charge = 0.0;
    double time   = 0.0;
    double time2   = 0.0;
    
    this.gammasel.getMaxEnergyEvt(adc);// Looks for channel with the maximum energy
       for(int key : adc.getComponents(0, 0)) {
            if(adc.hasEntry(0, 0, key)){
                charge=(adc.get(0, 0, key)*(LSB*nsPerSample/50))*this.charge2e;
                time = timecorrection(tdc.get(0, 0, key), key);
                sel = this.gammasel.SimCosmicEvtSel(key, charge, time, simSignalThr);
                H_SIM_TCROSS.get(0, 0, key).fill(time);
                H_SIM_VMAX.get(0, 0, key).fill((charge/1000.0));
                if(this.gammasel.EnergyEvMax.get(0, 0, key))H_fADC.fill(charge/1000.0);
                if(sel){
                    H_SIM_CHARGE.get(0, 0, key).fill(charge);
                    H_SIM_THALF.get(0, 0, key).fill(time);  
                }
            }
        }
    }
    
    public void updateCanvas(int keySelect) {
        // Energy - Cosmics for now
        this.getCanvas("Energy").cd(0);
        if (H_fADC.getEntries()>0) {
            hfADC = H_fADC.histClone(" ");
            hfADC.setFillColor(3);
            hfADC.setTitle("Max Energy events");
            hfADC.setXTitle("Energy (GeV)");
            hfADC.setYTitle("Counts");
            this.getCanvas("Energy").draw(hfADC);               
        }
        this.getCanvas("Energy").cd(1);
        if (H_SIM_fADC.hasEntry(0, 0, keySelect)) {
            hfADC = H_SIM_fADC.get(0, 0, keySelect).histClone(" ");
            hfADC.normalize(H_SIM_N.getBinContent(keySelect));
            hfADC.setFillColor(3);
            hfADC.setXTitle("fADC Sample");
            hfADC.setYTitle("fADC Counts");
            this.getCanvas("Energy").draw(hfADC);               
        }
        this.getCanvas("Energy").cd(2);
        if(H_SIM_VMAX.hasEntry(0, 0, keySelect)) {
            H1D hcharge = H_SIM_VMAX.get(0, 0,keySelect);
            this.getCanvas("Energy").draw(hcharge,"S");
        }
        this.getCanvas("Energy").cd(3);
        if(H_SIM_CHARGE.hasEntry(0, 0, keySelect)) {
            H1D hcharge = H_SIM_CHARGE.get(0, 0,keySelect);
            this.getCanvas("Energy").draw(hcharge,"S");
            if(hcharge.getEntries()>0) {
                //fitLandau(keySelect);
                this.getCanvas("Energy").draw(F_ChargeLandau.get(0, 0, keySelect),"sameS");
            }
        }     
        if(!this.simflag)timeTabReal(keySelect);
        else timeTabSim(keySelect);
    }
    
    private void timeTabSim(int keySelect){
              // Time
        int ipointer = 0;
        
        this.getCanvas("Time").cd(0);
        H1D ht = updateSimTimeFitResults(this.H_SIM_THALF, this.F_TimeGauss);
        this.getCanvas("Time").draw(ht,"S");
        F_time_mean.setRange((ht.getMean()-(7.0*ht.getRMS())), (ht.getMean()+(7.0*ht.getRMS())));
        F_time_mean.setParameter(0, ht.getBinContent(ht.getMaximumBin()));
        //F_time_mean.setParLimits(0, 1,300);
        F_time_mean.setParameter(1, ht.getMean());
        F_time_mean.setParameter(2, 0.5);
        F_time_mean.setParLimits(2, 0.2*ht.getRMS(), 2.0*ht.getRMS());
        ht.fit(F_time_mean,"LQ");
        this.getCanvas("Time").draw(F_time_mean,"sameS");
    
        for(int key : this.getDetector().getDetectorComponents()) {
            timeHALF[ipointer]     = this.F_TimeGauss.get(0, 0, key).getParameter(1);
            ipointer++;
        }
        GraphErrors  G_THALF = new GraphErrors(detectorIDs,timeHALF);
        G_THALF.setTitle(" "); //  title
        G_THALF.setXTitle("Crystal ID"); // X axis title
        G_THALF.setYTitle("Timing (ns)");   // Y axis title
        G_THALF.setMarkerColor(5); // color from 0-9 for given palette
        G_THALF.setMarkerSize(5);  // size in points on the screen
        G_THALF.setMarkerStyle(1); // Style can be 1 or 2
        this.getCanvas("Time").cd(1);
        this.getCanvas("Time").draw(G_THALF);
        this.getCanvas("Time").cd(2);
        if(H_SIM_TCROSS.hasEntry(0, 0, keySelect)) {
            H1D htime = H_SIM_TCROSS.get(0, 0, keySelect);
            this.getCanvas("Time").draw(htime,"S");
        }
        this.getCanvas("Time").cd(3);
        if(H_SIM_THALF.hasEntry(0, 0, keySelect)) {
            H1D htime = H_SIM_THALF.get(0, 0, keySelect);
            this.getCanvas("Time").draw(htime,"S");
            if(htime.getEntries()>0) {
                fitTime(keySelect);
                this.getCanvas("Time").draw(F_TimeGauss.get(0, 0, keySelect),"sameS");
            }
        }
        
    }
    
   private void timeTabReal(int keySelect){
        // Time
        int ipointer = 0;
        for(int key : this.getDetector().getDetectorComponents()) {
            timeCROSS[ipointer]    = this.F_TimeGauss.get(0, 0, key).getParameter(1);
            timeHALF[ipointer]     = this.F_TimeGauss.get(0, 0, key).getParameter(1);
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
        if(H_SIM_TCROSS.hasEntry(0, 0, keySelect)) {
            H1D htime = H_SIM_TCROSS.get(0, 0, keySelect);
            this.getCanvas("Time").draw(htime,"S");
        }
        this.getCanvas("Time").cd(3);
        if(H_SIM_THALF.hasEntry(0, 0, keySelect)) {
            H1D htime = H_SIM_THALF.get(0, 0, keySelect);
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
                case 0: value = this.H_SIM_CHARGE.get(0, 0, key).getEntries();
                    break;
                case 1: value = this.H_SIM_MEAN.getBinContent(key); //cambiare
                    break;
                case 2: value = this.H_SIM_SIGMA.getBinContent(key);//cambiare
                    break;
                case 3: value = this.H_SIM_CHI2.getBinContent(key);//cambiare
                    break;
                 case 4: value = this.F_TimeGauss.get(0, 0, key).getParameter(1);
                    break;
                 case 5: value = this.F_TimeGauss.get(0, 0, key).getParameter(2);   
                    break;
                case 6: value = this.signalThr*this.LSB;
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
            int nent = this.H_SIM_CHARGE.get(0, 0, key).getEntries();
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
    
    
//     @Override
//    public Color getColor(int key) {
//        Color col = new Color(100, 100, 100);
//        if (this.H_NOISE.hasEntry(0, 0, key)) {
//            int nent = this.H_NOISE.get(0, 0, key).getEntries();
//            if (nent > 0) {
//                if(this.getButtonIndex()!=0) 
//                    col = this.getSelectedParameter().getColor(this.getFieldValue(this.getButtonIndex(),key), false);
//                else {
//                    int color =0;
//                    col = this.getSelectedParameter().getColor(color, true);
//                    if(this.getFieldValue(this.getButtonIndex(),key)==3){
//                        color=-1;  
//                        col = this.getSelectedParameter().getColor(color, true);
//                    }
//                    else if(this.getFieldValue(this.getButtonIndex(),key)==1){
//                        color=1;
//                        col = this.getSelectedParameter().getColor(color, true);
//                    }
//                    else if(this.getFieldValue(this.getButtonIndex(),key)==5){
//                        col = new Color(255,255,0);
//                    }
//                }
//            } 
//        }
//        return col;
//    }
    
    
    public void addGammaToFile(FitParametersFile file) {
        file.add(this.F_ChargeLandau, this.H_SIM_CHARGE);
    }
    
    public void saveToFile(HipoFile histofile) {
        histofile.addToMap("Energy_histo", this.H_SIM_CHARGE);
        histofile.addToMap("Energy_fct", this.F_ChargeLandau);
        histofile.addToMap("Time_histo", this.H_SIM_THALF);
        histofile.addToMap("Time_fct", this.F_TimeGauss);
        histofile.addToMap("Fadc", this.H_SIM_fADC);
        histofile.addToMap("Amplitude", this.H_SIM_VMAX);
    }
    
    
    
}
