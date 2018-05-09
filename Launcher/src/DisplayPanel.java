import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

public class DisplayPanel {
    private static DisplayPanel instance = new DisplayPanel();
    private JComboBox resolution;
    private JComboBox bitDepth;
    private JComboBox refreshRate;
    private JCheckBox fullscreen;
    private JCheckBox vsync;

    public static void putDefaults(HashMap<String, String> map) {
        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        DisplayMode currentMode = g.getDefaultScreenDevice().getDisplayMode();
        map.put("displayResolution", currentMode.getWidth() + " x " + currentMode.getHeight());
        map.put("displayBitDepth", Integer.toString(currentMode.getBitDepth()));
        map.put("displayRefreshRate", Integer.toString(currentMode.getRefreshRate()));
        map.put("displayFullscreen", "1");
        map.put("displayVSync", "1");
    }

    public static void read(HashMap<String, String> map) {
        Launcher.setComboBoxIndex(instance.resolution, map.get("displayResolution"));
        Launcher.setComboBoxIndex(instance.bitDepth, map.get("displayBitDepth"));
        Launcher.setComboBoxIndex(instance.refreshRate, map.get("displayRefreshRate"));
        instance.fullscreen.setSelected(map.get("displayFullscreen").equals("1"));
        instance.vsync.setSelected(map.get("displayVSync").equals("1"));
    }

    public static void write(HashMap<String, String> map) {
        map.put("displayResolution", (String)instance.resolution.getSelectedItem());
        map.put("displayBitDepth", instance.bitDepth.getSelectedItem().toString());
        map.put("displayRefreshRate", instance.refreshRate.getSelectedItem().toString());
        map.put("displayFullscreen", instance.fullscreen.isSelected() ? "1" : "0");
        map.put("displayVSync", instance.vsync.isSelected() ? "1" : "0");
    }

    public static Component initialize() {
        Resolution.populateResolutions();

        ArrayList<ComponentGroup> groups = new ArrayList<>();

        instance.resolution = new JComboBox(Resolution.getList());
        groups.add(new ComponentGroup(new JLabel("Resolution: "), instance.resolution));

        instance.bitDepth = new JComboBox(new String[] { "" });
        groups.add(new ComponentGroup(new JLabel("Bit Depth: "), instance.bitDepth));

        instance.refreshRate = new JComboBox(new String[] { "" });
        groups.add(new ComponentGroup(new JLabel("Refresh Rate: "), instance.refreshRate));

        JPanel checkPanel = new JPanel();
        checkPanel.setBackground(Launcher.bgColor);
        checkPanel.setLayout(new BorderLayout());
        groups.add(new ComponentGroup(new JLabel(""), checkPanel));

        instance.fullscreen = new JCheckBox("Fullscreen");
        instance.fullscreen.setBackground(Launcher.bgColor);
        checkPanel.add(instance.fullscreen, BorderLayout.WEST);

        instance.vsync = new JCheckBox("VSync");
        instance.vsync.setBackground(Launcher.bgColor);
        checkPanel.add(instance.vsync, BorderLayout.EAST);

        // setup combobox actions

        instance.bitDepth.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (instance.bitDepth.getSelectedItem() == null) { return; }
                Resolution resolution = Resolution.get((String)instance.resolution.getSelectedItem());
                Object oldRefresh = instance.refreshRate.getSelectedItem();
                boolean hasOldRefresh = false;
                int highestRefresh = -1;
                int depth = (Integer)instance.bitDepth.getSelectedItem();
                instance.refreshRate.removeAllItems();
                for (Integer refresh : resolution.depthRefresh.get(depth)) {
                    if (refresh > highestRefresh) { highestRefresh = refresh; }
                    if (refresh.equals(oldRefresh)) { hasOldRefresh = true; }
                    instance.refreshRate.addItem(refresh);
                }
                if (hasOldRefresh) {
                    instance.refreshRate.setSelectedItem(oldRefresh);
                } else {
                    instance.refreshRate.setSelectedItem(highestRefresh);
                }
            }
        });

        instance.resolution.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (instance.resolution.getSelectedItem() == null) { return; }
                Resolution resolution = Resolution.get((String)instance.resolution.getSelectedItem());
                Object oldDepth = instance.bitDepth.getSelectedItem();
                boolean hasOldDepth = false;
                int highestDepth = -1;
                instance.bitDepth.removeAllItems();
                for (Integer depth : resolution.depthRefresh.keySet()) {
                    if (depth > highestDepth) { highestDepth = depth; }
                    if (depth.equals(oldDepth)) { hasOldDepth = true; }
                    instance.bitDepth.addItem(depth);
                }
                if (hasOldDepth) {
                    instance.bitDepth.setSelectedItem(oldDepth);
                } else {
                    instance.bitDepth.setSelectedItem(highestDepth);
                }
            }
        });

        return ComponentGroup.createGroupPanel(groups);
    }

    public static String getCommandString() {
        String displayString = " display=";
        displayString += (instance.resolution.getSelectedItem().toString()).replace(" x ", ",") + ",";
        displayString += instance.bitDepth.getSelectedItem().toString() + ",";
        displayString += instance.refreshRate.getSelectedItem().toString() + ",";
        displayString += (instance.fullscreen.isSelected() ? "1" : "0") + ",";
        displayString += (instance.vsync.isSelected() ? "1" : "0");
        return displayString;
    }
}
