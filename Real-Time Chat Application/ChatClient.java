import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ChatClient {
    private static JTextArea chatArea;
    private static JTextField messageField;
    private static JButton sendButton;
    private static PrintWriter out;
    private static String userName;
    private static WhiteboardPanel whiteboard;
    private static Socket socket;

    public static void main(String[] args) throws IOException {
        socket = new Socket("localhost", 12345);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        userName = JOptionPane.showInputDialog("Enter your name");
        out.println(userName);

        JFrame frame = new JFrame("Chat & WhiteBoard - " + userName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);

        JSplitPane splitPane = new JSplitPane();
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Send");
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        // File sending button
        JButton sendFileButton = new JButton("Send File");
        chatPanel.add(sendFileButton, BorderLayout.NORTH);
        sendFileButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(null);
            if(result == JFileChooser.APPROVE_OPTION){
                File file = chooser.getSelectedFile();
                try {
                    out.println("sendfile/" + file.getAbsolutePath());
                    out.flush();
                } catch (Exception ex) {
                    chatArea.append("Error sending file.\n");
                }
            }
        });

        whiteboard = new WhiteboardPanel(out);
        splitPane.setLeftComponent(chatPanel);
        splitPane.setRightComponent(whiteboard);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.6);
        frame.add(splitPane);
        frame.setVisible(true);

        // Thread: receive & interpret incoming server messages
        new Thread(() -> {
            try {
                String line;
                InputStream input = socket.getInputStream();
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("[FILE]")) {
                        String fileName = line.substring(6).trim();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            String checkEnd = new String(buffer, 0, bytesRead);
                            if (checkEnd.contains("<<EOF>>")) {
                                int index = checkEnd.indexOf("<<EOF>>");
                                baos.write(buffer, 0, index);
                                break;
                            } else {
                                baos.write(buffer, 0, bytesRead);
                            }
                        }
                        Files.write(Paths.get("Received_" + fileName), baos.toByteArray());
                        chatArea.append("📁 Received file: " + fileName + "\n");
                    } else if (line.startsWith("DRAW ")) {
                        whiteboard.receiveDrawCommand(line);
                    } else {
                        chatArea.append(line + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
    }

    private static void sendMessage() {
        String msg = messageField.getText().trim();
        if (!msg.isEmpty()) {
            out.println(msg);
            chatArea.append("Me: " + msg + "\n");
            messageField.setText("");
        }
    }
}

