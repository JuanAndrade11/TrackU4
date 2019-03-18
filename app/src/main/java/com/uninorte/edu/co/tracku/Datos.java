package com.uninorte.edu.co.tracku;

import com.google.gson.annotations.SerializedName;

public class Datos {
    @SerializedName("idUsuario")
    private String idUsuario;

    @SerializedName("Nombre")
    private String Nombre;

    @SerializedName("Clave")
    private String Clave;

    public void setIdUsuario(String idUsuario){
        this.idUsuario = idUsuario;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setClave(String clave) {
        Clave = clave;
    }

    public String getClave() {
        return Clave;
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }

    public String getNombre() {
        return Nombre;
    }
}
