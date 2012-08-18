package io.whereisit;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import javax.imageio.ImageIO;

public class WorldModel {
    
    private Hashtable<String, Country> countryLookup = new Hashtable<String, Country>();
    private Hashtable<String, Vector<Country>> regionLookup = new Hashtable<String, Vector<Country>>();
    private BufferedImage map;
    private Vector<Country> quizStack = null;
    private Country currentCountry = null;
    
    public BufferedImage getMap() {
        return map;
    }
    
    public Set<String> getRegions() {
        return regionLookup.keySet();
    }

    public WorldModel() throws IOException {
        loadCountriesAndCapitals();
        loadRegions();
        loadBorders();
        optimizeBorders();
        map = ImageIO.read(getResource("worldmap.png"));
    }
    
    private void loadCountriesAndCapitals() throws IOException {
        Scanner countries = new Scanner(getResource("countries_and_capitals.txt"));
        String previousCountryName = null;
        while (countries.hasNextLine()) {
            String[] tokens = countries.nextLine().split("\t");
            if (tokens.length > 1) {
                Country c = new Country(tokens[1].trim());
                c.addCapital(tokens[0].trim());
                countryLookup.put(c.getName(), c);
                previousCountryName = c.getName();
            } else if (previousCountryName != null && tokens.length == 1) {
                countryLookup.get(previousCountryName).addCapital(tokens[0].trim());
            }
        }
    }
    
    private void loadRegions() throws IOException {
        Scanner regions = new Scanner(getResource("regions.txt"));
        String previousRegion = null;
        while (regions.hasNextLine()) {
            String line = regions.nextLine().trim();
            if (!line.startsWith("===") && !line.equals("") && previousRegion != null) {
                assert(countryLookup.containsKey(line));
                regionLookup.get(previousRegion).add(countryLookup.get(line));
            } else if (line.startsWith("===")) {
                line = line.replaceAll("===", "");
                regionLookup.put(line, new Vector<Country>());
                previousRegion = line;
            }
        }
    }
    
    private void loadBorders() throws IOException {
        Scanner borders = new Scanner(getResource("world_borders.txt"));
        while (borders.hasNextLine()) {
            String line = borders.nextLine().trim();
            if (line.equals("")) {
                continue;
            }
            assert(countryLookup.containsKey(line));
            Country country = countryLookup.get(line);
            if (borders.hasNextLine()) {
                Vector<Point> border = new Vector<Point>();
                String[] pointTokens = borders.nextLine().split(" ");
                for (int i = 0; i < pointTokens.length; i+= 2) {
                    int x = Integer.parseInt(pointTokens[i]);
                    int y = Integer.parseInt(pointTokens[i + 1]);
                    border.add(new Point(x, y));
                }
                country.addBorder(border);
            }
        }
    }
    
    private void optimizeBorders() {
        for (Country c: countryLookup.values()) {
            int numPoints = 0;
            int maxx = Integer.MIN_VALUE;
            int minx = Integer.MAX_VALUE;
            int maxy = Integer.MIN_VALUE;
            int miny = Integer.MAX_VALUE;
            double avex = 0;
            double avey = 0;
            for (Vector<Point> border: c.getBorders()) {
                for (Point p: border) {
                    numPoints++;
                    int x = (int)p.getX();
                    int y = (int)p.getY();
                    avex += x;
                    avey += y;
                    maxx = Math.max(maxx, x);
                    maxy = Math.max(maxy, y);
                    minx = Math.min(minx, x);
                    miny = Math.min(miny, y);
                }
            }
            avex = avex / numPoints;
            avey = avey / numPoints;
            c.setCenter(new Point((int)avex, (int)avey));
            c.setExtent(Math.max((maxx - minx),(maxy-miny)));
        }
    }
    
    public Country findCountry(int x, int y) {
        for (Country c : countryLookup.values()) {
            int cx = (int) c.getCenter().getX();
            int cy = (int) c.getCenter().getY();
            double dist = Math.sqrt(Math.pow(cx - x, 2) + Math.pow(cy - y, 2));
            if (dist < c.getExtent()) {
                for (Vector<Point> border: c.getBorders()) {
                    int minx = Integer.MAX_VALUE;
                    int maxx = Integer.MIN_VALUE;
                    int miny = minx, maxy = maxx;
                    for (Point pi : border) {
                        if ((int) pi.getX() == x) {
                            miny = Math.min((int) pi.getY(), miny);
                            maxy = Math.max((int) pi.getY(), maxy);
                        }
                        if ((int) pi.getY() == y) {
                            minx = Math.min((int) pi.getX(), minx);
                            maxx = Math.max((int) pi.getX(), maxx);
                        }
                    }
                    if (minx < x && x < maxx && miny < y && y < maxy) {
                        return c;
                    }
                }
            }
        }
        return null;
    }
    
    public void setQuizRegions(Vector<String> regions) {
        Vector<Country> quizStack = new Vector<Country>();
        for (String region: regions) {
            for (Country country: regionLookup.get(region)) {
                if (quizStack.size() == 0) {
                    quizStack.add(country);
                } else {
                    quizStack.insertElementAt(country, (int)(Math.random() * quizStack.size() + 1));
                }
            }
        }
        this.quizStack = quizStack;
    }
    
    public Country getCurrentCountry() {
        return currentCountry;
    }
    
    public Vector<Country> getQuizStack() {
        return quizStack;
    }
    
    public void nextQuestion() {
        int index = (int)(Math.random() * this.quizStack.size());
        this.currentCountry = quizStack.remove(index);
    }
    
    private InputStream getResource(String path) throws IOException {
        URL resource = this.getClass().getClassLoader().getResource(path);
        if (resource != null) {
            return resource.openStream();
        } else {
            return new FileInputStream(new File(path));
        }
    }
    
}
