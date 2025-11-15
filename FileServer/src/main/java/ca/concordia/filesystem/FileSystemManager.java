package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
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

    public FileSystemManager(String fileName, int totalSize) throws IOException {
        // Initialize the file system manager with a file
        if (instance == null) {
            disk = new RandomAccessFile(fileName, "rw");
            byte[] emptyEntry = new byte[15];

            for (int i = 0; i < MAXFILES; i++){
                long offset = (long) i * emptyEntry.length;
            disk.seek(offset);
            disk.write(emptyEntry, 0, emptyEntry.length);           
            }

            for (int i = 0; i < MAXBLOCKS; i++){
                int indexOffset =  15 * MAXFILES + i * 4;
                int indexLinkedNode = indexOffset + 2;
                short index = (short)i;
                byte[] indexBuffer = ByteBuffer.allocate(2).putShort(index).array();
                byte[] linkedNodeBuffer = ByteBuffer.allocate(2).putShort((short) -1).array();
                disk.seek(indexOffset);
                disk.write(indexBuffer, 0, indexBuffer.length);
                disk.seek(indexLinkedNode);
                disk.write(linkedNodeBuffer, 0, linkedNodeBuffer.length);
            }
         
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

    }

    public void createFile(String fileName) throws Exception {
        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] == null) {
                inodeTable[i] = new FEntry(fileName, (short) 0, (short) -1);
                break;
            }
        }
    }

    public void deleteFile(String fileName) throws Exception {
        boolean fileFound = false;
        int blocksAmount = 0;

        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] != null && inodeTable[i].getFilename().equals(fileName)) {
                fileFound = true;
                if (inodeTable[i].getFilesize() % BLOCK_SIZE == 0) {
                    blocksAmount = inodeTable[i].getFilesize() / BLOCK_SIZE;
                } else {
                    blocksAmount = (inodeTable[i].getFilesize() / BLOCK_SIZE) + 1;
                }
                int firstBlock = inodeTable[i].getFirstBlock();
                int lastBlock = firstBlock + blocksAmount; // exclusive upper bound
                for (int j = firstBlock; j < lastBlock; j++) {
                    if (j < MAXBLOCKS)
                        freeBlockList[j] = true;
                }

                inodeTable[i] = null;
                break;

            }
        }

        if (!fileFound) {
            System.out.println("No file found");
        }

    }

    public void writeFile(String fileName, byte[] contents) throws Exception {

        int fileSize = contents.length;
        int blocksNeeded = 0;
        int firstBlock = -1;
        int blocksAvailableCount = 0;

        if (fileSize % BLOCK_SIZE == 0) {
            blocksNeeded = fileSize / BLOCK_SIZE;
        } else {
            blocksNeeded = (fileSize / BLOCK_SIZE) + 1;
        }

        for (int i = 0; i < MAXBLOCKS; i++) {
            if (freeBlockList[i] == true) {
                blocksAvailableCount += 1;
                if (firstBlock == -1) {
                    firstBlock = i;
                }
            }
            if (blocksAvailableCount == blocksNeeded) {
                break;
            }
        }

        if (firstBlock != -1 && (blocksAvailableCount == blocksNeeded)) {
            for (int i = 0; i < MAXFILES; i++) {
                if (inodeTable[i] != null && fileName.equals(inodeTable[i].getFilename())) {
                    inodeTable[i] = new FEntry(fileName, (short) fileSize, (short) 0);
                }
            }
        } else {
            System.out.println("Not enough space available available");
        }

    }

    public byte[] readFile(String fileName) throws Exception {
        // Todo
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    public String[] listFiles() {
        System.out.println("List of files:\n");

        int filesCount = 0;
        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] != null) {
                filesCount++;
            }
        }

        String[] filesList = new String[filesCount];
        int index = 0;

        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] != null) {
                String fileName = inodeTable[i].getFilename();
                System.out.println(fileName);
                filesList[index++] = fileName;
            }
        }

        return filesList;
    }

}
