package com.theheavenscode.facerec.config;
 

import com.theheavenscode.facerec.modal.SerialCommPort;

import org.springframework.context.annotation.Configuration;

import jssc.*;
 
//s@Configuration
public class SerialConnectionClass {

    static SerialPort serialPort;

    
    public SerialConnectionClass() {

        SerialConnectionClass.serialPort = new SerialPort(SerialPortList.getPortNames()[0]);
        try {
            SerialConnectionClass.serialPort.openPort();

            SerialConnectionClass.  serialPort.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

                    SerialConnectionClass. serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);

            // serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
 
            SerialCommPort.setSerialPort(SerialConnectionClass.serialPort);

        } catch (SerialPortException ex) {
            System.out.println("There are an error on writing string to port т: " + ex);
        }
    }



  
 

}