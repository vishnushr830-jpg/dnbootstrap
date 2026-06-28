package git.artdeell.dnbootstrap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import git.artdeell.dnbootstrap.assets.AppDirs;
import git.artdeell.dnbootstrap.assets.AssetsExtractor;
import git.artdeell.dnbootstrap.io.IOUtil;

public class AssetsCheckActivity extends AppCompatActivity implements AssetsExtractor.ProgressCallback {
    private static AssetsExtractor extractorTask;
    private ProgressBar extractionProgress;
    private Button selectButton;
    private TextView assetsMessage;
    private ActivityResultLauncher<String> selectGameLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppDirs appDirs = new AppDirs(getFilesDir());

        if (appDirs.isFullyInstalled()) {
            showLaunchMenu();
            return;
        }

        setContentView(R.layout.activity_assets_check);
        extractionProgress = findViewById(R.id.extraction_progress);
        selectButton = findViewById(R.id.select_game_data_btn);
        assetsMessage = findViewById(R.id.assets_message);

        if (extractorTask != null) {
            connectExtractionTask();
        } else if (appDirs.isGameInstalled()) {
            componentsOnlyExtract();
        } else {
            selectGameLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), this::fullExtract);
            selectButton.setOnClickListener((v) -> selectGameLauncher.launch("application/gzip"));
        }
    }

    private void showLaunchMenu() {
        setContentView(R.layout.activity_launch_menu);

        Button startButton = findViewById(R.id.btn_start_game);
        Button settingsButton = findViewById(R.id.btn_settings);
        Button logsButton = findViewById(R.id.btn_view_logs);
        Button shaderButton = findViewById(R.id.btn_shader_debug);
        Button patchButton = findViewById(R.id.btn_patch_shader);

        startButton.setOnClickListener(v -> startGame());

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageDataActivity.class);
            startActivity(intent);
        });

        logsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogViewerActivity.class);
            startActivity(intent);
        });

        shaderButton.setOnClickListener(v -> showShaderInActivity());

        patchButton.setOnClickListener(v -> patchShader());
    }

    private void showShaderInActivity() {
        File shaderFile = new File(getFilesDir(),
            "vs/vintagestory/assets/game/shaders/woittest.fsh");

        StringBuilder sb = new StringBuilder();

        if (!shaderFile.exists()) {
            sb.append("Shader file not found at:\n")
              .append(shaderFile.getAbsolutePath());
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(shaderFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (Exception e) {
                sb.append("Error reading shader:\n").append(e.getMessage());
            }
        }

        Intent intent = new Intent(this, LogViewerActivity.class);
        intent.putExtra("custom_text", sb.toString());
        startActivity(intent);
    }

    private void patchShader() {
        File shaderFile = new File(getFilesDir(),
            "vs/vintagestory/assets/game/shaders/woittest.fsh");

        if (!shaderFile.exists()) {
            Toast.makeText(this, "Shader not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Read the shader
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(shaderFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            // Patch — replace desktop GL with OpenGL ES
            String patched = sb.toString()
                .replace("#version 330 core", "#version 300 es");

            // Write back
            try (FileWriter writer = new FileWriter(shaderFile)) {
                writer.write(patched);
            }

            Toast.makeText(this,
                "Shader patched successfully!\nLaunch the game now.",
                Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this,
                "Patch failed: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
            Log.e("ShaderPatch", "Error patching shader", e);
        }
    }

    private void startGame() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        if (extractorTask != null) {
            extractorTask.setCallback(null);
            extractorTask = null;
        }
    }

    private void fullExtract(Uri gameUri) {
        long gameSize = IOUtil.getFileSize(getContentResolver(), gameUri);
        extractorTask = new AssetsExtractor(getApplicationContext(), gameUri, gameSize);
        connectExtractionTask();
        new Thread(extractorTask).start();
    }

    private void componentsOnlyExtract() {
        extractorTask = new AssetsExtractor(getApplicationContext(), null, 0);
        connectExtractionTask();
        new Thread(extractorTask).start();
    }

    private void connectExtractionTask() {
        assetsMessage.setText(R.string.extract_runtime);
        selectButton.setVisibility(View.GONE);
        extractionProgress.setIndeterminate(!extractorTask.progressAvailable());
        onProgressChanged();
        extractorTask.setCallback(this);
    }

    private void exit() {
        startGame();
    }

    @Override
    public void onProgressChanged() {
        runOnUiThread(() -> {
            extractionProgress.setProgress(
                (int) (extractorTask.getProgress() * extractionProgress.getMax()));
            if (extractorTask.progressComplete()) exit();
        });
    }
}
