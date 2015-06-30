package com.example.aosamesan.projectfinalfinal;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.Inet4Address;


public class MainActivity extends ActionBarActivity {
    public static final int REQUEST_TIMEOUT = 3000;
    static final int REQUEST_CODE_LIST = 3000;
    Button connectButton;
    Button exitButton;
    EditText ipEdit;
    View.OnClickListener connectButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                Inet4Address.getAllByName(ipEdit.getText().toString());
                Intent intent = new Intent(getBaseContext(), ListActivity.class);
                intent.putExtra("IP", ipEdit.getText().toString());
                startActivityForResult(intent, REQUEST_CODE_LIST);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "잘못 된 아이피 입니다.", Toast.LENGTH_LONG).show();
            }
        }
    };
    View.OnClickListener exitButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setResult(0);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        getSupportActionBar().hide();

        connectButton = (Button) findViewById(R.id.connect_button);
        ipEdit = (EditText) findViewById(R.id.ip_text);
        connectButton.setOnClickListener(connectButtonListener);


        exitButton = (Button) findViewById(R.id.exit_main_button);
        exitButton.setOnClickListener(exitButtonListener);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_LIST:
                if (resultCode == -1) {
                    Toast.makeText(getApplicationContext(), "접속할 수 없습니다.", Toast.LENGTH_LONG).show();
                }
                ipEdit.setText("");
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
}
