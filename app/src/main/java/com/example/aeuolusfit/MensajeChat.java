package com.example.aeuolusfit;

public class MensajeChat {
    private String texto;
    private boolean esUsuario; // true = Usuario, false = Asistente IA
    private String hora;

    public MensajeChat() {}

    public MensajeChat(String texto, boolean esUsuario, String hora) {
        this.texto = texto;
        this.esUsuario = esUsuario;
        this.hora = hora;
    }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public boolean isEsUsuario() { return esUsuario; }
    public void setEsUsuario(boolean esUsuario) { this.esUsuario = esUsuario; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
}