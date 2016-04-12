package org.clas.ftcal;


import org.clas.tools.CanvasBook;
import org.clas.tools.FitData;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.List;

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

import org.clas.tools.CalibrationData;

import org.clas.containers.FTHashTable;
import org.clas.containers.FTHashTableViewer;


import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeTabView;
import org.jlab.clas12.calib.IDetectorListener;
import org.jlab.clas12.detector.DetectorCounter;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.clas12.detector.FADCBasicFitter;
import org.root.attr.ColorPalette;
import org.root.func.F1D;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;
import org.clas.tools.CustomizeFit;
import org.clas.tools.ExtendedFADCFitter;
import org.clas.tools.FTDataSet;
import org.clas.tools.Miscellaneous;

import org.root.pad.TCanvas;
import org.root.pad.TGCanvas;

import org.clas.tools.NoGridCanvas;
import org.clas.containers.IFTHashTableListener;
import org.clas.tools.FTApplication;



public class FTCALCosmic implements IDetectorListener,IFTHashTableListener,ActionListener,ChangeListener{

    // detector view
    FTCALDetector viewFTCAL = new FTCALDetector("FTCAL");            
          
    // applications
    FTCALNoise ftNoise = null;
    
    // panels and canvases
    JPanel detectorPanel;
    ColorPalette palette = new ColorPalette();
    NoGridCanvas canvasEvent     = new NoGridCanvas();
    NoGridCanvas canvasEnergy    = new NoGridCanvas(2,2);
    NoGridCanvas canvasTime      = new NoGridCanvas(2,2);
    DetectorShapeTabView view      = new DetectorShapeTabView();

    FTHashTable       summaryTable = null; 
    FTHashTableViewer canvasTable  = null;   
    JPanel radioPane      = new JPanel();
    JPanel radioPaneNoise = new JPanel();
    JPanel radioPaneFits  = new JPanel();
    
    // file chooser
    JFileChooser fc = new JFileChooser();    
    
    // histograms, functions and graphs
    DetectorCollection<H1D> H_fADC = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_WAVE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_fADC   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_CHARGE = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_VMAX   = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_TCROSS  = new DetectorCollection<H1D>();
    DetectorCollection<H1D> H_COSMIC_THALF   = new DetectorCollection<H1D>();
    DetectorCollection<F1D> F_ChargeLandau = new DetectorCollection<F1D>();
    DetectorCollection<F1D> F_TimeGauss = new DetectorCollection<F1D>();
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
    CustomizeFit cfit = new CustomizeFit();
    
    double[] crystalID; 
    double[] pedestalMEAN;
    double[] pedestalRMS;
    double[] noiseRMS;
    double[] cosmicCharge;
    double[] timeCross;
    double[] timeHalf;

    // decoded related information
    int nProcessed = 0;
    EventDecoder decoder;
    ExtendedFADCFitter eFADCFitter = new ExtendedFADCFitter();
    
    
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

    
    
    public FTCALCosmic(){
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
        canvasTable.addListener(this);
        
        // create Tabbed Panel
        JTabbedPane tabbedPane = new JTabbedPane();
        
        tabbedPane.add("Event Viewer",this.canvasEvent);
        tabbedPane.add(ftNoise.getName(),ftNoise.getCanvas());
        tabbedPane.add("Energy"      ,this.canvasEnergy);
        tabbedPane.add("Time"        ,this.canvasTime);
        tabbedPane.add("Summary"     ,canvasTable);
        tabbedPane.addChangeListener(this);
        
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
        JButton cfitBtn = new JButton("Customize Fit...");
        cfitBtn.addActionListener(this);
        buttonPane.add(cfitBtn);
        JButton fileBtn = new JButton("Save to File");
        fileBtn.addActionListener(this);
        buttonPane.add(fileBtn);

//        JPanel viewPane = new JPanel();
//        viewPane.setLayout(new BorderLayout());
        
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
        
        radioPaneFits.setLayout(new FlowLayout());       
        radioPaneFits.add(occRb);
        radioPaneFits.add(meanRb);
        radioPaneFits.add(sigmaRb);         
        radioPaneFits.add(chi2Rb);         
 
        radioPane.add(ftNoise.getRadioPane());
        ftNoise.getRadioPane().setVisible(false);
        radioPane.add(radioPaneFits);
        radioPaneFits.setVisible(false);
        
        canvasPane.add(tabbedPane, BorderLayout.CENTER);
        canvasPane.add(buttonPane, BorderLayout.PAGE_END);
    
        view.add(radioPane, BorderLayout.PAGE_END);
        
        splitPane.setLeftComponent(this.view);
        splitPane.setRightComponent(canvasPane);
 
        this.detectorPanel.add(splitPane, BorderLayout.CENTER);

    }


    private void initTable() {
        summaryTable = new FTHashTable(3,"Pedestal:d","Noise:i","N. Events:i","Energy Mean:d","Energy Sigma:d","Energy Chi2:d","Time Mean:d","Time Sigma:d");
        double[] summaryInitialValues = {-1, -1, -1, -1, -1, -1, -1, -1};
        for (int component : viewFTCAL.getDetectorComponents()) {
            summaryTable.addRow(summaryInitialValues,0,0,component);
            summaryTable.addConstrain(3, 160.0, 240.0);
            summaryTable.addConstrain(4, 1.0, 1.5); 
            summaryTable.addConstrain(6, 5.0, 25.); 
        }
//        summaryTable = ftNoise.getTable();
        canvasTable = new FTHashTableViewer(summaryTable);
        canvasTable.getTable().getColumnModel().getColumn(0).setWidth(0);
        canvasTable.getTable().getColumnModel().getColumn(0).setMaxWidth(0);
        canvasTable.getTable().getColumnModel().getColumn(1).setWidth(0);
        canvasTable.getTable().getColumnModel().getColumn(1).setMaxWidth(0);
    }
    
    public void initDetector() {
        viewFTCAL.addPaddles();
        viewFTCAL.setThresholds(threshold);
        this.view.addDetectorLayer(viewFTCAL);
        view.addDetectorListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        System.out.println("FTCALViewerModule ACTION = " + e.getActionCommand());
        if (e.getActionCommand().compareTo("Clear Histograms") == 0) {
            this.resetHistograms();
        }
        if (e.getActionCommand().compareTo("Fit Histograms") == 0) {
            this.fitHistograms();
            if(plotSelect==3) this.cosmicsBook(H_COSMIC_THALF,  F_TimeGauss);
            else              this.cosmicsBook(H_COSMIC_CHARGE, F_ChargeLandau);
        }
        if (e.getActionCommand().compareTo("Save to File") == 0) {
            this.saveToFile();
        }
        if (e.getActionCommand().compareTo("Customize Fit...") == 0) {
            this.customizeFit();        
        }

        if (e.getActionCommand().compareTo("Occupancy") == 0) {
            drawSelect = 4;
        } else if (e.getActionCommand().compareTo("Mean") == 0) {
            drawSelect = 5;
        } else if (e.getActionCommand().compareTo("Sigma") == 0) {
            drawSelect = 6;
        } else if (e.getActionCommand().compareTo("Chi2") == 0) {
            drawSelect = 7;
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
        ftNoise = new FTCALNoise(viewFTCAL);
        ftNoise.setFitter(eFADCFitter);
        FTDataSet ftDataSet = new FTDataSet(viewFTCAL);
        H_fADC  = ftDataSet.addCollection(new H1D("fADC", 100, 0.0, 100.0));
        H_WAVE  = ftDataSet.addCollection(new H1D("Wave", 100, 0.0, 100.0),"fADC Sample","fADC Counts",5);
        H_COSMIC_fADC   = ftDataSet.addCollection(new H1D("fADC", 100, 0.0, 100.0),"fADC Sample","fADC Counts",3);
        H_COSMIC_CHARGE = ftDataSet.addCollection(new H1D("Charge", 80, 0.0, 40.0),"Charge (pC)","Counts",2);
        H_COSMIC_VMAX   = ftDataSet.addCollection(new H1D("VMax", 80, 0.0, 40.0), "Amplitude (mV)", "Counts", 2);
        H_COSMIC_TCROSS = ftDataSet.addCollection(new H1D("T_TRIG", 80, -20.0, 60.0), "Time (ns)", "Counts", 5);
        H_COSMIC_THALF  = ftDataSet.addCollection(new H1D("T_HALF", 80, -20.0, 60.0), "Time (ns)", "Counts", 5);
        F_ChargeLandau  = ftDataSet.addCollection(new F1D("landau",    0.0, 40.0),"Landau");
        F_TimeGauss     = ftDataSet.addCollection(new F1D("gaus", -20.0, 60.0),"Time");

        H_fADC_N       = new H1D("fADC"  , viewFTCAL.getComponentMaxCount(), 0, viewFTCAL.getComponentMaxCount());
        H_WMAX         = new H1D("WMAX"  , viewFTCAL.getComponentMaxCount(), 0, viewFTCAL.getComponentMaxCount());
        H_TCROSS       = new H1D("TCROSS", viewFTCAL.getComponentMaxCount(), 0, viewFTCAL.getComponentMaxCount());
        H_COSMIC_N     = new H1D("EVENT" , viewFTCAL.getComponentMaxCount(), 0, viewFTCAL.getComponentMaxCount());
        H_COSMIC_MEAN  = new H1D("MEAN"  , viewFTCAL.getComponentMaxCount(), 0, viewFTCAL.getComponentMaxCount());
        H_COSMIC_SIGMA = new H1D("SIGMA" , viewFTCAL.getComponentMaxCount(), 0, viewFTCAL.getComponentMaxCount());
        H_COSMIC_CHI2  = new H1D("CHI2"  , viewFTCAL.getComponentMaxCount(), 0, viewFTCAL.getComponentMaxCount());
        H_TIME_MEAN    = new H1D("MEAN"  , viewFTCAL.getComponentMaxCount(), 0, viewFTCAL.getComponentMaxCount());
        H_TIME_SIGMA   = new H1D("SIGMA" , viewFTCAL.getComponentMaxCount(), 0, viewFTCAL.getComponentMaxCount());
        H_TIME_CHI2    = new H1D("CHI2"  , viewFTCAL.getComponentMaxCount(), 0, viewFTCAL.getComponentMaxCount());
        
        crystalID       = new double[viewFTCAL.getNComponents()];
        timeCross       = new double[viewFTCAL.getNComponents()];
        timeHalf        = new double[viewFTCAL.getNComponents()];
        for(int i=0; i<viewFTCAL.getNComponents(); i++) {
            crystalID[i]=viewFTCAL.getIDArray()[i];
        }


    }

    public void resetHistograms() { 
        for (int component : viewFTCAL.getDetectorComponents()) {
            if(H_fADC.hasEntry(0, 0, component)) {
                H_fADC.get(0, 0, component).reset();
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
        if(hcosmic.getBinContent(0)==0) F_ChargeLandau.add(0, 0, key, new F1D("landau",     mlMin, mlMax));
        else                            F_ChargeLandau.add(0, 0, key, new F1D("landau+exp", mlMin, mlMax));
        if(hcosmic.getBinContent(0)<10) {
            F_ChargeLandau.get(0, 0, key).setParameter(0, hcosmic.getBinContent(hcosmic.getMaximumBin()));
        }
        else {
            F_ChargeLandau.get(0, 0, key).setParameter(0, 10);
        }
        F_ChargeLandau.get(0, 0, key).setParLimits(0, 0.0, 10000000.); 
        F_ChargeLandau.get(0, 0, key).setParameter(1,hcosmic.getMean());
        F_ChargeLandau.get(0, 0, key).setParLimits(1, 5.0, 30.);        
        F_ChargeLandau.get(0, 0, key).setParameter(2,2);
        F_ChargeLandau.get(0, 0, key).setParLimits(2, 0.5, 10);
        if(hcosmic.getBinContent(0)!=0) {
            F_ChargeLandau.get(0, 0, key).setParameter(3,hcosmic.getBinContent(0));
            F_ChargeLandau.get(0, 0, key).setParLimits(3,  0.0, 10000000.); 
            F_ChargeLandau.get(0, 0, key).setParameter(4, -0.2);
            F_ChargeLandau.get(0, 0, key).setParLimits(4, -10, 0.); 
        }
    }
    
    private void fitLandau(int key) {
        H1D hcosmic = H_COSMIC_CHARGE.get(0,0,key);
        initLandauFitPar(key);
        hcosmic.fit(F_ChargeLandau.get(0, 0, key),"LQ");
        this.updateFitResults(key);
    }

    private void initTimeGaussFitPar(int key, H1D htime) {
        double hAmp  = htime.getBinContent(htime.getMaximumBin());
        double hMean = htime.getAxis().getBinCenter(htime.getMaximumBin());
        double hRMS  = htime.getRMS();        
        double rangeMin = hMean - 3*hRMS; 
        double rangeMax = hMean + 3*hRMS;         
        F_TimeGauss.add(0, 0, key, new F1D("gaus", rangeMin, rangeMax));
        F_TimeGauss.get(0, 0, key).setParameter(0, hAmp);
        F_TimeGauss.get(0, 0, key).setParLimits(0, hAmp*0.8, hAmp*1.2);
        F_TimeGauss.get(0, 0, key).setParameter(1, hMean);       
        F_TimeGauss.get(0, 0, key).setParameter(2, 2.);
    }    

    private void fitTime(int key) {
        H1D htime = H_COSMIC_THALF.get(0,0,key);
        
        initTimeGaussFitPar(key,htime);
        htime.fit(F_TimeGauss.get(0, 0, key),"NQ");
        this.updateFitResults(key);
    }
    
    private void fitHistograms() {
        for(int key : H_COSMIC_CHARGE.getComponents(0, 0)) {
            if(H_COSMIC_CHARGE.get(0, 0, key).getEntries()>100) {
                //System.out.println("Fitting charge histos for component: " + key);
                this.fitLandau(key);
            }
            //else System.out.println("Skipping charge fit of component: " + key + 
            //                        ", only " + H_COSMIC_CHARGE.get(0, 0, key).getEntries() + " events");
            if(H_COSMIC_THALF.get(0,0,key).getEntries()>0) {        
                this.fitTime(key);
            }   
        }
    }
    
    private void updateFitResults(int key){
        H_COSMIC_MEAN.setBinContent(key, F_ChargeLandau.get(0, 0, key).getParameter(1));
        H_COSMIC_SIGMA.setBinContent(key, F_ChargeLandau.get(0, 0, key).getParameter(2));
        H_COSMIC_CHI2.setBinContent(key, F_ChargeLandau.get(0, 0, key).getChiSquare(H_COSMIC_CHARGE.get(0,0,key),"NR")
                                        /F_ChargeLandau.get(0, 0, key).getNDF(H_COSMIC_CHARGE.get(0,0,key).getDataSet()));
        H_TIME_MEAN.setBinContent(key, F_TimeGauss.get(0, 0, key).getParameter(1));
        H_TIME_SIGMA.setBinContent(key, F_TimeGauss.get(0, 0, key).getParameter(2));
        H_TIME_CHI2.setBinContent(key, F_TimeGauss.get(0, 0, key).getChiSquare(H_COSMIC_THALF.get(0,0,key),"NR")
                                      /F_TimeGauss.get(0, 0, key).getNDF(H_COSMIC_THALF.get(0,0,key).getDataSet()));
    }
    
    private void customizeFit(){     
        if(plotSelect==2) {
            cfit.FitPanel(H_COSMIC_CHARGE.get(0,0,keySelect), F_ChargeLandau.get(0,0,keySelect));
            this.canvasEnergy.update();
        }
        else if(plotSelect==3) {
            cfit.FitPanel(H_COSMIC_THALF.get(0,0,keySelect), F_TimeGauss.get(0,0,keySelect));
            this.canvasTime.update();            
        }
        this.updateFitResults(keySelect);
   }
    
    private void cosmicsBook(DetectorCollection<H1D> h, DetectorCollection<F1D> f){
        JFrame     frame = new JFrame();
        CanvasBook book  = new CanvasBook(4,4);
        for(int key : h.getComponents(0, 0)) {
            if(h.hasEntry(0, 0, key)) {
                book.add(h.get(0,0,key)," ");
                book.add(f.get(0,0,key),"same"); 
            }
         }
        book.reset();
        frame.add(book);
        frame.pack();
        frame.setVisible(true);
        book.drawNextBack(false);
    }   
    
    public void initDecoder() {
        decoder.addFitter(DetectorType.FTCAL,
                new FADCBasicFitter(ped_i1, // first bin for pedestal
                        ped_i2, // last bin for pedestal
                        pul_i1, // first bin for pulse integral
                        pul_i2 // last bin for pulse integral
                        )); 
        eFADCFitter.setPedestalRange(ped_i1, ped_i2);
        eFADCFitter.setPulseRange(pul_i1, pul_i2);
    }

    public void hashTableCallback(String string, Long l) {
        // ToDO
        System.out.println("Selected table row " + string + " " + l);
    }

    public void processDecodedEvent() {
        // TODO Auto-generated method stub

        nProcessed++;

        List<DetectorCounter> counters = decoder.getDetectorCounters(DetectorType.FTCAL);
        this.ftNoise.addEvent(counters);
        H_WMAX.reset();
        H_TCROSS.reset();
        double tPMTCross=0;
        double tPMTHalf=0;
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            if(H_fADC.hasEntry(0, 0, key)) {
    //                           System.out.println(counters.size() + " " + key + " " + counter.getDescriptor().getComponent());
    //                           System.out.println(counter);            
                eFADCFitter.fit(counter.getChannels().get(0),viewFTCAL.getThresholds().get(0, 0, key));
                short pulse[] = counter.getChannels().get(0).getPulse();
                H_fADC_N.fill(key);
                H_WAVE.get(0, 0, key).reset();
                for (int i = 0; i < Math.min(pulse.length, H_fADC.get(0, 0, key).getAxis().getNBins()); i++) {
                    H_fADC.get(0, 0, key).fill(i, pulse[i] - eFADCFitter.getPedestal() + 10.0);
                    H_WAVE.get(0, 0, key).fill(i, pulse[i]);
                }
                H_WMAX.fill(key,eFADCFitter.getWave_Max()-eFADCFitter.getPedestal());
                H_TCROSS.fill(key,eFADCFitter.getTime(3));
                    //            if(fadcFitter.getWave_Max()-fadcFitter.getPedestal()>threshold) 
                    //            System.out.println("   Component #" + key + " is above threshold, max=" + fadcFitter.getWave_Max() + " ped=" + fadcFitter.getPedestal());
                if(key==501) {      // top long PMT
                    tPMTCross = eFADCFitter.getTime(3);
                    tPMTHalf  = eFADCFitter.getTime(7);
                }
            }
        }
        for (DetectorCounter counter : counters) {
            int key = counter.getDescriptor().getComponent();
            if(H_fADC.hasEntry(0, 0, key)) {
                int iy  = key/nCrystalX;
                int ix  = key - iy * nCrystalX;
                int nCrystalInColumn = 0;
                eFADCFitter.fit(counter.getChannels().get(0),viewFTCAL.getThresholds().get(0, 0, key));
                int i1=(int) max(0,iy-ncry_cosmic);    // allowing for +/- to cope with dead channels
                int i2=(int) min(nCrystalY,iy+ncry_cosmic);
                for(int i=i1; i<=i2; i++) {
                    if(i!=iy && viewFTCAL.hasComponent(i*nCrystalX+ix)) {
    //                    System.out.println(ix + " " + iy + " " + i1 + " " + i2 + " " + i + " " +H_WMAX.getBinContent(i*nCrystalX+ix));
    //                    if(H_WMAX.getBinContent(i*nCrystalX+ix)>threshold && H_TCROSS.getBinContent(i*nCrystalX+ix)>0) nCrystalInColumn++;                    
                        if(H_WMAX.getBinContent(i*nCrystalX+ix)>viewFTCAL.getThresholds().get(0, 0, i*nCrystalX+ix) ) nCrystalInColumn++;                    
                    }
                }
                if(nCrystalInColumn>=ncry_cosmic) {
                    short pulse[] = counter.getChannels().get(0).getPulse();
                    H_COSMIC_N.fill(key);
                    for (int i = 0; i < Math.min(pulse.length, H_COSMIC_fADC.get(0, 0, key).getAxis().getNBins()); i++) {
                        H_COSMIC_fADC.get(0, 0, key).fill(i, pulse[i]-eFADCFitter.getPedestal() + 10.0);                
                    }
                    double charge=(counter.getChannels().get(0).getADC().get(0)*LSB*nsPerSample/50);
                    H_COSMIC_CHARGE.get(0, 0, key).fill(charge);
                    H_COSMIC_VMAX.get(0, 0, key).fill((eFADCFitter.getWave_Max()-eFADCFitter.getPedestal())*LSB);
                    H_COSMIC_TCROSS.get(0, 0, key).fill(eFADCFitter.getTime(3)-tPMTCross);
                    H_COSMIC_THALF.get(0, 0, key).fill(eFADCFitter.getTime(7)-tPMTHalf);                
                }
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
        this.ftNoise.updateCanvas(keySelect);
        int ipointer=0;
        for(int key : viewFTCAL.getDetectorComponents()) {
            timeCross[ipointer]    = H_COSMIC_TCROSS.get(0, 0, key).getMean();
            timeHalf[ipointer]     = H_COSMIC_THALF.get(0, 0, key).getMean();
            ipointer++;
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
                canvasEnergy.draw(F_ChargeLandau.get(0, 0, keySelect),"sameS");
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
                canvasTime.draw(F_TimeGauss.get(0, 0, keySelect),"sameS");
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
            ftNoise.getRadioPane().setVisible(false);
            radioPaneFits.setVisible(false);
            if(H_WMAX.getBinContent(paddle)>viewFTCAL.getThresholds().get(sector, layer, paddle)) {
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
            ftNoise.getRadioPane().setVisible(true);
            radioPaneFits.setVisible(false);
            Color col = ftNoise.getColor(paddle);
            shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
        }
        else if(plotSelect==2) {
            ftNoise.getRadioPane().setVisible(false);
            radioPaneFits.setVisible(true);
            if (this.H_COSMIC_CHARGE.hasEntry(sector, layer, paddle)) {
                if(drawSelect==5) {
                    double lmean = 10*H_COSMIC_MEAN.getBinContent(paddle);
                    Color col = palette.getColor3D(lmean, 500., true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());                    
                }
                else if(drawSelect==6) {
                    double lsigma = 10*H_COSMIC_SIGMA.getBinContent(paddle);
                    Color col = palette.getColor3D(lsigma, 100., true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());                    
                }
                else if(drawSelect==7) {
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
            ftNoise.getRadioPane().setVisible(false);
            radioPaneFits.setVisible(true);
            if (this.H_COSMIC_THALF.hasEntry(sector, layer, paddle)) {
                if(drawSelect==5) {
                    double lmean = 10*H_TIME_MEAN.getBinContent(paddle);
                    Color col = palette.getColor3D(lmean, 400., true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());                    
                }
                else if(drawSelect==6) {
                    double lsigma = 10*H_TIME_SIGMA.getBinContent(paddle);
                    Color col = palette.getColor3D(lsigma, 100., true);           
                    shape.setColor(col.getRed(),col.getGreen(),col.getBlue());                    
                }
                else if(drawSelect==7) {
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
//            radioPaneNoise.setVisible(false);
            ftNoise.getRadioPane().setVisible(false);
            radioPaneFits.setVisible(false);
        }
    }

    private void updateTable() {
        for(int key : viewFTCAL.getDetectorComponents()) {
//            ftNoise.getTable().show(); System.out.println(ftNoise.getTable().getValueAt(key, 6));
//            String pedestal = String.format ("%.1f", ftNoise.getTable().getValueAt(key, 6));
//            String noise    = String.format ("%.2f", ftNoise.getTable().getValueAt(key, 8));
            String nev      = String.format ("%d",   H_COSMIC_CHARGE.get(0,0,key).getEntries());
            String mips     = String.format ("%.2f", H_COSMIC_MEAN.getBinContent(key));
            String emips    = String.format ("%.2f", H_COSMIC_SIGMA.getBinContent(key));
            String chi2     = String.format ("%.2f", H_COSMIC_CHI2.getBinContent(key));
            String time     = String.format ("%.2f", F_TimeGauss.get(0, 0, key).getParameter(1));
            String stime    = String.format ("%.2f", F_TimeGauss.get(0, 0, key).getParameter(2));


//            summaryTable.setValueAtAsDouble(0, Double.parseDouble(pedestal), 0, 0, key);
//            summaryTable.setValueAtAsDouble(1, Double.parseDouble(noise)   , 0, 0, key);
            summaryTable.setValueAtAsDouble(2, Double.parseDouble(nev)     , 0, 0, key);
            summaryTable.setValueAtAsDouble(3, Double.parseDouble(mips)    , 0, 0, key);
            summaryTable.setValueAtAsDouble(4, Double.parseDouble(emips)   , 0, 0, key);
            summaryTable.setValueAtAsDouble(5, Double.parseDouble(chi2)    , 0, 0, key);
            summaryTable.setValueAtAsDouble(6, Double.parseDouble(time)    , 0, 0, key);
            summaryTable.setValueAtAsDouble(7, Double.parseDouble(stime)   , 0, 0, key);  
            
        }
    }

    private void saveToFile() {
//        this.fc.setCurrentDirectory(new File("calibfiles"));
//	int returnValue = fc.showSaveDialog(null);
//        if (returnValue == JFileChooser.APPROVE_OPTION) {
//            String outputFileName = fc.getSelectedFile().getAbsolutePath();
//            System.out.println("Saving calibration results to: " + outputFileName);
//            FitData cosmicFile = new FitData(mylandau,H_COSMIC_CHARGE);
//            cosmicFile.writeToFile(outputFileName);
//        }
        Miscellaneous extra = new Miscellaneous();
        CalibrationData calib = new CalibrationData();
        
        String outputFileName = extra.extractFileName("", "_Fit",".txt");
        this.fc.setCurrentDirectory(new File(outputFileName));
	int returnValue = fc.showSaveDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            outputFileName = fc.getSelectedFile().getAbsolutePath();
            System.out.println("Saving calibration results to: " + outputFileName);
            FitData cosmicFile = new FitData(F_ChargeLandau,H_COSMIC_CHARGE);
            cosmicFile.writeToFile(outputFileName);
        }

	    //      String CCDBoutFile = "./"+extra.datetime()+"_CCDB.txt";
//        extra.CCDBcosmic(F_ChargeLandau, H_PED, CCDBoutFile);
//        String outputname = extra.extractstring(outputFileName);

        
        String CCDBoutFile = extra.extractFileName("", "_CCDB",".txt");
        //extra.CCDBcosmic(mylandau, H_PED, CCDBoutFile); // Da reinserire //
        
      
        calib.addToMap("histograms", H_COSMIC_CHARGE);
        calib.addToMap("fitfunctions", F_ChargeLandau);
        //calib.addToMap("other", H_COSMIC_CHARGE.get(0, 0, 269));
        //calib.addToMap("other", H_COSMIC_CHARGE.get(0, 0, 268));
        String hipofile = extra.extractFileName("", "",".hipo");
        calib.fileWrite(hipofile);
        //calib.ls(hipofile);
        
        calib.readCosmicCalibFile(hipofile);
        DetectorCollection<H1D> H_calibration = calib.H_extracted;
        DetectorCollection<F1D> F_calibration = calib.F_extracted;
       
        
//        TGCanvas cc = new TGCanvas("","",600,600,2,1);
//        cc.cd(0);
//        cc.draw(H_COSMIC_CHARGE.get(0, 0, 269));
//        cc.cd(1);
//        cc.draw(H_calibration.get(0, 0, 269));
        

    }
    

    
}
