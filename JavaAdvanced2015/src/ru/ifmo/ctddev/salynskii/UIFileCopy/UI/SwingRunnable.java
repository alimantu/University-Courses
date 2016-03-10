package ru.ifmo.ctddev.salynskii.UIFileCopy.UI;

import javafx.util.Pair;
import ru.ifmo.ctddev.salynskii.UIFileCopy.utils.copy.CopyValues;
import ru.ifmo.ctddev.salynskii.UIFileCopy.utils.message.Message;
import ru.ifmo.ctddev.salynskii.UIFileCopy.utils.message.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Alimantu on 07/03/16.
 */
public class SwingRunnable implements Runnable {
    private final AtomicLong copiedBytes;
    private final AtomicLong totalBytes;
    private final JFrame jFrame;
    private JTextArea textArea;
    private JLabel speedLabel;
    private JLabel progressLabel;
    private JButton button;
    private JProgressBar progressBar;
    private final Pair<ConcurrentLinkedQueue<Message>, ConcurrentLinkedQueue<Message>> messagesChanel;
    private static final int DEFAULT_WIDTH = 750;
    private static final int DEFAULT_HEIGHT = 250;
    private static final int DEFAULT_HEIGHT_STEP = 50;
    private static final int DEFAULT_WIDTH_STEP = 100;
    private static final int PROGRESS_MAX = 10000;

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
        initUI();
        checkMessages();
    }

    private void checkMessages() {
        boolean alive = true;
        long start = 0;
        while (alive) {
            alive = !Thread.interrupted();
            if (!alive) {
                exit();
            }
            while (!messagesChanel.getKey().isEmpty()) {
                Message income = messagesChanel.getKey().poll();
                if (income.getMessageType() == MessageType.SCANNING_COMPLETED) {
                    alive = false;
                    start = System.currentTimeMillis();
                    break;
                } else {
                    exit();
                }
            }
        }

        jFrame.pack();
        jFrame.setVisible(true);

        boolean completed = false;
        while (!completed) {
            if (Thread.interrupted()) {
                exit();
            }
            while (!messagesChanel.getKey().isEmpty()) {
                Message income = messagesChanel.getKey().poll();
                switch (income.getMessageType()) {
                    case CORRELATIONS_MAP:
                        // here is some hint, I don't like it at all, but it is works.
                        ConcurrentMap<String, CopyValues> correlationResolutions =
                                getCorrelationsResolutions((Map<String, Pair<Path, Path>>) income.getValue());
                        while (correlationResolutions.isEmpty());
                        messagesChanel.getValue().add(new Message(MessageType.CORRELATION_RESOLUTIONS, correlationResolutions));
                        start = System.currentTimeMillis();
                        break;
                    case COPY_COMPLETED:
                        completed = true;
                        button.setText("Close");
                        break;
                }
            }
            speedLabel.setText(getSpeed(start));
            progressLabel.setText(getProgressText());
            progressBar.setValue(getProgress());
            if (messagesLogSet) {
                String tmp = "";
                while (!messagesLog.isEmpty()) {
                    tmp += messagesLog.poll() + "\n";
                }
                textArea.append(tmp);
            }
            Thread.yield();
        }
    }

    private ConcurrentMap<String, CopyValues> getCorrelationsResolutions(Map<String, Pair<Path, Path>> correlationsMap) {
        final ConcurrentMap<String, CopyValues> correlationResolutions = new ConcurrentHashMap<>();

        JDialog dialog = new JDialog(jFrame);
        dialog.setLocation(dialog.getParent().getX() + DEFAULT_WIDTH_STEP,
                dialog.getParent().getY() + DEFAULT_HEIGHT_STEP);
        GridBagLayout gbl = new GridBagLayout();
        dialog.getContentPane().setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        dialog.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        // First level - just label with some text
        JLabel label = new JLabel("Default correlation resolutions");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.addLayoutComponent(label, gbc);
        dialog.add(label);

        // Second level - some panel with checkboxes for resolve the way to solve correlations
        JPanel panel = new JPanel(new FlowLayout());
        JRadioButton button1 = new JRadioButton("Don't copy");
        JRadioButton button2 = new JRadioButton("Create new file");
        JRadioButton button3 = new JRadioButton("Replace");
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(button1);
        buttonGroup.add(button2);
        buttonGroup.add(button3);
        button2.setSelected(true);
        panel.add(button1);
        panel.add(button2);
        panel.add(button3);
        gbl.addLayoutComponent(panel, gbc);
        dialog.add(panel);

        // Third level - button to confirm the choose
        JButton button = new JButton("Confirm");
        button.addActionListener(e -> {
            fillCorrelationsResolutions(correlationResolutions, correlationsMap, button1, button3);
            dialog.dispose();
        });
        gbl.addLayoutComponent(button, gbc);
        dialog.add(button);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fillCorrelationsResolutions(correlationResolutions, correlationsMap, button1, button3);
                dialog.dispose();
            }
        });

        dialog.pack();
        dialog.setVisible(true);
        return correlationResolutions;
    }

    private void fillCorrelationsResolutions(ConcurrentMap<String, CopyValues> correlationResolutions,
                                             Map<String, Pair<Path, Path>> correlationsMap,
                                             JRadioButton button1, JRadioButton button3) {
        final CopyValues cv;
        if (button1.isSelected()) {
            cv = CopyValues.IGNORE_MODE;
        } else if (button3.isSelected()) {
            cv = CopyValues.REPLACE_MODE;
        } else {
            cv = CopyValues.COPYING_WITH_MARKER;
        }
        Map<String, CopyValues> tmp = new HashMap<>();
        correlationsMap.forEach((k, v) -> tmp.put(k, cv));
        correlationResolutions.putAll(tmp);
    }

    // It's very strange, but OS X think that there is about 1000000 bytes in one Mb so we don't use the 1048576 but 10^6
    private String getSpeed(long start) {
        return Long.toString(copiedBytes.longValue() / 1000 / (System.currentTimeMillis() - start + 1)) +
                " Mb/sec";
    }

    private int getProgress() {
        Double tmp = copiedBytes.longValue() * 10000.0 / totalBytes.longValue();
        return tmp.intValue();
    }


    private String getProgressText() {
        return String.format("Copied: %.2f/%.2f Mb", copiedBytes.longValue() * 1.0 / 1000000,
                totalBytes.longValue() * 1.0 / 1000000);
    }

    private void exit() {
        sendMessage(new Message(MessageType.CLOSE_APP, null));
        System.exit(0);
    }

    private void sendMessage(Message message) {
        messagesChanel.getValue().add(message);
    }

    private void initUI() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        jFrame.setIconImage(new ImageIcon("res/icon.png").getImage());

        jFrame.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        jFrame.setMinimumSize(new Dimension(DEFAULT_WIDTH - DEFAULT_WIDTH_STEP, DEFAULT_HEIGHT - DEFAULT_HEIGHT_STEP));
        jFrame.setMaximumSize(new Dimension(DEFAULT_WIDTH + DEFAULT_WIDTH_STEP, DEFAULT_HEIGHT + DEFAULT_HEIGHT_STEP));
        jFrame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        jFrame.setLocationRelativeTo(null);
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        jFrame.getContentPane().setLayout(gbl);
        jFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        // First level - progress bar
        progressBar = new JProgressBar(0, PROGRESS_MAX);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(7, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbl.addLayoutComponent(progressBar, gbc);
        jFrame.add(progressBar);

        // Second level - speed level and cancel button
        speedLabel = new JLabel();
        speedLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 5, 5, 5);
        gbl.addLayoutComponent(speedLabel, gbc);
        jFrame.add(speedLabel);
        progressLabel = new JLabel();
        gbl.addLayoutComponent(progressLabel, gbc);
        jFrame.add(progressLabel);
        button = new JButton("Cancel");
        button.addActionListener(e -> exit());
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.NONE;
        gbl.addLayoutComponent(button, gbc);
        jFrame.add(button);

        // Third level - additional info
        if (messagesLogSet) {
            textArea = new JTextArea();
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(300, 100));
            gbc.weighty = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 5, 5, 7);
            gbl.addLayoutComponent(scrollPane, gbc);
            jFrame.add(scrollPane);
        }
    }
}
