package it.unisa.compressionedati.gui;


import it.unisa.compressionedati.utils.UtilityMaskVideo;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class VideoMaskFrame extends JFrame {

    private final int FRAME_WIDTH = 400;
    private final Semaphore semaforo;
    private final int FRAME_HEIGHT = 550;
    private final String PATH_DATA = StartFrame.ROOTPATH+File.separator+"data";
    private final String PATH_SCRIPT = StartFrame.ROOTPATH+File.separator+"script"+File.separator+"compression";
    private final String PATH_FILE= StartFrame.ROOTPATH+File.separator+"data"+File.separator+"file"+File.separator+"secret.txt";

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public VideoMaskFrame(){
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setResizable(false);
        createMainPanel();
        semaforo = new Semaphore(1);
    }

    public void createMainPanel(){
        JPanel panel = new JPanel();
        //panel.setLayout(new GridLayout(1, 1, 0, 14));
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new GridLayout(2,1));
        JPanel compression = createCompressionPanel();
        JPanel decompression = createDecompressionPanel();
        JPanel indietro = createIndietroPanel();

        panel.add(innerPanel);
        innerPanel.add(compression);
        innerPanel.add(decompression);
        panel.add(indietro, BorderLayout.SOUTH);

        add(panel);
    }

    public JPanel createCompressionPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 1, 0, 5));

        ArrayList<String> strs = populateComboFileName("compress");
        // Sort dell'arraylist
        strs.sort(String::compareToIgnoreCase);
        JComboBox combo = new JComboBox(strs.toArray());

        ArrayList<String> mask= populateComboMask();
        mask.sort(String::compareToIgnoreCase);
        JComboBox cmbMask = new JComboBox(mask.toArray());

        ArrayList<String> classifier = new ArrayList<>();
        classifier.add("Haar Classifier");
        classifier.add("LBP Classifier");
        JComboBox cmbClassifier = new JComboBox(classifier.toArray());

        TextField text = new TextField();
        text.setText("password");
        text.setBackground(Color.WHITE);
        JButton btn = new JButton("Compress");

        class clickButton implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                String video = PATH_DATA +File.separator+ "video"+File.separator+"in"+File.separator + combo.getSelectedItem().toString();
                String mask = PATH_DATA + File.separator+"video"+File.separator+"mask"+File.separator+cmbMask.getSelectedItem().toString();
                String PATH_OUT = PATH_DATA+File.separator+"video"+File.separator+"out";
                String classifierType= cmbClassifier.getSelectedItem().toString();
                String fileName= (combo.getSelectedItem().toString()).replace(".mp4","");

                try {
                    semaforo.acquire();
                    UtilityMaskVideo videoMask = new UtilityMaskVideo(video,PATH_OUT,mask, semaforo,classifierType, fileName);
                    videoMask.startMasking();
                } catch (IOException | InterruptedException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

        ActionListener listener = new clickButton();
        btn.addActionListener(listener);

        panel.add(new JLabel("Video Input"));
        panel.add(combo);
        panel.add(new JLabel("Maschera"));
        panel.add(cmbMask);
        panel.add(new JLabel("Tipo di Riconoscimento"));
        panel.add(cmbClassifier);
        panel.add(text);
        panel.add(btn);
        panel.setBorder(new TitledBorder(new EtchedBorder(), "Compression"));

        return panel;
    }

    public JPanel createDecompressionPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1, 0, 5));

        ArrayList<String> strs = populateComboFileName("decompress");
        // Sort dell'arraylist
        strs.sort(String::compareToIgnoreCase);
        JComboBox combo = new JComboBox(strs.toArray());

        TextField text = new TextField();
        text.setText("password");
        text.setBackground(Color.WHITE);
        JButton btn = new JButton("Decompress");

        class clickButton implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                String video = PATH_DATA +File.separator+ "video"+File.separator+"out"+File.separator + combo.getSelectedItem().toString();
                String path_dataFrame = PATH_DATA +File.separator+ "video"+File.separator+"out";
                String PATH_OUT = PATH_DATA+File.separator+"video"+File.separator+"out";
                String fileName= (combo.getSelectedItem().toString()).replace(".mp4","");
                try {
                    semaforo.acquire();
                    UtilityMaskVideo videoMask = new UtilityMaskVideo(video, PATH_OUT, path_dataFrame, semaforo, fileName);
                    videoMask.startUnmasking();
                } catch (IOException | InterruptedException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

        ActionListener listener = new clickButton();
        btn.addActionListener(listener);

        panel.add(new JLabel("Video Input"));
        panel.add(combo);
        panel.add(text);
        panel.add(btn);
        panel.setBorder(new TitledBorder(new EtchedBorder(), "Decompression"));

        return panel;
    }

    public JPanel createIndietroPanel(){
        JPanel panel = new JPanel();

        JButton btn = new JButton("Back");

        class clickButton implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                StartFrame frame = new StartFrame();
                frame.setLocationRelativeTo(null);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                VideoMaskFrame.this.dispose();
            }
        }

        ActionListener listener = new clickButton();
        btn.addActionListener(listener);

        panel.add(btn);

        return panel;
    }

    public ArrayList<String> populateComboFileName(String mode) {
        File folder = null;
        if(mode.equals("compress"))
            folder = new File(PATH_DATA + File.separator+"video"+File.separator+"in");
        if(mode.equals("decompress"))
            folder = new File(PATH_DATA + File.separator+"video"+File.separator+"out");

        File[] files = folder.listFiles();
        ArrayList<String> classifiers = new ArrayList<>();

        if(files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if(files[i] != null) {
                        if(mode.equals("decompress"))
                            classifiers.add(files[i].getName());
                        else if(mode.equals("compress"))
                            classifiers.add(files[i].getName());
                    }
                }
            }
        } else {
            classifiers.add("empty");
        }
        return classifiers;
    }

    public ArrayList<String> populateComboMask() {
        File folder = new File(PATH_DATA + File.separator+"video"+File.separator+"mask");
        File[] files = folder.listFiles();
        ArrayList<String> masks = new ArrayList<>();


        if(files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if(files[i] != null) {
                        masks.add(files[i].getName());
                    }
                }
            }
        } else {
            masks.add("empty");
        }
        return masks;
    }

    public ArrayList<String> populateComboClassifierName() {
        File folder = new File(PATH_DATA + File.separator+"classifiers");
        File[] files = folder.listFiles();
        ArrayList<String> names = new ArrayList<>();

        if(files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if(files[i] != null) {
                        String classifier = files[i].getName().split("_")[1].split("\\.")[0];
                        names.add(classifier);
                    }
                }
            }
        } else {
            names.add("empty");
        }
        return names;
    }

    public ArrayList<String> populateComboCipherName() {
        ArrayList<String> ciphers = new ArrayList<>();
        ciphers.add("AES");
        return ciphers;
    }

    public ArrayList<String> populateComboModeName() {
        ArrayList<String> modes = new ArrayList<>();
        modes.add("CBC");
        modes.add("CFB");
        modes.add("ECB");
        modes.add("OFB");
        return modes;
    }

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

    public boolean checkTextLength(String cipher, String password) {
        if(cipher.equals("des")) {
            if(password.length() > 8) {
                showDialog("DES password must be at most eigth characters long.");
                return false;
            }
            return true;
        }
        return true;
    }

    public void showDialog(String msg) {
        JOptionPane.showMessageDialog(
                null,
                msg,
                "Compression dialog",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    public void compress(String imgIn, String type, String mask, String cipher, String mode, String password) throws IOException, InterruptedException {
        System.out.println("Start compression");

        long startTime = System.nanoTime();

        String coords = "";

        //Pulisco le cartelle di output
        cleanDirectory(PATH_DATA + File.separator+"imgs"+File.separator+"out"+File.separator+"processed");
        cleanDirectory(PATH_DATA + File.separator+"imgs"+File.separator+"out"+File.separator+"roi");
        cleanDirectory(PATH_DATA + File.separator+"imgs"+File.separator+"out"+File.separator+"secure");




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

            Imgcodecs.imwrite(PATH_DATA + File.separator+"imgs"+File.separator+"out"+File.separator+"roi"+File.separator + i + ".jpg", matrixImgROI);
            Mat matrixMask = Imgcodecs.imread(mask);
            Mat matrixMaskResized = new Mat();
            Imgproc.resize(matrixMask, matrixMaskResized, new Size(rect.width, rect.height));
            Mat matrixImgSecure = matrixImgIn.submat(new Rect(rect.x, rect.y, matrixMaskResized.cols(), matrixMaskResized.rows()));
            matrixMaskResized.copyTo(matrixImgSecure);

            // Parametri per la gestione della qualità della compressione JPEG
            ArrayList<Integer> list = new ArrayList<Integer>();
            list.add(Imgcodecs.IMWRITE_JPEG_QUALITY);
            list.add(85);
            MatOfInt params = new MatOfInt();
            params.fromList(list);

            //modificata estensione in png da jpg
            Imgcodecs.imwrite(PATH_DATA + File.separator+"imgs"+File.separator+"out"+File.separator+"secure"+File.separator+"secure.png", matrixImgIn, params);
            i++;
        }

        //Imgcodecs.imwrite(PATH_DATA + "/imgs/out/processed/processed.jpg", matrixImgIn);

        String script = PATH_SCRIPT + File.separator + cipher + File.separator+"compression.py";
        //if (!mode.equals("none")) {
        //  script = PATH_SCRIPT + File.separator + cipher + File.separator +"compression" + "-" + mode + ".py";
        //}
        //modificata estensione da jpg a png
        String msgRtn = execPythonScript(script, PATH_DATA + File.separator+"imgs"+File.separator+"out"+File.separator+"secure"+File.separator+"secure.png", coords, password,mode);

        long estimatedTime = System.nanoTime() - startTime;
        float seconds = estimatedTime/1000000000F;
        DecimalFormat df = new DecimalFormat("#.###");

        /*Inserimento dati all'interno dell'immagine
         *
         */
        execJavaScript( );

        showDialog("Detected " + detections.toArray().length + " ROI.\n" + msgRtn + "\nCompression done in " + df.format(seconds) + " seconds.");
        System.out.println("\n\nEnd compression");
        cleanDirectory(StartFrame.ROOTPATH+File.separator+"data"+File.separator+"file");


    }




    /*
     * Metodo per criptare le informazioni all'interno dell'immagine.
     */
    public boolean execJavaScript( ) throws IOException, InterruptedException
    {
        String coverfile= StartFrame.ROOTPATH+File.separator+"data"+File.separator+"imgs"+File.separator+"out"+File.separator+"secure"+File.separator+"secure.png";

        String stegofile= StartFrame.ROOTPATH+File.separator+"data"+File.separator+"imgs"+File.separator+"out"+File.separator+"secure"+File.separator+"test.png";


        //String args = path+" --embed --messagefile="+PATH_FILE+" --coverfile="+coverfile+" --stegofile="+stegofile;

        //String args = "cmd /C start java -jar "+path+" --embed --messagefile="+PATH_FILE+" --coverfile="+coverfile+" --stegofile="+stegofile;
        String args="";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows"))
            args="cmd /C start ";
        args = "java -jar "+ StartFrame.OPENSTEGOPATH+" --embed --messagefile="+PATH_FILE+" --coverfile="+coverfile+" --stegofile="+stegofile;

        System.out.print(args);

        Process p = Runtime.getRuntime().exec(args);
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(p.getErrorStream()));

// Read the output from the command
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

// Read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }

        return true;

    }




    public String execPythonScript(String script, String file, String coords, String passw, String mode) throws IOException {
        //String[] args = new String[] { "/Users/raffaeledragone/opt/anaconda3/envs/new/bin/python", script, file, coords, passw, mode , StartFrame.ROOTPATH };
        String[] args = new String[] { "/home/dangerous/anaconda3/envs/new/bin/python", script, file, coords, passw, mode , StartFrame.ROOTPATH };
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
        String s=decodeReturn(in.readLine());
        //System.out.println(s);
        return s;
    }

    public String decodeReturn(String ret) {
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
}
