import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainGUI extends JFrame {
    private JTextField urlField;
    private JComboBox<String> qualityCombo;
    private JTextArea logArea;
    private JButton downloadButton;
    private ExecutorService executorService;
    private Path outputDir;

    public MainGUI() {
        super("YT-DLP GUI");
        executorService = Executors.newSingleThreadExecutor();
        setupUI();
        setupOutputDirectory();
    }

    private void setupUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // URL input
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("URL:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        urlField = new JTextField(30);
        inputPanel.add(urlField, gbc);

        // Quality selector
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("Quality:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        qualityCombo = new JComboBox<>(new String[]{
                "Best",
                "4K (2160p)",
                "1440p",
                "1080p",
                "720p",
                "480p",
                "360p",
                "240p",
                "144p"
        });
        inputPanel.add(qualityCombo, gbc);

        // Download button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> startDownload());
        inputPanel.add(downloadButton, gbc);

        add(inputPanel, BorderLayout.NORTH);

        // Log area
        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        // Pack and center
        pack();
        setLocationRelativeTo(null);
    }

    private void setupOutputDirectory() {
        outputDir = Paths.get("output");
        try {
            Files.createDirectories(outputDir);
            log("Output directory created: " + outputDir.toAbsolutePath());
        } catch (IOException e) {
            log("Error creating output directory: " + e.getMessage());
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private String findYtDlpPath() {
        // Check current directory
        Path currentDir = Paths.get("yt-dlp.exe");
        if (Files.exists(currentDir)) {
            return "./yt-dlp.exe";
        }

        // Check bin directory
        Path binDir = Paths.get("bin", "yt-dlp.exe");
        if (Files.exists(binDir)) {
            return binDir.toString();
        }

        // Check PATH
        try {
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "--version");
            Process p = pb.start();
            if (p.waitFor() == 0) {
                return "yt-dlp";
            }
        } catch (IOException | InterruptedException e) {
            // Ignore and continue searching
        }

        return null;
    }

    private String getQualityFormat(int selectedIndex) {
        return switch(selectedIndex) {
            case 0 -> "best"; // Best
            case 1 -> "bestvideo[height<=2160]+bestaudio/best[height<=2160]"; // 4K
            case 2 -> "bestvideo[height<=1440]+bestaudio/best[height<=1440]"; // 1440p
            case 3 -> "bestvideo[height<=1080]+bestaudio/best[height<=1080]"; // 1080p
            case 4 -> "bestvideo[height<=720]+bestaudio/best[height<=720]"; // 720p
            case 5 -> "bestvideo[height<=480]+bestaudio/best[height<=480]"; // 480p
            case 6 -> "bestvideo[height<=360]+bestaudio/best[height<=360]"; // 360p
            case 7 -> "bestvideo[height<=240]+bestaudio/best[height<=240]"; // 240p
            case 8 -> "bestvideo[height<=144]+bestaudio/best[height<=144]"; // 144p
            default -> "best";
        };
    }

    private void startDownload() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a URL");
            return;
        }

        String ytDlpPath = findYtDlpPath();
        if (ytDlpPath == null) {
            JOptionPane.showMessageDialog(this,
                    "yt-dlp not found in:\n" +
                            "- Current directory\n" +
                            "- bin directory\n" +
                            "- System PATH");
            return;
        }

        downloadButton.setEnabled(false);
        executorService.submit(() -> {
            try {
                String quality = getQualityFormat(qualityCombo.getSelectedIndex());

                List<String> command = new ArrayList<>();
                command.add(ytDlpPath);
                command.add("-f");
                command.add(quality);
                command.add("-o");
                command.add(outputDir.resolve("%(title)s.%(ext)s").toString());
                command.add(url);

                log("Starting download with command: " + String.join(" ", command));
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log(line);
                    }
                }

                int exitCode = process.waitFor();
                log("Download " + (exitCode == 0 ? "completed successfully" : "failed"));
            } catch (Exception e) {
                log("Error: " + e.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> downloadButton.setEnabled(true));
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // the usual attempt to use native OS GUI style
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MainGUI().setVisible(true);
        });
    }
}