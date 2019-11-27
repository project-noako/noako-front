package com.theheavenscode.facerec.modal;

import jssc.SerialPort;

public class SerialCommPort {
    static SerialPort serialPort;

    public static SerialPort getSerialPort() {
        return SerialCommPort.serialPort;
    }

    public static void setSerialPort(SerialPort serialPortArg){
        SerialCommPort.serialPort= serialPortArg;
    }
}