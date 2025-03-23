package com.example.opencv.device;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.example.opencv.image.ImageEditActivity;
import com.example.opencv.modbus.ModbusTCPClient;

public class device_Control extends AppCompatActivity {

    public static final int AXIS_Y = 0;
    public static final int AXIS_X = 1;
    public static final int DEFAULT_SPEED = 100;  // mm/s
    public static final int DEFAULT_DISTANCE = 1; // mm
    public static final int LONG_PRESS_DELAY = 500;  // 长按触发延迟(ms)
    public static final int MOVE_INTERVAL = 100;  // 持续移动间隔(ms)
    ModbusTCPClient mtcp = ModbusTCPClient.getInstance();
    private static final String TAG = "devicecontrol";
    private Button button_up;
    private Button button_down;
    private Button button_left;
    private Button button_right;
    private NumberPicker da1Picker;
    private NumberPicker da2Picker;

    private static Toolbar toolbar;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device_control);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 初始化轴控制
        button_up = findViewById(R.id.btn_up);
        button_down = findViewById(R.id.btn_down);
        button_left = findViewById(R.id.btn_left);
        button_right = findViewById(R.id.btn_right);
        btnMoveControl(button_up, AXIS_Y, DEFAULT_SPEED, DEFAULT_DISTANCE);
        btnMoveControl(button_down, AXIS_Y, DEFAULT_SPEED, DEFAULT_DISTANCE * (-1));
        btnMoveControl(button_left, AXIS_X, DEFAULT_SPEED, DEFAULT_DISTANCE);
        btnMoveControl(button_right, AXIS_X, DEFAULT_SPEED, DEFAULT_DISTANCE * (-1));

        // 初始化DO控制
        GridLayout doGrid = findViewById(R.id.doGrid);
        for (int i = 1; i <= 8; i++) {
            Switch doSwitch = new Switch(this);
            final int doNumber = i;
            doSwitch.setText("DO" + i);
            doSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setDO(doNumber, isChecked);
                }
            });
            doGrid.addView(doSwitch);
        }

        // 初始化DA控制
        da1Picker = findViewById(R.id.da1Picker);
        da2Picker = findViewById(R.id.da2Picker);

        configureNumberPicker(da1Picker, 1);
        configureNumberPicker(da2Picker, 2);
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

    private void configureNumberPicker(NumberPicker picker, final int daNumber) {
        picker.setMinValue(50);
        picker.setMaxValue(10000); // 假设DA范围是0-5000mV
        picker.setValue(5000);
        picker.setWrapSelectorWheel(false);
        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setDA(daNumber, newVal);
            }
        });
    }


    @SuppressLint("ClickableViewAccessibility")
    private void btnMoveControl(Button button, int axis, int speed, int distance) {
        button.setOnTouchListener(new View.OnTouchListener() {
            private Handler handler = new Handler();
            private long downTime;
            private Runnable longPressRunnable = new Runnable() {
                @Override
                public void run() {
                    moveAxis(AXIS_Y, DEFAULT_SPEED, DEFAULT_DISTANCE);
                    handler.postDelayed(this, LONG_PRESS_DELAY);
                }
            };

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downTime = System.currentTimeMillis();
                        handler.postDelayed(longPressRunnable, LONG_PRESS_DELAY);
                        return true;
                    case MotionEvent.ACTION_UP:
                        long upTime = System.currentTimeMillis();
                        if (upTime - downTime < LONG_PRESS_DELAY) {
                            // 单击事件
                            moveAxis(AXIS_Y, DEFAULT_SPEED, DEFAULT_DISTANCE);
                        }
                        handler.removeCallbacks(longPressRunnable);
                        return true;
                }
                return false;
            }
        });
    }

    private void moveAxis(int axis, int speed, int distance) {
        // 在这里实现你的轴移动逻辑i
        // 参数：axis (0-3), speed (mm/s), distance (mm)，均为整数
        new Thread(new Runnable() {
            Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void run() {
                try {
                    mtcp.ControlAxisRun(axis, speed * 1000, distance * 1000);
                } catch (ModbusTCPClient.ModbusException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mtcp.onWriteFailed(device_Control.this);
                        }
                    });
                    Log.d("TCPTest", e.getMessage());
                }
            }
        }).start();
    }

    // 控制DO开关
    private void setDO(int doNumber, boolean state) {
        // 在这里实现你的DO控制逻辑
        // 参数：doNumber (1-8), state (true/false)
        new Thread(new Runnable() {
            Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void run() {
                try {
                    mtcp.ControlDO(doNumber, state ? 1 : 0);
                } catch (ModbusTCPClient.ModbusException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mtcp.onWriteFailed(device_Control.this);
                        }
                    });
                    Log.d("TCPTest", e.getMessage());
                }
            }
        }).start();
    }

    // 控制DA输出
    private void setDA(int daNumber, int value) {
        // 在这里实现你的DA控制逻辑
        // 参数：daNumber (1-2), value (mV)
        new Thread(new Runnable() {
            Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void run() {
                try {
                    mtcp.ControlDA(daNumber, value);
                } catch (ModbusTCPClient.ModbusException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mtcp.onWriteFailed(device_Control.this);
                        }
                    });
                    Log.d("TCPTest", e.getMessage());
                }
            }
        }).start();
    }

    public void Stop(View view) {
        new Thread(new Runnable() {
            Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void run() {
                try {
                    mtcp.ControlStop();
                } catch (ModbusTCPClient.ModbusException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mtcp.onWriteFailed(device_Control.this);
                        }
                    });
                    Log.d("TCPTest", e.getMessage());
                }
            }
        }).start();
    }

    public void Back(View view) {
        new Thread(new Runnable() {
            Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void run() {
                try {
                    mtcp.ControlBack();
                } catch (ModbusTCPClient.ModbusException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mtcp.onWriteFailed(device_Control.this);
                        }
                    });
                    Log.d("TCPTest", e.getMessage());
                }
            }
        }).start();
    }

    public void FTC(View view) {
        new Thread(new Runnable() {
            Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void run() {
                try {
                    mtcp.ControlFTC();
                } catch (ModbusTCPClient.ModbusException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mtcp.onWriteFailed(device_Control.this);
                        }
                    });
                    Log.d("TCPTest", e.getMessage());
                }
            }
        }).start();
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

        Intent intent = new Intent(device_Control.this, ImageEditActivity.class);
        startActivity(intent);
    }

    public void OnClickDeviceControl(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(device_Control.this, device_Control.class);
        startActivity(intent);
    }
}



