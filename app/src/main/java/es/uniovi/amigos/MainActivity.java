package es.uniovi.amigos;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity{
    private static final long UPDATE_PERIOD = 5000;

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;  // Este atributo guarda una referencia al objeto MapView
    // a través del cual podremos manipular el mapa que se muestre
    private List<Amigo> amigos = new ArrayList<Amigo>();
    String url = "http://192.168.1.84:80/api/amigos";
    Timer timer = new Timer();
    TimerTask updateAmigos = new UpdateAmigosPosition();

    class UpdateAmigosPosition extends TimerTask {
        public void run() {
            getAmigosList();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Leer la configuración de la aplicación
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // Crear el mapa desde el layout y asignarle la fuente de la que descargará las imágenes del mapa
        setContentView(R.layout.activity_main);
        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        // Solicitar al usuario los permisos "peligrosos". El usuario debe autorizarlos
        // Cuando los autorice, Android llamará a la función onRequestPermissionsResult
        // que implementamos más adelante
        requestPermissionsIfNecessary(new String[]{
                // WRITE_EXTERNAL_STORAGE este permiso es necesario para guardar las imagenes del mapa
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
        centrarMapaEnEuropa();

        timer.scheduleAtFixedRate(updateAmigos, 0, UPDATE_PERIOD);

        getAmigosList();
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Esta función será invocada cuando el usuario conceda los permisos
        // De momento hay que dejarla como está
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        // Itera por la lista de permisos y los va solicitando de uno en uno
        // a menos que estén ya concedidos (de ejecuciones previas)
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
    void centrarMapaEnEuropa() {
        // Esta función mueve el centro del mapa a Paris y ajusta el zoom
        // para que se vea Europa
        IMapController mapController = map.getController();
        mapController.setZoom(5.5);
        GeoPoint startPoint = new GeoPoint(48.8583, 2.2944);
        mapController.setCenter(startPoint);
    }

    public void getAmigosList() {
        amigos.clear();
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>(){
                    @Override
                    public void onResponse(JSONArray response){
                        System.out.println("Volley OK: " + response);
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);
                                String nombre = obj.getString("name");
                                double latitud = obj.getDouble("lati");
                                double longitud = obj.getDouble("longi");
                                Amigo a = new Amigo(nombre, latitud, longitud);
                                amigos.add(a);
                                }
                        } catch(org.json.JSONException e){}
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError e){
                        System.out.println("Volley: ERROR: " + e);
                    }
                }
        );
        queue.add(request);
    }

    private void addMarker(double latitud, double longitud, String name) {
        GeoPoint coords = new GeoPoint(latitud, longitud);
        Marker startMarker = new Marker(map);
        startMarker.setPosition(coords);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        startMarker.setTitle(name);
        startMarker.setIcon(getResources().getDrawable(R.drawable.icono));
        map.getOverlays().add(startMarker);
    }

    public void paintAmigosList(List<Amigo> amigos){
        map.getOverlays().clear();
        int i;
        for(i=0; i<amigos.size(); i++){
            Amigo a = new Amigo(amigos.get(i));
            addMarker(a.getLatitud(), a.getLongitud(), a.nombre);
        }
        map.getController().scrollBy(0,0);
    }
}