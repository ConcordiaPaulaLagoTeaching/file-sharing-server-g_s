package ca.concordia;

import java.io.FileNotFoundException;

import ca.concordia.server.FileServer;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        System.out.printf("Hello and welcome!");

        FileServer server = new FileServer(12345, "filesystem.dat", 10 * 128);
        // Start the file server
        server.start();
    }
}