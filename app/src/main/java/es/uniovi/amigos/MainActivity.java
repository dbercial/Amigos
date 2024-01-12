package es.uniovi.amigos;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.EditText;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
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
    public int id = -1;
    String url = "http://192.168.1.84:80/api/amigos";
    String url2 = "http://192.168.1.84:80/api/amigo/byName/" + this.mUserName ;
    String url3= "http://192.168.1.84:80/api/amigo/" + id;
    Timer timer = new Timer();
    TimerTask updateAmigos = new UpdateAmigosPosition();

    String mUserName = null;

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
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        });
        centrarMapaEnEuropa();
        timer.scheduleAtFixedRate(updateAmigos, 0, UPDATE_PERIOD);
        getAmigosList();
        SetupLocation();
        askUserName();
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

    public void askUserName() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Settings");
        alert.setMessage("User name:");

        // Crear un EditText para obtener el nombre
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                mUserName = input.getText().toString();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                // Canceled.
            }
        });

        alert.show();
    }

    void SetupLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            // Verificar por si acaso si tenemos el permiso, y si no
            // no hacemos nada
            return;
        }

        // Se debe adquirir una referencia al Location Manager del sistema
        LocationManager locationManager =
                (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Se obtiene el mejor provider de posición
        Criteria criteria = new Criteria();
        String  provider = locationManager.getBestProvider(criteria, false);

        // Se crea un listener de la clase que se va a definir luego
        MyLocationListener locationListener = new MyLocationListener();

        // Se registra el listener con el Location Manager para recibir actualizaciones
        // En este caso pedimos que nos notifique la nueva localización
        // si el teléfono se ha movido más de 10 metros
        locationManager.requestLocationUpdates(provider, 0, 10, locationListener);

        // Comprobar si se puede obtener la posición ahora mismo
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            // La posición actual es location
        } else {
            // Actualmente no se puede obtener la posición
        }
    }

    // Se define un Listener para escuchar por cambios en la posición
    class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            // Se llama cuando hay una nueva posición para ese location provider
            double lati = location.getLatitude();
            double longi = location.getLongitude();
            // AQUI HABRA QUE AÑADIR CODIGO MÁS ADELANTE
            // para actualizar en el backend la posición de este teléfono
            // ...
        }

        // El resto de métodos que debemos implementar los podemos dejar vacíos
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        // Se llama cuando se activa el provider
        @Override
        public void onProviderEnabled(String provider) {}
        // Se llama cuando se desactiva el provider
        @Override
        public void onProviderDisabled(String provider) {}
    }

    void getAmigoId(){
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,url2,null,
        new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response){
                System.out.println("Volley OK: " + response);
                try {
                    id= response.getInt("id");
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

    public void actualizaPosicion(double latitud, double longitud){
        if(id != -1){
            RequestQueue queue = Volley.newRequestQueue(this);
            try{
                JSONObject jsonToSend = new JSONObject();
                jsonToSend.put("lati", latitud);
                jsonToSend.put("longi", longitud);

                JsonObjectRequest objectRequest = new JsonObjectRequest(
                        Request.Method.PUT,url3,jsonToSend,null,null);
                queue.add(objectRequest);
            }
            catch(org.json.JSONException e){}
        }
    }

}