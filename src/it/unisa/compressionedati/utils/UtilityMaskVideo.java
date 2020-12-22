package it.unisa.compressionedati.utils;

import com.pngencoder.PngEncoder;
import it.unisa.compressionedati.gui.StartFrame;
import it.unisa.compressionedati.gui.WaitingPanelFrame;
import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Array;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

//import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;


public class UtilityMaskVideo {
    private ScheduledExecutorService timer;
    private VideoCapture capture;
    private boolean cameraActive;
    private CascadeClassifier faceCascade;
    private CascadeClassifier eyeClassifier;
    private int absoluteFaceSize;
    private int absoluteEyeSize;
    private VideoWriter writer;
    private VideoWriter writerApp;
    private String outfile;
    private FileWriter coordsRoiFile;
    private File dataFrameIn;
    private Scanner in;
    private String maskPath;
    private final Semaphore semaforo;
    private String video_in;
    private String classifierType;
    private String fileName;
    private WaitingPanelFrame waitingPanel;
    private int currFrame,totalFrame;
    private String out;
    private ArrayList<byte[]> listFrame;
    private int temp=0;
    private String Encrypt,password;

    public  UtilityMaskVideo(String _in_video, String _out_video, String mask, Semaphore semaphore, String classifierType, String fileName, WaitingPanelFrame waitingFrame, String password,String encryptionType) throws IOException {
        this.classifierType=classifierType;
        FileUtils.cleanDirectory(new File(_out_video));
        this.faceCascade = new CascadeClassifier();
        if(classifierType.equalsIgnoreCase("Haar Frontal Face"))
            this.faceCascade.load("resources/haarcascades/haarcascade_frontalface_alt.xml");
        else
            if(classifierType.equalsIgnoreCase("Haar Eye")) {
                this.faceCascade.load("resources/haarcascades/haarcascade_frontalface_alt.xml");
                this.eyeClassifier= new CascadeClassifier();
                this.eyeClassifier.load("resources/haarcascades/haarcascade_eye.xml");
            }
        this.capture = new VideoCapture(_in_video);
        this.Encrypt = encryptionType;
        this.password= password;
        this.absoluteFaceSize = 0;
        this.absoluteEyeSize = 0;
        this.currFrame=0;
        this.waitingPanel= waitingFrame;
        this.writer= new VideoWriter();
        this.outfile = _out_video;
        this.maskPath= mask;
        this.coordsRoiFile = new FileWriter(_out_video+"/dataFrame.txt");
        this.semaforo=semaphore;
        this.video_in=_in_video;
        this.fileName= fileName;
        this.listFrame = new ArrayList<>();
        this.out="";
    }

    public  UtilityMaskVideo(String _in_video, String _out_video, String pathDataFrame, Semaphore semaphore, String fileName, WaitingPanelFrame waitingFrame) throws IOException {
        this.capture = new VideoCapture(_in_video);
        this.writer= new VideoWriter();
        this.outfile = _out_video;
        this.waitingPanel= waitingFrame;
        this.unzipFile(pathDataFrame+"/dataFrame.zip",pathDataFrame+"/dataFrame.txt");
        dataFrameIn = new File(pathDataFrame+"/dataFrame.txt");
        in = new Scanner(dataFrameIn);
        this.semaforo=semaphore;
        this.video_in=_in_video;
        this.fileName= fileName;
    }


    public void startMasking()
    {

        this.waitingPanel.getParent().setVisible(false);
        this.waitingPanel.setVisible(true);
        this.waitingPanel.writeOnConsole(("Inizializzazione Parametri Masking\n"));
        this.waitingPanel.writeOnConsole(("Tipo di Masking: "+classifierType+"\n"));
        Size frameSize = new Size((int) this.capture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) this.capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        this.totalFrame= (int) this.capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        this.waitingPanel.writeOnConsole(("Numero di Frame: "+this.totalFrame+"\n"));
        int fps = (int) this.capture.get(Videoio.CAP_PROP_FPS);

        //this.writer.open(outfile+File.separator+fileName+".avi", VideoWriter.fourcc('x', '2','6','4'),fps, frameSize, true);
        this.writer.open(outfile+File.separator+fileName+".avi", VideoWriter.fourcc('H', 'F','Y','U'),fps, frameSize, true);


        // grab a frame every 33 ms (30 frames/sec)
        Runnable frameGrabber = new Runnable() {

            @Override
            public void run()
            {
                // effectively grab and process a single frame
                Mat frame = grabFrame(true);
                // convert and show the frame
                //Image imageToShow = Utils.mat2Image(frame);
                //updateImageView(originalFrame, imageToShow);
                //writer.write(frame); //COMMENTATO
            }
        };

        this.timer = Executors.newSingleThreadScheduledExecutor();
        this.timer.scheduleAtFixedRate(frameGrabber, 0, fps, TimeUnit.MILLISECONDS);


    }

    public void startUnmasking()
    {
        Size frameSize = new Size((int) this.capture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) this.capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        int fps = (int) this.capture.get(Videoio.CAP_PROP_FPS);
        this.writer.open(outfile+File.separator+fileName+"_decompressed.avi", VideoWriter.fourcc('x', '2','6','4'),fps, frameSize, true);


        // grab a frame every 33 ms (30 frames/sec)
        Runnable frameGrabber = new Runnable() {

            @Override
            public void run()
            {
                // effectively grab and process a single frame
                Mat frame = grabFrame(false);
                // convert and show the frame
                //Image imageToShow = Utils.mat2Image(frame);
                //updateImageView(originalFrame, imageToShow);

                writer.write(frame);
            }
        };

        this.timer = Executors.newSingleThreadScheduledExecutor();
        this.timer.scheduleAtFixedRate(frameGrabber, 0, fps, TimeUnit.MILLISECONDS);
    }

    /**
     * isMasking = true if grabFrame is called by mask operation, false if is called by unmask operation
     * @param isMasking
     * @return
     */
    private Mat grabFrame(boolean isMasking)
    {
        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened())
        {
            try
            {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty())
                {
                    // face detection
                    if(isMasking && this.classifierType.equalsIgnoreCase("Haar Frontal Face")){
                        this.detectAndDisplayAndMaskFace(frame);
                        }
                    else
                    if(isMasking && this.classifierType.equalsIgnoreCase("Haar Eye"))
                        this.detectAndDisplayAndMaskEye(frame);
                    else
                        if(!isMasking)
                            this.detectAndDisplayAndUnmask(frame);
                }else{
                    this.waitingPanel.writeOnConsole(("Estrazione Traccia Audio"+"\n"));

                    this.extractAudioTrack(video_in,outfile+File.separator+fileName+"_trackAudio.mp3");
                    stopAcquisition(isMasking);
                    System.out.println("FINE");
                    semaforo.release();
                }

            }
            catch (Exception e)
            {
                // log the (full) error
                System.err.println("Exception during the image elaboration: " + e);
                e.printStackTrace();
            }
        }

        return frame;
    }

    private void detectAndDisplayAndMaskFace(Mat frame) throws IOException {

        this.waitingPanel.writeOnConsole(("Rilevamento tratti e Masking frame "+currFrame+"/"+this.totalFrame+"\n"));

        MatOfRect faces = new MatOfRect();
        Mat grayFrame = new Mat();

        // convert the frame in gray scale
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        // equalize the frame histogram to improve the result
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // compute minimum face size (20% of the frame height, in our case)
        if (this.absoluteFaceSize == 0)
        {
            int height = grayFrame.rows();
            if (Math.round(height * 0.2f) > 0)
            {
                this.absoluteFaceSize = Math.round(height * 0.2f);
            }
        }

        // detect faces
        this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

        // each rectangle in faces is a face: draw them!
        Rect[] facesArray = faces.toArray();
        Mat matrixImgIn = frame;
        // Copia dell'immagine di partenza per sottrarre le ROI in modo tale che non vengano sovrascritte dopo aver applicato la maschera
        Mat matrixImgInCopy = frame;
        String mask = maskPath;


        String out1="";
        int i = 1;
        for (Rect rect : faces.toArray()) {
            String coords = "";
            Rect roi = new Rect(rect.x, rect.y, rect.width, rect.height);
            Mat matrixImgROI = matrixImgInCopy.submat(roi);


            // Formato della stringa con le informaizoni di ogni ROI: ID,X,Y
            // usiamo il carattere '-' per dividere le informazioni di ogni ROI
            coords = i + "," + rect.x + "," + rect.y + "-";
            out1+= "coord= "+coords+" ";
            //Imgcodecs.imwrite("/Users/raffaeledragone/Sviluppo/UnisaWs/CompressioneDati/SecureMasking/data/imgs/out/roi/" + i + ".jpg", matrixImgROI);
            Imgcodecs.imwrite("/home/alfonso/UNISA/CompressioneDati/SecureMaskingGit/data/imgs/out/roi/" + i + ".jpg", matrixImgROI);

            MatOfByte mob=new MatOfByte();
            Imgcodecs.imencode(".jpg", matrixImgROI, mob);
            byte ba[]=mob.toArray();
            byte[] ba2 = UtilCompression.compressImageInJpeg(ba, 0.50f);
            String value = Base64.getEncoder().encodeToString(ba2);

            //String value = Base64.getEncoder().encodeToString(ba);
            out1+="value= "+value+" ";
            File f=new File(mask);
            Mat matrixMask = Imgcodecs.imread(mask);
            Mat matrixMaskResized = new Mat();
            Imgproc.resize(matrixMask, matrixMaskResized, new Size(rect.width, rect.height));
            Mat matrixImgSecure = matrixImgIn.submat(new Rect(rect.x, rect.y, matrixMaskResized.cols(), matrixMaskResized.rows()));
            matrixMaskResized.copyTo(matrixImgSecure);

            i++;

        }

        MatOfByte mob=new MatOfByte();
        Imgcodecs.imencode(".png", frame, mob);
        byte ba[]=mob.toArray();
        listFrame.add(ba);



        if(out1!= "") {
            coordsRoiFile.append(out1 + "\n"); //foreach frame
            out+=out1+"\n";
        }
        else{
            coordsRoiFile.append("/\n"); //foreach frame
            out+="/\n";
        }
        ++currFrame;
    }




    private void detectAndDisplayAndMaskEye(Mat frame) throws IOException {

        this.waitingPanel.writeOnConsole(("Rilevamento tratti e Masking frame "+currFrame+"/"+this.totalFrame+"\n"));

        MatOfRect faces = new MatOfRect();
        Mat grayFrame = new Mat();

        // convert the frame in gray scale
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        // equalize the frame histogram to improve the result
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // compute minimum face size (20% of the frame height, in our case)
        if (this.absoluteFaceSize == 0)
        {
           int height = grayFrame.rows();
            if (Math.round(height * 0.1f) > 0)
            {
                this.absoluteFaceSize = Math.round(height * 0.1f);
            }
        }

        // detect faces
        this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

        // each rectangle in faces is a face: draw them!
        Rect[] facesArray = faces.toArray();
        Mat matrixImgIn = frame;
        // Copia dell'immagine di partenza per sottrarre le ROI in modo tale che non vengano sovrascritte dopo aver applicato la maschera
        Mat matrixImgInCopy = frame;
        String mask = maskPath;

        int i = 1;
        for (Rect rect : faces.toArray()) {
            String coords = "";
            Rect roi = new Rect(rect.x, rect.y, rect.width, rect.height);
            Mat matrixImgROI = matrixImgInCopy.submat(roi);
            MatOfRect eyes = new MatOfRect();
            Mat grayFrameEye = new Mat();
            Imgproc.cvtColor(matrixImgROI, grayFrameEye, Imgproc.COLOR_BGR2GRAY);
            // equalize the frame histogram to improve the result
            Imgproc.equalizeHist(grayFrameEye, grayFrameEye);
            String out1="";
            this.eyeClassifier.detectMultiScale(grayFrameEye,eyes,1.1,5);
            for(Rect eye: eyes.toArray()){
                File f=new File(mask);
                Mat matrixMask = Imgcodecs.imread(mask);
                Mat matrixMaskResized = new Mat();
                Imgproc.resize(matrixMask, matrixMaskResized, new Size(eye.width, eye.height));
                Mat matrixImgSecure = matrixImgROI.submat(new Rect(eye.x, eye.y, matrixMaskResized.cols(), matrixMaskResized.rows()));
                matrixMaskResized.copyTo(matrixImgSecure);
                coords =  eye.x + "," + eye.y + "-";
                out1+= "coord= "+coords+" ";



                MatOfByte mob=new MatOfByte();
                Imgcodecs.imencode(".jpg", matrixImgROI, mob);
                byte ba[]=mob.toArray();

                String value = Base64.getEncoder().encodeToString(ba);
                out1+="value= "+value+" ";
            }



            if(out1!= "") {
                coordsRoiFile.append(out1 + "\n"); //foreach frame
                out+=out1+"\n";
            }
            else{
                coordsRoiFile.append("/\n"); //foreach frame
                out+="/\n";
            }


        }
        MatOfByte mob=new MatOfByte();
        Imgcodecs.imencode(".png", frame, mob);
        byte ba[]=mob.toArray();
        listFrame.add(ba);

        ++currFrame;

    }



    private void extractAudioTrack(String _input, String _output){

        String videoPath = _input;
        //audio path
        String extractAudio=_output;
        try{
            //check the audio file exist or not ,remove it if exist
            File extractAudioFile = new File(extractAudio);
            if (extractAudioFile.exists()) {
                extractAudioFile.delete();
            }
            //audio recorder，extractAudio:audio path，2:channels
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(extractAudio, 2);
            recorder.setAudioOption("crf", "0");
            recorder.setAudioQuality(0);
            //bit rate
            recorder.setAudioBitrate(192000);
            //sample rate
            recorder.setSampleRate(44100);
            recorder.setAudioChannels(2);
            //encoder
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
            //start
            recorder.start();
            //load video
            FFmpegFrameGrabber grabber = FFmpegFrameGrabber.createDefault(videoPath);

            grabber.start();
            Frame f=null;
            //get audio sample and record it
            while ((f = grabber.grabSamples()) != null) {
                recorder.record(f);
            }
            // stop to save
            grabber.stop();
            recorder.release();
            //output audio path
            //LOGGER.info(extractAudio);
        } catch (Exception e) {
            //LOGGER.err("", e);
        }


    }


    private void detectAndDisplayAndUnmask(Mat frame) throws IOException {
        //for each frame
        if(temp<30){
            MatOfByte mob=new MatOfByte();
            Imgcodecs.imencode(".png", frame, mob);
            byte ba[]=mob.toArray();
                ByteArrayInputStream bais = new ByteArrayInputStream(ba);
                BufferedImage image = ImageIO.read(bais);
                new PngEncoder()
                        .withBufferedImage(image)
                        .toFile(new File("/Users/raffaeledragone/Sviluppo/UnisaWs/Compressione Dati/LocalStego/outputstegano/"+temp+".png"));
                ++temp;

        }
        ArrayList<Roi> rois = new ArrayList<>();
        Mat matrixImgIn = frame;

        if(in.hasNextLine()){
            String line = in.nextLine();    //leggo una riga corrispondente a un frame
            String[] parts = line.split("\\s+");    //splitto la riga in parti separate da whitespaces
            if(parts[0].equals("/"))
                return;
            for(int i=0; i<parts.length; i++) {
                int x = 0;
                int y = 0;
                String value = null;
                if(parts[i].equals("coord=")){
                    String[] coord = parts[++i].substring(0, parts[i].lastIndexOf("-")).split("[,]");
                    x = Integer.parseInt(coord[1]);
                    y = Integer.parseInt(coord[2]);
                }
                if(parts[++i].equals("value=")){
                    value = parts[++i];
                }
                Roi r = new Roi(x, y, value);
                rois.add(r);
                //System.out.println("x: " + x + ", y: " + y + ", value : " + value);
            }
            for(Roi r : rois){
                String value = r.getValue();
                //byte[] ba = null;
                //Base64.getDecoder().decode(ba);
                byte[] ba = Base64.getDecoder().decode(value);
                Mat mat = Imgcodecs.imdecode(new MatOfByte(ba),Imgcodecs.IMREAD_UNCHANGED); //immagine della roi
                Rect roi = new Rect(r.getX(), r.getY(), mat.cols(), mat.rows());    //coordinate della roi

                Mat matrixImg = matrixImgIn.submat(new Rect(roi.x, roi.y, roi.width, roi.height));
                mat.copyTo(matrixImg);
            }
        }
    }

    private void stopAcquisition(boolean isMasking)
    {
        if (this.timer!=null && !this.timer.isShutdown())
        {
            try
            {
                // stop the timer

                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);

                this.waitingPanel.writeOnConsole(("Start: Stenografia "+"\n"));


                char[] password = this.password.toCharArray();
                UtilStepanography steno= new UtilStepanography();
                steno.setCompression(true);
                steno.setEncryption(true);
                steno.setEncryptionMode(Encrypt);
                System.out.println("Password => "+password);
                int i=0;
                ArrayList<byte[]> list =steno.hide(listFrame,out,password);

                this.waitingPanel.writeOnConsole(("END: Stenografia "+"\n"+"Frame Utilizzati per stenografare: "+list.size()));


                for(i=0; i<list.size();++i){

                    this.waitingPanel.writeOnConsole(("Scrittura frame stenografato: "+i+"/"+list.size()));

                    Mat mat = Imgcodecs.imdecode(new MatOfByte(list.get(i)),Imgcodecs.IMREAD_UNCHANGED);
                    writer.write(mat);

                }
                System.out.println("Lista => "+list.size());
                for(; i<listFrame.size();++i){
                    this.waitingPanel.writeOnConsole(("Scrittura frame standard: "+i+"/"+totalFrame));


                    Mat mat = Imgcodecs.imdecode(new MatOfByte(listFrame.get(i)),Imgcodecs.IMREAD_UNCHANGED);
                    writer.write(mat);

                }

                this.writer.release();

                this.waitingPanel.writeOnConsole(("Inserimento traccia audio"));



                this.execFFmpegConvertVideoToMp4Lossless(outfile+File.separator+fileName+".avi",outfile+File.separator+fileName+"_secure.mp4");
                File f = new File(outfile+File.separator+fileName+".avi");
                f.delete();
                this.execFFmpegAddAudioTrack(outfile+File.separator+fileName+"_trackAudio.mp3",outfile+File.separator+fileName+"_secure.mp4",outfile+File.separator+fileName+"_secure_withaudio.mp4");
                f=new File(outfile+File.separator+fileName+"_secure.mp4");
                f.delete();
                this.execFFmpegWriteMetadataOnVideo(outfile+File.separator+fileName+"_secure_withaudio.mp4","album","nframe="+list.size()+"");
                f=new File(outfile+File.separator+fileName+"_secure_withaudio.mp4");
                f.delete();
                if(in != null)
                    this.in.close();
                if(coordsRoiFile != null)
                    this.coordsRoiFile.close();
                this.zipFile(this.outfile+"/dataFrame.txt",this.outfile+"/dataFrame.zip");
                this.capture.release();
                if(isMasking){
                    this.waitingPanel.writeOnConsole(("Ricostruzione video con traccia Audio"));

                    //this.execJavaScript(outfile+File.separator+fileName+"_trackAudio.mp3",outfile+File.separator+fileName+".avi",outfile+File.separator+fileName+"_secure.avi");
                    this.waitingPanel.writeOnConsole(("Eliminazione file intermedi"+"\n"));

                    File intermedieVideo = new File(outfile+File.separator+fileName+".avi");
                    File audioTrack = new File(outfile+File.separator+fileName+"_trackAudio.mp3");
                    intermedieVideo.delete();
                    audioTrack.delete();

                    this.waitingPanel.getParent().setVisible(true);
                    waitingPanel.dispatchEvent(new WindowEvent(waitingPanel, WindowEvent.WINDOW_CLOSING));
                }
                else{
                    this.execFFmpegWriteMetadataOnVideo(outfile+File.separator+fileName+"_trackAudio.mp3",outfile+File.separator+fileName+"_decompressed.avi",outfile+File.separator+fileName+"_decompressed.mp4");
                    File intermedieVideo = new File(outfile+File.separator+fileName+"_decompressed.avi");
                    File audioTrack = new File(outfile+File.separator+fileName+"_trackAudio.mp3");
                    intermedieVideo.delete();
                    audioTrack.delete();
                }
            }
            catch (InterruptedException | IOException e)
            {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            } catch (ShortBufferException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }


        }



        if (this.capture.isOpened())
        {
            // release the camera
            this.capture.release();
        }
    }

    private String execFFmpegWriteMetadataOnVideo(String path_video, String name_metadata, String metadata_value) throws IOException {
        String args="";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows"))
            args="cmd /C start ";
        //ffmpeg -i a_secure.avi_trackAudio.mp3 -c copy -metadata customMeta="22" a_secure.avi_trackAudio.mp3
        String name_video = path_video.replaceAll(".mp4","");
        name_video+="_mt.mp4";
        args += "ffmpeg -i "+path_video+" -metadata "+name_metadata+"="+metadata_value+" -codec copy "+name_video+"";
        //-metadata album="nframe=22" -codec copy output.avi
        System.out.print(args);
        String dir =System.getProperty("user.dir")+"/resources/ffmpeg/";
        Process p = Runtime.getRuntime().exec(args,null, new File(dir));

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

        return name_video;
    }

    private void unzipFile(String source, String target) throws IOException {
        String fileZip = source;
        File destDir = new File(target);
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = new File(target);
            // write file content
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    private void zipFile(String source, String target) throws IOException {
        String sourceFile = source;
        FileOutputStream fos = new FileOutputStream(target);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(sourceFile);
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        zipOut.close();
        fis.close();
        fos.close();

    }


    public boolean execFFmpegConvertVideoToMp4Lossless(String video_input, String video_out) throws IOException, InterruptedException
    {

        String args="";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows"))
            args="cmd /C start ";
        args += "ffmpeg -i "+video_input+" -vcodec png "+video_out;

        System.out.print(args);
        String dir =System.getProperty("user.dir")+"/resources/ffmpeg/";
        Process p = Runtime.getRuntime().exec(args,null, new File(dir));

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

    public boolean execFFmpegAddAudioTrack( String audio_input, String video_input, String video_out) throws IOException, InterruptedException
    {

        String args="";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows"))
            args="cmd /C start ";
        args += "ffmpeg -i "+video_input+" -i "+audio_input+" -c:v copy -c:a aac "+video_out;

        System.out.print(args);
        String dir =System.getProperty("user.dir")+"/resources/ffmpeg/";
        Process p = Runtime.getRuntime().exec(args,null, new File(dir));

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


}
