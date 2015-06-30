package com.example.aosamesan.projectfinalfinal;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;


public class ListActivity extends ActionBarActivity {
    private static final int MIN_MOVE = 200;
    final int PORT = 25252;
    final int REQUEST_CODE_CONTROLLER = 2500;
    ListView pptListView;
    ArrayAdapter<String> pptListAdapter;
    String IP = "localhost";
    HashMap<String, Integer> item;
    public AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                Intent intent = new Intent(getBaseContext(), ControllerActivity.class);
                String pptName = parent.getAdapter().getItem(position).toString();
                int pptNum = item.get(pptName);
                intent.putExtra("IP", IP);
                intent.putExtra("pptNum", pptNum);
                intent.putExtra("pptName", pptName);
                SendPptNumAsync sendPptNumAsync = new SendPptNumAsync();
                Object[] param = new Object[]{new Integer(pptNum)};
                sendPptNumAsync.execute(param);

                if (sendPptNumAsync.get())
                    startActivityForResult(intent, REQUEST_CODE_CONTROLLER);
                else
                    Toast.makeText(getApplicationContext(), "에러! 서버가 꺼져있는 듯 합니다.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    TextView ipText;
    Button exitButton;
    TabHost tabHost;
    boolean isConnected;
    TextView helpTextView;
    Button refreshButton;
    View.OnClickListener onRefreshListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                ConnectAsync connectAsync = new ConnectAsync();
                connectAsync.execute();
                isConnected = connectAsync.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setResult(0);
            finish();
        }
    };
    private float oldX, newX;
    View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return myTouchEvent(event);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        getSupportActionBar().hide();

        Intent intent = getIntent();

        item = new HashMap<>();
        IP = intent.getStringExtra("IP");

        ipText = (TextView) findViewById(R.id.ip_text_view);
        exitButton = (Button) findViewById(R.id.button_exit_list);
        exitButton.setOnClickListener(onClickListener);

        ipText.setText(IP);
        pptListView = (ListView) findViewById(R.id.listView);
        pptListAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.my_list_layout);
        pptListView.setAdapter(pptListAdapter);


        pptListView.setOnItemClickListener(itemClickListener);
        pptListView.setOnTouchListener(onTouchListener);

        refreshButton = (Button) findViewById(R.id.refresh_list_button);
        refreshButton.setOnClickListener(onRefreshListener);

        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();
        TabHost.TabSpec tabSpec = tabHost.newTabSpec("listView");
        tabSpec.setContent(R.id.listView);
        tabSpec.setIndicator("PPT List");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("help_text_view");
        tabSpec.setContent(R.id.help_text_view);
        tabSpec.setIndicator("Help");
        tabHost.addTab(tabSpec);

        tabHost.setOnTouchListener(onTouchListener);

        ((TextView) tabHost.getTabWidget().getChildAt(0).findViewById(android.R.id.title)).setTextColor(this.getResources().getColorStateList(android.R.color.background_light));
        ((TextView) tabHost.getTabWidget().getChildAt(1).findViewById(android.R.id.title)).setTextColor(this.getResources().getColorStateList(android.R.color.background_light));


        try {
            ConnectAsync connectAsync = new ConnectAsync();
            connectAsync.execute();
            isConnected = connectAsync.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isConnected)
            Toast.makeText(getApplicationContext(), "연결됨 : " + IP, Toast.LENGTH_SHORT).show();
        else {
            Intent resultIntent = new Intent();
            setResult(-1, resultIntent);
            finish();
        }
    }

    public boolean myTouchEvent(MotionEvent event) {
        boolean result = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                oldX = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                newX = event.getX();

                float deltaX = newX - oldX;

                if (Math.abs(deltaX) > MIN_MOVE) {
                    if (deltaX > 0) {
                        tabHost.setCurrentTab(0);
                    } else {
                        tabHost.setCurrentTab(1);
                    }
                }

                break;
        }


        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return myTouchEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CONTROLLER:
                Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_LONG).show();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ConnectAsync extends AsyncTask<Void, Void, Boolean> {
        private synchronized boolean getPresentationList() {
            try {
                Socket remoteSock = new Socket();
                SocketAddress address = new InetSocketAddress(IP, PORT);
                OutputStream outputStream = null;
                InputStream inputStream = null;

                remoteSock.connect(address, MainActivity.REQUEST_TIMEOUT);
                System.out.println("/////////////////// CONNECTED");
                outputStream = remoteSock.getOutputStream();
                inputStream = remoteSock.getInputStream();

                byte[] request = PMarionette.MessageSerializer.Serialize(PMarionette.LISTITEMREQUEST, -1);
                outputStream.write(request);
                outputStream.flush();

                byte[] responseType = new byte[4];
                inputStream.read(responseType, 0, 4);
                int msgType = ByteBuffer.wrap(responseType).getInt();

                switch (msgType) {
                    case PMarionette.LISTITEM:
                        byte[] responseLengthByte = new byte[4];
                        inputStream.read(responseLengthByte, 0, 4);
                        int msgSize = ByteBuffer.wrap(responseLengthByte).getInt();
                        if (msgSize < 0)
                            throw new SocketException();
                        else if (msgSize == 0)
                            break;
                        byte[] responseBuffer = new byte[msgSize];
                        inputStream.read(responseBuffer, 0, msgSize);
                        Object response = PMarionette.MessageSerializer.Deserialize(msgType, msgSize, responseBuffer);
                        item = (HashMap<String, Integer>) response;

                        ListActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pptListAdapter.clear();
                                for (String s : item.keySet()) {
                                    pptListAdapter.add(s);
                                }
                                pptListAdapter.notifyDataSetChanged();
                            }
                        });
                        break;
                    default:
                        break;
                }

                inputStream.close();
                remoteSock.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;
            try {
                result = getPresentationList();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }
    }

    // Object
    // pptNum
    private class SendPptNumAsync extends AsyncTask<Object, Void, Boolean> {
        private synchronized boolean sendPptNum(Object... objects) {
            try {
                Socket remoteSock = new Socket();
                SocketAddress address = new InetSocketAddress(IP, PORT);
                OutputStream outputStream;

                remoteSock.connect(address, MainActivity.REQUEST_TIMEOUT);
                outputStream = remoteSock.getOutputStream();

                Integer pptNum = (Integer) objects[0];

                byte[] request = PMarionette.MessageSerializer.Serialize(PMarionette.LISTITEM, pptNum);
                outputStream.write(request);
                outputStream.flush();

                outputStream.close();
                remoteSock.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return false;
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            boolean result = false;
            try {
                result = sendPptNum(params);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }
    }
}
