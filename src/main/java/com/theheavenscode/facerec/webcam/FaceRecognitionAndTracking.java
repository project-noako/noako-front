package com.theheavenscode.facerec.webcam;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.ipcam.IpCamDeviceRegistry;
import com.github.sarxos.webcam.ds.ipcam.IpCamDriver;
import com.github.sarxos.webcam.ds.ipcam.IpCamMode;
import com.theheavenscode.facerec.config.SerialConnectionClass;
import com.theheavenscode.facerec.impl.ImageSubscriber;
import com.theheavenscode.facerec.impl.ProccessedImageStreamImpl;
import com.theheavenscode.facerec.modal.FaceDetails;
import com.theheavenscode.facerec.modal.SerialCommPort;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture; 
import org.springframework.context.annotation.Configuration;

import static org.bytedeco.opencv.global.opencv_core.*;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Configuration
class FaceRecognitionAndTracking/*<SerialConnectionClass>*/ {

    static VideoCapture capture;

    static FaceRecognizer faceRecognizer;

    private static FaceRecognitionAndTracking context;

    static CascadeClassifier face_cascade;
    static {
        Webcam.setDriver(new IpCamDriver());
    }

    public FaceRecognitionAndTracking() {
        // try {
        // IpCamDeviceRegistry.register("Lignano", "http://192.168.8.125:5000 ",
        // IpCamMode.PUSH);

        // } catch (Exception e) {

        // }

        File[] originalImageFiles;

        MatVector images;

        Mat labels;

        File originals;

        File preProccessed;

        File[] imageFiles;

        String streamPath = "http://192.168.8.125:5000";

        capture = new VideoCapture(0);
        // capture.open( streamPath);
        // capture.open(arg0)
        // Webcam webcam= Webcam.getWebcams().get(0);

        new WebCamFrameGrabber(capture).start();
        // new WebCamFrameGrabber(capture).start();

        originals = new File("resources/originals/");
        if (!originals.exists()) {
            originals.mkdir();
        }
        preProccessed = new File("resources/preProccessed/");
        if (!preProccessed.exists()) {
            preProccessed.mkdir();
        }

        System.out.println("Camera Image Subscriber initialised..............!!!!!");
        context = this;

        String haarcascade = "haarcascades/haarcascade_frontalface_default.xml";
        String lbpcascadeImproved = "lbpcascades/lbpcascade_frontalface_improved.xml";
        String lbpcascade = "lbpcascades/lbpcascade_frontalface_improved.xml";

        try {

            face_cascade = new CascadeClassifier(new File("resources/" + lbpcascadeImproved).getAbsolutePath());

            // testPath = new
            // File(CameraImageSubscriber.class.getClassLoader().getResource("enrique.jpg").toURI())
            // .getAbsolutePath();
            FilenameFilter imgFilter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    name = name.toLowerCase();
                    return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
                }
            };

            originalImageFiles = originals.listFiles(imgFilter);

            for (File image : originalImageFiles) {

                try {
                    RectVector faces = new RectVector();
                    Mat dface = org.bytedeco.opencv.global.opencv_imgcodecs.imread(image.getAbsolutePath());
                    face_cascade.detectMultiScale(dface, faces);

                    for (int i = 0; i < faces.size(); i++) {
                        Rect face_i = faces.get(i);
                        Mat face = new Mat(org.bytedeco.opencv.global.opencv_imgcodecs.imread(image.getAbsolutePath(),
                                IMREAD_COLOR), face_i);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try {

                            byte[] b = new byte[face.channels() * face.cols() * face.rows()];

                            BytePointer buf = new BytePointer();
                            org.bytedeco.opencv.global.opencv_imgcodecs.imencode(".jpg", face, b);

                            InputStream in = new ByteArrayInputStream(b);

                            ImageIO.write(ImageIO.read(in), "JPG",
                                    new File(preProccessed.getAbsolutePath() + "/" + image.getName()));

                        } catch (IOException e) {

                            e.printStackTrace();

                            System.err.println("error : " + e.getMessage());

                        }

                    }

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            File root = preProccessed;

            imageFiles = root.listFiles(imgFilter);

            images = new MatVector(imageFiles.length);

            labels = new Mat(imageFiles.length, 1, CV_32SC1);
            IntBuffer labelsBuf = labels.createBuffer();

            int counter = 0;

            for (File image : imageFiles) {

                int label = Integer.parseInt(image.getName().split("\\-")[0]);

                System.out.println("Reading : " + image.getAbsolutePath() + " id : " + label);

                Mat img = imread(image.getAbsolutePath(), IMREAD_GRAYSCALE);

                equalizeHist(img, img);

                images.put(counter, img);

                labelsBuf.put(counter, label);

                counter++;
            }

            System.out.println("total training images : " + counter);
            // CameraImageSubscriber.faceRecognizer = LBPHFaceRecognizer .create(4, 8, 8, 8,
            // Double.MAX_VALUE );
            FaceRecognitionAndTracking.faceRecognizer = LBPHFaceRecognizer.create(2, 8, 8, 8, Double.MAX_VALUE);

            // FaceRecognizer faceRecognizer = FisherFaceRecognizer.create();
            // FaceRecognizer faceRecognizer = EigenFaceRecognizer.create();
            // FaceRecognizer faceRecognizer = LBPHFaceRecognizer.create();

            FaceRecognitionAndTracking.faceRecognizer.train(images, labels);
            FaceRecognitionAndTracking.faceRecognizer.save("trained_data.xml");
            FaceRecognitionAndTracking.faceRecognizer.read("trained_data.xml");

            // ÷÷÷÷÷÷÷÷÷÷÷÷÷÷

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        new WebCamStreamer(context);
        new ProccessedImageStreamer(context);
        imageSubscriber("thread one 1");
        // imageSubscriber(" thread two 2");
        // imageSubscriber("thread one 3");
        // imageSubscriber(" thread two 4");

        // Thread openCVStream = new Thread(new Runnable() {

        // @Override
        // public void run() {
        // while (true) {

        // BufferedImage image;
        // try {
        // image = ImageIO.read(
        // new
        // ByteArrayInputStream(ProccessedImageStreamImpl.webCamStreamBaos.toByteArray()));

        // imageSubscriber(image);

        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        // }

        // }
        // });
        // openCVStream.setDaemon(true);
        // openCVStream.start();

    }

    public void  imageSubscriber(String name) {

        Thread faceRecognitionThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {

                        Mat convertMat = new Mat(ProccessedImageStreamImpl.webCamStreamBaos.toByteArray());

                        Mat mat = org.bytedeco.opencv.global.opencv_imgcodecs.imdecode(convertMat, IMREAD_GRAYSCALE);

                        Mat matColor = org.bytedeco.opencv.global.opencv_imgcodecs.imdecode(convertMat, IMREAD_COLOR);

                        // IntPointer label = new IntPointer(1);
                        // DoublePointer confidence = new DoublePointer(1);
                        // equalizeHist(mat, mat);

                        startTracking(mat, matColor, Thread.currentThread().getName());

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        System.err.println(e.getMessage());
                    }
                }
            }
        });
        // faceRecognitionThread.setDaemon(true);
        faceRecognitionThread.setName(name);

        faceRecognitionThread.start();

    }

    void startTracking(Mat mat, Mat matColor, String name) throws URISyntaxException {

        RectVector faces = new RectVector();

        face_cascade.detectMultiScale(mat, faces);
        Mat face;
        for (int i = 0; i < faces.size(); i++) {
            Rect face_i = faces.get(i);

            face = new Mat(mat, face_i);

            // Mat face_resized = new Mat(mat, face_i);
            // resize(face, face_resized, new Size(3000, 3000), 1.0, 1.0, INTER_CUBIC);

            IntPointer label = new IntPointer(1);
            DoublePointer confidence = new DoublePointer(1);

            FaceRecognitionAndTracking.faceRecognizer.predict(face, label, confidence);

            int prediction = label.get(0);
            rectangle(matColor, face_i, new Scalar(0, 255, 0, 1));
            
         int midX=  face_i.tl().x()+ (face_i.width()/2) ;
         int midY=  face_i.tl().y()+ (face_i.height() /2);

            String box_text = "P 1 = " + prediction + " C 1 = " + (int) confidence.get(0) + " "
                    + System.currentTimeMillis();
                  
            int pos_x = Math.max(face_i.tl().x() - 10, 0);
            int pos_y = Math.max(face_i.tl().y() - 10, 0);
            
            putText(matColor, "X", new Point(midX, midY), FONT_HERSHEY_PLAIN, 1.0, new Scalar(0, 0, 255, 3.0));

            System.out.println("\n\nFace ( " + name + " ) : \n" + box_text);
            // SerialConnectionClass serialConnectionClass= new SerialConnectionClass();
            // try {
            //     FaceDetails faceDetails = new FaceDetails("name", String.valueOf(  midX ), String.valueOf(midY), String.valueOf(face_i.width()));
            //     SerialCommPort.getSerialPort().writeString(faceDetails.toString());
            //     SerialCommPort.getSerialPort().writeString( "\r\n");
            //     System.out.println(faceDetails.toString());
            

            // } catch (Exception e) {
            //     // TODO: handle exception
            // }
        }
        ImageConverterUtils.getProccessedMat(matColor);

    }

}