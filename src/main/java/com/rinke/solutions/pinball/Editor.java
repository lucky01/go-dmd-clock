package com.rinke.solutions.pinball;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.model.Format;
import com.rinke.solutions.pinball.model.Model;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Project;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;


public class Editor implements Runnable {
    
    private static Logger LOG = LoggerFactory.getLogger(Editor.class); 

    private static final String NO_TRANS = " - ";
    String[] args;
    private String lastPath;
    private XStream xstream;
    private XStream jstream;
    private XStream bstream;
    BinaryStreamDriver driver;
    
    public Editor(String[] args) {
        this.args = args;
        setupXStream();
    }
    
    private void setupXStream() {
        xstream = new XStream();
        jstream = new XStream(new JettisonMappedXmlDriver());
        jstream = new XStream(new JettisonMappedXmlDriver());
        
		driver = new BinaryStreamDriver();
		bstream = new XStream(driver);
        bstream.alias("rgb", RGB.class);
        bstream.alias("palette", Palette.class);
        bstream.alias("project", Project.class);
        bstream.alias("palMapping", PalMapping.class);
        bstream.setMode(XStream.NO_REFERENCES);
        
        xstream.alias("rgb", RGB.class);
        xstream.alias("palette", Palette.class);
        xstream.alias("project", Project.class);
        xstream.alias("palMapping", PalMapping.class);
        xstream.setMode(XStream.NO_REFERENCES);
        jstream.alias("rgb", RGB.class);
        jstream.alias("palette", Palette.class);
        jstream.alias("project", Project.class);
        jstream.alias("palMapping", PalMapping.class);
        jstream.setMode(XStream.NO_REFERENCES);
    }
    
    public void storeObject(Model obj,  String filename) {
    	try( OutputStream out = new FileOutputStream(filename)) {
            HierarchicalStreamWriter writer = null;
            switch (Format.byFilename(filename)) {
            case XML:
                xstream.toXML(obj, out);
                break;
            case JSON:
                jstream.toXML(obj, out);
                break;
            case BIN:
                writer = driver.createWriter(out);
                bstream.marshal(obj, writer);
                break;
            case DAT:
                DataOutputStream dos = new DataOutputStream(new FileOutputStream(filename));
                obj.writeTo(dos);
                dos.close();

            default:
                throw new RuntimeException("unsupported filetype / extension " +filename);
            }
            if(writer!=null) writer.close(); else out.close();
    	} catch( IOException e) {
    	    LOG.error("error on storing "+filename, e);
    	    throw new RuntimeException("error on storing "+filename,e);
    	}
    }
    
    public Object loadObject(String filename) {
        Object res = null;
    	try ( InputStream in = new FileInputStream(filename) ) {
            HierarchicalStreamReader reader = null;
            
            switch (Format.byFilename(filename)) {
            case XML:
                return xstream.fromXML(in);
            case JSON:
                return jstream.fromXML(in);
            case BIN:
                reader = driver.createReader(in);
                res = bstream.unmarshal(reader, null);
                break;

            default:
                throw new RuntimeException("unsupported filetype / extension " +filename);
            }
    	} catch( IOException e2) {
    	    LOG.error("error on load "+filename,e2);
    	    throw new RuntimeException("error on load "+filename, e2);
    	}
    	return res;
    }
    
    public void testStore() {
    	java.util.List<Palette> tpalettes = new ArrayList<Palette>();
    	tpalettes.add( new Palette(dmd.rgb, 0, "default"));
    	tpalettes.add( new Palette(dmd.rgb, 1, "logo"));
    	
		java.util.List<PalMapping> palMappings = new ArrayList<PalMapping>();
		
		byte[] digest = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
		byte[] digest2 = {0,-1,-2,-3,-4,-5,-6,-7,-8,-9,-10,-11,-12,-13,-14,-15};
		
		palMappings.add( new PalMapping(digest , 0,	1000, 30));
		palMappings.add( new PalMapping(digest2 , 1,	3000, 90));
		
		
		Project project = new Project(1,"tftc-dump.txt.gz", tpalettes, palMappings);
    	try {
    		DataOutputStream dos = new DataOutputStream(new FileOutputStream("tftc.dat"));
			storeObject(project, "tftc.xml");
			storeObject(project, "tftc.json");
			storeObject(project, "tftc.bin");
			project.writeTo(dos);
			dos.close();
			
			loadObject("tftc.bin");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    /**
     * Launch the application.
     * 
     * @param args
     */
    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.logFile", "go-dmd-editor.log");
        // Display display = Display.getDefault();
        Editor editor = new Editor(args);
        try {
//        	editor.testStore();
            editor.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DMDClock clock = new DMDClock(false);
    Shell shell;

    Animation selectedAnimation;
    int selectedAnimationIndex;

    java.util.List<Animation> sourceAnis = new ArrayList<>();
    java.util.List<Animation> targetAnis = new ArrayList<>();
    java.util.List<Animation> playingAnis = new ArrayList<>();    

    List sourceList;

    Text txtTdelay;
    Combo comboTransition;
    Combo comboFsk;
    Spinner spinnerCycle;
    Spinner spinnerHold;
    Text nameText;
    Button btnCut;
    Button btnDelete;
    Canvas previewCanvas;

    Display display;
    
    String[] fsks = new String[] { "18", "16", "12", "6" };
    private java.util.List<String> transitions;

    private DMD dmd = new DMD();
    private AnimationHandler animationHandler;

    int cutNameNumber = 1; // postfix for names
    int markStart = 0;
    long startTimeCode = 0L;
    long lastTimeCode = 0L;
    int markEnd = 0;

    int x1,y1,x2,y2;
    
    Project project = new Project();
    final java.util.List<Palette> palettes = project.palettes;
    final java.util.List<PalMapping> palMappings = project.palMappings;
    
    PalMapping palMapping;

    private int activePalette = 0;
    Combo paletteCombo;
    ComboViewer paletteViewer;
    final Button colBtn[] = new Button[16];
    Button useHash1;
    Button useHash2;
    java.util.List<byte[]> hashes = new ArrayList<byte[]>();

    private void saveHashes(java.util.List<byte[]> hashes) {
        if( hashes != null ) {
            this.hashes.clear();
            for( byte[] h : hashes) {
                this.hashes.add( Arrays.copyOf(h, h.length));
            }
        }
    }

    /**
     * @wbp.parser.entryPoint
     */
    public void run() {
        
        InputStream stream;
        String version = "";
        try{ 
            stream = this.getClass().getClassLoader().getResourceAsStream("version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            version = reader.readLine();
            reader.close();
        } catch(Exception e) {
            
        }
        
        display = Display.getDefault();
        shell = new Shell();
        GlobalExceptionHandler.getInstance().setDisplay(display);
        GlobalExceptionHandler.getInstance().setShell(shell);
        
        shell.setSize(1260, 600);
        shell.setText("Animation Editor - "+version);
        shell.setLayout(new GridLayout(2, false));
        
        palettes.add(new Palette(dmd.rgb, 0, "default"));

        Label lblAnimations = new Label(shell, SWT.NONE);
        lblAnimations.setText("Animations / Scenes");

        Group grpDetails = new Group(shell, SWT.NONE);
        GridData gd_grpDetails = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
        gd_grpDetails.widthHint = 812;
        grpDetails.setLayoutData(gd_grpDetails);
        grpDetails.setText("Details");

        final Label lblDetails = new Label(grpDetails, SWT.NONE);
        lblDetails.setBounds(10, 20, 115, 17);
        lblDetails.setText("pos");

        final Button btnStart = new Button(grpDetails, SWT.NONE);
        final Button btnStop = new Button(grpDetails, SWT.NONE);

        btnStart.setBounds(145, 10, 60, 29);
        btnStart.setText("Start");
        btnStart.addListener(SWT.Selection, e -> {
            animationHandler.start();
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
        });
        btnStart.setEnabled(false);

        btnStop.setBounds(206, 10, 60, 29);
        btnStop.setText("Stop");
        btnStop.addListener(SWT.Selection, e -> {
            animationHandler.stop();
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });

        Button btnPrev = new Button(grpDetails, SWT.NONE);
        btnPrev.setBounds(272, 10, 31, 29);
        btnPrev.setText("<");
        btnPrev.addListener(SWT.Selection, e -> animationHandler.prev());

        Button btnNext = new Button(grpDetails, SWT.NONE);
        btnNext.setBounds(303, 10, 31, 29);
        btnNext.setText(">");
        btnNext.addListener(SWT.Selection, e -> animationHandler.next());

        Button btnMarkStart = new Button(grpDetails, SWT.NONE);
        btnMarkStart.setBounds(336, 10, 91, 29);
        btnMarkStart.setText("Mark Start");
        btnMarkStart.addListener(SWT.Selection, e -> {
            markStart = selectedAnimation.actFrame;
            // store start frame for pal mapping
            palMapping = new PalMapping();
            if(useHash1.getSelection()) {
                palMapping.digest = hashes.get(0);
            }
            if(useHash2.getSelection()) {
                palMapping.digest = hashes.get(1);
            }
            palMapping.palIndex = palettes.get(activePalette).index;
            startTimeCode = lastTimeCode;
            btnCut.setEnabled(markEnd > 0 && markEnd > markStart);
        });

        Button btnMarkEnd = new Button(grpDetails, SWT.NONE);
        btnMarkEnd.setBounds(426, 10, 91, 29);
        btnMarkEnd.setText("Mark End");
        btnMarkEnd.addListener(SWT.Selection, e -> {
            markEnd = selectedAnimation.actFrame;
            btnCut.setEnabled(markEnd > 0 && markEnd > markStart);
            if( palMapping != null ) {
                palMapping.durationInMillis = (lastTimeCode - startTimeCode);
                palMapping.durationInFrames = (int) (palMapping.durationInMillis / (122 / 3));
            }
        });

        btnCut = new Button(grpDetails, SWT.NONE);
        btnCut.setBounds(523, 10, 91, 29);
        btnCut.setText("Cut");
        btnCut.setEnabled(false);
        btnCut.addListener(SWT.Selection, e -> cutOutNewClip(markStart, markEnd));

        final Button btnShowClock = new Button(grpDetails, SWT.CHECK);
        btnShowClock.addListener(SWT.Selection, e -> animationHandler.setShowClock(btnShowClock.getSelection()));

        btnShowClock.setBounds(632, 13, 115, 24);
        btnShowClock.setText("Show Clock");
        btnShowClock.setSelection(true);
        
        Label lblTc = new Label(grpDetails, SWT.NONE);
        lblTc.setBounds(749, 20, 120, 17);
        lblTc.setText("TimeCode");

        shell.setMenuBar(createMenu());

        // Group grpSource = new Group(shell, SWT.NONE);
        // grpSource.setText("Source");
        // grpSource.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
        // 1, 2));
        // grpSource.setLayout(new GridLayout(1, false));

        sourceList = new List(shell, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
        GridData gd_aniList = new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 3);
        gd_aniList.verticalSpan = 1;
        gd_aniList.widthHint = 221;
        gd_aniList.heightHint = 280;
        sourceList.setLayoutData(gd_aniList);

        sourceList.addListener(SWT.Selection, e -> {
            if( selectedAnimation != null ) {
                pullFromWidget(selectedAnimation);
                sourceList.setItem(selectedAnimationIndex, selectedAnimation.getDesc());
            }
            // TODO update palette idx on palMapping
            
            selectedAnimationIndex = sourceList.getSelectionIndex();
            selectedAnimation = sourceAnis.get(selectedAnimationIndex);
            playingAnis.clear();
            playingAnis.add(selectedAnimation);
            animationHandler.setAnimations(playingAnis);
            btnDelete.setEnabled(sourceList.getSelectionCount() > 0);
            bindToWidget();
            
            // add handling of project / palMapping
            if( selectedAnimationIndex>0 && selectedAnimationIndex <= project.palMappings.size() ) {
                // pull palette
                int palIdx = project.palMappings.get(selectedAnimationIndex-1).palIndex;
                paletteViewer.setSelection(new StructuredSelection(palettes.get(palIdx)));
            }
            if( selectedAnimationIndex == 0 ) {
                paletteViewer.setSelection(new StructuredSelection(palettes.get(0)));
            }
        });

        previewCanvas = new Canvas(shell, SWT.BORDER|SWT.DOUBLE_BUFFERED);
        GridData gd_canvas = new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1);
        gd_canvas.heightHint = 250;
        gd_canvas.widthHint = 960;
        previewCanvas.setLayoutData(gd_canvas);
        // canvas.setSize(960, 320);
        previewCanvas.addPaintListener(new DmdPaintListener());
        previewCanvas.setBackground(new Color(display, 10, 10, 10));
        previewCanvas.addListener( SWT.MouseDown, e -> handleMouse(e));
        previewCanvas.addListener( SWT.MouseUp, e -> handleMouse(e));
        previewCanvas.addListener( SWT.MouseMove, e -> handleMouse(e));

        animationHandler = new AnimationHandler(playingAnis, clock, dmd, previewCanvas, false);
        new Label(shell, SWT.NONE);

        final Scale scale = new Scale(shell, SWT.NONE);
        GridData gd_scale = new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1);
        gd_scale.widthHint = 967;
        scale.setLayoutData(gd_scale);
        animationHandler.setScale(scale);
        scale.addListener(SWT.Selection, e -> animationHandler.setPos(scale.getSelection()));

        Group grpActions = new Group(shell, SWT.NONE);
        GridData gd_grpActions = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
        gd_grpActions.heightHint = 192;
        gd_grpActions.widthHint = 242;
        grpActions.setLayoutData(gd_grpActions);
        grpActions.setText("Actions");

        Button btnSave = new Button(grpActions, SWT.NONE);
        btnSave.setBounds(106, 61, 91, 29);
        btnSave.setText("Save Ani");
        btnSave.addListener(SWT.Selection, e -> save());

        btnDelete = new Button(grpActions, SWT.NONE);
        btnDelete.setText("Delete");
        btnDelete.setEnabled(false);
        btnDelete.setBounds(9, 61, 91, 29);
        btnDelete.addListener(SWT.Selection, e -> deleteFromList(sourceList.getSelection(), sourceAnis));

        Button btnLoad = new Button(grpActions, SWT.NONE);
        btnLoad.addListener(SWT.Selection, e -> loadAni(false));

        btnLoad.setText("Load Ani");
        btnLoad.setBounds(9, 26, 91, 29);

        Button btnAdd = new Button(grpActions, SWT.NONE);
        btnAdd.addListener(SWT.Selection, e -> loadAni(true));
        btnAdd.setText("Add Ani");
        btnAdd.setBounds(106, 26, 91, 29);

        Button btnSelectAll = new Button(grpActions, SWT.NONE);
        btnSelectAll.setText("Select All");
        btnSelectAll.setBounds(9, 98, 91, 29);
        btnSelectAll.addListener(SWT.Selection, e -> sourceList.selectAll());
        
        Button btnLoadPal = new Button(grpActions, SWT.NONE);
        btnLoadPal.setBounds(9, 133, 91, 29);
        btnLoadPal.setText("Load Pal");
        btnLoadPal.addListener(SWT.Selection, e -> loadPalette(e));
        
        Button btnSavePal = new Button(grpActions, SWT.NONE);
        btnSavePal.setBounds(106, 133, 91, 29);
        btnSavePal.setText("Save Pal");
        btnSavePal.addListener(SWT.Selection, e -> savePalette(e));
        
        Group grpDetails_1 = new Group(shell, SWT.NONE);
        GridData gd_grpDetails_1 = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
        gd_grpDetails_1.heightHint = 134;
        gd_grpDetails_1.widthHint = 980;
        grpDetails_1.setLayoutData(gd_grpDetails_1);
        grpDetails_1.setText("Details");

        Label lblFsk = new Label(grpDetails_1, SWT.NONE);
        lblFsk.setBounds(17, 35, 35, 17);
        lblFsk.setText("FSK");

        Label lblHold = new Label(grpDetails_1, SWT.NONE);
        lblHold.setText("Hold");
        lblHold.setBounds(139, 35, 35, 17);

        Label lblCycle = new Label(grpDetails_1, SWT.NONE);
        lblCycle.setText("Cycle");
        lblCycle.setBounds(268, 35, 35, 17);

        Label lblTransition = new Label(grpDetails_1, SWT.NONE);
        lblTransition.setText("Transition");
        lblTransition.setBounds(405, 35, 75, 17);

        spinnerCycle = new Spinner(grpDetails_1, SWT.BORDER);
        spinnerCycle.setBounds(309, 30, 90, 27);
        // spinnerCycle.setValues(0, 0, 20);

        spinnerHold = new Spinner(grpDetails_1, SWT.BORDER);
        spinnerHold.setBounds(173, 30, 89, 27);

        comboFsk = new Combo(grpDetails_1, SWT.READ_ONLY);
        comboFsk.setBounds(56, 30, 75, 17);
        comboFsk.setItems(fsks);

        comboTransition = new Combo(grpDetails_1, SWT.READ_ONLY);
        comboTransition.setBounds(478, 30, 106, 27);
        comboTransition.addListener(SWT.Selection, e -> pullTransition(selectedAnimation));

        transitions = buildTransitions(transitionsPath, comboTransition);

        Label lblDelay = new Label(grpDetails_1, SWT.NONE);
        lblDelay.setText("Delay");
        lblDelay.setBounds(590, 35, 45, 17);

        txtTdelay = new Text(grpDetails_1, SWT.BORDER);
        txtTdelay.setText("30");
        txtTdelay.setBounds(644, 30, 75, 27);

        useHash1 = new Button(grpDetails_1, SWT.CHECK);
        useHash1.setBounds(16, 105, 26, 24);    
        useHash2 = new Button(grpDetails_1, SWT.CHECK);
        useHash2.setBounds(16, 123, 26, 24);
        
        Label lblName = new Label(grpDetails_1, SWT.NONE);
        lblName.setText("Name");
        lblName.setBounds(10, 64, 50, 17);

        nameText = new Text(grpDetails_1, SWT.BORDER);
        nameText.setBounds(56, 61, 149, 27);
        
        Button btnRemoveMasks = new Button(grpDetails_1, SWT.NONE);
        btnRemoveMasks.setBounds(590, 79, 119, 29);
        btnRemoveMasks.setText("Remove Masks");
        
        Label hashLabel = new Label(grpDetails_1, SWT.NONE);
        hashLabel.setBounds(48, 108, 404, 35);
        hashLabel.setText("Hashes");
        
        createColorButtons(grpDetails_1);
        
        paletteCombo = new Combo(grpDetails_1, SWT.NONE);
        paletteCombo.setBounds(590, 155, 173, 29);
        paletteViewer = new ComboViewer(paletteCombo);
        paletteViewer.setContentProvider(ArrayContentProvider.getInstance());
        paletteViewer.setLabelProvider(new LabelProvider(){

            @Override
            public String getText(Object element) {
                if( element instanceof Palette) {
                    return ((Palette) element).name;
                }
                return super.getText(element);
            }
            
        } );
        paletteViewer.addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                  if (selection.size() > 0){
                      Palette pal = (Palette)selection.getFirstElement();
                      activePalette = pal.index;
                      dmd.rgb = pal.colors;
                      setColorBtn();
                      
                  }
        });

        
        paletteViewer.setInput(palettes);
        paletteViewer.setSelection(new StructuredSelection(palettes.get(0)));
        
        Label lblPalette = new Label(grpDetails_1, SWT.NONE);
        lblPalette.setBounds(525, 162, 70, 17);
        lblPalette.setText("Palette:");

        Button btnReset = new Button(grpDetails_1, SWT.NONE);
        btnReset.setBounds(715, 79, 106, 29);
        btnReset.setText("Reset Pal");        
        
        btnReset.addListener(SWT.Selection, e -> { 
            dmd.resetColors();
            setColorBtn();
        });
        
        Button btnNewPalette = new Button(grpDetails_1, SWT.NONE);
        btnNewPalette.setBounds(782, 155, 58, 29);
        btnNewPalette.setText("New");
        btnNewPalette.addListener(SWT.Selection, e -> {
            String name = this.paletteCombo.getText();
            if( !isNewPaletteName(name)) {
                name = name+"1";
            }
            Palette p = new Palette(dmd.rgb,palettes.size(), name);
            palettes.add(p);
            paletteViewer.add(p);
            activePalette = palettes.size()-1;
        });
        
        
        Button btnRename = new Button(grpDetails_1, SWT.NONE);
        btnRename.setBounds(846, 155, 75, 29);
        btnRename.setText("Rename");
        btnRename.addListener(SWT.Selection, e -> {
            palettes.get(activePalette).name = paletteCombo.getText();
            paletteViewer.refresh();
        });

        btnRemoveMasks.addListener(SWT.Selection, e -> {
            dmd.removeAllMasks();
            previewCanvas.update();
        });
        
        animationHandler.setLabelHandler(new EventHandler() {

            @Override
            public void notifyAni(AniEvent evt) {
                switch (evt.evtType) {
                case ANI:
                    lblDetails.setText("Frame: " + evt.actFrame);
                    lblTc.setText("TC: "+evt.timecode);
                    hashLabel.setText(evt.getPrintableHashes().replaceAll("plane 1","\nplane 1"));
                    saveHashes(evt.hashes);
                    lastTimeCode = evt.timecode;
                    break;
                case CLOCK:
                    lblDetails.setText("");
                    // sourceList.deselectAll();
                    break;
                }
            }

        });

        shell.open();
        shell.layout();

        display.timerExec(animationHandler.getRefreshDelay(), animationHandler);
        int retry = 0;
        while (true ) {
            try {
                LOG.info("entering event loop");
                while (!shell.isDisposed()) {
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                System.exit(0);
            } catch( Exception e) {
                GlobalExceptionHandler.getInstance().showError(e);
                LOG.error("unexpected error: {}",e);
                if( retry++ > 10 ) System.exit(1);
            }
        }

    }

    private byte[] buildDigest() {
        // TODO Auto-generated method stub
        return null;
    }

    private void createColorButtons(Group grpDetails_1) {
        for(int i = 0; i < colBtn.length; i++) {
            colBtn[i] = new Button(grpDetails_1, SWT.PUSH);
            colBtn[i].setData(Integer.valueOf(i));
            // on mac use wider buttons e.g. 32 pix instead of 26
            colBtn[i].setBounds(525+i*28, 113, 32, 26);
            colBtn[i].setImage(getSquareImage(display, new Color(display,dmd.rgb[i])));
            colBtn[i].addListener(SWT.Selection, e -> {
                ColorDialog cd = new ColorDialog(shell);
                cd.setText("ColorDialog Demo");
                int j = (Integer) e.widget.getData();
                cd.setRGB(dmd.getColor(j));
                RGB newColor = cd.open();
                if (newColor == null) {
                    return;
                }
                ((Button)e.widget).setImage(getSquareImage(display, new Color(display,newColor)));
                dmd.setColor(j, newColor);
            });
        }
    }
    
    private Menu createMenu() {
        Menu menuBar = new Menu(shell, SWT.BAR);
        MenuItem fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        fileMenuHeader.setText("&File");

        Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
        fileMenuHeader.setMenu(fileMenu);

        MenuItem fileLoadItem = new MenuItem(fileMenu, SWT.PUSH);
        fileLoadItem.setText("&Load Project");
        fileLoadItem.addListener(SWT.Selection, e -> loadProject(e));

        MenuItem fileSaveItem = new MenuItem(fileMenu, SWT.PUSH);
        fileSaveItem.setText("&Save Project");
        fileSaveItem.addListener(SWT.Selection, e -> saveProject(e));
        
        new MenuItem(fileMenu, SWT.SEPARATOR);

        MenuItem fileLoadPalItem = new MenuItem(fileMenu, SWT.PUSH);
        fileLoadPalItem.setText("&Load Palette");
        fileLoadPalItem.addListener(SWT.Selection, e -> loadPalette(e));

        MenuItem fileSavePalItem = new MenuItem(fileMenu, SWT.PUSH);
        fileSavePalItem.setText("&Save Palette");
        fileSavePalItem.addListener(SWT.Selection, e -> savePalette(e));
        
        new MenuItem(fileMenu, SWT.SEPARATOR);

        MenuItem fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
        fileExitItem.setText("E&xit");
        fileExitItem.addListener(SWT.Selection, e -> {
            // TODO check dirty before save
            shell.close();
            display.dispose();
        });

        MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuHeader.setText("&Help");

        Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
        helpMenuHeader.setMenu(helpMenu);

        MenuItem helpGetHelpItem = new MenuItem(helpMenu, SWT.PUSH);
        helpGetHelpItem.setText("&Get Help");

        return menuBar;
    }

    private Object savePalette(Event e)
    {
        FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
        fileChooser.setOverwrite(true);
        
        fileChooser.setFileName(palettes.get(activePalette).name);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.xml", "*.json" });
        fileChooser.setFilterNames(new String[] { "Paletten XML", "Paletten JSON" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename != null) {
            LOG.info("store palette to {}",filename);
            storeObject(palettes.get(activePalette), filename);
        }
        return null;
    }

    private void loadPalette(Event e) {
        FileDialog fileChooser = new FileDialog(shell, SWT.OPEN);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.xml;*.json;" });
        fileChooser.setFilterNames(new String[] { "Palette XML", "Palette JSON" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename != null) {
            Palette pal = (Palette) loadObject(filename);
            LOG.info("load palette from {}",filename);
            palettes.add(pal);
            activePalette = palettes.size()-1;
        }
    }
    
    private Object loadProject(Event e) {
        FileDialog fileChooser = new FileDialog(shell, SWT.OPEN);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.xml;*.json;" });
        fileChooser.setFilterNames(new String[] { "Project XML", "Project JSON" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename != null) {
            LOG.info("load project from {}",filename);
            Project projectToLoad  = (Project) loadObject(filename);

            if( projectToLoad != null ) {
                // TODO update frame title
                project = projectToLoad;
            }
        }
        
        // TODO recreate animation list on load of project
        // which means reload initial source file and recreate
        // scenes and populate source list
        
        return null;
    }

    private void saveProject(Event e) {
        FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
        fileChooser.setOverwrite(true);
        //fileChooser.setFileName(project.name);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.xml", "*.json", "*.dat" });
        fileChooser.setFilterNames(new String[] { "Project XML", "Project JSON", "Export dat" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();        
        if (filename != null) {
            LOG.info("write project to {}",filename);
            storeObject(project, filename);
         }
    }

    private void setColorBtn() {
        for (int i = 0; i < colBtn.length; i++) {
            colBtn[i].setImage(getSquareImage(display, new Color(display,dmd.rgb[i])));
        }
    }

    private boolean isNewPaletteName(String text) {
        for(Palette pal : palettes) {
            if( pal.name.equals(text)) return false;
        }
        return true;
    }

    private void handleMouse(Event e)
    {
        switch (e.type) {
        case SWT.MouseDown:
            x1=e.x; y1=e.y;
            button = e.button;
            draw(e);
            break;
        case SWT.MouseUp:
            button = 0;
            x2=e.x; y2=e.y;
            dmd.setMask(x1,y1,x2,y2);
            break;
        case SWT.MouseMove:
            if( button > 0 ) {
                draw(e);
            }
            break;

        default:
            break;
        }
    }
    
    int button = 0;
    Point lastPoint = new Point(0, 0);
    
    private void draw(Event e) {
        Point p = dmd.transformCoord(e.x, e.y);
        if( !p.equals(lastPath)) {
            lastPoint = p;
            if (selectedAnimation != null && animationHandler.isStopped()) {
                selectedAnimation.setPixel( p.x, p.y);
                previewCanvas.update();
            }
        }
    }
    
    static Image getSquareImage(Display display, Color col) {
        Image image = new Image(display, 10, 10);
        GC gc = new GC(image);
        gc.setBackground(col);
        gc.fillRectangle(0, 0, 10, 10);
        //gc.setForeground(col);
        gc.dispose();
        return image;
      }

    /**
     * deletes from the underlying list
     * 
     * @param keys
     *            key to delete
     * @param anis
     *            list to delete from
     */
    protected void deleteFromList(String[] keys, java.util.List<Animation> anis) {
        for (String key : keys) {
            Iterator<Animation> i = anis.iterator();
            while (i.hasNext()) {
                Animation a = i.next();
                if (a.getDesc().equals(key)) {
                    i.remove();
                }
            }
        }
        populateList(sourceList, anis);
        // remove pal mapping
    }

    protected void cutOutNewClip(int start, int end) {
        // only works if current is MAME
        Animation ani = buildMameAnimation(selectedAnimation.getBasePath() + selectedAnimation.getName());
        ani.start = start;
        ani.end = end;
        ani.setDesc(selectedAnimation.getDesc() + (cutNameNumber++));
        sourceAnis.add(ani);
        populateList(sourceList, sourceAnis);
        
        if( palMapping != null ) {
            palMapping.palIndex = palettes.get(activePalette).index;
            palMappings.add(palMapping);
        }
    }

    private java.util.List<String> buildTransitions(String basePath, Combo transitions) {
        Pattern pattern = Pattern.compile("^([a-z_\\.\\-A-Z]*)([0-9]*)\\.png$");
        String[] list = new File(basePath+"transitions/").list();
        LinkedHashSet<String> trans = new LinkedHashSet<String>();
        if (list != null)
            for (String name : list) {
                Matcher matcher = pattern.matcher(name);
                if (matcher.matches()) {
                    // System.out.println(matcher.group(1));
                    if (!matcher.group(1).isEmpty())
                        LOG.debug("name: "+name+" '"+matcher.group(1)+"'");
                        trans.add(matcher.group(1));
                }
            }
        trans.add(NO_TRANS);
        trans.forEach(key -> transitions.add(key));
        return new ArrayList<>(trans);
    }

    private String transitionsPath = "./";//home/sr/Downloads/Pinball/";

    private class DmdPaintListener implements PaintListener {

        public void paintControl(PaintEvent e) {
            dmd.draw(e);
            if (animationHandler != null)
                e.display.timerExec(animationHandler.getRefreshDelay(), animationHandler);
            e.gc.dispose();
        }
    }

    protected void save() {
        FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
        fileChooser.setOverwrite(true);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.ani" });
        fileChooser.setFilterNames(new String[] { "Animationen", "ani" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename == null)
            return;
        AnimationCompiler.writeToCompiledFile(sourceAnis, filename);
    }

    // TODO switch also palMapping / palette
    private void bindToWidget() {
        if (selectedAnimation != null) {
            comboFsk.select(comboFsk.indexOf(String.valueOf(selectedAnimation.getFsk())));
            spinnerCycle.setSelection(selectedAnimation.getCycles());
            spinnerHold.setSelection(selectedAnimation.getHoldCycles());
            txtTdelay.setText(String.valueOf(selectedAnimation.getTransitionDelay()));
            nameText.setText(selectedAnimation.getDesc());
            if( selectedAnimation.getTransitionName()==null || selectedAnimation.getTransitionName().isEmpty()) {
                comboTransition.deselectAll();
            } else {
                comboTransition.select(transitions.indexOf(selectedAnimation.getTransitionName()));
            }
        }
    }
    
    private void pullFromWidget(Animation ani) {
        ani.setFsk(Integer.valueOf(fsks[comboFsk.getSelectionIndex()]));
        ani.setCycles(spinnerCycle.getSelection());
        ani.setHoldCycles(spinnerHold.getSelection());
        ani.setDesc(nameText.getText());
        pullTransition(ani);
    }

    private void pullTransition(Animation ani) {
        int index = comboTransition.getSelectionIndex();
        if( index != -1 && !transitions.get(index).equals(NO_TRANS)) {
            ani.setTransitionDelay(Integer.valueOf(txtTdelay.getText()));
            ani.setTransitionName(transitions.get(index));
            ani.setTransitionFrom(ani.end);
            ani.setTransitionsPath(transitionsPath);
        } else {
            ani.setTransitionName(null);
            ani.setTransitionFrom(0);
        }
    }


    protected void loadAni(boolean append) {
        FileDialog fileChooser = new FileDialog(shell, SWT.OPEN);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.properties;*.ani;*.txt.gz" });
        fileChooser.setFilterNames(new String[] { "Animationen", "properties, txt.gz, ani" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename == null)
            return;

        java.util.List<Animation> loadedList = new ArrayList<>();
        if (filename.endsWith(".ani")) {
            loadedList.addAll(AnimationCompiler.readFromCompiledFile(filename));
        } else if (filename.endsWith(".txt.gz")) {
            loadedList.add(buildMameAnimation(filename));
        } else if (filename.endsWith(".properties")) {
            loadedList.addAll(AnimationFactory.createAnimationsFromProperties(filename));
        }
        project.inputFile = new File(filename).getName();
        
        // animationHandler.setAnimations(sourceAnis);
        if (!append) {
            sourceAnis.clear();
            selectedAnimation = null;
            selectedAnimationIndex = 0;
            playingAnis.clear();
        }
        sourceAnis.addAll(loadedList);
        populateList(sourceList, sourceAnis);
    }

    private Animation buildMameAnimation(String filename) {
        File file = new File(filename);
        String base = file.getName();
        Animation ani = new Animation(AnimationType.MAME, base, 0, 0, 1, 1, 0);
        ani.setBasePath(file.getParent() + "/");
        ani.setDesc(base.substring(0, base.indexOf('.')));
        return ani;
    }

    private void populateList(List list, java.util.List<Animation> anis) {
        list.removeAll();
        anis.forEach(animation -> list.add(animation.getDesc()));
    }
}
