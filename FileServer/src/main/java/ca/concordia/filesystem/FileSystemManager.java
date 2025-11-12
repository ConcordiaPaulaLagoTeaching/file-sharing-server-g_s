package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private static FileSystemManager instance;
    private final RandomAccessFile disk;
    private ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) throws FileNotFoundException {
        // Initialize the file system manager with a file
        if (instance == null) {
            disk = new RandomAccessFile(filename, "rw");
            inodeTable = new FEntry[MAXFILES];
            for (int i = 0; i < MAXFILES; i++) {
                inodeTable[i] = new FEntry("", (short) 0, (short) -1);
            }
            // TODO Initialize the file system
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

    }

    public void createFile(String fileName) throws Exception {
        // TODO
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    public void deleteFile(String filenName) throws Exception {
        // Todo
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    public void writeFile(String fileName, byte[] contents) throws Exception {
        // Todo
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    public byte[] readFile(String fileName) throws Exception {
        // Todo
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    public String[] listFiles() {
        // Todo
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

}

// TODO: Add readFile, writeFile and other required methods,
