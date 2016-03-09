package ru.ifmo.ctddev.salynskii.UIFileCopy.Utils;

import javafx.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Alimantu on 07/03/16.
 */
public class SwingRunnable implements Runnable {
    private final AtomicLong copiedBytes;
    private final AtomicLong totalBytes;
    private final JFrame jFrame;
    private final Pair<ConcurrentLinkedQueue<Message>, ConcurrentLinkedQueue<Message>> messagesChanel;
    private final int DEFAULT_WIDTH = 720;
    private final int DEFAULT_HEIGHT = 250;
    private ConcurrentLinkedQueue<String> messagesLog;
    private boolean messagesLogSet = false;

    public SwingRunnable(AtomicLong copiedBytes, AtomicLong totalBytes,
                         Pair<ConcurrentLinkedQueue<Message>, ConcurrentLinkedQueue<Message>> uIMessagesChanel) {
        checkNull(new Pair<>("AtomicLong copiedBytes", copiedBytes),
                new Pair<>("AtomicLong totalBytes", totalBytes));
        this.copiedBytes = copiedBytes;
        this.totalBytes = totalBytes;
        this.jFrame = new JFrame("UIFileCopy");
        this.messagesChanel = uIMessagesChanel;
    }

    public void setMessagesLog(ConcurrentLinkedQueue<String> messagesLog) {
        checkNull(new Pair<>("ConcurrentLinkedQueue<String> messagesLog", messagesLog));
        this.messagesLog = messagesLog;
        this.messagesLogSet = true;
    }

    @SafeVarargs
    private final void checkNull(Pair<String, Object>... pairs) {
        for (Pair<String, Object> p : pairs) {
            if (p.getValue() == null) {
                throw new IllegalArgumentException("Expected " + p.getKey() + ", but found null");
            }
        }
    }

    @Override
    public void run() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        // TODO jFrame.setIconImage();
//        jFrame.addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowClosing(WindowEvent e) {
//                super.windowClosing(e);
//            }
//        });
        //
        //
        // TODO uncomment max/min size
        jFrame.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
//        jFrame.setMinimumSize(new Dimension(200, 70));
//        jFrame.setMaximumSize(new Dimension(500, 100));
        jFrame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        jFrame.setLocationRelativeTo(null);
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.fill = GridBagConstraints.BOTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
//        gbc.gridheight = GridBagConstraints.RELATIVE;
        gbc.weightx = 1;
        gbc.weighty = 0;
        jFrame.getContentPane().setLayout(gbl);
        jFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        // First level - progress bar
        JProgressBar progressBar = new JProgressBar(0, 10000);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(7, 5, 5, 5);;
        gbl.addLayoutComponent(progressBar, gbc);
        jFrame.add(progressBar);

        // Second level - speed level and cancel button
        // TODO progress text field to speed text field
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(0, 5, 5, 5);
        gbl.addLayoutComponent(label, gbc);
        jFrame.add(label);
        JButton button = new JButton("Cancel");
        button.addActionListener(e -> exit());
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.NONE;
        gbl.addLayoutComponent(button, gbc);
        jFrame.add(button);
        // Third level - button to open additional info
        // TODO button

        // Fourth level - additional info
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(300, 100));
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 5, 5, 7);
        gbl.addLayoutComponent(scrollPane, gbc);
        jFrame.add(scrollPane);

        jFrame.pack();
        jFrame.setVisible(true);

        while (!Thread.interrupted()) {
            label.setText(copiedBytes  + "/" + totalBytes);
            progressBar.setValue(getProgress());
            if (messagesLogSet) {
                String tmp = "";
                while (!messagesLog.isEmpty()) {
//                    System.err.println("Hey we are here!");
                    tmp += messagesLog.poll() + "\n";
                }
//                System.out.println(tmp);
                textArea.append(tmp);
            }
            Thread.yield();
        }
    }

    private void exit() {
        sendMessage(new Message(MessageType.CLOSE_APP, null));
        // TODO uncomment
//        throw new BreakException();
        // TODO comment
        System.exit(0);
    }

    private void sendMessage(Message message) {
        messagesChanel.getValue().add(message);
    }

    private int getProgress() {
        Double tmp = copiedBytes.longValue() * 10000.0 / totalBytes.longValue();
        return tmp.intValue();
    }
}
