package com.example.aeuolusfit;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditarPerfilActivity extends AppCompatActivity {

    private String idFamiliar, nombreActual, colorHexActual, sensacionActual;
    private int edadActual = 0;
    private double ajusteActual = 0.0;
    private boolean esTitular = false;
    private boolean esPerfilTermicoOficial = false; // Identifica si es perfil_1 a perfil_4

    private TextView tvSubtituloEditarPerfil, tvAvatarInicialEditar;
    private TextView tvHeaderNombreEditar, tvHeaderEdadEditar, tvHeaderSensacionEditar;
    private EditText etNombrePerfilEditar, etEdadPerfilEditar;
    private MaterialCardView cardOpcionFrio, cardOpcionCalor;
    private TextView tvLabelFrio, tvLabelCalor;
    private MaterialButton btnEliminar;

    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // 1. Recoger datos pasados por el Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            idFamiliar = extras.getString("id", "titular");
            nombreActual = extras.getString("nombre", "");
            edadActual = extras.getInt("edad", 0);
            sensacionActual = extras.getString("sensacion", "Suele tener frío");
            colorHexActual = extras.getString("colorHex", "#E8CEBF");
            esTitular = extras.getBoolean("esTitular", false);
            ajusteActual = extras.getDouble("ajuste", 0.0);
        } else {
            idFamiliar = "titular";
            nombreActual = "";
            edadActual = 0;
            sensacionActual = "Suele tener frío";
            colorHexActual = "#E8CEBF";
            esTitular = true;
        }

        // Si el ID empieza por "perfil_", sabemos que estamos editando uno de los 4 perfiles térmicos unificados
        esPerfilTermicoOficial = (idFamiliar != null && idFamiliar.startsWith("perfil_"));

        inicializarVistas();
        configurarEscuchadoresTexto();
        actualizarCabecera();

        findViewById(R.id.tvVolverFamilia).setOnClickListener(v -> finish());

        cardOpcionFrio.setOnClickListener(v -> seleccionarSensacion(true));
        cardOpcionCalor.setOnClickListener(v -> seleccionarSensacion(false));

        MaterialButton btnGuardar = findViewById(R.id.btnGuardarPerfilFamiliar);
        btnGuardar.setOnClickListener(v -> guardarCambiosFirestore());

        btnEliminar = findViewById(R.id.btnAbrirModalEliminar);
        // Si es el titular o uno de los 4 perfiles térmicos oficiales del sistema, protegemos ocultando el borrado
        if (esTitular || esPerfilTermicoOficial) {
            btnEliminar.setVisibility(View.GONE);
        } else {
            btnEliminar.setVisibility(View.VISIBLE);
            btnEliminar.setOnClickListener(v -> mostrarPopupConfirmarEliminacion());
        }
    }

    private void inicializarVistas() {
        tvSubtituloEditarPerfil = findViewById(R.id.tvSubtituloEditarPerfil);
        tvAvatarInicialEditar = findViewById(R.id.tvAvatarInicialEditar);
        tvHeaderNombreEditar = findViewById(R.id.tvHeaderNombreEditar);
        tvHeaderEdadEditar = findViewById(R.id.tvHeaderEdadEditar);
        tvHeaderSensacionEditar = findViewById(R.id.tvHeaderSensacionEditar);

        etNombrePerfilEditar = findViewById(R.id.etNombrePerfilEditar);
        etEdadPerfilEditar = findViewById(R.id.etEdadPerfilEditar);

        cardOpcionFrio = findViewById(R.id.cardOpcionFrio);
        cardOpcionCalor = findViewById(R.id.cardOpcionCalor);
        tvLabelFrio = findViewById(R.id.tvLabelFrio);
        tvLabelCalor = findViewById(R.id.tvLabelCalor);

        etNombrePerfilEditar.setText(nombreActual);

        if (esPerfilTermicoOficial) {
            // Si es perfil térmico, en el campo edad mostramos temporalmente su valor de ajuste numérico (-5, +5, etc.)
            etEdadPerfilEditar.setText(String.valueOf(ajusteActual));
            tvHeaderEdadEditar.setText("Ajuste térmico: " + ajusteActual + "°C");
        } else {
            if (edadActual > 0) {
                etEdadPerfilEditar.setText(String.valueOf(edadActual));
            } else {
                etEdadPerfilEditar.setText("");
            }
        }

        seleccionarSensacion(sensacionActual != null && (sensacionActual.contains("frío") || sensacionActual.contains("Ajuste")));
    }

    private void configurarEscuchadoresTexto() {
        etNombrePerfilEditar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String n = s.toString().trim();
                if (!n.isEmpty()) {
                    tvAvatarInicialEditar.setText(String.valueOf(n.charAt(0)).toUpperCase());
                    tvHeaderNombreEditar.setText(n + (esTitular ? " (Titular)" : ""));
                } else {
                    tvAvatarInicialEditar.setText("—");
                    tvHeaderNombreEditar.setText("Perfil");
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etEdadPerfilEditar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String e = s.toString().trim();
                if (!e.isEmpty()) {
                    if (esPerfilTermicoOficial) {
                        tvHeaderEdadEditar.setText("Ajuste térmico: " + e + "°C");
                    } else {
                        tvHeaderEdadEditar.setText(e + " años aprox.");
                    }
                } else {
                    tvHeaderEdadEditar.setText(esPerfilTermicoOficial ? "Ajuste sin definir" : "Edad sin definir");
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void seleccionarSensacion(boolean sienteFrio) {
        sensacionActual = sienteFrio ? "Suele tener frío" : "Suele tener calor";

        if (sienteFrio) {
            cardOpcionFrio.setCardBackgroundColor(Color.parseColor("#EAEFE5"));
            cardOpcionFrio.setStrokeColor(Color.parseColor("#4F6342"));
            tvLabelFrio.setTypeface(null, Typeface.BOLD);
            tvLabelFrio.setTextColor(Color.parseColor("#4F6342"));

            cardOpcionCalor.setCardBackgroundColor(Color.WHITE);
            cardOpcionCalor.setStrokeColor(Color.parseColor("#D4D6CF"));
            tvLabelCalor.setTypeface(null, Typeface.NORMAL);
            tvLabelCalor.setTextColor(Color.parseColor("#686D65"));

            tvHeaderSensacionEditar.setText("❄️  Sensibilidad baja / Frío");
        } else {
            cardOpcionCalor.setCardBackgroundColor(Color.parseColor("#EAEFE5"));
            cardOpcionCalor.setStrokeColor(Color.parseColor("#4F6342"));
            tvLabelCalor.setTypeface(null, Typeface.BOLD);
            tvLabelCalor.setTextColor(Color.parseColor("#4F6342"));

            cardOpcionFrio.setCardBackgroundColor(Color.WHITE);
            cardOpcionFrio.setStrokeColor(Color.parseColor("#D4D6CF"));
            tvLabelFrio.setTypeface(null, Typeface.NORMAL);
            tvLabelFrio.setTextColor(Color.parseColor("#686D65"));

            tvHeaderSensacionEditar.setText("☀️  Sensibilidad alta / Calor");
        }
    }

    private void actualizarCabecera() {
        String nombreMostrado = (nombreActual != null && !nombreActual.isEmpty()) ? nombreActual : "Perfil";
        tvSubtituloEditarPerfil.setText(esPerfilTermicoOficial ? "Ajusta el nombre y el valor térmico del perfil IoT." : "Preferencias del perfil.");

        if (!nombreMostrado.isEmpty()) {
            tvAvatarInicialEditar.setText(String.valueOf(nombreMostrado.charAt(0)).toUpperCase());
        } else {
            tvAvatarInicialEditar.setText("—");
        }

        tvHeaderNombreEditar.setText(nombreMostrado);
        if (esPerfilTermicoOficial) {
            tvHeaderEdadEditar.setText("Ajuste térmico: " + ajusteActual + "°C");
        } else {
            tvHeaderEdadEditar.setText(edadActual > 0 ? (edadActual + " años aprox.") : "Edad sin definir");
        }
    }

    private void guardarCambiosFirestore() {
        String nuevoNombre = etNombrePerfilEditar.getText().toString().trim();
        String valorStr = etEdadPerfilEditar.getText().toString().trim();

        if (nuevoNombre.isEmpty()) {
            etNombrePerfilEditar.setError("El nombre es obligatorio");
            etNombrePerfilEditar.requestFocus();
            return;
        }

        if (valorStr.isEmpty()) {
            etEdadPerfilEditar.setError("Indica un valor");
            etEdadPerfilEditar.requestFocus();
            return;
        }

        if (user != null) {
            if (esPerfilTermicoOficial) {
                // 🔴 GUARDAR CAMBIOS PARA LOS 4 PERFILES TÉRMICOS OFICIALES (perfil_1 a perfil_4)
                double nuevoAjuste;
                try {
                    nuevoAjuste = Double.parseDouble(valorStr);
                } catch (NumberFormatException e) {
                    etEdadPerfilEditar.setError("Introduce un número válido (ej. -5 o 5)");
                    return;
                }

                Map<String, Object> datosPerfilMap = new HashMap<>();
                datosPerfilMap.put("nombre", nuevoNombre);
                datosPerfilMap.put("ajuste", nuevoAjuste);

                Map<String, Object> actualizacionFinal = new HashMap<>();
                actualizacionFinal.put(idFamiliar, datosPerfilMap);

                db.collection("usuarios").document(user.getUid())
                        .collection("configuracion").document("perfiles")
                        .set(actualizacionFinal, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Perfil térmico actualizado con éxito", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            } else if (esTitular) {
                // Actualiza datos del titular
                int nuevaEdad;
                try { nuevaEdad = Integer.parseInt(valorStr); } catch (Exception ex) { nuevaEdad = 25; }

                Map<String, Object> datosTitular = new HashMap<>();
                datosTitular.put("nombre", nuevoNombre);
                datosTitular.put("edad", nuevaEdad);
                datosTitular.put("sensacionTermica", sensacionActual);

                db.collection("usuarios").document(user.getUid())
                        .set(datosTitular, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Perfil titular actualizado", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show());
            } else {
                // Perfil familiar secundario normal
                int nuevaEdad;
                try { nuevaEdad = Integer.parseInt(valorStr); } catch (Exception ex) { nuevaEdad = 25; }

                Familiar actualizado = new Familiar(idFamiliar, nuevoNombre, "Familiar", colorHexActual, false, nuevaEdad, sensacionActual);
                db.collection("usuarios").document(user.getUid())
                        .collection("familiares").document(idFamiliar)
                        .set(actualizado, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void mostrarPopupConfirmarEliminacion() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_confirmar_eliminacion);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitulo = dialog.findViewById(R.id.tvTituloEliminarModal);
        TextView tvInicial = dialog.findViewById(R.id.tvInicialModal);
        TextView tvNombreEdad = dialog.findViewById(R.id.tvResumenNombreEdadModal);
        TextView tvSensacion = dialog.findViewById(R.id.tvResumenSensacionModal);

        String nombreMostrado = etNombrePerfilEditar.getText().toString().trim();
        String valorMostrado = etEdadPerfilEditar.getText().toString().trim();

        tvTitulo.setText("¿Eliminar perfil " + nombreMostrado + "?");
        tvInicial.setText(!nombreMostrado.isEmpty() ? String.valueOf(nombreMostrado.charAt(0)).toUpperCase() : "—");
        tvNombreEdad.setText(nombreMostrado);
        tvSensacion.setText(sensacionActual);

        dialog.findViewById(R.id.btnCerrarModalX).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnCancelarEliminarModal).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btnConfirmarEliminarDefinitivo).setOnClickListener(v -> {
            if (user != null && !esTitular && !esPerfilTermicoOficial) {
                db.collection("usuarios").document(user.getUid())
                        .collection("familiares").document(idFamiliar)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Perfil eliminado", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show());
            }
        });

        dialog.show();
    }
}