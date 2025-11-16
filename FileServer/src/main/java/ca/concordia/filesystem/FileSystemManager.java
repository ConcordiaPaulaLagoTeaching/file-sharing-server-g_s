package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private static FileSystemManager instance;
    private final RandomAccessFile disk;
    private ReentrantLock globalLock = new ReentrantLock();
    private final ReadWriteLock rw = new ReentrantReadWriteLock(true);
    private static final int BLOCK_SIZE = 128; // Example block size
    private byte[] zeroBlock = new byte[BLOCK_SIZE];

    private FEntry[] entriesTable; // Array of inodes
    private FNode[] blocksTable;
    private boolean[] freeBlockList; // Bitmap for free blocks

    public static synchronized void init(String fileName, int totalSize) throws IOException {
        if (instance == null) {
           instance =  new FileSystemManager(fileName, totalSize);
        }
    }

    public static FileSystemManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FileSystemManager has not been initialized. Call init() first.");
        }
        return instance;
    }

    public FileSystemManager(String fileName, int totalSize) throws IOException {
        // Initialize the file system manager with a file
        if (instance == null) {
            disk = new RandomAccessFile(fileName, "rw");
            byte[] emptyEntry = new byte[15];
            entriesTable = new FEntry[MAXFILES];
            blocksTable = new FNode[MAXBLOCKS];
            freeBlockList = new boolean[MAXBLOCKS];

            for (int i = 0; i < MAXFILES; i++) {
                long offset = (long) i * emptyEntry.length;
                disk.seek(offset);
                disk.write(emptyEntry, 0, emptyEntry.length);
                entriesTable[i] = new FEntry("", (short) 0, (short) -1);
            }

            for (int i = 0; i < MAXBLOCKS; i++) {
                int indexOffset = 15 * MAXFILES + i * 4;
                int indexLinkedNode = indexOffset + 2;
                short index = (short) i;
                byte[] indexBuffer = ByteBuffer.allocate(2).putShort(index).array();
                byte[] linkedNodeBuffer = ByteBuffer.allocate(2).putShort((short) -1).array();
                byte[] emptyNameBuffer = "".getBytes();
                disk.seek(indexOffset);
                disk.write(emptyNameBuffer, 0, emptyNameBuffer.length);
                disk.seek(indexOffset);
                disk.write(indexBuffer, 0, indexBuffer.length);
                disk.seek(indexLinkedNode);
                disk.write(linkedNodeBuffer, 0, linkedNodeBuffer.length);

                blocksTable[i] = new FNode((short) i);
                if (i == 0) {
                    freeBlockList[i] = false;
                } else {
                    freeBlockList[i] = true;
                }
            }

        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

        instance = this;

    }

    public void createFile(String fileName) throws Exception {
        boolean freeFileFound = false;
        short indexFile = 0;
        short firstNode = -1;
        for (int i = 0; i < MAXFILES; i++) {
            if (entriesTable[i].getFilename().equals("")) {
                freeFileFound = true;
                indexFile = (short) i;
                break;
            }
        }
        if (freeFileFound == true) {
            for (int j = 0; j < MAXBLOCKS; j++) {
                if (freeBlockList[j] == true && firstNode == (short) -1) {
                    firstNode = (short) j;
                    freeBlockList[j] = false;
                }
            }

        }

        if (freeFileFound == false) {
            System.out.println("No free file, you must delete one before creating one.");
            return;
        } else if (firstNode == (short) -1) {
            System.out.println("No space available");
            return;
        }
        entriesTable[indexFile].setFilename(fileName);
        entriesTable[indexFile].setFilesize((short) 0);
        entriesTable[indexFile].setFirstBlock(firstNode);
        disk.seek(indexFile * 15);
        disk.write(fileName.getBytes(), 0, fileName.getBytes().length);
        disk.seek(indexFile * 15 + 11);
        disk.write(ByteBuffer.allocate(2).putShort((short) 0).array(), 0, 2);
        disk.seek(indexFile * 15 + 13);
        disk.write(ByteBuffer.allocate(2).putShort(firstNode).array(), 0, 2);
    }

    public void deleteFile(String fileName) throws Exception {
        
        boolean fileFound = false;
        short indexFile = 0;
        short firstNodeIndex = -1;
        short currentNodeIndex;
        short nextNodeIndex;
        boolean isNextNode = false;
        for (int i = 0; i < MAXFILES; i++) {
            if (entriesTable[i].getFilename().equals(fileName)) {
                fileFound = true;
                indexFile = (short) i;
                firstNodeIndex = entriesTable[i].getFirstBlock();
                break;
            }
        }
        if (fileFound) {
            entriesTable[indexFile].setFilename("");
            entriesTable[indexFile].setFilesize((short) 0);
            entriesTable[indexFile].setFirstBlock((short) -1);
            disk.seek(indexFile * 15);
            disk.write(entriesTable[indexFile].getFilename().getBytes(), 0,
                    entriesTable[indexFile].getFilename().getBytes().length);
            disk.seek(indexFile * 15 + 11);
            disk.write(ByteBuffer.allocate(2).putShort((short) 0).array(), 0, 2);
            disk.seek(indexFile * 15 + 13);
            disk.write(ByteBuffer.allocate(2).putShort((short) -1).array(), 0, 2);
            currentNodeIndex = firstNodeIndex;

            while (currentNodeIndex != -1) {

                disk.seek(blocksTable[currentNodeIndex].getBlockIndex() * BLOCK_SIZE + BLOCK_SIZE);
                disk.write(zeroBlock);

                freeBlockList[blocksTable[currentNodeIndex].getBlockIndex()] = true;


                nextNodeIndex = (short) blocksTable[currentNodeIndex].getNext();

                blocksTable[currentNodeIndex].setNext((short) -1);


                currentNodeIndex = nextNodeIndex;
            }

        } else {
            System.out.println("File not Found.");
        }
    }

public void writeFile(String fileName, byte[] contents) throws Exception {

    short entryIndex = -1;
    for (int i = 0; i < MAXFILES; i++) {
        if (entriesTable[i].getFilename().equals(fileName)) {
            entryIndex = (short) i;
            break;
        }
    }

    if (entryIndex == -1) {
        System.out.println("File not found.");
        return;
    }


    short oldNode = entriesTable[entryIndex].getFirstBlock();
    int oldBlocks = 0;

    short tempNode = oldNode;
    while (tempNode != -1) {
        oldBlocks++;
        tempNode = (short) blocksTable[tempNode].getNext();
    }


    int fileSize = contents.length;
    int blocksNeeded = (fileSize + BLOCK_SIZE - 1) / BLOCK_SIZE;

    int freeBlocks = 0;
    for (int i = 0; i < MAXBLOCKS; i++) {
        if (freeBlockList[i]) {
            freeBlocks++;
        }
    }


    int totalAvailable = oldBlocks + freeBlocks;

    if (totalAvailable < blocksNeeded) {
        System.out.println("Not enough space for write operation.");
        return;
    }


    short current = oldNode;
    while (current != -1) {

        short blockIndex = (short) blocksTable[current].getBlockIndex();
        short nextNode = (short) blocksTable[current].getNext();

        // erase data (128 bytes)
        disk.seek(BLOCK_SIZE + blockIndex * BLOCK_SIZE);
        disk.write(zeroBlock);

        freeBlockList[blockIndex] = true;


        blocksTable[current].setNext((short) -1);

        current = nextNode;
    }


    short firstBlock = -1;
    short prevNode = -1;

    int copied = 0;
    int needed = blocksNeeded;

    for (int i = 0; i < MAXBLOCKS && needed > 0; i++) {

        if (freeBlockList[i]) {

            freeBlockList[i] = false; // mark used
            short thisNode = (short) i;

            if (firstBlock == -1) {
                firstBlock = thisNode;
            } else {
                blocksTable[prevNode].setNext(thisNode);
            }

    
            short blockIndex = (short) blocksTable[thisNode].getBlockIndex();
            long offset = BLOCK_SIZE + blockIndex * BLOCK_SIZE;

            disk.seek(offset);

            int chunk = Math.min(BLOCK_SIZE, fileSize - copied);
            disk.write(contents, copied, chunk);

            if (chunk < BLOCK_SIZE) {
                disk.write(new byte[BLOCK_SIZE - chunk]);
            }

            copied += chunk;
            prevNode = thisNode;
            needed--;
        }
    }


    if (prevNode != -1) {
        blocksTable[prevNode].setNext((short) -1);
    }


    entriesTable[entryIndex].setFilesize((short) fileSize);
    entriesTable[entryIndex].setFirstBlock(firstBlock);

    byte[] nameBytes = new byte[11];
    byte[] raw = entriesTable[entryIndex].getFilename().getBytes();
    System.arraycopy(raw, 0, nameBytes, 0, raw.length);

    long inodeOffset = entryIndex * 15;

    disk.seek(inodeOffset);
    disk.write(nameBytes);

    disk.write(ByteBuffer.allocate(2).putShort((short) fileSize).array());
    disk.write(ByteBuffer.allocate(2).putShort(firstBlock).array());

    System.out.println("Write complete.");
}


    public byte[] readFile(String fileName) throws Exception {

        short entryIndex = -1;
        for (int i = 0; i < MAXFILES; i++) {
            if (entriesTable[i].getFilename().equals(fileName)) {
                entryIndex = (short) i;
                break;
            }
            
        }

        if (entryIndex == -1) {
            System.out.println("File not found.");
            return null;
        }

        short fileSize = entriesTable[entryIndex].getFilesize();
        short firstBlockIndex = entriesTable[entryIndex].getFirstBlock();

        if (fileSize == 0 || firstBlockIndex == -1) {
            return new byte[0];
        }

        byte[] output = new byte[fileSize];
        int copied = 0;

        short currentNodeIndex = firstBlockIndex;

        while (currentNodeIndex != -1 && copied < fileSize) {

            short blockIndex = (short) blocksTable[currentNodeIndex].getBlockIndex();

            long offset = BLOCK_SIZE + blockIndex * BLOCK_SIZE;
            disk.seek(offset);

            byte[] temp = new byte[BLOCK_SIZE];
            disk.read(temp);

            int bytesToCopy = Math.min(BLOCK_SIZE, fileSize - copied);

            System.arraycopy(temp, 0, output, copied, bytesToCopy);
            copied += bytesToCopy;

            currentNodeIndex = (short) blocksTable[currentNodeIndex].getNext();
        }

        return output;
    }

    public String[] listFiles() {
        
        System.out.println("List of files:\n");

        int filesCount = 0;
        for (int i = 0; i < MAXFILES; i++) {
            if (!entriesTable[i].getFilename().equals("")) {
                filesCount++;
            }
        }

        String[] filesList = new String[filesCount];
        int index = 0;

        for (int i = 0; i < MAXFILES; i++) {
            if (!entriesTable[i].getFilename().equals("")) {
                String fileName = entriesTable[i].getFilename();
                System.out.println(fileName);
                filesList[index++] = fileName;
            }
        }

        return filesList;
       
}
}

