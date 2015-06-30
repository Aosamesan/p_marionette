package com.example.aosamesan.projectfinalfinal;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.aosamesan.projectfinalfinal.util.SystemUiHider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class ControllerActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
    private static final float MIN_MOVEMENT = 200.0f;
    private static String IP = "localhost";
    private static int PORT = 25252;
    private static int pptNum = -1;
    private static String pptName = "None";
    Bitmap bitmap;
    View.OnClickListener exitOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setResult(0);
            finish();
        }
    };
    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;
    private float oldX, newX, oldY, newY;
    private ImageView imageView = null;
    private Button buttonExit = null;
    private Button buttonRefresh = null;
    private boolean isStart = false;
    View.OnClickListener refreshOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isStart) {
                byte[] imageBytes = null;

                try {
                    ReceiveImageAsync receiveImageAsync = new ReceiveImageAsync();
                    receiveImageAsync.execute();

                    imageBytes = receiveImageAsync.get();
                    if (imageBytes != null) {
                        final byte[] unzippedBytes = (byte[]) PMarionette.MessageSerializer.Deserialize(PMarionette.IMAGE, imageBytes.length, imageBytes);
                        System.out.println("************ Image Bytes : " + unzippedBytes.length);

                        if (unzippedBytes != null) {
                            ControllerActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(unzippedBytes);
                                        bitmap = BitmapFactory.decodeStream(byteArrayInputStream);
                                        imageView.setImageBitmap(bitmap);
                                        byteArrayInputStream.close();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        oldX = oldY = newX = newY = 0;

        setContentView(R.layout.activity_controller);

        getActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        IP = getIntent().getStringExtra("IP");
        pptNum = getIntent().getIntExtra("pptNum", 10);
        pptName = getIntent().getStringExtra("pptName");

        ((TextView) findViewById(R.id.fullscreen_content)).setText(pptName);


        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        imageView = (ImageView) findViewById(R.id.slide_image);
        buttonExit = (Button) findViewById(R.id.exit_button);
        buttonExit.setOnClickListener(exitOnClick);
        buttonRefresh = (Button) findViewById(R.id.refresh_button);
        buttonRefresh.setOnClickListener(refreshOnClick);
    }

    @Override
    protected void onDestroy() {
        isStart = false;
        SendSignalAsync sendSignalAsync = new SendSignalAsync();
        Integer[] params = new Integer[]{new Integer(PMarionette.APPLCIATIONEXIT)};
        sendSignalAsync.execute(params);
        super.onDestroy();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = false;
        SendSignalAsync sendSignalAsync = null;
        ReceiveImageAsync receiveImageAsync = null;
        Integer[] params = null;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                oldX = event.getX();
                oldY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                newX = event.getX();
                newY = event.getY();

                float deltaX = newX - oldX;
                float deltaY = newY - oldY;

                sendSignalAsync = new SendSignalAsync();
                if (Math.abs(deltaX) > MIN_MOVEMENT) {
                    if (deltaX > 0) {
                        params = new Integer[]{new Integer(PMarionette.PREV)};
                        receiveImageAsync = new ReceiveImageAsync();

                    } else {
                        params = new Integer[]{new Integer(PMarionette.NEXT)};
                        receiveImageAsync = new ReceiveImageAsync();
                    }
                    result = true;
                } else if (Math.abs(deltaY) > MIN_MOVEMENT) {
                    if (deltaY < 0) {
                        params = new Integer[]{new Integer(PMarionette.START)};
                        isStart = true;
                        receiveImageAsync = new ReceiveImageAsync();
                    } else {
                        params = new Integer[]{new Integer(PMarionette.STOP)};
                        isStart = false;
                        ControllerActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.p_marionette_title));
                            }
                        });
                    }
                    result = true;
                }
                if (params != null)
                    sendSignalAsync.execute(params);

                try {
                    if (result && receiveImageAsync != null && sendSignalAsync.get()) {
                        receiveImageAsync.execute();
                        final byte[] zipedImageBytes = receiveImageAsync.get();
                        if (zipedImageBytes != null) {
                            final byte[] imageBytes = (byte[]) PMarionette.MessageSerializer.Deserialize(PMarionette.IMAGE, zipedImageBytes.length, zipedImageBytes);
                            System.out.println("************ Image Bytes : " + imageBytes.length);

                            if (imageBytes != null) {
                                ControllerActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
                                            bitmap = BitmapFactory.decodeStream(byteArrayInputStream);
                                            imageView.setImageBitmap(bitmap);
                                            byteArrayInputStream.close();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

        }

        return result;
    }

    public static class SendSignalAsync extends AsyncTask<Integer, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Integer... params) {
            boolean result = false;
            try {
                int signal = params[0];


                byte[] request = PMarionette.MessageSerializer.Serialize(PMarionette.SIGNAL, signal);

                Socket remoteSock = new Socket();
                SocketAddress address = new InetSocketAddress(IP, PORT);
                OutputStream outputStream = null;

                remoteSock.connect(address, MainActivity.REQUEST_TIMEOUT);
                outputStream = remoteSock.getOutputStream();

                outputStream.write(request);
                outputStream.flush();

                outputStream.close();
                remoteSock.close();
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }
    }

    public static class ReceiveImageAsync extends AsyncTask<Void, Void, byte[]> {
        static final int BUFFER_SIZE = 512;

        @Override
        protected byte[] doInBackground(Void... params) {
            byte[] result = null;

            try {
                byte[] typeBuf = new byte[4];
                byte[] sizeBuf = new byte[4];
                int type = PMarionette.UNDEFINED;
                int size = 0;
                int allSize = 0;

                Socket remoteSock = new Socket();
                SocketAddress address = new InetSocketAddress(IP, PORT);
                InputStream inputStream;
                OutputStream outputStream;

                remoteSock.connect(address, MainActivity.REQUEST_TIMEOUT);
                inputStream = remoteSock.getInputStream();
                outputStream = remoteSock.getOutputStream();

                byte[] request = PMarionette.MessageSerializer.Serialize(PMarionette.IMAGE, 4);
                outputStream.write(request);
                outputStream.flush();

                inputStream.read(typeBuf, 0, 4);
                type = ByteBuffer.wrap(typeBuf).getInt();
                inputStream.read(sizeBuf, 0, 4);
                size = ByteBuffer.wrap(sizeBuf).getInt();

                System.out.println("Size : " + size + "\tType : " + type);

                if (type == PMarionette.IMAGE) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[BUFFER_SIZE];

                    while (remoteSock.isConnected()) {
                        int read = inputStream.read(buffer, 0, BUFFER_SIZE);
                        byteArrayOutputStream.write(buffer, 0, read);
                        allSize += read;
                        System.out.println("Read : " + read);
                        if (allSize >= size)
                            break;
                    }

                    result = PMarionette.MessageSerializer.UnZip(byteArrayOutputStream.toByteArray());
                    System.out.println("************** result length : " + result.length);
                }
                inputStream.close();
                outputStream.close();
                remoteSock.close();

                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }
    }
}
