package com.example.aeuolusfit;

import java.util.ArrayList;
import java.util.List;

public class Familiar {
    private String id;
    private String nombre;
    private String rol;
    private String colorHex;
    private boolean seleccionado;
    private int edad;
    private String sensacionTermica;
    private String tipoUso;
    private List<String> historialMemoriaIA = new ArrayList<>();
    private String ultimoTituloConjunto;
    private String ultimaDescripcionConjunto;

    public Familiar() {} // Constructor vacío para Firestore

    public Familiar(String id, String nombre, String rol, String colorHex, boolean seleccionado, int edad, String sensacionTermica) {
        this.id = id;
        this.nombre = nombre;
        this.rol = rol;
        this.colorHex = colorHex;
        this.seleccionado = seleccionado;
        this.edad = edad;
        this.sensacionTermica = sensacionTermica;
        this.tipoUso = "personal";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public String getSensacionTermica() {
        return (sensacionTermica != null && !sensacionTermica.isEmpty()) ? sensacionTermica : "Suele tener frío";
    }
    public void setSensacionTermica(String sensacionTermica) { this.sensacionTermica = sensacionTermica; }

    public String getTipoUso() { return tipoUso; }
    public void setTipoUso(String tipoUso) { this.tipoUso = tipoUso; }

    public List<String> getHistorialMemoriaIA() {
        if (historialMemoriaIA == null) historialMemoriaIA = new ArrayList<>();
        return historialMemoriaIA;
    }
    public void setHistorialMemoriaIA(List<String> historialMemoriaIA) { this.historialMemoriaIA = historialMemoriaIA; }

    public String getUltimoTituloConjunto() { return ultimoTituloConjunto; }
    public void setUltimoTituloConjunto(String ultimoTituloConjunto) { this.ultimoTituloConjunto = ultimoTituloConjunto; }

    public String getUltimaDescripcionConjunto() { return ultimaDescripcionConjunto; }
    public void setUltimaDescripcionConjunto(String ultimaDescripcionConjunto) { this.ultimaDescripcionConjunto = ultimaDescripcionConjunto; }

    public String getInicial() {
        if (nombre != null && !nombre.trim().isEmpty()) {
            return String.valueOf(nombre.trim().charAt(0)).toUpperCase();
        }
        return "—";
    }
}