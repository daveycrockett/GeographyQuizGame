package io.whereisit.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class Segmenter extends JPanel implements MouseListener, ActionListener {
    private static final long serialVersionUID = 1L;

    private BufferedImage br;
    private Vector<String> countries = new Vector<String>();
    private Iterator<String> countryIterator;
    private Vector<Vector<Point>> currentBorders = new Vector<Vector<Point>>();
//    private PrintWriter pr;
    private JPanel statusPanel = new JPanel();
    private JLabel countryLabel = new JLabel();
    private JButton nextButton = new JButton("next");
    private JButton skipButton = new JButton("skip");
    private JButton zoomButton = new JButton("zoom");
    private JScrollPane scroller;
    private double zoomFactor = 0.25;
    private String currentCountry;
    
    public Segmenter() {
        try {
            br = ImageIO.read(new File("worldmap.png"));
            this.setPreferredSize(new Dimension(br.getWidth(), br.getHeight()));
//            pr = new PrintWriter(new FileWriter("world_borders.txt", false));
            Scanner scan = new Scanner(new File("countries_and_capitals.txt"));
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                String[] tokens = line.split("\t");
                if (tokens.length > 1) {
                    countries.add(tokens[1]);
                }
            }
            countryIterator = countries.iterator();
            scroller = new JScrollPane(this);
            scroller.setPreferredSize(new Dimension((int)(br.getWidth() * zoomFactor), (int)(br.getHeight() * zoomFactor)));
            
            nextButton.addActionListener(this);
            skipButton.addActionListener(this);
            zoomButton.addActionListener(this);
            this.addMouseListener(this);
            
            currentCountry = countryIterator.next();
            countryLabel.setText(currentCountry);
            
            statusPanel.add(countryLabel);
            statusPanel.add(nextButton);
            statusPanel.add(skipButton);
            statusPanel.add(zoomButton);
        } catch (IOException ioe) {
            System.out.println(ioe);
            System.exit(-1);
        }
    }
    
    public JPanel getStatusPanel() {
        return statusPanel;
    }

    protected void paintComponent(Graphics g) {
        g.drawImage(br, 0, 0, (int)(br.getWidth() * zoomFactor), (int)(br.getHeight() * zoomFactor), null);
        g.setColor(Color.BLUE);
        for (Vector<Point> currentBorder: currentBorders) {
            for (Point p: currentBorder) {
                g.fillRect((int)(p.getX() * zoomFactor), (int)(p.getY() * zoomFactor), 1, 1);
            }
        }
    }
    
    private Vector<Point> checkPoint(Point toCheck) {
        Hashtable<Point, Point> borderHash = new Hashtable<Point, Point>();
        Hashtable<Point, Point> visited = new Hashtable<Point, Point>();
        Vector<Point> toVisit = new Vector<Point>();
        Point pToVisit = new Point((int)(toCheck.getX() / zoomFactor), (int)(toCheck.getY() / zoomFactor));
        toVisit.add(pToVisit);
        
        int[][] offsets = {
                {-1, 0},
                {1, 0},
                {0, 1},
                {0, -1},
        };
        while (toVisit.size() > 0) {
            Point p = toVisit.remove(0);
            if (visited.containsKey(p)) {
                continue;
            }
            visited.put(p, p);
            for (int[] offset: offsets) {
                int xprime = (int)p.getX() + offset[0];
                int yprime = (int)p.getY() + offset[1];
                if (xprime > -1 && xprime < br.getWidth() && yprime > -1 && yprime < br.getHeight()) {
                    if (new Color(br.getRGB(xprime, yprime)).equals(Color.white)) {
                        if (!borderHash.containsKey(p)) {
                            borderHash.put(p, p);
                        }
                    } else if (!visited.containsKey(new Point(xprime, yprime))) {
                        toVisit.add(new Point(xprime, yprime));
                    }
                }
            }
        }
        Vector<Point> toRet = new Vector<Point>();
        toRet.addAll(borderHash.values());
        return toRet;
    }

    @Override
    public void mouseClicked(MouseEvent evt) {
        Vector<Point> border = checkPoint(evt.getPoint());
        currentBorders.add(border);
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == nextButton) {
            for (Vector<Point> border: currentBorders) {
                System.out.println(currentCountry);
//                pr.println(currentCountry);
                for (Point p: border) {
//                    pr.print((int)p.getX() + " " + (int)p.getY() + " ");
                    System.out.print((int)p.getX() + " " + (int)p.getY() + " ");
                }
//                pr.println();
                System.out.println();
            }
            
        } else if (evt.getSource() == skipButton) {
            // just skip it, don't print it
        } else if (evt.getSource() == zoomButton) {
            zoomFactor = (zoomFactor == 0.25) ? 1.0 : 0.25;
            scroller.setPreferredSize(new Dimension((int)(br.getWidth() * zoomFactor), (int)(br.getHeight() * zoomFactor)));
            repaint();
            return;
        }
        if (countryIterator.hasNext()) {
            currentCountry = countryIterator.next();
            countryLabel.setText(currentCountry);
            currentBorders.clear();
            repaint();
        } else {
//            pr.flush();
//            pr.close();
            System.exit(0);
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {}
    @Override
    public void mouseExited(MouseEvent arg0) { }
    @Override
    public void mousePressed(MouseEvent arg0) {}
    @Override
    public void mouseReleased(MouseEvent arg0) {}

    public static void main(String[] argv) {
        JFrame frame = new JFrame("Map Segmenter");
        Segmenter s = new Segmenter();
        JScrollPane scroller = new JScrollPane(s);
        Container c = frame.getContentPane();
        c.setLayout(new BorderLayout());
        c.add(scroller, BorderLayout.CENTER);
        c.add(s.getStatusPanel(), BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
