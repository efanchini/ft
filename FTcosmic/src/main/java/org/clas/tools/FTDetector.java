/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.util.Set;
import org.jlab.clas12.calib.DetectorShapeView2D;

/**
 *
 * @author devita
 */
public abstract class FTDetector extends DetectorShapeView2D {

    public FTDetector(String name) {
        super(name);
    }
    
    public abstract Set<Integer> getDetectorComponents() ;
    
    public abstract boolean hasComponent(int component);  
    
    public abstract int getNComponents(); // return the total number of components
    
    
}
