package com.example.aeuolusfit;

public class Prenda {
    private String id;
    private String nombre;
    private String categoria;
    private String climaIdeal;
    private String uidUsuario;

    public Prenda() {} // Necesario para Firestore

    public Prenda(String id, String nombre, String categoria, String climaIdeal, String uidUsuario) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.climaIdeal = climaIdeal;
        this.uidUsuario = uidUsuario;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getClimaIdeal() { return climaIdeal; }
    public String getUidUsuario() { return uidUsuario; }
}