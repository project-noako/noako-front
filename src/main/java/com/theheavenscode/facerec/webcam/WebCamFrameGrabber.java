package com.theheavenscode.facerec.webcam;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;


class WebCamFrameGrabber extends Thread {

    private static VideoCapture capture;

    public WebCamFrameGrabber(VideoCapture capture) {
        this.capture = capture;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String streamPath = "http://192.168.8.125:5000/";
                // Mat matrix = new Mat(uriToByteArray(streamPath));
                Mat matrix = new Mat();
                // matrix.

                // Mat convertMat = new
                // Mat(ProccessedImageStreamImpl.webCamStreamBaos.toByteArray());

                WebCamFrameGrabber.capture.read(matrix);

                ImageConverterUtils.getProccessedMatToBuffered(matrix);
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
    }

    public byte[] uriToByteArray(String uri) throws IOException {

        String streamPath = "http://192.168.8.125:5000/";
        URL url = new URL(streamPath);
        BufferedImage c = ImageIO.read(url);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(c, "jpg", baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();

        /*
         * URL url = new URL(uri); ByteArrayOutputStream output = new
         * ByteArrayOutputStream();
         * 
         * try (InputStream inputStream = url.openStream()) { int n = 0; byte[] buffer =
         * new byte[1024*1024*1024];
         * 
         * while (-1 != (n = inputStream.read(buffer))) { output.write(buffer, 0, n); }
         * }
         */

        return imageInByte;
    }
}