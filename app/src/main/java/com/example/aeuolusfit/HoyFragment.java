package com.example.aeuolusfit;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HoyFragment extends Fragment implements SensorEventListener {

    private RecyclerView rvPerfilesHoy;
    private AdaptadorPerfilesChip adaptadorPerfiles;
    private final List<Familiar> listaPerfiles = new ArrayList<>();

    private TextView tvSaludo, tvTemperaturaHoy, tvHumedadHoy;
    private TextView tvParaUsuario, tvTituloConjunto, tvDescripcionConjunto;
    private TextView tvContadorPrendas, tvEstadoSensores;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Sensores (Unidad 7)
    private SensorManager sensorManager;
    private Sensor sensorTemp, sensorHumedad;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // En Fragments independientes configuramos las vistas en onCreateView (Ud2 - 2.8.1)
        View v = inflater.inflate(R.layout.fragment_hoy, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tvSaludo = v.findViewById(R.id.tvSaludo);
        tvTemperaturaHoy = v.findViewById(R.id.tvTemperaturaHoy);
        tvHumedadHoy = v.findViewById(R.id.tvHumedadHoy);
        tvParaUsuario = v.findViewById(R.id.tvParaUsuario);
        tvTituloConjunto = v.findViewById(R.id.tvTituloConjunto);
        tvDescripcionConjunto = v.findViewById(R.id.tvDescripcionConjunto);
        tvContadorPrendas = v.findViewById(R.id.tvContadorPrendas);
        tvEstadoSensores = v.findViewById(R.id.tvEstadoSensores);
        rvPerfilesHoy = v.findViewById(R.id.rvPerfilesHoy);

        configurarSaludoYHora();
        configurarRecyclerViewPerfiles();
        cargarPerfilesFirestore();
        escucharContadorPrendas();
        configurarSensores();

        // Intención implícita para compartir conjunto (Unidad 3 - 3.9)
        ImageView btnCompartir = v.findViewById(R.id.btnCompartirConjunto);
        btnCompartir.setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, "Mi conjunto AeolusFit de hoy (" + tvParaUsuario.getText() + "): "
                    + tvTituloConjunto.getText() + " - " + tvDescripcionConjunto.getText());
            startActivity(Intent.createChooser(i, "Compartir conjunto con..."));
        });

        // Al pulsar la alerta de lluvia lanzamos una Notificación real (Unidad 6)
        MaterialCardView cardAlerta = v.findViewById(R.id.cardAlertaLluvia);
        cardAlerta.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).lanzarNotificacionClimatica(
                        "Alerta de Lluvia AeolusFit",
                        "60% de probabilidad esta tarde. ¡No olvides tu paraguas!");
                Toast.makeText(getContext(), "Recordatorio enviado a tus notificaciones", Toast.LENGTH_SHORT).show();
            }
        });

        MaterialButton btnVerConjunto = v.findViewById(R.id.btnVerConjunto);
        btnVerConjunto.setOnClickListener(view -> {
            // Navegar a la pestaña de Armario (posición 2 del ViewPager2)
            ViewPager2 vp = requireActivity().findViewById(R.id.viewpager);
            vp.setCurrentItem(2, true);
        });

        return v;
    }

    private void configurarSaludoYHora() {
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hora >= 6 && hora < 14) tvSaludo.setText("¡Buenos días!");
        else if (hora >= 14 && hora < 21) tvSaludo.setText("¡Buenas tardes!");
        else tvSaludo.setText("¡Buenas noches!");

        String horaActual = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        tvEstadoSensores.setText("Sensores actualizados a las " + horaActual);
    }

    private void configurarRecyclerViewPerfiles() {
        rvPerfilesHoy.setHasFixedSize(true);
        rvPerfilesHoy.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        adaptadorPerfiles = new AdaptadorPerfilesChip(listaPerfiles);

        adaptadorPerfiles.setOnItemClickListener(v -> {
            int pos = rvPerfilesHoy.getChildAdapterPosition(v);
            if (pos != RecyclerView.NO_POSITION) {
                for (int i = 0; i < listaPerfiles.size(); i++) {
                    listaPerfiles.get(i).setSeleccionado(i == pos);
                }
                adaptadorPerfiles.notifyDataSetChanged();
                calcularConjuntoEnHiloSecundario(listaPerfiles.get(pos), pos);
            }
        });

        rvPerfilesHoy.setAdapter(adaptadorPerfiles);
    }

    // Cálculo en un Hilo secundario usando Thread y runOnUiThread (Unidad 7 - 7.2.3)
    private void calcularConjuntoEnHiloSecundario(Familiar familiar, int indice) {
        new Thread(() -> {
            String titulo;
            String desc;
            if (indice % 3 == 0) {
                titulo = "Camiseta, pantalón largo y chaqueta ligera";
                desc = "Capas suaves para un día fresco.";
            } else if (indice % 3 == 1) {
                titulo = "Camisa oxford, vaqueros y chubasquero";
                desc = "Cómodo y protegido para la lluvia de la tarde.";
            } else {
                titulo = "Sudadera térmica, pantalón cómodo y botas";
                desc = "Ideal para moverse con libertad sin pasar frío.";
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvParaUsuario.setText("• Para " + familiar.getNombre());
                    tvTituloConjunto.setText(titulo);
                    tvDescripcionConjunto.setText(desc);
                });
            }
        }).start();
    }

    private void cargarPerfilesFirestore() {
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        db.collection("usuarios").document(uid).get().addOnSuccessListener(doc -> {
            String nombrePrincipal = "Lucía";
            if (doc.exists() && doc.getString("nombre") != null && !doc.getString("nombre").isEmpty()) {
                nombrePrincipal = doc.getString("nombre").split(" ")[0];
            } else if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                nombrePrincipal = currentUser.getDisplayName().split(" ")[0];
            }

            final String nombreFinal = nombrePrincipal;

            // Escuchar familiares añadidos en Firestore en tiempo real
            db.collection("usuarios").document(uid).collection("familiares")
                    .addSnapshotListener((snapshots, e) -> {
                        listaPerfiles.clear();
                        listaPerfiles.add(new Familiar("principal", nombreFinal, "Principal", "#E8CEBF", true));

                        if (snapshots != null && !snapshots.isEmpty()) {
                            for (QueryDocumentSnapshot document : snapshots) {
                                Familiar fam = document.toObject(Familiar.class);
                                fam.setSeleccionado(false);
                                listaPerfiles.add(fam);
                            }
                        } else {
                            // Perfiles del diseño Hi-Fi por defecto si aún no ha creado familiares
                            listaPerfiles.add(new Familiar("pablo", "Pablo", "Pareja", "#D9E2D5", false));
                            listaPerfiles.add(new Familiar("emma", "Emma", "Hija", "#EDE3CE", false));
                        }
                        adaptadorPerfiles.notifyDataSetChanged();
                        tvParaUsuario.setText("• Para " + nombreFinal);
                    });
        });
    }

    private void escucharContadorPrendas() {
        if (currentUser == null) return;
        db.collection("prendas")
                .whereEqualTo("uidUsuario", currentUser.getUid())
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots != null) {
                        int total = snapshots.size();
                        tvContadorPrendas.setText("Armario al día · " + (total > 0 ? total : 48) + " prendas");
                    }
                });
    }

    // Gestión de Sensores (Unidad 7 - 7.1)
    private void configurarSensores() {
        if (getContext() != null) {
            sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                List<Sensor> listaTemp = sensorManager.getSensorList(Sensor.TYPE_AMBIENT_TEMPERATURE);
                if (!listaTemp.isEmpty()) sensorTemp = listaTemp.get(0);

                List<Sensor> listaHum = sensorManager.getSensorList(Sensor.TYPE_RELATIVE_HUMIDITY);
                if (!listaHum.isEmpty()) sensorHumedad = listaHum.get(0);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (sensorTemp != null) sensorManager.registerListener(this, sensorTemp, SensorManager.SENSOR_DELAY_UI);
            if (sensorHumedad != null) sensorManager.registerListener(this, sensorHumedad, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            tvTemperaturaHoy.setText(Math.round(event.values[0]) + " °C");
        } else if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
            tvHumedadHoy.setText("💧 Humedad relativa: " + Math.round(event.values[0]) + " %");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}