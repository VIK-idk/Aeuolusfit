package com.example.aeuolusfit;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class DatosInicialesFirestore {

    public static void cargarDatosCompletos(android.content.Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) return;

        // Definimos los 4 perfiles térmicos oficiales
        Map<String, Object> perfiles = new HashMap<>();

        Map<String, Object> p1 = new HashMap<>();
        p1.put("nombre", "Lucía (Friolera)");
        p1.put("ajuste", -5.0);
        perfiles.put("perfil_1", p1);

        Map<String, Object> p2 = new HashMap<>();
        p2.put("nombre", "Pablo (Caluroso)");
        p2.put("ajuste", 5.0);
        perfiles.put("perfil_2", p2);

        Map<String, Object> p3 = new HashMap<>();
        p3.put("nombre", "Emma (Ext. Friolera)");
        p3.put("ajuste", -10.0);
        perfiles.put("perfil_3", p3);

        Map<String, Object> p4 = new HashMap<>();
        p4.put("nombre", "Carlos (Ext. Caluroso)");
        p4.put("ajuste", 10.0);
        perfiles.put("perfil_4", p4);

        // Guardar los perfiles limpios en Firestore
        db.collection("usuarios").document(user.getUid())
                .collection("configuracion").document("perfiles")
                .set(perfiles);

        // Inicializar el estado actual por defecto en perfil_1
        Map<String, Object> estadoActual = new HashMap<>();
        estadoActual.put("perfilActivo", "perfil_1");

        db.collection("usuarios").document(user.getUid())
                .collection("configuracion").document("estado_actual")
                .set(estadoActual, SetOptions.merge());
    }
}