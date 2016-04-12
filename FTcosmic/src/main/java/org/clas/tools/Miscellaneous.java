/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jlab.clas.detector.DetectorCollection;
import org.root.func.F1D;
import org.root.histogram.H1D;

/**
 *
 * @author fanchini
 */
public class Miscellaneous {
    public Miscellaneous(){};
       
    //Miscellaneous.evioInOutput evioclas = new Miscellaneous.evioInOutput();
   
            
    public String datetime(){
        
        Date date = new Date();// in ms from 1970 //
        // New Foramt Year(2016) Month (01-12) day(01-31) 24h minute(00-60) seconds //
        SimpleDateFormat ft = new SimpleDateFormat ("yyyyMMddHHmmss");
        String sdate = ft.format(date);
         //System.out.println("Current Date: " + sdate);
        return sdate;
   }
     
    private String extractstring(String name){
        // Name types like yyyyyyyyyyy.yyy.yy.yyy.type  and remove (.type) ///
         String extraction="";
         if(!name.isEmpty())extraction= name.substring(0,name.lastIndexOf("."));
         return extraction;
     }
     
     public String extractFileName(String input, String prj, String type){
         String final_filename =null;
         String str = null;
         String date = datetime();
         
         if(input.isEmpty() && type.isEmpty())System.out.println("ERROR Filename and type not specified!!!");
         else if(input.isEmpty() && !type.isEmpty())final_filename = "./"+date + prj + type;
         else{
             str = input;
             if(!type.isEmpty()){
                 str = extractstring(input);
             }
             final_filename = str + prj + type;
             //System.out.println("Output file: "+final_filename);
         }
         return final_filename;
     }
     
    public void CCDBcosmic(DetectorCollection<F1D> fE, DetectorCollection<H1D> Ped, String name) { 
        // File Format //
        // Component   Charge   Pedestal  Sigma   Preamp-Gain  Photosensor-Gain //
        //
        int preampG =600;// Preamp-Gain
        int pmtG =150;// Photosensor-Gain
//        FTCALViewerModule ftmodule = new FTCALViewerModule();
        double[] pars = new double[3];
        DecimalFormat format = new DecimalFormat("0.00");
        try {
            PrintWriter fout;
            fout = new PrintWriter(name);
            fout.printf("Component \t  Charge \t  Pedestal \t Sigma  \t Preamp-Gain \t Photosensor-Gain");

            //for(int key=0;key<22*22;key++){
            for(int key : fE.getComponents(0, 0)) {
                if(fE.hasEntry(0, 0, key)){
                    
                    pars[0]=fE.get(0,0,key).getParameter(1);// Par1 is the landau mean value //
                   }else pars[0]=0;
                
                if(Ped.hasEntry(0, 0, key)){
                    pars[1]=Ped.get(0,0,key).getMean();// Mean value
                    pars[2]=Ped.get(0,0,key).getRMS();// RMS value 
                }else {
                    pars[1]=0;
                    pars[2]=0;
                }
                for(int i=0; i<pars.length; i++){
                    if(i==0){
                        fout.print(key+"\t"+format.format(pars[i])+"\t");
                        System.out.print(key+"\t"+format.format(pars[i])+"\t");
                    }
                    else if(i==pars.length-1)
                    {
                      fout.print(format.format(pars[i])+"\t"+preampG+"\t"+pmtG+"\n");
                      System.out.print(format.format(pars[i])+"\t"+preampG+"\t"+pmtG+"\n");
                    }else{
                        fout.print(format.format(pars[i])+"\t");
                        System.out.print(format.format(pars[i])+"\t");
                    }
                    
                }
                
            }
            fout.close();
            System.out.println("Cosmic fit file written: "+name);
        } catch (FileNotFoundException ex) {
         
        }
    }  
        
}
