/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Map;
import org.clas.containers.FTHashGenerator;
import org.jlab.clas.detector.DetectorCollection;
import org.root.func.F1D;
import org.root.histogram.H1D;
import org.clas.containers.FTHashTable;

/**
 *
 * @author devita
 */
public class FitData {

     
    public FitData(){}

    
    public void writeFile(String filename, FTHashTable hashtable){
       
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
            System.out.println("FitData => File from HashTable: "+filename);
        }
        catch(FileNotFoundException ex){}
    }
    
    
    
        public void CCDBcosmic(String filename, FTHashTable table) {     
        // File Format //
        // Sector Layer Component Pedestal Noise Energy Sigma  Preamp-Gain  Photosensor-Gain //
        int preampG =600;// Preamp-Gain
        int pmtG =150;// Photosensor-Gain

        try {
            PrintWriter fout;
            fout = new PrintWriter(filename);
            String col="";
            //fout.printf("Sector \t Layer \t Component \t  Pedestal \t Noise \t <E> \t \u03C3(E) \t Preamp-Gain \t Photosensor-Gain");
            for(int r=0; r<table.getRowCount(); r++){
                if(r==0){
                    for(int c=0; c<table.getColumnCount(); c++){
                        col=table.getColumnName(c);
                        if(col.equals("S") || col.equals("L") || col.equals("C") || col.equals("Pedestal") || col.equals("Noise")
                       || col.equals("<E>") || col.equals("\u03C3(E)")) fout.printf(col+"\t");
                    }
                    fout.printf("Preamp-Gain \t Photosensor-Gain \n");
                }
                for(int c=0; c<table.getColumnCount(); c++){
                    col=table.getColumnName(c);
                    if(col.equals("S") || col.equals("L") || col.equals("C") || col.equals("Pedestal") || col.equals("Noise")
                       || col.equals("<E>") || col.equals("\u03C3(E)")){
                        fout.printf(table.getValueAt(r, c)+"\t");
                    }
                }
                fout.printf(preampG+"\t"+ pmtG +"\n");
                }
            fout.close();
            System.out.println("FitData => CCDB file written: "+filename);
        } catch (FileNotFoundException ex) {
         
        }
    }  
        
        
    
}
