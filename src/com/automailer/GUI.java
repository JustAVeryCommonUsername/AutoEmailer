package com.automailer;

import com.blocks.BlockPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class GUI extends JFrame {
    private BlockPanel panel;
    public static Image icon;

    static {
        try {
            icon = ImageIO.read(GUI.class.getResource("/resources/icon.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public GUI() {
        super("Auto Emailer");

        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        setSize((int) (size.getWidth() / 2), (int) (size.getHeight() / 1.5));

        setLocationRelativeTo(null);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(icon);

        panel = new BlockPanel(Main.data.getPrograms().get(0), this);
        add(panel);

        new Thread(() -> {
            long lastTime = System.nanoTime();
            final double ns = 1000000000.0 / 60.0;
            double delta = 0;
            while (true) {
                long now = System.nanoTime();
                delta += (now - lastTime) / ns;
                lastTime = now;
                while (delta >= 1) {
                    panel.setSize(getSize());
                    panel.repaint();
                    delta--;
                }
            }
        }).start();
    }
}
