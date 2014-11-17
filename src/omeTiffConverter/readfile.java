package omeTiffConverter;

import java.awt.Rectangle;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.*;
import ij.measure.Calibration;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.process.ShortProcessor;
import loci.formats.services.OMEXMLService;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.out.OMETiffWriter;
import loci.common.DataTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.IFormatWriter;
import loci.formats.ImageReader;
import loci.formats.ImageWriter;
import loci.formats.MetadataTools;
import loci.formats.gui.AWTImageTools;
import loci.formats.gui.ExtensionFileFilter;
import loci.formats.gui.GUITools;
import loci.formats.gui.Index16ColorModel;
import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import loci.formats.meta.IMetadata;
import loci.plugins.BF;
import loci.plugins.LociExporter;
import loci.plugins.util.RecordedImageProcessor;
import loci.plugins.util.WindowTools;
import loci.plugins.util.ROIHandler;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.cli.ErrorHandler;
import ome.formats.importer.cli.LoggingImportMonitor;
import ome.model.containers.Dataset;
import ome.xml.meta.OMEXMLMetadataRoot;
import ome.xml.model.primitives.PositiveFloat;
import ome.xml.model.primitives.PositiveInteger;
import omero.RDouble;
import omero.RString;
import omero.rtypes;
import omero.api.IRoiPrx;
import omero.api.RoiResult;
import omero.api.ServiceFactoryPrx;
import omero.model.EllipseI;
import omero.model.LineI;
import omero.model.PointI;
import omero.model.Polygon;
import omero.model.PolygonI;
import omero.model.PolylineI;
import omero.model.RectI;
import omero.model.Shape;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.PixelType;
import ome.xml.model.enums.DimensionOrder;





public class readfile {

    /**
     * 
     * @return
     */
    public Roi[] readFromRoiManager(){

        RoiManager manager = RoiManager.getInstance();
        Roi[] rois = manager.getRoisAsArray();
        return rois;
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public Shape readRoiFile() throws Exception{

        //Read ROI File
        OpenDialog od1 = new OpenDialog("Select an ROI File", null); 
        String aInputDirectory = od1.getDirectory();
        String aInputFileName = aInputDirectory + od1.getFileName();

        RoiDecoder roi = new RoiDecoder(aInputFileName);
        Roi roi1 = roi.getShapeRoi();
        Shape shape = convertToOmeroRoi(roi1);

        return shape;	
    }


    /**
     * Converts an ImageJ shape into an OMERO shape.
     *
     * @param roiType The shape to convert.
     * @return See above
     * @throws Exception
     */
    public Shape convertToOmeroRoi(Roi roiType) throws Exception {

        if (roiType == null) 
            throw new IllegalArgumentException("ROI cannot be null.");
        String st1 = roiType.getTypeAsString();
        Shape shape = null;
        if (st1.matches("Straight Line")){
            //extract x1,x2,y1,y2 from ImageJ Roi

            int[] xcoords = roiType.getPolygon().xpoints;
            int[] ycoords = roiType.getPolygon().ypoints;

            //omero straight line params
            LineI line = new LineI();
            line.setX1(rdouble(xcoords[0]));
            line.setX2(rdouble(xcoords[1]));
            line.setY1(rdouble(ycoords[0]));
            line.setY2(rdouble(ycoords[1]));
            shape = line;
            //Link to omero image id
            //save rois from imagej as roi files and attach as annotations to the imageid
        }else if(st1.matches("Rectangle")){

            //extract x,y,w,h from ImageJ Roi
            Rectangle vnRectBounds = roiType.getPolygon().getBounds();
            int x = vnRectBounds.x;
            int y = vnRectBounds.y;
            int w = vnRectBounds.width;
            int h = vnRectBounds.height;

            //omero rectangle params
            RectI rectangle = new RectI();
            rectangle.setX(rdouble(x));
            rectangle.setY(rdouble(y));
            rectangle.setWidth(rdouble(w));
            rectangle.setHeight(rdouble(h));
            shape = rectangle;
        }else if(st1.matches("Oval")){

            Rectangle vnRectBounds = roiType.getPolygon().getBounds();
            int x = vnRectBounds.x;
            int y = vnRectBounds.y;
            int rx = vnRectBounds.width;
            int ry = vnRectBounds.height;

            EllipseI ellipse = new EllipseI();
            ellipse.setCx(rdouble(x));
            ellipse.setCy(rdouble(y));
            ellipse.setRx(rdouble(rx));
            ellipse.setRy(rdouble(ry));
            shape = ellipse;
        }else if(st1.matches("Polygon") || st1.matches("Angle") || st1.matches("Freehand") || st1.matches("Polyline") || st1.matches("Freeline")){


            int[] x = roiType.getPolygon().xpoints;
            int[] y = roiType.getPolygon().ypoints;

            System.out.println(st1);
            String points = "1";
            for (int i=0 ; i<x.length ; i++){
                System.out.println(x[i] + "," + y[i]);  
                if(i==0){                    
                    points = (x[i] + "," + y[i]);
                }else{
                    points= (points + " " + x[i] + "," + y[i]);
                }

            }

            if(st1.matches("Polyline") || st1.matches("Freeline")){
                PolylineI polyline = new PolylineI();
                polyline.setPoints(rtypes.rstring(points));
                shape = polyline;
            }else{
                PolygonI polygon = new PolygonI();
                polygon.setPoints(rtypes.rstring(points));
                polygon.getPoints();
                shape=polygon;
            }

        }else if(st1.matches("Point")){

            int[] x = roiType.getPolygon().xpoints;
            int[] y = roiType.getPolygon().ypoints;

            PointI point = new PointI();
            point.setCx(rdouble(x[0]));
            point.setCy(rdouble(y[0]));
            shape = point;

        }

        return shape;
    }

    private int[] toIntArray(List<Integer> list)  {
        int[] ret = new int[list.size()];
        int i = 0;
        for (Integer e : list)  
            ret[i++] = e.intValue();
        return ret;
    }
    /**
     * Converts Omero Roi's to ImageJ Roi's
     * @param session
     * @param imageId
     * @throws Exception
     */
    public void convertToImagejRoi(ServiceFactoryPrx session,long imageId) throws Exception {

        IRoiPrx service = session.getRoiService();
        RoiResult roiResult = service.findByImage(imageId, null);
        List<omero.model.Roi> rois = roiResult.rois;
        int n = rois.size();
        //        Roi roi = null;
        //        shapeType = "";
        for (int thisROI=0  ; thisROI<n ; thisROI++){
            omero.model.Roi roi = rois.get(thisROI-1);
            int numShapes = roi.sizeOfShapes();
            for(int ns=0 ; ns<numShapes ; ns++){
                Shape shape = roi.getShape(ns-1);
                ImagePlus imp = IJ.getImage();
                RString values = null;

                if(shape instanceof PolygonI || shape instanceof PolylineI) {


                    if(shape instanceof PolygonI){
                        PolygonI shape1 = (PolygonI) roi.getShape(ns-1);
                        values = shape1.getPoints();
                    }else{
                        PolylineI shape1 = (PolylineI) roi.getShape(ns-1);
                        values = shape1.getPoints();
                    }

                    String points = values.getValue();

                    List<Integer> x = new ArrayList<Integer>();
                    List<Integer> y = new ArrayList<Integer>();
                    StringTokenizer tt = new StringTokenizer(points, " ");
                    int numTokens = tt.countTokens();
                    StringTokenizer t;
                    int total;
                    for (int i = 0; i < numTokens; i++) {
                        t = new StringTokenizer(tt.nextToken(), ",");
                        total = t.countTokens()/2;
                        for (int j = 0; j < total; j++) {
                            x.add(new Integer(t.nextToken()));
                            y.add(new Integer(t.nextToken()));
                        }
                    }
                    int[] xpoints = toIntArray(x);
                    int[] ypoints = toIntArray(y);
                    for (int i = 0; i < ypoints.length; i++) {
                        System.err.println("y :"+ypoints[i]);
                    }
                    for (int i = 0; i < xpoints.length; i++) {
                        System.err.println("x :"+xpoints[i]);
                    }

                    PolygonRoi roi1 = new PolygonRoi(xpoints,ypoints,4,Roi.POLYGON);
                    imp.setRoi(roi1);

                }if(shape instanceof LineI){
                    LineI shape1 = (LineI) roi.getShape(ns-1);
                    double x1 = shape1.getX1().getValue();
                    double y1 = shape1.getY1().getValue();
                    double x2 = shape1.getX2().getValue();
                    double y2 = shape1.getY2().getValue();
                    Line roi1 = new Line((int) (x1),(int) (y1),(int) (x2), (int) (y2),imp);
                    imp.setRoi(roi1);

                }if(shape instanceof PointI){
                    PointI shape1 = (PointI) roi.getShape(ns-1);
                    double ox1 = shape1.getCx().getValue();
                    double oy1 = shape1.getCy().getValue();

                    PointRoi roi1 = new PointRoi((int) ox1,(int) oy1);
                    imp.setRoi(roi1);

                }if(shape instanceof EllipseI || shape instanceof RectI){
                    EllipseI shape1 = (EllipseI) roi.getShape(ns-1);
                    double x1 = shape1.getCx().getValue();
                    double y1 = shape1.getCy().getValue();
                    double width = shape1.getRx().getValue();
                    double height = shape1.getRy().getValue();

                    if(shape instanceof EllipseI){
                        OvalRoi roi1 = new OvalRoi((int) x1,(int) y1,(int) width,(int) height);
                        imp.setRoi(roi1);
                    }
                    if(shape instanceof RectI){
                        Roi roi1 = new Roi((int) x1,(int) y1,(int) width,(int) height);
                        imp.setRoi(roi1);
                    }
                }

            }

        }


    }

    /**
     * 
     * @param image
     * @throws DependencyException
     * @throws ServiceException
     * @throws FormatException
     * @throws IOException
     * @throws EnumerationException 
     */

    public void ExporterWithImageJRoi() throws DependencyException, ServiceException, FormatException, IOException, EnumerationException{

        ImagePlus image = IJ.getImage();
        LociExporter plugin = new LociExporter();
        String ORDER = "XYCZT";

        String outfile = null;
        Boolean splitZ = null;
        Boolean splitC = null;
        Boolean splitT = null;

        if (plugin.arg != null) {
            outfile = Macro.getValue(plugin.arg, "outfile", null);

            String z = Macro.getValue(plugin.arg, "splitZ", null);
            String c = Macro.getValue(plugin.arg, "splitC", null);
            String t = Macro.getValue(plugin.arg, "splitT", null);

            splitZ = z == null ? null : Boolean.valueOf(z);
            splitC = c == null ? null : Boolean.valueOf(c);
            splitT = t == null ? null : Boolean.valueOf(t);
            plugin.arg = null;
        }

        if (outfile == null) {
            String options = Macro.getOptions();
            if (options != null) {
                String save = Macro.getValue(options, "save", null);
                if (save != null) outfile = save;
            }
        }

        if (outfile == null || outfile.length() == 0) {
            // open a dialog prompting for the filename to save

            //SaveDialog sd = new SaveDialog("Bio-Formats Exporter", "", "");
            //String dir = sd.getDirectory();
            //String name = sd.getFileName();

            // NB: Copied and adapted from ij.io.SaveDIalog.jSaveDispatchThread,
            // so that the save dialog has a file filter for choosing output format.

            String dir = null, name = null;
            JFileChooser fc = GUITools.buildFileChooser(new ImageWriter(), false);
            fc.setDialogTitle("Bio-Formats Exporter");
            String defaultDir = OpenDialog.getDefaultDirectory();
            if (defaultDir != null) fc.setCurrentDirectory(new File(defaultDir));

            // set OME-TIFF as the default output format
            FileFilter[] ff = fc.getChoosableFileFilters();
            FileFilter defaultFilter = null;
            for (int i=0; i<ff.length; i++) {
                if (ff[i] instanceof ExtensionFileFilter) {
                    ExtensionFileFilter eff = (ExtensionFileFilter) ff[i];
                    if (i == 0 || eff.getExtension().equals("ome.tif")) {
                        defaultFilter = eff;
                        break;
                    }
                }
            }
            if (defaultFilter != null) fc.setFileFilter(defaultFilter);

            int returnVal = fc.showSaveDialog(IJ.getInstance());
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                Macro.abort();
                return;
            }
            File f = fc.getSelectedFile();
            if (f.exists()) {
                int ret = JOptionPane.showConfirmDialog(fc,
                        "The file " + f.getName() + " already exists. \n" +
                                "Would you like to replace it?", "Replace?",
                                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret != JOptionPane.OK_OPTION) f = null;
            }
            if (f == null) Macro.abort();
            else {
                dir = fc.getCurrentDirectory().getPath() + File.separator;
                name = fc.getName(f);

                // ensure filename matches selected filter
                FileFilter filter = fc.getFileFilter();
                if (filter instanceof ExtensionFileFilter) {
                    ExtensionFileFilter eff = (ExtensionFileFilter) filter;
                    String[] ext = eff.getExtensions();
                    String lName = name.toLowerCase();
                    boolean hasExtension = false;
                    for (int i=0; i<ext.length; i++) {
                        if (lName.endsWith("." + ext[i])) {
                            hasExtension = true;
                            break;
                        }
                    }
                    if (!hasExtension && ext.length > 0) {
                        // append chosen extension
                        name = name + "." + ext[0];
                    }
                }

                // do some ImageJ bookkeeping
                OpenDialog.setDefaultDirectory(dir);
                if (Recorder.record) Recorder.recordPath("save", dir+name);
            }

            if (dir == null || name == null) return;
            outfile = new File(dir, name).getAbsolutePath();
            if (outfile == null) return;
        }

        if (splitZ == null || splitC == null || splitT == null) {
            // ask if we want to export multiple files

            GenericDialog multiFile =
                    new GenericDialog("Bio-Formats Exporter - Multiple Files");
            multiFile.addCheckbox("Write_each_Z_section to a separate file", false);
            multiFile.addCheckbox("Write_each_timepoint to a separate file", false);
            multiFile.addCheckbox("Write_each_channel to a separate file", false);
            multiFile.showDialog();

            splitZ = multiFile.getNextBoolean();
            splitT = multiFile.getNextBoolean();
            splitC = multiFile.getNextBoolean();
        }


        //Extract Pixel Type
        int ptype = 0;
        int channels = image.getNChannels();

        switch (image.getType()) {
            case ImagePlus.GRAY8:
            case ImagePlus.COLOR_256:
                ptype = FormatTools.UINT8;
                break;
            case ImagePlus.COLOR_RGB:
                channels = 3;
                ptype = FormatTools.UINT8;
                break;
            case ImagePlus.GRAY16:
                ptype = FormatTools.UINT16;
                break;
            case ImagePlus.GRAY32:
                ptype = FormatTools.FLOAT;
                break;
        }
        String title = image.getTitle();
        @SuppressWarnings("resource")
        IFormatWriter w = new ImageWriter().getWriter(outfile);
        w.setWriteSequentially(true);

        FileInfo fi = image.getOriginalFileInfo();
        String xml = fi == null ? null : fi.description == null ? null :
            fi.description.indexOf("xml") == -1 ? null : fi.description;

        OMEXMLService service = null;
        IMetadata store = null;

        try {
            ServiceFactory factory = new ServiceFactory();
            service = factory.getInstance(OMEXMLService.class);
            store = service.createOMEXMLMetadata(xml);
        }
        catch (DependencyException de) { }
        catch (ServiceException se) { }

        if (store == null) IJ.error("OME-XML Java library not found.");
        if (xml == null) {
            store.createRoot();
        }
        else if (store.getImageCount() > 1) {
            // the original dataset had multiple series
            // we need to modify the IMetadata to represent the correct series

            ArrayList<Integer> matchingSeries = new ArrayList<Integer>();
            for (int series=0; series<store.getImageCount(); series++) {
                String type = store.getPixelsType(series).toString();
                int pixelType = FormatTools.pixelTypeFromString(type);
                if (pixelType == ptype) {
                    String imageName = store.getImageName(series);
                    if (title.indexOf(imageName) >= 0) {
                        matchingSeries.add(series);
                    }
                }
            }

            int series = 0;
            if (matchingSeries.size() > 1) {
                for (int i=0; i<matchingSeries.size(); i++) {
                    int index = matchingSeries.get(i);
                    String name = store.getImageName(index);
                    boolean valid = true;
                    for (int j=0; j<matchingSeries.size(); j++) {
                        if (i != j) {
                            String compName = store.getImageName(matchingSeries.get(j));
                            if (compName.indexOf(name) >= 0) {
                                valid = false;
                                break;
                            }
                        }
                    }
                    if (valid) {
                        series = index;
                        break;
                    }
                }
            }
            else if (matchingSeries.size() == 1) series = matchingSeries.get(0);
            OMEXMLMetadataRoot root = (OMEXMLMetadataRoot) store.getRoot();
            ome.xml.model.Image exportImage = root.getImage(series);
            List<ome.xml.model.Image> allImages = root.copyImageList();
            for (ome.xml.model.Image img : allImages) {
                if (!img.equals(exportImage)) {
                    root.removeImage(img);
                }
            }
            store.setRoot(root);
        }

        //Set Pixels ID
        String pixelsID = MetadataTools.createLSID("Pixels", 0);
        store.setPixelsID(pixelsID, 0);

        //Set Pixels Type
        store.setPixelsType(PixelType.fromString(FormatTools.getPixelTypeString(ptype)), 0);       

        //Set Image Dimensions
        store.setPixelsSizeX(new PositiveInteger(image.getWidth()), 0);
        store.setPixelsSizeY(new PositiveInteger(image.getHeight()), 0);
        store.setPixelsSizeZ(new PositiveInteger(image.getNSlices()), 0);
        store.setPixelsSizeC(new PositiveInteger(channels*image.getNChannels()), 0);
        store.setPixelsSizeT(new PositiveInteger(image.getNFrames()), 0);

        if (store.getImageID(0) == null) {
            store.setImageID(MetadataTools.createLSID("Image", 0), 0);
        }
        if (store.getPixelsID(0) == null) {
            store.setPixelsID(MetadataTools.createLSID("Pixels", 0), 0);
        }

        // always reset the pixel type
        // this prevents problems if the user changed the bit depth of the image
        try {
            store.setPixelsType(PixelType.fromString(
                    FormatTools.getPixelTypeString(ptype)), 0);
        }
        catch (EnumerationException e) { }

        if (store.getPixelsBinDataCount(0) == 0 ||
                store.getPixelsBinDataBigEndian(0, 0) == null)
        {
            store.setPixelsBinDataBigEndian(Boolean.FALSE, 0, 0);
        }
        if (store.getPixelsDimensionOrder(0) == null) {
            try {
                store.setPixelsDimensionOrder(DimensionOrder.fromString(ORDER), 0);
            }
            catch (EnumerationException e) { }
        }

        LUT[] luts = new LUT[image.getNChannels()];

        for (int c=0; c<image.getNChannels(); c++) {
            if (c >= store.getChannelCount(0) || store.getChannelID(0, c) == null) {
                String lsid = MetadataTools.createLSID("Channel", 0, c);
                store.setChannelID(lsid, 0, c);
            }
            store.setChannelSamplesPerPixel(new PositiveInteger(channels), 0, 0);

            if (image instanceof CompositeImage) {
                luts[c] = ((CompositeImage) image).getChannelLut(c + 1);
            }
        }
        Calibration cal = image.getCalibration();

        //Set Pixel Calibration (Size)
        store.setPixelsPhysicalSizeX(new PositiveFloat(cal.pixelWidth), 0);
        store.setPixelsPhysicalSizeY(new PositiveFloat(cal.pixelHeight), 0);
        store.setPixelsPhysicalSizeZ(new PositiveFloat(cal.pixelDepth), 0);
        //        store.setPixelsTimeIncrement(new Time(new Double(cal.frameInterval), UNITS.S), 0);

        if (image.getImageStackSize() !=
                image.getNChannels() * image.getNSlices() * image.getNFrames())
        {
            IJ.showMessageWithCancel("Bio-Formats Exporter Warning",
                    "The number of planes in the stack (" + image.getImageStackSize() +
                    ") does not match the number of expected planes (" +
                    (image.getNChannels() * image.getNSlices() * image.getNFrames()) + ")." +
                    "\nIf you select 'OK', only " + image.getImageStackSize() +
                    " planes will be exported. If you wish to export all of the " +
                    "planes,\nselect 'Cancel' and convert the Image5D window " +
                    "to a stack.");
            store.setPixelsSizeZ(new PositiveInteger(image.getImageStackSize()), 0);
            store.setPixelsSizeC(new PositiveInteger(1), 0);
            store.setPixelsSizeT(new PositiveInteger(1), 0);
        }

        String[] outputFiles = new String[] {outfile};

        int sizeZ = store.getPixelsSizeZ(0).getValue();
        int sizeC = store.getPixelsSizeC(0).getValue();
        int sizeT = store.getPixelsSizeT(0).getValue();

        if (splitZ || splitC || splitT) {
            int nFiles = 1;
            if (splitZ) {
                nFiles *= sizeZ;
            }
            if (splitC) {
                nFiles *= sizeC;
            }
            if (splitT) {
                nFiles *= sizeT;
            }

            outputFiles = new String[nFiles];

            int dot = outfile.indexOf(".", outfile.lastIndexOf(File.separator));
            String base = outfile.substring(0, dot);
            String ext = outfile.substring(dot);

            int nextFile = 0;
            for (int z=0; z<(splitZ ? sizeZ : 1); z++) {
                for (int c=0; c<(splitC ? sizeC : 1); c++) {
                    for (int t=0; t<(splitT ? sizeT : 1); t++) {
                        outputFiles[nextFile++] = base + (splitZ ? "_Z" + z : "") +
                                (splitC ? "_C" + c : "") + (splitT ? "_T" + t : "") + ext;
                    }
                }
            }
        }

        if (!w.getFormat().startsWith("OME")) {
            if (splitZ) {
                store.setPixelsSizeZ(new PositiveInteger(1), 0);
            }
            if (splitC) {
                store.setPixelsSizeC(new PositiveInteger(1), 0);
            }
            if (splitT) {
                store.setPixelsSizeT(new PositiveInteger(1), 0);
            }
        }

        store = RoiHandler1.openImageJRois(store);

        w.setMetadataRetrieve(store);
        String[] codecs = w.getCompressionTypes();
        ImageProcessor proc = image.getImageStack().getProcessor(1);
        Image firstImage = proc.createImage();
        firstImage = AWTImageTools.makeBuffered(firstImage, proc.getColorModel());
        int thisType = AWTImageTools.getPixelType((BufferedImage) firstImage);
        if (proc instanceof ColorProcessor) {
            thisType = FormatTools.UINT8;
        }
        else if (proc instanceof ShortProcessor) {
            thisType = FormatTools.UINT16;
        }

        boolean notSupportedType = !w.isSupportedType(thisType);
        if (notSupportedType) {
            IJ.error("Pixel type (" + FormatTools.getPixelTypeString(thisType) +
                    ") not supported by this format.");
        }

        if (codecs != null && codecs.length > 1) {
            GenericDialog gd =
                    new GenericDialog("Bio-Formats Exporter Options");
            gd.addChoice("Compression type: ", codecs, codecs[0]);
            gd.showDialog();
            if (gd.wasCanceled()) return;

            w.setCompression(gd.getNextChoice());
        }

        // convert and save slices

        int size = image.getImageStackSize();
        ImageStack is = image.getImageStack();
        boolean doStack = w.canDoStacks() && size > 1;
        int start = doStack ? 0 : image.getCurrentSlice() - 1;
        int end = doStack ? size : start + 1;

        boolean littleEndian =
                !w.getMetadataRetrieve().getPixelsBinDataBigEndian(0, 0).booleanValue();
        byte[] plane = null;
        w.setInterleaved(false);

        int[] no = new int[outputFiles.length];
        for (int i=start; i<end; i++) {
            if (doStack) {
                BF.status(false, "Saving plane " + (i + 1) + "/" + size);
                BF.progress(false, i, size);
            }
            else BF.status(false, "Saving image");
            proc = is.getProcessor(i + 1);

            if (proc instanceof RecordedImageProcessor) {
                proc = ((RecordedImageProcessor) proc).getChild();
            }

            int x = proc.getWidth();
            int y = proc.getHeight();

            if (proc instanceof ByteProcessor) {
                plane = (byte[]) proc.getPixels();
            }
            else if (proc instanceof ShortProcessor) {
                plane = DataTools.shortsToBytes(
                        (short[]) proc.getPixels(), littleEndian);
            }
            else if (proc instanceof FloatProcessor) {
                plane = DataTools.floatsToBytes(
                        (float[]) proc.getPixels(), littleEndian);
            }
            else if (proc instanceof ColorProcessor) {
                byte[][] pix = new byte[3][x*y];
                ((ColorProcessor) proc).getRGB(pix[0], pix[1], pix[2]);
                plane = new byte[3 * x * y];
                System.arraycopy(pix[0], 0, plane, 0, x * y);
                System.arraycopy(pix[1], 0, plane, x * y, x * y);
                System.arraycopy(pix[2], 0, plane, 2 * x * y, x * y);

                if (i == start) {
                    sizeC /= 3;
                }
            }

            int fileIndex = 0;
            if (doStack) {
                int[] coords =
                        FormatTools.getZCTCoords(ORDER, sizeZ, sizeC, sizeT, size, i);
                int realZ = sizeZ;
                int realC = sizeC;
                int realT = sizeT;

                if (!splitZ) {
                    coords[0] = 0;
                    realZ = 1;
                }
                if (!splitC) {
                    coords[1] = 0;
                    realC = 1;
                }
                if (!splitT) {
                    coords[2] = 0;
                    realT = 1;
                }
                fileIndex = FormatTools.getIndex(ORDER, realZ, realC, realT,
                        realZ * realC * realT, coords[0], coords[1], coords[2]);
            }
            if (notSupportedType) {
                IJ.error("Pixel type not supported by this format.");
            }
            else {
                w.changeOutputFile(outputFiles[fileIndex]);

                int currentChannel = FormatTools.getZCTCoords(
                        ORDER, sizeZ, sizeC, sizeT, image.getStackSize(), i)[1];

                if (luts[currentChannel] != null) {
                    // expand to 16-bit LUT if necessary

                    int bpp = FormatTools.getBytesPerPixel(thisType);
                    if (bpp == 1) {
                        w.setColorModel(luts[currentChannel]);
                    }
                    else if (bpp == 2) {
                        int lutSize = luts[currentChannel].getMapSize();
                        byte[][] lut = new byte[3][lutSize];
                        luts[currentChannel].getReds(lut[0]);
                        luts[currentChannel].getGreens(lut[1]);
                        luts[currentChannel].getBlues(lut[2]);

                        short[][] newLut = new short[3][65536];
                        int bins = newLut[0].length / lut[0].length;
                        for (int c=0; c<newLut.length; c++) {
                            for (int q=0; q<newLut[c].length; q++) {
                                int index = q / bins;
                                newLut[c][q] = (short) ((lut[c][index] * lut[0].length) + (q % bins));
                            }
                        }

                        w.setColorModel(new Index16ColorModel(16, newLut[0].length,
                                newLut, littleEndian));
                    }
                }
                else {
                    w.setColorModel(proc.getColorModel());
                }
                w.saveBytes(no[fileIndex]++, plane);
            }
        }
        w.close();
        System.out.println(" [done]");
    }







    private RDouble rdouble(int x1) {
        // TODO Auto-generated method stub
        return null;
    }

    public void omeroImport(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("Usage: OmeroImport <host> <port> " +
                    "<username> <password> <path>");
            System.exit(1);
        }

        // Set up configuration parameters
        ImportConfig config = new ImportConfig();
        config.email.set("");
        config.sendFiles.set(true);
        config.sendReport.set(false);
        config.contOnError.set(false);
        config.debug.set(false);
        config.hostname.set(args[0]);
        config.port.set(Integer.parseInt(args[1]));
        config.username.set(args[2]);
        config.password.set(args[3]);
        config.targetClass.set(Dataset.class.getName());
        //        objectTypes ot = new getObjectTypes();
        config.targetId.set(Long.parseLong(args[4]));

        String[] paths = new String[] { args[5] };

        OMEROMetadataStoreClient store = config.createStore();
        try {
            store.logVersionInfo(config.getIniVersionNumber());
            OMEROWrapper reader = new OMEROWrapper(config);
            ImportLibrary library = new ImportLibrary(store, reader);
            ErrorHandler handler = new ErrorHandler(config);
            library.addObserver(new LoggingImportMonitor());

            ImportCandidates candidates =
                    new ImportCandidates(reader, paths, handler);
            reader.setMetadataOptions(
                    new DefaultMetadataOptions(MetadataLevel.ALL));
            boolean success = library.importCandidates(config, candidates);
            if (success) {
                System.exit(0);
            } else {
                System.exit(1);
            }
        } finally {
            store.logout();
        }
    }


    class resultObject{ 
        public RoiManager manager;
        public int vnRectBoundsx,vnRectBoundsy,vnRectBoundsheight,vnRectBoundswidth,NumOfRois,numofrois,slice;;
        public int[] polygonx,polygony;
        public float [] xcoords,ycoords;
        public String strName;
        public Roi roivec;
        //		public Color [] getFillColor,getStrokeColor;
        public OMEXMLMetadata omexmlMeta; 
        public Hashtable<String, Roi> table;
        public ImageReader reader;

    }
}

