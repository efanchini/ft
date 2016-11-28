/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
            Cosmic fit based on 1 landau + exponencial functions
            // Format //
            // Sector Layer Component N. Events NFiPar Amp E Sigma(E) err_sigma(E) P0  P1  P2  Fct_Xmin  Fct_XmaxChi^2 NDF// 
        */
       this.fname=filename;
       PrintWriter filetowrite = new PrintWriter(this.fname);
       this.file=filetowrite;
       System.out.println("FitParametersFile=> Cosmic Fit Parameters File: "+ this.fname);
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
    
            public void CCDBcosmic(String filename, FTHashTable table) {     
        // File Format //
        // Da scrivere in 67 diverse tabelle//
        // Sector Layer Component Pedestal Ped_Rms Noise Noise_Rms Energy Sigma Threshold Ch_Status Preamp-Gain  Photosensor-Gain //
        int preampG =700;// Preamp-Gain
        int pmtG =150;// Photosensor-Gain

        try {
            PrintWriter fout;
            fout = new PrintWriter(filename);
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
                    }
                    fout.printf("Preamp-Gain \t Photosensor-Gain \n");
                }
                for(int c=0; c<table.getColumnCount(); c++){
                    col=table.getColumnName(c);
                    //if(col.equals("S") || col.equals("L") || col.equals("C") || col.equals("Pedestal") || col.equals("Noise")
                      // || col.equals("<E>") || col.equals("\u03C3(E)")){
                        fout.printf(table.getValueAt(r, c)+"\t");
                        //System.out.println("PIPPO "+c+" "+col+" "+table.getValueAt(r, c));
                   // }
                }
                fout.printf(preampG+"\t"+ pmtG +"\n");
                }
            fout.close();
            System.out.println("FitParametersFile => CCDB file written: "+filename);
        } catch (FileNotFoundException ex) {
         
        }
    }  
 
    
}
