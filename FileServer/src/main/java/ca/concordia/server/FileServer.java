package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileServer {

    private final int port;
    private final FileSystemManager fsManager;

    public FileServer(int port, String fileSystemName, int totalSize) {
        this.port = port;
        try {
            FileSystemManager.init(fileSystemName, totalSize);
            this.fsManager = FileSystemManager.getInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize FileSystemManager", e);
        }
    }

    
    public void start() {
        
        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("File server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from " + clientSocket.getRemoteSocketAddress());
                
                executor.execute(new ClientHandler(clientSocket, fsManager));
            }
        } catch (IOException e) {
            System.err.println("Failed to start server on port " + port);
            e.printStackTrace();
        }
      
    }

    
    private static class ClientHandler implements Runnable {

        private final Socket socket;
        private final FileSystemManager fsManager;

        ClientHandler(Socket socket, FileSystemManager fsManager) {
            this.socket = socket;
            this.fsManager = fsManager;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        out.println("ERROR: Unknown command.");
                        continue;
                    }

                    System.out.println("[" + socket.getRemoteSocketAddress() + "] " + line);
                    String[] tokens = line.split("\\s+", 3);
                    String cmd = tokens[0].toUpperCase();

                    try {
                        if ("CREATE".equals(cmd)) {
                            handleCreate(tokens, out);
                        } else if ("LIST".equals(cmd)) {
                            handleList(out);
                        } else if ("WRITE".equals(cmd)) {
                            handleWrite(tokens, out);
                        } else if ("READ".equals(cmd)) {
                            handleRead(tokens, out);
                        } else if ("DELETE".equals(cmd)) {
                            handleDelete(tokens, out);
                        } else if ("QUIT".equals(cmd)) {
                            out.println("SUCCESS: Disconnecting.");
                            break;
                        } else {
                            out.println("ERROR: Unknown command.");
                        }
                    } catch (Exception e) {
                        out.println("ERROR: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("I/O error with client " + socket);
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }


        private void handleCreate(String[] tokens, PrintWriter out) throws Exception {
            if (tokens.length < 2) {
                out.println("ERROR: missing filename");
                return;
            }
            String filename = tokens[1];
            fsManager.createFile(filename);
            out.println("SUCCESS: File '" + filename + "' created.");
        }

        private void handleList(PrintWriter out) {
            String[] files = fsManager.listFiles();
            if (files == null || files.length == 0) {
                out.println("(empty)");
                return;
            }
            for (String name : files) {
                out.println(name);
            }
        }

        private void handleWrite(String[] tokens, PrintWriter out) throws Exception {
            if (tokens.length < 3) {
                out.println("ERROR: missing payload");
                return;
            }
            String filename = tokens[1];
            String encoded = tokens[2];

            try {
                byte[] payload = Base64.getDecoder().decode(encoded);
                fsManager.writeFile(filename, payload);
                out.println("SUCCESS: Wrote " + payload.length + " bytes to '" + filename + "'.");
            } catch (IllegalArgumentException ex) {
                out.println("ERROR: invalid base64 payload");
            }
        }

        private void handleRead(String[] tokens, PrintWriter out) throws Exception {
            if (tokens.length < 2) {
                out.println("ERROR: missing filename");
                return;
            }
            String filename = tokens[1];
            byte[] data = fsManager.readFile(filename);
            String encoded = Base64.getEncoder().encodeToString(data);
            out.println(encoded);
        }

        private void handleDelete(String[] tokens, PrintWriter out) throws Exception {
            if (tokens.length < 2) {
                out.println("ERROR: missing filename");
                return;
            }
            String filename = tokens[1];
            fsManager.deleteFile(filename);
            out.println("SUCCESS: File '" + filename + "' deleted.");
        }
    }
}