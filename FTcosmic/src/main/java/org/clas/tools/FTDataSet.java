/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import org.jlab.clas.detector.DetectorCollection;
import org.root.func.F1D;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;

/**
 *
 * @author devita
 */
public class FTDataSet {
    FTDetector ft;

    public FTDataSet(FTDetector d) {
        this.ft = d;
    }
    
    public DetectorCollection addCollection(H1D histo) {
        DetectorCollection h = new DetectorCollection();
        for(int key : this.ft.getDetectorComponents()) {
            H1D histoComponent = histo.histClone(histo.getName() + "_" + key);
//            histoComponent.setTitle("Component " + key + " (" + ft.ge + "," + iy + ")";);
            h.add(0,0,key,histoComponent);
        }
        return h;
    }

    public DetectorCollection addCollection(H1D histo, String XTitle, String YTitle, int Col) {
        DetectorCollection h = new DetectorCollection();
        for(int key : this.ft.getDetectorComponents()) {
            H1D histoComponent = histo.histClone(histo.getName() + "_" + key);
            h.add(0,0,key,histoComponent);
        }
        return h;
    }

    public DetectorCollection addCollection(F1D funct) {
        DetectorCollection f = new DetectorCollection();
        for(int key : this.ft.getDetectorComponents()) {
            F1D functComponent = new F1D(funct.getName(),funct.getMin(),funct.getMax());
            f.add(0,0,key,functComponent);
        }
        return f;
    }
    
    public DetectorCollection addCollection(GraphErrors graf) {
        DetectorCollection g = new DetectorCollection();
        for(int key : this.ft.getDetectorComponents()) {
            GraphErrors graphComponent = new GraphErrors();
            graphComponent.setName(graf.getName());
            g.add(0,0,key,graphComponent);
        }
        return g;
    }

}
