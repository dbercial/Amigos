package es.uniovi.amigos;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String token) {
        System.out.println("FCM Refreshed token: " + token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        System.out.println("FCM: From " + remoteMessage.getFrom());

        // El mensaje en sí son datos que pueda enviar el servidor
        // en forma de cadena que contiene una especie de JSON
        // No lo usaremos, por lo que vendrá vacío
        if (remoteMessage.getData().size() > 0) {
            System.out.println("FCM: Message = " + remoteMessage.getData());
        }

        // La notificación es el texto que aparecería en la barra
        // de notificaciones, el que se especifica en la consola
        // web de Firebase
        if (remoteMessage.getNotification() != null) {
            System.out.println("FCM: Notification Body ="
                    + remoteMessage.getNotification().getBody());
        }
        Intent updateFromServer = new Intent("updateFromServer");
        LocalBroadcastManager.getInstance(this).sendBroadcast(updateFromServer);
    }
}
