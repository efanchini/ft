/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jlab.clas.detector.DetectorCollection;
import org.root.func.F1D;
import org.root.group.TBrowser;
import org.root.group.TDirectory;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;
import org.root.histogram.H2D;

/**
 *
 * @author fanchini
 */
public class HipoFile {
    
    private final String file;
    private final TreeMap<String,Object>         map            = new TreeMap<String,Object>();
    private final TDirectory                     hdirw          = new TDirectory();
    private final TDirectory                     hdirr          = new TDirectory();
    private final TreeMap<String,Object>      map_exctracted;
    
    public HipoFile(String filename){
       this.map_exctracted = new TreeMap<String,Object>();
       this.file=filename;
       File filein = new File(this.file);
       if(filein.exists()){
           hdirr.readHipo(this.file);
           readHipoFile();
       } 
       
    }
    
    public void addToMap(String str, Object h){
        String key=str;
        Object obj = null;
        if(!this.map.isEmpty()){
            for(Map.Entry<String,Object> subdir : this.map.entrySet()){
                if(subdir.getKey().equals(str)){                    
                    ArrayList alnew = new ArrayList();
                    if(subdir.getValue() instanceof ArrayList){
                        ArrayList al = (ArrayList) subdir.getValue();
                        for(int i=0; i<al.size(); i++){alnew.add(al.get(i));}
                    }
                    else 
                    key=str;
                    alnew.add(h);
                    obj=alnew.clone();
                    this.map.remove(key);
                }
                else {
                    key=str;
                    obj=h;
                }
            } 
        }
        else {
            key=str;
            obj=h;
             
        }
        this.map.put(key,obj);
    }

    public void writeHipoFile(String filename){
       // It is possible to include different types of objects:
       //DetectorCollection or single objects like 1 histogram at the time //
       System.out.println("WRITE TO FILE:");
      H1D h=null;
      int j;
        for(Map.Entry<String,Object> subdir : this.map.entrySet()){
            TDirectory current = new TDirectory(subdir.getKey());
            String dirName = subdir.getKey();
             if(subdir.getValue() instanceof DetectorCollection){
                 DetectorCollection dc = (DetectorCollection) subdir.getValue();
                 for(Object obj : dc.getComponents(0, 0)){
                    int ii =(Integer)obj;
                    //System.out.println("HIPO DetectorCollection ");
                    if(dc.get(0, 0, ii) instanceof H1D) current.add((H1D)dc.get(0, 0, ii));
                    if(dc.get(0, 0, ii) instanceof H2D) current.add((H2D)dc.get(0, 0, ii));
                    if(dc.get(0, 0, ii) instanceof F1D) current.add((F1D)dc.get(0, 0, ii));
                    if(dc.get(0, 0, ii) instanceof GraphErrors) current.add((GraphErrors)dc.get(0, 0, ii));
                 }
             }
            if(subdir.getValue() instanceof ArrayList){
                    ArrayList al = (ArrayList)subdir.getValue();
                    //System.out.println("HIPO ArrayList");
                    for(int k=0; k<al.size(); k++){
                        if(al.get(k) instanceof H1D) current.add((H1D)al.get(k));
                        if(al.get(k) instanceof H2D) current.add((H2D)al.get(k));
                        if(al.get(k) instanceof F1D) current.add((F1D)al.get(k));
                        if(al.get(k) instanceof GraphErrors) current.add((GraphErrors)al.get(k));
                    }
             }
             else {
                    if(subdir.getValue() instanceof H1D) current.add((H1D)subdir.getValue());
                    if(subdir.getValue() instanceof H2D) current.add((H2D)subdir.getValue());
                    if(subdir.getValue() instanceof F1D) current.add((F1D)subdir.getValue());
                    if(subdir.getValue() instanceof GraphErrors) current.add((GraphErrors)subdir.getValue());
             }
             this.hdirw.addDirectory(current);
        }
        
        File nn = new File(filename);
        if(nn.exists()) nn.delete();
        this.hdirw.writeHipo(filename);
        readHipoFile();
        System.out.println("Hipo file written: "+filename);
    }
        
       private void readHipoFile()
    {
        if(!this.map_exctracted.isEmpty())this.map_exctracted.clear();
        System.out.println("Reading Hipo file: "+this.file);
        Enumeration sdir  = this.hdirr.getTree().children();
        while (sdir.hasMoreElements()){
            String dirname = sdir.nextElement().toString();
            TDirectory newdir = this.hdirr.getDirectory(dirname);
            ArrayList all = new ArrayList();
            for(Map.Entry<String,Object> ss : newdir.getObjects().entrySet()){
                Object obj =ss.getValue();
                all.add(obj);
         }
            Object f = all;
            this.map_exctracted.put(dirname, f);
            ArrayList tt = (ArrayList)this.map_exctracted.get(dirname);
        }
    }
  
public void browsFile(String filename){
    TDirectory d = new TDirectory();
    d.readHipo(filename);
    TBrowser br = new TBrowser(d);
}    
    

public ArrayList getArrayList(String subdirname){
    ArrayList AL = new ArrayList();
    int nd=0, nde=0;
    for(Map.Entry<String,Object> ss : this.map_exctracted.entrySet()){
            String key = ss.getKey();
            if(key.equals(subdirname)){
                AL = (ArrayList) this.map_exctracted.get(key);
            }else nde++;
            nd++;
     }
    if(nde==nd)System.out.println("SUB-DIRECTORY NOT FOUND ");
    return AL;
}


    
 public void ls(){
     this.hdirr.ls();
 }  
    
 public int getNsubdirs(){
    int ndirs=0;
   Enumeration sdir  = this.hdirr.getTree().children();
   while (sdir.hasMoreElements()){
       String dirname = sdir.nextElement().toString();
       ndirs++;
   }
   return ndirs;
 }
    
 public List getNsubdirNames(){
      List names = new ArrayList();
      int i=0;
      if(this.map_exctracted.isEmpty())System.out.println("No subdirectories");
      for(Map.Entry<String,Object> ss : this.map_exctracted.entrySet()){
          names.add(i, ss.getKey());
          i++;
        }
    return names;
    }

}