package it.unisa.compressionedati.utils;

import it.unisa.compressionedati.gui.StartFrame;
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

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;


public class UtilityMaskVideo {
    private ScheduledExecutorService timer;
    private VideoCapture capture;
    private boolean cameraActive;
    private CascadeClassifier faceCascade;
    private int absoluteFaceSize;
    private VideoWriter writer;
    private String outfile;
    private FileWriter coordsRoiFile;
    private File dataFrameIn;
    private Scanner in;
    private String maskPath;
    private final Semaphore semaforo;
    private String video_in;
    private String fileName;


    public  UtilityMaskVideo(String _in_video, String _out_video, String mask, Semaphore semaphore, String classifierType,String fileName) throws IOException {
        FileUtils.cleanDirectory(new File(_out_video));
        this.faceCascade = new CascadeClassifier();
        if(classifierType.equalsIgnoreCase("Haar Classifier"))
            this.faceCascade.load("resources/haarcascades/haarcascade_frontalface_alt.xml");
        else
            if(classifierType.equalsIgnoreCase("LBP Classifier"))
                this.faceCascade.load("resources/lbpcascades/lbpcascade_frontalface.xml");
        this.capture = new VideoCapture(_in_video);

        this.absoluteFaceSize = 0;
        this.writer= new VideoWriter();
        this.outfile = _out_video;
        this.maskPath= mask;
        this.coordsRoiFile = new FileWriter(_out_video+"/dataFrame.txt");
        this.semaforo=semaphore;
        this.video_in=_in_video;
      this.fileName= fileName;
    }

    public  UtilityMaskVideo(String _in_video, String _out_video, String pathDataFrame, Semaphore semaphore) throws IOException {
        this.capture = new VideoCapture(_in_video);
        this.writer= new VideoWriter();
        this.outfile = _out_video;
        dataFrameIn = new File(pathDataFrame+"/dataFrame.txt");
        in = new Scanner(dataFrameIn);
        this.semaforo=semaphore;
    }


    public void startMasking()
    {


        Size frameSize = new Size((int) this.capture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) this.capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        int fps = (int) this.capture.get(Videoio.CAP_PROP_FPS);

        this.writer.open(outfile+File.separator+fileName+".avi", VideoWriter.fourcc('x', '2','6','4'),fps, frameSize, true);


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
                writer.write(frame);
            }
        };

        this.timer = Executors.newSingleThreadScheduledExecutor();
        this.timer.scheduleAtFixedRate(frameGrabber, 0, fps, TimeUnit.MILLISECONDS);


    }

    public void startUnmasking()
    {
        Size frameSize = new Size((int) this.capture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) this.capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        int fps = (int) this.capture.get(Videoio.CAP_PROP_FPS);
        this.writer.open(outfile+File.separator+"decompressed.avi", VideoWriter.fourcc('x', '2','6','4'),fps, frameSize, true);


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
                    if(isMasking)
                        this.detectAndDisplayAndMask(frame);
                    else
                        this.detectAndDisplayAndUnmask(frame);
                }else{
                    this.extractAudioTrack(video_in,outfile+File.separator+fileName+"_trackAudio.mp3");
                    stopAcquisition();
                    //System.exit(0);
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

    private void detectAndDisplayAndMask(Mat frame) throws IOException {
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
        String out="";
        int i = 1;
        for (Rect rect : faces.toArray()) {
            String coords = "";
            Rect roi = new Rect(rect.x, rect.y, rect.width, rect.height);
            Mat matrixImgROI = matrixImgInCopy.submat(roi);

            // Formato della stringa con le informaizoni di ogni ROI: ID,X,Y
            // usiamo il carattere '-' per dividere le informazioni di ogni ROI
            coords += i + "," + rect.x + "," + rect.y + "-";
            out+= "coord= "+coords+" ";
            Imgcodecs.imwrite("/home/dangerous/Scrivania/CD/SecureMasking/data/imgs/out/roi/" + i + ".jpg", matrixImgROI);
            MatOfByte mob=new MatOfByte();
            Imgcodecs.imencode(".jpg", matrixImgROI, mob);
            byte ba[]=mob.toArray();
            String value = Base64.getEncoder().encodeToString(ba);
            out+="value= "+value+" ";
            File f=new File(mask);
            Mat matrixMask = Imgcodecs.imread(mask);
            Mat matrixMaskResized = new Mat();
            Imgproc.resize(matrixMask, matrixMaskResized, new Size(rect.width, rect.height));
            Mat matrixImgSecure = matrixImgIn.submat(new Rect(rect.x, rect.y, matrixMaskResized.cols(), matrixMaskResized.rows()));
            matrixMaskResized.copyTo(matrixImgSecure);

            i++;

        }

        if(out!= "")
            coordsRoiFile.append(out+"\n"); //foreach frame
        else
            coordsRoiFile.append("/"+"\n");

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
            LOGGER.info(extractAudio);
        } catch (Exception e) {
            //LOGGER.err("", e);
        }


    }


    private void detectAndDisplayAndUnmask(Mat frame) throws IOException {
        //for each frame
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

    private void checkboxSelection(String classifierPath)
    {
        // load the classifier(s)
        this.faceCascade.load(classifierPath);

        // now the video capture can start
        //this.cameraButton.setDisable(false);
    }
    private void stopAcquisition()
    {
        if (this.timer!=null && !this.timer.isShutdown())
        {
            try
            {
                // stop the timer

                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
                this.writer.release();
                if(in != null)
                    this.in.close();
                if(coordsRoiFile != null)
                    this.coordsRoiFile.close();
                this.zipFile(this.outfile+"/dataFrame.txt",this.outfile+"/dataFrame.zip");
                this.capture.release();
                this.execJavaScript(outfile+File.separator+fileName+"_trackAudio.mp3",outfile+File.separator+fileName+".avi",outfile+File.separator+fileName+"_secure.mp4");
                File intermedieVideo = new File(outfile+File.separator+fileName+".avi");
                File audioTrack = new File(outfile+File.separator+fileName+"_trackAudio.mp3");
                intermedieVideo.delete();
                audioTrack.delete();
            }
            catch (InterruptedException | IOException e)
            {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }



        if (this.capture.isOpened())
        {
            // release the camera
            this.capture.release();
        }
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


    public boolean execJavaScript( String audio_input, String video_input, String video_out) throws IOException, InterruptedException
    {

        String args="";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows"))
            args="cmd /C start ";
        args = "ffmpeg -i "+video_input+" -i "+audio_input+" -c:v copy -c:a aac "+video_out;

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
    protected void setClosed() throws IOException {
        this.writer.release();
        this.coordsRoiFile.close();
        this.capture.release();
        this.stopAcquisition();
    }


}
