package com.panard.navhelper;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Context;
import android.os.IBinder;

import android.util.Log;
import android.view.Display;
import android.graphics.Rect;
import android.os.RemoteException;
import android.content.res.Configuration;

import static com.panard.navhelper.R.id.text_stat;

//import com.android.testapp.NotificationUtils;

public class MyActivity extends Activity {
    private int m_main_iface[] = null;
    private int m_aux_iface[] = null;

    private boolean DBG = true;
    private String TAG = "NavHelper";

    public final int MAIN_DISPLAY = 0;
    public final int AUX_DISPLAY = 1;

    private boolean isNavRunning = false;
    private boolean isScreenOn = false;
    private boolean isPlayerRunning = false;
    //private NotificationUtils notificationUtils;
    public static final String id = "channel_1";
    public static final String name = "名字";
    private NotificationManager manager;
    private boolean mThreadExited = true;
    private PlayThread playThread = null;

    private final int buf_size = 8192;
    private int sample_rate = 44100;
    private AudioTrack mPlayer = null;

    final String CHANNEL_ID = "channel_id_1";
    final String CHANNEL_NAME = "channel_name_1";


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        init();
        sample_rate = AudioTrack.getNativeOutputSampleRate(3);

        final TextView stat = (TextView) findViewById(R.id.text_stat);

        final Button btn_ctrl = (Button)findViewById(R.id.btn_ctrl);
        btn_ctrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlayerRunning) {
                    isPlayerRunning = false;
                    getManager().cancel(1);
                    btn_ctrl.setText(R.string.ctrl_start);
                    stat.setText(R.string.stat_stopped);
                    stat.setTextColor(getColor(R.color.orangered));
                } else {
                    //Toast.makeText(MyActivity.this,"btn_ctrl clicked",Toast.LENGTH_SHORT).show();
                    isPlayerRunning = true;
                    btn_ctrl.setText(R.string.ctrl_stop);
                    sendNotification("导航宝运行中", "点击查看详情");
                    stat.setText(R.string.stat_running);
                    stat.setTextColor(getColor(R.color.lime));
                    if (mPlayer == null) {
                        //play BT stream to virtual snd card, let audio system do mix and resample for it
                        mPlayer = new AudioTrack(AudioManager.STREAM_MUSIC,
                                sample_rate,
                                AudioFormat.CHANNEL_OUT_STEREO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                buf_size,
                                AudioTrack.MODE_STREAM
                        );
                    }
                    while (!mThreadExited) {
                        try {
                            Thread.sleep(200*1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //if (playThread == null) {
                    if (true) {
                        playThread = new PlayThread();
                        playThread.setName("nav_helper_audio");
                        playThread.setPriority(Thread.MAX_PRIORITY);
                    }
                    playThread.start();
                }
            }
        });

    }

    private void init() {
        Log.d(TAG, "init");
    }

    public Notification.Builder getChannelNotification(String title, String content){
        return new Notification.Builder(getApplicationContext(), id)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setAutoCancel(true);
    }

    public void createNotificationChannel(){
        NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
        getManager().createNotificationChannel(channel);
    }
    private NotificationManager getManager(){
        if (manager == null){
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        return manager;
    }

    public void sendNotification(String title, String content){
        if (Build.VERSION.SDK_INT>=26){
            createNotificationChannel();
            Notification notification = getChannelNotification
                    (title, content).build();
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            getManager().notify(1,notification);
        }else{
            Log.d("NotificationUtils", "sdk version low :" + Build.VERSION.SDK_INT);
        }
    }

    private class PlayThread extends Thread {
        byte[] buffer = new byte[buf_size];
        @Override
        public void run(){
            mPlayer.play();
            while (isPlayerRunning) {
                try {
                    mPlayer.write(buffer, 0, buf_size);
                    //Log.d(TAG, "write " + buf_size);
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
            mThreadExited = true;
            mPlayer.stop();
        }
    }

}
