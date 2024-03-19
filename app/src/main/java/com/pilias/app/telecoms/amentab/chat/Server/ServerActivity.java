package com.pilias.app.telecoms.amentab.chat.Server;

import static com.pilias.app.telecoms.amentab.chat.Client.ClientActivity.inConnexion;
import static com.pilias.app.telecoms.amentab.chat.MainActivity.prefs;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.pilias.app.telecoms.amentab.chat.Client.ClientActivity;
import com.pilias.app.telecoms.amentab.chat.MainActivity;
import com.pilias.app.telecoms.amentab.chat.Prefs;
import com.pilias.app.telecoms.amentab.chat.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mandar on 08-04-2018.
 */

public class ServerActivity extends AppCompatActivity {
    ImageButton send_but;
    EditText messagespace;
    TextView chatspace;
    final Context context = this;
    String ip,name;
    static Socket arr[] = new Socket[100];
    static int num = 0;
    Handler handler = new Handler();
    //private ChatArrayAdapter chatArrayAdapter;
    public static ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private boolean side = false;
    ServerSocket ss;

    public static List<String> listMess = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_server);
       // sendNotification("arr[1]","tick","arr[0]",true,true,R.mipmap.ic_launcher_foreground);

        prefs  =  new Prefs(this);
        ip = Utils.getIPAddress(true);
        messagespace = (EditText) findViewById(R.id.messagespace);
        send_but = findViewById(R.id.send_but);

        name = prefs.getString("uname","#");
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
            actionBar.setLogo(R.drawable.at_logo);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setSubtitle(name+"@"+ip);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }/**/
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


        Thread sc = new Thread(new StartCommunication());
        sc.start();

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

    boolean exit = false;
    @Override
    public void onBackPressed() {
        if (!exit){
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit =false;
                }
            },  3000);

            Toast.makeText(getApplicationContext(), getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show();

        }else{

            //moveTaskToBack(true);
            super.onBackPressed();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for(int i=0;i<num;i++) {
            Socket temp = arr[i];
            SendToAll thread = new SendToAll(temp,"exit");
            thread.start();
        }
        try{
            ss.close();
        }catch(Exception e){}
    }

    private boolean sendChatMessage(String str,boolean notify) {
        String arr[] = str.split(":");
        if(arr.length == 1) {
            if(str.contains("Serveur démarré") || str.contains("rejoint"))
                chatArrayAdapter.add(new ChatMessage(false, "<font color='#00AA00'>*** " + str + "***</font>"));
            else{
                chatArrayAdapter.add(new ChatMessage(false, "<font color='#AA0000'>*** " + str + "***</font>"));
                sendNotification(str,"tick",ip,true,true,R.mipmap.ic_launcher_round);
            }
        }
        else if (!arr[0].equals(name)){
            chatArrayAdapter.add(new ChatMessage(false, "<font color='#0077CC'>" + arr[0] + "</font><br/>" + arr[1]));

            if(notify){
                sendNotification(arr[1],"tick",arr[0],true,true,R.mipmap.ic_launcher_round);
                //sendNotification(arr[1]);
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
        Intent intent = new Intent(this, ServerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        //showSmallNotification("String title", "String message");
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
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        notificationBuilder.setDefaults(notification.defaults);
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



    private void showSmallNotification(String title, String message){
        String CHANNEL_ID = "CHANNEL_1";
        String CHANNEL_NAME = "Notification";

        Intent intent = new Intent(this, ServerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // I removed one of the semi-colons in the next line of code
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        inboxStyle.addLine(message);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // I would suggest that you use IMPORTANCE_DEFAULT instead of IMPORTANCE_HIGH
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setLightColor(Color.BLUE);
            channel.enableLights(true);
            /* /channel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getApplicationContext().getPackageName() + "/" + R.raw.notification),
            new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build());*/

            //channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI),
            //channel.canShowBadge();
            // Did you mean to set the property to enable Show Badge?
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setVibrate(new long[]{0, 100})
                .setPriority(Notification.PRIORITY_MAX)
                .setLights(Color.BLUE, 3000, 3000)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(inboxStyle)
                //.setLargeIcon(BitmapFactory.decodeResource(this.getResources(), icon))
                .setContentText(message);
        // Removed .build() since you use it below...no need to build it twice

        // Don't forget to set the ChannelID!!
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            notificationBuilder.setChannelId(CHANNEL_ID);
        }

        notificationManager.notify(CHANNEL_ID, 1, notificationBuilder.build());
    }

    private void sendNotificationOld(String message, String tick, String title, boolean sound, boolean vibrate, int iconID) {

        /*Runnable runnable = new Runnable() {
            @Override
            public void run() {}
        };
        new Thread( new Runnable() {
            @Override public void run() {
            }
        }
        ).start();

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
            }
        });*/
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Run whatever background code you want here.
                Intent intent = new Intent(ServerActivity.this, ServerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(ServerActivity.this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                Notification notification = new Notification();

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ServerActivity.this);

                if (sound) {
                    notification.defaults |= Notification.DEFAULT_SOUND;
                }

                if (vibrate) {
                    notification.defaults |= Notification.DEFAULT_VIBRATE;
                }

                notificationBuilder.setDefaults(notification.defaults);
                notificationBuilder.setSmallIcon(iconID)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setTicker(tick)
                        .setContentIntent(pendingIntent);

                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                notificationManager.notify(1,
                        //* ID of notification
                notificationBuilder.build());

    }
},  1000);

        /*new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                ShowNotification();
            }
        });*/

    }

    class StartCommunication implements Runnable {

        @Override
        public void run()
        {
            try {
                if (ss!=null){
                    if (!ss.isClosed()){
                        ss = new ServerSocket(55555);
                    }
                }else  ss = new ServerSocket(55555);
                /*ss = new ServerSocket(55555);*/


                String str2 = "Serveur démarré sur " + ip + "\n";
                //sendChatMessage(str2,false);
                listMess.add(str2);
                for (String s : listMess){
                    sendChatMessage(s,false);
                }
                inConnexion = true;

                send_but.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String message = messagespace.getText().toString();
                        if(!message.equals("")){
                            messagespace.setText("");
                            message = name + ": " + message;
                            final String mes = message;
                            String str2 = mes + "\n";
                            sendChatMessage(str2,true);
                            listMess.add(str2);
                            for(int i=0;i<num;i++) {
                                Socket temp = arr[i];
                                SendToAll thread = new SendToAll(temp,message);
                                thread.start();
                            }
                        }

                    }
                });

                //messagespace.setImeOptions(EditorInfo.IME_ACTION_SEND);
                messagespace.setOnKeyListener(new View.OnKeyListener() {
                    @RequiresApi(api = Build.VERSION_CODES.R)
                    @Override
                    public boolean onKey(View view, int i, KeyEvent keyEvent) {

                        if(keyEvent.getAction()==KeyEvent.ACTION_DOWN && i == keyEvent.KEYCODE_ENTER){
                            String message = messagespace.getText().toString();
                            if(!message.equals("")){
                                messagespace.setText("");
                                message = name + ": " + message;
                                final String mes = message;
                                String str2 = mes + "\n";
                                sendChatMessage(str2,true);

                                if(inConnexion){
                                    listMess.add(str2);
                                }
                                for(int i2=0;i2<num;i2++) {
                                    Socket temp = arr[i2];
                                    SendToAll thread = new SendToAll(temp,message);
                                    thread.start();
                                }
                            }
                            return true;
                        }
                        return false;
                    }
                });

                while (true){
                    Socket clientSocket = ss.accept( );
                    ServerThread thread = new ServerThread(clientSocket);
                    arr[num++] = clientSocket;
                    thread.start( );
                }

            }catch(final Exception e){

                sendChatMessage(e.toString(),true);

            }
        }
    }

    class SendToAll extends Thread {

        Socket s;
        String msg;
        SendToAll(Socket s,String msg)
        {
            this.s = s;
            this.msg = msg;
        }

        public void run() {
            try{
                PrintStream ps = new PrintStream(s.getOutputStream());
                ps.println(msg);
                if(msg.equalsIgnoreCase("exit"))
                    for(int i=0;i<num;i++) {
                        if(arr[i] == s)
                        {
                            s.close();
                            break;
                        }
                    }
                ps.flush();
                //listMess.add(msg);
            }catch(final Exception e){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        chatspace.append(e.toString());
                    }
                });
            }
        }
    }

    class ServerThread extends Thread {

        Socket clientSocket;

        ServerThread(Socket cs){
            clientSocket = cs;
        }

        public void run(){
            try{
                String str;
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                while(true)
                {
                    str = br.readLine( );
                    if(str.startsWith("Ex1+:"))
                    {
                        str = str.substring(5,str.length()) + " Left";
                        for(int i=0;i<num;i++) {
                            if(arr[i] == clientSocket)
                                for(int j=i;j<num - 1;j++)
                                    arr[j] = arr[j+1];
                            num--;

                        }
                        clientSocket.close();
                        for(int i=0;i<num;i++) {
                            Socket temp = arr[i];
                            SendToAll thread = new SendToAll(temp,str);
                            thread.start();
                        }
                        String str2 = str + "\n";
                        sendChatMessage(str2,true);
                        listMess.add(str2);
                        inConnexion = false;
                        break;
                    }

                    if(str.substring(0,6).equals("j01ne6")){
                        str = str.substring(7,str.length()) + " rejoint";
                    }

                    sendChatMessage(str + "\n",true);
                    listMess.add(str + "\n");
                    Log.d("ServerActivity:rejoint", str);



                    for(int i=0;i<num;i++) {
                        Socket temp = arr[i];
                        SendToAll thread = new SendToAll(temp,str);
                        thread.start();
                    }

                }
            }
            catch(final Exception e){
                try{

                    for(int i=0;i<num;i++) {
                        if(arr[i] == clientSocket)
                            for(int j=i;j<num - 1;j++)
                                arr[j] = arr[j+1];
                    }
                    num--;
                    clientSocket.close();
                }catch(Exception ex){ex.printStackTrace();}}
        }
    }
}
