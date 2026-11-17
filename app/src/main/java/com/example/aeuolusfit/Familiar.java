package com.example.aeuolusfit;

public class Familiar {
    private String id;
    private String nombre;
    private String rol;
    private String colorHex;
    private boolean seleccionado;

    public Familiar() {} // Necesario para Firestore

    public Familiar(String id, String nombre, String rol, String colorHex, boolean seleccionado) {
        this.id = id;
        this.nombre = nombre;
        this.rol = rol;
        this.colorHex = colorHex;
        this.seleccionado = seleccionado;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public String getColorHex() { return colorHex; }
    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    public String getInicial() {
        if (nombre != null && !nombre.trim().isEmpty()) {
            return String.valueOf(nombre.trim().charAt(0)).toUpperCase();
        }
        return "U";
    }
}