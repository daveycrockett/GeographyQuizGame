package io.whereisit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class GeographyQuizGame extends JPanel implements ActionListener {
    private WorldModel model;
    private double SCALE_FACTOR = .5;
    private double MAP_TO_POINT_SCALE = .5;
    
    private Country mouseOverCountry;
    BufferedImage br = null;
    private final JLabel status;
    JCheckBox[] checkBoxes;
    boolean[] prevCheckStatus;
    JButton nextButton, flipButton;
    JRadioButton fronts, backs, both, neither, random;
    boolean front = false;
    private JFrame frame;

    public void reformatStatusBar() {
        if (model.getCurrentCountry() == null && mouseOverCountry == null) {
            return;
        }
        String status = "";
        if (mouseOverCountry != null) {
            status += "In Blue: " + mouseOverCountry.getName() + "     ";
        }
        if (model.getCurrentCountry() != null) {
            String cardFront = model.getCurrentCountry().getCapitals().get(0);
            String cardBack = model.getCurrentCountry().getName();
            status += "Quiz ";
            String keyString = "";
            if (model.getCurrentCountry() != null) {
                keyString = " (in red)";
            } else {
                keyString = " (no border shown)";
            }
            if (neither.isSelected()) {
                if (model.getCurrentCountry() == null) {
                    if (front) {
                        status += "Capital: " + cardFront;
                    } else {
                        status += "Province: " + cardBack;
                    }
                } else {
                    status += "Country" + keyString + ": ?";
                }
            } else if (both.isSelected()) {
                status += "Region" + keyString + ": " + cardFront
                        + ", " + cardBack;
            } else if (front) {
                status += "Capital City" + keyString + ": " + cardFront;
            } else {
                status += "Region " + keyString + ": " + cardBack;
            }
        }
        this.status.setText(status);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(br, 0, 0, (int)(br.getWidth() * SCALE_FACTOR), (int)(br.getHeight() * SCALE_FACTOR), null);
        if (model.getCurrentCountry() != null) {
            g.setColor(Color.red);
            for (Vector<Point> border: model.getCurrentCountry().getBorders()) {
                for (Point pi : border) {
                    g.fillRect((int) (pi.getX() * SCALE_FACTOR * MAP_TO_POINT_SCALE), (int) (pi.getY() * SCALE_FACTOR * MAP_TO_POINT_SCALE), 1, 1);
                }
            }
        }
        if (mouseOverCountry != null) {
            g.setColor(Color.blue);
            for (Vector<Point> border: mouseOverCountry.getBorders()) {
                for (Point pi : border) {
                    g.fillRect((int) (pi.getX() * SCALE_FACTOR * MAP_TO_POINT_SCALE), (int) (pi.getY() * SCALE_FACTOR * MAP_TO_POINT_SCALE), 1, 1);
                }
            }
        }
    }

    public GeographyQuizGame(JFrame parent) {
        try {
            model = new WorldModel();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(-1);
        }
        br = model.getMap();
        frame = parent;
        status = new JLabel(
                "To begin, select the stacks you want to\n draw from, and whether you want to be quizzed on fronts of cards (capitals),\n backs of cards (states, provinces, countries), both, or neither.  Mouse over a country to get its name");

        JPanel statusPanel = new JPanel();
        statusPanel.add(status);

        parent.getContentPane().setLayout(new BorderLayout());
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        SCALE_FACTOR = Math.max((screenSize.width * 0.8) / (double)br.getWidth(), (screenSize.height * 0.9) / (double)br.getHeight());
        setPreferredSize(new Dimension((int)(br.getWidth() * SCALE_FACTOR), (int)(br.getHeight() * SCALE_FACTOR)));
        JScrollPane scroller = new JScrollPane(this);
        frame.getContentPane().add(scroller, BorderLayout.CENTER);
        frame.getContentPane().add(statusPanel, BorderLayout.SOUTH);
        frame.getContentPane().add(makeButtonPanel(), BorderLayout.WEST);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        System.out.println("SCALE_FACTOR = " + SCALE_FACTOR);
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent me) {
                int x = (int) me.getPoint().getX();
                int y = (int) me.getPoint().getY();
                checkMouseOverCountry(x, y);
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == flipButton && model.getCurrentCountry() != null) {
            if (front) {
                front = false;
            } else {
                front = true;
            }
            reformatStatusBar();
        } else if (ae.getSource() == nextButton) {
            boolean changed = false;
            for (int i = 0; i < checkBoxes.length; i++) {
                if (checkBoxes[i].isSelected() != prevCheckStatus[i]) {
                    changed = true;
                    prevCheckStatus[i] = checkBoxes[i].isSelected();
                }

            }
            if (changed || model.getQuizStack().size() == 0) {
                Vector<String> regions = new Vector<String>();
                for (JCheckBox checkbox: checkBoxes) {
                    if (checkbox.isSelected()) {
                        regions.add(checkbox.getText());
                    }
                }
                model.setQuizRegions(regions);
            }
            if (model.getQuizStack().size() == 0) {
                status.setText("You must check some stacks to draw from before clicking next");
                return;
            } else {
                model.nextQuestion();
                repaint();
                if (random.isSelected()) {
                    if (Math.random() < .5) {
                        front = true;
                    } else {
                        front = false;
                    }
                }
            }
            reformatStatusBar();
        } else {
            if (fronts.isSelected()) {
                front = true;
            } else if (backs.isSelected()) {
                front = false;
            } else if (random.isSelected()) {
                if (Math.random() < .5) {
                    front = true;
                } else {
                    front = false;
                }
            } else if (neither.isSelected()) {
                front = false;
            }
            reformatStatusBar();
        }

    }

    public void checkMouseOverCountry(int x, int y) {
        mouseOverCountry = model.findCountry((int)((x / SCALE_FACTOR) / MAP_TO_POINT_SCALE), (int)((y / SCALE_FACTOR) / MAP_TO_POINT_SCALE));
        if (mouseOverCountry != null) {
            reformatStatusBar();
            repaint();
        }
    }

    public JPanel makeButtonPanel() {
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

        nextButton = new JButton("next");

        flipButton = new JButton("flip");
        nextButton.addActionListener(this);
        flipButton.addActionListener(this);
        checkBoxPanel.add(nextButton);
        checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        checkBoxPanel.add(flipButton);
        Set<String> stacks = model.getRegions();
        checkBoxes = new JCheckBox[stacks.size()];
        prevCheckStatus = new boolean[stacks.size()];
        checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        int i = 0;
        for (String region: stacks) {
            checkBoxes[i] = new JCheckBox(region);
            prevCheckStatus[i] = false;
            checkBoxPanel.add(checkBoxes[i]);
            i++;
        }

        backs = new JRadioButton("show country names", true);
        fronts = new JRadioButton("show capital cities");
        both = new JRadioButton("show both");
        random = new JRadioButton("show random");
        neither = new JRadioButton("show neither");

        fronts.addActionListener(this);
        backs.addActionListener(this);
        both.addActionListener(this);
        random.addActionListener(this);
        neither.addActionListener(this);

        ButtonGroup bg = new ButtonGroup();
        checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        bg.add(fronts);
        bg.add(backs);
        bg.add(both);
        bg.add(random);
        bg.add(neither);
        checkBoxPanel.add(fronts);
        checkBoxPanel.add(backs);
        checkBoxPanel.add(both);
        checkBoxPanel.add(random);
        checkBoxPanel.add(neither);

        return checkBoxPanel;

    }

    public static void main(String[] argv) {
        new GeographyQuizGame(new JFrame("Countries of the World"));
    }
}
