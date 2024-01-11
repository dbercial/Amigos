package es.uniovi.amigos;

import java.util.List;

public class Amigo {
    String nombre;
    double latitud;
    double longitud;


    public Amigo(String n, double lat, double lon){
        this.nombre = n;
        this.latitud = lat;
        this.longitud = lon;
    }
    public Amigo(Amigo a){
        this.nombre = a.getNombre();
        this.latitud = a.getLatitud();
        this.longitud = a.getLongitud();
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public String getNombre() {
        return nombre;
    }
    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }
    public double getLatitud() {
        return latitud;
    }
    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }
    public double getLongitud() {
        return longitud;
    }


}