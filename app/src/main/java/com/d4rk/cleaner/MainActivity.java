package com.d4rk.cleaner;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.d4rk.cleaner.clipboard.ClipboardActivity;
import com.d4rk.cleaner.databinding.ActivityMainBinding;
import com.d4rk.cleaner.invalid.ui.InvalidActivity;
import com.google.android.material.navigation.NavigationView;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Objects;
public class MainActivity extends AppCompatActivity {
    static boolean running = false;
    static SharedPreferences prefs;
    DrawerLayout drawerLayout;
    ActionBarDrawerToggle actionBarDrawerToggle;
    NavigationView navigationView;
    final Context context = this;
    public ActivityMainBinding binding;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.cleanButton.setOnClickListener(this::clean);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        WhitelistActivity.getWhiteList();
        setUpToolbar();
        navigationView = findViewById(R.id.navigation_view);
        @SuppressLint("RestrictedApi") ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, "atm_shortcut")
                .setShortLabel(getString(R.string.atmegame))
                .setLongLabel(getString(R.string.long_shortcut_atmegame))
                .setIcon(IconCompat.createFromIcon(Icon.createWithResource(context, R.mipmap.ic_launch_atmegame)))
                .setIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.atmegame.com/?utm_source=D4Cleaner&utm_medium=D4Cleaner")))
                .build();
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut);
        navigationView.setNavigationItemSelectedListener(MenuItem -> {
            int id = MenuItem.getItemId();
            if (id == R.id.nav_drawer_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
            if (id == R.id.nav_drawer_whitelist) {
                startActivity(new Intent(MainActivity.this, WhitelistActivity.class));
            }
            if (id == R.id.nav_drawer_clipboard_cleaner) {
                startActivity(new Intent(MainActivity.this, ClipboardActivity.class));
            }
            if (id == R.id.nav_drawer_invalid_media_cleaner) {
                startActivity(new Intent(MainActivity.this, InvalidActivity.class));
            }
            if (id == R.id.nav_drawer_about) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
            if (id == R.id.nav_drawer_atmegame) {
                Intent openURL = new Intent(Intent.ACTION_VIEW);
                openURL.setData(Uri.parse("https://www.atmegame.com/?utm_source=D4Cleaner&utm_medium=D4Cleaner"));
                startActivity(openURL);
            }
            if (id == R.id.nav_drawer_support) {
                Intent openURL = new Intent(Intent.ACTION_VIEW);
                openURL.setData(Uri.parse("https://www.paypal.me/d4rkmichaeltutorials"));
                startActivity(openURL);
            }
            if (id == R.id.nav_drawer_share) {
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    String shareBody = "https://play.google.com/store/apps/details?id=com.d4rk.cleaner";
                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Try right now!");
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                    startActivity(Intent.createChooser(sharingIntent, "Share using..."));
            }
            return false;
        });
    }
    /**
     * Runs search and delete on background thread
     */
    public final void clean(View view) {
        requestWriteExternalPermission();
        if (!running) {
            if (!prefs.getBoolean("one_click", false))
                new AlertDialog.Builder(this,R.style.MyAlertDialogTheme)
                        .setTitle(R.string.main_select_task)
                        .setMessage(R.string.main_select_task_description)
                        .setPositiveButton(R.string.main_clean, (dialog, whichButton) -> new Thread(()-> scan(true)).start())
                        .setNegativeButton(R.string.main_analyze, (dialog, whichButton) -> new Thread(()-> scan(false)).start()).show();
            else new Thread(()-> scan(true)).start();
        }
    }
    public final void link(View view) {
        Intent openURL = new Intent(android.content.Intent.ACTION_VIEW);
        openURL.setData(Uri.parse("https://www.atmegame.com/?utm_source=D4Cleaner&utm_medium=D4Cleaner"));
        startActivity(openURL);
    }
    public final void adflylink(View view) {
        Intent openURL = new Intent(android.content.Intent.ACTION_VIEW);
        openURL.setData(Uri.parse("http://anthargo.com/9cUM"));
        startActivity(openURL);
    }
    public void setUpToolbar() {
        drawerLayout = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(null);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }
    public void animateBtn() {
        binding.topSpacer.setVisibility(View.GONE);
        binding.fileScrollView.setVisibility(View.VISIBLE);
    }
    private void scan(boolean delete) {
        Looper.prepare();
        running = true;
        runOnUiThread(()->findViewById(R.id.cleanButton).setEnabled(!running));
        reset();
        File path = Environment.getExternalStorageDirectory();
        FileScanner fs = new FileScanner(path, this)
                .setEmptyDir(prefs.getBoolean("empty", false))
                .setAutoWhite(prefs.getBoolean("auto_white", true))
                .setDelete(delete)
                .setCorpse(prefs.getBoolean("corpse", false))
                .setGUI(binding)
                .setContext(this)
                .setUpFilters(
                        prefs.getBoolean("generic", true),
                        prefs.getBoolean("aggressive", false),
                        prefs.getBoolean("true_aggressive", false),
                        prefs.getBoolean("apk", false));
        if (path.listFiles() == null) {
            TextView textView = printTextView(printTextView(), Color.RED);
            runOnUiThread(() -> binding.fileListView.addView(textView));
        }
        runOnUiThread(() -> {
            animateBtn();
            binding.statusTextView.setText(getString(R.string.main_status_running));
            binding.scanProgress.setProgress(binding.scanProgress.getMax());
            TextView textView = binding.frameLayout.findViewById(R.id.scanTextView);
            textView.setText("100%");
        });
        long kilobytesTotal = fs.startScan();
        runOnUiThread(() -> {
            if (delete) {
                binding.statusTextView.setText(getString(R.string.main_freed) + " " + convertSize(kilobytesTotal));
            } else {
                binding.statusTextView.setText(getString(R.string.main_found) + " " + convertSize(kilobytesTotal));
            }
        });
        binding.fileScrollView.post(() -> binding.fileScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        running = false;
        runOnUiThread(()->findViewById(R.id.cleanButton).setEnabled(!running));
        Looper.loop();
    }
    private String printTextView() {
        return null;
    }
    /**
     * Convenience method to quickly create a textview
     * @param text - text of textview
     * @return - created textview
     */
    private synchronized TextView printTextView(String text, int color) {
        TextView textView = new TextView(MainActivity.this);
        textView.setTextColor(color);
        textView.setText(text);
        textView.setPadding(3,3,3,3);
        return textView;
    }
    private String convertSize(long length) {
        final DecimalFormat format = new DecimalFormat("#.##");
        final long MiB = 1024 * 1024;
        final long KiB = 1024;
        if (length > MiB) {
            return format.format(length / MiB) + " MB";
        }
        if (length > KiB) {
            return format.format(length / KiB) + " KB";
        }
        return format.format(length) + " B";
    }
    public synchronized TextView displayPath(File file) {
        TextView textView = printTextView(file.getAbsolutePath(), getResources().getColor(R.color.colorAccent));
        runOnUiThread(() -> binding.fileListView.addView(textView));
        binding.fileScrollView.post(() -> binding.fileScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        return textView;
    }
    private synchronized void reset() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        runOnUiThread(() -> {
            binding.fileListView.removeAllViews();
            binding.scanProgress.setProgress(0);
            binding.scanProgress.setMax(1);
        });
    }
    public synchronized void requestWriteExternalPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE},
                    1);
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Permission needed!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
            if (!isAccessGranted()) {
                Intent intent;
                intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean isAccessGranted() {
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode;
            mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid, applicationInfo.packageName);
            return (mode == AppOpsManager.MODE_ALLOWED);

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 &&
                grantResults.length > 0 &&
                grantResults[0] != PackageManager.PERMISSION_GRANTED)
            prompt();
    }
    public final void prompt() {
        Intent intent = new Intent(this, PromptActivity.class);
        startActivity(intent);
    }
}