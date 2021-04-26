package it.unisa.compressionedati.main;


import it.unisa.compressionedati.utils.UtilCompression;
import org.json.simple.parser.ParseException;

import java.io.IOException;


public class TestJsonFile {

    public static void main(String[] args) throws IOException, ParseException {

        String input = "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDABALDA4MChAODQ4SERATGCgaGBYWGDEjJR0oOjM9PDkzODdASFxOQERXRTc4UG1RV19iZ2hnPk1xeXBkeFxlZ2P/2wBDARESEhgVGC8aGi9jQjhCY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2P/wAARCABNAE0DASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/";
        String name_file = "testjson.json";
        String key = UtilCompression.write_content(input,name_file);
        String output = UtilCompression.read_content(name_file,key);
        System.out.println(output);

    }

}
