package ademar.textlauncher;

import static android.content.Intent.ACTION_MAIN;
import static android.content.Intent.ACTION_PACKAGE_ADDED;
import static android.content.Intent.ACTION_PACKAGE_REMOVED;
import static android.content.Intent.ACTION_PACKAGE_REPLACED;
import static android.content.Intent.CATEGORY_LAUNCHER;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Objects;
import java.util.stream.Collectors;

import name.svmhdvn.mytextlauncher.R;

public final class Activity extends android.app.Activity implements
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    private record Model(String label, String packageName) implements Comparable<Model> {
        @Override
        public int compareTo(Model other) {
            return label.compareTo(other.label);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private ArrayAdapter<Model> adapter;

    private final IntentFilter intentFilter = new IntentFilter();

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    private final Intent launcherIntent = new Intent(ACTION_MAIN, null).addCategory(CATEGORY_LAUNCHER);
    private final Intent appDetailsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, null).addCategory(CATEGORY_LAUNCHER);

    public Activity() {
        super();
        intentFilter.addAction(ACTION_PACKAGE_ADDED);
        intentFilter.addAction(ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(ACTION_PACKAGE_REPLACED);
        intentFilter.addDataScheme("package");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);

        adapter = new ArrayAdapter<Model>(this, R.layout.model);

        update();

        final ListView list = findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int index, long id) {
        try {
            startActivity(getPackageManager().getLaunchIntentForPackage(Objects.requireNonNull(adapter.getItem(index)).packageName));
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int index, long id) {
        try {
            startActivity(appDetailsIntent.setData(Uri.fromParts("package", Objects.requireNonNull(adapter.getItem(index)).packageName, null)));
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void update() {
        final PackageManager packageManager = getPackageManager();
        adapter.clear();
        adapter.addAll(packageManager.queryIntentActivities(launcherIntent, 0)
                .stream()
                .map(r -> new Model(r.loadLabel(packageManager).toString(), r.activityInfo.packageName))
                .sorted()
                .collect(Collectors.toList()));
        adapter.notifyDataSetChanged();
    }

}
