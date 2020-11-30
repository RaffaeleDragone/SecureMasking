package it.unisa.compressionedati.test;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import javax.swing.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

public class Test {

    private final String PATH_DATA = "C:\\Users\\maico\\Desktop\\Progetto_CD\\secure-jpeg-masking\\data";
    private final String PATH_SCRIPT = "C:\\Users\\maico\\Desktop\\Progetto_CD\\secure-jpeg-masking\\script";
    private final String PATH_FILE="C:\\Users\\maico\\Desktop\\Progetto_CD\\secure-jpeg-masking\\data\\file\\secret.txt";

    private ArrayList<String> types;
    private ArrayList<String> masks;
    private ArrayList<String> ciphers;
    private ArrayList<String> modes;

    private ArrayList<String> comTimes;
    private ArrayList<String> decTimes;
    private ArrayList<String> initDims;
    private ArrayList<String> secDims;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public Test() {

        deleteResults();

        String password = "password";

        types = new ArrayList<>();
        masks = new ArrayList<>();
        ciphers = new ArrayList<>();
        modes = new ArrayList<>();

        populateLists();

        for (String type : types) {
            for (String mask : masks) {
                for (String cipher : ciphers) {
                    for (String mode : modes) {
                        try {
                            test(type, mask, cipher, mode, password);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        showDialog("Test successfully completed.");

    }

    public ArrayList<String> compress(String imgIn, String type, String mask, String cipher, String mode, String password) throws IOException, InterruptedException {
        ArrayList<String> results = new ArrayList<>();

        long startTime = System.nanoTime();

        String coords = "";

        //Pulisco le cartelle di output
        cleanDirectory(PATH_DATA + "\\imgs\\out\\processed");
        cleanDirectory(PATH_DATA + "\\imgs\\out\\roi");
        cleanDirectory(PATH_DATA + "\\imgs\\out\\secure");

        Mat matrixImgIn = Imgcodecs.imread(imgIn);
        // Copia dell'immagine di partenza per sottrarre le ROI in modo tale che non vengano sovrascritte dopo aver applicato la maschera
        Mat matrixImgInCopy = Imgcodecs.imread(imgIn);

        // Tento prima il riconoscimento dei volti
        CascadeClassifier classifier = new CascadeClassifier(type);
        MatOfRect detections = new MatOfRect();
        classifier.detectMultiScale(matrixImgIn, detections);

        //System.out.println(String.format("Detected %s ROI", detections.toArray().length));

        int i = 1;
        for (Rect rect : detections.toArray()) {

            Rect roi = new Rect(rect.x, rect.y, rect.width, rect.height);
            Mat matrixImgROI = matrixImgInCopy.submat(roi);

            /*
            // Decommentalo per disegnare anche il rettangolo intorno le ROI
            Imgproc.rectangle(
                    matrixImgIn,
                    new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(0, 255, 0),
                    1);
            */

            // Formato della stringa con le informaizoni di ogni ROI: ID,X,Y
            // usiamo il carattere '-' per dividere le informazioni di ogni ROI
            coords += i + "," + rect.x + "," + rect.y + "-";

            Imgcodecs.imwrite(PATH_DATA + "\\imgs\\out\\roi\\" + i + ".jpg", matrixImgROI);
            Mat matrixMask = Imgcodecs.imread(mask);
            Mat matrixMaskResized = new Mat();
            Imgproc.resize(matrixMask, matrixMaskResized, new Size(rect.width, rect.height));
            Mat matrixImgSecure = matrixImgIn.submat(new Rect(rect.x, rect.y, matrixMaskResized.cols(), matrixMaskResized.rows()));
            matrixMaskResized.copyTo(matrixImgSecure);

            // Parametri per la gestione della qualità della compressione JPEG
            ArrayList<Integer> list = new ArrayList();
            list.add(Imgcodecs.IMWRITE_JPEG_QUALITY);
            list.add(85);
            MatOfInt params = new MatOfInt();
            params.fromList(list);

            //modificata estensione in png da jpg
            Imgcodecs.imwrite(PATH_DATA + "/imgs/out/secure/secure.png", matrixImgIn, params);
            i++;
        }

        String script = PATH_SCRIPT + "\\compression\\" + cipher + "\\compression" + "-" + mode + ".py";

        //modificata estensione da jpg a png
        String msgRtn = execPythonScript1(script, PATH_DATA + "\\imgs\\out\\secure\\secure.png", coords, password);
        //execPythonScript(script, PATH_DATA + "/imgs/out/secure/secure.jpg", coords, password);

        //Inserimento dati all'interno dell'immagine
        execJavaScript1();

        long estimatedTime = System.nanoTime() - startTime;
        float seconds = estimatedTime/1000000000F;
        DecimalFormat df = new DecimalFormat("#.###");

        // ArrayList di risultati
        // Alla posizione 0 salvo il numero delle detection
        // Alla posizione 1 salvo il tempo impiegato per la compressione
        // Alla posizione 2 salvo la dimensione iniziale del file
        // Alla posizione 3 salvo la dimensione del file sicuro
        results.add(String.valueOf(detections.toArray().length));
        results.add(String.valueOf(df.format(seconds)));
        results.add(String.valueOf(getFileDimension(imgIn)));
        results.add(String.valueOf(getFileDimension(PATH_DATA + "\\imgs\\out\\secure\\secure.png")));

        return results;
    }

    /*
     * Metodo per criptare le informazioni all'interno dell'immagine.
     */
    public boolean execJavaScript1( ) throws IOException, InterruptedException
    {
        String coverfile="C:\\Users\\maico\\Desktop\\Progetto_CD\\secure-jpeg-masking\\data\\imgs\\out\\secure\\secure.png";
        String stegofile="C:\\Users\\maico\\Desktop\\Progetto_CD\\secure-jpeg-masking\\data\\imgs\\out\\secure\\test.png";
        String path="C:\\Users\\maico\\Desktop\\Progetto_CD\\secure-jpeg-masking\\libs\\openstego.jar";

        //String args = path+" --embed --messagefile="+PATH_FILE+" --coverfile="+coverfile+" --stegofile="+stegofile;

        //String args = "cmd /C start java -jar "+path+" --embed --messagefile="+PATH_FILE+" --coverfile="+coverfile+" --stegofile="+stegofile;
        String args = "cmd /C start java -jar "+path+" --embed --messagefile="+PATH_FILE+" --coverfile="+coverfile+" --stegofile="+stegofile;

        System.out.print(args);

        Process p = Runtime.getRuntime().exec(args);
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(p.getErrorStream()));

    //Read the output from the command
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

    //Read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }
        return true;
    }
    public String execPythonScript1(String script, String file, String coords, String passw) throws IOException {
        String[] args = new String[] { "C:\\Users\\maico\\anaconda3\\envs\\new\\python", script, file, coords, passw };
        Process process = new ProcessBuilder(args).start();

        //System.out.println(script+"\n"+file+"\n"+coords+"\n"+passw);

        // Leggo il ritorno dello script in python
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
      /*  try {
			process.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
        String s=decodeReturn1(in.readLine());
        //System.out.println(s);
        return s;
    }
    public String decodeReturn1(String ret) {
        String ret_decoded = "";
        switch (ret) {
            case "0":
                ret_decoded =  "Metadata set correctly.";
                break;
            case "-1":
                ret_decoded = "IPTC metadata not set correctly.";
                break;
            case "-2":
                ret_decoded = "Exiv metadata not set correctly.";
                break;
            case "-3":
                ret_decoded = "IPTC and Exiv metadata not set correctly.";
                break;
            case "error":
                ret_decoded = "Metadata not set correctly.";
                break;

        }
        return ret_decoded;
    }

    public String decompress(String cipher, String mode, String password) throws IOException, InterruptedException {

        long startTime = System.nanoTime();

        // Pulisco la cartella di output
        // cleanDirectory(PATH_DATA + "/imgs/out/decompressed");

        execJavaScript2();

        Thread.sleep(4000);

        String script = PATH_SCRIPT + "\\decompression\\" + cipher + "\\decompression" + "-" + mode + ".py";
        String msgRtn = execPythonScript2(script, PATH_DATA + "\\imgs\\out\\secure\\secure.png", cipher, mode, password);

        long estimatedTime = System.nanoTime() - startTime;
        float seconds = estimatedTime/1000000000F;
        DecimalFormat df = new DecimalFormat("#.###");

        return String.valueOf(df.format(seconds));
    }

    //Modifica aggiunta decriptazione delle info dalla foto
    /*
     * Metodo per criptare le informazioni all'interno dell'immagine.
     */
    public boolean execJavaScript2( ) throws IOException, InterruptedException
    {
        String extractdir="C:\\Users\\maico\\Desktop\\Progetto_CD\\secure-jpeg-masking\\data\\file";
        String stegofile="C:\\Users\\maico\\Desktop\\Progetto_CD\\secure-jpeg-masking\\data\\imgs\\out\\secure\\test.png";
        String path="C:\\Users\\maico\\Desktop\\Progetto_CD\\secure-jpeg-masking\\libs\\openstego.jar";

        //String args = path+" --embed --messagefile="+PATH_FILE+" --coverfile="+coverfile+" --stegofile="+stegofile;

        String args = "cmd /C start java -jar "+path+" extract --stegofile="+stegofile+" --extractdir="+extractdir;

        System.out.print(args);

        Runtime.getRuntime().exec(args);

        return true;
    }
    public String execPythonScript2(String script, String file, String cipher, String mode, String passw) throws IOException {
        String[] args = new String[] { "C:\\Users\\maico\\anaconda3\\envs\\new\\python", script, file, cipher, mode, passw };

        //System.out.println(script+"\n"+file+"\n"+cipher+"\n"+mode+"\n"+passw);

        Process process = new ProcessBuilder(args).start();

        // Leggo il ritorno dello script in python
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

        return decodeReturn2(in.readLine());
    }
    public String decodeReturn2(String ret) {
        String ret_decoded = "";
        switch (ret) {
            case "0":
                ret_decoded =  "Decompression done correctly.";
                break;
            case "-1":
                ret_decoded = "Wrong password.";
                break;
            case "-2":
                ret_decoded = "Wrong cipher.";
                break;
            case "-3":
                ret_decoded = "Wrong cipher's mode.";
                break;
            case "-4":
                ret_decoded = "Wrongs cipher and cipher's mode.";
                break;
        }
        return ret_decoded;
    }
    /*
    public void execPythonScript(String script, String file, String coords, String passw) throws IOException {
        String[] args = new String[] { "C:\\Users\\maico\\anaconda3\\envs\\new\\python", script, file, coords, passw };
        Process process = new ProcessBuilder(args).start();
    }
    public void execPythonScript(String script, String file, String cipher, String mode, String passw) throws IOException {
        String[] args = new String[] { "C:\\Users\\maico\\anaconda3\\envs\\new\\python", script, file, cipher, mode, passw };
        Process process = new ProcessBuilder(args).start();;
    }
    */

    public void cleanDirectory(String directory) {
        File folder = new File(directory);
        File[] files = folder.listFiles();

        if(files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if(files[i] != null) {
                        files[i].delete();
                    }
                }
            }
        }
    }

    public void showDialog(String msg) {
        JOptionPane.showMessageDialog(
                null,
                msg,
                "Test dialog",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    public void test(String type, String mask, String cipher, String mode, String password) throws IOException {
        File folder = new File(PATH_DATA + "\\imgs\\in");
        File[] files = folder.listFiles();

        comTimes = new ArrayList<>();
        decTimes = new ArrayList<>();
        initDims = new ArrayList<>();
        secDims = new ArrayList<>();

        if(files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if (files[i] != null) {

                        String img = files[i].toString();
                        String imgName = img.split("\\\\")[9];

                        ArrayList<String> results = new ArrayList<>();

                        try {
                            results = compress(img, type, mask, cipher, mode, password);
                            results.add(decompress(cipher, mode, password));
                            results.add(String.valueOf(getFileDimension(PATH_DATA + "\\imgs\\out\\decompressed\\decompressed.jpg")));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } catch (InterruptedException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }

                        // La prima immagine è esclusa dalla media
                        if(i > 0) {
                            comTimes.add(results.get(1));
                            decTimes.add(results.get(4));
                            initDims.add(results.get(2));
                            secDims.add(results.get(3));
                        }

                        if(i == 0) {
                            // Istruzione vuota, perchè la prima immagina è scartata
                        } else if(i == 1) {
                            writeResults(imgName, results, type, cipher, mode, true, false);
                        } else if( i == files.length - 1) {
                            writeResults(imgName, results, type, cipher, mode, false, true);
                        } else {
                            writeResults(imgName, results, type, cipher, mode, false, false);
                        }

                    }
                }
            }
        }
    }

    public void writeResults(String img, ArrayList<String> results, String type, String cipher, String mode, boolean first, boolean last) throws IOException {
        File file = new File(PATH_DATA + "\\results\\results.txt");
        if(!file.exists()){
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file, true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw);

        if(first == true) {
            String classifier = type.split("\\\\")[8];
            if (classifier.contains("_") == true) {
                classifier = classifier.split("_")[1].split("\\.")[0];
            } else {
                classifier = classifier.split("\\.")[0];
            }
            out.println("Results on the classifier: " + classifier.toUpperCase() + " for the cipher: " + cipher.toUpperCase() + " on mode: " + mode.toUpperCase() + "\n");
        }

        out.println("img: " + img + " | detection: " + results.get(0) + " | compression time: " + results.get(1) + "s | decompression time: " + results.get(4) + "s | img initial dimension: " + results.get(2) + "kb | img secure dimension: " + results.get(3) + "kb | img decompressed dimensions: " + results.get(5) + "kb.");

        if(last == true) {
            out.println("\naverage compression time: " + getAverage(comTimes) + "s | average decompression time: " + getAverage(decTimes) + "s | average initial dimension: " + getAverage(initDims) + "kb | average secure dimension: " + getAverage(secDims) + "kb.\n\n");
        }

        out.close();
    }

    public void deleteResults() {
        File file = new File(PATH_DATA + "\\results\\results.txt");
        file.delete();
    }

    public void populateLists() {
        if(types != null) {
            types.add(PATH_DATA + "\\classifiers\\haarcascade_frontalface.xml");
            types.add(PATH_DATA + "\\classifiers\\haarcascade_eyes.xml");
        }
        if(masks != null) {
            masks.add(PATH_DATA + "\\imgs\\masks\\frontalface_mask.jpg");
            masks.add(PATH_DATA + "\\imgs\\masks\\eyes_mask.jpg");
        }
        if(ciphers != null) {
            ciphers.add("aes");
            //ciphers.add("des");
        }
        if(modes != null) {
            modes.add("cbc");
            modes.add("cfb");
            modes.add("ecb");
            modes.add("ofb");
        }
    }

    public String getFileDimension(String img) {
        File file = new File(img);
        double dim = 0;

        if(file.exists()) {
            dim = (file.length() / 1024);
        }

        DecimalFormat df = new DecimalFormat("#.###");
        return String.valueOf(df.format(dim));
    }

    public String getAverage(ArrayList<String> list) {
        Collections.sort(list);
        double sum = 0;

        for (String num : list) {
            String numFormatted = num.replace(",", ".");
            sum += Double.valueOf(numFormatted);
        }

        double avg = sum / list.size();

        DecimalFormat df = new DecimalFormat("#.###");
        return String.valueOf(df.format(avg));
    }

}

