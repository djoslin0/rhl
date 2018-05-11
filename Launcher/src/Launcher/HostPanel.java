package Launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

public class HostPanel implements ActionListener {
    private static HostPanel instance = new HostPanel();
    private JTextField port;
    private JTextField bots;

    public static Component initialize() {
        ArrayList<ComponentGroup> groups = new ArrayList<>();

        instance.port = new JTextField();
        groups.add(new ComponentGroup(new JLabel("Port: "), instance.port));

        instance.bots = new JTextField();
        groups.add(new ComponentGroup(new JLabel("Bots: "), instance.bots));

        JButton joinButton = new JButton("Host");
        joinButton.setActionCommand( "Host" );
        joinButton.addActionListener(instance);
        joinButton.setMaximumSize(new Dimension(400, 20));
        groups.add(new ComponentGroup(new JLabel(""), joinButton));

        return ComponentGroup.createGroupPanel(groups);
    }

    public static void putDefaults(HashMap<String, String> map) {
        map.put("hostPort", "8800");
        map.put("hostBots", "0");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Launcher.launchGame("s " + port.getText() + " bots=" + bots.getText());
    }

    public static void read(HashMap<String, String> map) {
        instance.port.setText(map.get("hostPort"));
        instance.bots.setText(map.get("hostBots"));
    }

    public static void write(HashMap<String, String> map) {
        map.put("hostPort", instance.port.getText());
        map.put("hostBots", instance.bots.getText());
    }
}
