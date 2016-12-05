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
    
    public FTCalGammaSelection(FTDetector d){
        super(d);
    }
    
  public Boolean SimCosmicEvtSel(int key, Double charge, double simSignalThr){
        //System.out.println("SimCosmicEvtSel "+simSignalThr);
        Boolean flagsel=false;
        if(charge>simSignalThr) flagsel=true;
        return flagsel;
    }  
    
    public Boolean SignalAboveThrSel(DetectorCounter counter, int key, H1D H_WMAX, ExtendedFADCFitter fitter, double singleChThr){
       Boolean flagsel=false;
       fitter.fit(counter.getChannels().get(0),singleChThr);
       if(H_WMAX.getBinContent(key)>singleChThr) flagsel=true;
        return flagsel;
    }
    
    
}
