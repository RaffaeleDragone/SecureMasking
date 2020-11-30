package it.unisa.compressionedati.utils;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

public class UtilFiles {
    private static Logger logger= Logger.getLogger(UtilFiles.class);
    public static Properties readPropertyFile(String propFileName){
        Properties prop = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(propFileName);
        } catch (FileNotFoundException ex) {
            logger.warn("ERROR"+ex.getMessage());
            ex.printStackTrace();
        }
        try {
            prop.load(is);
        } catch (IOException ex) {
            logger.warn("ERROR"+ex.getMessage());
            ex.printStackTrace();
        }
        return prop;
    }


}
