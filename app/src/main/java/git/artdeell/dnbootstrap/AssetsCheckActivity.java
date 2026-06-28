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
        Button transShaderButton = findViewById(R.id.btn_show_transparency_shaders);
        Button patchConfigButton = findViewById(R.id.btn_patch_config);
        Button patchSpecificButton = findViewById(R.id.btn_patch_specific);
        Button patchButton = findViewById(R.id.btn_patch_shader);
        Button patchAllButton = findViewById(R.id.btn_patch_all_shaders);
        Button restoreButton = findViewById(R.id.btn_restore_shaders);

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

        transShaderButton.setOnClickListener(v -> showTransparencyShaders());

        patchConfigButton.setOnClickListener(v -> patchClientSettings());

        patchSpecificButton.setOnClickListener(v -> patchSpecificShaders());

        patchButton.setOnClickListener(v -> patchShader());

        patchAllButton.setOnClickListener(v -> patchAllShaders());

        restoreButton.setOnClickListener(v -> restoreShaders());
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

    private void showTransparencyShaders() {
        String[] shaderNames = {
            "woittest.fsh",
            "transparentliquid.fsh",
            "transparent.fsh",
            "chunkliquid.fsh",
            "chunkopaque.fsh"
        };

        StringBuilder sb = new StringBuilder();
        File shaderDir = new File(getFilesDir(),
            "vs/vintagestory/assets/game/shaders");

        for (String name : shaderNames) {
            File shader = new File(shaderDir, name);
            sb.append("=== ").append(name).append(" ===\n");
            if (!shader.exists()) {
                sb.append("NOT FOUND\n\n");
                continue;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(shader))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (Exception e) {
                sb.append("Error: ").append(e.getMessage()).append("\n");
            }
            sb.append("\n\n");
        }

        Intent intent = new Intent(this, LogViewerActivity.class);
        intent.putExtra("custom_text", sb.toString());
        startActivity(intent);
    }

    private void patchClientSettings() {
        File configFile = new File(getFilesDir(),
            "home/.config/VintagestoryData/clientsettings.json");

        if (!configFile.exists()) {
            Toast.makeText(this,
                "Config not found!\nLaunch game first.",
                Toast.LENGTH_SHORT).show();
            return;
        }

        patchFile(configFile,
            "\"transparentRenderPass\": true",
            "\"transparentRenderPass\": false");

        Toast.makeText(this,
            "Config patched!\nTransparent render pass disabled.\nLaunch game now.",
            Toast.LENGTH_LONG).show();
    }

    private void patchSpecificShaders() {
        File shaderDir = new File(getFilesDir(),
            "vs/vintagestory/assets/game/shaders");

        // Fix 1: chunkopaque.fsh — remove unsupported extension
        patchFile(new File(shaderDir, "chunkopaque.fsh"),
            "#extension GL_ARB_explicit_attrib_location: enable\n",
            "");

        // Fix 2: woittest.fsh — fix outReveal assignment
        patchFile(new File(shaderDir, "woittest.fsh"),
            "outReveal.r = alpha;",
            "outReveal = vec4(alpha, 0.0, 0.0, 1.0);");

        Toast.makeText(this,
            "Specific shaders patched!\nLaunch game now.",
            Toast.LENGTH_LONG).show();
    }

    private void patchFile(File file, String from, String to) {
        if (!file.exists()) return;
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String patched = sb.toString().replace(from, to);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(patched);
            }
            Log.d("ShaderPatch", "Patched: " + file.getName());
        } catch (Exception e) {
            Log.e("ShaderPatch", "Failed: " + file.getName(), e);
        }
    }

    private void patchShader() {
        File shaderFile = new File(getFilesDir(),
            "vs/vintagestory/assets/game/shaders/woittest.fsh");

        if (!shaderFile.exists()) {
            Toast.makeText(this, "Shader not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        String patchedShader =
            "#version 330 core\n" +
            "\n" +
            "in vec4 v_color;\n" +
            "\n" +
            "out vec4 outAccu;\n" +
            "out vec4 outReveal;\n" +
            "\n" +
            "void main() {\n" +
            "    float alpha = v_color.a;\n" +
            "    float z = gl_FragCoord.z;\n" +
            "    float weight = max(0.01, min(3000.0, 0.03 / (0.00001 + pow(z / 200.0, 4.0))));\n" +
            "    outAccu = vec4(v_color.rgb * alpha, alpha) * weight;\n" +
            "    outReveal = vec4(alpha, 0.0, 0.0, 1.0);\n" +
            "}\n";

        try (FileWriter writer = new FileWriter(shaderFile)) {
            writer.write(patchedShader);
            Toast.makeText(this,
                "Shader patched! Launch game now.",
                Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this,
                "Patch failed: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
            Log.e("ShaderPatch", "Error patching shader", e);
        }
    }

    private void patchAllShaders() {
        File shaderDir = new File(getFilesDir(),
            "vs/vintagestory/assets/game/shaders");

        if (!shaderDir.exists()) {
            Toast.makeText(this, "Shader folder not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        File[] shaderFiles = shaderDir.listFiles();
        if (shaderFiles == null) {
            Toast.makeText(this, "No shaders found!", Toast.LENGTH_SHORT).show();
            return;
        }

        int patched = 0;
        int failed = 0;

        for (File shader : shaderFiles) {
            boolean isFsh = shader.getName().endsWith(".fsh");
            boolean isVsh = shader.getName().endsWith(".vsh");
            if (!isFsh && !isVsh) continue;

            try {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(shader))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }

                String content = sb.toString();
                if (!content.contains("#version 330 core")) continue;

                String patchedContent;
                if (isFsh) {
                    patchedContent = content.replace(
                        "#version 330 core",
                        "#version 300 es\n" +
                        "precision highp float;\n" +
                        "precision highp int;\n" +
                        "precision highp sampler2D;\n" +
                        "precision highp sampler2DArray;\n" +
                        "precision highp sampler3D;\n" +
                        "precision highp samplerCube;\n" +
                        "precision highp sampler2DShadow;"
                    );
                } else {
                    patchedContent = content.replace(
                        "#version 330 core",
                        "#version 300 es"
                    );
                }

                try (FileWriter writer = new FileWriter(shader)) {
                    writer.write(patchedContent);
                }

                patched++;

            } catch (Exception e) {
                Log.e("ShaderPatch", "Failed: " + shader.getName(), e);
                failed++;
            }
        }

        Toast.makeText(this,
            "Patched " + patched + " shaders!\n" +
            "Failed: " + failed + "\n" +
            "Launch game now.",
            Toast.LENGTH_LONG).show();
    }

    private void restoreShaders() {
        File vsDir = new File(getFilesDir(), "vs");
        File installedMarker = new File(vsDir, ".installed");
        if (installedMarker.exists()) {
            installedMarker.delete();
        }

        Toast.makeText(this,
            "Marked for reinstall.\nRestarting app...",
            Toast.LENGTH_LONG).show();

        finish();
        startActivity(getIntent());
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
