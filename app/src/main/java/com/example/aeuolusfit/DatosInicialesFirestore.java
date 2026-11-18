package com.example.aeuolusfit;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatosInicialesFirestore {

    public static void cargarDatosCompletos(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Inicia sesión primero", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        // 1. CUENTA RAÍZ (Datos de configuración general del armario)
        Map<String, Object> cuentaRaiz = new HashMap<>();
        cuentaRaiz.put("email", user.getEmail());
        cuentaRaiz.put("localidad", "Valencia");
        cuentaRaiz.put("latitud", 39.4699);
        cuentaRaiz.put("longitud", -0.3763);
        cuentaRaiz.put("tipoUsoArmario", "familiar");
        cuentaRaiz.put("onboardingCompletado", true);

        db.collection("usuarios").document(uid).set(cuentaRaiz, SetOptions.merge());

        // 2. SUBCOLECCIÓN 'familiares': Perfil 1 (El titular de la cuenta)
        String nombreTitular = user.getDisplayName() != null ? user.getDisplayName() : "Tú";
        crearFamiliarConChat(db, uid, "titular", nombreTitular, "Titular de la cuenta", "#E8CEBF", 25, "Suele tener frío");

        // 3. SUBCOLECCIÓN 'familiares': Perfiles adicionales
        crearFamiliarConChat(db, uid, "fam_laura", "Laura", "Hermana", "#D9E2D5", 22, "Suele tener calor");
        crearFamiliarConChat(db, uid, "fam_carlos", "Carlos", "Padre", "#EAEFE5", 54, "Suele tener frío");

        // 4. DISPOSITIVO IOT
        Map<String, Object> sensor = new HashMap<>();
        sensor.put("conectado", true);
        sensor.put("temperatura", 17.5);
        sensor.put("humedad", 62.0);
        sensor.put("bateria", 95);
        sensor.put("nombre", "Módulo M001 Exterior");
        db.collection("usuarios").document(uid).collection("dispositivos").document("M001").set(sensor, SetOptions.merge());

        // 5. COLECCIÓN 'prendas' VINCULADAS A CADA FAMILIAR
        // Prendas del Titular
        crearPrenda(db, uid, "p1", "Sudadera Verde Oliva", "Superior", "Casual", "#4F6342", "titular");
        crearPrenda(db, uid, "p2", "Pantalón Denim Clásico", "Inferior", "Todo tiempo", "#3A4B5C", "titular");
        crearPrenda(db, uid, "p3", "Chaqueta Cortavientos", "Abrigo", "Lluvia/Frío", "#2C3329", "titular");

        // Prendas de Laura
        crearPrenda(db, uid, "p4", "Camiseta Algodón Beige", "Superior", "Calor", "#E8CEBF", "fam_laura");
        crearPrenda(db, uid, "p5", "Short Deportivo", "Inferior", "Calor", "#9EB393", "fam_laura");

        // Prendas de Carlos
        crearPrenda(db, uid, "p6", "Jersey Lana Grueso", "Superior", "Frío", "#7D524A", "fam_carlos");
        crearPrenda(db, uid, "p7", "Chubasquero Impermeable", "Abrigo", "Lluvia", "#686D65", "fam_carlos");

        Toast.makeText(context, "Arquitectura y datos sincronizados en Firestore", Toast.LENGTH_LONG).show();
    }

    private static void crearFamiliarConChat(FirebaseFirestore db, String uid, String idFam, String nombre, String rol, String color, int edad, String sensacion) {
        Map<String, Object> fam = new HashMap<>();
        fam.put("id", idFam);
        fam.put("nombre", nombre);
        fam.put("rol", rol);
        fam.put("colorHex", color);
        fam.put("edad", edad);
        fam.put("sensacionTermica", sensacion);
        fam.put("tipoUso", "personal");
        fam.put("ultimoTituloConjunto", sensacion.contains("frío") ? "Sudadera térmica y chaqueta" : "Camiseta suave de algodón");
        fam.put("ultimaDescripcionConjunto", "Sugerencia personalizada según su tolerancia térmica.");

        List<String> hist = new ArrayList<>();
        hist.add("Registro inicial: Perfil configurado como " + sensacion.toLowerCase());
        fam.put("historialMemoriaIA", hist);

        // Guardar documento del familiar en usuarios/{uid}/familiares/{idFam}
        db.collection("usuarios").document(uid).collection("familiares").document(idFam)
                .set(fam, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Historial conversacional del Chatbot en usuarios/{uid}/familiares/{idFam}/mensajesChat
                    Map<String, Object> msg1 = new HashMap<>();
                    msg1.put("texto", "¡Hola " + nombre + "! Tienes tus recomendaciones listas según tus prendas.");
                    msg1.put("esUsuario", false);
                    msg1.put("timestamp", System.currentTimeMillis() - 60000);

                    Map<String, Object> msg2 = new HashMap<>();
                    msg2.put("texto", "¿Qué tal me vendría salir con manga corta hoy?");
                    msg2.put("esUsuario", true);
                    msg2.put("timestamp", System.currentTimeMillis() - 30000);

                    Map<String, Object> msg3 = new HashMap<>();
                    msg3.put("texto", "Con 17°C y tu perfil habitual (" + sensacion + "), te recomiendo llevar al menos una chaqueta fina.");
                    msg3.put("esUsuario", false);
                    msg3.put("timestamp", System.currentTimeMillis());

                    db.collection("usuarios").document(uid).collection("familiares").document(idFam)
                            .collection("mensajesChat").add(msg1);
                    db.collection("usuarios").document(uid).collection("familiares").document(idFam)
                            .collection("mensajesChat").add(msg2);
                    db.collection("usuarios").document(uid).collection("familiares").document(idFam)
                            .collection("mensajesChat").add(msg3);
                });
    }

    private static void crearPrenda(FirebaseFirestore db, String uid, String idPrenda, String nombre, String categoria, String clima, String colorHex, String idFamiliar) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", idPrenda);
        p.put("nombre", nombre);
        p.put("categoria", categoria);
        p.put("climaIdeal", clima);
        p.put("colorHex", colorHex);
        p.put("uidUsuario", uid);
        p.put("idFamiliar", idFamiliar); // Vinculación relacional

        db.collection("prendas").document(uid + "_" + idPrenda).set(p, SetOptions.merge());
    }
}