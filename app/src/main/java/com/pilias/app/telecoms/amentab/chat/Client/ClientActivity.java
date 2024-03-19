package com.pilias.app.telecoms.amentab.chat.Client;

import static com.pilias.app.telecoms.amentab.chat.Server.ServerActivity.listMess;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;


import com.pilias.app.telecoms.amentab.chat.Prefs;
import com.pilias.app.telecoms.amentab.chat.R;
import com.pilias.app.telecoms.amentab.chat.Server.ServerActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;


/**
 * Created by mandar on 08-04-2018.
 */

public class ClientActivity extends AppCompatActivity {
    ImageButton send_but;
    EditText messagespace;
    Handler handler = new Handler();
    String ip,name;
    final Context context = this;
    Socket clientSocket;
    InetSocketAddress inetAddress;
    PrintStream ps = null;
    //private ChatArrayAdapter chatArrayAdapter;
    public static ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    Prefs prefs;

    public static  Thread sc ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_client);

        messagespace = (EditText) findViewById(R.id.messagespace);
        //messagespace.setImeOptions(EditorInfo.IME_ACTION_SEND);

        send_but =  findViewById(R.id.send_but);


        listView = (ListView) findViewById(R.id.msgview);

        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.right);
        listView.setAdapter(chatArrayAdapter);

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);

        //to scroll the list view to bottom on data change
        chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount() - 1);
            }
        });


        prefs  =  new Prefs(this);
        name = prefs.getString("uname","#");
        ip = prefs.getString("ip","#");

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
            actionBar.setSubtitle(name+"@"+ip);
            actionBar.setLogo(R.mipmap.ic_launcher);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //  breakTaskAct = false;
        //if(sc==null)
          sc = new Thread(new StartCommunication());
        sc.start();





       /*
        sc = new Thread(new StartCommunication());
         if(sc.isAlive()){
             try {
                 Thread.sleep(2000);
             } catch (InterruptedException e) {
             }
             sc.interrupt();
         }
        //  breakTaskAct = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //  breakTaskAct = true;
                sc.start();
            }
        },  2000);

       LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.prompt_client, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        alertDialogBuilder.setView(promptsView);

        final EditText uname = (EditText) promptsView.findViewById(R.id.uname);
        final EditText sip = (EditText) promptsView.findViewById(R.id.sip);


        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        name = uname.getText().toString();
                        ip = sip.getText().toString();
                        Thread sc = new Thread(new StartCommunication());
                        sc.start();
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();*/

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //DbModelSong song = dbModelSongList.get(idGeneral);
        int id = item.getItemId();
        if (id==android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean sendChatMessage(String str, boolean notify) {

        String arr[] = str.split(":");
        if(arr.length == 1) {
            if (str.contains("rejoint") || str.contains("Connecté à"))
                chatArrayAdapter.add(new ChatMessage(false, "<font color='#00AA00'>*** " + str + " ***</font>"));
            else{
                chatArrayAdapter.add(new ChatMessage(false, "<font color='#AA0000'>*** " + str + " ***</font>"));
                sendNotification(str,"tick",ip,true,true,R.mipmap.ic_launcher_round);
            }
        }else if (!arr[0].equals(name)){
            chatArrayAdapter.add(new ChatMessage(false, "<font color='#0077CC'>" + arr[0] + "</font><br/>" + arr[1]));

            if(notify){
                sendNotification(arr[1],"tick",arr[0],true,true,R.mipmap.ic_launcher_round);
            }
        }
        else{

            chatArrayAdapter.add(new ChatMessage(true, arr[1]));
        }

        return true;
    }

    // Helper method to create a notification channel for
    // Android 8.0+
    private void makeNotificationChannel(String channelId, String channelName, int importance) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            NotificationChannel channel
                    = new NotificationChannel(
                    channelId, channelName, importance);
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setVibrationPattern(new long[]{1000,1000});
            NotificationManager notificationManager
                    = (NotificationManager)getSystemService(
                    NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(
                    channel);
        }
    }

    private void sendNotification(String message, String tick, String title, boolean sound, boolean vibrate, int iconID) {
        // Create an explicit intent for an Activity in your app.
        Intent intent = new Intent(this, ClientActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the notification channel for Android 8.0+

            makeNotificationChannel(
                    "CHANNEL_1", getString(R.string.app_name),//"Example channel",
                    NotificationManager.IMPORTANCE_DEFAULT);


        }
// Creating the notification builder
        NotificationCompat.Builder notificationBuilder
                = new NotificationCompat.Builder(this,
                "CHANNEL_1");

        // Getting the notification manager and send the
        // notification
        NotificationManager notificationManager
                = (NotificationManager)getSystemService(
                Context.NOTIFICATION_SERVICE);

        // it is better to not use 0 as notification id, so
        // used 1.

        Notification notification = new Notification();

        if (sound) {
            //notification.defaults |= Notification.DEFAULT_SOUND;
        }

        if (vibrate) {
            //notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        //notificationBuilder.setDefaults(notification.defaults);
        notificationBuilder.setSmallIcon(iconID)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setTicker(tick)
                .setNumber(3)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{500,500});
                //.setLights(Color.RED,3000,3000);
                //.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        if (vibrate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(500);
            }
        }


        notificationManager.notify(1,
                //* ID of notification
                notificationBuilder.build());



    }



    boolean exitLocker = false;
    @Override
    public void onBackPressed() {
        if (!exitLocker){
            exitLocker = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exitLocker =false;
                }
            },  3000);

            Toast.makeText(getApplicationContext(), getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show();

        }else {
            //  breakTaskAct = false;

            //moveTaskToBack(true);
            super.onBackPressed();
        }

    }

    @Override protected void onDestroy() {
        //  breakTaskAct = false;
        super.onDestroy();
        if (ps != null) {
            try{
                ps.println("Ex1+:" + name);
                ps.close();
            } catch (Exception e) {
                //throw new RuntimeException(e);
            }
        }
    }

    public static boolean sameTaskAct = false;
    //public static boolean  breakTaskAct = true;
    public static int numberTaskAct = 0;
    public static boolean inConnexion = false;
    class StartCommunication implements Runnable {

        String serverExit = "";

        @Override
        public void run() {
            numberTaskAct++;

            try {

                inetAddress = new InetSocketAddress(ip,55555);
                clientSocket = new Socket();
                clientSocket.connect(inetAddress,7000);
                //if (ps == null)
                ps = new PrintStream(clientSocket.getOutputStream());

                inConnexion = true;

                listMess.add("Connecté à "+ ip + "\n");
                int i = 0;
                for (String s : listMess){
                    /*i++;
                    if(i>0){
                    if(s.contains("joint:")){
                            if(s.equals(listMess.get(i-1))){
                                listMess.remove(i);
                            }
                        }

                    }*/
                   sendChatMessage(s,false);
                    Log.d("ClientActivity:listMess", s);
                }
                // sendChatMessage("Connecté à "+ ip + "\n",true);
                ps.println("j01ne6:" + name);

                send_but.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!messagespace.getText().toString().equals("")){
                            Thread st = new Thread(new SendThread());
                            st.start();
                        }
                    }
                });

                messagespace.setOnKeyListener(new View.OnKeyListener() {
                    @RequiresApi(api = Build.VERSION_CODES.R)
                    @Override
                    public boolean onKey(View view, int i, KeyEvent keyEvent) {
                        if(keyEvent.getAction()==KeyEvent.ACTION_DOWN && i == keyEvent.KEYCODE_ENTER){

                            if(!messagespace.getText().toString().equals("")){
                                Thread st = new Thread(new SendThread());
                                st.start();
                            }
                            return true;
                        }
                        return false;
                    }
                });
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                //if(!sameTaskAct){ }
                // while(breakTaskAct)
                while(true)
                {
                    if(numberTaskAct>1){
                        numberTaskAct --;
                        break;
                    }
                        //sameTaskAct=true;
                        final String str = br.readLine();
                        if(str!=null){
                            if(str.equalsIgnoreCase("exit"))
                            {
                                final String str2 = "Le serveur a fermé la Connexion !";
                                numberTaskAct = 0;
                                serverExit = str2;
                                sendChatMessage(str2,true);
                                listMess.add(str2);
                                Thread.sleep(2000);
                                //finish();
                                //  breakTaskAct = false;
                                break;
                            }

                            sendChatMessage(str,true);
                            listMess.add(str);
                            //if(!str.equals("")){ }


                        }


                    }


            }catch(final Exception e){
                String str = "Impossible de se connecter!";
                numberTaskAct = 0;
                sendChatMessage(/*e.getMessage() e+*/str,true);
                listMess.add(str);
                //  breakTaskAct = false;
                inConnexion = false;

                    if(!serverExit.equals("Le serveur a fermé la Connexion !"))
                    {
                        try {
                            Thread.sleep(2000);
                        } catch (Exception exx){ }
                        //moveTaskToBack(true);
                        finish();
                    }
            }
        }
    }

    class SendThread implements Runnable {
        @Override
        public void run() {
            try {
                String message = messagespace.getText().toString();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        messagespace.setText("");
                    }
                });
                message = name + ": " + message;
                ps.println(message);
                ps.flush();

                if(inConnexion){
                    //listMess.add(message + "\n");
                }
            }
            catch (Exception e) {

            }
        }
    }
}
