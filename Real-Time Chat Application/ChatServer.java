import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

public class ChatServer {
    static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(12345);
        System.out.println("Server started...");

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket);
            clients.add(handler);
            new Thread(handler).start();
        }
    }

    static void broadcast(String msg, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(msg);
                }
            }
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String name;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            name = in.readLine();

            ChatServer.broadcast("🔔 " + name + " has joined the chat.", this);

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("sendfile/")) {
                    String filePath = msg.substring(9).trim();
                    File file = new File(filePath);
                    if (file.exists()) {
                        sendFileToClients(file, name);
                    } else {
                        sendMessage("❌ File not found: " + filePath);
                    }
                } else if (msg.startsWith("DRAW ")) {
                    ChatServer.broadcast(msg, this);
                } else {
                    ChatServer.broadcast(name + ": " + msg, this);
                }
            }
        } catch (IOException e) {
            System.out.println(name + " disconnected.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ChatServer.clients.remove(this);
            ChatServer.broadcast("❌ " + name + " has left the chat.", this);
        }
    }

    void sendMessage(String msg) {
        out.println(msg);
    }

    void sendFileToClients(File file, String fromName) {
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            synchronized (ChatServer.clients) {
                for (ClientHandler client : ChatServer.clients) {
                    if (client != this) {
                        OutputStream os = client.socket.getOutputStream();
                        client.sendMessage("[FILE]" + file.getName());
                        os.write(fileBytes);
                        os.write("<<EOF>>".getBytes());
                        os.flush();
                        client.sendMessage("✅ " + fromName + " sent a file: " + file.getName());
                    }
                }
            }
            sendMessage("✅ File sent: " + file.getName());
        } catch (IOException ex) {
            sendMessage("❌ Error sending file: " + ex.getMessage());
        }
    }
}


