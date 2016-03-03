package ru.ifmo.ctddev.salynskii.HelloUDP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * This class is simple realization of {@link Runnable} interface that send messages in the
 * <code>prefix + threadNumb + "_" + messageNumb</code> format to the specified port of the received
 * inet address.
 * @author Alimantu
 */
public class ClientThread implements Runnable {
    private final String prefix;
    private final int threadNumber;
    private final int messageCount;
    private final InetAddress inetAddress;
    private final int port;

    /**
     * Receives the values used for the constructing of the sent messages and the information about
     * server, that should receive them.
     * @param prefix prefix of the sent message
     * @param threadNumber id of the current thread
     * @param messageCount number of messages, that should be sent and received after
     * @param ia address of the server
     * @param port port used by the server to receive the messages
     */
    ClientThread(String prefix, int threadNumber, int messageCount, InetAddress ia, int port) {
        this.prefix = prefix;
        this.threadNumber = threadNumber;
        this.messageCount = messageCount;
        this.inetAddress = ia;
        this.port = port;
    }

    /**
     * Starts to sent the messages in the <code>prefix + threadNumb + "_" + messageNumb</code> format and
     * receives the answers from the server, stops after receiving the {@link ClientThread#messageCount} of the
     * correct server answers.
     */
    @Override
    public void run() {
        int j = 0;
        while (j < messageCount) {
            try (DatagramSocket ds = new DatagramSocket()) {
                String message = prefix + threadNumber + "_" + j;
                byte[] receiveMessage = new byte[1024];
                byte[] sendMessage = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendMessage, sendMessage.length, inetAddress, port);
                DatagramPacket receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
                ds.send(sendPacket);
                ds.setSoTimeout(100);
                ds.receive(receivePacket);
                if ((new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength())).equals("Hello, " + message)) {
                    j++;
                    System.err.println(message);
                    System.err.println(new String(receivePacket.getData(), 0, receivePacket.getLength()));
                }
            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
