/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import java.util.ArrayList;
import java.util.List;
import org.clas.tools.FTApplication;
import org.clas.tools.FTDetector;
import org.jlab.clas.detector.DetectorCollection;
import org.root.func.F1D;
import org.root.histogram.H1D;
import org.clas.tools.CalibrationData;
import org.clas.ftcal.FTCALCosmic;
import org.jlab.clas12.detector.DetectorCounter;

/**
 *
 * @author fanchini
 */
public class FTCALCompareApp extends FTApplication{
    
    CalibrationData calib = null;
    FTCALCosmic cosmic =null;

    // Data Collection 
    DetectorCollection<H1D> H_CHARGE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_TIME  = new DetectorCollection<H1D>();
    DetectorCollection<F1D> F_Charge  = new DetectorCollection<F1D>();
    DetectorCollection<F1D> F_Time     = new DetectorCollection<F1D>();
    
    DetectorCollection<H1D> H_REF_CHARGE    = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_REF_TIME     = new DetectorCollection<H1D>();
    DetectorCollection<F1D> F_Ref_Charge    = new DetectorCollection<F1D>();
    DetectorCollection<F1D> F_Ref_Time      = new DetectorCollection<F1D>();
    
    H1D H_COSMIC_MEAN      = null;
    H1D H_TIME_MEAN        = null;
    H1D H_REF_COSMIC_MEAN  = null;
    H1D H_REF_TIME_MEAN    = null;
    H1D hCharge            = null;
    H1D hTime              = null;
    H1D hRefCharge         = null;
    H1D hRefTime           = null;
    
    
    
    
    public FTCALCompareApp(FTDetector d) {
        super(d);
        this.setName("Comparison");
        this.addCanvas("Comparison");
        this.getCanvas("Comparison").divideCanvas(2, 2);
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
        this.calib = new CalibrationData();
        this.cosmic = new FTCALCosmic();
    
        
        H_CHARGE    = this.getData().addCollection(new H1D("Charge", 80, 0.0, 40.0),"Charge (pC)","Counts",2,"H_CHARGE");
        H_TIME      = this.getData().addCollection(new H1D("T_Time", 80, -20.0, 60.0), "Time (ns)", "Counts", 5, "H_TIME");
        F_Charge    = this.getData().addCollection(new F1D("landau", 0.0, 40.0),"Landau","F_Charge");
        F_Time      = this.getData().addCollection(new F1D("gaus", -20.0, 60.0),"Time","F_Charge");       
        H_REF_CHARGE    = this.getData().addCollection(new H1D("Reference Charge", 80, 0.0, 40.0),"Charge (pC)","Counts",2,"H_REF_CHARGE");
        H_REF_TIME      = this.getData().addCollection(new H1D("Reference T_Time", 80, -20.0, 60.0), "Time (ns)", "Counts", 5, "H_REF_TIME");
        F_Ref_Charge    = this.getData().addCollection(new F1D("landau", 0.0, 40.0),"Landau","F_Ref_Charge");
        F_Ref_Time      = this.getData().addCollection(new F1D("gaus", -20.0, 60.0),"Time","F_Ref_Charge");

        
        H_COSMIC_MEAN      = new H1D("ENERGY MEAN"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_TIME_MEAN        = new H1D("TIME   MEAN"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_REF_COSMIC_MEAN  = new H1D("Reference ENERGY MEAN"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_REF_TIME_MEAN    = new H1D("Reference TIME   MEAN"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());        
        
        String hipoFile = "./Run533Hipotest.hipo";
        dumpCalib_Ref(hipoFile);
        
    }
   
    
    public void dumpCalib_Ref(String hipoFile){
        // Cosmic calibration //
        calib.getFile(hipoFile);
        H_REF_CHARGE = calib.getCollection("Energy_histo");
        H_REF_TIME   = calib.getCollection("Noise_histo"); 
        F_Ref_Charge = calib.getCollection("Energy_fct"); 
        F_Ref_Time   = calib.getCollection("Noise_fct"); 
    }
    
   public void dumpCalib(String hipoFile){
        // Cosmic calibration //
        calib.getFile(hipoFile);
        H_CHARGE = calib.getCollection("Energy_histo");
        H_TIME   = calib.getCollection("Noise_histo"); 
        F_Charge = calib.getCollection("Energy_fct"); 
        F_Time   = calib.getCollection("Noise_fct"); 
    }
  
  
    void updateCanvas(int keySelect) {

        this.getCanvas("Comparison").cd(0);
        if (this.H_CHARGE.hasEntry(0, 0, keySelect)) {
            this.hCharge = this.H_CHARGE.get(0, 0, keySelect).histClone(" ");
            this.hCharge.setFillColor(4);
            this.hCharge.setTitle(this.H_CHARGE.get(0, 0, keySelect).getTitle());
            this.hCharge.setXTitle(this.H_CHARGE.get(0, 0, keySelect).getXTitle());
            this.hCharge.setYTitle(this.H_CHARGE.get(0, 0, keySelect).getYTitle());
            this.getCanvas("Comparison").draw(this.hCharge);
        }
        this.getCanvas("Comparison").cd(1);
        if (this.H_TIME.hasEntry(0, 0, keySelect)) {
            this.hTime = this.H_TIME.get(0, 0, keySelect).histClone(" ");
            this.hTime.setFillColor(4);
            this.hTime.setTitle(this.H_TIME.get(0, 0, keySelect).getTitle());
            this.hTime.setXTitle(this.H_TIME.get(0, 0, keySelect).getXTitle());
            this.hTime.setYTitle(this.H_TIME.get(0, 0, keySelect).getYTitle());
            this.getCanvas("Comparison").draw(this.hTime);
        }
            
        this.getCanvas("Comparison").cd(2);
        if (this.H_REF_CHARGE.hasEntry(0, 0, keySelect)) {
            this.hRefCharge = this.H_REF_CHARGE.get(0, 0, keySelect).histClone(" ");
            this.hRefCharge.setFillColor(3);
            this.hRefCharge.setTitle(this.H_REF_CHARGE.get(0, 0, keySelect).getTitle());
            this.hRefCharge.setXTitle(this.H_REF_CHARGE.get(0, 0, keySelect).getXTitle());
            this.hRefCharge.setYTitle(this.H_REF_CHARGE.get(0, 0, keySelect).getYTitle());
            this.getCanvas("Comparison").draw(this.hRefCharge);
        }
        this.getCanvas("Comparison").cd(3);
        if (this.H_REF_TIME.hasEntry(0, 0, keySelect)) {
            this.hRefTime = this.H_REF_TIME.get(0, 0, keySelect).histClone(" ");
            this.hRefTime.setFillColor(3);
            this.hRefTime.setTitle(this.H_REF_TIME.get(0, 0, keySelect).getTitle());
            this.hRefTime.setXTitle(this.H_REF_TIME.get(0, 0, keySelect).getXTitle());
            this.hRefTime.setYTitle(this.H_REF_TIME.get(0, 0, keySelect).getYTitle());
            this.getCanvas("Comparison").draw(this.hRefTime);    
        }
        
    }
    
   public void addEvent(List<DetectorCounter> counters) {  
      
   } 
   
   public void clearCollerctions(){
       this.H_CHARGE.clear();
       this.H_TIME.clear();
       this.F_Charge.clear();
       this.F_Time.clear();
   }
    
   @Override
    public void resetCollections() {
        
        for (int component : this.getDetector().getDetectorComponents()) {     
            this.H_CHARGE.get(0, 0, component).reset();
            this.H_TIME.get(0, 0, component).reset();       
            this.H_REF_CHARGE.get(0, 0, component).reset();
            this.H_REF_TIME.get(0, 0, component).reset();       
        }
        
        this.H_COSMIC_MEAN.reset();
        this.H_TIME_MEAN.reset();
        this.H_REF_COSMIC_MEAN.reset();
        this.H_REF_TIME_MEAN  .reset();
    } 
    
}
