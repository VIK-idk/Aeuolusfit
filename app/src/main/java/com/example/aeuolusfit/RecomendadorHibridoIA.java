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
    // 1. OBTENER RECOMENDACIÓN DE CONJUNTO DIARIO CON AJUSTE TÉRMICO
    // =========================================================================
    public static void obtenerRecomendacion(Familiar familiar, Aplicacion app, List<Prenda> prendas, OnResultadoRecomendacion callback) {

        // Primero consultamos el ajuste térmico personalizado del perfil en Firestore
        obtenerAjusteTermicoPerfil(familiar, ajusteTermico -> {

            if (familiar.getUltimoTituloConjunto() != null && !familiar.getUltimoTituloConjunto().isEmpty()) {
                callback.onRecomendacionLista(familiar.getUltimoTituloConjunto(), familiar.getUltimaDescripcionConjunto(), true);
            }

            new Thread(() -> {
                boolean exitoIA = false;
                String tituloFinal = "";
                String descFinal = "";

                // Temperatura real del entorno modificada por la sensibilidad del perfil
                double tempReal = app.getTemperaturaActual() != null ? app.getTemperaturaActual() : 18.0;
                double temperaturaAjustada = tempReal + ajusteTermico;

                if (!API_KEY.startsWith("TU_")) {
                    try {
                        String prompt = "Eres el asistente inteligente de AeolusFit. Genera una recomendación de conjunto totalmente adaptada.\n"
                                + "Usuario: " + familiar.getNombre() + ".\n"
                                + "Temperatura real exterior: " + tempReal + "°C, Humedad: " + (app.getHumedadActual() != null ? app.getHumedadActual() : 60) + "%.\n"
                                + "Sensibilidad térmica (Ajuste aplicado): " + ajusteTermico + "°C (Temperatura percibida por el usuario: " + temperaturaAjustada + "°C).\n"
                                + "Previsión: " + app.getConsejoLluvia() + ".\n"
                                + "Prendas disponibles en armario: " + (prendas != null ? prendas.size() : 0) + ".\n\n"
                                + "Instrucción crucial: Basándote en su temperatura percibida (" + temperaturaAjustada + "°C), recomfréndale un conjunto específico (si siente mucho frío por un ajuste bajo, abrígalo con capas pesadas; si siente mucho calor, usa ropa muy ligera).\n"
                                + "Responde ÚNICAMENTE un JSON con este formato:\n"
                                + "{\"titulo\": \"Conjunto breve (máx 6 palabras)\", \"descripcion\": \"Consejo de vestimenta de 2 frases teniendo en cuenta su sensibilidad térmica y la lluvia.\"}";

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
                    String[] local = calcularRecomendacionLocalConAjuste(temperaturaAjustada, familiar, app);
                    tituloFinal = local[0];
                    descFinal = local[1];
                }

                guardarMemoriaFamiliar(familiar, tituloFinal, descFinal);

                callback.onRecomendacionLista(tituloFinal, descFinal, exitoIA);
            }).start();
        });
    }

    // Método auxiliar para leer el ajuste térmico de Firestore de cada perfil
    private static void obtenerAjusteTermicoPerfil(Familiar familiar, OnAjusteLoadedListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            listener.onAjusteLoaded(0.0);
            return;
        }

        FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid())
                .collection("configuracion").document("perfiles")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Buscamos el perfil según el ID o nombre (ej: "perfil_1", "perfil_2", etc.)
                        Map<String, Object> perfilData = (Map<String, Object>) documentSnapshot.get(familiar.getId());
                        if (perfilData != null && perfilData.containsKey("ajuste")) {
                            double ajuste = ((Number) perfilData.get("ajuste")).doubleValue();
                            listener.onAjusteLoaded(ajuste);
                            return;
                        }
                    }
                    listener.onAjusteLoaded(0.0); // Valor por defecto si no lo encuentra
                })
                .addOnFailureListener(e -> listener.onAjusteLoaded(0.0));
    }

    public interface OnAjusteLoadedListener {
        void onAjusteLoaded(double ajuste);
    }

    private static String[] calcularRecomendacionLocalConAjuste(double temperaturaAjustada, Familiar familiar, Aplicacion app) {
        int lluvia = app.getProbLluviaTarde();
        String tit;
        String desc;

        if (temperaturaAjustada < 5) {
            tit = "Abrigo pesado, bufanda y capas térmicas";
            desc = "Clima extremadamente frío percibido para " + familiar.getNombre() + ". Se requiere máxima protección.";
        } else if (temperaturaAjustada < 15) {
            tit = "Chaqueta gruesa y jersey de manga larga";
            desc = "Ambiente fresco para la sensibilidad de " + familiar.getNombre() + ". Capas intermedias recomendadas.";
        } else if (temperaturaAjustada < 25) {
            tit = "Camiseta cómoda y pantalón ligero";
            desc = "Temperatura agradable y equilibrada para el confort de " + familiar.getNombre() + ".";
        } else {
            tit = "Ropa muy ligera y transpirable";
            desc = "Calor intenso percibido por " + familiar.getNombre() + ". Prioriza prendas frescas y ligeras.";
        }

        if (lluvia >= 40) {
            desc += " Lleva paraguas o chubasquero por previsión de lluvia.";
        }

        return new String[]{tit, desc};
    }

    // =========================================================================
    // 2. APRENDIZAJE ADAPTATIVO POR FEEDBACK (IA ADAPTA EL AJUSTE)
    // =========================================================================
    public static void registrarFeedbackYAdaptarAjuste(Familiar familiar, boolean pasoFrio) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid())
                .collection("configuracion").document("perfiles")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> perfilData = (Map<String, Object>) documentSnapshot.get(familiar.getId());
                        if (perfilData != null && perfilData.containsKey("ajuste")) {
                            double ajusteActual = ((Number) perfilData.get("ajuste")).doubleValue();

                            // Si pasó frío, restamos 1.0 grado (haciéndolo más friolero/sensible). Si pasó calor, sumamos 1.0
                            if (pasoFrio) {
                                ajusteActual -= 1.0;
                            } else {
                                ajusteActual += 1.0;
                            }

                            perfilData.put("ajuste", ajusteActual);

                            // Guardamos el nuevo ajuste aprendido en Firestore
                            FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid())
                                    .collection("configuracion").document("perfiles")
                                    .update(familiar.getId(), perfilData);
                        }
                    }
                });
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
    // 3. CONVERSACIÓN EN EL CHATBOT FLOTANTE
    // =========================================================================
    public static void conversarConIA(Familiar familiar, String mensajeUsuario, Aplicacion app, List<MensajeChat> ultimosMensajes, OnChatRespuestaListener listener) {

        // Detectar automáticamente si el usuario da feedback de temperatura en el chat
        String mensajeLower = mensajeUsuario.toLowerCase();
        if (mensajeLower.contains("frío") || mensajeLower.contains("frio")) {
            registrarFeedbackYAdaptarAjuste(familiar, true);
        } else if (mensajeLower.contains("calor")) {
            registrarFeedbackYAdaptarAjuste(familiar, false);
        }

        new Thread(() -> {
            String respuestaTexto = "";

            if (!API_KEY.startsWith("TU_")) {
                try {
                    JSONObject contextoSistema = new JSONObject();
                    contextoSistema.put("usuario_nombre", familiar.getNombre());
                    contextoSistema.put("usuario_edad", familiar.getEdad());
                    contextoSistema.put("ciudad", app.getCiudadActual());
                    contextoSistema.put("temperatura_actual", app.getTemperaturaActual() != null ? app.getTemperaturaActual() : 18);
                    contextoSistema.put("humedad_actual", app.getHumedadActual() != null ? app.getHumedadActual() : 60);
                    contextoSistema.put("probabilidad_lluvia_tarde", app.getProbLluviaTarde());
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

                    String prompt = "Eres el asistente inteligente de AeolusFit, un sistema de armario inteligente.\n"
                            + "Contexto meteorológico y del usuario:\n"
                            + contextoSistema.toString() + "\n\n"
                            + "Historial reciente:\n"
                            + historialArr.toString() + "\n\n"
                            + "Nuevo mensaje del usuario: \"" + mensajeUsuario + "\"\n\n"
                            + "Instrucciones:\n"
                            + "- Responde de forma natural, cercana y concisa (máximo 2-3 oraciones).\n"
                            + "- Si te comenta que ha pasado frío o calor, demuéstrale empatía y confírmale que has adaptado su sensibilidad térmica en el sistema.";

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
                respuestaTexto = "He tomado nota de tu comentario para adaptar las sugerencias de tu armario.";
            }

            guardarMensajeEnFirestore(familiar.getId(), respuestaTexto, false);
            listener.onRespuesta(respuestaTexto);
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