package com.example.aeuolusfit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

public class ArmarioFragment extends Fragment {

    // Vistas de la Interfaz
    private TextView tvEstadoPuerta, tvTempModuloArmario, tvHumModuloArmario;
    private MaterialCardView cardDispositivoArmario, cardDispositivoModulo;
    private MaterialButton btnIrConectarDispositivo;

    private FirebaseFirestore db;

    public ArmarioFragment() {
        // Constructor público vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_armario, container, false);

        // 1. Inicializar Base de Datos
        db = FirebaseFirestore.getInstance();

        // 2. Vincular elementos visuales (findViewById)
        tvEstadoPuerta = view.findViewById(R.id.tvEstadoPuerta);
        tvTempModuloArmario = view.findViewById(R.id.tvTempModuloArmario);
        tvHumModuloArmario = view.findViewById(R.id.tvHumModuloArmario);
        cardDispositivoArmario = view.findViewById(R.id.cardDispositivoArmario);
        cardDispositivoModulo = view.findViewById(R.id.cardDispositivoModulo);
        btnIrConectarDispositivo = view.findViewById(R.id.btnIrConectarDispositivo);

        // 3. Configurar clic para conectar dispositivos
        View.OnClickListener abrirAsistente = v -> {
            Intent intent = new Intent(getContext(), ConectarDispositivoActivity.class);
            startActivity(intent);
        };

        if (btnIrConectarDispositivo != null) btnIrConectarDispositivo.setOnClickListener(abrirAsistente);
        if (cardDispositivoArmario != null) cardDispositivoArmario.setOnClickListener(abrirAsistente);
        if (cardDispositivoModulo != null) cardDispositivoModulo.setOnClickListener(abrirAsistente);

        // 4. Iniciar escucha en tiempo real
        escucharSensoresArmario();

        return view;
    }

    private void escucharSensoresArmario() {
        // Escucha directamente los datos subidos por el Gateway de Raspberry Pi
        db.collection("dispositivos_iot").document("M001")
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        Log.e("ArmarioFragment", "Error escuchando sensores: ", e);
                        return;
                    }

                    if (doc != null && doc.exists()) {
                        Double temp = doc.getDouble("temperatura");
                        Double hum = doc.getDouble("humedad");
                        Boolean puertaAbierta = doc.getBoolean("puertaAbierta");
                        Boolean presencia = doc.getBoolean("presencia");

                        if (getActivity() != null && isAdded()) {
                            getActivity().runOnUiThread(() -> {

                                // Actualizar Clima del Módulo Exterior
                                if (tvTempModuloArmario != null && temp != null) {
                                    tvTempModuloArmario.setText(String.format(java.util.Locale.getDefault(), "%.0f °C", temp));
                                }
                                if (tvHumModuloArmario != null && hum != null) {
                                    tvHumModuloArmario.setText(String.format(java.util.Locale.getDefault(), "%.0f %%", hum));
                                }

                                // Actualizar Estado del Armario
                                if (tvEstadoPuerta != null) {
                                    if (Boolean.TRUE.equals(puertaAbierta)) {
                                        String texto = "Estado: ABIERTO";
                                        if (Boolean.TRUE.equals(presencia)) {
                                            texto += "\n(Usuario detectado)";
                                        }
                                        tvEstadoPuerta.setText(texto);
                                        tvEstadoPuerta.setTextColor(android.graphics.Color.parseColor("#8D4C42"));
                                    } else {
                                        tvEstadoPuerta.setText("Estado: CERRADO");
                                        tvEstadoPuerta.setTextColor(android.graphics.Color.parseColor("#4F6342"));
                                    }
                                }
                            });
                        }
                    }
                });
    }
}