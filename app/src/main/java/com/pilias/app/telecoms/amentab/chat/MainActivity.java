package com.pilias.app.telecoms.amentab.chat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.pilias.app.telecoms.amentab.chat.Client.ClientActivity;
import com.pilias.app.telecoms.amentab.chat.Server.ServerActivity;


public class MainActivity extends AppCompatActivity {

    private Button beserver;
    private Button beclient;
    EditText etName;
    EditText etIp;
    public static Prefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new Prefs(this);
        beserver = (Button) findViewById(R.id.be_server);
        beclient = (Button) findViewById(R.id.be_client);
        etName= findViewById(R.id.uname);
        etIp =  findViewById(R.id.sip);

        //kyb
        //etIp.requestFocus();
        //etName.requestFocus(0);
        //InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        // manager.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
        //manager.showSoftInputFromInputMethod(editText.getWindowToken(),0);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        etIp.setText(prefs.getString("ip",""));
        etName.setText(prefs.getString("uname",""));


        beserver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String un = String.valueOf(etName.getText());
                String ip = String.valueOf(etIp.getText());
                if(!un.equals("")){
                    prefs.setString("uname",un);
                    prefs.setString("ip",ip);
                    launchServerActivity();
                }else Toast.makeText(MainActivity.this, "Votre  nom !", Toast.LENGTH_SHORT).show();
            }
        });

        beclient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String un = String.valueOf(etName.getText());
                String ip = String.valueOf(etIp.getText());


                if(!un.equals("")&&!ip.equals("")){
                    prefs.setString("ip",ip);
                    prefs.setString("uname",un);
                    launchClientActivity();
                }else Toast.makeText(MainActivity.this, "Le nom et l'adresse svp !", Toast.LENGTH_SHORT).show();
            }
        });
    }




    private void launchServerActivity() {

        Intent intent = new Intent(this, ServerActivity.class);
        startActivity(intent);
    }

    private void launchClientActivity() {

        Intent intent = new Intent(this, ClientActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //ClientActivity.breakTaskAct  = true;
    }
}
