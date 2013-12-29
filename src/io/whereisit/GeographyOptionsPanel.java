package io.whereisit;

import io.whereisit.WorldModel.CardStatus;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

@SuppressWarnings("serial")
public class GeographyOptionsPanel extends JPanel implements ActionListener {

    JCheckBox[] checkBoxes;
    public JCheckBox[] getCheckBoxes() {
        return checkBoxes;
    }

    public boolean[] getPrevCheckStatus() {
        return prevCheckStatus;
    }

    boolean[] prevCheckStatus;
    JRadioButton fronts, backs, both, neither, random;
    boolean front = false;
    GeographyQuizGame view;
    WorldModel model;
    private JFrame frame = null;

    private class CardStatusButton extends JRadioButton {
        private CardStatus status;

        public CardStatusButton(String label, CardStatus status) {
            super(label);
            this.status = status;
        }

        public CardStatusButton(String label, CardStatus status, boolean selected) {
            super(label, selected);
            this.status = status;
        }

        public CardStatus getStatus() {
            return status;
        }
    }

    public GeographyOptionsPanel(GeographyQuizGame view, WorldModel model) {
        this.view = view;
        this.model = model;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Set<String> stacks = model.getRegions();
        checkBoxes = new JCheckBox[stacks.size()];
        prevCheckStatus = new boolean[stacks.size()];
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        int i = 0;
        for (String region: stacks) {
            checkBoxes[i] = new JCheckBox(region);
            checkBoxes[i].addActionListener(this);
            prevCheckStatus[i] = false;
            this.add(checkBoxes[i]);
            i++;
        }

        backs = new CardStatusButton("show country names", CardStatus.FRONT, true);
        fronts = new CardStatusButton("show capital cities", CardStatus.BACK);
        both = new CardStatusButton("show both", CardStatus.BOTH);
        random = new CardStatusButton("show random", CardStatus.RANDOM);
        neither = new CardStatusButton("show neither", CardStatus.NEITHER);

        fronts.addActionListener(this);
        backs.addActionListener(this);
        both.addActionListener(this);
        random.addActionListener(this);
        neither.addActionListener(this);

        ButtonGroup bg = new ButtonGroup();
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        bg.add(fronts);
        bg.add(backs);
        bg.add(both);
        bg.add(random);
        bg.add(neither);
        this.add(fronts);
        this.add(backs);
        this.add(both);
        this.add(random);
        this.add(neither);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof CardStatusButton) {
            model.setCardStatus(((CardStatusButton)e.getSource()).getStatus());
            view.reformatStatusBar();
        } else {
            JCheckBox checkBox = (JCheckBox)e.getSource();
            if (checkBox.isSelected()) {
                System.out.println("adding " + checkBox.getText());
                model.addRegion(checkBox.getText());
            } else {
                System.out.println("removing " + checkBox.getText());
                model.removeRegion(checkBox.getText());
            }
        }
    }

    public void showOptions() {
        if (frame == null) {
            frame = new JFrame("Options");
            frame.getContentPane().add(this);
            frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            frame.pack();
        }

        frame.setVisible(true);
    }

}
