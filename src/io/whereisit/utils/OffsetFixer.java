package io.whereisit.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class OffsetFixer {

    public static void main(String[] argv) {
        int OFFSET = -446;
        try {
            Scanner scan = new Scanner(new File("countries_tofix.txt"));
            PrintWriter pr = new PrintWriter(new FileWriter("countries_fixed.txt"));
            while (scan.hasNextLine()) {
                String country = scan.nextLine();
                if (scan.hasNextLine()) {
                    pr.println(country);
                    String[] tokens = scan.nextLine().split(" ");
                    for (int i = 0; i < tokens.length; i+=2) {
                        pr.print(tokens[i] + " ");
                        int y = Integer.parseInt(tokens[i + 1]);
                        y += OFFSET;
                        pr.print(y + " ");
                    }
                    pr.println();
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
