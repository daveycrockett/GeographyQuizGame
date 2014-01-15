package io.whereisit;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import javax.imageio.ImageIO;

public class WorldModel {

    private Hashtable<String, Country> countryLookup = new Hashtable<String, Country>();
    private Hashtable<String, Vector<Country>> regionLookup = new Hashtable<String, Vector<Country>>();
    private BufferedImage map;
    private Set<Country> quizStack = new HashSet<Country>();
    private ArrayList<String> quizRegions = new ArrayList<String>();
    private Country currentCountry = null;
    private Country mouseOverCountry;
    private CardStatus cardStatus;

    Random random = new Random(System.currentTimeMillis());

    public void setCardStatus(CardStatus cardStatus) {
        this.cardStatus = cardStatus;
    }

    boolean front = false;

    public Country getMouseOverCountry() {
        return mouseOverCountry;
    }

    public void setMouseOverCountry(Country mouseOverCountry) {
        this.mouseOverCountry = mouseOverCountry;
    }

    public enum CardStatus {
        FRONT,
        BACK,
        BOTH,
        NEITHER,
        RANDOM
    }

    public String getCurrentCardsDisplay() {
        if (getCurrentCountry() == null && mouseOverCountry == null) {
            return "";
        }
        String status = "";
        if (mouseOverCountry != null) {
            status += "In Blue: " + mouseOverCountry.getName() + "     ";
        }
        if (getCurrentCountry() != null) {
            String cardFront = getCurrentCountry().getCapitals().get(0);
            String cardBack = getCurrentCountry().getName();
            status += "Quiz ";
            String keyString = "";
            if (getCurrentCountry() != null) {
                keyString = " (in red)";
            } else {
                keyString = " (no border shown)";
            }
            if (cardStatus == CardStatus.NEITHER) {
                if (getCurrentCountry() == null) {
                    if (front) {
                        status += "Capital: " + cardFront;
                    } else {
                        status += "Province: " + cardBack;
                    }
                } else {
                    status += "Country" + keyString + ": ?";
                }
            } else if (cardStatus == CardStatus.BOTH) {
                status += "Region" + keyString + ": " + cardFront
                        + ", " + cardBack;
            } else {
                boolean front = (cardStatus == CardStatus.FRONT);
                if (cardStatus == CardStatus.RANDOM) {
                    front = random.nextBoolean();
                }
                if (front) {
                    status += "Capital City" + keyString + ": " + cardFront;
                } else {
                    status += "Region " + keyString + ": " + cardBack;
                }
            }
        }
        return status;
    }

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
        Scanner countries = new Scanner(getResource("countries_and_capitals.txt"), "UTF8");
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
        Scanner regions = new Scanner(getResource("regions.txt"), "UTF8");
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
        Scanner borders = new Scanner(getResource("world_borders.txt"), "UTF8");
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

    public Country getCurrentCountry() {
        return currentCountry;
    }

    public void nextQuestion() throws IllegalQuizStateException {
        if (quizStack.size() == 0) {
            if (quizRegions.size() == 0) {
                throw new IllegalQuizStateException();
            } else {
                for (String region: quizRegions) {
                    addToQuizStack(region);
                }
            }
        }

        int index = random.nextInt(quizStack.size());
        currentCountry = quizStack.toArray(new Country[0])[index];
        quizStack.remove(currentCountry);
    }

    public static InputStream getResource(String path) throws IOException {
        URL resource = WorldModel.class.getClassLoader().getResource(path);
        if (resource != null) {
            return resource.openStream();
        } else {
            return new FileInputStream(new File(path));
        }
    }

    public void addRegion(String region) {
        if (!quizRegions.contains(region) && regionLookup.containsKey(region)) {
            quizRegions.add(region);
            addToQuizStack(region);
        }
    }

    private void addToQuizStack(String region) {
        for (Country c: regionLookup.get(region)) {
            quizStack.add(c);
        }
    }

    public void removeRegion(String region) {
        if (quizRegions.contains(region)) {
            quizRegions.remove(region);
            if (regionLookup.containsKey(region)) {
                for (Country c: regionLookup.get(region)) {
                    quizStack.remove(c);
                }
            }
        }
    }

}
