package Launcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

public class ComponentGroup {
    public Component c1;
    public Component c2;

    public ComponentGroup(Component c1, Component c2) {
        this.c1 = c1;
        this.c2 = c2;
    }

    public static JPanel createGroupPanel(ArrayList<ComponentGroup> groups) {
        JPanel panel = new JPanel();
        panel.setBackground(Launcher.bgColor);
        panel.setBorder(new EmptyBorder(5, 10, 10, 10));

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

        GroupLayout.ParallelGroup hpg1 = layout.createParallelGroup();
        GroupLayout.ParallelGroup hpg2 = layout.createParallelGroup();

        hGroup.addGroup(hpg1);
        hGroup.addGroup(hpg2);

        for (ComponentGroup group : groups) {
            hpg1.addComponent(group.c1);
            hpg2.addComponent(group.c2);
            vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(group.c1).addComponent(group.c2));
        }

        layout.setHorizontalGroup(hGroup);
        layout.setVerticalGroup(vGroup);

        return panel;
    }
}