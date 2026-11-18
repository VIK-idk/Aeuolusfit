package com.example.aeuolusfit;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ArmarioFragment extends Fragment implements Aplicacion.OnClimaChangeListener {

    private TextView tvSubtituloArmario, tvResumenDispositivos;
    private TextView tvEstadoArmario, tvAccionArmario, tvEstadoModulo, tvAccionModulo;
    private TextView tvTempModuloArmario, tvHumModuloArmario, tvUltimaLecturaModulo, tvEtiquetaDatosModulo;
    private TextView tvEstadoLuzLed, tvPorcentajeBrillo;
    private MaterialCardView cardAvisoBloqueoLuz, cardDispositivoArmario, cardDispositivoModulo;
    private SwitchCompat switchLuzLed;
    private SeekBar seekBarBrillo;
    private Spinner spinnerDuracionLuz, spinnerTiempoPantalla;
    private MaterialButton btnGuardarAjustesLuz, btnIrConectarDispositivo;

    private FirebaseFirestore db;
    private FirebaseUser user;
    private Aplicacion appGlobal;

    private boolean armarioConectado = false;
    private boolean moduloConectado = false;

    private final String[] opcionesDuracion = {"30 s", "60 s", "120 s"};
    private final String[] opcionesPantalla = {"15 s", "30 s", "60 s"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_armario, container, false);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        appGlobal = (Aplicacion) requireActivity().getApplication();

        inicializarVistas(v);
        configurarSpinners();
        configurarControlesIluminacion();
        escucharEstadoDispositivosFirestore();

        View.OnClickListener abrirAsistente = view -> {
            Intent i = new Intent(getContext(), ConectarDispositivoActivity.class);
            startActivity(i);
        };

        btnIrConectarDispositivo.setOnClickListener(abrirAsistente);
        cardDispositivoArmario.setOnClickListener(abrirAsistente);
        cardDispositivoModulo.setOnClickListener(abrirAsistente);

        onClimaActualizado();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        appGlobal.registrarListener(this);
        onClimaActualizado();
    }

    @Override
    public void onPause() {
        super.onPause();
        appGlobal.desregistrarListener(this);
    }

    // Sincronización idéntica con HoyFragment
    @Override
    public void onClimaActualizado() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (moduloConectado) {
                Double t = appGlobal.getTemperaturaActual();
                Double h = appGlobal.getHumedadActual();
                tvTempModuloArmario.setText(t != null ? Math.round(t) + " °C" : "— °C");
                tvHumModuloArmario.setText(h != null ? Math.round(h) + " %" : "— %");
                String hora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                tvUltimaLecturaModulo.setText("Última lectura · " + hora);
            }
        });
    }

    private void inicializarVistas(View v) {
        tvSubtituloArmario = v.findViewById(R.id.tvSubtituloArmario);
        tvResumenDispositivos = v.findViewById(R.id.tvResumenDispositivos);
        tvEstadoArmario = v.findViewById(R.id.tvEstadoArmario);
        tvAccionArmario = v.findViewById(R.id.tvAccionArmario);
        tvEstadoModulo = v.findViewById(R.id.tvEstadoModulo);
        tvAccionModulo = v.findViewById(R.id.tvAccionModulo);
        tvTempModuloArmario = v.findViewById(R.id.tvTempModuloArmario);
        tvHumModuloArmario = v.findViewById(R.id.tvHumModuloArmario);
        tvUltimaLecturaModulo = v.findViewById(R.id.tvUltimaLecturaModulo);
        tvEtiquetaDatosModulo = v.findViewById(R.id.tvEtiquetaDatosModulo);
        tvEstadoLuzLed = v.findViewById(R.id.tvEstadoLuzLed);
        tvPorcentajeBrillo = v.findViewById(R.id.tvPorcentajeBrillo);

        cardAvisoBloqueoLuz = v.findViewById(R.id.cardAvisoBloqueoLuz);
        cardDispositivoArmario = v.findViewById(R.id.cardDispositivoArmario);
        cardDispositivoModulo = v.findViewById(R.id.cardDispositivoModulo);

        switchLuzLed = v.findViewById(R.id.switchLuzLed);
        seekBarBrillo = v.findViewById(R.id.seekBarBrillo);
        spinnerDuracionLuz = v.findViewById(R.id.spinnerDuracionLuz);
        spinnerTiempoPantalla = v.findViewById(R.id.spinnerTiempoPantalla);
        btnGuardarAjustesLuz = v.findViewById(R.id.btnGuardarAjustesLuz);
        btnIrConectarDispositivo = v.findViewById(R.id.btnIrConectarDispositivo);
    }

    private void configurarSpinners() {
        if (getContext() == null) return;
        ArrayAdapter<String> adapterDuracion = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, opcionesDuracion);
        adapterDuracion.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDuracionLuz.setAdapter(adapterDuracion);
        spinnerDuracionLuz.setSelection(1);

        ArrayAdapter<String> adapterPantalla = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, opcionesPantalla);
        adapterPantalla.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTiempoPantalla.setAdapter(adapterPantalla);
        spinnerTiempoPantalla.setSelection(1);
    }

    private void configurarControlesIluminacion() {
        switchLuzLed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (armarioConectado) {
                tvEstadoLuzLed.setText(isChecked ? "Encendida" : "Apagada");
            }
        });

        seekBarBrillo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (armarioConectado) {
                    tvPorcentajeBrillo.setText(progress + " %");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnGuardarAjustesLuz.setOnClickListener(v -> guardarAjustesEnFirestore());
    }

    private void escucharEstadoDispositivosFirestore() {
        if (user == null) return;
        db.collection("usuarios").document(user.getUid())
                .collection("configuracion").document("iot")
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        Boolean armCon = snapshot.getBoolean("armarioConectado");
                        Boolean modCon = snapshot.getBoolean("moduloConectado");
                        armarioConectado = armCon != null && armCon;
                        moduloConectado = modCon != null && modCon;

                        Boolean ledOn = snapshot.getBoolean("luzLed");
                        Long brillo = snapshot.getLong("brillo");
                        if (ledOn != null) switchLuzLed.setChecked(ledOn);
                        if (brillo != null) seekBarBrillo.setProgress(brillo.intValue());
                    } else {
                        armarioConectado = false;
                        moduloConectado = false;
                    }
                    actualizarVistaSegunEstado();
                    onClimaActualizado();
                });
    }

    private void actualizarVistaSegunEstado() {
        int total = (armarioConectado ? 1 : 0) + (moduloConectado ? 1 : 0);

        if (total == 0) {
            tvSubtituloArmario.setText("Conecta tu primer armario inteligente para empezar.");
            tvResumenDispositivos.setText("Sin dispositivos");
            tvResumenDispositivos.setTextColor(Color.parseColor("#8C5C54"));
        } else if (total == 1) {
            tvSubtituloArmario.setText("1 de 2 dispositivos vinculados.");
            tvResumenDispositivos.setText("Requiere atención");
            tvResumenDispositivos.setTextColor(Color.parseColor("#8C5C54"));
        } else {
            tvSubtituloArmario.setText("Tu hogar conectado, a tu manera.");
            tvResumenDispositivos.setText("2 conectados");
            tvResumenDispositivos.setTextColor(Color.parseColor("#4F6342"));
        }

        if (armarioConectado) {
            tvEstadoArmario.setText("✓  Conectado (A001)");
            tvEstadoArmario.setTextColor(Color.parseColor("#4F6342"));
            tvAccionArmario.setText("Ver detalles  ›");
            cardAvisoBloqueoLuz.setVisibility(View.GONE);

            switchLuzLed.setEnabled(true);
            seekBarBrillo.setEnabled(true);
            spinnerDuracionLuz.setEnabled(true);
            spinnerTiempoPantalla.setEnabled(true);
            btnGuardarAjustesLuz.setEnabled(true);
            btnGuardarAjustesLuz.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4F6342")));
            btnGuardarAjustesLuz.setTextColor(Color.WHITE);
            tvEstadoLuzLed.setText(switchLuzLed.isChecked() ? "Encendida" : "Apagada");
            tvPorcentajeBrillo.setText(seekBarBrillo.getProgress() + " %");
        } else {
            tvEstadoArmario.setText("🧭  Desconectado");
            tvEstadoArmario.setTextColor(Color.parseColor("#8C5C54"));
            tvAccionArmario.setText("Vincular armario (A001)  ›");
            cardAvisoBloqueoLuz.setVisibility(View.VISIBLE);

            switchLuzLed.setEnabled(false);
            seekBarBrillo.setEnabled(false);
            spinnerDuracionLuz.setEnabled(false);
            spinnerTiempoPantalla.setEnabled(false);
            btnGuardarAjustesLuz.setEnabled(false);
            btnGuardarAjustesLuz.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0DED7")));
            btnGuardarAjustesLuz.setTextColor(Color.parseColor("#686D65"));
            tvEstadoLuzLed.setText("No disponible");
            tvPorcentajeBrillo.setText("— %");
        }

        if (moduloConectado) {
            tvEstadoModulo.setText("✓  Conectado (M001)");
            tvEstadoModulo.setTextColor(Color.parseColor("#4F6342"));
            tvAccionModulo.setText("Ver detalles  ›");
            tvEtiquetaDatosModulo.setVisibility(View.VISIBLE);
        } else {
            tvEstadoModulo.setText("🕒  Sin configurar");
            tvEstadoModulo.setTextColor(Color.parseColor("#8C5C54"));
            tvAccionModulo.setText("Vincular módulo (M001)  ›");
            tvTempModuloArmario.setText("— °C");
            tvHumModuloArmario.setText("— %");
            tvUltimaLecturaModulo.setText("Sin lecturas · vincula el módulo exterior");
            tvEtiquetaDatosModulo.setVisibility(View.GONE);
        }
    }

    private void guardarAjustesEnFirestore() {
        if (user == null || !armarioConectado) return;
        Map<String, Object> ajustes = new HashMap<>();
        ajustes.put("luzLed", switchLuzLed.isChecked());
        ajustes.put("brillo", seekBarBrillo.getProgress());
        ajustes.put("duracionLuz", spinnerDuracionLuz.getSelectedItem().toString());
        ajustes.put("tiempoPantalla", spinnerTiempoPantalla.getSelectedItem().toString());

        db.collection("usuarios").document(user.getUid())
                .collection("configuracion").document("iot")
                .update(ajustes)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Ajustes guardados en Firestore", Toast.LENGTH_SHORT).show());
    }
}