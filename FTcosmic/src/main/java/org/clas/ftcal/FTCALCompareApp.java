/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import java.awt.Color;
import java.io.File;
import javax.swing.JFileChooser;
import org.clas.tools.FTApplication;
import org.clas.tools.FTDetector;
import org.jlab.clas.detector.DetectorCollection;
import org.root.func.F1D;
import org.root.histogram.H1D;
import org.clas.tools.CalibrationData;
import org.clas.ftcal.FTCALCosmic;
import org.clas.tools.Miscellaneous;
import org.root.attr.ColorPalette;

/**
 *
 * @author fanchini
 */
public class FTCALCompareApp extends FTApplication{
    
    CalibrationData calib = null;
    FTCALCosmic cosmic =null;
   

    // Data Collection 
    DetectorCollection<H1D> H_CHARGE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_TIME   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_PED    = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NOISE  = new DetectorCollection<H1D>();
    
    DetectorCollection<F1D> F_Charge  = new DetectorCollection<F1D>();
    DetectorCollection<F1D> F_Time     = new DetectorCollection<F1D>();
    
    DetectorCollection<H1D> H_REF_CHARGE    = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_REF_TIME      = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_REF_PED       = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_REF_NOISE     = new DetectorCollection<H1D>();
    
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
    H1D hPed               = null;
    H1D hNoise             = null;
    H1D hRefPed            = null;
    H1D hRefNoise          = null;
    
     Miscellaneous extra = new Miscellaneous();
    
    
    
    public FTCALCompareApp(FTDetector d) {
        super(d);
        this.setName("Comparison");
        this.addCanvas("Comparison");
        this.getCanvas("Comparison").divideCanvas(4, 2);
        this.addFields("Occupancy", "<E>", "\u03C3(E)", "\u03C7\u00B2(E)", "<T>", "\u03C3(T)");
//        this.getParameter(0).setRanges(0.,1000000.,1.,1000000.);
//        this.getParameter(1).setRanges(5.,25.,10.,50.);
//        this.getParameter(2).setRanges(0.,10.,10.,10.);
//        this.getParameter(3).setRanges(0.,2.,10.,5.);
//        this.getParameter(4).setRanges(0.,50.,10.,50.);
//        this.getParameter(5).setRanges(0.,5.,10.,5.);
        this.initCollections();
    }

    
    private void initCollections() {
        this.calib = new CalibrationData();
        this.cosmic = new FTCALCosmic();
        
        H_CHARGE     = this.getData().addCollection(new H1D("Charge", 80, 0.0, 40.0),"Charge (pC)","Counts",2,"H_CHARGE");
        H_TIME       = this.getData().addCollection(new H1D("T_Time", 80, -20.0, 60.0), "Time (ns)", "Counts", 5, "H_TIME");
        H_PED        = this.getData().addCollection(new H1D("Pedestal", 400, 100., 300.0),"Pedestal (fADC counts)","Counts",2,"H_PED");
        H_NOISE      = this.getData().addCollection(new H1D("Noise", 200, 0.0, 10.0),"RMS (mV)","Counts",4,"H_NOISE"); 
        F_Charge     = this.getData().addCollection(new F1D("landau", 0.0, 40.0),"Landau","F_Charge");
        F_Time       = this.getData().addCollection(new F1D("gaus", -20.0, 60.0),"Time","F_Charge");       
        H_REF_CHARGE = this.getData().addCollection(new H1D("Reference Charge", 80, 0.0, 40.0),"Charge (pC)","Counts",2,"H_REF_CHARGE");
        H_REF_TIME   = this.getData().addCollection(new H1D("Reference T_Time", 80, -20.0, 60.0), "Time (ns)", "Counts", 5, "H_REF_TIME");
        H_REF_PED    = this.getData().addCollection(new H1D("Reference Pedestal", 400, 100., 300.0),"Pedestal (fADC counts)","Counts",2,"H_REF_PED");
        H_REF_NOISE  = this.getData().addCollection(new H1D("Reference Noise", 200, 0.0, 10.0),"RMS (mV)","Counts",4,"H_REF_NOISE"); 
        F_Ref_Charge = this.getData().addCollection(new F1D("landau", 0.0, 40.0),"Landau","F_Ref_Charge");
        F_Ref_Time   = this.getData().addCollection(new F1D("gaus", -20.0, 60.0),"Time","F_Ref_Charge");

        
        H_COSMIC_MEAN      = new H1D("ENERGY MEAN"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_TIME_MEAN        = new H1D("TIME   MEAN"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_REF_COSMIC_MEAN  = new H1D("Reference ENERGY MEAN"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());
        H_REF_TIME_MEAN    = new H1D("Reference TIME   MEAN"  , this.getDetector().getComponentMaxCount(), 0, this.getDetector().getComponentMaxCount());        
        
        
    }
   
    
    private void dumpCalib_Ref(String hipoFile){
        // Cosmic calibration //
        calib.getFile(hipoFile);
        H_REF_CHARGE = calib.getCollection("Energy_histo");
        H_REF_TIME   = calib.getCollection("Time_histo"); 
        F_Ref_Charge = calib.getCollection("Energy_fct"); 
        F_Ref_Time   = calib.getCollection("Time_fct"); 
        H_REF_PED    = calib.getCollection("Pedestal_histo");
        H_REF_NOISE  = calib.getCollection("Noise_histo");
    }
    
   public void dumpCalib(String hipoFile){
        // Cosmic calibration //
        calib.getFile(hipoFile);
        H_CHARGE = calib.getCollection("Energy_histo");
        H_TIME   = calib.getCollection("Time_histo"); 
        F_Charge = calib.getCollection("Energy_fct"); 
        F_Time   = calib.getCollection("Time_fct"); 
        H_PED    = calib.getCollection("Pedestal_histo");
        H_NOISE  = calib.getCollection("Noise_histo");
    }
  
  
    void updateCanvas(int keySelect) {
        String htitle="", xtitle="", ytitle="";
        
        htitle = "Component "+keySelect;
        this.getCanvas("Comparison").cd(0);
        if (this.H_CHARGE.hasEntry(0, 0, keySelect)) {
            xtitle="Charge (pC)"; 
            ytitle="Counts";
            this.hCharge = this.H_CHARGE.get(0, 0, keySelect).histClone(" ");
            this.hCharge.setFillColor(2);
            this.hCharge.setTitle(htitle);
            this.hCharge.setXTitle(xtitle);
            this.hCharge.setYTitle(ytitle);
            this.getCanvas("Comparison").draw(this.hCharge);
            this.getCanvas("Comparison").draw(this.F_Charge.get(0, 0, keySelect),"same");
        }
        this.getCanvas("Comparison").cd(1);
        if (this.H_TIME.hasEntry(0, 0, keySelect)) {
            xtitle="Time (ns)"; 
            ytitle="Counts";
            this.hTime = this.H_TIME.get(0, 0, keySelect).histClone(" ");
            this.hTime.setFillColor(5);
            this.hTime.setTitle(htitle);
            this.hTime.setXTitle(xtitle);
            this.hTime.setYTitle(ytitle);
            this.getCanvas("Comparison").draw(this.hTime);
            this.getCanvas("Comparison").draw(this.F_Time.get(0, 0, keySelect),"same");
        }
        this.getCanvas("Comparison").cd(2);
        if (this.H_PED.hasEntry(0, 0, keySelect)) {
            xtitle="Pedestal"; 
            ytitle="Counts";
            this.hPed = this.H_PED.get(0, 0, keySelect).histClone(" ");
            this.hPed.setFillColor(5);
            this.hPed.setTitle(htitle);
            this.hPed.setXTitle(xtitle);
            this.hPed.setYTitle(ytitle);
            this.getCanvas("Comparison").draw(this.hPed);
        }   
        this.getCanvas("Comparison").cd(3);
        if (this.H_NOISE.hasEntry(0, 0, keySelect)) {
            xtitle="Noise"; 
            ytitle="Counts";
            this.hNoise = this.H_NOISE.get(0, 0, keySelect).histClone(" ");
            this.hNoise.setFillColor(5);
            this.hNoise.setTitle(htitle);
            this.hNoise.setXTitle(xtitle);
            this.hNoise.setYTitle(ytitle);
            this.getCanvas("Comparison").draw(this.hNoise);
        }   
        this.getCanvas("Comparison").cd(4);
        if (this.H_REF_CHARGE.hasEntry(0, 0, keySelect)) {
            this.hRefCharge = this.H_REF_CHARGE.get(0, 0, keySelect).histClone(" ");
            this.hRefCharge.setFillColor(22);
            this.hRefCharge.setTitle(this.H_REF_CHARGE.get(0, 0, keySelect).getTitle());
            this.hRefCharge.setXTitle(this.H_REF_CHARGE.get(0, 0, keySelect).getXTitle());
            this.hRefCharge.setYTitle(this.H_REF_CHARGE.get(0, 0, keySelect).getYTitle());
            this.getCanvas("Comparison").draw(this.hRefCharge);
        }
        this.getCanvas("Comparison").cd(5);
        if (this.H_REF_TIME.hasEntry(0, 0, keySelect)) {
            this.hRefTime = this.H_REF_TIME.get(0, 0, keySelect).histClone(" ");
            this.hRefTime.setFillColor(24);
            this.hRefTime.setTitle(this.H_REF_TIME.get(0, 0, keySelect).getTitle());
            this.hRefTime.setXTitle(this.H_REF_TIME.get(0, 0, keySelect).getXTitle());
            this.hRefTime.setYTitle(this.H_REF_TIME.get(0, 0, keySelect).getYTitle());
            this.getCanvas("Comparison").draw(this.hRefTime);    
        }
        this.getCanvas("Comparison").cd(6);
        if (this.H_REF_PED.hasEntry(0, 0, keySelect)) {
            this.hRefPed = this.H_REF_PED.get(0, 0, keySelect).histClone(" ");
            this.hRefPed.setFillColor(23);
            this.hRefPed.setTitle(this.H_REF_PED.get(0, 0, keySelect).getTitle());
            this.hRefPed.setXTitle(this.H_REF_PED.get(0, 0, keySelect).getXTitle());
            this.hRefPed.setYTitle(this.H_REF_PED.get(0, 0, keySelect).getYTitle());
            this.getCanvas("Comparison").draw(this.hRefPed);    
        }
        this.getCanvas("Comparison").cd(7);
        if (this.H_REF_NOISE.hasEntry(0, 0, keySelect)) {
            xtitle="Noise"; 
            ytitle="Counts";
            this.hRefNoise = this.H_REF_NOISE.get(0, 0, keySelect).histClone(" ");
            this.hRefNoise.setFillColor(5);
            this.hRefNoise.setTitle(htitle);
            this.hRefNoise.setXTitle(xtitle);
            this.hRefNoise.setYTitle(ytitle);
            this.getCanvas("Comparison").draw(this.hRefNoise);
        }   
    }
    
   public void clearCollections(){
       this.H_CHARGE.clear();
       this.H_TIME.clear();
       this.F_Charge.clear();
       this.F_Time.clear();
   }
    
   @Override
    public void resetCollections() {
        
        for (int component : this.getDetector().getDetectorComponents()) {  
            if(this.H_CHARGE.get(0, 0, component).getEntries()>0)this.H_CHARGE.get(0, 0, component).reset();
            if(this.H_TIME.get(0, 0, component).getEntries()>0)this.H_TIME.get(0, 0, component).reset();       
            if(this.H_REF_CHARGE.get(0, 0, component).getEntries()>0)this.H_REF_CHARGE.get(0, 0, component).reset();
            if(this.H_REF_TIME.get(0, 0, component).getEntries()>0)this.H_REF_TIME.get(0, 0, component).reset();   
        }
        if(this.H_COSMIC_MEAN.getEntries()>0)this.H_COSMIC_MEAN.reset();
        if(this.H_TIME_MEAN.getEntries()>0)this.H_TIME_MEAN.reset();
        if(this.H_REF_COSMIC_MEAN.getEntries()>0)this.H_REF_COSMIC_MEAN.reset();
        if(this.H_REF_TIME_MEAN.getEntries()>0)this.H_REF_TIME_MEAN.reset();
    } 
    
    public void fileSelection(){
        JFileChooser fc = new JFileChooser();  // file chooser   
        String outputFileName = "";
        String buttonFileName = "";
        fc.setCurrentDirectory(new File(outputFileName));
	int returnValue = fc.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            buttonFileName = fc.getSelectedFile().getAbsolutePath();
            outputFileName = buttonFileName;
        }
       
        if(outputFileName=="")System.out.println("Comparison: No file chosen");
        else dumpCalib_Ref(outputFileName);
    }
    
}
