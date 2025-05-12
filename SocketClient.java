import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketClient extends JFrame implements ActionListener, Runnable {
    JTextPane textPane = new JTextPane();
    JScrollPane jp = new JScrollPane(textPane);
    JTextField inputText = new JTextField();
    JButton emojiButton = new JButton("😊");  // Emoji picker button
    JPanel bottomPanel = new JPanel(new BorderLayout());
    Socket sk;
    BufferedReader br;
    PrintWriter pw;
    String name;

    public SocketClient() {
        super("Client Chat");
        setLayout(new BorderLayout());

        textPane.setEditable(false);
        textPane.setFont(new Font("SansSerif", Font.PLAIN, 14));

        jp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(jp, BorderLayout.CENTER);

        bottomPanel.add(inputText, BorderLayout.CENTER);
        bottomPanel.add(emojiButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        inputText.setToolTipText("Enter your Message (supports emojis)");
        inputText.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputText.setBackground(new Color(230, 230, 250));

        inputText.addActionListener(this);
        emojiButton.addActionListener(e -> openEmojiPicker());

        setSize(600, 500);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        inputText.requestFocus();
    }

    public void serverConnection() {
        try {
            String IP = JOptionPane.showInputDialog(this, "Please enter server IP.", JOptionPane.INFORMATION_MESSAGE);
            if (IP == null || IP.isEmpty()) {
                appendToPane("Connection cancelled by user.\n", Color.RED, false);
                return;
            }

            sk = new Socket(IP, 1234);

            name = JOptionPane.showInputDialog(this, "Please enter a nickname", JOptionPane.INFORMATION_MESSAGE);
            if (name == null || name.isEmpty()) {
                appendToPane("You must enter a nickname to connect.\n", Color.RED, false);
                return;
            }

            br = new BufferedReader(new InputStreamReader(sk.getInputStream()));
            pw = new PrintWriter(sk.getOutputStream(), true);
            pw.println(name);  // Send username to server

            appendToPane("Connected to server.\n", Color.GREEN, false);
            new Thread(this).start();  // Start the thread to listen for incoming messages

        } catch (UnknownHostException e) {
            appendToPane("Error: Unknown host - " + e.getMessage() + "\n", Color.RED, false);
        } catch (IOException e) {
            appendToPane("Error connecting to server: " + e.getMessage() + "\n", Color.RED, false);
        }
    }

    private void appendToPane(String msg, Color c, boolean bold) {
        StyledDocument doc = textPane.getStyledDocument();
        Style style = textPane.addStyle("I'm a Style", null);
        StyleConstants.setForeground(style, c);
        StyleConstants.setBold(style, bold);
        try {
            doc.insertString(doc.getLength(), msg, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        textPane.setCaretPosition(doc.getLength());
    }

    private void openEmojiPicker() {
        JDialog emojiDialog = new JDialog(this, "Pick an Emoji", true);
        emojiDialog.setLayout(new GridLayout(3, 5));

        String[] emojis = {"😊", "😂", "😍", "😎", "👍", "💯", "🎉", "🔥", "💖", "😢", "🤔", "🙄", "👏", "🎶", "🐱"};

        for (String emoji : emojis) {
            JButton emojiButton = new JButton(emoji);
            emojiButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
            emojiButton.addActionListener(e -> {
                inputText.setText(inputText.getText() + emoji);  // Append emoji to input
                emojiDialog.dispose();
            });
            emojiDialog.add(emojiButton);
        }

        emojiDialog.setSize(300, 200);
        emojiDialog.setLocationRelativeTo(this);
        emojiDialog.setVisible(true);
    }

    @Override
    public void run() {
        String data;
        try {
            while ((data = br.readLine()) != null) {
                appendToPane(data + "\n", Color.BLACK, false);
            }
        } catch (IOException e) {
            appendToPane("Connection to server lost: " + e.getMessage() + "\n", Color.RED, false);
        } finally {
            closeConnection();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String data = inputText.getText();
        if (!data.isEmpty()) {
            pw.println(data);
            inputText.setText("");  // Clear the input field after sending
        }
    }

    private void closeConnection() {
        try {
            if (br != null) br.close();
            if (pw != null) pw.close();
            if (sk != null) sk.close();
            appendToPane("Disconnected from server.\n", Color.ORANGE, false);
        } catch (IOException e) {
            appendToPane("Error closing connection: " + e.getMessage() + "\n", Color.RED, false);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SocketClient().serverConnection());
    }
}