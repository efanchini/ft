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
public class FTCalCosmicSelection extends FTApplication{
    
    public FTCalCosmicSelection(FTDetector d){
        super(d);
    }
    
    
    public Boolean HorizonthalSel(DetectorCounter counter, int key, int ncry_cosmic, H1D H_WMAX, ExtendedFADCFitter fitter, double singleChThr){
       //System.out.println("HorizonthalSel "+singleChThr);
       Boolean flagsel=false;  
       int ix  = this.getDetector().getIX(key);
       int iy  = this.getDetector().getIY(key);
       int nCrystalInColumn = 0;
       //System.out.println("Erica: "+counter.getChannels().get(0)+"  "+this.getDetector().getThresholds().get(0, 0, key));
       //fitter.fit(counter.getChannels().get(0),this.getDetector().getThresholds().get(0, 0, key));
       //only for runs 714 -717 -718 //
//      if(key==9 || key==10 || key==31 || key==32|| key==53||key==54 ||key==75||key==76||key==97
//              || key==98||key==118||key==119||key==120||key==140||key==141){
//         fitter.setPulseRange(65, 100);
//        
//      }
       fitter.fit(counter.getChannels().get(0),singleChThr);
       int i1=(int) max(0,iy-ncry_cosmic);    // allowing for +/- to cope with dead channels
       int i2=iy+ncry_cosmic;
                for(int i=i1; i<=i2; i++) {
                    int component = this.getDetector().getComponent(ix, i);
                    if(i!=iy && this.getDetector().hasComponent(component)) {
                        //if(H_WMAX.getBinContent(component)>this.getDetector().getThresholds().get(0, 0, component) ) nCrystalInColumn++; 
                        if(H_WMAX.getBinContent(component)>singleChThr) nCrystalInColumn++; 
                    }
                }
                if(nCrystalInColumn>=ncry_cosmic) {
                    flagsel=true;
                }

        return flagsel;
    }
    
  public Boolean SimCosmicEvtSel(int key, Double charge, double simSignalThr){
        //System.out.println("SimCosmicEvtSel "+simSignalThr);
        Boolean flagsel=false;
        if(charge>simSignalThr) flagsel=true;
        return flagsel;
    }  
    
    public Boolean SignalAboveThrSel(DetectorCounter counter, int key, H1D H_WMAX, ExtendedFADCFitter fitter, double singleChThr){
        //System.out.println("SignalAboveThrSel "+singleChThr);
        Boolean flagsel=false;
       //fitter.fit(counter.getChannels().get(0),this.getDetector().getThresholds().get(0, 0, key));
       fitter.fit(counter.getChannels().get(0),singleChThr);
       //if(H_WMAX.getBinContent(key)>this.getDetector().getThresholds().get(0, 0, key)) flagsel=true;
       
       if(H_WMAX.getBinContent(key)>singleChThr) flagsel=true;
        return flagsel;
    }
    
    public Boolean VerticalCosmicSel(DetectorCounter counter, int key, H1D H_WMAX, ExtendedFADCFitter fitter, double clusterThr, double singleChThr){
       
       Boolean flagsel=false;
       // Looks for cluster events and rejects them to look for ONLY vertical cosmic events //
       int ix  = this.getDetector().getIX(key);
       int iy  = this.getDetector().getIY(key);
       int component = key;
       int cluster=0; 
       //fitter.fit(counter.getChannels().get(0),this.getDetector().getThresholds().get(0, 0, key));
       fitter.fit(counter.getChannels().get(0),singleChThr);
       //if(H_WMAX.getBinContent(key)>this.getDetector().getThresholds().get(0, 0, key)) flagsel=true;
       if(H_WMAX.getBinContent(key)>singleChThr) flagsel=true;
       if(flagsel){
           for(int i=ix-1; i<=ix+1; i++) {
               if(i<0 || i>21)continue;
               for(int j=iy-1; j<=iy+1; j++) {
                   if(j<0 || j>21)continue;
                   if(i!=ix || j!=iy){
                      component = this.getDetector().getComponent(i, j);
                      if(this.getDetector().hasComponent(component)) {
                         if(H_WMAX.getBinContent(component)>clusterThr)cluster++;
                         //if(key==300)System.out.println("VerticalCosmicSel c: "+key+" "+ix+" "+iy+" CC: "+component+" "+ i+" "+j+" "+H_WMAX.getBinContent(key)+"  "+H_WMAX.getBinContent(component)+"  "+cluster);
                      }
                   }
               } 
            }
        if(cluster>0)flagsel=false;
       }
       //if (flagsel && key==176)System.out.println("VerticalCosmicSel "+singleChThr+" "+clusterThr+"  "+H_WMAX.getBinContent(key));
       
       return flagsel;
    }
    
    public Boolean TestSel(DetectorCounter counter, int key, H1D H_WMAX, DetectorCollection<H1D> H_COSMIC_fADC, ExtendedFADCFitter fitter,int evt, double singleChThr){
       //System.out.println("TestSel "+singleChThr);
       Boolean flagsel=false;
       int Athr=0;
       int Alow=0;
       fitter.fit(counter.getChannels().get(0),singleChThr);
       short pulse[] = counter.getChannels().get(0).getPulse();
     
        for (int i = 0; i < Math.min(pulse.length, H_COSMIC_fADC.get(0, 0, key).getAxis().getNBins()); i++) {
             if((pulse[i]-fitter.getPedestal())>800)Athr++;
             if((pulse[i]-fitter.getPedestal())<0.0)Alow++;
         }
        if(Athr>=4 && Alow>=4)flagsel=false;
        else flagsel=true;
                  
                    
        return flagsel;
    }
    
  public Boolean LargeEventRemoval(List<DetectorCounter> counters, H1D H_WMAX, ExtendedFADCFitter fitter, double singleChThr){
      Boolean flagLE = false;
      int mpt =0;
      //System.out.println("LargeEventRemoval "+singleChThr);
      for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            if(this.getDetector().hasComponent(key)) {
               //if(H_WMAX.getBinContent(key)>this.getDetector().getThresholds().get(0, 0, key)){
               if(H_WMAX.getBinContent(key)>1000.0){
                   //if(H_WMAX.getBinContent(key)>singleChThr){
                   mpt++;
               }
            }
         }
      if(mpt>220){
          flagLE=true;
      }//1/2 FTCal + 2/3
      return flagLE;
  }  
    
}
