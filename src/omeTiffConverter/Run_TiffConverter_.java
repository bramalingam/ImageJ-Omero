package omeTiffConverter;


import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

import ome.xml.model.enums.EnumerationException;
import omero.model.Shape;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import loci.plugins.LociExporter;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.PlugIn;



public class Run_TiffConverter_ implements PlugIn {

    public void run(String arg) {

        readfile reader2 = new readfile();
        Roi[] rois = reader2.readFromRoiManager();

        //        if (rois.length==0){
        //            System.out.println("No Roi's drawn");
        //        }else {
        //            try {
        //                Shape c =  reader2.convertToOmeroRoi(rois[0]);
        //            } catch (Exception e) {
        //                // TODO Auto-generated catch block
        //                e.printStackTrace();
        //            }
        //        }
        //        System.out.println("Done");

//        
        for(int i=0; i< rois.length; i++){
            System.out.println(rois[i].getTypeAsString());
        }

        try {
            reader2.ExporterWithImageJRoi();
        } catch (DependencyException | ServiceException | FormatException
                | IOException | EnumerationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 



    }

}
