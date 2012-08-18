package io.whereisit.utils;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Vector;

public class ConflictResolver {
    
    
    
    public static void main(String[] argv) {
        try {
            Scanner regions = new Scanner(new File("regions.txt"));
            Vector<String> regionCountries = new Vector<String>();
            while (regions.hasNextLine()) {
                String line = regions.nextLine().trim();
                if (!line.startsWith("===") && !line.equals("")) {
                    regionCountries.add(line);
                }
            }
            
            Scanner borders = new Scanner(new File("world_borders.txt"));
            Vector<String> borderCountries = new Vector<String>();
            while (borders.hasNextLine()) {
                String line = borders.nextLine().trim();
                borderCountries.add(line);
                if (borders.hasNextLine()) {
                    borders.nextLine();
                }
            }
            
            Scanner countries = new Scanner(new File("countries_and_capitals.txt"));
            Vector<String> worldCountries = new Vector<String>();
            while (countries.hasNextLine()) {
                String[] tokens = countries.nextLine().split("\t");
                if (tokens.length > 1) {
                    worldCountries.add(tokens[1]);
                }
            }
            
            System.out.println("countries_and_capitals.txt");
            System.out.println("--------------------------");
            for (String c: worldCountries) {
                if (!borderCountries.contains(c) || !regionCountries.contains(c)) {
                    System.out.print(c + ": ");
                    if (!borderCountries.contains(c)) {
                        System.out.print("no borders, ");
                    }
                    if (!regionCountries.contains(c)) {
                        System.out.print("no region");
                    }
                    System.out.println();
                }
            }
            System.out.println("\n\n\n");

            System.out.println("regions.txt");
            System.out.println("--------------------------");
            for (String c: regionCountries) {
                if (!borderCountries.contains(c) || !worldCountries.contains(c)) {
                    System.out.print(c + ": ");
                    if (!borderCountries.contains(c)) {
                        System.out.print("no borders, ");
                    }
                    if (!worldCountries.contains(c)) {
                        System.out.print("not in master");
                    }
                    System.out.println();
                }
            }
            System.out.println("\n\n\n");
            
            System.out.println("world_borders.txt");
            System.out.println("--------------------------");
            for (String c: borderCountries) {
                if (!regionCountries.contains(c) || !worldCountries.contains(c)) {
                    System.out.print(c + ": ");
                    if (!regionCountries.contains(c)) {
                        System.out.print("no region, ");
                    }
                    if (!worldCountries.contains(c)) {
                        System.out.print("not in master");
                    }
                    System.out.println();
                }
            }
            System.out.println("\n\n\n");
            
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
