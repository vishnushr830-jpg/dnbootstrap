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
import java.util.Locale;

import java.io.IOException;

import git.artdeell.dnbootstrap.assets.AppDirs;
import git.artdeell.dnbootstrap.utils.DnbUtils;

public class ManageDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_data);
        findViewById(R.id.manage_space_uninstall_game).setOnClickListener(v->performAction(this::uninstallGame));
        findViewById(R.id.manage_space_uninstall_components).setOnClickListener(v->performAction(this::uninstallComponents));
        findViewById(R.id.manage_space_uninstall_everything).setOnClickListener(v->eraseAppData());
        findViewById(R.id.manage_space_open_data_directory).setOnClickListener(v->{
            DnbUtils.openPath(this, getFilesDir(), false);
        });

        SharedPreferences prefs = getSharedPreferences(git.artdeell.dnbootstrap.input.ControlLayout.PREFS_NAME, Context.MODE_PRIVATE);
        float sensitivity = prefs.getFloat(git.artdeell.dnbootstrap.input.ControlLayout.KEY_SENSITIVITY, git.artdeell.dnbootstrap.input.ControlLayout.DEFAULT_SENSITIVITY);
        SeekBar sensitivitySeekBar = findViewById(R.id.seekbar_sensitivity);
        TextView sensitivityValueLabel = findViewById(R.id.label_sensitivity_value);
        sensitivitySeekBar.setProgress(Math.round(sensitivity * 100f));
        updateSensitivityLabel(sensitivityValueLabel, sensitivity);
        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float newSensitivity = Math.max(0.01f, progress / 100f);
                prefs.edit().putFloat(git.artdeell.dnbootstrap.input.ControlLayout.KEY_SENSITIVITY, newSensitivity).apply();
                updateSensitivityLabel(sensitivityValueLabel, newSensitivity);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void updateSensitivityLabel(TextView label, float sensitivity) {
        label.setText(String.format(Locale.ENGLISH, "%.2fx", sensitivity));
    }

    private void performAction(UninstallAction action) {
        new Thread(()->{
            try {
                action.performAction();
                showDone();
            }catch (IOException e) {
                DnbUtils.showErrorDialog(this, e, false);
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
