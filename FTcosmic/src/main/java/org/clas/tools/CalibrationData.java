/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jlab.clas.detector.DetectorCollection;
import org.root.base.EvioWritableTree;
import org.root.func.F1D;
import org.root.group.TDirectory;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;
import org.root.histogram.H2D;
import org.root.pad.TCanvas;

/**
 *
 * @author fanchini
 */
public class CalibrationData {
    
    
    private TreeMap<String,Object> map = new  TreeMap<String,Object>();
    private TDirectory hdir = new TDirectory();
    public DetectorCollection<H1D> H_extracted = new DetectorCollection<H1D>();
    public DetectorCollection<F1D> F_extracted = new DetectorCollection<F1D>();
      
    
    
    
    public void addToMap(String str, Object h){
        String key=str;
        Object obj = null;
        if(!this.map.isEmpty()){
        for(Map.Entry<String,Object> subdir : this.map.entrySet()){
           if(subdir.getKey()==str){
              key=str+"1";
             }
              else obj=h;
         } 
        }
        else {
            key=str;
            obj=h;
            this.map.put(key,obj);
        }
        this.map.put(key,obj);
        
    }
    
    public void fileWrite(String fileoutput){
       // It is possible to include different types of objects:
       //DetectorCollection or single objects like 1 histogram at the time //
      H1D h=null;
      int j=0;
        for(Map.Entry<String,Object> subdir : this.map.entrySet()){
            TDirectory current = new TDirectory(subdir.getKey());
            String dirName = subdir.getKey();
            System.out.println("Subdir: "+dirName+"  type: "+subdir.getValue());
             if(subdir.getValue() instanceof DetectorCollection){
                 DetectorCollection dc = (DetectorCollection) subdir.getValue();
                 j=0;
                 for(Object obj : dc.getComponents(0, 0)){
                    if(dc.get(0, 0, j) instanceof H1D) current.add((H1D)dc.get(0, 0, j));
                    if(dc.get(0, 0, j) instanceof H2D) current.add((H2D)dc.get(0, 0, j));
                    if(dc.get(0, 0, j) instanceof F1D) current.add((F1D)dc.get(0, 0, j));
                    if(dc.get(0, 0, j) instanceof GraphErrors) current.add((GraphErrors)dc.get(0, 0, j));
                    j++;
                 }
             }
             else {
                    if(subdir.getValue() instanceof H1D) current.add((H1D)subdir.getValue());
                    if(subdir.getValue() instanceof H2D) current.add((H2D)subdir.getValue());
                    if(subdir.getValue() instanceof F1D) current.add((F1D)subdir.getValue());
                    if(subdir.getValue() instanceof GraphErrors) current.add((GraphErrors)subdir.getValue());
                 
             }
             this.hdir.addDirectory(current);
        }
        
        File nn = new File(fileoutput);
        if(nn.exists()) nn.delete();
        this.hdir.writeHipo(fileoutput);
        System.out.println("Hipo file written: "+fileoutput);

    }
    
    
 
      public void readCosmicCalibFile(String file){
        TDirectory dir = new TDirectory();
        this.H_extracted=null;
        this.F_extracted=null;
        File filein = new File(file);
       if(!filein.exists()){
           System.out.println("Hipo File not existing");   
       } 
       else{
        dir.readHipo(file);
        //dir.ls();
        String directory ="histograms";
        String objectname;
        String directory_fct  ="fitfunctions";
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
        this.H_extracted = hh;
        this.F_extracted = ff;
      }
     }
     
 
 public void ls(String file){
     TDirectory dir = new TDirectory();
     dir.readHipo(file);
     dir.ls();
 }  

 
 
 
  public void pippo(String file)
    {
        TDirectory dir = new TDirectory();
        dir.readHipo(file);
        
        for(Map.Entry<String,Object> subdir : this.map.entrySet()){
            TDirectory current = new TDirectory(subdir.getKey());
            String dirName = subdir.getKey();
            System.out.println("Erica: "+dirName+"  "+current.getName());
            System.out.println("Erica 1: "+current.getName()+"  ");
            System.out.println("Erica 2: "+subdir.getKey()+"  ");

         
            }
//            for(Map.Entry<String,Object> obj : current.getObjects().entrySet()){
//                System.out.println("Erica 2:"+obj);
//                
//            }
        //}
        
    }

 
 
}
