package com.example.aeuolusfit;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HoyFragment extends Fragment implements Aplicacion.OnClimaChangeListener {

    private RecyclerView rvPerfilesHoy;
    private AdaptadorPerfilesChip adaptadorPerfiles;
    private final List<Familiar> listaPerfiles = new ArrayList<>();
    private final List<Prenda> listaPrendas = new ArrayList<>();

    private TextView tvSaludo, tvUbicacionClima, tvTemperaturaHoy, tvHumedadHoy, tvEstadoClima;
    private TextView tvMaxMinHoy, tvPronosticoTarde, tvAlertaTexto;
    private TextView tvParaUsuario, tvTituloConjunto, tvDescripcionConjunto;
    private TextView tvContadorPrendas, tvEstadoSensores;
    private ExtendedFloatingActionButton fabFeedbackIA;

    private final List<MensajeChat> mensajesConversacion = new ArrayList<>();
    private AdaptadorChat adaptadorChat;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Aplicacion appGlobal;

    private Familiar familiarSeleccionado = null;
    private boolean actualizandoDesdeRemoto = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_hoy, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        appGlobal = (Aplicacion) requireActivity().getApplication();

        tvSaludo = v.findViewById(R.id.tvSaludo);
        tvUbicacionClima = v.findViewById(R.id.tvUbicacionClima);
        tvTemperaturaHoy = v.findViewById(R.id.tvTemperaturaHoy);
        tvHumedadHoy = v.findViewById(R.id.tvHumedadHoy);
        tvEstadoClima = v.findViewById(R.id.tvEstadoClima);
        tvMaxMinHoy = v.findViewById(R.id.tvMaxMinHoy);
        tvPronosticoTarde = v.findViewById(R.id.tvPronosticoTarde);
        tvAlertaTexto = v.findViewById(R.id.tvAlertaTexto);
        tvParaUsuario = v.findViewById(R.id.tvParaUsuario);
        tvTituloConjunto = v.findViewById(R.id.tvTituloConjunto);
        tvDescripcionConjunto = v.findViewById(R.id.tvDescripcionConjunto);
        tvContadorPrendas = v.findViewById(R.id.tvContadorPrendas);
        tvEstadoSensores = v.findViewById(R.id.tvEstadoSensores);
        rvPerfilesHoy = v.findViewById(R.id.rvPerfilesHoy);
        fabFeedbackIA = v.findViewById(R.id.fabFeedbackIA);

        configurarSaludoYHora();
        configurarRecyclerViewPerfiles();
        cargarPerfilesTermicosOficiales();
        escucharPrendasReales();
        escucharModuloIoTFibestore();

        onClimaActualizado();

        ImageView btnCompartir = v.findViewById(R.id.btnCompartirConjunto);
        btnCompartir.setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, "AeolusFit: " + tvTituloConjunto.getText() + " (" + tvTemperaturaHoy.getText() + ")");
            startActivity(Intent.createChooser(i, "Compartir conjunto"));
        });

        MaterialCardView cardAlerta = v.findViewById(R.id.cardAlertaLluvia);
        cardAlerta.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).lanzarNotificacionClimatica(
                        "Alerta Climática AeolusFit",
                        tvAlertaTexto.getText().toString());
                Toast.makeText(getContext(), "Aviso enviado al centro de notificaciones", Toast.LENGTH_SHORT).show();
            }
        });

        MaterialButton btnVerConjunto = v.findViewById(R.id.btnVerConjunto);
        btnVerConjunto.setOnClickListener(view -> {
            ViewPager2 vp = requireActivity().findViewById(R.id.viewpager);
            vp.setCurrentItem(2, true);
        });

        fabFeedbackIA.setOnClickListener(view -> mostrarDialogoFeedback());

        return v;
    }

    private void mostrarDialogoFeedback() {
        if (familiarSeleccionado == null) return;

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_chat_ia);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (int) (getResources().getDisplayMetrics().heightPixels * 0.85));
        }

        TextView tvTitulo = dialog.findViewById(R.id.tvTituloChatIA);
        TextView tvSubtitulo = dialog.findViewById(R.id.tvSubtituloChatIA);
        RecyclerView rvChat = dialog.findViewById(R.id.rvMensajesChat);
        EditText etInput = dialog.findViewById(R.id.etMensajeChat);
        View btnEnviar = dialog.findViewById(R.id.btnEnviarMensajeChat);
        TextView tvPensando = dialog.findViewById(R.id.tvEstadoEscribiendo);

        tvTitulo.setText("Hablando con " + familiarSeleccionado.getNombre());
        tvSubtitulo.setText("T: " + tvTemperaturaHoy.getText() + " · " + familiarSeleccionado.getSensacionTermica());

        mensajesConversacion.clear();
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        adaptadorChat = new AdaptadorChat(mensajesConversacion);
        rvChat.setAdapter(adaptadorChat);

        if (currentUser != null) {
            db.collection("usuarios").document(currentUser.getUid())
                    .collection("familiares").document(familiarSeleccionado.getId())
                    .collection("mensajesChat")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .limit(20)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots != null && !snapshots.isEmpty()) {
                            mensajesConversacion.clear();
                            for (QueryDocumentSnapshot doc : snapshots) {
                                String txt = doc.getString("texto");
                                Boolean esUsr = doc.getBoolean("esUsuario");
                                Long ts = doc.getLong("timestamp");
                                String hora = ts != null ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ts)) : "";
                                mensajesConversacion.add(new MensajeChat(txt, Boolean.TRUE.equals(esUsr), hora));
                            }
                            adaptadorChat.notifyDataSetChanged();
                            rvChat.scrollToPosition(mensajesConversacion.size() - 1);
                        } else {
                            String hora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                            mensajesConversacion.add(new MensajeChat(
                                    "¡Hola " + familiarSeleccionado.getNombre() + "! Hoy tenemos " + tvTemperaturaHoy.getText() + ". ¿Tienes alguna duda sobre el tiempo o cómo te sentiste con la ropa?",
                                    false,
                                    hora
                            ));
                            adaptadorChat.notifyDataSetChanged();
                        }
                    });
        }

        dialog.findViewById(R.id.btnCerrarChat).setOnClickListener(x -> dialog.dismiss());

        btnEnviar.setOnClickListener(v -> {
            String texto = etInput.getText().toString().trim();
            if (texto.isEmpty()) return;

            String hora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            mensajesConversacion.add(new MensajeChat(texto, true, hora));
            adaptadorChat.notifyItemInserted(mensajesConversacion.size() - 1);
            rvChat.scrollToPosition(mensajesConversacion.size() - 1);

            etInput.setText("");
            tvPensando.setVisibility(View.VISIBLE);
            btnEnviar.setEnabled(false);

            RecomendadorHibridoIA.guardarMensajeEnFirestore(familiarSeleccionado.getId(), texto, true);

            RecomendadorHibridoIA.conversarConIA(familiarSeleccionado, texto, appGlobal, mensajesConversacion, respuestaIA -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvPensando.setVisibility(View.GONE);
                        btnEnviar.setEnabled(true);
                        String horaIA = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                        mensajesConversacion.add(new MensajeChat(respuestaIA, false, horaIA));
                        adaptadorChat.notifyItemInserted(mensajesConversacion.size() - 1);
                        rvChat.smoothScrollToPosition(mensajesConversacion.size() - 1);
                    });
                }
            });
        });

        dialog.show();
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

    @Override
    public void onClimaActualizado() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            Double temp = appGlobal.getTemperaturaActual();
            Double hum = appGlobal.getHumedadActual();

            if (temp != null) {
                tvTemperaturaHoy.setText(Math.round(temp) + " °C");
            } else {
                tvTemperaturaHoy.setText("— °C");
            }

            if (hum != null) {
                tvHumedadHoy.setText("💧 Humedad: " + Math.round(hum) + " %");
            } else {
                tvHumedadHoy.setText("💧 Humedad: — %");
            }

            tvUbicacionClima.setText(appGlobal.getCiudadActual() + " · Ahora");
            tvEstadoClima.setText(appGlobal.getEstadoCielo());

            if (appGlobal.getMaxTemp() != 0 || appGlobal.getMinTemp() != 0) {
                tvMaxMinHoy.setText("Máx. " + appGlobal.getMaxTemp() + "° · Mín. " + appGlobal.getMinTemp() + "°");
            }
            tvPronosticoTarde.setText("🌧️ Tarde · " + appGlobal.getProbLluviaTarde() + " %");
            tvAlertaTexto.setText(appGlobal.getConsejoLluvia());

            if (familiarSeleccionado != null) {
                solicitarRecomendacion(familiarSeleccionado);
            }
        });
    }

    private void escucharModuloIoTFibestore() {
        if (currentUser == null) return;

        db.collection("dispositivos_iot").document("M001")
                .addSnapshotListener((doc, e) -> {
                    if (e != null) return;

                    if (doc != null && doc.exists()) {
                        Double temp = doc.getDouble("temperatura");
                        Double hum = doc.getDouble("humedad");
                        Boolean conectado = doc.getBoolean("conectado");
                        Boolean puertaAbierta = doc.getBoolean("puertaAbierta");
                        Boolean presencia = doc.getBoolean("presencia");

                        if (Boolean.TRUE.equals(conectado) && temp != null && hum != null) {
                            if (appGlobal != null) {
                                appGlobal.actualizarDesdeHardware(temp, hum);
                            }
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (tvEstadoSensores != null) {
                                    if (Boolean.TRUE.equals(puertaAbierta)) {
                                        tvEstadoSensores.setText("Armario ABIERTO" + (Boolean.TRUE.equals(presencia) ? " · Usuario detectado" : ""));
                                    } else {
                                        String horaActual = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());
                                        tvEstadoSensores.setText("Sensores sincronizados a las " + horaActual);
                                    }
                                }
                            });
                        }
                    }
                });
    }

    private void configurarSaludoYHora() {
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hora >= 6 && hora < 14) tvSaludo.setText("¡Buenos días!");
        else if (hora >= 14 && hora < 21) tvSaludo.setText("¡Buenas tardes!");
        else tvSaludo.setText("¡Buenas noches!");

        String horaActual = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        tvEstadoSensores.setText("Sensores sincronizados a las " + horaActual);
    }

    private void configurarRecyclerViewPerfiles() {
        rvPerfilesHoy.setHasFixedSize(true);
        rvPerfilesHoy.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        adaptadorPerfiles = new AdaptadorPerfilesChip(listaPerfiles);

        adaptadorPerfiles.setOnPerfilClickListener((familiar, posicion) -> {
            seleccionarPerfilActivo(familiar, true);
        });

        rvPerfilesHoy.setAdapter(adaptadorPerfiles);
    }

    private void seleccionarPerfilActivo(Familiar familiar, boolean origenApp) {
        familiarSeleccionado = familiar;
        tvParaUsuario.setText("• Para " + familiar.getNombre());
        fabFeedbackIA.setText("¿Cómo se sintió " + familiar.getNombre() + "?");

        for (Familiar f : listaPerfiles) {
            f.setSeleccionado(f.getId().equals(familiar.getId()));
        }
        adaptadorPerfiles.notifyDataSetChanged();

        if (origenApp && currentUser != null && !actualizandoDesdeRemoto) {
            Map<String, Object> update = new HashMap<>();
            update.put("perfilActivo", familiar.getId());

            db.collection("usuarios").document(currentUser.getUid())
                    .collection("configuracion").document("estado_actual")
                    .set(update, SetOptions.merge());
        }

        solicitarRecomendacion(familiar);
    }

    private void cargarPerfilesTermicosOficiales() {
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        db.collection("usuarios").document(uid)
                .collection("configuracion").document("perfiles")
                .addSnapshotListener((docPerfiles, error) -> {
                    if (docPerfiles == null || !docPerfiles.exists()) return;

                    List<Familiar> perfilesNuevos = new ArrayList<>();

                    for (int i = 1; i <= 4; i++) {
                        String key = "perfil_" + i;
                        if (docPerfiles.contains(key)) {
                            Map<String, Object> data = (Map<String, Object>) docPerfiles.get(key);
                            if (data != null) {
                                String nombre = (String) data.get("nombre");
                                double ajuste = data.containsKey("ajuste") ? ((Number) data.get("ajuste")).doubleValue() : 0.0;

                                Familiar f = new Familiar();
                                f.setId(key);
                                f.setNombre(nombre != null ? nombre : "Perfil " + i);
                                f.setSensacionTermica("Ajuste térmico: " + ajuste + "°C");
                                f.setColorHex("#4F6342");
                                f.setSeleccionado(i == 1);
                                perfilesNuevos.add(f);
                            }
                        }
                    }

                    if (!perfilesNuevos.isEmpty()) {
                        listaPerfiles.clear();
                        listaPerfiles.addAll(perfilesNuevos);
                        adaptadorPerfiles.notifyDataSetChanged();

                        if (familiarSeleccionado == null) {
                            seleccionarPerfilActivo(listaPerfiles.get(0), false);
                        }
                    }
                });

        db.collection("usuarios").document(uid)
                .collection("configuracion").document("estado_actual")
                .addSnapshotListener((docEstado, err) -> {
                    if (docEstado != null && docEstado.exists()) {
                        String idActivo = docEstado.getString("perfilActivo");
                        if (idActivo != null) {
                            for (Familiar f : listaPerfiles) {
                                if (f.getId().equals(idActivo)) {
                                    if (familiarSeleccionado == null || !familiarSeleccionado.getId().equals(idActivo)) {
                                        actualizandoDesdeRemoto = true;
                                        seleccionarPerfilActivo(f, false);
                                        actualizandoDesdeRemoto = false;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                });
    }

    private void solicitarRecomendacion(Familiar familiar) {
        if (familiar.getUltimoTituloConjunto() != null && !familiar.getUltimoTituloConjunto().isEmpty()) {
            tvTituloConjunto.setText(familiar.getUltimoTituloConjunto());
            tvDescripcionConjunto.setText(familiar.getUltimaDescripcionConjunto());
        }

        RecomendadorHibridoIA.obtenerRecomendacion(familiar, appGlobal, listaPrendas, (titulo, descripcion, esDesdeIA) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvTituloConjunto.setText(titulo);
                    tvDescripcionConjunto.setText(descripcion);
                });
            }
        });
    }

    private void escucharPrendasReales() {
        if (currentUser == null) return;
        db.collection("prendas")
                .whereEqualTo("uidUsuario", currentUser.getUid())
                .addSnapshotListener((snapshots, error) -> {
                    listaPrendas.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot d : snapshots) {
                            listaPrendas.add(d.toObject(Prenda.class));
                        }
                        int total = listaPrendas.size();
                        if (total == 0) {
                            tvContadorPrendas.setText("Armario al día · 0 prendas");
                        } else {
                            tvContadorPrendas.setText("Armario al día · " + total + " prendas");
                        }

                        if (familiarSeleccionado != null) {
                            solicitarRecomendacion(familiarSeleccionado);
                        }
                    }
                });
    }
}