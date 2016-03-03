package ru.ifmo.ctddev.salynskii.HelloUDP;

import java.io.IOException;
import java.net.*;

/**
 * This class is simple realization of the {@link Runnable} interface using for creating
 * some threads to handle the incoming messages.
 * @author Alimantu
 */
public class ServerThread implements Runnable {
    private final DatagramSocket ds;
    private static final int BUFF_SIZE = 1024;

    /**
     * @param ds UDP socket, that should be listened
     */
    public ServerThread(DatagramSocket ds) {
        this.ds = ds;
    }

    /**
     * Start listening the previously specified socket for the incoming messages.
     */
    @Override
    public void run() {
        try {
            byte[] receiveMessage = new byte[BUFF_SIZE];
            byte[] sendMessage;
            DatagramPacket receivePacket;
            DatagramPacket sendPacket;
            InetAddress inetAddress;
            int extPort;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
                ds.setSoTimeout(10);
                ds.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                message = "Hello, " + message;
                sendMessage = message.getBytes();
                inetAddress = receivePacket.getAddress();
                extPort = receivePacket.getPort();
                sendPacket = new DatagramPacket(sendMessage, sendMessage.length, inetAddress, extPort);
                ds.send(sendPacket);
                } catch (SocketTimeoutException ignore) {}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
