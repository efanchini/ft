/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import java.util.ArrayList;
import org.clas.tools.FTApplication;
import org.clas.tools.FTDetector;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.evio.clas12.EvioDataBank;
import org.jlab.evio.clas12.EvioDataEvent;
import org.jlab.evio.clas12.EvioDataSync;
import org.jlab.evio.clas12.EvioFactory;

/**
 *
 * @author fanchini
 */
public class FTCALSimulatedData{// extends FTApplication{
    private EvioDataBank eviobank= null;
    private EvioDataEvent evt;
    private DetectorCollection<Double> sim_adc = new DetectorCollection<Double>();
    private DetectorCollection<Double> sim_tdc = new DetectorCollection<Double>();
    EvioDataBank bank = null;
    
//    public FTCALSimulatedData(FTDetector d) {
//        super(d);
//    }
    
  public FTCALSimulatedData() {
    }
  

// public void setEvent(EvioDataEvent event){
//     this.evt = event;
//     System.out.println("HELP");
//     this.evt.show();
// }
  
    public void eventBankDecoder(EvioDataEvent event, String bank){
            eviobank = (EvioDataBank) event.getBank(bank);
            if(bank=="FTCAL::dgtz")ftcalBank(bank);
            else {
                System.out.println("Different bank not included");
            }
            //eviobank.show(); // printout the content of the bank
    }
    
   private void ftcalBank(String bank){
        clearSimcollection();
        
        double adc=0.0, tdc=0.0;
        int s=0, l=0, c=0;
        for (int row = 0; row < this.eviobank.rows(); row++) {
  
            s=this.eviobank.getInt("sector"   , row);
            l=this.eviobank.getInt("layer"    , row);
            c=this.eviobank.getInt("component", row);
                
            adc = (double)this.eviobank.getInt("ADC", row);
            tdc = (double)this.eviobank.getInt("TDC", row);
           
            //System.out.println("TEST: => slot "+s+" layer "+l+" component "+c+" adc: "+adc+" tdc: "+tdc);
            
            this.sim_adc.add(0, 0, c, adc);
            this.sim_tdc.add(0, 0, c, tdc);
            //if(c==130 || c==155|| c==291 ||c==391)System.out.println("Erica Sim1: sim_ad.size:"+row+" "+s+"  "+l+"  "+c
            //    +"  "+adc+"  "+tdc);
            }
        //System.out.println("Erica Sim1: sim_ad.size:"+this.sim_adc.getComponents(0, 0).size());
   }
   
 
               
      public DetectorCollection<Double> getSimAdc() {
          //System.out.println("Erica Sim2: sim_ad.size:"+this.sim_adc.getComponents(0, 0).size());
        return this.sim_adc;
    }
        
       public DetectorCollection<Double> getSimTdc() {
         //System.out.println("Erica Sim2: sim_ad.size:"+this.sim_tdc.getComponents(0, 0).size());
        return this.sim_tdc;
    }
       
    
   public void writeBank(ArrayList<ArrayList> data){
       
            int nrows = data.get(0).size();
            //EvioDataBank 
               bank = EvioFactory.createBank("FTCAL::dgtz", nrows);
            for(int loop = 0; loop < nrows; loop++){
                bank.setInt("sector",    loop, 1);
                bank.setInt("layer",     loop, 1);
                bank.setInt("component", loop, Integer.parseInt(data.get(0).get(loop).toString()));
                bank.setInt("ADC",       loop, (Integer)data.get(1).get(loop));
                bank.setInt("TDC",       loop, (Integer)data.get(2).get(loop));
                bank.setInt("hitn",      loop, loop);
            }
            EvioDataSync writer = new EvioDataSync();
            EvioDataEvent outevent = writer.createEvent(EvioFactory.getDictionary());
            outevent.appendBank(bank);
            this.evt=outevent;
    } 
   
   public EvioDataEvent getEvent(){
       return this.evt;
   }
   public EvioDataBank getBank(){
       return this.bank;
   }

       
    public void clearSimcollection(){
        this.sim_adc.clear();
        this.sim_tdc.clear();
    }
    
}
