/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import org.jlab.clas.detector.DetectorCollection;
import org.root.func.F1D;
import org.root.histogram.H1D;

/**
 *
 * @author devita
 */
public class FitData {

        private DetectorCollection<F1D> myfct;
        private DetectorCollection<H1D> histo;
        private String fileName;
    
    public FitData(DetectorCollection<F1D> fFunctions, DetectorCollection<H1D> fHisto) {
        this.myfct = fFunctions;
        this.histo = fHisto;
    }
        
        
    public void writeToFile(String fFile) { 
        this.fileName = fFile;
        // Format //
        // component Pi ErPi(i from 0 to 3) Chi2 NDF N.Entries MyChi2//
        int ip=3; // N. of parameters that we want to have //
        DecimalFormat format = new DecimalFormat("0.00");
        try {
            PrintWriter fout;
            fout = new PrintWriter(this.fileName);
            fout.printf("Component \t Par_i \t Err_Par_i \t Chi2(java) \t NDF \t histo_Entries \n");
            for(int key : histo.getComponents(0, 0)){
                if(myfct.hasEntry(0, 0, key)){
                    fout.print(key+"\t");
                    System.out.print(key+"\t");
                    for(int i=0; i<ip; i++){
                        fout.printf("%.3f \t %.3f \t",myfct.get(0,0,key).getParameter(i),myfct.get(0,0,key).parameter(i).error());
                        System.out.printf("%.3f \t %.3f \t",myfct.get(0,0,key).getParameter(i),myfct.get(0,0,key).parameter(i).error());
                    }    
                    fout.print(format.format(myfct.get(0, 0, key).getChiSquare(histo.get(0,0,key),"NR"))+"\t"+myfct.get(0, 0, key).getNDF(histo.get(0,0,key).getDataSet())
                                    +"\t"+histo.get(0,0,key).getEntries()+"\n");
                    System.out.print(format.format(myfct.get(0, 0, key).getChiSquare(histo.get(0,0,key),"NR"))+"\t"+myfct.get(0, 0, key).getNDF(histo.get(0,0,key).getDataSet())
                                    +"\t"+histo.get(0,0,key).getEntries()+"\n");
                }
                else {
                    for(int i=0; i<ip; i++){
                        fout.printf("0.000 \t 0.000 \t");
                        System.out.printf("0.000 \t 0.000 \t");
                    }
                     fout.print("0.000 \t 0.000 \t"+histo.get(0,0,key).getEntries()+"\n");
                    System.out.print("0.000 \t 0.000 \t"+histo.get(0,0,key).getEntries()+"\n");
                }
            }
            fout.close();
            System.out.println("Fit results written to file: "+fileName);
        } catch (FileNotFoundException ex) {
         
        }
    }
    
    
}
