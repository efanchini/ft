/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeView2D;

/**
 *
 * @author devita
 */
public class FTCALView extends DetectorShapeView2D {
    
    String viewName;
    
    private final int nCrystalX = 22;
    private final int nCrystalY = nCrystalX;
    private final int nCrystalComponents = nCrystalX*nCrystalY;
    
    private final double crystal_size = 15;
    
    private double x0=0;
    private double y0=0;
    

    private DetectorCollection<int []> id = new DetectorCollection<int []>();

    public FTCALView(String name) {
        super(name);
        
        int[] ixy = new int[2];
        for (int component = 0; component < nCrystalX*nCrystalY; component++) {
            if(doesThisCrystalExist(component)) {
                ixy[1] = component / nCrystalX;
                ixy[0] = component - ixy[1] * nCrystalX;
                double xcenter = crystal_size * (nCrystalX - ixy[0] - 0.5);
                double ycenter = crystal_size * (nCrystalY - ixy[1] - 0.5 + 1.);
                DetectorShape2D shape = new DetectorShape2D(DetectorType.FTCAL, 0, 0, component);
                id.add(0, 0, component, ixy);
                shape.createBarXY(crystal_size, crystal_size);
                shape.getShapePath().translateXYZ(xcenter+x0, ycenter+y0, 0.0);
                shape.setColor(0, 145, 0);
                this.addShape(shape);           
            }
        }
    }

    
    public void setCenter(double xCenter, double yCenter) {
        this.x0=xCenter;
        this.y0=yCenter;
    }
    
    public void addPaddles() {
        for(int ipaddle=0; ipaddle<4; ipaddle++) {
            DetectorShape2D paddle = new DetectorShape2D(DetectorType.FTCAL, 0, 0, 501+ipaddle);
            paddle.createBarXY(crystal_size*nCrystalX, crystal_size/2.);
            paddle.getShapePath().translateXYZ(crystal_size*nCrystalX/2.,crystal_size*(nCrystalX+2)*(ipaddle % 2)+crystal_size/4.*(((int) ipaddle/2)*2-1),0.0);
            paddle.setColor(0, 145, 0);
            this.addShape(paddle);
        }
    }
    
    public DetectorCollection getIdXY() {
        return this.id;
    }
    
    private boolean doesThisCrystalExist(int id) {

        boolean crystalExist=false;
        int iy = id / nCrystalX;
        int ix = id - iy * nCrystalX;

        double xcrystal = crystal_size * (nCrystalX - ix - 0.5);
        double ycrystal = crystal_size * (nCrystalY - iy - 0.5);
        double rcrystal = Math.sqrt(Math.pow(xcrystal - crystal_size * 11, 2.0) + Math.pow(ycrystal - crystal_size * 11, 2.0));
        if (rcrystal > crystal_size * 4 && rcrystal < crystal_size * 11) {
            crystalExist=true;
        }
        return crystalExist;
    }
    
}
