package it.unisa.compressionedati.utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class UtilityMaskVideo {
    private ScheduledExecutorService timer;
    private VideoCapture capture;
    private boolean cameraActive;
    private CascadeClassifier faceCascade;
    private int absoluteFaceSize;
    private VideoWriter writer;
    private String outfile;
    private FileWriter coordsRoiFile;
    private String maskPath;
    private final Semaphore semaforo;

    public  UtilityMaskVideo(String _in_video, String _out_video, String mask, Semaphore semaphore, String classifierType) throws IOException {
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

    }


    public void startMasking()
    {
        Size frameSize = new Size((int) this.capture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) this.capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        int fps = (int) this.capture.get(Videoio.CAP_PROP_FPS);
        this.writer.open(outfile+File.separator+"test.avi", VideoWriter.fourcc('x', '2','6','4'),fps, frameSize, true);


        // grab a frame every 33 ms (30 frames/sec)
        Runnable frameGrabber = new Runnable() {

            @Override
            public void run()
            {
                // effectively grab and process a single frame
                Mat frame = grabFrame();
                // convert and show the frame
                //Image imageToShow = Utils.mat2Image(frame);
                //updateImageView(originalFrame, imageToShow);
                writer.write(frame);
            }
        };

        this.timer = Executors.newSingleThreadScheduledExecutor();
        this.timer.scheduleAtFixedRate(frameGrabber, 0, fps, TimeUnit.MILLISECONDS);


    }

    private Mat grabFrame()
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

                    this.detectAndDisplayAndMask(frame);

                }else{

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
        int i = 1;
        String coords = "";
        Mat matrixImgIn = frame;
        // Copia dell'immagine di partenza per sottrarre le ROI in modo tale che non vengano sovrascritte dopo aver applicato la maschera
        Mat matrixImgInCopy = frame;
        String mask = maskPath;
        String out="";
        for (Rect rect : faces.toArray()) {

            Rect roi = new Rect(rect.x, rect.y, rect.width, rect.height);
            Mat matrixImgROI = matrixImgInCopy.submat(roi);


            // Formato della stringa con le informaizoni di ogni ROI: ID,X,Y
            // usiamo il carattere '-' per dividere le informazioni di ogni ROI
            coords += i + "," + rect.x + "," + rect.y + "-";
            out+= "coord= "+coords+" ";
            Imgcodecs.imwrite("/home/alfonso/Scaricati/face-detection/face-detection/data/imgs/out/roi/" + i + ".jpg", matrixImgROI);
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
            coordsRoiFile.append(out+"\n");
        else
            coordsRoiFile.append("/"+"\n");

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
                this.coordsRoiFile.close();
                this.capture.release();
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
    protected void setClosed() throws IOException {
        this.writer.release();
        this.coordsRoiFile.close();
        this.capture.release();
        this.stopAcquisition();
    }


}
