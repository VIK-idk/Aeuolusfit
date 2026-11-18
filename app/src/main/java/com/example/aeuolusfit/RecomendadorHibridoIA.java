package com.example.aeuolusfit;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecomendadorHibridoIA {

    private static final String API_KEY = "TU_GEMINI_API_KEY_AQUI";
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    // Interfaz para la recomendación de la tarjeta principal
    public interface OnResultadoRecomendacion {
        void onRecomendacionLista(String titulo, String descripcion, boolean esDesdeIA);
    }

    // Interfaz para la respuesta del Chatbot
    public interface OnChatRespuestaListener {
        void onRespuesta(String textoIA);
    }

    // =========================================================================
    // 1. OBTENER RECOMENDACIÓN DE CONJUNTO DIARIO (Soluciona los errores en HoyFragment)
    // =========================================================================
    public static void obtenerRecomendacion(Familiar familiar, Aplicacion app, List<Prenda> prendas, OnResultadoRecomendacion callback) {
        if (familiar.getUltimoTituloConjunto() != null && !familiar.getUltimoTituloConjunto().isEmpty()) {
            callback.onRecomendacionLista(familiar.getUltimoTituloConjunto(), familiar.getUltimaDescripcionConjunto(), true);
        }

        new Thread(() -> {
            boolean exitoIA = false;
            String tituloFinal = "";
            String descFinal = "";

            if (!API_KEY.startsWith("TU_")) {
                try {
                    String prompt = "Eres el asistente inteligente de AeolusFit. Genera una recomendación de conjunto.\n"
                            + "Usuario: " + familiar.getNombre() + " (" + familiar.getEdad() + " años, suele tener " + familiar.getSensacionTermica() + ").\n"
                            + "Clima actual: " + (app.getTemperaturaActual() != null ? app.getTemperaturaActual() : 18) + "°C, "
                            + "Humedad: " + (app.getHumedadActual() != null ? app.getHumedadActual() : 60) + "%.\n"
                            + "Previsión: " + app.getConsejoLluvia() + ".\n"
                            + "Prendas disponibles en armario: " + (prendas != null ? prendas.size() : 0) + ".\n\n"
                            + "Responde ÚNICAMENTE un JSON con este formato:\n"
                            + "{\"titulo\": \"Conjunto breve (máx 6 palabras)\", \"descripcion\": \"Consejo de vestimenta de 2 frases teniendo en cuenta su preferencia térmica y la lluvia.\"}";

                    JSONObject textPart = new JSONObject().put("text", prompt);
                    JSONArray parts = new JSONArray().put(textPart);
                    JSONObject content = new JSONObject().put("parts", parts);
                    JSONArray contents = new JSONArray().put(content);
                    JSONObject payload = new JSONObject().put("contents", contents);

                    URL url = new URL(ENDPOINT);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.close();

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();

                        JSONObject respJson = new JSONObject(sb.toString());
                        String rawText = respJson.getJSONArray("candidates")
                                .getJSONObject(0).getJSONObject("content")
                                .getJSONArray("parts").getJSONObject(0).getString("text");

                        rawText = rawText.replace("```json", "").replace("```", "").trim();
                        JSONObject parsed = new JSONObject(rawText);

                        tituloFinal = parsed.getString("titulo");
                        descFinal = parsed.getString("descripcion");
                        exitoIA = true;
                    }
                    conn.disconnect();
                } catch (Exception ignored) {
                    exitoIA = false;
                }
            }

            if (!exitoIA) {
                String[] local = calcularRecomendacionLocal(familiar, app);
                tituloFinal = local[0];
                descFinal = local[1];
            }

            guardarMemoriaFamiliar(familiar, tituloFinal, descFinal);

            String fTit = tituloFinal;
            String fDesc = descFinal;
            boolean fExito = exitoIA;
            callback.onRecomendacionLista(fTit, fDesc, fExito);
        }).start();
    }

    private static String[] calcularRecomendacionLocal(Familiar familiar, Aplicacion app) {
        double temp = app.getTemperaturaActual() != null ? app.getTemperaturaActual() : 18.0;
        boolean friolero = familiar.getSensacionTermica().contains("frío");
        int lluvia = app.getProbLluviaTarde();

        String tit;
        String desc;

        if (temp < 15) {
            tit = friolero ? "Chaqueta térmica, jersey y pantalón grueso" : "Sudadera de algodón y pantalón largo";
            desc = "Día fresco. Abrigamos especialmente a " + familiar.getNombre() + " según su preferencia térmica.";
        } else if (temp < 23) {
            tit = friolero ? "Camisa de manga larga y chaqueta ligera" : "Camiseta suave y pantalón cómodo";
            desc = "Temperatura suave. Conjunto equilibrado para la jornada de " + familiar.getNombre() + ".";
        } else {
            tit = "Ropa fresca y transpirable";
            desc = "Ambiente cálido. Prendas ligeras para evitar el exceso de calor.";
        }

        if (lluvia >= 40) {
            desc += " No olvides añadir un chubasquero o paraguas por previsión de lluvia.";
        }

        return new String[]{tit, desc};
    }

    private static void guardarMemoriaFamiliar(Familiar f, String titulo, String desc) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        f.setUltimoTituloConjunto(titulo);
        f.setUltimaDescripcionConjunto(desc);

        String fecha = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date());
        String nuevaEntrada = fecha + ": Sugerido " + titulo;

        List<String> hist = f.getHistorialMemoriaIA();
        if (!hist.contains(nuevaEntrada)) {
            if (hist.size() >= 5) hist.remove(0);
            hist.add(nuevaEntrada);
        }

        Map<String, Object> update = new HashMap<>();
        update.put("ultimoTituloConjunto", titulo);
        update.put("ultimaDescripcionConjunto", desc);
        update.put("historialMemoriaIA", hist);

        if ("titular".equals(f.getId())) {
            FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid())
                    .set(update, SetOptions.merge());
        } else {
            FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid())
                    .collection("familiares").document(f.getId())
                    .set(update, SetOptions.merge());
        }
    }

    // =========================================================================
    // 2. CONVERSACIÓN EN EL CHATBOT FLOTANTE
    // =========================================================================
    public static void conversarConIA(Familiar familiar, String mensajeUsuario, Aplicacion app, List<MensajeChat> ultimosMensajes, OnChatRespuestaListener listener) {
        new Thread(() -> {
            String respuestaTexto = "";

            if (!API_KEY.startsWith("TU_")) {
                try {
                    JSONObject contextoSistema = new JSONObject();
                    contextoSistema.put("usuario_nombre", familiar.getNombre());
                    contextoSistema.put("usuario_edad", familiar.getEdad());
                    contextoSistema.put("usuario_sensacion_habitual", familiar.getSensacionTermica());
                    contextoSistema.put("ciudad", app.getCiudadActual());
                    contextoSistema.put("temperatura_actual", app.getTemperaturaActual() != null ? app.getTemperaturaActual() : 18);
                    contextoSistema.put("humedad_actual", app.getHumedadActual() != null ? app.getHumedadActual() : 60);
                    contextoSistema.put("temperatura_max_hoy", app.getMaxTemp());
                    contextoSistema.put("temperatura_min_hoy", app.getMinTemp());
                    contextoSistema.put("probabilidad_lluvia_tarde", app.getProbLluviaTarde());
                    contextoSistema.put("consejo_clima_hoy", app.getConsejoLluvia());
                    contextoSistema.put("conjunto_asignado", familiar.getUltimoTituloConjunto());

                    JSONArray historialArr = new JSONArray();
                    if (ultimosMensajes != null) {
                        for (MensajeChat m : ultimosMensajes) {
                            JSONObject obj = new JSONObject();
                            obj.put("emisor", m.isEsUsuario() ? "Usuario" : "Asistente");
                            obj.put("texto", m.getTexto());
                            historialArr.put(obj);
                        }
                    }

                    String prompt = "Eres el asistente inteligente de AeolusFit, un sistema de armario inteligente para el hogar.\n"
                            + "Dispones del siguiente contexto técnico y meteorológico en tiempo real:\n"
                            + contextoSistema.toString() + "\n\n"
                            + "Historial reciente de la conversación:\n"
                            + historialArr.toString() + "\n\n"
                            + "Nuevo mensaje del usuario: \"" + mensajeUsuario + "\"\n\n"
                            + "Instrucciones:\n"
                            + "- Responde a cualquier consulta sobre el clima, la ropa, la sensación térmica o sugerencias usando los datos proporcionados.\n"
                            + "- Si te da feedback sobre el conjunto (pasó frío, calor, etc.), muestra empatía y explica cómo lo adaptarás.\n"
                            + "- Mantén un tono natural, cercano y conciso (máximo 2-3 oraciones). Sin viñetas ni Markdown pesado.";

                    JSONObject textPart = new JSONObject().put("text", prompt);
                    JSONArray parts = new JSONArray().put(textPart);
                    JSONObject content = new JSONObject().put("parts", parts);
                    JSONArray contents = new JSONArray().put(content);
                    JSONObject payload = new JSONObject().put("contents", contents);

                    URL url = new URL(ENDPOINT);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.close();

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();

                        JSONObject respJson = new JSONObject(sb.toString());
                        respuestaTexto = respJson.getJSONArray("candidates")
                                .getJSONObject(0).getJSONObject("content")
                                .getJSONArray("parts").getJSONObject(0).getString("text").trim();
                    }
                    conn.disconnect();
                } catch (Exception ignored) {}
            }

            if (respuestaTexto.isEmpty()) {
                respuestaTexto = "He tomado nota de tu comentario para adaptar las sugerencias de tu armario en tu perfil.";
            }

            guardarMensajeEnFirestore(familiar.getId(), respuestaTexto, false);

            String finalRespuesta = respuestaTexto;
            listener.onRespuesta(finalRespuesta);
        }).start();
    }

    public static void guardarMensajeEnFirestore(String idFamiliar, String texto, boolean esUsuario) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> mensaje = new HashMap<>();
        mensaje.put("texto", texto);
        mensaje.put("esUsuario", esUsuario);
        mensaje.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid())
                .collection("familiares").document(idFamiliar)
                .collection("mensajesChat")
                .add(mensaje);
    }
}