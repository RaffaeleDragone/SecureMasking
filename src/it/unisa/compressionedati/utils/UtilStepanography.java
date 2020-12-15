package it.unisa.compressionedati.utils;

import it.unisa.compressionedati.gui.StartFrame;

import java.io.File;
import java.io.IOException;

public class UtilStepanography {



    /*
     * Metodo per criptare le informazioni all'interno dell'immagine.
     */
    public static boolean execSteganography( String path_file) throws IOException, InterruptedException
    {
        String coverfile= StartFrame.ROOTPATH+ File.separator+"data"+File.separator+"video"+File.separator+"out"+File.separator+"firstFrame.png";

        String stegofile= StartFrame.ROOTPATH+File.separator+"data"+File.separator+"video"+File.separator+"out"+File.separator+"test.png";

        String args="";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows"))
            args="cmd /C start ";
        args += "java -jar "+ StartFrame.OPENSTEGOPATH+" --embed --messagefile="+path_file+" --coverfile="+coverfile+" --stegofile="+stegofile;

        System.out.print(args);

        Process p = Runtime.getRuntime().exec(args);

        return true;

    }

}
