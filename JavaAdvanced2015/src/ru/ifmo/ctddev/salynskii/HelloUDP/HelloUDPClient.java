package ru.ifmo.ctddev.salynskii.HelloUDP;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * This class is realizes the client part for the client - server communication using UDP connection,
 * also it is the implementation of the {@link info.kgeorgiy.java.advanced.hello.HelloClient}.
 * @author Alimantu
 * @see info.kgeorgiy.java.advanced.hello.HelloClient
 */
public class HelloUDPClient implements HelloClient{

    public static void main(String[] args) {
        if (args.length != 5) {
            throw new IllegalArgumentException("Wrong format, expected:" +
                    " <Comp name/IP> <port> <message prefix> <messages per thread> <threads count>");
        }
        HelloUDPClient hc = new HelloUDPClient();
        hc.start(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
    }

    /**
     * This method creates specified count of client threads, that sends the message in format
     * <code>prefix + threadNumb + "_" + messageNumb</code>, to the receivers <code>addrName</code> port <code>port</code>.
     * @param addrName name or IP-address of the server
     * @param port port that should be used for the connection to the server
     * @param messPrefix prefix of the sent message
     * @param messPerThread number of messages we should send from each thread
     * @param threadCount number of threads should be used for sending messages
     */
    @Override
    public void start(String addrName, int port, String messPrefix, int messPerThread, int threadCount) {
        try {
            ArrayList<Thread> threads = new ArrayList<>();
            InetAddress ia = InetAddress.getByName(addrName);
            for (int i = 0; i < threadCount; i++) {
                Thread t = new Thread(new ClientThread(messPrefix, i, messPerThread, ia, port));
                threads.add(t);
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
        } catch (UnknownHostException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
