import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.*;
import javax.swing.text.*;

public class SocketServer extends JFrame implements ActionListener {
    JTextPane textPane = new JTextPane();
    JScrollPane scrollPane = new JScrollPane(textPane);
    JTextField inputText = new JTextField();
    JPanel bottomPanel = new JPanel(new BorderLayout());

    ServerSocket server;
    Socket socket;
    InetAddress addr;
    ArrayList<ServerThread> clientList = new ArrayList<>();
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public SocketServer() {
        super("Server Chat Room");
        setLayout(new BorderLayout());

        textPane.setEditable(false);
        textPane.setFont(new Font("SansSerif", Font.PLAIN, 14));

        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(scrollPane, BorderLayout.CENTER);

        inputText.setToolTipText("Enter your message (supports emojis)");
        inputText.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputText.setBackground(new Color(230, 230, 250));
        bottomPanel.add(inputText, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        inputText.addActionListener(this);

        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        startServer();
    }

    private void startServer() {
        try {
            addr = InetAddress.getByName("0.0.0.0");
            server = new ServerSocket(1234, 50, addr);
            appendToPane("Waiting for client connections...\n", Color.BLUE, true, false);

            while (true) {
                try {
                    socket = server.accept();
                    appendToPane("Client connected: " + socket.getInetAddress() + "\n", Color.GREEN, true, false);

                    // Create a new thread for each client connection
                    ServerThread st = new ServerThread(this, socket);
                    addClientThread(st);
                    st.start();
                } catch (IOException e) {
                    appendToPane("Error accepting client connection: " + e.getMessage() + "\n", Color.RED, true, false);
                }
            }
        } catch (UnknownHostException e) {
            appendToPane("Error: Invalid server address - " + e.getMessage() + "\n", Color.RED, true, false);
        } catch (IOException e) {
            appendToPane("Error starting server: " + e.getMessage() + "\n", Color.RED, true, false);
        } catch (IllegalArgumentException e) {
            appendToPane("Error: Invalid port number - " + e.getMessage() + "\n", Color.RED, true, false);
        } catch (SecurityException e) {
            appendToPane("Error: Security exception - " + e.getMessage() + "\n", Color.RED, true, false);
        }
    }

    public void addClientThread(ServerThread st) {
        clientList.add(st);
    }

    public void removeClientThread(ServerThread st) {
        clientList.remove(st);
    }

    public void broadcast(String message) {
        for (ServerThread st : clientList) {
            st.pw.println(message);
        }
    }

    public void appendToPane(String msg, Color color, boolean bold, boolean isServer) {
        StyledDocument doc = textPane.getStyledDocument();
        Style style = textPane.addStyle("Style", null);
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, bold);

        String timestamp = timeFormat.format(new Date());
        String fullMsg = (isServer ? "[Server] " : "") + timestamp + " " + msg;

        try {
            doc.insertString(doc.getLength(), fullMsg, style);
            doc.insertString(doc.getLength(), "\n", style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        textPane.setCaretPosition(doc.getLength());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String message = inputText.getText();
        message = formatMessage(message);
        appendToPane("[Server]: " + message + "\n", Color.BLUE, true, true);
        broadcast("[Server]: " + message);
        inputText.setText("");
    }

    public String formatMessage(String message) {
        message = message.replace(":)", "😊");
        message = message.replace(":(", "☹");
        message = message.replace("<3", "❤");
        return message;
    }

    public static void main(String[] args) {
        new SocketServer();
    }
}

class ServerThread extends Thread {
    SocketServer server;
    Socket clientSocket;
    PrintWriter pw;
    BufferedReader br;
    String name;
    Color userColor;

    public ServerThread(SocketServer server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
        this.userColor = new Color((int) (Math.random() * 0x1000000));
    }

    @Override
    public void run() {
        try {
            br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            pw = new PrintWriter(clientSocket.getOutputStream(), true);

            name = br.readLine();
            if (name == null) {
                throw new IOException("Client disconnected before sending a name.");
            }

            server.broadcast("[" + name + "] Entered the chat.");
            server.appendToPane("[" + name + "] has entered the chat\n", userColor, true, false);

            String data;
            while ((data = br.readLine()) != null) {
                data = server.formatMessage(data);
                server.broadcast("[" + name + "] " + data);
                server.appendToPane("[" + name + "]: " + data + "\n", userColor, false, false);
            }
        } catch (IOException e) {
            server.appendToPane("Error with client [" + (name != null ? name : "unknown") + "]: " + e.getMessage() + "\n", Color.RED, true, false);
        } finally {
            server.removeClientThread(this);
            if (name != null) {
                server.broadcast("[" + name + "] Left the chat.");
                server.appendToPane("[" + name + "] has left the chat\n", Color.ORANGE, true, false);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                server.appendToPane("Error closing client socket: " + e.getMessage() + "\n", Color.RED, true, false);
            }
        }
    }
}