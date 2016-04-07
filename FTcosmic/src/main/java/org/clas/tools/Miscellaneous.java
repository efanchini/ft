/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect.Type;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import org.jlab.clas.detector.DetectorCollection;
import org.root.base.EvioWritableTree;
import org.root.basic.EmbeddedCanvas;
import org.root.func.F1D;
import org.root.group.TDirectory;
import org.root.histogram.H1D;
import org.root.pad.TCanvas;
import org.root.pad.TGCanvas;

/**
 *
 * @author fanchini
 */
public class Miscellaneous {
    public Miscellaneous(){};
       
    Miscellaneous.evioInOutput evioclas = new Miscellaneous.evioInOutput();
   
            
     public String datetime(){
        
        Date date = new Date();// in ms from 1970 //
        // New Foramt Year(2016) Month (01-12) day(01-31) 24h minute(00-60) seconds //
        SimpleDateFormat ft = new SimpleDateFormat ("yyyyMMddHHmmss");
        String sdate = ft.format(date);
         //System.out.println("Current Date: " + sdate);
        return sdate;
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
        
     public String extractstring(String name){
         String extraction="";
         if(!name.isEmpty())extraction= name.substring(0,name.lastIndexOf("."));
         return extraction;
     }
     
    public class evioInOutput{
        
        public DetectorCollection<H1D> H_CHARGE_evio = new DetectorCollection<H1D>();
        public DetectorCollection<F1D> mylandau_evio = new DetectorCollection<F1D>();
        
    
     public void eviofile(DetectorCollection<H1D> h,DetectorCollection<F1D> f, String filename){
        String date = datetime();
        
        // Evio file
        String eviofile = "./"+ date +"_evioHisto.evio";
        if(!filename.isEmpty()){
            eviofile=filename+"_evioHisto.evio";
        }
        String nome = eviofile.replace(".",".0.");// da rimuovere quando il file creato ed 
                                                   // avra' il nome corretto e sostituire con eviofile
                                                             
        File nn = new File(nome);
        if(nn.exists()) nn.delete();
       
        TDirectory evio_dir = new TDirectory();
        TDirectory  evio_histo  = new TDirectory("histograms");
        TDirectory  evio_fct  = new TDirectory("functions");
        for(int key : h.getComponents(0, 0)){
            if(key<=484){
                evio_histo.add(h.get(0, 0, key));
                evio_fct.add(f.get(0, 0, key));
            }
        }
        evio_dir.addDirectory(evio_histo);
        evio_dir.addDirectory(evio_fct);
        
        
        evio_dir.write(eviofile);
        System.out.println("Evio file saved: "+eviofile); 
        //TDirectory d = new TDirectory();
        //d.readFile("/home/fanchini/TEST_evioHisto.0.evio");
        //d.ls();
        H1D hh = (H1D) evio_dir.getDirectory("histograms").getObject("Charge_269");
//        F1D ff = (F1D) d.getDirectory("sector").getObject("f1");
        TCanvas c = new TCanvas("c","c",400,400,1,1);
        c.cd(0);
        c.draw(hh);
//        c.draw(ff,"same");

        this.readeviofile(nome);   
        System.out.println("Erica: BASTA "+this.H_CHARGE_evio.hasEntry(0, 0, 269));
        this.H_CHARGE_evio.get(0, 0, 269).setLineColor(2);
        c.draw(this.H_CHARGE_evio.get(0, 0, 269),"same");
        
        
     }
     
     private void readeviofile(String file){
        
        TDirectory dir = new TDirectory();
        dir.readFile(file);
        //dir.ls();
        
        String directory ="histograms";
        String objectname;
        String directory_fct  ="functions";
        String objectname_fct;
        Object objf;
        Object obj;
        DetectorCollection<H1D> hh = new DetectorCollection<H1D>();
        DetectorCollection<F1D> ff = new DetectorCollection<F1D>();
        for(int key=0; key<484; key++){
            objectname = "Charge_" + key;
            objectname_fct = "mylandau_"+key;
            if(dir.hasDirectory(directory)==true){
                    if(dir.getDirectory(directory).hasObject(objectname)==true){
                        obj = dir.getDirectory(directory).getObject(objectname);
                        if(obj instanceof H1D){
                            hh.add(0, 0, key, (H1D)obj);
                            if(dir.getDirectory(directory_fct).hasObject(objectname_fct)==false){
                                objectname_fct="f1";
                                if(dir.getDirectory(directory_fct).hasObject(objectname_fct)==false){
                                    System.out.println("EVIO==> No fct available for component "+key);
                                    objf = null;
                                }
                                else objf = dir.getDirectory(directory).getObject(objectname_fct);
                            }
                            else objf = dir.getDirectory(directory).getObject(objectname_fct);
                            ff.add(0, 0, key, (F1D)objf);
                        }
                    }
            }
        }
        this.H_CHARGE_evio = hh;
        this.mylandau_evio = ff;
        
     }
}
 
    
//    public void mycanvas(){
//        
//         FTCALViewerModule ft = new FTCALViewerModule();
//         JFrame frame = new JFrame();
//         EmbeddedCanvas c = new EmbeddedCanvas();
//         c.divide(1, 2);
//         c.cd(0);
//         c.setGridX(false);
//         c.setGridY(false);
//         c.draw(ft.H_COSMIC_CHARGE.get(0, 0, 269));
//         c.cd(1);
//         c.draw(ft.H_COSMIC_CHARGE.get(0, 0, 269));
//         
//        frame.add(c);
//        frame.pack();
//        frame.setVisible(true);
//        
//        
//    }
//    
//    
//    public class myCanvasStyle extends EmbeddedCanvas{
//        
//     
//    }
//            
    
}
