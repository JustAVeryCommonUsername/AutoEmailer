package com.automailer;

import com.automailer.email.EmailManager;
import com.blocks.*;

import javax.imageio.ImageIO;
import javax.naming.ldap.Control;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Main {
    private static final File APPDATA_LOCATION = new File(System.getenv("APPDATA"), "AutoMailer\\data.dat");

    public static GUI gui;
    public static AppData data;
    public static EmailManager em;

    public static void main(String[] args) {
        Main main = new Main();

        if (!(args.length != 0 && args[0].equals("nogui"))) {
            main.createGui();
        }
    }

    public Main() {
        checkAvailability(37041);
        //addToStartup();

        BlockPalette.loadDefaultPalette();

        data = AppData.getAppData(APPDATA_LOCATION);
        if (data.getPrograms().isEmpty())
            data.getPrograms().add(new Program());

        HashMap<String, Object> variables = Program.variables;
        for (int i = 1; i < 10; i++) {
            variables.put("#" + i, i);
        }
    }

    public void createGui() {
        gui = new GUI();
        gui.setVisible(true);
        gui.addWindowListener(new WindowListener() {
            public void windowClosing(WindowEvent e) {
                data.saveAppData(APPDATA_LOCATION);
            }
            public void windowOpened(WindowEvent e) { }
            public void windowClosed(WindowEvent e) { }
            public void windowIconified(WindowEvent e) { }
            public void windowDeiconified(WindowEvent e) { }
            public void windowActivated(WindowEvent e) { }
            public void windowDeactivated(WindowEvent e) { }
        });
    }

    private void checkAvailability(int port) {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
        } catch (IOException e) {
            try {
                // Attempt to connect to other program
                Socket socket = new Socket("localhost", port);

                // Wait until other program is finished exiting
                socket.getInputStream().read();
                socket.close();
            } catch (IOException ignored) {
            }
        } finally {
            // Attempt to bind to port until available
            while (ss == null) {
                try {
                    ss = new ServerSocket(port);
                } catch (IOException ignored) {
                }
            }

            // Exit program if socket connects
            ServerSocket finalSs = ss;
            new Thread(() -> {
                try {
                    Socket socket = finalSs.accept();
                    data.saveAppData(APPDATA_LOCATION);

                    // Tell other program exiting is finished
                    socket.getOutputStream().write(1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }).start();
        }
    }

    private void addToStartup() {
        File file = new File("C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\StartUp",
                "AutoMailer.bat");
        try {
            if (file.createNewFile()) {
                String jarFile = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write("@echo off\n");
                writer.write("IF not exist \"" + jarFile + "\" GOTO notexist\n");
                writer.write("java -jar \"" + jarFile + "\" nogui\n");
                writer.write("EXIT\n");
                writer.write(":notexist\n");
                writer.write("(goto) 2>nul & del \"%~f0\"\n");
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}