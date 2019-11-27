package com.theheavenscode.facerec.webcam;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.theheavenscode.facerec.impl.ProccessedImageStreamImpl;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

final class ImageConverterUtils {

    public ImageConverterUtils() {
    }

    public static void getProccessedMatToBuffered(final Mat mat) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {

            byte[] b = new byte[mat.channels() * mat.cols() * mat.rows()];

            org.bytedeco.opencv.global.opencv_imgcodecs.imencode(".jpg", mat, b);

            InputStream in = new ByteArrayInputStream(b);

            ImageIO.write(ImageIO.read(in), "JPG", baos);
            ProccessedImageStreamImpl.webCamStreamBaos = baos;

        } catch (IOException e) {

            e.printStackTrace();

            System.err.println("error : " + e.getMessage());

        }

    }

    public static void getProccessedMat(final Mat mat) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {

            byte[] b = new byte[mat.channels() * mat.cols() * mat.rows()];

            org.bytedeco.opencv.global.opencv_imgcodecs.imencode(".jpg", mat, b);

            InputStream in = new ByteArrayInputStream(b);

            ImageIO.write(ImageIO.read(in), "JPG", baos);
            ProccessedImageStreamImpl.baos = baos;

        } catch (IOException e) {

            e.printStackTrace();

            System.err.println("error : " + e.getMessage());

        }

    }

    public byte[] getProccessedImage(final Mat mat) {
        byte[] return_buff = new byte[(int) (mat.total() * mat.channels())];

        return return_buff;
    }
    public static Mat toMat(final BufferedImage bi) {
        OpenCVFrameConverter.ToIplImage cv = new OpenCVFrameConverter.ToIplImage();
        Java2DFrameConverter jcv = new Java2DFrameConverter();
        return cv.convertToMat(jcv.convert(bi));
    }

    public static Mat toMat(final byte[] bs) {
        return toMat(toBufferedImage(bs));
    }

    public static BufferedImage toBufferedImage(final byte[] bs) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bs));
        } catch (IOException ex) {
         }
        return null;
    }


    public static void resizeImg(String inputImagePath, String outputImagePath, double percent) throws IOException {
        File inputFile = new File(inputImagePath);
        BufferedImage inputImage = ImageIO.read(inputFile);
        int scaledWidth = (int) (inputImage.getWidth() * percent);
        int scaledHeight = (int) (inputImage.getHeight() * percent);
        ImageConverterUtils.resizeImg(inputImagePath, outputImagePath, scaledWidth, scaledHeight);
    }

    public static void resizeImg(String inputImagePath, String outputImagePath, int scaledWidth, int scaledHeight)
            throws IOException {

        File inputFile = new File(inputImagePath);
        BufferedImage inputImage = ImageIO.read(inputFile);

        BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType());

        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        String formatName = outputImagePath.substring(outputImagePath.lastIndexOf(".") + 1);

        ImageIO.write(outputImage, formatName, new File(outputImagePath));
    }


}
