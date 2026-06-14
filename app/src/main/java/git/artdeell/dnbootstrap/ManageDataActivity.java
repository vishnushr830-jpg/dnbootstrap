package git.artdeell.dnbootstrap;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.FileUtils;

import java.io.IOException;

import git.artdeell.dnbootstrap.assets.AppDirs;
import git.artdeell.dnbootstrap.input.ControlLayout;
import git.artdeell.dnbootstrap.utils.Utils;

public class ManageDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_data);
        findViewById(R.id.manage_space_uninstall_game).setOnClickListener(v->performAction(this::uninstallGame));
        findViewById(R.id.manage_space_uninstall_components).setOnClickListener(v->performAction(this::uninstallComponents));
        findViewById(R.id.manage_space_uninstall_everything).setOnClickListener(v->eraseAppData());
        findViewById(R.id.manage_space_open_data_directory).setOnClickListener(v->{
            Utils.openPath(this, getFilesDir(), false);
        });

        // --- Mouse Sensitivity Slider ---
        SeekBar sensitivitySeekBar = findViewById(R.id.seekbar_sensitivity);
        TextView sensitivityLabel = findViewById(R.id.label_sensitivity_value);

        SharedPreferences prefs = getSharedPreferences(ControlLayout.PREFS_NAME, Context.MODE_PRIVATE);
        float currentSensitivity = prefs.getFloat(ControlLayout.KEY_SENSITIVITY, ControlLayout.DEFAULT_SENSITIVITY);

        // Slider range: 0-200 representing 0.1x to 3.0x sensitivity
        int progress = sensitivityToProgress(currentSensitivity);
        sensitivitySeekBar.setMax(200);
        sensitivitySeekBar.setProgress(progress);
        sensitivityLabel.setText(String.format("%.1fx", currentSensitivity));

        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float sensitivity = progressToSensitivity(progress);
                sensitivityLabel.setText(String.format("%.1fx", sensitivity));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float sensitivity = progressToSensitivity(seekBar.getProgress());
                prefs.edit().putFloat(ControlLayout.KEY_SENSITIVITY, sensitivity).apply();
                Toast.makeText(ManageDataActivity.this, "Sensitivity saved!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Convert progress (0-200) to sensitivity (0.1 - 3.0)
    private float progressToSensitivity(int progress) {
        return 0.1f + (progress / 200.0f) * 2.9f;
    }

    // Convert sensitivity (0.1 - 3.0) to progress (0-200)
    private int sensitivityToProgress(float sensitivity) {
        return Math.round((sensitivity - 0.1f) / 2.9f * 200);
    }

    private void performAction(UninstallAction action) {
        new Thread(()->{
            try {
                action.performAction();
                showDone();
            }catch (IOException e) {
                Utils.showErrorDialog(this, e, false);
            }
        }).start();
    }

    private void uninstallGame() throws IOException {
        FileUtils.deleteDirectory(new AppDirs(getFilesDir()).vs);
    }

    private void uninstallComponents() throws IOException {
        AppDirs appDirs = new AppDirs(getFilesDir());
        FileUtils.deleteDirectory(appDirs.vs);
        FileUtils.deleteDirectory(appDirs.runtime);
        FileUtils.deleteDirectory(appDirs.fontconfig);
    }

    private void eraseAppData() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.clearApplicationUserData();
    }

    private void showDone() {
        runOnUiThread(()-> Toast.makeText(this, R.string.manage_data_done, Toast.LENGTH_SHORT).show());
    }

    private interface UninstallAction {
        void performAction() throws IOException;
    }
}
