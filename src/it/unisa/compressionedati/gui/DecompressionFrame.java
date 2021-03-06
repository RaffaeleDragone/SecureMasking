package it.unisa.compressionedati.gui;

import org.opencv.core.Core;

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

public class DecompressionFrame extends JFrame {

    private final int FRAME_WIDTH = 170;
    private final int FRAME_HEIGHT = 255;
    private final String PATH_DATA = StartFrame.ROOTPATH+File.separator+"data";
    private final String PATH_SCRIPT = StartFrame.ROOTPATH+File.separator+"script"+File.separator+"decompression";
    private final String PATH_FILE= StartFrame.ROOTPATH+File.separator+"data"+File.separator+"file"+File.separator+"secret.txt";


    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public DecompressionFrame(){
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setResizable(false);
        createMainPanel();
    }

    public void createMainPanel(){
        JPanel panel = new JPanel();
        //panel.setLayout(new GridLayout(1, 1, 0, 14));
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JPanel options = createOptionsPanel();
        JPanel indietro = createIndietroPanel();

        panel.add(options);
        panel.add(indietro, BorderLayout.SOUTH);

        add(panel);
    }

    public JPanel createOptionsPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 1, 0, 5));

        ArrayList<String> strs = populateComboFileName();
        // Sort dell'arraylist
        strs.sort(String::compareToIgnoreCase);
        JComboBox combo = new JComboBox(strs.toArray());

        ArrayList<String> ciphers = populateComboCipherName();
        JComboBox comboCiphers = new JComboBox(ciphers.toArray());

        ArrayList<String> modes = populateComboModeName();
        JComboBox comboModes = new JComboBox(modes.toArray());

        TextField text = new TextField();
        text.setText("password");
        text.setBackground(Color.WHITE);
        JButton btn = new JButton("Decompress");

        class clickButton implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                String password = text.getText();
                String cipher = comboCiphers.getSelectedItem().toString().toLowerCase();
                String mode = "none";
                if (!cipher.equals("chacha20")) {
                    mode = comboModes.getSelectedItem().toString().toLowerCase();
                }


                try {
                    if(checkTextLength(cipher, password) == true) {
                        decompress(cipher, mode, password);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }

        ActionListener listener = new clickButton();
        btn.addActionListener(listener);

        panel.add(combo);
        panel.add(comboCiphers);
        panel.add(comboModes);
        panel.add(text);
        panel.add(btn);
        panel.setBorder(new TitledBorder(new EtchedBorder(), "Options"));

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
                DecompressionFrame.this.dispose();
            }
        }

        ActionListener listener = new clickButton();
        btn.addActionListener(listener);

        panel.add(btn);

        return panel;
    }

    public ArrayList<String> populateComboFileName() {
        File folder = new File(PATH_DATA + File.separator+"imgs"+File.separator+"out"+File.separator+"secure");
        File[] files = folder.listFiles();
        ArrayList<String> names = new ArrayList<>();

        if(files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if(files[i] != null) {
                        names.add(files[i].getName());
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
                showDialog("DES password must be at most eight characters long.");
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
                "Decompression dialog",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    public void decompress(String cipher, String mode, String password) throws IOException, InterruptedException {
        System.out.println("Start decompression");

        execJavaScript();

        Thread.sleep(4000);

        long startTime = System.nanoTime();


        String script = PATH_SCRIPT + "/" + cipher + "/decompression.py";
        //if (!mode.equals("none")) {
        //    script = PATH_SCRIPT + "/" + cipher + "/decompression" + "-" + mode + ".py";
        //}

        String msgRtn = execPythonScript(script, PATH_DATA + File.separator+"imgs"+File.separator+"out"+File.separator+"secure"+File.separator+"secure.png", cipher, mode,password);
        long estimatedTime = System.nanoTime() - startTime;
        float seconds = estimatedTime/1000000000F;
        DecimalFormat df = new DecimalFormat("#.###");

        showDialog(msgRtn + "\nDecompression done in " + df.format(seconds) + " seconds.");
        System.out.println("\n\nEnd decompression");
        //cleanDirectory(StartFrame.ROOTPATH+File.separator+"data"+File.separator+"file");

    }


    //Modifica aggiunta decriptazione delle info dalla foto
    /*
     * Metodo per criptare le informazioni all'interno dell'immagine.
     */
    public boolean execJavaScript( ) throws IOException, InterruptedException
    {


        String stegofile= StartFrame.ROOTPATH+File.separator+"data"+File.separator+"imgs"+File.separator+"out"+File.separator+"secure"+File.separator+"test.png";
        String extractdir= StartFrame.ROOTPATH+File.separator+"data"+File.separator+"file";

        //String args = path+" --embed --messagefile="+PATH_FILE+" --coverfile="+coverfile+" --stegofile="+stegofile;
        String args="";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows"))
            args="cmd /C start ";
        args = "/usr/lib/jvm/java-11-openjdk-amd64/bin/java -jar "+ StartFrame.OPENSTEGOPATH+" extract --stegofile="+stegofile+" --extractdir="+extractdir;

        System.out.print(args);

        Runtime.getRuntime().exec(args);

        return true;

    }


    public String execPythonScript(String script, String file, String cipher, String mode, String passw) throws IOException {
        //String[] args = new String[] { "/Users/raffaeledragone/opt/anaconda3/envs/new/bin/python", script, file, cipher, mode, passw, StartFrame.ROOTPATH };
        String[] args = new String[] { "/home/dangerous/anaconda3/envs/new/bin/python", script, file, cipher, mode, passw, StartFrame.ROOTPATH };

        //System.out.println(script+"\n"+file+"\n"+cipher+"\n"+mode+"\n"+passw);

        Process process = new ProcessBuilder(args).start();

        // Leggo il ritorno dello script in python
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

        return decodeReturn(in.readLine());
    }

    public String decodeReturn(String ret) {
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
}
