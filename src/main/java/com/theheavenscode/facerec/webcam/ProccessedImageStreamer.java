package com.theheavenscode.facerec.webcam;

import java.io.BufferedOutputStream;
import java.io.BufferedReader; 
import java.io.ByteArrayOutputStream;
import java.io.Closeable; 
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
import com.theheavenscode.facerec.impl.ProccessedImageStreamImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 

class ProccessedImageStreamer implements ThreadFactory {

    private static FaceRecognitionAndTracking cameraImageSubscriber;

    private long last = -1;

    private long delay = -1;

    private int number = 0;

    public int port = 9000;

    private AtomicBoolean started = new AtomicBoolean(false);

    private ExecutorService executor = Executors.newCachedThreadPool(this);

    private int fps;

    private static final Logger LOG = LoggerFactory.getLogger(ProccessedImageStreamer.class);

    private static final String BOUNDARY = "mjpegframe";

    private static final String CRLF = "\r\n";

    private class Acceptor implements Runnable {

        @Override
        public void run() {
            try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))) {
                System.out.println("OpenCV Mat streamer listening.........");
                while (started.get()) {
                    System.out.println("OpenCV Mat streamer trying to connect....!!!!");
                    executor.execute(new Connection(server.accept()));
                }
            } catch (Exception e) {
                LOG.error("Cannot accept socket connection", e);
            }
        }
    }

    private class Connection implements Runnable {

        private Socket socket = null;

        public Connection(Socket socket) {
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

                while (true) {
                    System.out.println("OpenCV mat loop listening.....");

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
                            ProccessedImageStreamImpl.baos.writeTo(baos); 
                        } 

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

                LOG.error("Error ProccessedImageStreamer : ", e);

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

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, String.format("proccessed_streamer-thread-%s", number++));
        // thread.setUncaughtExceptionHandler(WebcamExceptionHandler.getInstance());
        thread.setDaemon(true);
        return thread;
    }

    public ProccessedImageStreamer(FaceRecognitionAndTracking cameraImageSubscriber) {

        ProccessedImageStreamer.cameraImageSubscriber = cameraImageSubscriber;
        this.fps = 24;
        this.delay = (long) (1000 / fps);
        // ProccessedImageStreamer.matFile = new File("mat.jpg");
        start();

    }

    public void start() {
        if (started.compareAndSet(false, true)) {
            System.out.println("Proccessed image streamer started");

            executor.execute(new Acceptor());

        }
    }

}