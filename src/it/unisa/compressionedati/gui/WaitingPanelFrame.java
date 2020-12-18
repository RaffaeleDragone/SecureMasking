package it.unisa.compressionedati.gui;

import javax.swing.*;
import java.awt.*;

public class WaitingPanelFrame extends JFrame {

    private JTextArea console;
    private JFrame  parentFrame;

    public WaitingPanelFrame(JFrame parent) throws HeadlessException {
        this.console = new JTextArea();
        this.parentFrame=parent;
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(500,250);
        this.setLocation(parentFrame.getX(),parentFrame.getY());
        this.setUndecorated(true);
        console = new JTextArea();
        JScrollPane scr = new JScrollPane(console,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        console.setSize(470,240);
        console.setEditable(false);

        this.add(scr);
    }

    public void writeOnConsole(String text){
        this.console.append(text+"\n");
        this.console.setCaretPosition(console.getDocument().getLength());
    }

    public JFrame getParent(){
        return this.parentFrame;

    }
}
