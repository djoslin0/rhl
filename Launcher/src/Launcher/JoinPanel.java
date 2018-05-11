package Launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

public class JoinPanel implements ActionListener {
    private static JoinPanel instance = new JoinPanel();
    private JTextField ip;
    private JTextField port;
    private JComboBox head;

    public static Component initialize() {
        ArrayList<ComponentGroup> groups = new ArrayList<>();

        instance.ip = new JTextField();
        groups.add(new ComponentGroup(new JLabel("IP: "), instance.ip));

        instance.port = new JTextField();
        groups.add(new ComponentGroup(new JLabel("Port: "), instance.port));

        instance.head = new JComboBox(Launcher.headStrings);
        groups.add(new ComponentGroup(new JLabel("Head: "), instance.head));

        JButton joinButton = new JButton("Join");
        joinButton.setActionCommand( "Join" );
        joinButton.addActionListener(instance);
        joinButton.setMaximumSize(new Dimension(400, 20));
        groups.add(new ComponentGroup(new JLabel(""), joinButton));

        return ComponentGroup.createGroupPanel(groups);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Launcher.launchGame("c " + ip.getText() + " " + port.getText() + " head=" + (instance.head.getSelectedIndex() + 1) + DisplayPanel.getCommandString());
    }

    public static void putDefaults(HashMap<String, String> map) {
        map.put("joinIp", "localhost");
        map.put("joinPort", "8800");
        map.put("joinHead", Launcher.headStrings[0]);
    }

    public static void read(HashMap<String, String> map) {
        instance.ip.setText(map.get("joinIp"));
        instance.port.setText(map.get("joinPort"));
        Launcher.setComboBoxIndex(instance.head, map.get("joinHead"));
    }

    public static void write(HashMap<String, String> map) {
        map.put("joinIp", instance.ip.getText());
        map.put("joinPort", instance.port.getText());
        map.put("joinHead", (String)instance.head.getSelectedItem());
    }
}
