package com.example.opencv.device;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.example.opencv.image.ImageEditActivity;
import com.example.opencv.modbus.ModbusTCPClient;

import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DeviceActivity extends AppCompatActivity implements UdpReceiver.OnDeviceReceivedListener {
    ModbusTCPClient mtcp = ModbusTCPClient.getInstance();
    private static final String TAG = "UdpListener";
    private static final long DEVICE_EXPIRATION_MS = 10 * 1000; // 10秒超时
    private Set<Device> deviceSet;
    private UdpReceiver udpReceiver;
    private DeviceTableAdapter deviceTableAdapter;
    private RecyclerView deviceTable;
    private ExecutorService executorService;
    private Handler expirationHandler = new Handler(Looper.getMainLooper());
    public static ImageView noDeviceImage;
    //public static ScrollView deviceList;
    public static Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // 隐藏导航栏和状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        noDeviceImage = findViewById(R.id.nodeviceimage);
        //deviceList = findViewById(R.id.devicelist);

        deviceSet = new CopyOnWriteArraySet<>();

        deviceTable = findViewById(R.id.recyclerViewDevice);
        deviceTable.setLayoutManager(new LinearLayoutManager(this));

        DeviceTableAdapter.OnDeviceClickListener ondeviceClickListener = new DeviceTableAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(Device device) {
                Handler handler = new Handler(Looper.getMainLooper());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mtcp.connect(device.getIp(), device.getPort(), 1, DeviceActivity.this);
                            mtcp.ConnectDeviceId = device.getDeviceId();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mtcp.onConnected(DeviceActivity.this, device.getDeviceId());
                                    findViewById(R.id.button8).setClickable(true);
                                }
                            });
                        } catch (ModbusTCPClient.ModbusException e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mtcp.onConnectionFailed(DeviceActivity.this, device.getDeviceId());
                                }
                            });
                            Log.d("UdpListener", e.getMessage());
                        }
                    }
                }).start();

            }
        };
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        deviceTableAdapter = new DeviceTableAdapter(new CopyOnWriteArrayList<>(), deviceSet, ondeviceClickListener);
        deviceTable.setAdapter(deviceTableAdapter);
        //添加Android自带的分割线
        deviceTable.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        executorService = Executors.newSingleThreadExecutor();

        udpReceiver = new UdpReceiver(4001, new Handler(Looper.getMainLooper()), this);
        udpReceiver.startReceiving();

        //updateDeviceListVisibility();
    }

    // 加载 Toolbar 菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    // 监听 Toolbar 按钮点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.User_image) {
            Toast.makeText(this, "社区功能开发中", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDeviceReceived(Device device) {
        if (!deviceSet.contains(device)) {
            deviceTableAdapter.addData(device);
        } else {
            // 如果已存在，更新时间戳
            device.updateTimestamp();
        }
        //runOnUiThread(this::updateDeviceListVisibility);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        expirationHandler.removeCallbacksAndMessages(null);
        udpReceiver.closeSocket();
        udpReceiver.shutdownExecutor();
        executorService.shutdown();
    }

    // 定期清理过期设备
    private void startExpirationCheck() {
        expirationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                cleanExpiredDevices();
                expirationHandler.postDelayed(this, DEVICE_EXPIRATION_MS);
            }
        }, DEVICE_EXPIRATION_MS);
    }

    private void cleanExpiredDevices() {
        new Thread(() -> {
            long currentTime = System.currentTimeMillis();
            Iterator<Device> iterator = deviceSet.iterator();
            while (iterator.hasNext()) {
                Device device = iterator.next();
                if (currentTime - device.getLastUpdated() > DEVICE_EXPIRATION_MS) {
                    iterator.remove();
                    deviceTableAdapter.removeDevice(device);
                }
            }
        }).start();
        //runOnUiThread(this::updateDeviceListVisibility);
    }

    private void updateDeviceListVisibility() {
        if (deviceSet.isEmpty()) {
            noDeviceImage.setVisibility(View.VISIBLE);
            //deviceList.setVisibility(View.VISIBLE);
        } else {
            noDeviceImage.setVisibility(View.GONE);
            //deviceList.setVisibility(View.VISIBLE);
        }
    }

    public void toSystemWifi(View view) {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        startActivity(intent);
    }

    public void showHelpInfo(View view) {
        // 使用 Toast 显示帮助信息
        Toast.makeText(this, "请尝试重启打印机", Toast.LENGTH_SHORT).show();
    }

    public void OnClickDeviceInfo(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(DeviceActivity.this, DeviceInfoActivity.class);
        startActivity(intent);
    }

    public void OnClickDeviceControl(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(DeviceActivity.this, device_Control.class);
        startActivity(intent);
    }

    public void mainPage(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    public void editImage(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(DeviceActivity.this, ImageEditActivity.class);
        startActivity(intent);
    }
}