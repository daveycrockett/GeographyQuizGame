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
    private double SCALE_FACTOR;
    private double MIN_ZOOM;
    
    /** The difference in scale (precision) between points in the borders
     * file and points on the map image.  This can be caused by labeling countries
     * and a high-resolution map which is then scaled down for size reasons.  In the
     * case of the existing world_borders.txt and worldmap.png, the borders were 
     * labeled on an image with dimensions twice as large as the final, displayed map.
     */
    private double MAP_TO_BORDER_SCALE = .5;
    private double EXTENT_THRESHOLD = 300;
    private Dimension preferredSize;
    int minWindowX, minWindowY, maxWindowX, maxWindowY;
    int panToMinWindowX, panToMinWindowY, panToMaxWindowX, panToMaxWindowY;
    private boolean panning = false;
    private int frames;
    
    private Country mouseOverCountry;
    BufferedImage br = null;
    private final JLabel status;
    JCheckBox[] checkBoxes;
    boolean[] prevCheckStatus;
    JButton nextButton, flipButton, zoomButton;
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
        g.drawImage(br, 0, 0, preferredSize.width, preferredSize.height, minWindowX, minWindowY, maxWindowX, maxWindowY, Color.white, null);
        if (!panning) {
            if (model.getCurrentCountry() != null) {
                g.setColor(Color.red);
                for (Vector<Point> border: model.getCurrentCountry().getBorders()) {
                    for (Point pi : border) {
                        double xPrime = ((pi.getX() * MAP_TO_BORDER_SCALE) - minWindowX) * SCALE_FACTOR;
                        double yPrime = ((pi.getY() * MAP_TO_BORDER_SCALE) - minWindowY) * SCALE_FACTOR;
                        g.fillRect((int) xPrime, (int) yPrime, 1, 1);
                    }
                }
            }
            if (mouseOverCountry != null) {
                g.setColor(Color.blue);
                for (Vector<Point> border: mouseOverCountry.getBorders()) {
                    for (Point pi : border) {
                        double xPrime = ((pi.getX() * MAP_TO_BORDER_SCALE) - minWindowX) * SCALE_FACTOR;
                        double yPrime = ((pi.getY() * MAP_TO_BORDER_SCALE) - minWindowY) * SCALE_FACTOR;
                        g.fillRect((int) xPrime, (int) yPrime, 1, 1);
                    }
                }
            }
        } else {
            frames--;
            if (frames == 0) {
                minWindowX = panToMinWindowX;
                minWindowY = panToMinWindowY;
                maxWindowX = panToMaxWindowX;
                maxWindowY = panToMaxWindowY;
                panning = false;
            } else {
                minWindowX = interpolate(minWindowX, panToMinWindowX, frames);
                maxWindowX = interpolate(maxWindowX, panToMaxWindowX, frames);
                minWindowY = interpolate(minWindowY, panToMinWindowY, frames);
                maxWindowY = interpolate(maxWindowY, panToMaxWindowY, frames);
            }
            repaint();
        }
    }
    
    private int interpolate(int start, int end, int frames) {
        return (int)(((end - start) / (double)frames) + start);
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
        
        preferredSize = Toolkit.getDefaultToolkit().getScreenSize();
        preferredSize = new Dimension((int)(preferredSize.width * 0.8), (int)(preferredSize.height * 0.9));
        MIN_ZOOM = SCALE_FACTOR = Math.max(preferredSize.width / (double)br.getWidth(), preferredSize.height / (double)br.getHeight());
        preferredSize = new Dimension((int)(br.getWidth() * SCALE_FACTOR), (int)(br.getHeight() * SCALE_FACTOR));
        setPreferredSize(preferredSize);
        minWindowX = minWindowY = 0;
        maxWindowX = br.getWidth();
        maxWindowY = br.getHeight();
        JScrollPane scroller = new JScrollPane(this);
        frame.getContentPane().add(scroller, BorderLayout.CENTER);
        frame.getContentPane().add(statusPanel, BorderLayout.SOUTH);
        frame.getContentPane().add(makeButtonPanel(), BorderLayout.WEST);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

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
                mouseOverCountry = null;
                if (model.getCurrentCountry().getExtent() < EXTENT_THRESHOLD) {
                    int centerX = (int)(model.getCurrentCountry().getCenter().getX() * MAP_TO_BORDER_SCALE);
                    int centerY = (int)(model.getCurrentCountry().getCenter().getY() * MAP_TO_BORDER_SCALE);
                    SCALE_FACTOR = MIN_ZOOM * 2;
                    double sourceWidth = preferredSize.width / SCALE_FACTOR;
                    double sourceHeight = preferredSize.height / SCALE_FACTOR;
                    panToMinWindowX = (int) Math.max(centerX - (sourceWidth / 2), 0);
                    if (panToMinWindowX == 0) {
                        panToMaxWindowX = (int)sourceWidth;
                    } else {
                        panToMaxWindowX = (int) Math.min(centerX + (sourceWidth / 2), br.getWidth());
                        panToMinWindowX = panToMaxWindowX - (int)sourceWidth;
                    }
                    
                    panToMinWindowY = (int) Math.max(centerY - (sourceHeight / 2), 0);
                    if (panToMinWindowY == 0) {
                        panToMaxWindowY = (int)sourceHeight;
                    } else {
                        panToMaxWindowY = (int) Math.min(centerY + (sourceHeight / 2), br.getHeight());
                        panToMinWindowY = panToMaxWindowY - (int)sourceHeight;
                    }
                } else {
                    panToMinWindowX = panToMinWindowY = 0;
                    panToMaxWindowX = br.getWidth();
                    panToMaxWindowY = br.getHeight();
                    SCALE_FACTOR = MIN_ZOOM;
                }
                panning = true;
                frames = 20;
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
        } else if (ae.getSource() == zoomButton) {
            panToMinWindowX = panToMinWindowY = 0;
            panToMaxWindowX = br.getWidth();
            panToMaxWindowY = br.getHeight();
            SCALE_FACTOR = MIN_ZOOM;
            panning = true;
            frames = 20;
            repaint();
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
        mouseOverCountry = model.findCountry((int)(((x / SCALE_FACTOR) + minWindowX) / MAP_TO_BORDER_SCALE), (int)(((y / SCALE_FACTOR) + minWindowY) / MAP_TO_BORDER_SCALE));
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
        zoomButton = new JButton("zoom");
        nextButton.addActionListener(this);
        flipButton.addActionListener(this);
        zoomButton.addActionListener(this);
        checkBoxPanel.add(nextButton);
        checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        checkBoxPanel.add(flipButton);
        checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        checkBoxPanel.add(zoomButton);
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
