package Launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

public class SinglePlayerPanel implements ActionListener {
    private static SinglePlayerPanel instance = new SinglePlayerPanel();
    private JTextField bots;
    private JComboBox head;

    public static void read(HashMap<String, String> map) {
        instance.bots.setText(map.get("spBots"));
        Launcher.setComboBoxIndex(instance.head, map.get("spHead"));
    }

    public static void write(HashMap<String, String> map) {
        map.put("spBots", instance.bots.getText());
        map.put("spHead", (String)instance.head.getSelectedItem());
    }

    public static Component initialize() {
        ArrayList<ComponentGroup> groups = new ArrayList<>();

        instance.bots = new JTextField();
        groups.add(new ComponentGroup(new JLabel("Bots: "), instance.bots));

        instance.head = new JComboBox(Launcher.headStrings);
        groups.add(new ComponentGroup(new JLabel("Head: "), instance.head));

        JButton playButton = new JButton("Play");
        playButton.setActionCommand( "Play" );
        playButton.addActionListener(instance);
        playButton.setMaximumSize(new Dimension(400, 20));
        groups.add(new ComponentGroup(new JLabel(""), playButton));

        return ComponentGroup.createGroupPanel(groups);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Launcher.launchGame("bots=" + instance.bots.getText() + " head=" + (instance.head.getSelectedIndex() + 1) + DisplayPanel.getCommandString());
    }

    public static void putDefaults(HashMap<String, String> map) {
        map.put("spBots", "1");
        map.put("spHead", Launcher.headStrings[0]);
    }
}
