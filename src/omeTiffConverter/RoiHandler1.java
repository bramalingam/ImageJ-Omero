/*
 *------------------------------------------------------------------------------
 *  Copyright (C) 2014 University of Dundee. All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package omeTiffConverter;

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import ij.plugin.frame.RoiManager;

import java.awt.Color;
import java.awt.Rectangle;

import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataStore;
import loci.formats.ome.OMEXMLMetadata;
import ome.xml.model.Ellipse;
import ome.xml.model.Image;
import ome.xml.model.OME;
import ome.xml.model.Point;
import ome.xml.model.Polygon;
import ome.xml.model.Polyline;
import ome.xml.model.Shape;
import ome.xml.model.Union;
import omero.RDouble;
import omero.rtypes;
import omero.model.EllipseI;
import omero.model.LineI;
import omero.model.PointI;
import omero.model.PolygonI;
import omero.model.PolylineI;
import omero.model.Rect;
import omero.model.RectI;



/**
 * 
 *
 * @author Balaji Ramalingam &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:b.ramalingam@dundee.ac.uk">b.ramalingam@dundee.ac.uk</a>
 * @since 5.1
 */
public class RoiHandler1 {

    public static Roi[] readFromRoiManager(){

        RoiManager manager = RoiManager.getInstance();
        Roi[] rois = manager.getRoisAsArray();
        return rois;
    }
    
    public omero.model.Shape ImageJToOmeroRoi(Roi roiType) throws Exception {

        if (roiType == null) 
            throw new IllegalArgumentException("ROI cannot be null.");
        String st1 = roiType.getTypeAsString();
        omero.model.Shape shape = null;
        if (st1.matches("Straight Line")){
            //extract x1,x2,y1,y2 from ImageJ Roi

            int[] xcoords = roiType.getPolygon().xpoints;
            int[] ycoords = roiType.getPolygon().ypoints;

            //omero straight line params
            omero.model.Line line = new LineI();
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
            omero.model.Rect rectangle = new RectI();
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

            omero.model.Ellipse ellipse = new EllipseI();
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
                omero.model.Polyline polyline = new PolylineI();
                polyline.setPoints(rtypes.rstring(points));
                shape = polyline;
            }else{
                omero.model.Polygon polygon = new PolygonI();
                polygon.setPoints(rtypes.rstring(points));
                polygon.getPoints();
                shape=polygon;
            }

        }else if(st1.matches("Point")){

            int[] x = roiType.getPolygon().xpoints;
            int[] y = roiType.getPolygon().ypoints;

            omero.model.Point point = new PointI();
            point.setCx(rdouble(x[0]));
            point.setCy(rdouble(y[0]));
            shape = point;

        }

        return shape;
    }

    private RDouble rdouble(int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public static IMetadata openImageJRois(IMetadata store){


        Roi[] rois = readFromRoiManager();
        Roi roiType = rois[0];

        System.out.println(roiType.getTypeAsString()); 

        //Get Roitype from ImageJ
        int roiIndex = 0;String roiID = null;
        String st1 = roiType.getTypeAsString();
        roiID = null;

        for (int shapeIndex=0; shapeIndex<rois.length; shapeIndex++){


            if (st1.matches("Straight Line") || st1.matches("Point")){

                String polylineID = MetadataTools.createLSID("Shape", roiIndex,shapeIndex);
                roiID = MetadataTools.createLSID("ROI", roiIndex, shapeIndex);


                int[] xcoords = roiType.getPolygon().xpoints;
                int[] ycoords = roiType.getPolygon().ypoints;

                if (st1.matches("Straight Line")){
                    //For creating a Line
                    store.setLineID(polylineID, roiIndex, shapeIndex);

                    store.setLineX1((double) xcoords[0], roiIndex, shapeIndex);
                    store.setLineX2((double) xcoords[1], roiIndex, shapeIndex);
                    store.setLineY1((double) ycoords[0], roiIndex, shapeIndex);
                    store.setLineY2((double) ycoords[1], roiIndex, shapeIndex);
                    store.setLineLocked(true, roiIndex, shapeIndex);
                }else{
                    //For creating a Point
                    store.setPointX((double) xcoords[0], roiIndex, shapeIndex);
                    store.setPointY((double) ycoords[0], roiIndex, shapeIndex);
                    store.setPointLocked(true, roiIndex, shapeIndex);
                }

            }else if(st1.matches("Polygon") || st1.matches("Angle") || st1.matches("Freehand") || st1.matches("Polyline") || st1.matches("Freeline")){

                String polylineID = MetadataTools.createLSID("Shape", roiIndex,shapeIndex);
                roiID = MetadataTools.createLSID("ROI", roiIndex, shapeIndex);

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
                if (st1.matches("Polyline") || st1.matches("Freeline")){
                    //For creating  a polyline
                    store.setPolylineID(polylineID, roiIndex, shapeIndex);
                    store.setPolylinePoints(points, roiIndex, shapeIndex);
                    store.setPolylineLocked(true, roiIndex, shapeIndex);
                }else{
                    //Create a polygon instead
                    store.setPolygonID(polylineID, roiIndex, shapeIndex);
                    store.setPolygonPoints(points, roiIndex,
                            shapeIndex);       
                    store.setPolygonLocked(true, roiIndex, shapeIndex);
                }
            }else if(st1.matches("Oval") || st1.matches("Rectangle")){

                String polylineID = MetadataTools.createLSID("Shape", roiIndex,shapeIndex);
                roiID = MetadataTools.createLSID("ROI", roiIndex, shapeIndex);

                Rectangle vnRectBounds = roiType.getPolygon().getBounds();
                int x = vnRectBounds.x;
                int y = vnRectBounds.y;
                int rx = vnRectBounds.width;
                int ry = vnRectBounds.height;

                if(st1.matches("Oval")){
                    //For creating a Ellipse
                    store.setEllipseID(polylineID, roiIndex, shapeIndex);
                    store.setEllipseX((double) x, roiIndex, shapeIndex);
                    store.setEllipseY((double) y, roiIndex, shapeIndex);
                    store.setEllipseRadiusX((double) rx, roiIndex, shapeIndex);
                    store.setEllipseRadiusY((double) ry, roiIndex, shapeIndex);
                    store.setEllipseLocked(true, roiIndex, shapeIndex);
                }else{
                    //For creating a Rectangle
                    store.setRectangleWidth((double) rx, roiIndex, shapeIndex);
                    store.setRectangleHeight((double) ry, roiIndex, shapeIndex);
                    store.setRectangleX((double) x, roiIndex, shapeIndex);
                    store.setRectangleY((double) y, roiIndex, shapeIndex);
                    store.setRectangleLocked(true, roiIndex, shapeIndex);
                }

            }
            System.out.print(roiIndex+" "+shapeIndex);
        }
        //Save Roi's using ROIHandler
        if (roiID != null) {
            store.setROIID(roiID, roiIndex);
            store.setImageROIRef(roiID, 0, roiIndex);
        }
        return store;
    }

    public static void openOmeroROIs(IMetadata retrieve, ImagePlus[] images) {
        if (!(retrieve instanceof OMEXMLMetadata)) return;
        int nextRoi = 0;
        RoiManager manager = RoiManager.getInstance();

        OME root = (OME) retrieve.getRoot();

        int imageCount = images.length;
        for (int imageNum=0; imageNum<imageCount; imageNum++) {
            int roiCount = root.sizeOfROIList();
            if (roiCount > 0 && manager == null) {
                manager = new RoiManager();
            }
            for (int roiNum=0; roiNum<roiCount; roiNum++) {
                Union shapeSet = root.getROI(roiNum).getUnion();
                int shapeCount = shapeSet.sizeOfShapeList();

                for (int shape=0; shape<shapeCount; shape++) {
                    Shape shapeObject = shapeSet.getShape(shape);

                    Roi roi = null;

                    if (shapeObject instanceof Ellipse) {
                        Ellipse ellipse = (Ellipse) shapeObject;
                        int cx = ellipse.getX().intValue();
                        int cy = ellipse.getY().intValue();
                        int rx = ellipse.getRadiusX().intValue();
                        int ry = ellipse.getRadiusY().intValue();
                        roi = new OvalRoi(cx - rx, cy - ry, rx * 2, ry * 2);
                    }
                    else if (shapeObject instanceof ome.xml.model.Line) {
                        ome.xml.model.Line line = (ome.xml.model.Line) shapeObject;
                        int x1 = line.getX1().intValue();
                        int x2 = line.getX2().intValue();
                        int y1 = line.getY1().intValue();
                        int y2 = line.getY2().intValue();
                        roi = new Line(x1, y1, x2, y2);
                    }
                    else if (shapeObject instanceof Point) {
                        Point point = (Point) shapeObject;
                        int x = point.getX().intValue();
                        int y = point.getY().intValue();
                        roi = new OvalRoi(x, y, 0, 0);
                    }
                    else if (shapeObject instanceof Polyline) {
                        Polyline polyline = (Polyline) shapeObject;
                        String points = polyline.getPoints();
                        int[][] coordinates = parsePoints(points);
                        roi = new PolygonRoi(coordinates[0], coordinates[1],
                                coordinates[0].length, Roi.POLYLINE);
                    }
                    else if (shapeObject instanceof Polygon) {
                        Polygon polygon = (Polygon) shapeObject;
                        String points = polygon.getPoints();
                        int[][] coordinates = parsePoints(points);
                        roi = new PolygonRoi(coordinates[0], coordinates[1],
                                coordinates[0].length, Roi.POLYGON);
                    }
                    else if (shapeObject instanceof ome.xml.model.Rectangle) {
                        ome.xml.model.Rectangle rectangle =
                                (ome.xml.model.Rectangle) shapeObject;
                        int x = rectangle.getX().intValue();
                        int y = rectangle.getY().intValue();
                        int w = rectangle.getWidth().intValue();
                        int h = rectangle.getHeight().intValue();
                        roi = new Roi(x, y, w, h);

                    }

                    if (roi != null) {
                        Roi.setColor(Color.WHITE);
                        roi.setImage(images[imageNum]);
                        manager.add(images[imageNum], roi, nextRoi++);
                    }
                }
            }
        }
    }
    /**
     * Parse (x, y) coordinates from a String returned by
     * MetadataRetrieve.getPolygonpoints(...) or
     * MetadataRetrieve.getPolylinepoints(...)
     */
    private static int[][] parsePoints(String points) {
        // assuming points are stored like this:
        // x0,y0 x1,y1 x2,y2 ...
        String[] pointList = points.split(" ");
        int[][] coordinates = new int[2][pointList.length];

        for (int q=0; q<pointList.length; q++) {
            pointList[q] = pointList[q].trim();
            int delim = pointList[q].indexOf(",");
            coordinates[0][q] =
                    (int) Double.parseDouble(pointList[q].substring(0, delim));
            coordinates[1][q] =
                    (int) Double.parseDouble(pointList[q].substring(delim + 1));
        }
        return coordinates;
    }
}
