/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.ftcal;

import java.util.ArrayList;
import java.util.Set;
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
    
    private final double crystal_size = 15;
    
    private double x0=0;
    private double y0=0;
    
    private DetectorCollection<CrystalPoint> id = new DetectorCollection<CrystalPoint>();
    

    public FTCALView(String name) {
        super(name);
        
        for (int component = 0; component < nCrystalX*nCrystalY; component++) {
            if(doesThisCrystalExist(component)) {
                int iy = component / nCrystalX;
                int ix = component - iy * nCrystalX;               
                double xcenter = crystal_size * (nCrystalX - ix - 0.5);
                double ycenter = crystal_size * (nCrystalY - iy - 0.5 + 1.);
                if (ix > 10) {
                    ix = ix - 10;
                } else {
                    ix = ix - 11;
                }
                if (iy > 10) {
                    iy = iy - 10;
                } else {
                    iy = iy - 11;
                }
                id.add(0, 0, component, new CrystalPoint(ix,iy));
                DetectorShape2D shape = new DetectorShape2D(DetectorType.FTCAL, 0, 0, component);
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
            int component = 501+ipaddle;
            id.add(0, 0, component, new CrystalPoint(0,0));
            DetectorShape2D paddle = new DetectorShape2D(DetectorType.FTCAL, 0, 0, 501+ipaddle);
            paddle.createBarXY(crystal_size*nCrystalX, crystal_size/2.);
            paddle.getShapePath().translateXYZ(crystal_size*nCrystalX/2.,crystal_size*(nCrystalX+2)*(ipaddle % 2)+crystal_size/4.*(((int) ipaddle/2)*2-1),0.0);
            paddle.setColor(0, 145, 0);
            this.addShape(paddle);
        }
    }
    
    public void addSync() {
        DetectorShape2D paddle = new DetectorShape2D(DetectorType.FTCAL, 0, 0, 500);
        id.add(0, 0, 500, new CrystalPoint(0,0));
        paddle.createBarXY(crystal_size, crystal_size);
        paddle.getShapePath().translateXYZ(crystal_size*0.5,crystal_size*(22-0.5+1),0.0);
        paddle.setColor(0, 145, 0);
        this.addShape(paddle);
    }
    
    public DetectorCollection getIdXY() {
        return this.id;
    }
    
    public int getIdX(int component) {
        return this.id.get(0, 0, component).x();
    }
    
    public int getIdY(int component) {
        return this.id.get(0, 0, component).y();
    }
    
    public Set<Integer> getViewComponents() {
        return this.id.getComponents(0, 0);
    }
 
    public boolean hasComponent(int component) {
        return this.id.hasEntry(0, 0, component);
    }
    
    public int getNComponents() {
        return this.id.getComponents(0, 0).size();
    }
    
    public int[] getIDArray() {
        int[] crystalID = new int[this.getNComponents()];
        int ipointer=0;
        for(int component : this.id.getComponents(0, 0)) {
            crystalID[ipointer]=component;
            ipointer++;
        }
        return crystalID;
    }
    
    public class CrystalPoint {
        private int x; // the x coordinate
        private int y; // the y coordinate
    
        public CrystalPoint(int x, int y) {
            set(x, y);
        }    

        private void set(int x, int y) {
            setX(x);
            setY(y);
        }

        private void setX(int x) {
            this.x = x;
        }

        private void setY(int y) {
            this.y = y;
        }
        
        public int x() {
            return x;
        }
        
        public int y() {
            return y;
        }
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
