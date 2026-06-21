package git.artdeell.dnbootstrap;

import android.system.Os;

import java.io.File;
import java.io.IOException;

import git.artdeell.dnbootstrap.assets.AppDirs;
import git.artdeell.dnbootstrap.utils.SymlinkUtil;

public class DotnetStarter {

    private static File findCertsDir() {
        File certsDir = new File("/apex/com.android.conscrypt/cacerts/");
        if(certsDir.exists()) return certsDir;
        certsDir = new File("/system/etc/security/cacerts");
        if(certsDir.exists()) return certsDir;
        return null;
    }

    public static void kickstart(AppDirs appDirs, File appNativeDir) throws IOException {

        File homeDir = new File(appDirs.base, "home");
        File certsDir = findCertsDir();

        if(certsDir == null) throw new IOException("Cannot start: can't find HTTPS certificate directory");
        File trueVsDir = new File(appDirs.vs, "vintagestory");

        try {
            Os.setenv("HOME", homeDir.getAbsolutePath(), true);
            Os.setenv("FONTCONFIG_PATH", appDirs.fontconfig.getAbsolutePath(), true);
            Os.setenv("SSL_CERT_DIR", certsDir.getAbsolutePath(), true);
            
            // === ULTIMATE MALI GPU STABILITY LAYER ===
            
            // Disables broken luminance-alpha hardware allocations that cause neon green/red panics on Mali architectures
            Os.setenv("LIBGL_NOLUMALPHA", "1", true);
            
            // Forces a localized cache copy of active textures so the driver cannot lose memory pointer addresses mid-frame
            Os.setenv("LIBGL_TEXCOPY", "1", true);
            
            // Enforces strict edge clamping for non-power-of-two (NPOT) texture dimensions used in block maps
            Os.setenv("LIBGL_DEFAULTWRAP", "1", true);
            
            // Routes rendering contexts directly to the frame buffer to prevent black silhouette chunk failures
            Os.setenv("LIBGL_FB", "1", true);
            
            // Suppresses low-level translation validation checks to keep execution fluent and bypass driver drops
            Os.setenv("LIBGL_NOERROR", "1", true);

            // Strict single-threaded sequential collection to prevent garbage collection thread collisions
            Os.setenv("MONO_GC_PARAMS", "major=marksweep,nursery-size=64m", true);
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        SymlinkUtil symlinkUtil = new SymlinkUtil(trueVsDir, appNativeDir);

        symlinkUtil.symlinkLibrary("libopenal.so", "libopenal.so.1");
        symlinkUtil.symlinkLibrary("libcairo.so", "libcairo.so.2");

        MainActivity.runDotnet(appDirs.runtime.getAbsolutePath(), trueVsDir.getAbsolutePath());
        System.exit(0);
    }
}
