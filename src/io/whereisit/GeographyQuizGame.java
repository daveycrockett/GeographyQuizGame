package io.whereisit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
    private double EXTENT_THRESHOLD = 800;
    private Dimension preferredSize;
    int minWindowX, minWindowY, maxWindowX, maxWindowY;
    int panToMinWindowX, panToMinWindowY, panToMaxWindowX, panToMaxWindowY;
    private boolean panning = false;
    private int frames;
    GeographyOptionsPanel optionsPanel;
    JScrollPane mapScroller;

    BufferedImage br = null;
    private final JLabel status;
    JButton nextButton, flipButton, zoomButton, optionsButton;
    boolean front = false;
    private JFrame frame;

    public void reformatStatusBar() {
        String statusText = model.getCurrentCardsDisplay();
        if (!statusText.equals(""));
        this.status.setText(statusText);
    }

    private void paintBorder(Graphics g, Country country, Color color) {
        g.setColor(color);
        for (Vector<Point> border: country.getBorders()) {
            for (Point pi : border) {
                double xPrime = ((pi.getX() * MAP_TO_BORDER_SCALE) - (minWindowX - 1)) * SCALE_FACTOR;
                double yPrime = ((pi.getY() * MAP_TO_BORDER_SCALE) - (minWindowY - 1)) * SCALE_FACTOR;
                g.fillRect((int) xPrime, (int) yPrime, 1, 1);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(br, 0, 0, preferredSize.width, preferredSize.height, minWindowX, minWindowY, maxWindowX, maxWindowY, Color.white, null);
        if (!panning) {
            if (model.getCurrentCountry() != null) {
                paintBorder(g, model.getCurrentCountry(), Color.red);
            }
            if (model.getMouseOverCountry() != null) {
                paintBorder(g, model.getMouseOverCountry(), Color.blue);
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

        optionsPanel = new GeographyOptionsPanel(this, model);
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
        mapScroller = new JScrollPane(this);
        frame.getContentPane().add(mapScroller, BorderLayout.CENTER);
        frame.getContentPane().add(statusPanel, BorderLayout.SOUTH);
        frame.getContentPane().add(makeButtonPanel(), BorderLayout.WEST);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        MouseAdapter me = new MouseAdapter() {

            Point dragStart;
            Rectangle dragRect;

            @Override
            public void mouseMoved(MouseEvent me) {
                checkMouseOverCountry((int)me.getPoint().getX(), (int)me.getPoint().getY());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
                dragRect = mapScroller.getVisibleRect();
            }

            @Override
            public void mouseDragged(MouseEvent me) {
                double xDelt = me.getX() - dragStart.getX();
                double yDelt = me.getY() - dragStart.getY();
                dragStart = me.getPoint();
                int newX = (int)Math.min(Math.max((dragRect.getMinX() - xDelt), 0), (getWidth() - dragRect.getWidth()));
                int newY = (int)Math.min(Math.max((dragRect.getMinY() - yDelt), 0), (getHeight() - dragRect.getHeight()));
                dragRect.setBounds(newX, newY, (int)(dragRect.getWidth()), (int)(dragRect.getHeight()));
                scrollRectToVisible(dragRect);
            }
        };

        addMouseMotionListener(me);
        addMouseListener(me);

    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == flipButton && model.getCurrentCountry() != null) {
            front = !front;
            reformatStatusBar();
        } else if (ae.getSource() == nextButton) {
            try {
                model.nextQuestion();
                model.setMouseOverCountry(null);
                if (model.getCurrentCountry().getExtent() < EXTENT_THRESHOLD) {
                    int centerX = (int)(model.getCurrentCountry().getCenter().getX() * MAP_TO_BORDER_SCALE);
                    int centerY = (int)(model.getCurrentCountry().getCenter().getY() * MAP_TO_BORDER_SCALE);
                    SCALE_FACTOR = MIN_ZOOM * 4;
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
                reformatStatusBar();
            } catch (IllegalQuizStateException e) {
                status.setText("You must check some stacks to draw from before clicking next");
            }
        } else if (ae.getSource() == zoomButton) {
            panToMinWindowX = panToMinWindowY = 0;
            panToMaxWindowX = br.getWidth();
            panToMaxWindowY = br.getHeight();
            SCALE_FACTOR = MIN_ZOOM;
            panning = true;
            frames = 20;
            repaint();
        } else if (ae.getSource() == optionsButton) {
            optionsPanel.showOptions();
        }
    }

    public void checkMouseOverCountry(int x, int y) {
        Country mouseOverCountry = model.findCountry((int)(((x / SCALE_FACTOR) + minWindowX) / MAP_TO_BORDER_SCALE), (int)(((y / SCALE_FACTOR) + minWindowY) / MAP_TO_BORDER_SCALE));
        if (mouseOverCountry != null) {
            model.setMouseOverCountry(mouseOverCountry);
            reformatStatusBar();
            repaint();
        }
    }

    public JPanel makeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        nextButton = new JButton("next");
        flipButton = new JButton("flip");
        zoomButton = new JButton("zoom");
        optionsButton = new JButton("options");
        nextButton.addActionListener(this);
        flipButton.addActionListener(this);
        zoomButton.addActionListener(this);
        optionsButton.addActionListener(this);
        buttonPanel.add(nextButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        buttonPanel.add(flipButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        buttonPanel.add(zoomButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        buttonPanel.add(optionsButton);
        return buttonPanel;
    }

    public static void main(String[] argv) {
        new GeographyQuizGame(new JFrame("Countries of the World"));
    }
}
