package com.example.aeuolusfit;

public class Prenda {
    private String id;
    private String nombre;
    private String categoria;
    private String climaIdeal;
    private String colorHex;
    private String uidUsuario;
    private String idFamiliar; // "titular", "fam_laura", etc.

    public Prenda() {}

    public Prenda(String id, String nombre, String categoria, String climaIdeal, String colorHex, String uidUsuario, String idFamiliar) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.climaIdeal = climaIdeal;
        this.colorHex = colorHex;
        this.uidUsuario = uidUsuario;
        this.idFamiliar = idFamiliar;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getClimaIdeal() { return climaIdeal; }
    public void setClimaIdeal(String climaIdeal) { this.climaIdeal = climaIdeal; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getUidUsuario() { return uidUsuario; }
    public void setUidUsuario(String uidUsuario) { this.uidUsuario = uidUsuario; }

    public String getIdFamiliar() { return idFamiliar; }
    public void setIdFamiliar(String idFamiliar) { this.idFamiliar = idFamiliar; }
}