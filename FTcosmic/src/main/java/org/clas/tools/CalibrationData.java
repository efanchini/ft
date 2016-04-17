/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import static java.lang.Integer.parseInt;
import java.util.ArrayList;
import java.util.TreeMap;
import org.jlab.clas.detector.DetectorCollection;
import org.root.func.F1D;
import org.root.group.TDirectory;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;
import org.root.histogram.H2D;

/**
 *
 * @author fanchini
 */
public class CalibrationData {

    public DetectorCollection getCollection(String filename, String dirname){
    // The object name must contain the crystal number.                  //
    // It is used to establish the detector collection key of the object //
    // At the moment are ####_id  and the key is key=id                  //
    DetectorCollection DC = new DetectorCollection();
    HipoFile hipofile = new HipoFile(filename);
    int j=0; int key=0;
    ArrayList arrayl = hipofile.getArrayList(dirname);
    for (Object obj : arrayl) {
        String obj_label = getObjlabel(obj);
        if(!extractstring(obj_label).isEmpty()) key = parseInt(extractstring(obj_label));
        else key=j;
        DC.add(0, 0, key, obj);
        j++;
    }
    
    
    return DC;
}    
    
    
private String getObjlabel(Object obj){
        String obj_label ="";
        H1D h_1d; 
        H2D h_2d;
        F1D f_1d; 
        GraphErrors g_err;
        if(obj instanceof H1D){
                h_1d = (H1D) obj;
                obj_label = h_1d.getName();
        }
        if(obj instanceof H2D){
                h_2d = (H2D) obj;
                obj_label = h_2d.getName();
        }
        if(obj instanceof F1D){
                f_1d = (F1D) obj;
                obj_label = f_1d.getName();
        }
        if(obj instanceof H1D){
                h_1d = (H1D) obj;
                obj_label = h_1d.getName();
        }
        if(obj instanceof GraphErrors){
                g_err = (GraphErrors) obj;
                obj_label = g_err.getName();
        }
        return obj_label;
    } 

private String extractstring(String name){
        // Name types like yyy_###  and extract (###) ///
         String extraction="";
         if(!name.isEmpty() && name.lastIndexOf("_")!=-1) extraction= name.substring(name.lastIndexOf("_")+1,name.length());
         return extraction;
     }    
    

 
 
 
  

 
 
}
