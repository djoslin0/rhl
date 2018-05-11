package Launcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class Launcher extends JFrame {
    public static Color bgColor = new Color(200, 221, 242);
    public static String[] headStrings = { "Jaw", "Helmet" };
    private static boolean classMode = false;

    public Launcher(boolean classMode) {
        Launcher.classMode = classMode;
        setTitle("Robo Hockey League");
        setSize( 400, 400 );
        setLocationRelativeTo(null);
        setResizable(false);

        try {
            initializeLayout();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setVisible(true);

        readIni();
    }

    private HashMap<String, String> getDefaults() {
        HashMap<String, String> map = new HashMap<>();

        JoinPanel.putDefaults(map);
        HostPanel.putDefaults(map);
        SinglePlayerPanel.putDefaults(map);
        DisplayPanel.putDefaults(map);

        return map;
    }

    private void readIni() {
        HashMap<String, String> map = getDefaults();

        HashMap<String, String> readMap = new HashMap<>();

        String fileName1 = System.getProperty("user.home") + File.separator + "settings.ini";
        File file1 = new File(fileName1);

        String fileName2 = "settings.ini";
        File file2 = new File(fileName2);

        File file;
        if (file1.exists() && file2.exists()) {
            if (file1.lastModified() > file2.lastModified()) {
                readMap = Ini.read(fileName1);
            } else {
                readMap = Ini.read(fileName2);
            }
        } else if (file1.exists()) {
            readMap = Ini.read(fileName1);
        } else if (file2.exists()) {
            readMap = Ini.read(fileName2);
        }

        for (String key : readMap.keySet()) {
            map.put(key, readMap.get(key));
        }

        // load
        JoinPanel.read(map);
        HostPanel.read(map);
        SinglePlayerPanel.read(map);
        DisplayPanel.read(map);
    }

    private static void writeIni() {
        HashMap<String, String> map = new HashMap<>();

        JoinPanel.write(map);
        HostPanel.write(map);
        SinglePlayerPanel.write(map);
        DisplayPanel.write(map);

        String fileName = "settings.ini";

        try {
            Ini.write(fileName, map);
        } catch (Exception e) {
            fileName = System.getProperty("user.home") + File.separator + "settings.ini";
            Ini.write(fileName, map);
        }
    }

    public static void setComboBoxIndex(JComboBox comboBox, String key) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (comboBox.getItemAt(i).toString().equals(key)) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private void initializeLayout() throws IOException {
        setLayout (new BorderLayout());

        // logo
        JPanel topPanel = new JPanel();
        topPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        File logoFile = new File("assets/icons/logo.png");
        if (!logoFile.exists()) { logoFile = new File("game/assets/icons/logo.png"); }
        BufferedImage logo = ImageIO.read(logoFile);
        topPanel.add(new JLabel(new ImageIcon(logo)));
        this.add(topPanel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        this.add(tabs, BorderLayout.CENTER);

        tabs.addTab("Join", JoinPanel.initialize());
        tabs.addTab("Host", HostPanel.initialize());
        tabs.addTab("Single Player", SinglePlayerPanel.initialize());
        tabs.addTab("Display", DisplayPanel.initialize());
    }

    public static void launchGame(String params) {
        String javaParams = classMode ? "a3.MyGame" : "-jar RoboHockeyLeague.jar";
        String[] arr = ("java -Dsun.java2d.noddraw=true " + javaParams + " " + params).split(" ");
        ProcessBuilder pb = new ProcessBuilder(arr);
        if (!classMode) {
            pb.directory(new File(System.getProperty("user.dir") + "/game/"));
        }

        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        writeIni();
        System.exit(0);
    }

    public static void main(String[] args) throws IOException {
        new Launcher(args.length > 0 && args[0].equals("class"));
    }

}
