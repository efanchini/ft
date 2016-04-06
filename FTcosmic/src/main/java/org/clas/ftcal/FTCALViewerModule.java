package org.clas.ftcal;


import tools.BookCanvas;
import tools.FitData;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.List;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
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

public class FTCALViewerModule implements IDetectorListener,IHashTableListener,ActionListener,ChangeListener{

    // detector view
    FTCALView viewFTCAL = new FTCALView("FTCAL");
    
    // panels and canvases
    JPanel detectorPanel;
    ColorPalette palette = new ColorPalette();
    EmbeddedCanvas canvasEvent     = new EmbeddedCanvas();
    EmbeddedCanvas canvasNoise     = new EmbeddedCanvas();
    EmbeddedCanvas canvasEnergy    = new EmbeddedCanvas();
    EmbeddedCanvas canvasTime      = new EmbeddedCanvas();
    DetectorShapeTabView view = new DetectorShapeTabView();
    HashTable  summaryTable   = null; 
    JPanel radioPane      = new JPanel();
    JPanel radioPaneNoise = new JPanel();
    JPanel radioPaneFits  = new JPanel();
    
    // file chooser
    JFileChooser fc = new JFileChooser();

    // histograms, functions and graphs
    DetectorCollection<H1D> H_fADC = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_WAVE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_PED  = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_NOISE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_fADC   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_CHARGE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_VMAX   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_TCROSS  = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_THALF   = new DetectorCollection<H1D>();
    DetectorCollection<F1D> mylandau = new DetectorCollection<F1D>();
    DetectorCollection<F1D> myTimeGauss = new DetectorCollection<F1D>();
    DetectorCollection<Double> thresholdValue = new DetectorCollection<Double>();
    H1D hfADC          = null;
    H1D H_fADC_N       = null;
    H1D H_WMAX         = null;
    H1D H_TCROSS       = null;
    H1D H_COSMIC_N     = null;
    H1D H_COSMIC_MEAN  = null;
    H1D H_COSMIC_SIGMA = null;
    H1D H_COSMIC_CHI2  = null;
    H1D H_TIME_MEAN    = null;
    H1D H_TIME_SIGMA   = null;
    H1D H_TIME_CHI2    = null;
    
    double[] crystalID; 
    double[] pedestalMEAN;
    double[] noiseRMS;
    double[] cosmicCharge;
    double[] timeCross;
    double[] timeHalf;
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


    int nCrystalX = 22;
    int nCrystalY = nCrystalX;
    int nCrystalComponents = nCrystalX*nCrystalY;
    
    // analysis parameters
    double threshold = 12; // 10 fADC value <-> ~ 5mV
    int ped_i1 = 4;
    int ped_i2 = 24;
    int pul_i1 = 30;
    int pul_i2 = 70;
    double nsPerSample=4;
    double LSB = 0.4884;
    int ncry_cosmic = 4;        // number of crystals above threshold in a column for cosmics selection
    double crystal_size = 15;


    // control variables
    private int plotSelect = 0;  
    private int drawSelect = 0;
    private int keySelect = 8;

    
    
    public FTCALViewerModule(){
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
        tabbedPane.add("Energy"      ,this.canvasEnergy);
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
        JButton fileBtn = new JButton("Save to File");
        fileBtn.addActionListener(this);
        buttonPane.add(fileBtn);

//        JPanel viewPane = new JPanel();
//        viewPane.setLayout(new BorderLayout());
        
        JRadioButton statRb  = new JRadioButton("Status");
        JRadioButton pedRb   = new JRadioButton("Pedestal");
        JRadioButton rmsRb   = new JRadioButton("RMS");
        ButtonGroup groupNoise = new ButtonGroup();
        groupNoise.add(statRb);
        groupNoise.add(pedRb);
        groupNoise.add(rmsRb);
        statRb.setSelected(true);
        statRb.addActionListener(this);
        pedRb.addActionListener(this);
        rmsRb.addActionListener(this);
        
        JRadioButton occRb   = new JRadioButton("Occupancy");
        JRadioButton meanRb  = new JRadioButton("Mean");
        JRadioButton sigmaRb = new JRadioButton("Sigma");
        JRadioButton chi2Rb  = new JRadioButton("Chi2");
        ButtonGroup groupFits = new ButtonGroup();
        groupFits.add(occRb);
        groupFits.add(meanRb);
        groupFits.add(sigmaRb);
        groupFits.add(chi2Rb);
        occRb.setSelected(true);
        occRb.addActionListener(this);
        meanRb.addActionListener(this);
        sigmaRb.addActionListener(this);
        chi2Rb.addActionListener(this);
        
        radioPaneNoise.setLayout(new FlowLayout());       
        radioPaneNoise.add(statRb);
        radioPaneNoise.add(pedRb);
        radioPaneNoise.add(rmsRb);
        
        radioPaneFits.setLayout(new FlowLayout());       
        radioPaneFits.add(occRb);
        radioPaneFits.add(meanRb);
        radioPaneFits.add(sigmaRb);         
        radioPaneFits.add(chi2Rb);         
 
        radioPane.add(radioPaneNoise);
        radioPane.add(radioPaneFits);
        radioPaneNoise.setVisible(false);
        radioPaneFits.setVisible(false);
        
        canvasPane.add(tabbedPane, BorderLayout.CENTER);
        canvasPane.add(buttonPane, BorderLayout.PAGE_END);
    
//        viewPane.add(this.view, BorderLayout.CENTER);
//        viewPane.add(radioPane, BorderLayout.PAGE_END);
        view.add(radioPane, BorderLayout.PAGE_END);
//        view.add(radioPaneFits, BorderLayout.PAGE_END);
        
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
        // energy
        this.canvasEnergy.divide(2, 2);
        this.canvasEnergy.cd(0);
        this.canvasEnergy.setGridX(false);
        this.canvasEnergy.setGridY(false);
        this.canvasEnergy.setAxisFontSize(10);
        this.canvasEnergy.setTitleFontSize(16);
        this.canvasEnergy.setAxisTitleFontSize(14);
        this.canvasEnergy.setStatBoxFontSize(8);
        this.canvasEnergy.cd(1);
        this.canvasEnergy.setGridX(false);
        this.canvasEnergy.setGridY(false);
        this.canvasEnergy.setAxisFontSize(10);
        this.canvasEnergy.setTitleFontSize(16);
        this.canvasEnergy.setAxisTitleFontSize(14);
        this.canvasEnergy.setStatBoxFontSize(8);
        this.canvasEnergy.cd(2);
        this.canvasEnergy.setGridX(false);
        this.canvasEnergy.setGridY(false);
        this.canvasEnergy.setAxisFontSize(10);
        this.canvasEnergy.setTitleFontSize(16);
        this.canvasEnergy.setAxisTitleFontSize(14);
        this.canvasEnergy.setStatBoxFontSize(8);
        this.canvasEnergy.cd(3);
        this.canvasEnergy.setGridX(false);
        this.canvasEnergy.setGridY(false);
        this.canvasEnergy.setAxisFontSize(10);
        this.canvasEnergy.setTitleFontSize(16);
        this.canvasEnergy.setAxisTitleFontSize(14);
        this.canvasEnergy.setStatBoxFontSize(8);
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
        summaryTable = new HashTable(3,"Pedestal:d","Noise:i","N. Events:i","Energy Mean:d","Energy Sigma:d","Energy Chi2:d","Time Mean:d","Time Sigma:d");
        double[] summaryInitialValues = {-1, -1, -1, -1, -1, -1, -1, -1};
        for (int component = 0; component < nCrystalX*nCrystalY; component++) {
            if(doesThisCrystalExist(component)) {
                summaryTable.addRow(summaryInitialValues,0,0,component);
                summaryTable.addConstrain(3, 160.0, 240.0);
                summaryTable.addConstrain(4, 1.0, 1.5); 
                summaryTable.addConstrain(6, 5.0, 25.); 
            }
        }
    }
    
    public void initDetector() {
        viewFTCAL.addPaddles();
        this.view.addDetectorLayer(viewFTCAL);
        view.addDetectorListener(this);
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

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        System.out.println("FTCALViewerModule ACTION = " + e.getActionCommand());
        if (e.getActionCommand().compareTo("Clear Histograms") == 0) {
            this.resetHistograms();
        }
        if (e.getActionCommand().compareTo("Fit Histograms") == 0) {
            this.fitHistograms();
            this.plotCosmicsHistograms(H_COSMIC_CHARGE, mylandau);
        }
        if (e.getActionCommand().compareTo("Save to File") == 0) {
            this.saveToFile();
        }

        if (e.getActionCommand().compareTo("Status") == 0) {
            drawSelect = 0;
        } else if (e.getActionCommand().compareTo("Pedestal") == 0) {
            drawSelect = 1;
        } else if (e.getActionCommand().compareTo("RMS") == 0) {
            drawSelect = 2;
        } else if (e.getActionCommand().compareTo("Occupancy") == 0) {
            drawSelect = 3;
        } else if (e.getActionCommand().compareTo("Mean") == 0) {
            drawSelect = 4;
        } else if (e.getActionCommand().compareTo("Sigma") == 0) {
            drawSelect = 5;
        } else if (e.getActionCommand().compareTo("Chi2") == 0) {
            drawSelect = 6;
        }
        this.view.repaint();
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

                int iy = component / nCrystalX;
                int ix = component - iy * nCrystalX;
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
                H_PED.add(0, 0, component, new H1D("Pedestal_" + component, title, 200, 150., 250.0));
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
                H_COSMIC_fADC.add(0, 0, component, new H1D("FADC_" + component, title, 100, 0.0, 100.0));
                H_COSMIC_fADC.get(0, 0, component).setFillColor(3);
                H_COSMIC_fADC.get(0, 0, component).setXTitle("fADC Sample");
                H_COSMIC_fADC.get(0, 0, component).setYTitle("fADC Counts");
                H_COSMIC_CHARGE.add(0, 0, component, new H1D("Charge_" + component, title, 80, 0.0, 40.0));
                H_COSMIC_CHARGE.get(0, 0, component).setFillColor(2);
                H_COSMIC_CHARGE.get(0, 0, component).setXTitle("Charge (pC)");
                H_COSMIC_CHARGE.get(0, 0, component).setYTitle("Counts");
                H_COSMIC_VMAX.add(0, 0, component, new H1D("VMax_" + component, title, 80, 0.0, 40.0));
                H_COSMIC_VMAX.get(0, 0, component).setFillColor(2);
                H_COSMIC_VMAX.get(0, 0, component).setXTitle("Amplitude (mV)");
                H_COSMIC_VMAX.get(0, 0, component).setYTitle("Counts");
                H_COSMIC_TCROSS.add(0, 0, component, new H1D("T_TRIG_" + component, title, 80, -20.0, 60.0));
                H_COSMIC_TCROSS.get(0, 0, component).setFillColor(5);
                H_COSMIC_TCROSS.get(0, 0, component).setXTitle("Time (ns)");
                H_COSMIC_TCROSS.get(0, 0, component).setYTitle("Counts");
                H_COSMIC_THALF.add(0, 0, component, new H1D("T_HALF_" + component, title, 80,-20.0, 60.0));
                H_COSMIC_THALF.get(0, 0, component).setFillColor(5);
                H_COSMIC_THALF.get(0, 0, component).setXTitle("Time (ns)");
                H_COSMIC_THALF.get(0, 0, component).setYTitle("Counts"); 
                mylandau.add(0, 0, component, new F1D("landau",    0.0, 40.0));
                myTimeGauss.add(0, 0, component, new F1D("gaus", -20.0, 60.0));
                if(ix!=-9) {
                    thresholdValue.add(0, 0, component, threshold);
                }
                else if(ix==-9) {
//                    if     (iy==-6 || iy==-5 || iy==-4 || iy==-3 || iy==-2 || iy==-1) thresholdValue.add(0, 0, component, threshold/3.);     
//                    else if(iy== 6 ||           iy==4  || iy==3  || iy==2  || iy==1 ) thresholdValue.add(0, 0, component, threshold/2.);
//                    else                                                              thresholdValue.add(0, 0, component, threshold);
                    if     (iy==-7 || iy==5 || iy==6 || iy==7) thresholdValue.add(0, 0, component, threshold);
                    else                                       thresholdValue.add(0, 0, component, threshold/3.);
                }
            }
        }
        H_fADC_N       = new H1D("fADC"  , 504, 0, 504);
        H_WMAX         = new H1D("WMAX"  , 504, 0, 504);
        H_TCROSS       = new H1D("TCROSS", 504, 0, 504);
        H_COSMIC_N     = new H1D("EVENT" , 504, 0, 504);
        H_COSMIC_MEAN  = new H1D("MEAN"  , 504, 0, 504);
        H_COSMIC_SIGMA = new H1D("SIGMA" , 504, 0, 504);
        H_COSMIC_CHI2  = new H1D("CHI2"  , 504, 0, 504);
        H_TIME_MEAN    = new H1D("MEAN"  , 504, 0, 504);
        H_TIME_SIGMA   = new H1D("SIGMA" , 504, 0, 504);
        H_TIME_CHI2    = new H1D("CHI2"  , 504, 0, 504);
        
        crystalID       = new double[332];
        pedestalMEAN    = new double[332];
        noiseRMS        = new double[332];
        timeCross       = new double[332];
        timeHalf        = new double[332];
        crystalPointers = new int[nCrystalComponents];
        int ipointer=0;
        for(int i=0; i<nCrystalComponents; i++) {
            if(doesThisCrystalExist(i)) {
                crystalPointers[i]=ipointer;
                crystalID[ipointer]=i;
                ipointer++;
            }
            else {
                crystalPointers[i]=-1;
            }
        }


    }

    public void resetHistograms() { 
        for (int component = 0; component < nCrystalX * nCrystalY; component++) {
            if(H_fADC.hasEntry(0, 0, component)) {
                H_fADC.get(0, 0, component).reset();
                H_NOISE.get(0, 0, component).reset();
                H_COSMIC_fADC.get(0, 0, component).reset();
                H_COSMIC_CHARGE.get(0, 0, component).reset();
                H_COSMIC_VMAX.get(0, 0, component).reset();
                H_COSMIC_TCROSS.get(0, 0, component).reset();
                H_COSMIC_THALF.get(0, 0, component).reset();
            }
            H_fADC_N.reset();
            H_COSMIC_N.reset();
            H_COSMIC_MEAN.reset();
            H_COSMIC_SIGMA.reset();
            H_COSMIC_CHI2.reset();
        }
        // TODO Auto-generated method stub
    }
    
    private void initLandauFitPar(int key) {
        H1D hcosmic = H_COSMIC_CHARGE.get(0,0,key);
        double mlMin=hcosmic.getAxis().min();
        double mlMax=hcosmic.getAxis().max();
        mlMin=1.0;
        if(hcosmic.getBinContent(0)==0) mylandau.add(0, 0, key, new F1D("landau",     mlMin, mlMax));
        else                            mylandau.add(0, 0, key, new F1D("landau+exp", mlMin, mlMax));
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
    
    private void fitLandau(int key) {
        H1D hcosmic = H_COSMIC_CHARGE.get(0,0,key);
        initLandauFitPar(key);
        hcosmic.fit(mylandau.get(0, 0, key),"LQ");
        H_COSMIC_MEAN.setBinContent(key, mylandau.get(0, 0, key).getParameter(1));
        H_COSMIC_SIGMA.setBinContent(key, mylandau.get(0, 0, key).getParameter(2));
        H_COSMIC_CHI2.setBinContent(key, mylandau.get(0, 0, key).getChiSquare(H_COSMIC_CHARGE.get(0,0,key),"NR")
                                        /mylandau.get(0, 0, key).getNDF(H_COSMIC_CHARGE.get(0,0,key).getDataSet()));
    }

    private void initTimeGaussFitPar(int key, H1D htime) {
        double hAmp  = htime.getBinContent(htime.getMaximumBin());
        double hMean = htime.getAxis().getBinCenter(htime.getMaximumBin());
        double hRMS  = htime.getRMS();        
        double rangeMin = hMean - 3*hRMS; 
        double rangeMax = hMean + 3*hRMS;         
        myTimeGauss.add(0, 0, key, new F1D("gaus", rangeMin, rangeMax));
        myTimeGauss.get(0, 0, key).setParameter(0, hAmp);
        myTimeGauss.get(0, 0, key).setParLimits(0, hAmp*0.8, hAmp*1.2);
        myTimeGauss.get(0, 0, key).setParameter(1, hMean);       
        myTimeGauss.get(0, 0, key).setParameter(2, 2.);
    }    

    private void fitTime(int key) {
        H1D htime = H_COSMIC_THALF.get(0,0,key);
        initTimeGaussFitPar(key,htime);
        htime.fit(myTimeGauss.get(0, 0, key),"NQ");
        H_TIME_MEAN.setBinContent(key, myTimeGauss.get(0, 0, key).getParameter(1));
        H_TIME_SIGMA.setBinContent(key, myTimeGauss.get(0, 0, key).getParameter(2));
        H_TIME_CHI2.setBinContent(key, myTimeGauss.get(0, 0, key).getChiSquare(H_COSMIC_THALF.get(0,0,key),"NR")
                                      /myTimeGauss.get(0, 0, key).getNDF(H_COSMIC_THALF.get(0,0,key).getDataSet()));
    }
    
    private void fitHistograms() {
        for(int key : H_COSMIC_CHARGE.getComponents(0, 0)) {
            if(H_COSMIC_CHARGE.get(0, 0, key).getEntries()>100) {
                System.out.println("Fitting charge histos for component: " + key);
                this.fitLandau(key);
            }
            else System.out.println("Skiping charge fit of component: " + key + 
                                    ", only " + H_COSMIC_CHARGE.get(0, 0, key).getEntries() + " events");
            if(H_COSMIC_THALF.get(0,0,key).getEntries()>0) {        
                this.fitTime(key);
            }   
        }
    }
    
    private void plotCosmicsHistograms(DetectorCollection<H1D> h, DetectorCollection<F1D> f){
        JFrame frame = new JFrame();
        BookCanvas book = new BookCanvas(3,3);
      
        for(int key : h.getComponents(0,0)) {
            if(h.hasEntry(0, 0, key)) {
              book.add(h.get(0,0,key),"");
              book.add(f.get(0,0,key),"same"); 
            }
         }

        frame.add(book);
        frame.pack();
        frame.setVisible(true);
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
        private int    thresholdCrossing=0;
        private double time_3 = 0;
        private double time_7 = 0;

        public double getPedestal() {
            return pedestal;
        }

        public double getRMS() {
            return rms;
        }

        public double getWave_Max() {
            return wave_max;
        }

        public int getThresholdCrossing() {
            return thresholdCrossing;
        }
        
        public double getTime(int mode) {
            double time=0;
            if(mode==3)       time = time_3;
            else if(mode==7)  time = time_7;
            else System.out.println(" Unknown mode for time calculation, check...");
            return time;
        }

        public void fit(DetectorChannel dc) {
            short[] pulse = dc.getPulse();
            double ped    = 0.0;
            double noise  = 0;
            double wmax   = 0;
            double pmax   = 0;
            int    tcross = 0; 
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
                    break; 
                }
            }
            // calculating high resolution time    
            double halfMax = (pmax+pedestal)/2;
            time_7 = time_3;
            int t0 = -1;
            if(tcross>0) { 
                for (int bin=tcross-1; bin<pul_i2; bin++) {
                    if (pulse[bin]<=halfMax && pulse[bin+1]>halfMax) {
                        t0 = bin;
                        break;
                    }
                }
                if(t0>-1) { 
                    int t1 = t0 + 1;
                    int a0 = pulse[t0];
                    int a1 = pulse[t1];
                       //final double slope = (a1 - a0); // units = ADC/sample
                       //final double yint = a1 - slope * t1;  // units = ADC 
                    time_7 = ((halfMax - a0)/(a1-a0) + t0)* nsPerSample;
                }
            }
       }

    }

    public void processDecodedEvent() {
        // TODO Auto-generated method stub

        nProcessed++;

        //    System.out.println("event #: " + nProcessed);
        List<DetectorCounter> counters = decoder.getDetectorCounters(DetectorType.FTCAL);
        FTCALViewerModule.MyADCFitter fadcFitter = new FTCALViewerModule.MyADCFitter();
        H_WMAX.reset();
        H_TCROSS.reset();
        double tPMTCross=0;
        double tPMTHalf=0;
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            //                System.out.println(counters.size() + " " + key + " " + counter.getDescriptor().getComponent());
            //                 System.out.println(counter);
            fadcFitter.fit(counter.getChannels().get(0));
            short pulse[] = counter.getChannels().get(0).getPulse();
            H_fADC_N.fill(key);
            H_WAVE.get(0, 0, key).reset();
            for (int i = 0; i < Math.min(pulse.length, H_fADC.get(0, 0, key).getAxis().getNBins()); i++) {
                H_fADC.get(0, 0, key).fill(i, pulse[i] - fadcFitter.getPedestal() + 10.0);
                H_WAVE.get(0, 0, key).fill(i, pulse[i]);
            }
            H_WMAX.fill(key,fadcFitter.getWave_Max()-fadcFitter.getPedestal());
            H_TCROSS.fill(key,fadcFitter.getTime(3));
                //            if(fadcFitter.getWave_Max()-fadcFitter.getPedestal()>threshold) 
                //            System.out.println("   Component #" + key + " is above threshold, max=" + fadcFitter.getWave_Max() + " ped=" + fadcFitter.getPedestal());
            H_PED.get(0, 0, key).fill(fadcFitter.getPedestal());
            H_NOISE.get(0, 0, key).fill(fadcFitter.getRMS());
            if(key==501) {      // top long PMT
                tPMTCross = fadcFitter.getTime(3);
                tPMTHalf  = fadcFitter.getTime(7);
            }
        }
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            int iy  = key/nCrystalX;
            int ix  = key - iy * nCrystalX;
            int nCrystalInColumn = 0;
            fadcFitter.fit(counter.getChannels().get(0));
            int i1=(int) max(0,iy-ncry_cosmic);    // allowing for +/- to cope with dead channels
            int i2=(int) min(nCrystalY,iy+ncry_cosmic);
            for(int i=i1; i<=i2; i++) {
//                threshold =12;
//                if(ix==-9 && (i==-6 || i==-5 || i==-4 || i==-3 || i==-2 || i==-1)) threshold=4;
//                if(ix==-9 && (i== 6 ||          i==4  || i==3  || i==2  || i==1)) threshold=6;
                if(i!=iy && doesThisCrystalExist(i*nCrystalX+ix)) {
//                    System.out.println(ix + " " + iy + " " + i1 + " " + i2 + " " + i + " " +H_WMAX.getBinContent(i*nCrystalX+ix));
//                    if(H_WMAX.getBinContent(i*nCrystalX+ix)>threshold && H_TCROSS.getBinContent(i*nCrystalX+ix)>0) nCrystalInColumn++;                    
                    if(H_WMAX.getBinContent(i*nCrystalX+ix)>thresholdValue.get(0, 0, i*nCrystalX+ix) ) nCrystalInColumn++;                    
                }
            }
            if(nCrystalInColumn>=ncry_cosmic) {
                short pulse[] = counter.getChannels().get(0).getPulse();
                H_COSMIC_N.fill(key);
                for (int i = 0; i < Math.min(pulse.length, H_COSMIC_fADC.get(0, 0, key).getAxis().getNBins()); i++) {
                    H_COSMIC_fADC.get(0, 0, key).fill(i, pulse[i]-fadcFitter.getPedestal() + 10.0);                
                }
                double charge=(counter.getChannels().get(0).getADC().get(0)*LSB*nsPerSample/50);
                H_COSMIC_CHARGE.get(0, 0, key).fill(charge);
                H_COSMIC_VMAX.get(0, 0, key).fill((fadcFitter.getWave_Max()-fadcFitter.getPedestal())*LSB);
                H_COSMIC_TCROSS.get(0, 0, key).fill(fadcFitter.getTime(3)-tPMTCross);
                H_COSMIC_THALF.get(0, 0, key).fill(fadcFitter.getTime(7)-tPMTHalf);                
            }
        }
        this.canvasEvent.draw(H_WAVE.get(0, 0, keySelect)); 
        //this.dcHits.show();
        this.view.repaint();


    }    
    
    public void detectorSelected(DetectorDescriptor desc) {
        // TODO Auto-generated method stub


        keySelect = desc.getComponent();

        // event viewer
        this.canvasEvent.draw(H_WAVE.get(0, 0, keySelect));
        // noise
        for(int key=0; key<crystalPointers.length; key++) {
            if(crystalPointers[key]>=0) {
                pedestalMEAN[crystalPointers[key]] = H_PED.get(0,0,key).getMean();
                noiseRMS[crystalPointers[key]]     = H_NOISE.get(0, 0, key).getMean();
                timeCross[crystalPointers[key]]    = H_COSMIC_TCROSS.get(0, 0, key).getMean();
                timeHalf[crystalPointers[key]]     = H_COSMIC_THALF.get(0, 0, key).getMean();
            }
        }
        GraphErrors  G_PED = new GraphErrors(crystalID,pedestalMEAN);
        G_PED.setTitle(" "); //  title
        G_PED.setXTitle("Crystal ID"); // X axis title
        G_PED.setYTitle("Noise RMS (mV)");   // Y axis title
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
        // Energy - Cosmics for now
        canvasEnergy.cd(0);
        if (H_fADC.hasEntry(0, 0, keySelect)) {
            hfADC = H_fADC.get(0, 0, keySelect).histClone(" ");
            hfADC.normalize(H_fADC_N.getBinContent(keySelect));
            hfADC.setFillColor(3);
            hfADC.setXTitle("fADC Sample");
            hfADC.setYTitle("fADC Counts");
            canvasEnergy.draw(hfADC);               
        }
        canvasEnergy.cd(1);
        if (H_COSMIC_fADC.hasEntry(0, 0, keySelect)) {
            hfADC = H_COSMIC_fADC.get(0, 0, keySelect).histClone(" ");
            hfADC.normalize(H_COSMIC_N.getBinContent(keySelect));
            hfADC.setFillColor(3);
            hfADC.setXTitle("fADC Sample");
            hfADC.setYTitle("fADC Counts");
            canvasEnergy.draw(hfADC);               
        }
        canvasEnergy.cd(2);
        if(H_COSMIC_VMAX.hasEntry(0, 0, keySelect)) {
            H1D hcosmic = H_COSMIC_VMAX.get(0,0,keySelect);
            canvasEnergy.draw(hcosmic,"S");
        }
        canvasEnergy.cd(3);
        if(H_COSMIC_CHARGE.hasEntry(0, 0, keySelect)) {
            H1D hcosmic = H_COSMIC_CHARGE.get(0,0,keySelect);
            canvasEnergy.draw(hcosmic,"S");
            if(hcosmic.getEntries()>0) {
                fitLandau(keySelect);
                canvasEnergy.draw(mylandau.get(0, 0, keySelect),"sameS");
            }
        } 
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
        if(H_COSMIC_TCROSS.hasEntry(0, 0, keySelect)) {
            H1D htime = H_COSMIC_TCROSS.get(0, 0, keySelect);
            canvasTime.draw(htime,"S");
        }
        canvasTime.cd(3);
        if(H_COSMIC_THALF.hasEntry(0, 0, keySelect)) {
            H1D htime = H_COSMIC_THALF.get(0, 0, keySelect);
            canvasTime.draw(htime,"S");
            if(htime.getEntries()>0) {
                fitTime(keySelect);
                canvasTime.draw(myTimeGauss.get(0, 0, keySelect),"sameS");
            }
        }
        this.updateTable();
    }

    public void update(DetectorShape2D shape) {
    
        int sector = shape.getDescriptor().getSector();
        int layer = shape.getDescriptor().getLayer();
        int paddle = shape.getDescriptor().getComponent();
        //shape.setColor(200, 200, 200);
        if(plotSelect==0) {
            radioPaneNoise.setVisible(false);
            radioPaneFits.setVisible(false);
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
            radioPaneNoise.setVisible(true);
            radioPaneFits.setVisible(false);
            if (this.H_fADC.hasEntry(sector, layer, paddle)) {
                int nent = this.H_fADC.get(sector, layer, paddle).getEntries();
                if (nent > 0) {
                    if(drawSelect==1) {
                        double cvalue = this.H_PED.get(sector, layer, paddle).getMean();
                        Color col = palette.getColor3D(cvalue, 400., true);           
                        shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
                    }
                    else if(drawSelect==2) {
                        double cvalue = this.H_NOISE.get(sector, layer, paddle).getMean();
                        Color col = palette.getColor3D(cvalue, 2., true);           
                        shape.setColor(col.getRed(),col.getGreen(),col.getBlue());                        
                    }
                    else {
                        if (this.H_NOISE.get(sector, layer, paddle).getMean() > 1.0
                                && this.H_NOISE.get(sector, layer, paddle).getMean() < 1.5) {
                            shape.setColor(0, 145, 0);
                        } else if (this.H_NOISE.get(sector, layer, paddle).getMean() < 1.0) {
                            shape.setColor(0, 0, 100);
                        } else {
                            shape.setColor(255, 100, 0);
                        }
                    }
                } else {
                    shape.setColor(100, 100, 100);
                }
            }
        }
        else if(plotSelect==2) {
            radioPaneNoise.setVisible(false);
            radioPaneFits.setVisible(true);
            if (this.H_COSMIC_CHARGE.hasEntry(sector, layer, paddle)) {
                if(drawSelect==4) {
                    double lmean = 10*H_COSMIC_MEAN.getBinContent(paddle);
                    Color col = palette.getColor3D(lmean, 100., true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());                    
                }
                else if(drawSelect==5) {
                    double lsigma = 10*H_COSMIC_SIGMA.getBinContent(paddle);
                    Color col = palette.getColor3D(lsigma, 100., true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());                    
                }
                else if(drawSelect==6) {
                    double lchi2 = 100*H_COSMIC_CHI2.getBinContent(paddle);
                    Color col = palette.getColor3D(lchi2, 500., true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());                    
                }
                else {
                    int nent = this.H_COSMIC_CHARGE.get(sector, layer, paddle).getEntries();
                    Color col = palette.getColor3D(nent, nProcessed, true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
                }
            }
        }
        else if(plotSelect==3) {
            radioPaneNoise.setVisible(false);
            radioPaneFits.setVisible(true);
            if (this.H_COSMIC_THALF.hasEntry(sector, layer, paddle)) {
                if(drawSelect==4) {
                    double lmean = 10*H_TIME_MEAN.getBinContent(paddle);
                    Color col = palette.getColor3D(lmean, 400., true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());                    
                }
                else if(drawSelect==5) {
                    double lsigma = 10*H_TIME_SIGMA.getBinContent(paddle);
                    Color col = palette.getColor3D(lsigma, 100., true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());                    
                }
                else if(drawSelect==6) {
                    double lchi2 = 100*H_TIME_CHI2.getBinContent(paddle);
                    Color col = palette.getColor3D(lchi2, 300., true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());                    
                }
                else {
                    int nent = this.H_COSMIC_THALF.get(sector, layer, paddle).getEntries();
                    Color col = palette.getColor3D(nent, nProcessed, true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
                }
            }
        }
        else if(plotSelect==4) {
            radioPaneNoise.setVisible(false);
            radioPaneFits.setVisible(false);
        }
    }

    private void updateTable() {
        for(int key=0; key<nCrystalX*nCrystalY; key++) {
            if(doesThisCrystalExist(key)) {
                String pedestal = String.format ("%.1f", H_PED.get(0, 0, key).getMean());
                String noise    = String.format ("%.2f", H_NOISE.get(0, 0, key).getMean());
                String nev      = String.format ("%d",   H_COSMIC_CHARGE.get(0,0,key).getEntries());
                String mips     = String.format ("%.2f", H_COSMIC_MEAN.getBinContent(key));
                String emips    = String.format ("%.2f", H_COSMIC_SIGMA.getBinContent(key));
                String chi2     = String.format ("%.2f", H_COSMIC_CHI2.getBinContent(key));
                String time     = String.format ("%.2f", myTimeGauss.get(0, 0, key).getParameter(1));
                String stime    = String.format ("%.2f", myTimeGauss.get(0, 0, key).getParameter(2));
                
                
                summaryTable.setValueAtAsDouble(0, Double.parseDouble(pedestal), 0, 0, key);
                summaryTable.setValueAtAsDouble(1, Double.parseDouble(noise)   , 0, 0, key);
                summaryTable.setValueAtAsDouble(2, Double.parseDouble(nev)     , 0, 0, key);
                summaryTable.setValueAtAsDouble(3, Double.parseDouble(mips)    , 0, 0, key);
                summaryTable.setValueAtAsDouble(4, Double.parseDouble(emips)   , 0, 0, key);
                summaryTable.setValueAtAsDouble(5, Double.parseDouble(chi2)    , 0, 0, key);
                summaryTable.setValueAtAsDouble(6, Double.parseDouble(time)    , 0, 0, key);
                summaryTable.setValueAtAsDouble(7, Double.parseDouble(stime)   , 0, 0, key);                
            }            
        }
//        summaryTable.show();
    }

    private void saveToFile() {
        this.fc.setCurrentDirectory(new File("calibfiles"));
	int returnValue = fc.showSaveDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            String outputFileName = fc.getSelectedFile().getAbsolutePath();
            System.out.println("Saving calibration results to: " + outputFileName);
            FitData cosmicFile = new FitData(mylandau,H_COSMIC_CHARGE);
            cosmicFile.writeToFile(outputFileName);
        }
    }
    
}
