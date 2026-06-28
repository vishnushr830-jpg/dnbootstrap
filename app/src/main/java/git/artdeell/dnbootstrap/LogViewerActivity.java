package git.artdeell.dnbootstrap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LogViewerActivity extends AppCompatActivity {

    private static final String LOG_DIR = "home/.config/VintagestoryData/Logs";
    private static final String TAG = "LogViewer";

    private TextView logText;
    private ScrollView scrollView;
    private Button refreshBtn;
    private Button shareBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        // Fix status bar overlap properly
        View rootView = findViewById(R.id.root_layout);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        try {
            logText = findViewById(R.id.log_text);
            scrollView = findViewById(R.id.log_scroll);
            refreshBtn = findViewById(R.id.btn_refresh);
            shareBtn = findViewById(R.id.btn_share_log);

            loadLog();

            if (refreshBtn != null) refreshBtn.setOnClickListener(v -> loadLog());
            if (shareBtn != null) shareBtn.setOnClickListener(v -> shareLog());

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    private void loadLog() {
        File logDir = new File(getFilesDir(), LOG_DIR);

        if (!logDir.exists()) {
            logText.setText("No logs folder found yet.\nLaunch the game first.\n\nPath: "
                + logDir.getAbsolutePath());
            return;
        }

        File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));

        if (logFiles == null || logFiles.length == 0) {
            logText.setText("No log files found in:\n" + logDir.getAbsolutePath());
            return;
        }

        File latestLog = logFiles[0];
        for (File f : logFiles) {
            if (f.lastModified() > latestLog.lastModified()) {
                latestLog = f;
            }
        }

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

        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
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
