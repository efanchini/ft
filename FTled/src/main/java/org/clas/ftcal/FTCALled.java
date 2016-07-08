package org.clas.ftcal;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jlab.clas.detector.DetectorCollection;  
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeTabView;
import org.jlab.clas12.calib.DetectorShapeView2D;
import org.jlab.clas12.calib.IDetectorListener;
import org.jlab.clas12.detector.DetectorChannel;
import org.jlab.clas12.detector.DetectorCounter;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.clas12.detector.FADCBasicFitter;
import org.jlab.clas12.detector.IFADCFitter;
import org.jlab.containers.HashTable;
import org.jlab.containers.HashTableViewer;
import org.jlab.containers.IHashTableListener;
import org.root.attr.ColorPalette;
import org.root.func.F1D;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;
import org.root.basic.EmbeddedCanvas;
import org.root.data.DataVector;
import org.root.base.PaveText;

public class FTCALled implements IDetectorListener,IHashTableListener,ActionListener,ChangeListener{

    // panels and canvases
    JPanel detectorPanel;
    ColorPalette palette = new ColorPalette();
    EmbeddedCanvas canvasEvent     = new EmbeddedCanvas();
    EmbeddedCanvas canvasNoise     = new EmbeddedCanvas();
    EmbeddedCanvas canvasCharge    = new EmbeddedCanvas();
    EmbeddedCanvas canvasAmpli     = new EmbeddedCanvas();
    EmbeddedCanvas canvasWidth     = new EmbeddedCanvas();
    EmbeddedCanvas canvasTime      = new EmbeddedCanvas();
    DetectorShapeTabView view = new DetectorShapeTabView();
    HashTable  summaryTable   = null; 
    
    // histograms, functions and graphs
    DetectorCollection<H1D> H_fADC = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_WAVE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_WAVE_PED = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_WAVE_PUL = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_PED  = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NOISE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_LED_fADC   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_LED_CHARGE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_LED_VMAX   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_LED_WIDTH   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_LED_TCROSS  = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_LED_THALF   = new DetectorCollection<H1D>();
    H1D hfADC      = null;
    H1D H_fADC_N   = null;
    H1D H_WMAX     = null;
    H1D H_TCROSS   = null;
    H1D H_LED_N = null;
    
    DetectorCollection<F1D> mylandau = new DetectorCollection<F1D>();
    DetectorCollection<F1D> myTimeGauss = new DetectorCollection<F1D>();
    
    DetectorCollection<GraphErrors> G_LED_CHARGE        = new DetectorCollection<GraphErrors>();
    DetectorCollection<GraphErrors> G_LED_CHARGE_SELECT = new DetectorCollection<GraphErrors>();
    DetectorCollection<GraphErrors> G_LED_AMPLI         = new DetectorCollection<GraphErrors>();
    DetectorCollection<GraphErrors> G_LED_AMPLI_SELECT  = new DetectorCollection<GraphErrors>();
    DetectorCollection<GraphErrors> G_LED_WIDTH         = new DetectorCollection<GraphErrors>();

    double[] ledCharge;
    double[] ledCharge2;
    double[] ledAmpli;
    double[] ledAmpli2;
    double[] ledWidth;
    double[] ledWidth2;
    int[]    ledEvent;    
    int[]    ledNEvents;
    int nLedEvents=50;
    int nLedSkip=10;

    DetectorCollection<GraphErrors> G_PULSE_ANALYSIS = new DetectorCollection<GraphErrors>();
    
    double[] crystalID; 
    double[] pedestalMEAN;
    double[] noiseRMS;
    double[] cosmicCharge;
    double[] timeCross;
    double[] timeHalf;
    double[] fullWidthHM;
    int[] crystalPointers;    
    
    // decoded related information
    int nProcessed = 0;
    EventDecoder decoder;
    
    public EventDecoder getDecoder() {
        return decoder;
    }
    
    public void setDecoder(EventDecoder decoder) {
        this.decoder = decoder;
    }


    // analysis parameters
    int threshold = 50; // 10 fADC value <-> ~ 5mV
    int ped_i1 = 1;
    int ped_i2 = 20;
    int pul_i1 = 21;
    int pul_i2 = 60;
    double nsPerSample=4;
    double LSB = 0.4884;
    double crystal_size = 15;


    // control variables
    private int plotSelect = 0;  // 0 - waveforms, 1 - noise
    private int keySelect = 8;
    private boolean debugFlag=false;

    
    
    public FTCALled(){
        this.detectorPanel=null;
        this.decoder=null;
    }


    public JPanel getDetectorPanel() {
        return detectorPanel;
    }

    public void setDetectorPanel(JPanel detectorPanel) {
        this.detectorPanel = detectorPanel;
    }

    public void initPanel() {
        // detector panel consists of a split pane with detector view and tabbed canvases
        JSplitPane splitPane = new JSplitPane();

        // the last tab will contain the summmary table
        this.initTable();
        HashTableViewer canvasTable = new HashTableViewer(summaryTable);
        canvasTable.addListener(this);
        
        // create Tabbed Panel
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Event Viewer",this.canvasEvent);
        tabbedPane.add("Noise"       ,this.canvasNoise);
        tabbedPane.add("Charge"      ,this.canvasCharge);
        tabbedPane.add("Amplitude"   ,this.canvasAmpli);
        tabbedPane.add("Width"       ,this.canvasWidth);
        tabbedPane.add("Time"        ,this.canvasTime);
        tabbedPane.add("Summary"     ,canvasTable);
        tabbedPane.addChangeListener(this);
        this.initCanvas();
        
        JPanel canvasPane = new JPanel();

        canvasPane.setLayout(new BorderLayout());
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());

        JButton resetBtn = new JButton("Clear Histograms");
        resetBtn.addActionListener(this);
        buttonPane.add(resetBtn);

        JButton fitBtn = new JButton("Fit Histograms");
        fitBtn.addActionListener(this);
        buttonPane.add(fitBtn);

        JButton tableBtn = new JButton("Update Summary");
        tableBtn.addActionListener(this);
        buttonPane.add(tableBtn);

        JRadioButton wavesRb  = new JRadioButton("Waveforms");
        JRadioButton noiseRb  = new JRadioButton("Noise");
        JRadioButton cosmicOccRb = new JRadioButton("Cosmics(Occ)");
        JRadioButton cosmicCrgRb = new JRadioButton("Cosmics(Fit)");
        ButtonGroup group = new ButtonGroup();
        group.add(wavesRb);
        group.add(noiseRb);
        group.add(cosmicOccRb);
        group.add(cosmicCrgRb);
//        buttonPane.add(wavesRb);
//        buttonPane.add(noiseRb);
//        buttonPane.add(cosmicOccRb);
//        buttonPane.add(cosmicCrgRb);
        wavesRb.setSelected(true);
        wavesRb.addActionListener(this);
        noiseRb.addActionListener(this);
        cosmicOccRb.addActionListener(this);
        cosmicCrgRb.addActionListener(this);

        canvasPane.add(tabbedPane, BorderLayout.CENTER);
        canvasPane.add(buttonPane, BorderLayout.PAGE_END);
    
        splitPane.setLeftComponent(this.view);
        splitPane.setRightComponent(canvasPane);
 
        this.detectorPanel.add(splitPane, BorderLayout.CENTER);

    }

    public void initCanvas() {
        // event canvas
        this.canvasEvent.setGridX(false);
        this.canvasEvent.setGridY(false);
        this.canvasEvent.setAxisFontSize(10);
        this.canvasEvent.setTitleFontSize(16);
        this.canvasEvent.setAxisTitleFontSize(14);
        this.canvasEvent.setStatBoxFontSize(8);
        // noise
        this.canvasNoise.divide(2, 2);
        this.canvasNoise.cd(0);
        this.canvasNoise.setGridX(false);
        this.canvasNoise.setGridY(false);
        this.canvasNoise.setAxisFontSize(10);
        this.canvasNoise.setTitleFontSize(16);
        this.canvasNoise.setAxisTitleFontSize(14);
        this.canvasNoise.setStatBoxFontSize(8);
        this.canvasNoise.cd(1);
        this.canvasNoise.setGridX(false);
        this.canvasNoise.setGridY(false);
        this.canvasNoise.setAxisFontSize(10);
        this.canvasNoise.setTitleFontSize(16);
        this.canvasNoise.setAxisTitleFontSize(14);
        this.canvasNoise.setStatBoxFontSize(8);
        this.canvasNoise.cd(2);
        this.canvasNoise.setGridX(false);
        this.canvasNoise.setGridY(false);
        this.canvasNoise.setAxisFontSize(10);
        this.canvasNoise.setTitleFontSize(16);
        this.canvasNoise.setAxisTitleFontSize(14);
        this.canvasNoise.setStatBoxFontSize(8);
        this.canvasNoise.cd(3);
        this.canvasNoise.setGridX(false);
        this.canvasNoise.setGridY(false);
        this.canvasNoise.setAxisFontSize(10);
        this.canvasNoise.setTitleFontSize(16);
        this.canvasNoise.setAxisTitleFontSize(14);
        this.canvasNoise.setStatBoxFontSize(8);
        // charge
        this.canvasCharge.divide(2, 2);
        this.canvasCharge.cd(0);
        this.canvasCharge.setGridX(false);
        this.canvasCharge.setGridY(false);
        this.canvasCharge.setAxisFontSize(10);
        this.canvasCharge.setTitleFontSize(16);
        this.canvasCharge.setAxisTitleFontSize(14);
        this.canvasCharge.setStatBoxFontSize(8);
        this.canvasCharge.cd(1);
        this.canvasCharge.setGridX(false);
        this.canvasCharge.setGridY(false);
        this.canvasCharge.setAxisFontSize(10);
        this.canvasCharge.setTitleFontSize(16);
        this.canvasCharge.setAxisTitleFontSize(14);
        this.canvasCharge.setStatBoxFontSize(8);
        this.canvasCharge.cd(2);
        this.canvasCharge.setGridX(false);
        this.canvasCharge.setGridY(false);
        this.canvasCharge.setAxisFontSize(10);
        this.canvasCharge.setTitleFontSize(16);
        this.canvasCharge.setAxisTitleFontSize(14);
        this.canvasCharge.setStatBoxFontSize(8);
        this.canvasCharge.cd(3);
        this.canvasCharge.setGridX(false);
        this.canvasCharge.setGridY(false);
        this.canvasCharge.setAxisFontSize(10);
        this.canvasCharge.setTitleFontSize(16);
        this.canvasCharge.setAxisTitleFontSize(14);
        this.canvasCharge.setStatBoxFontSize(8);
        // amplitude
        this.canvasAmpli.divide(2, 2);
        this.canvasAmpli.cd(0);
        this.canvasAmpli.setGridX(false);
        this.canvasAmpli.setGridY(false);
        this.canvasAmpli.setAxisFontSize(10);
        this.canvasAmpli.setTitleFontSize(16);
        this.canvasAmpli.setAxisTitleFontSize(14);
        this.canvasAmpli.setStatBoxFontSize(8);
        this.canvasAmpli.cd(1);
        this.canvasAmpli.setGridX(false);
        this.canvasAmpli.setGridY(false);
        this.canvasAmpli.setAxisFontSize(10);
        this.canvasAmpli.setTitleFontSize(16);
        this.canvasAmpli.setAxisTitleFontSize(14);
        this.canvasAmpli.setStatBoxFontSize(8);
        this.canvasAmpli.cd(2);
        this.canvasAmpli.setGridX(false);
        this.canvasAmpli.setGridY(false);
        this.canvasAmpli.setAxisFontSize(10);
        this.canvasAmpli.setTitleFontSize(16);
        this.canvasAmpli.setAxisTitleFontSize(14);
        this.canvasAmpli.setStatBoxFontSize(8);
        this.canvasAmpli.cd(3);
        this.canvasAmpli.setGridX(false);
        this.canvasAmpli.setGridY(false);
        this.canvasAmpli.setAxisFontSize(10);
        this.canvasAmpli.setTitleFontSize(16);
        this.canvasAmpli.setAxisTitleFontSize(14);
        this.canvasAmpli.setStatBoxFontSize(8);
        // Width
        this.canvasWidth.divide(2, 2);
        this.canvasWidth.cd(0);
        this.canvasWidth.setGridX(false);
        this.canvasWidth.setGridY(false);
        this.canvasWidth.setAxisFontSize(10);
        this.canvasWidth.setTitleFontSize(16);
        this.canvasWidth.setAxisTitleFontSize(14);
        this.canvasWidth.setStatBoxFontSize(8);
        this.canvasWidth.cd(1);
        this.canvasWidth.setGridX(false);
        this.canvasWidth.setGridY(false);
        this.canvasWidth.setAxisFontSize(10);
        this.canvasWidth.setTitleFontSize(16);
        this.canvasWidth.setAxisTitleFontSize(14);
        this.canvasWidth.setStatBoxFontSize(8);
        this.canvasWidth.cd(2);
        this.canvasWidth.setGridX(false);
        this.canvasWidth.setGridY(false);
        this.canvasWidth.setAxisFontSize(10);
        this.canvasWidth.setTitleFontSize(16);
        this.canvasWidth.setAxisTitleFontSize(14);
        this.canvasWidth.setStatBoxFontSize(8);
        this.canvasWidth.cd(3);
        this.canvasWidth.setGridX(false);
        this.canvasWidth.setGridY(false);
        this.canvasWidth.setAxisFontSize(10);
        this.canvasWidth.setTitleFontSize(16);
        this.canvasWidth.setAxisTitleFontSize(14);
        this.canvasWidth.setStatBoxFontSize(8);
        // time
        this.canvasTime.divide(2, 2);
        this.canvasTime.cd(0);
        this.canvasTime.setGridX(false);
        this.canvasTime.setGridY(false);
        this.canvasTime.setAxisFontSize(10);
        this.canvasTime.setTitleFontSize(16);
        this.canvasTime.setAxisTitleFontSize(14);
        this.canvasTime.setStatBoxFontSize(8);
        this.canvasTime.cd(1);
        this.canvasTime.setGridX(false);
        this.canvasTime.setGridY(false);
        this.canvasTime.setAxisFontSize(10);
        this.canvasTime.setTitleFontSize(16);
        this.canvasTime.setAxisTitleFontSize(14);
        this.canvasTime.setStatBoxFontSize(8);
        this.canvasTime.cd(2);
        this.canvasTime.setGridX(false);
        this.canvasTime.setGridY(false);
        this.canvasTime.setAxisFontSize(10);
        this.canvasTime.setTitleFontSize(16);
        this.canvasTime.setAxisTitleFontSize(14);
        this.canvasTime.setStatBoxFontSize(8);
        this.canvasTime.cd(3);
        this.canvasTime.setGridX(false);
        this.canvasTime.setGridY(false);
        this.canvasTime.setAxisFontSize(10);
        this.canvasTime.setTitleFontSize(16);
        this.canvasTime.setAxisTitleFontSize(14);
        this.canvasTime.setStatBoxFontSize(8);
    }

    
    private void initTable() {
//        summaryTable = new HashTable(3,"Pedestal (ch):d","Noise (mV):i","LED Mean (pC):d","LED Sigma(%):d","Time Mean (nS):d","Time Sigma (nS):d");
        summaryTable = new HashTable(3,"Charge Mean (pC):d","Charge Sigma (pC):d","Amp. Mean (mV):d","Amp. Sigma (mV):d");
        double[] summaryInitialValues = {-1, -1,-1, -1};
        for (int component = 0; component < 22*22; component++) {
            if(doesThisCrystalExist(component)) {
                summaryTable.addRow(summaryInitialValues,0,0,component);
//                summaryTable.addConstrain(3, 130.0, 240.0);
//                summaryTable.addConstrain(4, 1.0, 1.55); 
                summaryTable.addConstrain(3, 100.0, 1500.); 
                summaryTable.addConstrain(4, 0.1, 10.);
                summaryTable.addConstrain(5, 100.0, 1500.); 
                summaryTable.addConstrain(6, 0.1, 10.);
//                summaryTable.addConstrain(7, 100, 200.);
            }
        }
    }
    
    public void initDetector() {
        DetectorShapeView2D viewFTCAL = this.drawDetector(0.,0.);
        this.view.addDetectorLayer(viewFTCAL);
        view.addDetectorListener(this);
    }

    public DetectorShapeView2D drawDetector(double x0, double y0) {
        DetectorShapeView2D viewFTCAL = new DetectorShapeView2D("FTCAL");
        // draw calorimeter
        for (int component = 0; component < 22*22; component++) {
            if(doesThisCrystalExist(component)) {
                int iy = component / 22;
                int ix = component - iy * 22;
                double xcenter = crystal_size * (22 - ix - 0.5);
                double ycenter = crystal_size * (22 - iy - 0.5 + 1.);
                DetectorShape2D shape = new DetectorShape2D(DetectorType.FTCAL, 0, 0, component);
                shape.createBarXY(crystal_size, crystal_size);
                shape.getShapePath().translateXYZ(xcenter+x0, ycenter+y0, 0.0);
                shape.setColor(0, 145, 0);
                viewFTCAL.addShape(shape);               
            }
        }  
        // add small square for sync signal
        DetectorShape2D paddle = new DetectorShape2D(DetectorType.FTCAL, 0, 0, 500);
        paddle.createBarXY(crystal_size, crystal_size);
        paddle.getShapePath().translateXYZ(crystal_size*0.5,crystal_size*(22-0.5+1),0.0);
        paddle.setColor(0, 145, 0);
        viewFTCAL.addShape(paddle);
//        for(int ipaddle=0; ipaddle<4; ipaddle++) {
//            DetectorShape2D paddle = new DetectorShape2D(DetectorType.FTCAL, 0, 0, 501+ipaddle);
//            paddle.createBarXY(crystal_size*11, crystal_size/2.);
//            paddle.getShapePath().translateXYZ(crystal_size*11/2.*(((int) ipaddle/2)*2+1),crystal_size*(22+2)*(ipaddle % 2),0.0);
//            paddle.setColor(0, 145, 0);
//            viewFTCAL.addShape(paddle);
//        }
        return viewFTCAL;
    }

    private boolean doesThisCrystalExist(int id) {

        boolean crystalExist=false;
        int iy = id / 22;
        int ix = id - iy * 22;

        double xcrystal = (22 - ix - 0.5);
        double ycrystal = (22 - iy - 0.5);
        double rcrystal = Math.sqrt(Math.pow(xcrystal - 11, 2.0) + Math.pow(ycrystal - 11, 2.0));
        if (rcrystal > 4 && rcrystal < 11) {
            crystalExist=true;
        }
        return crystalExist;
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        System.out.println("FTCALViewerModule ACTION = " + e.getActionCommand());
        if (e.getActionCommand().compareTo("Clear Histograms") == 0) {
            resetHistograms();
        }
        if (e.getActionCommand().compareTo("Fit Histograms") == 0) {
            fitHistograms();
        }
        if (e.getActionCommand().compareTo("Update Summary") == 0) {
            updateTable();
        }

//        if (e.getActionCommand().compareTo("Waveforms") == 0) {
//            plotSelect = 0;
//            resetCanvas();
//        } else if (e.getActionCommand().compareTo("Noise") == 0) {
//            plotSelect = 1;
//        } else if (e.getActionCommand().compareTo("Cosmics(Occ)") == 0) {
//            plotSelect = 2;
//        } else if (e.getActionCommand().compareTo("Cosmics(Fit)") == 0) {
//            plotSelect = 3;
//        }

    }

     public void stateChanged(ChangeEvent e) {
        JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
        int index = sourceTabbedPane.getSelectedIndex();
        System.out.println("Tab changed to: " + sourceTabbedPane.getTitleAt(index) + " with index " + index);
        plotSelect = index;
        this.updateTable();
        this.view.repaint();
    }

    public void initHistograms() {
        for (int component = 0; component < 505; component++) {
            if(doesThisCrystalExist(component) || component>499) {

                int iy = component / 22;
                int ix = component - iy * 22;
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
                String title = "Crystal " + component + " (" + ix + "," + iy + ")";
                H_fADC.add(0, 0, component, new H1D("fADC_" + component, title, 100, 0.0, 100.0));
                H_PED.add(0, 0, component, new H1D("Pedestal_" + component, title, 200, 130., 250.0));
                H_PED.get(0, 0, component).setFillColor(2);
                H_PED.get(0, 0, component).setXTitle("Pedestal (fADC counts)");
                H_PED.get(0, 0, component).setYTitle("Counts");                        
                H_NOISE.add(0, 0, component, new H1D("Noise_" + component, title, 200, 0.0, 10.0));
                H_NOISE.get(0, 0, component).setFillColor(4);
                H_NOISE.get(0, 0, component).setXTitle("RMS (mV)");
                H_NOISE.get(0, 0, component).setYTitle("Counts");  
                H_WAVE.add(0, 0, component, new H1D("Wave_" + component, title, 100, 0.0, 100.0));
                H_WAVE.get(0, 0, component).setFillColor(5);
                H_WAVE.get(0, 0, component).setXTitle("fADC Sample");
                H_WAVE.get(0, 0, component).setYTitle("fADC Counts");
                H_WAVE_PED.add(0, 0, component, new H1D("Wave_PED_" + component, title, 100, 0.0, 100.0));
                H_WAVE_PED.get(0, 0, component).setLineStyle(2);
                H_WAVE_PED.get(0, 0, component).setXTitle("fADC Sample");
                H_WAVE_PED.get(0, 0, component).setYTitle("fADC Counts");
                H_WAVE_PUL.add(0, 0, component, new H1D("Wave_PUL_" + component, title, 100, 0.0, 100.0));
                H_WAVE_PUL.get(0, 0, component).setLineStyle(4);
                H_WAVE_PUL.get(0, 0, component).setXTitle("fADC Sample");
                H_WAVE_PUL.get(0, 0, component).setYTitle("fADC Counts");
                H_LED_fADC.add(0, 0, component, new H1D("FADC_" + component, title, 100, 0.0, 100.0));
                H_LED_fADC.get(0, 0, component).setFillColor(3);
                H_LED_fADC.get(0, 0, component).setXTitle("fADC Sample");
                H_LED_fADC.get(0, 0, component).setYTitle("fADC Counts");
                H_LED_CHARGE.add(0, 0, component, new H1D("Charge_" + component, title, 300, 0.0, 1500.0));
                H_LED_CHARGE.get(0, 0, component).setFillColor(2);
                H_LED_CHARGE.get(0, 0, component).setXTitle("Charge (pC)");
                H_LED_CHARGE.get(0, 0, component).setYTitle("Counts");
                H_LED_VMAX.add(0, 0, component, new H1D("VMax_" + component, title, 300, 0.0, 1500.0));
                H_LED_VMAX.get(0, 0, component).setFillColor(2);
                H_LED_VMAX.get(0, 0, component).setXTitle("Amplitude (mV)");
                H_LED_VMAX.get(0, 0, component).setYTitle("Counts");
                H_LED_WIDTH.add(0, 0, component, new H1D("Width_" + component, title, 300, 0.0, 100.0));
                H_LED_WIDTH.get(0, 0, component).setFillColor(2);
                H_LED_WIDTH.get(0, 0, component).setXTitle("FWHM (ns)");
                H_LED_WIDTH.get(0, 0, component).setYTitle("Counts");
                H_LED_TCROSS.add(0, 0, component, new H1D("T_TRIG_" + component, title, 200, 60.0, 160.0));
                H_LED_TCROSS.get(0, 0, component).setFillColor(5);
                H_LED_TCROSS.get(0, 0, component).setXTitle("Time (ns)");
                H_LED_TCROSS.get(0, 0, component).setYTitle("Counts");
                H_LED_THALF.add(0, 0, component, new H1D("T_TRIG_" + component, title, 200, 60.0, 160.0));
                H_LED_THALF.get(0, 0, component).setFillColor(5);
                H_LED_THALF.get(0, 0, component).setXTitle("Time (ns)");
                H_LED_THALF.get(0, 0, component).setYTitle("Counts"); 
                // graphs
                G_LED_CHARGE.add(0,0,component, new GraphErrors());
                G_LED_CHARGE.get(0, 0, component).setTitle(" "); //  title
                G_LED_CHARGE.get(0, 0, component).setXTitle("Event");             // X axis title
                G_LED_CHARGE.get(0, 0, component).setYTitle("LED Charge (pC)");   // Y axis title
                G_LED_CHARGE.get(0, 0, component).setMarkerColor(1); // color from 0-9 for given palette
                G_LED_CHARGE.get(0, 0, component).setMarkerSize(5); // size in points on the screen
                G_LED_CHARGE.get(0, 0, component).setMarkerStyle(1); // Style can be 1 or 2 
                G_LED_CHARGE_SELECT.add(0,0,component, new GraphErrors());
                G_LED_CHARGE_SELECT.get(0, 0, component).setTitle(" "); //  title
                G_LED_CHARGE_SELECT.get(0, 0, component).setXTitle("Event");             // X axis title
                G_LED_CHARGE_SELECT.get(0, 0, component).setYTitle("LED Charge (pC)");   // Y axis title
                G_LED_CHARGE_SELECT.get(0, 0, component).setMarkerColor(2); // color from 0-9 for given palette
                G_LED_CHARGE_SELECT.get(0, 0, component).setMarkerSize(5); // size in points on the screen
                G_LED_CHARGE_SELECT.get(0, 0, component).setMarkerStyle(1); // Style can be 1 or 2 
                G_LED_AMPLI.add(0,0,component, new GraphErrors());
                G_LED_AMPLI.get(0, 0, component).setTitle(" "); //  title
                G_LED_AMPLI.get(0, 0, component).setXTitle("Event");             // X axis title
                G_LED_AMPLI.get(0, 0, component).setYTitle("LED Amplitude (mV)");   // Y axis title
                G_LED_AMPLI.get(0, 0, component).setMarkerColor(1); // color from 0-9 for given palette
                G_LED_AMPLI.get(0, 0, component).setMarkerSize(5); // size in points on the screen
                G_LED_AMPLI.get(0, 0, component).setMarkerStyle(1); // Style can be 1 or 2 
                G_LED_AMPLI_SELECT.add(0,0,component, new GraphErrors());
                G_LED_AMPLI_SELECT.get(0, 0, component).setTitle(" "); //  title
                G_LED_AMPLI_SELECT.get(0, 0, component).setXTitle("Event");             // X axis title
                G_LED_AMPLI_SELECT.get(0, 0, component).setYTitle("LED Amplitude (mV)");   // Y axis title
                G_LED_AMPLI_SELECT.get(0, 0, component).setMarkerColor(2); // color from 0-9 for given palette
                G_LED_AMPLI_SELECT.get(0, 0, component).setMarkerSize(5); // size in points on the screen
                G_LED_AMPLI_SELECT.get(0, 0, component).setMarkerStyle(1); // Style can be 1 or 2 
                G_LED_WIDTH.add(0,0,component, new GraphErrors());
                G_LED_WIDTH.get(0, 0, component).setTitle(" "); //  title
                G_LED_WIDTH.get(0, 0, component).setXTitle("Event");             // X axis title
                G_LED_WIDTH.get(0, 0, component).setYTitle("LED FWHM (ns)");   // Y axis title
                G_LED_WIDTH.get(0, 0, component).setMarkerColor(1); // color from 0-9 for given palette
                G_LED_WIDTH.get(0, 0, component).setMarkerSize(5); // size in points on the screen
                G_LED_WIDTH.get(0, 0, component).setMarkerStyle(1); // Style can be 1 or 2 
                G_PULSE_ANALYSIS.add(0,0,component, new GraphErrors());
                G_PULSE_ANALYSIS.get(0, 0, component).setTitle(" "); //  title
                G_PULSE_ANALYSIS.get(0, 0, component).setXTitle("Event");             // X axis title
                G_PULSE_ANALYSIS.get(0, 0, component).setYTitle("LED Amplitude (mV)");   // Y axis title
                G_PULSE_ANALYSIS.get(0, 0, component).setMarkerColor(2); // color from 0-9 for given palette
                G_PULSE_ANALYSIS.get(0, 0, component).setMarkerSize(5); // size in points on the screen
                G_PULSE_ANALYSIS.get(0, 0, component).setMarkerStyle(1); // Style can be 1 or 2 
                // fit functions
                mylandau.add(0, 0, component, new F1D("landau",     0.0, 40.0));
                myTimeGauss.add(0, 0, component, new F1D("gaus", -20.0, 60.0));
            }
        }
        H_fADC_N   = new H1D("fADC"  , 504, 0, 504);
        H_WMAX     = new H1D("WMAX"  , 504, 0, 504);
        H_TCROSS   = new H1D("TCROSS", 504, 0, 504);
        H_LED_N    = new H1D("EVENT" , 504, 0, 504);

        crystalID       = new double[332];
        pedestalMEAN    = new double[332];
        noiseRMS        = new double[332];
        timeCross       = new double[332];
        timeHalf        = new double[332];
        fullWidthHM     = new double[332];
        crystalPointers = new int[484];
        ledCharge  = new double[484];
        ledCharge2 = new double[484];
        ledAmpli   = new double[484];
        ledAmpli2  = new double[484];
        ledWidth   = new double[484];
        ledWidth2  = new double[484];
        ledEvent   = new int[484];
        ledNEvents = new int[484];                
        int ipointer=0;
        for(int i=0; i<484; i++) {
            if(doesThisCrystalExist(i)) {
                crystalPointers[i]=ipointer;
                crystalID[ipointer]=i;
                ipointer++;
            }
            else {
                crystalPointers[i]=-1;
            }
            ledEvent[i]   = -1;
            ledNEvents[i] =  0;
        }


    }

    private void initLandauFitPar(int key, H1D hcosmic) {
        if(hcosmic.getBinContent(0)==0) mylandau.add(0, 0, key, new F1D("landau",     0.0, 40.0));
        else                            mylandau.add(0, 0, key, new F1D("landau+exp", 0.0, 40.0));
        if(hcosmic.getBinContent(0)<10) {
            mylandau.get(0, 0, key).setParameter(0, hcosmic.getBinContent(hcosmic.getMaximumBin()));
        }
        else {
            mylandau.get(0, 0, key).setParameter(0, 10);
        }
        mylandau.get(0, 0, key).setParameter(1,hcosmic.getMean());
        mylandau.get(0, 0, key).setParLimits(1, 5.0, 30.);        
        mylandau.get(0, 0, key).setParameter(2,2);
        mylandau.get(0, 0, key).setParLimits(2, 0.5, 10);
        if(hcosmic.getBinContent(0)!=0) {
            mylandau.get(0, 0, key).setParameter(3,hcosmic.getBinContent(0));
            mylandau.get(0, 0, key).setParameter(4, -0.2);
        }
    }

    private void initTimeGaussFitPar(int key, H1D htime) {
        double hAmp  = htime.getBinContent(htime.getMaximumBin());
        double hMean = htime.getMean();
        double hRMS  = htime.getRMS();        
        double rangeMin = hMean - 3*hRMS; 
        double rangeMax = hMean + 3*hRMS;         
        myTimeGauss.add(0, 0, key, new F1D("gaus", rangeMin, rangeMax));
        myTimeGauss.get(0, 0, key).setParameter(0, hAmp);
        myTimeGauss.get(0, 0, key).setParLimits(0, hAmp*0.8, hAmp*1.2);
        myTimeGauss.get(0, 0, key).setParameter(1, hMean);       
        myTimeGauss.get(0, 0, key).setParameter(2, 2.);
    }    
    
    private void fitHistograms() {
        for(int key=0; key< 22*22; key++) {
            if(H_LED_CHARGE.hasEntry(0, 0, key)) {
                if(H_LED_CHARGE.get(0, 0, key).getEntries()>200) {
                    H1D hcosmic = H_LED_CHARGE.get(0,0,key);
                    initLandauFitPar(key,hcosmic);
                    hcosmic.fit(mylandau.get(0, 0, key),"L");
                    H1D htime = H_LED_THALF.get(0,0,key);
                    initTimeGaussFitPar(key,htime);
                    htime.fit(myTimeGauss.get(0, 0, key),"L");
                }
            }   
        }
        boolean flag_parnames=true;
        for(int key=0; key< 22*22; key++) {
            if(mylandau.hasEntry(0, 0, key)) {
                if(flag_parnames) {
                    System.out.println("Component\t amp\t mean\t sigma\t p0\t p1\t Chi2");
                    flag_parnames=false;
                }
                System.out.print(key + "\t\t ");
                for(int i=0; i<mylandau.get(0, 0, key).getNParams(); i++) System.out.format("%.2f\t ",mylandau.get(0, 0, key).getParameter(i));
                if(mylandau.get(0, 0, key).getNParams()==3) System.out.print("0.0\t 0.0\t");
                double perrors = mylandau.get(0, 0, key).parameter(0).error();
                if(mylandau.get(0, 0, key).getParameter(0)>0)
                    System.out.format("%.2f\n",mylandau.get(0, 0, key).getChiSquare(H_LED_CHARGE.get(0,0,key).getDataSet())
                            /mylandau.get(0, 0, key).getNDF(H_LED_CHARGE.get(0,0,key).getDataSet()));
                else
                    System.out.format("0.0\n");
            }
        }
    }

    public void resetHistograms() { 
        for (int component = 0; component < 505; component++) {
            if(H_fADC.hasEntry(0, 0, component)) {
                H_fADC.get(0, 0, component).reset();
                H_NOISE.get(0, 0, component).reset();
                H_LED_fADC.get(0, 0, component).reset();
                H_LED_CHARGE.get(0, 0, component).reset();
                H_LED_VMAX.get(0, 0, component).reset();
                H_LED_TCROSS.get(0, 0, component).reset();
                H_LED_THALF.get(0, 0, component).reset();
                H_LED_WIDTH.get(0, 0, component).reset();
            }
            H_fADC_N.reset();
            H_LED_N.reset();
        }
        // TODO Auto-generated method stub
    }
    
    public void resetWave(int component) { 
        if(H_WAVE.hasEntry(0, 0, component)) {
            H_WAVE.get(0, 0, component).reset();
            H_WAVE_PED.get(0, 0, component).reset();
            H_WAVE_PUL.get(0, 0, component).reset();
        }
    }
                
    public void initDecoder() {
        decoder.addFitter(DetectorType.FTCAL,
                new FADCBasicFitter(ped_i1, // first bin for pedestal
                        ped_i2, // last bin for pedestal
                        pul_i1, // first bin for pulse integral
                        pul_i2 // last bin for pulse integral
                        ));    
    }

    public void hashTableCallback(String string, Long l) {
        // ToDO
        System.out.println("Selected table row " + string + " " + l);
    }

    public class MyADCFitter implements IFADCFitter {

        private double rms = 0;
        private double pedestal = 0;
        private double wave_max=0;
        private double pulse_max=0;
        private double half_max=0;
        private int    thresholdCrossing=0;
        private int    pulsePosition=0;
        private double time_3 = 0;
        private double time_7 = 0;
        private double time_f = 0;
        private double width = 0;

        public double getPedestal() {
            return pedestal;
        }

        public double getRMS() {
            return rms;
        }

        public double getWave_Max() {
            return wave_max;
        }

        public double getPulse_Max() {
            return pulse_max;
        }

        public double getHalf_Max() {
            return half_max;
        }

        public int getThresholdCrossing() {
            return thresholdCrossing;
        }

        public int getPulsePosition() {
            return pulsePosition;
        }
        
        public double getTime(int mode) {
            double time=0;
            if(mode==3)       time = time_3;
            else if(mode==7)  time = time_7;
            else System.out.println(" Unknown mode for time calculation, check...");
            return time;
        }

        public double getTimeF() {
            return time_f;
        }

        public double getFWHM() {
            return width;
        }

        public void fit(DetectorChannel dc) {
            short[] pulse = dc.getPulse();
            double ped    = 0.0;
            double noise  = 0;
            double wmax   = 0;
            double pmax   = 0;
            int    tcross = 0; 
            int    ppos   = 0;
            // calculate pedestal means and noise
            for (int bin = ped_i1; bin < ped_i2; bin++) {
                ped += pulse[bin];
                noise += pulse[bin] * pulse[bin];
            }
            pedestal = ped / (ped_i2 - ped_i1);
            rms = LSB * Math.sqrt(noise / (ped_i2 - ped_i1) - pedestal * pedestal);
            // determine waveform max
            for (int bin=0; bin<pulse.length; bin++) {
                if(pulse[bin]>wmax) wmax=pulse[bin];
            }
            wave_max=wmax;
            // find threshold crossing in pulse region: this determines mode-3 time (4 ns resolution)
            for (int bin=pul_i1; bin<pul_i2; bin++) {
                if(pulse[bin]>pedestal+threshold) {
                    tcross=bin;
                    break;
                }
            }
            thresholdCrossing=tcross;
            time_3=tcross*nsPerSample;
            // find pulse max
            for (int bin=thresholdCrossing; bin<pulse.length; bin++) { 
                if (pulse[bin+1]<pulse[bin]){ 
                    pmax=pulse[bin];
                    ppos=bin;
                    break; 
                }
            }
            pulse_max=pmax;
            pulsePosition=ppos;
            // calculating high resolution time    
            double halfMax = (pmax+pedestal)/2;
            half_max = halfMax;
            time_7 = time_3;
            int t0 = -1;
            int t1 = -1;
            if(tcross>0) { 
                for (int bin=tcross-1; bin<pul_i2; bin++) {
                    if (pulse[bin]<=halfMax && pulse[bin+1]>halfMax) {
                        t0 = bin;
                        break;
                    }
                }
                for (int bin=ppos; bin<pul_i2; bin++) {
                    if (pulse[bin]>halfMax && pulse[bin+1]<=halfMax) {
                        t1 = bin;
                        break;
                    }
                }
                if(t0>-1) { 
//                    int t1 = t0 + 1;
                    int a0 = pulse[t0];
                    int a1 = pulse[t0+1];
                       //final double slope = (a1 - a0); // units = ADC/sample
                       //final double yint = a1 - slope * t1;  // units = ADC 
                    time_7 = ((halfMax - a0)/(a1-a0) + t0)* nsPerSample;
                }
                if(t1>-1 && t0>-1) {
                    int a0 = pulse[t1];
                    int a1 = pulse[t1+1];
                    time_f = t1*nsPerSample;//((halfMax - a0)/(a1-a0) + t1)* nsPerSample;
                    width  = time_f - time_7;
                }
            }
       }

    }

    public void processDecodedEvent() {
        // TODO Auto-generated method stub

        nProcessed++;

        //    System.out.println("event #: " + nProcessed);
        List<DetectorCounter> counters = decoder.getDetectorCounters(DetectorType.FTCAL);
        System.out.println(counters.size());
        FTCALled.MyADCFitter fadcFitter = new FTCALled.MyADCFitter();
        H_WMAX.reset();
        H_TCROSS.reset();
        double tPMTCross=0;
        double tPMTHalf=0;
        G_PULSE_ANALYSIS.clear();
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            //                System.out.println(counters.size() + " " + key + " " + counter.getDescriptor().getComponent());
            //                 System.out.println(counter);
            fadcFitter.fit(counter.getChannels().get(0));
            short pulse[] = counter.getChannels().get(0).getPulse();
            H_fADC_N.fill(key);
            this.resetWave(key);
            for (int i = 0; i < Math.min(pulse.length, H_fADC.get(0, 0, key).getAxis().getNBins()); i++) {
                H_fADC.get(0, 0, key).fill(i, pulse[i] - fadcFitter.getPedestal() + 10.0);
                H_WAVE.get(0, 0, key).fill(i, pulse[i]);
                if(i>ped_i1 && i<=ped_i2) H_WAVE_PED.get(0, 0, key).fill(i, pulse[i]);
                if(i>pul_i1 && i<=pul_i2) H_WAVE_PUL.get(0, 0, key).fill(i, pulse[i]);
            }
            H_WMAX.fill(key,fadcFitter.getWave_Max()-fadcFitter.getPedestal());
            H_TCROSS.fill(key,fadcFitter.getTime(3));
                //            if(fadcFitter.getWave_Max()-fadcFitter.getPedestal()>threshold) 
                //            System.out.println("   Component #" + key + " is above threshold, max=" + fadcFitter.getWave_Max() + " ped=" + fadcFitter.getPedestal());
            H_PED.get(0, 0, key).fill(fadcFitter.getPedestal());
            H_NOISE.get(0, 0, key).fill(fadcFitter.getRMS());
            // save relevant info in pulse-analysis graph for debugging
            GraphErrors G_PULSE = new GraphErrors();
            G_PULSE.add(pul_i1,fadcFitter.getPedestal());
            G_PULSE.add(pul_i2,fadcFitter.getPedestal());            
            if(fadcFitter.getWave_Max()-fadcFitter.getPedestal()>threshold) {
                H_LED_N.fill(key);
                for (int i = 0; i < Math.min(pulse.length, H_LED_fADC.get(0, 0, key).getAxis().getNBins()); i++) {
                    H_LED_fADC.get(0, 0, key).fill(i, pulse[i]-fadcFitter.getPedestal() + 10.0);                
                }
                H_LED_CHARGE.get(0, 0, key).fill(counter.getChannels().get(0).getADC().get(0)*LSB*nsPerSample/50);
                H_LED_VMAX.get(0, 0, key).fill((fadcFitter.getWave_Max()-fadcFitter.getPedestal())*LSB);
                H_LED_TCROSS.get(0, 0, key).fill(fadcFitter.getTime(3)-tPMTCross);
                H_LED_THALF.get(0, 0, key).fill(fadcFitter.getTime(7)-tPMTHalf); 
                H_LED_WIDTH.get(0, 0, key).fill(fadcFitter.getFWHM()); 
                G_PULSE.add(fadcFitter.getTime(3)/nsPerSample,fadcFitter.getPedestal()+threshold);
                G_PULSE.add(fadcFitter.getTime(7)/nsPerSample,fadcFitter.getHalf_Max());
                G_PULSE.add(fadcFitter.getPulsePosition(),fadcFitter.getPulse_Max());
                G_PULSE.add(fadcFitter.getTimeF()/nsPerSample,fadcFitter.getHalf_Max());
                
                // fill info for time dependence evaluation
                if(doesThisCrystalExist(key)) {
                    double ledCH = counter.getChannels().get(0).getADC().get(0)*LSB*nsPerSample/50;
                    double ledAM = (fadcFitter.getWave_Max()-fadcFitter.getPedestal())*LSB;
                    double ledFW = (fadcFitter.getFWHM());
                    if(ledEvent[key]==-1) {
                        ledEvent[key] = nProcessed;
                    }
                    if(ledEvent[key]!=-1) {
                        if(nProcessed>ledEvent[key]+nLedEvents ) {
                            ledCharge[key]  = ledCharge[key]/ledNEvents[key];
                            ledCharge2[key] = ledCharge2[key]/ledNEvents[key]; 
                            ledAmpli[key]  = ledAmpli[key]/ledNEvents[key];
                            ledAmpli2[key] = ledAmpli2[key]/ledNEvents[key]; 
                            ledWidth[key]  = ledWidth[key]/ledNEvents[key];
                            ledWidth2[key] = ledWidth2[key]/ledNEvents[key]; 
                            double ledX  = ledEvent[key]+nLedEvents/2.;
                            double ledY  = ledCharge[key];
                            double ledEX = nLedEvents;
                            double ledEY = sqrt(ledCharge2[key]-ledCharge[key]*ledCharge[key])/sqrt(ledNEvents[key]);
                            G_LED_CHARGE.get(0, 0, key).add(ledX,ledY,ledEX,ledEY);
                            if(G_LED_CHARGE.get(0, 0, key).getDataSize()>nLedSkip) G_LED_CHARGE_SELECT.get(0, 0, key).add(ledX,ledY,ledEX,ledEY); 
                            ledY  = ledAmpli[key];
                            ledEY = sqrt(ledAmpli2[key]-ledAmpli[key]*ledAmpli[key])/sqrt(ledNEvents[key]);
                            G_LED_AMPLI.get(0, 0, key).add(ledX,ledY,ledEX,ledEY);
                            if(G_LED_AMPLI.get(0, 0, key).getDataSize()>nLedSkip) G_LED_AMPLI_SELECT.get(0, 0, key).add(ledX,ledY,ledEX,ledEY); 
                            ledY  = ledWidth[key];
                            ledEY = sqrt(ledWidth2[key]-ledWidth[key]*ledWidth[key])/sqrt(ledNEvents[key]);
                            G_LED_WIDTH.get(0, 0, key).add(ledX,ledY,ledEX,ledEY);
//                            if(G_LED_WIDTH.get(0, 0, key).getDataSize()>nLedSkip) G_LED_WIDTH_SELECT.get(0, 0, key).add(ledX,ledY,ledEX,ledEY); 
                            ledEvent[key]   = nProcessed;
                            ledCharge[key]  = ledCH;
                            ledCharge2[key] = ledCH*ledCH;
                            ledAmpli[key]   = ledAM;
                            ledAmpli2[key]  = ledAM*ledAM;
                            ledWidth[key]   = ledFW;
                            ledWidth2[key]  = ledFW*ledFW;
                            ledNEvents[key] = 1;
                        }
                        else {
                            ledCharge[key]  += ledCH;
                            ledCharge2[key] += ledCH*ledCH;
                            ledAmpli[key]   += ledAM;
                            ledAmpli2[key]  += ledAM*ledAM;
                            ledWidth[key]   += ledFW;
                            ledWidth2[key]  += ledFW*ledFW;
                            ledNEvents[key]++;
                        }
                    }
                }
            }
            G_PULSE_ANALYSIS.add(0, 0, key, G_PULSE);
        }
        this.drawWave(this.canvasEvent);
        
//        Graphics2D g2d = (Graphics2D) this.canvasEvent.getGraphics();
//        PaveText info = new PaveText();
//        info.setFont("Helvetica", 14);
//        info.addText("Pedestal :   " + fadcFitter.getPedestal());
//        info.addText("Wave Max :   " + fadcFitter.getWave_Max());
//        info.addText("Time thrs. : " + fadcFitter.getThresholdCrossing());
//        info.addText("Time mode3 : " + fadcFitter.getTime(3));
//        info.addText("Time mode7 : " + fadcFitter.getTime(7));
//        info.drawOnCanvas(g2d, 400, 100, 1);
//        this.canvasEvent.update();
        
            //this.dcHits.show();
        this.view.repaint();


    }
    
    public void drawWave(EmbeddedCanvas canvas) {
        canvas.cd(0);
        canvas.draw(H_WAVE.get(0, 0, keySelect),"S");
        if(debugFlag) {
            canvas.draw(H_WAVE_PED.get(0, 0, keySelect),"same");
            canvas.draw(H_WAVE_PUL.get(0, 0, keySelect),"same");
            if(G_PULSE_ANALYSIS.hasEntry(0, 0, keySelect)) {
                if(G_PULSE_ANALYSIS.get(0, 0, keySelect).getDataSize()>0) canvas.draw(G_PULSE_ANALYSIS.get(0, 0, keySelect),"same");
            }
        }        
    }
    
    
    public boolean getComponentStatus(int key) {
        boolean componentStatus = false;
        if(H_WMAX.getBinContent(key)>threshold) {
            componentStatus= true;
        }
        return componentStatus;
    }
    
    public void detectorSelected(DetectorDescriptor desc) {
        // TODO Auto-generated method stub


        keySelect = desc.getComponent();

        // event viewer
        this.drawWave(this.canvasEvent);
        // noise
        for(int key=0; key<crystalPointers.length; key++) {
            if(crystalPointers[key]>=0) {
                pedestalMEAN[crystalPointers[key]] = H_PED.get(0,0,key).getMean();
                noiseRMS[crystalPointers[key]]     = H_NOISE.get(0, 0, key).getMean();
                timeCross[crystalPointers[key]]    = H_LED_TCROSS.get(0, 0, key).getMean();
                timeHalf[crystalPointers[key]]     = H_LED_THALF.get(0, 0, key).getMean();
                fullWidthHM[crystalPointers[key]]  = H_LED_WIDTH.get(0, 0, key).getMean();
            }
        }
        GraphErrors  G_PED = new GraphErrors(crystalID,pedestalMEAN);
        G_PED.setTitle(" "); //  title
        G_PED.setXTitle("Crystal ID"); // X axis title
        G_PED.setYTitle("Pedestal (Counts)");   // Y axis title
        G_PED.setMarkerColor(2); // color from 0-9 for given palette
        G_PED.setMarkerSize(5); // size in points on the screen
        G_PED.setMarkerStyle(1); // Style can be 1 or 2
        GraphErrors  G_NOISE = new GraphErrors(crystalID,noiseRMS);
        G_NOISE.setTitle(" "); //  title
        G_NOISE.setXTitle("Crystal ID"); // X axis title
        G_NOISE.setYTitle("Noise RMS (mV)");   // Y axis title
        G_NOISE.setMarkerColor(4); // color from 0-9 for given palette
        G_NOISE.setMarkerSize(5); // size in points on the screen
        G_NOISE.setMarkerStyle(1); // Style can be 1 or 2
        canvasNoise.cd(0);
        canvasNoise.draw(G_PED);
        canvasNoise.cd(1);
        canvasNoise.draw(G_NOISE);
        if (H_NOISE.hasEntry(0, 0, keySelect)) {
            H1D hnoise = H_NOISE.get(0, 0, keySelect);
            H1D hped   = H_PED.get(0, 0, keySelect);
            canvasNoise.cd(2);
            canvasNoise.draw(hped,"S");
            canvasNoise.cd(3);
            canvasNoise.draw(hnoise,"S");
        }
        // Charge
        canvasCharge.cd(0);
        if (H_LED_fADC.hasEntry(0, 0, keySelect)) {
            if(H_LED_N.getBinContent(keySelect)>0) {
                hfADC = H_LED_fADC.get(0, 0, keySelect).histClone(" ");
                hfADC.normalize(H_LED_N.getBinContent(keySelect));
            }
            else {
                hfADC= new H1D("fADC", 100, 0.0, 100.0);
            }
            hfADC.setFillColor(3);
            hfADC.setXTitle("fADC Sample");
            hfADC.setYTitle("fADC Counts");
            canvasCharge.draw(hfADC);               
        }
        canvasCharge.cd(1);
        canvasCharge.draw(G_LED_CHARGE.get(0, 0, keySelect));
        if(G_LED_CHARGE_SELECT.get(0, 0, keySelect).getDataSize()>0) canvasCharge.draw(G_LED_CHARGE_SELECT.get(0, 0, keySelect),"same");
        canvasCharge.cd(2);
        if(H_LED_CHARGE.hasEntry(0, 0, keySelect)) {
            H1D hled = H_LED_CHARGE.get(0,0,keySelect);
            canvasCharge.draw(hled,"S");
        } 
        canvasCharge.cd(3);
        canvasCharge.draw(G_LED_CHARGE_SELECT.get(0, 0, keySelect));
        // Amplitude
        canvasAmpli.cd(0);
        if (H_LED_fADC.hasEntry(0, 0, keySelect)) {
            if(H_LED_N.getBinContent(keySelect)>0) {
                hfADC = H_LED_fADC.get(0, 0, keySelect).histClone(" ");
                hfADC.normalize(H_LED_N.getBinContent(keySelect));
            }
            else {
                hfADC= new H1D("fADC", 100, 0.0, 100.0);
            }
            hfADC.setFillColor(3);
            hfADC.setXTitle("fADC Sample");
            hfADC.setYTitle("fADC Counts");
            canvasAmpli.draw(hfADC);               
        }
        canvasAmpli.cd(1);
        canvasAmpli.draw(G_LED_AMPLI.get(0, 0, keySelect));
        if(G_LED_AMPLI_SELECT.get(0, 0, keySelect).getDataSize()>0) canvasAmpli.draw(G_LED_AMPLI_SELECT.get(0, 0, keySelect),"same");
        canvasAmpli.cd(2);
        if(H_LED_VMAX.hasEntry(0, 0, keySelect)) {
            H1D hled = H_LED_VMAX.get(0,0,keySelect);
            canvasAmpli.draw(hled,"S");
        }
        canvasAmpli.cd(3);
        canvasAmpli.draw(G_LED_AMPLI_SELECT.get(0, 0, keySelect));
        // Width
        GraphErrors  G_WIDTH = new GraphErrors(crystalID,fullWidthHM);
        G_WIDTH.setTitle(" "); //  title
        G_WIDTH.setXTitle("Crystal ID"); // X axis title
        G_WIDTH.setYTitle("FWHM (ns)");   // Y axis title
        G_WIDTH.setMarkerColor(2); // color from 0-9 for given palette
        G_WIDTH.setMarkerSize(5); // size in points on the screen
        G_WIDTH.setMarkerStyle(1); // Style can be 1 or 2
        canvasWidth.cd(0);
        if (H_LED_fADC.hasEntry(0, 0, keySelect)) {
            if(H_LED_N.getBinContent(keySelect)>0) {
                hfADC = H_LED_fADC.get(0, 0, keySelect).histClone(" ");
                hfADC.normalize(H_LED_N.getBinContent(keySelect));
            }
            else {
                hfADC= new H1D("fADC", 100, 0.0, 100.0);
            }
            hfADC.setFillColor(3);
            hfADC.setXTitle("fADC Sample");
            hfADC.setYTitle("fADC Counts");
            canvasWidth.draw(hfADC);               
        }
        canvasWidth.cd(1); 
        canvasWidth.draw(G_WIDTH);
        canvasWidth.cd(2);
        if(H_LED_WIDTH.hasEntry(0, 0, keySelect)) {
            H1D hwidth = H_LED_WIDTH.get(0, 0, keySelect);
            canvasWidth.draw(hwidth,"S");
        }        
        canvasWidth.cd(3);
        canvasWidth.draw(G_LED_WIDTH.get(0, 0, keySelect));
        // Time
        GraphErrors  G_TCROSS = new GraphErrors(crystalID,timeCross);
        G_TCROSS.setTitle(" "); //  title
        G_TCROSS.setXTitle("Crystal ID"); // X axis title
        G_TCROSS.setYTitle("Trigger Time (ns)");   // Y axis title
        G_TCROSS.setMarkerColor(5); // color from 0-9 for given palette
        G_TCROSS.setMarkerSize(5);  // size in points on the screen
        G_TCROSS.setMarkerStyle(1); // Style can be 1 or 2
        GraphErrors  G_THALF = new GraphErrors(crystalID,timeHalf);
        G_THALF.setTitle(" "); //  title
        G_THALF.setXTitle("Crystal ID"); // X axis title
        G_THALF.setYTitle("Mode-7 Time (ns)");   // Y axis title
        G_THALF.setMarkerColor(5); // color from 0-9 for given palette
        G_THALF.setMarkerSize(5);  // size in points on the screen
        G_THALF.setMarkerStyle(1); // Style can be 1 or 2
        canvasTime.cd(0);
        canvasTime.draw(G_TCROSS);
        canvasTime.cd(1);
        canvasTime.draw(G_THALF);
        canvasTime.cd(2);
        if(H_LED_TCROSS.hasEntry(0, 0, keySelect)) {
            H1D htime = H_LED_TCROSS.get(0, 0, keySelect);
            canvasTime.draw(htime,"S");
        }
        canvasTime.cd(3);
        if(H_LED_THALF.hasEntry(0, 0, keySelect)) {
            H1D htime = H_LED_THALF.get(0, 0, keySelect);
            initTimeGaussFitPar(keySelect,htime);
            htime.fit(myTimeGauss.get(0, 0, keySelect),"NQ");
            canvasTime.draw(htime,"S");
            canvasTime.draw(myTimeGauss.get(0, 0, keySelect),"sameS");
        }
        this.updateTable();
    }

    public void update(DetectorShape2D shape) {
    

        int sector = shape.getDescriptor().getSector();
        int layer = shape.getDescriptor().getLayer();
        int paddle = shape.getDescriptor().getComponent();
        //shape.setColor(200, 200, 200);
        if(plotSelect==0) {
            if(H_WMAX.getBinContent(paddle)>threshold) {
                if(H_TCROSS.getBinContent(paddle)>0) {
                    shape.setColor(140, 0, 200);
                }
                else {
                    shape.setColor(200, 0, 200);
                }
            }
            else {
                shape.setColor(100, 100, 100);
            }
        }
        else if(plotSelect==1) {
            if (this.H_fADC.hasEntry(sector, layer, paddle)) {
                int nent = this.H_fADC.get(sector, layer, paddle).getEntries();
                //            Color col = palette.getColor3D(nent, nProcessed, true);           
                /*int colorRed = 240;
                 if(nProcessed!=0){
                 colorRed = (255*nent)/(nProcessed);
                 }*/
                //            shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
                if (nent > 0) {
                    if (this.H_NOISE.get(sector, layer, paddle).getMean() > 0.75
                     && this.H_NOISE.get(sector, layer, paddle).getMean() < 1.05) {
                        shape.setColor(0, 145, 0);
                    } else if (this.H_NOISE.get(sector, layer, paddle).getMean() < 1.0) {
                        shape.setColor(0, 0, 100);
                    } else {
                        shape.setColor(255, 100, 0);
                    }
                } else {
                    shape.setColor(100, 100, 100);
                }
            }
        }
        else if(plotSelect==2 || plotSelect==3) {
            if (this.H_LED_CHARGE.hasEntry(sector, layer, paddle)) {
                if(plotSelect==2) {
                    int nent = (int) this.H_LED_N.getBinContent(paddle);
                    Color col = palette.getColor3D(nent, nProcessed, true);           
                    /*int colorRed = 240;
                 if(nProcessed!=0){
                 colorRed = (255*nent)/(nProcessed);
                 }*/
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
                }
                else if(plotSelect==3){
                    double lmean = this.H_LED_VMAX.get(sector, layer, paddle).getMean();
                    if(G_LED_AMPLI.get(sector, layer, paddle).getDataSize()>0) 
                        lmean = G_LED_AMPLI.get(sector, layer, paddle).getDataY(G_LED_AMPLI.get(sector, layer, paddle).getDataSize()-1);
                    Color col = palette.getColor3D(lmean, 1000., true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
                }
            }
        }
        else if(plotSelect==4) {
            if (this.H_LED_WIDTH.hasEntry(sector, layer, paddle)) {
                double lmean = this.H_LED_WIDTH.get(sector, layer, paddle).getMean();
                Color col = palette.getColor3D(lmean, 100., true); 
                shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
            }
        }
        else if(plotSelect==5) {
            if (this.H_LED_THALF.hasEntry(sector, layer, paddle)) {
                int nent = this.H_LED_THALF.get(sector, layer, paddle).getEntries();
                Color col = palette.getColor3D(nent, nProcessed, true); 
                shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
            }
        }
        else if(plotSelect==6) {

        }
    }

    private void updateTable() {
        for(int key=0; key<22*22; key++) {
            if(doesThisCrystalExist(key)) {
//                double led_ave = 0;
//                double led2_ave = 0;
//                double led_rms =100;
//                if(G_LED_CHARGE_SELECT.get(0, 0, key).getDataSize()>0) {
//                    DataVector leds = G_LED_CHARGE.get(0, 0, key).getDataY().getMean().getDataY();
//                     led_ave = 0;
//                     led2_ave = 0;
//                    for(int i=0; i<leds.getSize()-1; i++ ) {
//                        if(i>=5) {
//                            led_ave += leds.getValue(i);
//                            led2_ave += leds.getValue(i)*leds.getValue(i);                            
//                        }
//                    }
//                    led_ave=led_ave/(leds.getSize()-5-1);
//                    led2_ave=led2_ave/(leds.getSize()-5-1);
//                    led_rms=sqrt(led2_ave-led_ave*led_ave);
//                }
                String pedestal   = String.format ("%.1f", H_PED.get(0, 0, key).getMean());
                String noise      = String.format ("%.2f", H_NOISE.get(0, 0, key).getMean());
                String ledCharge  = String.format ("%.2f", 0.);
                String eledCharge = String.format ("%.2f", 100.);
                if(G_LED_CHARGE_SELECT.get(0, 0, key).getDataSize()>0) {
                    ledCharge     = String.format ("%.2f", G_LED_CHARGE_SELECT.get(0, 0, key).getDataY().getMean());
                    eledCharge    = String.format ("%.2f", G_LED_CHARGE_SELECT.get(0, 0, key).getDataY().getRMS());
                }
                String ledAmp     = String.format ("%.2f", 0.);
                String eledAmp    = String.format ("%.2f", 100.);
                if(G_LED_AMPLI_SELECT.get(0, 0, key).getDataSize()>0) {
                    ledAmp       = String.format ("%.2f", G_LED_AMPLI_SELECT.get(0, 0, key).getDataY().getMean());
                    eledAmp      = String.format ("%.2f", G_LED_AMPLI_SELECT.get(0, 0, key).getDataY().getRMS());
                }
                String time     = String.format ("%.2f", H_LED_THALF.get(0 , 0, key).getMean());
                String stime    = String.format ("%.2f", H_LED_THALF.get(0 , 0, key).getRMS());
                
                
//                summaryTable.setValueAtAsDouble(0, Double.parseDouble(pedestal), 0, 0, key);
//                summaryTable.setValueAtAsDouble(1, Double.parseDouble(noise)   , 0, 0, key);
                summaryTable.setValueAtAsDouble(0, Double.parseDouble(ledCharge)     , 0, 0, key);
                summaryTable.setValueAtAsDouble(1, Double.parseDouble(eledCharge)    , 0, 0, key);
                summaryTable.setValueAtAsDouble(2, Double.parseDouble(ledAmp)        , 0, 0, key);
                summaryTable.setValueAtAsDouble(3, Double.parseDouble(eledAmp)       , 0, 0, key);
//                summaryTable.setValueAtAsDouble(4, Double.parseDouble(time)    , 0, 0, key);
//                summaryTable.setValueAtAsDouble(5, Double.parseDouble(stime)   , 0, 0, key);                
            }            
        }
//        summaryTable.show();
    }


}
