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
        Button patchOitButton = findViewById(R.id.btn_patch_oit);
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

        patchOitButton.setOnClickListener(v -> patchOIT());

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
        StringBuilder sb = new StringBuilder();

        File shaderDir = new File(getFilesDir(),
            "vs/vintagestory/assets/game/shaders");
        File includeDir = new File(getFilesDir(),
            "vs/vintagestory/assets/game/shaderincludes");

        String[] shaderNames = {
            "woittest.fsh",
            "chunkliquid.fsh"
        };

        String[] includeNames = {
            "oit.fsh"
        };

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

        for (String name : includeNames) {
            File shader = new File(includeDir, name);
            sb.append("=== [include] ").append(name).append(" ===\n");
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

    private void patchOIT() {
        File oitFile = new File(getFilesDir(),
            "vs/vintagestory/assets/game/shaderincludes/oit.fsh");

        if (!oitFile.exists()) {
            Toast.makeText(this, "oit.fsh not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        String patchedOIT =
            "#if USEOIT > 0\n" +
            "#define OIT_BINS 3\n" +
            "#define OIT_BIN_SCALE 30.0\n" +
            "\n" +
            "layout(location = 0) out vec4 OITreveal;\n" +
            "layout(location = 1) out vec4 outReveal;\n" +
            "layout(location = 2) out vec4 outGlow;\n" +
            "layout(location = 3) out vec4 OITaccumulation0;\n" +
            "layout(location = 4) out vec4 OITaccumulation1;\n" +
            "layout(location = 5) out vec4 OITaccumulation2;\n" +
            "\n" +
            "void OITaccumulate(int bin, vec4 x){\n" +
            "    switch(bin){\n" +
            "        case 0:  OITaccumulation0 = x; return;\n" +
            "        case 1:  OITaccumulation1 = x; return;\n" +
            "        default: OITaccumulation2 = x;\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "float OITbellcurve(float t){\n" +
            "    float n = t / 0.832;\n" +
            "    return exp(-n * n);\n" +
            "}\n" +
            "\n" +
            "float OITweight(float t, float a){\n" +
            "    return exp(-t / 100.0);\n" +
            "}\n" +
            "\n" +
            "void OIT(vec4 colour, float glow, float depth){\n" +
            "    depth /= OIT_BIN_SCALE;\n" +
            "    float bin = log(depth + 1.0);\n" +
            "    float w = OITweight(depth, colour.a);\n" +
            "    colour.rgb *= colour.a;\n" +
            "    for(int i = 0; i < OIT_BINS; i++){\n" +
            "        float b = OITbellcurve(bin - float(i));\n" +
            "        if(i == (OIT_BINS-1) && bin > float(OIT_BINS-1)) b = 1.0;\n" +
            "        OITaccumulate(i, colour * w * b);\n" +
            "        OITreveal[i] = 1.0 - colour.a * b;\n" +
            "    }\n" +
            "    outReveal = vec4(1.0 - colour.a);\n" +
            "    outGlow = vec4(glow, 0.0, 0.0, colour.a);\n" +
            "}\n" +
            "\n" +
            "void OIT(vec4 colour, float glow){\n" +
            "    float depth = (gl_FragCoord.z * 2.0 - 1.0) / gl_FragCoord.w;\n" +
            "    OIT(colour, glow, depth);\n" +
            "}\n" +
            "\n" +
            "#else\n" +
            "\n" +
            "// Fallback when USEOIT is disabled\n" +
            "layout(location = 0) out vec4 outAccu;\n" +
            "layout(location = 1) out vec4 outReveal;\n" +
            "layout(location = 2) out vec4 outGlow;\n" +
            "\n" +
            "void OIT(vec4 colour, float glow){\n" +
            "    float alpha = colour.a;\n" +
            "    float z = gl_FragCoord.z;\n" +
            "    float weight = max(0.01, min(3000.0, 0.03 / (0.00001 + pow(z / 200.0, 4.0))));\n" +
            "    outAccu = vec4(colour.rgb * alpha, alpha) * weight;\n" +
            "    outReveal = vec4(alpha, 0.0, 0.0, 1.0);\n" +
            "    outGlow = vec4(glow, 0.0, 0.0, alpha);\n" +
            "}\n" +
            "\n" +
            "void OIT(vec4 colour, float glow, float depth){\n" +
            "    OIT(colour, glow);\n" +
            "}\n" +
            "\n" +
            "#endif\n";

        try (FileWriter writer = new FileWriter(oitFile)) {
            writer.write(patchedOIT);
            Toast.makeText(this,
                "OIT shader patched!\nLaunch game now.",
                Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this,
                "OIT patch failed: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
            Log.e("ShaderPatch", "Error patching oit.fsh", e);
        }
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
            "\"transparentRenderPass\": false",
            "\"transparentRenderPass\": true");

        patchFile(configFile,
            "\"chunkVerticesUploadRateLimiter\": 3",
            "\"chunkVerticesUploadRateLimiter\": 1");

        patchFile(configFile,
            "\"modelDataPoolMaxVertexSize\": 500000",
            "\"modelDataPoolMaxVertexSize\": 200000");

        patchFile(configFile,
            "\"modelDataPoolMaxIndexSize\": 750000",
            "\"modelDataPoolMaxIndexSize\": 300000");

        patchFile(configFile,
            "\"modelDataPoolMaxParts\": 1500",
            "\"modelDataPoolMaxParts\": 600");

        Toast.makeText(this,
            "Config patched!\nLaunch game now.",
            Toast.LENGTH_LONG).show();
    }

    private void patchSpecificShaders() {
        File shaderDir = new File(getFilesDir(),
            "vs/vintagestory/assets/game/shaders");

        String[] shadersToFix = {
            "particlesquad.fsh",
            "woittest.fsh",
            "chunkliquid.fsh",
            "chunktransparent.fsh",
            "blockhighlights.fsh",
            "particlesquad2d.fsh",
            "aurora.fsh",
            "cloudvolumetric.fsh",
            "entityanimated.fsh"
        };

        patchFile(new File(shaderDir, "chunkopaque.fsh"),
            "#extension GL_ARB_explicit_attrib_location: enable\n",
            "");

        for (String shaderName : shadersToFix) {
            File shader = new File(shaderDir, shaderName);
            if (!shader.exists()) continue;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(shader))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (Exception e) {
                Log.e("ShaderPatch", "Failed to read: " + shaderName, e);
                continue;
            }

            String content = sb.toString();

            content = content
                .replace("outReveal.r = alpha;",
                    "outReveal = vec4(alpha, 0.0, 0.0, 1.0);")
                .replace("outReveal.r = color.a;",
                    "outReveal = vec4(color.a, 0.0, 0.0, 1.0);")
                .replace("outReveal.r = rgba.a;",
                    "outReveal = vec4(rgba.a, 0.0, 0.0, 1.0);")
                .replace("outReveal.r = texColor.a;",
                    "outReveal = vec4(texColor.a, 0.0, 0.0, 1.0);")
                .replace("outReveal.r = finalColor.a;",
                    "outReveal = vec4(finalColor.a, 0.0, 0.0, 1.0);");

            try (FileWriter writer = new FileWriter(shader)) {
                writer.write(content);
                Log.d("ShaderPatch", "Patched: " + shaderName);
            } catch (Exception e) {
                Log.e("ShaderPatch", "Failed to write: " + shaderName, e);
            }
        }

        Toast.makeText(this,
            "All failing shaders patched!\nLaunch game now.",
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
