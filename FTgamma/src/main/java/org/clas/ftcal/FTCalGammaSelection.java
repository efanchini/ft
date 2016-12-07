/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import static java.lang.Integer.max;
import java.util.List;
import org.clas.tools.ExtendedFADCFitter;
import org.clas.tools.FTApplication;
import org.clas.tools.FTDetector;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas12.detector.DetectorCounter;
import org.root.histogram.H1D;

/**
 *
 * @author fanchini
 */
public class FTCalGammaSelection extends FTApplication{
    private Double targetDist = 189.0; //(cm) distance between target and FTCal
    
    public DetectorCollection<Boolean> EnergyEvMax     = new DetectorCollection<Boolean>(); // is set to 1 for the max energy event
    
    public FTCalGammaSelection(FTDetector d){
        super(d);
    }
    
  public Boolean SimCosmicEvtSel(int key, Double charge, Double tdc, double simSignalThr){
        //System.out.println("SimCosmicEvtSel charge:"+charge+" tdc: "+tdc+" thr:"+simSignalThr);
        Boolean flagsel=false;
        if(charge>simSignalThr && tdc>0.0) flagsel=true;
        //System.out.println("SimCosmicEvtSel charge:"+charge+" tdc: "+tdc+" thr:"+simSignalThr+" flag:"+flagsel);
        return flagsel;
    }  
    
    public Boolean SignalAboveThrSel(DetectorCounter counter, int key, H1D H_WMAX, ExtendedFADCFitter fitter, double singleChThr){
       Boolean flagsel=false;
       fitter.fit(counter.getChannels().get(0),singleChThr);
       if(H_WMAX.getBinContent(key)>singleChThr) flagsel=true;
        return flagsel;
    }
    
    public void getMaxEnergyEvt(DetectorCollection<Double> adc){
        double maxadc = 0.0;
        int    mkey   = 0;
        
        for(int key: adc.getComponents(0, 0)){
            if(adc.get(0, 0, key)>maxadc){
                maxadc=adc.get(0, 0, key);
                mkey = key;
            }
        }
        for(int key: adc.getComponents(0, 0)){
            if(key==mkey)EnergyEvMax.add(0, 0, key, true);
            else EnergyEvMax.add(0, 0, key, false);
        }
    }
    
    
    
}
