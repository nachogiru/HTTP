package main.java.com.httpclient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * Simple Swing‑based GUI wrapper around SimpleHttpClient.
 * Inline comments only, no Javadoc – per instructions.
 */
public class HttpClientGUI extends JFrame {

    // --- UI components ---
    private final JComboBox<String> methodBox;   // HTTP method dropdown
    private final JTextField urlField;           // request URL
    private final JTextArea headersArea;         // raw header input
    private final JTextArea bodyArea;            // request body input
    private final JTextArea responseArea;        // response display

    // HTTP client reused between requests (handles cookies internally)
    private final SimpleHttpClient client = new SimpleHttpClient(null);

    public HttpClientGUI() {
        // basic window setup
        setTitle("HTTP Client GUI");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // === Top (method + URL) ===
        JPanel topPanel = new JPanel(new BorderLayout());
        methodBox = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE"});
        urlField  = new JTextField("http://localhost:8080/resources");
        topPanel.add(methodBox, BorderLayout.WEST);
        topPanel.add(urlField,  BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // === Center split: inputs vs. response ===
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerSplit.setResizeWeight(0.4); // 40% top, 60% bottom

        // left‑side = headers; right‑side = body
        JPanel inputPanel = new JPanel(new GridLayout(1, 2));

        headersArea = new JTextArea("Content-Type: application/json\n" +
                "Accept: application/json\n" +
                "User-Agent: HttpClientGUI/1.0\n");
        headersArea.setBorder(BorderFactory.createTitledBorder("Headers"));

        bodyArea = new JTextArea("{\n  \"nombre\": \"ejemplo\",\n  \"edad\": 25\n}");
        bodyArea.setBorder(BorderFactory.createTitledBorder("Body"));

        inputPanel.add(new JScrollPane(headersArea));
        inputPanel.add(new JScrollPane(bodyArea));

        // response viewer (read‑only)
        responseArea = new JTextArea();
        responseArea.setEditable(false);
        responseArea.setBorder(BorderFactory.createTitledBorder("Response"));

        centerSplit.setTopComponent(inputPanel);
        centerSplit.setBottomComponent(new JScrollPane(responseArea));
        add(centerSplit, BorderLayout.CENTER);

        // === Send button at bottom ===
        JButton sendButton = new JButton("Send Request");
        sendButton.addActionListener(this::sendRequest);
        add(sendButton, BorderLayout.SOUTH);
    }

    // Fired when user clicks "Send Request"
    private void sendRequest(ActionEvent event) {
        try {
            // gather inputs
            String method = (String) methodBox.getSelectedItem();
            String url    = urlField.getText().trim();
            String body   = bodyArea.getText().trim();
            Map<String,String> headers = parseHeaders(headersArea.getText());

            // perform request
            HttpResponse response = client.request(method, url, headers, body);

            // render response to text area
            responseArea.setText(
                    "Status: " + response.getStatusCode() + " " + response.getStatusMessage() + "\n\n" +
                            "Headers:\n" + formatHeaders(response.getHeaders()) + "\n\n" +
                            "Body:\n" + response.getBody()
            );
        } catch (Exception e) {
            responseArea.setText("ERROR: " + e.getMessage());
        }
    }

    // Parse multiline "Key: Value" input into map
    private Map<String,String> parseHeaders(String text) {
        Map<String,String> headers = new HashMap<>();
        for (String line : text.split("\n")) {
            int idx = line.indexOf(":");
            if (idx != -1) {
                String key = line.substring(0, idx).trim();
                String val = line.substring(idx + 1).trim();
                headers.put(key, val);
            }
        }
        return headers;
    }

    // Convert map back to multiline string for display
    private String formatHeaders(Map<String,String> headers) {
        return headers.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    public static void main(String[] args) {
        // Launch on Swing event dispatch thread
        SwingUtilities.invokeLater(() -> {
            HttpClientGUI gui = new HttpClientGUI();
            gui.setVisible(true);
        });
    }
}
