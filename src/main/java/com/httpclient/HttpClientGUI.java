package main.java.com.httpclient;

import main.java.com.common.ApiKeyConfig;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// Swing GUI for interactive HTTP client – supports multiple verbs, headers, and bodies
public class HttpClientGUI extends JFrame {
    private final JComboBox<String> methodBox;
    private final JTextField urlField;
    private final JTextArea headersArea;
    private final JTextArea bodyArea;
    private final JTextArea responseArea;
    private final SimpleHttpClient client;

    public HttpClientGUI(String apiKey) {
        client = new SimpleHttpClient(apiKey);

        setTitle("HTTP Client GUI");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top: HTTP method selector and URL input
        JPanel topPanel = new JPanel(new BorderLayout());
        methodBox = new JComboBox<>(new String[]{"GET", "HEAD", "POST", "PUT", "DELETE"});
        urlField = new JTextField("http://localhost:8080/resources");
        topPanel.add(methodBox, BorderLayout.WEST);
        topPanel.add(urlField, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Center: split for request inputs and response view
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerSplit.setResizeWeight(0.3);

        // Request inputs: headers and body
        JPanel inputPanel = new JPanel(new GridLayout(1, 2));
        headersArea = new JTextArea(
                "Accept: */*\n" +
                        "User-Agent: HttpClientGUI/1.0\n" +
                        "Connection: close\n" +
                        "Content-Type: application/json\n\n" +
                        "// Add additional headers (Key:Value) below\n"
        );
        headersArea.setBorder(BorderFactory.createTitledBorder("Headers"));
        bodyArea = new JTextArea(
                "{\n" +
                        "  \"name\": \"example\",\n" +
                        "  \"age\": 25\n" +
                        "}\n"
        );
        bodyArea.setBorder(BorderFactory.createTitledBorder("Body"));
        inputPanel.add(new JScrollPane(headersArea));
        inputPanel.add(new JScrollPane(bodyArea));

        // Response area (read-only)
        responseArea = new JTextArea();
        responseArea.setEditable(false);
        responseArea.setBorder(BorderFactory.createTitledBorder("Response"));

        centerSplit.setTopComponent(inputPanel);
        centerSplit.setBottomComponent(new JScrollPane(responseArea));
        add(centerSplit, BorderLayout.CENTER);

        // Send button
        JButton sendButton = new JButton("Send Request");
        sendButton.addActionListener(this::sendRequest);
        add(sendButton, BorderLayout.SOUTH);
    }

    // Builds and sends request, then displays status, headers, and body
    private void sendRequest(ActionEvent event) {
        String method = ((String) methodBox.getSelectedItem()).trim();
        String url    = urlField.getText().trim();
        Map<String, String> headers = parseHeaders(headersArea.getText());
        String rawBody = bodyArea.getText();
        String body   = (rawBody == null || rawBody.isBlank()) ? null : rawBody;

        try {
            HttpResponse response = client.request(method, url, headers, body);

            String respBody = response.getBody() != null ? response.getBody() : "";
            String respHeaders = formatHeaders(response.getHeaders());

            responseArea.setText(
                    "Status: " + response.getStatusCode() + " " + response.getStatusMessage() + "\n\n" +
                            "Headers:\n" + respHeaders + "\n\n" +
                            "Body:\n" + respBody
            );
        } catch (Exception e) {
            responseArea.setText("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Parses multiline Key:Value headers into a map
    private Map<String, String> parseHeaders(String text) {
        Map<String, String> headers = new HashMap<>();
        for (String line : text.split("\\R")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            int idx = line.indexOf(":");
            if (idx > 0) {
                String key = line.substring(0, idx).trim();
                String val = line.substring(idx + 1).trim();
                headers.put(key, val);
            }
        }
        return headers;
    }

    // Formats response headers map into a displayable string
    private String formatHeaders(Map<String, String> headers) {
        return headers.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    public static void main(String[] args) {
        String apiKey = ApiKeyConfig.load(args, 0);
        SwingUtilities.invokeLater(() -> {
            HttpClientGUI gui = new HttpClientGUI(apiKey);
            gui.setVisible(true);
        });
    }
}