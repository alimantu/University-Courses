package ru.ifmo.ctddev.salynskii.HelloUDP;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * This class should be used for creating the simple implementation of the UDP server, that realizes the
 * {@link info.kgeorgiy.java.advanced.hello.HelloServer} interface.
 * @author Alimantu
 * @see info.kgeorgiy.java.advanced.hello.HelloServer
 */
public class HelloUDPServer implements HelloServer{
    private ArrayList<Thread> threads;
    private DatagramSocket ds;

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Wrong format, expected: <port> <message handlers count>");
        }
        HelloUDPServer hs = new HelloUDPServer();
        hs.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }

    /**
     * This method is uses for starting the servers on the specified port <code>port</code>, also server
     * would handle messages using the specified number of threads.
     * @param port port that should be listened
     * @param threadNumb number of threads, that should handle the incoming messages
     */
    @Override
    public void start(int port, int threadNumb) {
        threads = new ArrayList<>();
        try {
            ds = new DatagramSocket(port);
            System.err.println("InetAddress: " + ds.getLocalSocketAddress().toString() + "\nPort: " + ds.getLocalPort());
            for (int i = 0; i < threadNumb; i++) {
                Thread t = new Thread(new ServerThread(ds));
                threads.add(t);
                t.start();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method stop all the threads started using {@link HelloUDPServer#start(int, int)}
     * and waited for the incoming messages.
     */
    @Override
    public void close() {
        for (Thread t : threads) {
            t.interrupt();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        while (!ds.isClosed()) {
            ds.close();
        }
    }
}
