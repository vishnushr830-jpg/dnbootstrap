package git.artdeell.dnbootstrap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LogViewerActivity extends AppCompatActivity {

    private static final String LOG_DIR = "home/.config/VintagestoryData/Logs";

    private TextView logText;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        logText = findViewById(R.id.log_text);
        scrollView = findViewById(R.id.log_scroll);

        Button refreshBtn = findViewById(R.id.btn_refresh);
        Button shareBtn = findViewById(R.id.btn_share_log);

        loadLog();

        refreshBtn.setOnClickListener(v -> loadLog());
        shareBtn.setOnClickListener(v -> shareLog());
    }

    private void loadLog() {
        File logDir = new File(getFilesDir(), LOG_DIR);

        if (!logDir.exists()) {
            logText.setText("No logs folder found yet.\nLaunch the game first.");
            return;
        }

        // Find the most recent .log file in the folder
        File[] logFiles = logDir.listFiles(
            (dir, name) -> name.endsWith(".log")
        );

        if (logFiles == null || logFiles.length == 0) {
            logText.setText("No log files found in:\n" + logDir.getAbsolutePath());
            return;
        }

        // Pick the most recently modified log file
        File latestLog = logFiles[0];
        for (File f : logFiles) {
            if (f.lastModified() > latestLog.lastModified()) {
                latestLog = f;
            }
        }

        // Read it
        StringBuilder sb = new StringBuilder();
        sb.append("File: ").append(latestLog.getName()).append("\n\n");

        try (BufferedReader reader = new BufferedReader(new FileReader(latestLog))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            sb.append("Error reading log: ").append(e.getMessage());
        }

        logText.setText(sb.toString());

        // Auto-scroll to bottom
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void shareLog() {
        String logContent = logText.getText().toString();
        if (logContent.isEmpty()) return;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Vintage Story Log");
        shareIntent.putExtra(Intent.EXTRA_TEXT, logContent);
        startActivity(Intent.createChooser(shareIntent, "Share Log"));
    }
}
