package com.theheavenscode.facerec.webcam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import com.theheavenscode.facerec.impl.ProccessedImageStreamImpl;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

// import com.github.sarxos.webcam.Webcam;
// import com.github.sarxos.webcam.WebcamEvent;
// import com.github.sarxos.webcam.WebcamExceptionHandler;
// import com.github.sarxos.webcam.WebcamResolution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

class WebCamStreamer implements ThreadFactory {

    private double fps = 0;

    private int number = 0;

    private int port = 8000;

    private long last = -1;

    private long delay = -1;

    // private BufferedImage image = null;

    private ExecutorService executor = Executors.newCachedThreadPool(this);

    private AtomicBoolean started = new AtomicBoolean(false);

    private FaceRecognitionAndTracking cameraImageSubscriber; 

    private static final Logger LOG = LoggerFactory.getLogger(WebCamStreamer.class);

    private static final String BOUNDARY = "mjpegframe";

    private static final String CRLF = "\r\n";

    private class Acceptor implements Runnable {

        private FaceRecognitionAndTracking cameraImageSubscriber;

        public  Acceptor(FaceRecognitionAndTracking cameraImageSubscriber) {
            this.cameraImageSubscriber = cameraImageSubscriber;
        }

        @Override
        public void run() {
            try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))) {
                while (started.get()) {
                    executor.execute(new Connection(server.accept(), cameraImageSubscriber));
                }
            } catch (Exception e) {
                LOG.error("Cannot accept socket connection", e);
            }
        }
    }

    private class Connection implements Runnable {

        private Socket socket = null;
        private FaceRecognitionAndTracking cameraImageSubscriber;

        public Connection(Socket socket, FaceRecognitionAndTracking cameraImageSubscriber) {
            this.cameraImageSubscriber = cameraImageSubscriber;
            this.socket = socket;
        }

        @Override
        public void run() {

            LOG.info("New connection from {}", socket.getRemoteSocketAddress());

            final BufferedReader br;
            final BufferedOutputStream bos;
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try {
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                bos = new BufferedOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                LOG.error("Fatal I/O exception when creating socket streams", e);
                try {
                    socket.close();
                } catch (IOException e1) {
                    LOG.error("Canot close socket connection from " + socket.getRemoteSocketAddress(), e1);
                }
                return;
            }

            // consume whole input

            try {
                while (br.ready()) {
                    br.readLine();
                }
            } catch (IOException e) {
                LOG.error("Error when reading input", e);
                return;
            }

            // stream

            try {

                socket.setSoTimeout(0);
                socket.setKeepAlive(false);
                socket.setTcpNoDelay(true);

                while (started.get()) { 

                    StringBuilder sb = new StringBuilder();
                    sb.append("HTTP/1.0 200 OK").append(CRLF);
                    sb.append("Connection: close").append(CRLF);
                    sb.append("Cache-Control: no-cache").append(CRLF);
                    sb.append("Cache-Control: private").append(CRLF);
                    sb.append("Pragma: no-cache").append(CRLF);
                    sb.append("Content-type: multipart/x-mixed-replace; boundary=--").append(BOUNDARY).append(CRLF);
                    sb.append(CRLF);

                    bos.write(sb.toString().getBytes());

                    do { 
                        baos.reset();

                        long now = System.currentTimeMillis();
                        if (now > last + delay) {

                            // image = ImageIO.read(
                            //         new ByteArrayInputStream(ProccessedImageStreamImpl.webCamStreamBaos.toByteArray()));

                            ImageIO.write(ImageIO.read(
                                new ByteArrayInputStream(ProccessedImageStreamImpl.webCamStreamBaos.toByteArray())), "JPG", baos);

                            // this.cameraImageSubscriber.imageSubscriber(image);
                        }

                        // ImageIO.write(image, "JPG", baos);

                        sb.delete(0, sb.length());
                        sb.append("--").append(BOUNDARY).append(CRLF);
                        sb.append("Content-type: image/jpeg").append(CRLF);
                        sb.append("Content-Length: ").append(baos.size()).append(CRLF);
                        sb.append(CRLF);

                        try {
                            bos.write(sb.toString().getBytes());
                            bos.write(baos.toByteArray());
                            bos.write(CRLF.getBytes());
                            bos.flush();
                        } catch (SocketException e) {

                            if (!socket.isConnected()) {
                                LOG.debug("Connection to client has been lost");
                            }
                            if (socket.isClosed()) {
                                LOG.debug("Connection to client is closed");
                            }

                            try {
                                br.close();
                                bos.close();
                            } catch (SocketException se) {
                                LOG.debug("Exception when closing socket", se);
                            }

                            LOG.debug("Socket exception from " + socket.getRemoteSocketAddress(), e);

                            return;
                        }

                        Thread.sleep(delay);

                    } while (started.get());
                }
            } catch (Exception e) {

                String message = e.getMessage();

                if (message != null) {
                    if (message.startsWith("Software caused connection abort")) {
                        LOG.info("User closed stream");
                        return;
                    }
                    if (message.startsWith("Broken pipe")) {
                        LOG.info("User connection broken");
                        return;
                    }
                }

                LOG.error("Error", e);

                try {
                    bos.write("HTTP/1.0 501 Internal Server Error\r\n\r\n\r\n".getBytes());
                } catch (IOException e1) {
                    LOG.error("Not ablte to write to output stream", e);
                }

            } finally {

                LOG.info("Closing connection from {}", socket.getRemoteSocketAddress());

                for (Closeable closeable : new Closeable[] { br, bos, baos }) {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        LOG.debug("Cannot close socket", e);
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    LOG.debug("Cannot close socket", e);
                }
            }
        }
    }

    public WebCamStreamer(FaceRecognitionAndTracking cameraImageSubscriber ) {
 
        // Dimension[] myResolution = new Dimension[] { new Dimension(640, 480) }; // new Dimension(1280, 800)
     
        this.cameraImageSubscriber = cameraImageSubscriber; 
 
        this.fps = 1000;
        this.delay = (long) (1000 / fps);

        start();

    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, String.format("streamer-thread-%s", number++));
        // thread.setUncaughtExceptionHandler(WebcamExceptionHandler.getInstance());
        thread.setDaemon(true);
        return thread;
    }

    public void start() {
        if (started.compareAndSet(false, true)) {

            System.out.println("WebCam image streamer started");
      
            executor.execute(new Acceptor(this.cameraImageSubscriber));
        }
    }

    public void stop() {
        if (started.compareAndSet(true, false)) {
            executor.shutdown(); 
        }
    }

    public double getFPS() {
        return fps;
    }

    public boolean isInitialized() {
        return started.get();
    }

    public int getPort() {
        return port;
    }

     
}