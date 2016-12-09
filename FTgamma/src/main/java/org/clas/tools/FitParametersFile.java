/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.clas.containers.FTHashTable;
import org.jlab.clas.detector.DetectorCollection;
import org.root.func.F1D;
import org.root.histogram.H1D;


/**
 *
 * @author fanchini
 */
public class FitParametersFile {
    
    private  String fname;
    private  PrintWriter file;
    private ArrayList<DetectorCollection<F1D>> fct   = new ArrayList<DetectorCollection<F1D>>();
    private ArrayList<DetectorCollection<H1D>> histo = new ArrayList<DetectorCollection<H1D>>();
    
    
    public FitParametersFile(){  
    }
    
    public void add(DetectorCollection<F1D> ff, DetectorCollection<H1D> hh){
        this.fct.add(ff);
        this.histo.add(hh);
    }
    
    
    public void writeFitLandauFile(String filename) throws FileNotFoundException {
        /*
            Fit based on 1 landau + exponencial functions
            // Format //
            // Sector Layer Component N. Events NFiPar Amp E Sigma(E) err_sigma(E) P0  P1  P2  Fct_Xmin  Fct_XmaxChi^2 NDF// 
        */
       this.fname=filename;
       PrintWriter filetowrite = new PrintWriter(this.fname);
       this.file=filetowrite;
       System.out.println("FitParametersFile=> Fit Parameters File: "+ this.fname);
        PrintWriter fout = this.file;
        int nevt=0;
        String dd = "";       
        for(int i=0; i<this.fct.size(); i++){
            fout.printf("Sector \t Layer \t Component \t N.Events \t NParametersFit(#=0-i) \t Pi \t ErrPi \t ... \t fct_XMin \t fct_XMax \t \u03C7\u00B2 \t NDF \n");
            for(int key : this.histo.get(i).getComponents(0, 0)) {
                H1D hh = this.histo.get(i).get(0, 0, key);
                F1D ff = this.fct.get(i).get(0, 0, key);
                nevt=hh.getEntries();
                fout.printf("0 \t 0 \t"+key+"\t"+nevt+"\t"+ff.getNParams()+"\t");
                for(int j=0; j<ff.getNParams(); j++){
                    dd=String.format("%.3f",ff.getParameter(j));
                    fout.printf(dd+"\t");
                    dd=String.format("%.4f",ff.getParError(j));
                    fout.printf(dd+"\t");
                }
                dd=String.format("%.1f",ff.getDataRegion().MINIMUM_X);
                fout.printf(dd+"\t");
                dd=String.format("%.1f",ff.getDataRegion().MAXIMUM_X);
                fout.printf(dd+"\t");
                dd =String.format("%.3f",ff.getChiSquare(hh,"NR"));
                fout.printf(dd+"\t"+ff.getNDF(hh.getDataSet())+"\n");  
            }  
        }
        fout.close();
    }    
    
    public void writeSummaryTFile(String filename, FTHashTable hashtable){
        // Create File form the HashTable SummaryTable //
        FTHashTable table = hashtable;
        // Format //
        // Sector Layer Component Pedestal Noise N. Events E Sigma(E)  Chi^2  T  Sigma(T) // 
        try {
            PrintWriter fout;
            fout = new PrintWriter(filename);
            String col="";
            for(int r=0; r<table.getRowCount(); r++){
                if(r==0){
                    for(int c=0; c<table.getColumnCount(); c++){
                        col=table.getColumnName(c);
                        fout.printf(col+"\t");
                    }
                        fout.println();
                }
                for(int c=0; c<table.getColumnCount(); c++){
                    fout.printf(table.getValueAt(r, c)+"\t");
                    }
                fout.printf("\n");
                }
            fout.close();
            System.out.println("FitParametersFile => File from HashTable: "+filename);
        }
        catch(FileNotFoundException ex){}
    }
    
    public void CCDBFile(String filename, FTHashTable table) {     
        // File Format //
        // Da scrivere in diverse tabelle//
        // Sector Layer Component Pedestal Ped_Rms Noise Noise_Rms N.Events, Energy, E Sigma, Chi, Time, rms,  Ch_Status, Threshold Preamp-Gain  Photosensor-Gain //
        int preampG = 670;//(int)(1800/2.7);// Preamp-Gain
        int pmtG =150;// Photosensor-Gain
        

        try {
            PrintWriter fout;// Full table //
            fout = new PrintWriter(filename);
            
            PrintWriter fout_status;// Status table //
            fout_status = new PrintWriter("./ftcal_status.txt");
            
            PrintWriter fout_timeoffset;// Time offset table //
            fout_timeoffset = new PrintWriter("./ftcal_time_offset.txt");
            
            // Da implementare //
//            PrintWriter fout_noise;// Noise table //
//            fout_noise = new PrintWriter("./ftcal_noise.txt");          
//            
//            PrintWriter fout_q2e;// Chareg to energy table //
//            fout_q2e = new PrintWriter("./ftcal_charge_to_energy.txt");

            String col="";
            //fout.printf("Sector \t Layer \t Component \t  Pedestal \t RMS \t Noise \t RMS \t <E> \t \u03C3(E) \t Threshold \t Status \t Preamp-Gain \t Photosensor-Gain");
            for(int r=0; r<table.getRowCount(); r++){
                if(r==0){
                    for(int c=0; c<table.getColumnCount(); c++){
                        //System.out.println("PARAMETERs name: "+table.getColumnName(c));
                        col=table.getColumnName(c);
                        //if(col.equals("S") || col.equals("L") || col.equals("C") || col.equals("Pedestal") || col.equals("Noise")
                       //|| col.equals("<E>") || col.equals("\u03C3(E)")) 
                            fout.printf(col+"\t");
                            if(col.equals("S") || col.equals("L") || col.equals("C")|| col.equals("Status"))fout_status.printf(col+"\t");
                            if(col.equals("S") || col.equals("L") || col.equals("C")|| col.equals("<T>")|| col.equals("\u03C3(T)"))fout_timeoffset.printf(col+"\t");
                    }
                    fout.printf("Preamp-Gain \t Photosensor-Gain \n");
                    fout_status.printf("\n");
                    fout_timeoffset.printf("\n");
                }
                
                for(int c=0; c<table.getColumnCount(); c++){
                    col=table.getColumnName(c);
                    fout.printf(table.getValueAt(r, c)+"\t");
                    if(col.equals("S") || col.equals("L") || col.equals("C")){
                         fout_status.printf(table.getValueAt(r, c)+"\t");
                         fout_timeoffset.printf(table.getValueAt(r, c)+"\t");
                     }
                   if(col.equals("Status")){
                       Double pp =Double.parseDouble(table.getValueAt(r, c).toString());
                       fout_status.printf(pp.intValue()+"\t");
                   }
                   if(col.equals("<T>")|| col.equals("\u03C3(T)"))fout_timeoffset.printf(table.getValueAt(r, c)+"\t");
                }
                fout.printf(preampG+"\t"+ pmtG +"\n");
                fout_status.printf("\n");
                fout_timeoffset.printf("\n");
                
                }
            fout.close();
            fout_status.close();
            //fout_noise.close();
            fout_timeoffset.close();
            //fout_q2e.close();
            
            System.out.println("FitParametersFile => CCDB file written: "+filename);
        } catch (FileNotFoundException ex) {
         
        }
    }  
            
            
public void readCCDBFiles() throws IOException{
     String[] fileIn = new String[2];
        fileIn[0] ="/project/Gruppo3/fiber6/fanchini/JLab/FwdT/analysis/ft/FTgamma/ftcal_time_offset.txt";
        fileIn[1] ="/project/Gruppo3/fiber6/fanchini/JLab/FwdT/analysis/ft/FTgamma/ftcal_time_offset_Raffa.txt";
        
        for(int i=0; i<fileIn.length; i++){
            System.out.println("ERICA1: "+fileIn[i]);
            readFile(fileIn[i]);
        }
        
        
}

    private ArrayList<DetectorCollection> readFile(String file) throws IOException{
        
        ArrayList<DetectorCollection> values = new ArrayList<DetectorCollection>();
        DetectorCollection dc = new DetectorCollection();
        ArrayList al = new ArrayList();
                
        BufferedReader br = null;
            try {
               File ff = new File(file);
               if(ff.exists()){
                FileReader fin = new FileReader(file);
                br = new BufferedReader(fin);
               }
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            Integer nl=0, ni=0; Double nd=0.0;
            String[] scol = null;
            Integer comp=0;
            ArrayList<List> ch = new ArrayList<List>();
            String tt ="";
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
//                if(nl==0){
//                    System.out.println("ERICA: "+line);
//                        }
            if(line != null){
               scol = line.split("\t");
               int nc=0;
                for(int i=2; i<scol.length; i++){
                    if(i==2){
                        comp=Integer.parseInt((String)scol[i]);
                    }
                  al.add(nc, scol[i]);      
                 System.out.println("ERICA1: "+i+"  "+scol[i]);
                 nc++;
               }
                
            nl++;
            }
           }
//            for(int c=0; c<scol.length; c++){
//                    values.add(c, dc);
//            }
            
            for(int i=0; i<values.size();i++){
                System.out.println("Erica: "+i+"  "+values.get(i).getName());
            for (Iterator it = values.get(i).getComponents(0, 0).iterator(); it.hasNext();) {
                Integer j = (Integer) it.next();
                System.out.println("  ERICA 2: "+i+"  "+j+"  "+values.get(i).get(0, 0, j));
            } 
            
            }
            
            return values;
    }
 
    
            
 
    
}
