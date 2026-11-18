package com.example.aeuolusfit;

import android.app.Application;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Aplicacion extends Application {

    public interface OnClimaChangeListener {
        void onClimaActualizado();
    }

    // Variables globales meteorológicas
    private Double temperaturaActual = null;
    private Double humedadActual = null;
    private String estadoCielo = "Consultando...";
    private String ciudadActual = "Valencia";
    private int maxTemp = 0;
    private int minTemp = 0;
    private int probLluviaManana = 0;
    private int probLluviaTarde = 0;
    private int probLluviaNoche = 0;
    private String consejoLluvia = "Revisando previsión...";
    private boolean esLecturaHardware = false;

    private final List<OnClimaChangeListener> listeners = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        // Al arrancar la app, lanzamos la consulta meteorológica global en un hilo secundario (Ud7)
        consultarMeteorologiaGlobal(39.4699, -0.3763, "Valencia");
    }

    public void registrarListener(OnClimaChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void desregistrarListener(OnClimaChangeListener listener) {
        listeners.remove(listener);
    }

    private void notificarCambio() {
        for (OnClimaChangeListener l : listeners) {
            if (l != null) l.onClimaActualizado();
        }
    }

    // Actualización manual desde el hardware M001
    public synchronized void actualizarDesdeHardware(double temp, double hum) {
        this.esLecturaHardware = true;
        this.temperaturaActual = temp;
        this.humedadActual = hum;
        this.ciudadActual = "Módulo M001";
        this.estadoCielo = temp < 15 ? "Fresco exterior" : "Clima templado";
        notificarCambio();
    }

    // Consulta de previsión horaria completa (Open-Meteo)
    public void consultarMeteorologiaGlobal(double lat, double lon, String ciudad) {
        new Thread(() -> {
            HttpURLConnection conexion = null;
            BufferedReader lector = null;
            try {
                String endpoint = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                        + "&current=temperature_2m,relative_humidity_2m,weather_code"
                        + "&hourly=precipitation_probability,weather_code"
                        + "&daily=temperature_2m_max,temperature_2m_min&timezone=auto";

                URL url = new URL(endpoint);
                conexion = (HttpURLConnection) url.openConnection();
                conexion.setRequestMethod("GET");
                conexion.setConnectTimeout(5000);
                conexion.setReadTimeout(5000);

                if (conexion.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    lector = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String linea;
                    while ((linea = lector.readLine()) != null) {
                        sb.append(linea);
                    }

                    JSONObject json = new JSONObject(sb.toString());

                    // 1. Datos actuales
                    JSONObject current = json.getJSONObject("current");
                    double temp = current.getDouble("temperature_2m");
                    double hum = current.getDouble("relative_humidity_2m");
                    int code = current.getInt("weather_code");

                    // 2. Máxima y Mínima del día
                    JSONObject daily = json.getJSONObject("daily");
                    int max = (int) Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0));
                    int min = (int) Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0));

                    // 3. Análisis de lluvia por tramos (Mañana: 08-14h, Tarde: 14-20h, Noche: 20-00h)
                    JSONObject hourly = json.getJSONObject("hourly");
                    JSONArray probLluviaHoras = hourly.getJSONArray("precipitation_probability");

                    int maxManana = 0;
                    int maxTarde = 0;
                    int maxNoche = 0;

                    for (int h = 0; h < 24 && h < probLluviaHoras.length(); h++) {
                        int p = probLluviaHoras.getInt(h);
                        if (h >= 7 && h < 14) {
                            if (p > maxManana) maxManana = p;
                        } else if (h >= 14 && h < 20) {
                            if (p > maxTarde) maxTarde = p;
                        } else if (h >= 20) {
                            if (p > maxNoche) maxNoche = p;
                        }
                    }

                    // Construcción del consejo inteligente
                    String consejo;
                    if (maxManana >= 45 && maxTarde >= 45) {
                        consejo = "☂️  Lluvia prevista mañana (" + maxManana + "%) y tarde (" + maxTarde + "%): paraguas obligatorio todo el día.";
                    } else if (maxManana >= 45) {
                        consejo = "☂️  Lluvia por la mañana (" + maxManana + "%): sal con paraguas.";
                    } else if (maxTarde >= 45) {
                        consejo = "☂️  Mañana seca pero lloverá por la tarde (" + maxTarde + "%): si estás fuera todo el día, lleva paraguas.";
                    } else if (maxNoche >= 45) {
                        consejo = "☂️  Posible lluvia al final del día (" + maxNoche + "%): tenlo en cuenta si vuelves tarde.";
                    } else {
                        consejo = "☀️  Baja probabilidad de lluvia todo el día: no necesitas paraguas.";
                    }

                    synchronized (this) {
                        if (!esLecturaHardware) {
                            this.temperaturaActual = temp;
                            this.humedadActual = hum;
                            this.ciudadActual = ciudad;
                            this.estadoCielo = interpretarCodigoWMO(code);
                        }
                        this.maxTemp = max;
                        this.minTemp = min;
                        this.probLluviaManana = maxManana;
                        this.probLluviaTarde = maxTarde;
                        this.probLluviaNoche = maxNoche;
                        this.consejoLluvia = consejo;
                    }

                    notificarCambio();
                }
            } catch (Exception ignored) {
            } finally {
                try {
                    if (lector != null) lector.close();
                    if (conexion != null) conexion.disconnect();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private String interpretarCodigoWMO(int code) {
        if (code == 0) return "Cielo despejado";
        if (code >= 1 && code <= 3) return "Parcialmente nublado";
        if (code >= 45 && code <= 48) return "Niebla";
        if (code >= 51 && code <= 67) return "Lluvia ligera";
        if (code >= 71 && code <= 77) return "Nieve";
        if (code >= 80 && code <= 82) return "Chubascos";
        if (code >= 95) return "Tormenta";
        return "Nublado";
    }

    // Getters globales
    public Double getTemperaturaActual() { return temperaturaActual; }
    public Double getHumedadActual() { return humedadActual; }
    public String getEstadoCielo() { return estadoCielo; }
    public String getCiudadActual() { return ciudadActual; }
    public int getMaxTemp() { return maxTemp; }
    public int getMinTemp() { return minTemp; }
    public String getConsejoLluvia() { return consejoLluvia; }
    public int getProbLluviaTarde() { return probLluviaTarde; }
}