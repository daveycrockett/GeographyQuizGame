package io.whereisit;

import java.awt.Point;
import java.util.Vector;

public class Country {

    private String name;
    private Vector<String> capitals;
    private String region;
    private Vector<Vector<Point>> borders;
    private Point center;
    private double extent;
    
    public String getName() {
        return name;
    }

    public Vector<String> getCapitals() {
        return capitals;
    }

    public Vector<Vector<Point>> getBorders() {
        return borders;
    }
    
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
    
    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public double getExtent() {
        return extent;
    }

    public void setExtent(double extent) {
        this.extent = extent;
    }
    
    public void addCapital(String capital) {
        capitals.add(capital);
    }

    public void addBorder(Vector<Point> border) {
        borders.add(border);
    }
    
    public Country() {
        capitals = new Vector<String>();
        borders = new Vector<Vector<Point>>();
    }
    
    public Country(String name) {
        this();
        this.name = name;
    }
    
    public Country(String name, String region) {
        this(name);
        this.region = region;
    }
    
    
    
}
