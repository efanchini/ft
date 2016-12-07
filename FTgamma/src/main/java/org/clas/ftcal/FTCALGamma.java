package org.clas.ftcal;



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.clas.containers.FTHashTable;
import org.clas.containers.FTHashTableViewer;
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeTabView;
import org.jlab.clas12.calib.IDetectorListener;
import org.jlab.clas12.detector.DetectorCounter;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.clas12.detector.FADCBasicFitter;
import org.root.attr.ColorPalette;
import org.clas.tools.ExtendedFADCFitter;
import org.clas.tools.FitParametersFile;
import org.clas.tools.HipoFile;
import org.clas.tools.Miscellaneous;
import org.clas.tools.NoGridCanvas;
import org.jlab.clas.detector.DetectorCollection;
import org.root.group.SpringUtilities;



public class FTCALGamma implements IDetectorListener,ActionListener,ChangeListener,ListSelectionListener{

    // detector view
    FTCALDetector viewFTCAL = new FTCALDetector("FTCAL");            
          
    // applications
    FTCALEventApp   ftEvent = null;
    FTCALNoiseApp   ftNoise = null;
    FTCALGammaApp   ftGamma = null;
    FTCALCompareApp ftCompare = null;
  
    //tools 
    Miscellaneous extra = new Miscellaneous();
    
    // panels and canvases
    JPanel detectorPanel;
    ColorPalette palette = new ColorPalette();
    NoGridCanvas canvasTime      = new NoGridCanvas(2,2);
    DetectorShapeTabView view      = new DetectorShapeTabView();

    FTHashTable       summaryTable = null; 
    FTHashTable       ccdbTable    = null; 
    FTHashTableViewer canvasTable  = null;   
    JPanel radioPane      = new JPanel();
    
    // file chooser
    JFileChooser fc = new JFileChooser();    

    // decoded related information
    int nProcessed = 0;
    EventDecoder decoder;
    ExtendedFADCFitter eFADCFitter = new ExtendedFADCFitter();
    private ArrayList parSelection = new ArrayList();;
    double threshold = 20; // 10 fADC value <-> ~ 5mV
    
    Boolean simflag=false;
   

    public EventDecoder getDecoder() {
        return decoder;
    }
    
    public void setDecoder(EventDecoder decoder) {
        this.decoder = decoder;
    }
    
    int ped_i1 = 4;
    int ped_i2 = 24;
    int pul_i1 = 45;
    int pul_i2 = 85;

    // control variables
    private String canvasSelect = "Event";  
    private int    keySelect = 8;
    private int[]  keyToRow  = null;
    JButton gammaSelBtn;

  
    
    public FTCALGamma(){
        this.detectorPanel=null;
        this.decoder=null;  
    }

    public JPanel getDetectorPanel() {
        return detectorPanel;
    }

    public void setDetectorPanel(JPanel detectorPanel) {
        this.detectorPanel = detectorPanel;
    }
    
    public void initApps() {
        
        // event
        ftEvent = new FTCALEventApp(viewFTCAL);
        ftEvent.setFitter(eFADCFitter);
        // noise
        ftNoise = new FTCALNoiseApp(viewFTCAL);
        ftNoise.setFitter(eFADCFitter);
        // gamma  
        ftGamma = new FTCALGammaApp(viewFTCAL);
        ftGamma.setFitter(eFADCFitter);
        //Comparison
        ftCompare = new FTCALCompareApp(viewFTCAL);
        ftCompare.setFitter(eFADCFitter);
      
    }

    public void initPanel() {
        
        // detector panel consists of a split pane with detector view and tabbed canvases
        JSplitPane splitPane = new JSplitPane();

        // the last tab will contain the summmary table
        this.initTable();
        this.initCCDBTable();
        canvasTable.getTable().getSelectionModel().addListSelectionListener(this);
        
        // create Tabbed Panel
        JTabbedPane tabbedPane = new JTabbedPane();
        
        tabbedPane.add(ftEvent.getCanvas(0).getName(),ftEvent.getCanvas(ftEvent.getName()));
        tabbedPane.add(ftNoise.getCanvas(ftNoise.getName()).getName(),ftNoise.getCanvas(ftNoise.getName()));
        tabbedPane.add(ftGamma.getCanvas("Energy").getName(),ftGamma.getCanvas("Energy"));
        tabbedPane.add(ftGamma.getCanvas("Time").getName(),ftGamma.getCanvas("Time"));
        tabbedPane.add("Summary"     ,canvasTable);
        tabbedPane.add(ftCompare.getCanvas("Comparison").getName(),ftCompare.getCanvas("Comparison"));
        tabbedPane.addChangeListener(this);
        
        JPanel canvasPane = new JPanel();
        canvasPane.setLayout(new BorderLayout());

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        gammaSelBtn = new JButton("Gamma Selection");
        gammaSelBtn.setBackground(Color.RED);
        gammaSelBtn.addActionListener(this);
        buttonPane.add(gammaSelBtn);
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
        JButton fCpr = new JButton("File Comparison");
        fCpr.addActionListener(this);
        buttonPane.add(fCpr);
 
        radioPane.add(ftNoise.getRadioPane());
        radioPane.add(ftGamma.getRadioPane());
        ftNoise.getRadioPane().setVisible(false);
        ftGamma.getRadioPane().setVisible(false);
        ftCompare.getRadioPane().setVisible(false);
        
        canvasPane.add(tabbedPane, BorderLayout.CENTER);
        canvasPane.add(buttonPane, BorderLayout.PAGE_END);
    
        view.add(radioPane, BorderLayout.PAGE_END);
        
        splitPane.setLeftComponent(this.view);
        splitPane.setRightComponent(canvasPane);
 
        this.detectorPanel.add(splitPane, BorderLayout.CENTER);
        this.ftGamma.fitFastCollections();
        
        //this.TabbedPaneDemo();
        
    }
   
 private void initTable() {
     // SummaryTable //
        summaryTable = new FTHashTable(3,"Pedestal:d","Noise:d","N. Events:i","<E>:d","\u03C3(E):d","\u03C7\u00B2(E):d","<T>:d","\u03C3(T):d",
                "Status:i");
        double[] summaryInitialValues = {-1, -1, -1, -1, -1, -1, -1, -1, -1};
        int irow=0;
        keyToRow = new int[viewFTCAL.getComponentMaxCount()+1];
        for (int component : viewFTCAL.getDetectorComponents()) {
           keyToRow[component]=irow;            
            summaryTable.addRow(summaryInitialValues,1,1,component);
            summaryTable.addConstrain(0+3, ftNoise.getParameter("Pedestal Mean").getParMin(), ftNoise.getParameter("Pedestal Mean").getParMax());
            summaryTable.addConstrain(1+3, ftNoise.getParameter("Noise").getParMin(), ftNoise.getParameter("Noise").getParMax());
            summaryTable.addConstrain(3+3, ftGamma.getParameter("<E>").getParMin(), ftGamma.getParameter("<E>").getParMax()); 
            summaryTable.addConstrain(5+3, ftGamma.getParameter("\u03C7\u00B2(E)").getParMin(), ftGamma.getParameter("\u03C7\u00B2(E)").getParMax()); 
            summaryTable.addConstrain(7+3, ftGamma.getParameter("\u03C3(T)").getParMin(), ftGamma.getParameter("\u03C3(T)").getParMax()); 
            summaryTable.addConstrain(8+3, ftNoise.getParameter("Status").getParMin(), ftNoise.getParameter("Status").getParMax());
            //System.out.println("Erica tabkle: "+ftNoise.getParameter("Status").getParMin()+"  "+ftNoise.getParameter("Status").getParMax()+"  "+ftNoise.getParameter(0).getName());
            irow++;
        }
        canvasTable = new FTHashTableViewer(summaryTable);
        canvasTable.getTable().getColumnModel().getColumn(0).setWidth(0);
        canvasTable.getTable().getColumnModel().getColumn(0).setMaxWidth(0);
        canvasTable.getTable().getColumnModel().getColumn(1).setWidth(0);
        canvasTable.getTable().getColumnModel().getColumn(1).setMaxWidth(0);
    }  
 
    private void initCCDBTable() {
        // CCDB table with all parameters//
        ccdbTable = new FTHashTable(3,"Pedestal:d","Pedestal RMS:d","Noise:d","Noise RMS:d","N. Events:i","<E>:d",
                "\u03C3(E):d","\u03C7\u00B2(E):d","<T>:d","\u03C3(T):d","Status:i","Threshold:d");
        double[] summaryInitialValues = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        int irow=0;
        keyToRow = new int[viewFTCAL.getComponentMaxCount()+1];
        for (int component : viewFTCAL.getDetectorComponents()) {
                keyToRow[component]=irow;            
                ccdbTable.addRow(summaryInitialValues,1,1,component);
                irow++;  
        }
    }
    
    public void initDetector() {
        viewFTCAL.setThresholds(threshold);
        this.view.addDetectorLayer(viewFTCAL);
        view.addDetectorListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        System.out.println("FTCALViewerModule ACTION = " + e.getActionCommand());
        
        if (e.getActionCommand().compareTo("Gamma Selection") == 0) {
           this.TabbedPaneDemo();
            
        }
        if (e.getActionCommand().compareTo("Clear Histograms") == 0) {
            this.resetHistograms();
            
        }
        if (e.getActionCommand().compareTo("Fit Histograms") == 0) {
            this.ftGamma.fitCollections();
        }
        if (e.getActionCommand().compareTo("Save to File") == 0) {
            try {
              this.saveToFile();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FTCALGamma.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (e.getActionCommand().compareTo("Customize Fit...") == 0) {
            this.ftGamma.customizeFit(keySelect);   
        }
        if (e.getActionCommand().compareTo("File Comparison") == 0) {
           this.ftGamma.fitFastCollections();
           this.ftCompare.fileSelection();
        }
        this.view.repaint();
        
    }

    public void stateChanged(ChangeEvent e) {
        JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
        int index = sourceTabbedPane.getSelectedIndex();
        canvasSelect = sourceTabbedPane.getTitleAt(index);
        this.ftGamma.setCanvasSelect(canvasSelect);
        this.updateTable();
        this.view.repaint();
        if(canvasSelect=="Comparison"){
            this.ftCompare.clearCollections();
            this.ftGamma.fitCollections("Comparison");
            String hipotmpfile = "./tmp.hipo";
            this.writeHipoFile(hipotmpfile);
            this.ftCompare.dumpCalib(hipotmpfile);
            this.ftCompare.updateCanvas(keySelect);
        }
    }
     
    public void resetHistograms() { 
        this.ftEvent.resetCollections();
        this.ftNoise.resetCollections();
        this.ftGamma.resetCollections();
        this.ftCompare.resetCollections();
        this.view.repaint();
        this.resetGammaSel();
        
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

    public void processDecodedEvent() {
        nProcessed++;
        if(nProcessed>0)freezeGammaSel();
        List<DetectorCounter> counters = decoder.getDetectorCounters(DetectorType.FTCAL);
        this.ftEvent.addEvent(counters);
        this.ftNoise.addEvent(counters);
        this.ftGamma.addEvent(counters);
        this.ftEvent.updateCanvas(keySelect);
        this.view.repaint();
    }    
    
    public void processDecodedSimEvent(DetectorCollection<Double> adc, DetectorCollection<Double> tdc) {
        nProcessed++;
        //if(nProcessed>0)freezeGammaSel();
        this.view.repaint();
        this.ftEvent.addSimEvent(adc);
        this.ftNoise.addSimEvent(adc);
        this.ftGamma.addSimEvent(adc, tdc);
        this.ftEvent.updateCanvas(keySelect);
        this.view.repaint();
    }    
    
    public void detectorSelected(DetectorDescriptor desc) {
        
        keySelect = desc.getComponent();
        // event viewer
        this.ftEvent.updateCanvas(keySelect);
        // noise
        this.ftNoise.updateCanvas(keySelect);
        // gamma
        this.ftGamma.updateCanvas(keySelect);
        // summary
        this.updateTable();
        this.canvasTable.getTable().clearSelection();
        this.canvasTable.getTable().changeSelection(keyToRow[keySelect], 0, true, false);
        // compare
        this.ftCompare.updateCanvas(keySelect);
           
    }

    public void update(DetectorShape2D shape) {
    
        int sector = shape.getDescriptor().getSector();
        int layer = shape.getDescriptor().getLayer();
        int paddle = shape.getDescriptor().getComponent();
        if(canvasSelect == "Event") {
            ftNoise.getRadioPane().setVisible(false);
            ftGamma.getRadioPane().setVisible(false);
            Color col = ftEvent.getColor(paddle);
            shape.setColor(col.getRed(),col.getGreen(),col.getBlue());  
        }
        else if(canvasSelect == "Noise") {
            ftNoise.getRadioPane().setVisible(true);
            ftGamma.getRadioPane().setVisible(false);
            Color col = ftNoise.getColor(paddle);
            shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
        }
        else if(canvasSelect == "Energy" || canvasSelect == "Time") {
            ftNoise.getRadioPane().setVisible(false);
            ftGamma.getRadioPane().setVisible(true);
            Color col = ftGamma.getColor(paddle);
            shape.setColor(col.getRed(),col.getGreen(),col.getBlue()); 
        }
        else if(canvasSelect == "Summary") {
            ftNoise.getRadioPane().setVisible(false);
            ftGamma.getRadioPane().setVisible(false);
            Color col = new Color(255, 100, 0);
            if(this.summaryTable.isRowValid(sector,layer,paddle)) col = new Color(0, 0, 100);
            String selectedKey = (String) summaryTable.getValueAt(canvasTable.getTable().getSelectedRow(), 2);
            if(paddle == Integer.parseInt(selectedKey)) col = new Color(255,0,0);
            shape.setColor(col.getRed(),col.getGreen(),col.getBlue());
        }
    }

   private void updateTable() {
        for(int key : viewFTCAL.getDetectorComponents()) {
            String pedestal = String.format ("%.1f", ftNoise.getFieldValue("Pedestal Mean",key));
            String noise    = String.format ("%.2f", ftNoise.getFieldValue("Noise",key));
            //String nev      = String.format ("%d",   ftGamma.getFieldValue("Occupancy", key));
            String nev      = String.format ("%d",   (int)ftGamma.getFieldValue("Occupancy", key));
            String mips     = String.format ("%.2f", ftGamma.getFieldValue("<E>", key));
            String emips    = String.format ("%.2f", ftGamma.getFieldValue("\u03C3(E)", key));
            String chi2     = String.format ("%.2f", ftGamma.getFieldValue("\u03C7\u00B2(E)", key));
            String time     = String.format ("%.2f", ftGamma.getFieldValue("<T>", key));
            String stime    = String.format ("%.2f", ftGamma.getFieldValue("\u03C3(T)", key));
            String status   = String.format ("%d",   (int)ftNoise.getFieldValue("Status", key));
            //String status   = String.format ("%d",   ftNoise.getFieldValue("Status", key));

            summaryTable.setValueAtAsDouble(0, Double.parseDouble(pedestal), 1, 1, key);
            summaryTable.setValueAtAsDouble(1, Double.parseDouble(noise)   , 1, 1, key);
            summaryTable.setValueAtAsDouble(2, Integer.parseInt(nev)       , 1, 1, key);
            //summaryTable.setValueAtAsDouble(2, Double.parseDouble(nev)     , 1, 1, key);
            summaryTable.setValueAtAsDouble(3, Double.parseDouble(mips)    , 1, 1, key);
            summaryTable.setValueAtAsDouble(4, Double.parseDouble(emips)   , 1, 1, key);
            summaryTable.setValueAtAsDouble(5, Double.parseDouble(chi2)    , 1, 1, key);
            summaryTable.setValueAtAsDouble(6, Double.parseDouble(time)    , 1, 1, key);
            summaryTable.setValueAtAsDouble(7, Double.parseDouble(stime)   , 1, 1, key);  
            //summaryTable.setValueAtAsDouble(8, Double.parseDouble(status)  , 1, 1, key);  
            summaryTable.setValueAtAsDouble(8, Integer.parseInt(status)  , 1, 1, key);  
        }
    }

 
    private void updateCCDBTable() {
        System.out.println("ERICA CCDB2 TABLE");
        for(int key : viewFTCAL.getDetectorComponents()) {
            //String status   = String.format ("%.1f", ftNoise.getFieldValue("Status", key));
            String status   = String.format ("%d", (int)ftNoise.getFieldValue("Status", key));
            String pedestal = String.format ("%.1f", ftNoise.getFieldValue("Pedestal Mean",key));
            String ped_rms  = String.format ("%.1f", ftNoise.getFieldValue("Pedestal RMS",key));
            String noise    = String.format ("%.2f", ftNoise.getFieldValue("Noise",key));
            String noise_rms= String.format ("%.2f", ftNoise.getFieldValue("Noise RMS",key));
            //String nev      = String.format ("%f", ftGamma.getFieldValue("Occupancy", key));
            String nev      = String.format ("%d",   (int) ftGamma.getFieldValue("Occupancy", key));
            String mips     = String.format ("%.2f", ftGamma.getFieldValue("<E>", key));
            String emips    = String.format ("%.2f", ftGamma.getFieldValue("\u03C3(E)", key));
            String chi2     = String.format ("%.2f", ftGamma.getFieldValue("\u03C7\u00B2(E)", key));
            String time     = String.format ("%.2f", ftGamma.getFieldValue("<T>", key));
            String stime    = String.format ("%.2f", ftGamma.getFieldValue("\u03C3(T)", key));
            String thr      = String.format ("%.2f", ftGamma.getFieldValue("Threshold", key));
                                                                          // S, L, C //  
            ccdbTable.setValueAtAsDouble(0, Double.parseDouble(pedestal)  , 1, 1, key);
            ccdbTable.setValueAtAsDouble(1, Double.parseDouble(ped_rms)   , 1, 1, key);
            ccdbTable.setValueAtAsDouble(2, Double.parseDouble(noise)     , 1, 1, key);
            ccdbTable.setValueAtAsDouble(3, Double.parseDouble(noise_rms) , 1, 1, key);
            //ccdbTable.setValueAtAsDouble(4, Double.parseDouble(nev)       , 1, 1, key);
            ccdbTable.setValueAtAsDouble(4, Integer.parseInt(nev)         , 1, 1, key);
            ccdbTable.setValueAtAsDouble(5, Double.parseDouble(mips)      , 1, 1, key);
            ccdbTable.setValueAtAsDouble(6, Double.parseDouble(emips)     , 1, 1, key);
            ccdbTable.setValueAtAsDouble(7, Double.parseDouble(chi2)      , 1, 1, key);
            ccdbTable.setValueAtAsDouble(8, Double.parseDouble(time)      , 1, 1, key);
            ccdbTable.setValueAtAsDouble(9, Double.parseDouble(stime)     , 1, 1, key); 
            //ccdbTable.setValueAtAsDouble(10,Double.parseDouble(status)    , 1, 1, key);  
            ccdbTable.setValueAtAsDouble(10,Integer.parseInt(status)      , 1, 1, key);  
            ccdbTable.setValueAtAsDouble(11,Double.parseDouble(thr)       , 1, 1, key);
            
        }
    }
    
    
    public void valueChanged(ListSelectionEvent e) {
        this.updateTable();
        if (e.getValueIsAdjusting()) return;
        this.view.repaint();
    }
    
   private void saveToFile() throws FileNotFoundException {
        
        FitParametersFile gammaFile = new FitParametersFile();
        
        // TXT table summary FILE //
        String outputFileName = "Run_SummaryTable.txt";
        String buttonFileName = "";
        this.fc.setCurrentDirectory(new File(outputFileName));
	int returnValue = fc.showSaveDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            buttonFileName = fc.getSelectedFile().getAbsolutePath();
            updateTable();
            outputFileName = extra.extractFileName(buttonFileName, "_SummaryTable",".txt");
            gammaFile.writeSummaryTFile(outputFileName, summaryTable);
        }
         //Hipofile
        String hipoFileName = "./test.hipo";
        if(buttonFileName!="")hipoFileName = extra.extractFileName(buttonFileName, "",".hipo");
        writeHipoFile(hipoFileName);
        
        // CCDB File //
        String CCDBoutFile = extra.extractFileName("Gamma.txt", "_CCDB",".txt");
        updateTable();
        updateCCDBTable();
        gammaFile.CCDBFile(CCDBoutFile, ccdbTable);
          
        //TXT file with fit parameters (Landau from data) //
        String gammaFileName = "./test.hipo";
        if(buttonFileName!="")gammaFileName = extra.extractFileName(buttonFileName, "_GammaFit",".txt");
        this.ftGamma.addGammaToFile(gammaFile);
        gammaFile.writeFitLandauFile(gammaFileName); 
    }
   
   public void writeHipoFile(String filename){
        HipoFile histofile = new HipoFile(filename);
        this.ftGamma.saveToFile(histofile);
        this.ftNoise.saveToFile(histofile);
        histofile.writeHipoFile(filename);
        //histofile.browsFile(hipoFileName);
   }
   
   
   public void TabbedPaneDemo() { 
 
        JFrame TabbedPaneDemo = new JFrame();
        TabbedPaneDemo.setBounds(10, 10, 500, 180);
        JTabbedPane tabbedPane = new JTabbedPane();     

        
        String[] text3 = new String[1];
        text3[0]="Signal Thrd (MeV):";
        CustomPanel cp3 = new CustomPanel(text3.length, text3);
        tabbedPane.addTab("FTCal Signal Thr", cp3);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_3);
        
        String[] text4 = new String[1];
        text4[0]="Signal Thr (MeV)";
        CustomPanel cp4 = new CustomPanel(text4.length, text4);
        tabbedPane.addTab("Simulated integrated event", cp4);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_4); 
 
        //Add the tabbed pane to this panel.
        TabbedPaneDemo.add(tabbedPane);
         
        //The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        
        TabbedPaneDemo.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        TabbedPaneDemo.setVisible(true);
    }

    private class CustomPanel extends JPanel implements ActionListener{  
	JTextField[] params = new JTextField[100];
        private int nPar =0;
	private CustomPanel(int npar, String[] text){
            this.setLayout(new SpringLayout());
            this.nPar=npar;
            //this.setSize(10000, 500);
            for (int i = 0; i < npar; i++) {  
                params[i] = new JTextField(1);
                JLabel l = new JLabel(text[i], JLabel.TRAILING);
                this.add(l);
                this.add(params[i]);
            }
            JButton bclear = new JButton("Clear");
            this.add(bclear);
            bclear.addActionListener(this);
            
            JButton bsave = new JButton("Ok");
            this.add(bsave);
            bsave.addActionListener(this);
            
            SpringUtilities.makeCompactGrid(this,
                                        npar+1, 2,  //rows, cols
                                        6, 6,        //initX, initY
                                        50, 10);       //xPad, yPad
	}

        public void actionPerformed(ActionEvent e) {
            if(e.getActionCommand().compareTo("Ok")==0){
                JButton g =(JButton) e.getSource();
                JPanel p =(JPanel) g.getParent();
                JTabbedPane t =(JTabbedPane)p.getParent();
                int tabselected = t.getSelectedIndex();
                ArrayList pp = new ArrayList();
                pp.add(tabselected);
                for(int i=0; i<this.nPar; i++){  
                    if(params[i].getText().isEmpty()){
                        // default values //
                        pp.add(ftGamma.getDefaultSelPar(tabselected, i));
                    }
                    else {
                        pp.add(Double.parseDouble(params[i].getText()));
                    }
                }   
               
                ftGamma.LoadSelection(pp);
                ftGamma.reloadCollections();
                gammaSelBtn.setBackground(Color.GREEN);
                for( ActionListener al : gammaSelBtn.getActionListeners() ) {
                        gammaSelBtn.removeActionListener( al );
                 }
                JFrame frame = (JFrame)SwingUtilities.getRoot((Component) e.getSource());
                frame.dispose();
            }
            if(e.getActionCommand().compareTo("Clear")==0){
                for (int i = 0; i < this.nPar; i++) {
                    params[i].setText("");
                }
                
            }
        }
       
    }
    
  private void resetGammaSel(){
            //clear all values stored
            gammaSelBtn.addActionListener(this);
            gammaSelBtn.setBackground(Color.RED);
        }
  
  private void freezeGammaSel(){
        gammaSelBtn.removeActionListener(this);
        gammaSelBtn.setBackground(Color.GREEN);
  }
}
