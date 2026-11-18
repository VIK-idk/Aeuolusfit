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
    private boolean esTitular = false;

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

        // 1. Recoger datos pasados por el Intent (Unidad 3 - 3.2 Comunicación entre actividades)
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            idFamiliar = extras.getString("id", "titular");
            nombreActual = extras.getString("nombre", "");
            edadActual = extras.getInt("edad", 0);
            sensacionActual = extras.getString("sensacion", "Suele tener frío");
            colorHexActual = extras.getString("colorHex", "#E8CEBF");
            esTitular = extras.getBoolean("esTitular", false);
        } else {
            idFamiliar = "titular";
            nombreActual = "";
            edadActual = 0;
            sensacionActual = "Suele tener frío";
            colorHexActual = "#E8CEBF";
            esTitular = true;
        }

        inicializarVistas();
        configurarEscuchadoresTexto();
        actualizarCabecera();

        findViewById(R.id.tvVolverFamilia).setOnClickListener(v -> finish());

        cardOpcionFrio.setOnClickListener(v -> seleccionarSensacion(true));
        cardOpcionCalor.setOnClickListener(v -> seleccionarSensacion(false));

        MaterialButton btnGuardar = findViewById(R.id.btnGuardarPerfilFamiliar);
        btnGuardar.setOnClickListener(v -> guardarCambiosFirestore());

        btnEliminar = findViewById(R.id.btnAbrirModalEliminar);
        // Si es el titular, protegemos la cuenta ocultando la opción de borrado
        if (esTitular) {
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
        if (edadActual > 0) {
            etEdadPerfilEditar.setText(String.valueOf(edadActual));
        } else {
            etEdadPerfilEditar.setText("");
        }

        seleccionarSensacion(sensacionActual != null && sensacionActual.contains("frío"));
    }

    private void configurarEscuchadoresTexto() {
        // Actualiza el avatar e inicial en vivo mientras el usuario teclea
        etNombrePerfilEditar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String n = s.toString().trim();
                if (!n.isEmpty()) {
                    tvAvatarInicialEditar.setText(String.valueOf(n.charAt(0)).toUpperCase());
                    tvHeaderNombreEditar.setText(n + (esTitular ? " (Titular)" : ""));
                    tvSubtituloEditarPerfil.setText(esTitular ? "Tus preferencias personales." : "Pequeños detalles para cuidar de " + n + ".");
                } else {
                    tvAvatarInicialEditar.setText("—");
                    tvHeaderNombreEditar.setText(esTitular ? "Titular" : "Familiar");
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Actualiza la edad en vivo en la tarjeta de cabecera
        etEdadPerfilEditar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String e = s.toString().trim();
                if (!e.isEmpty()) {
                    tvHeaderEdadEditar.setText(e + " años aprox.");
                } else {
                    tvHeaderEdadEditar.setText("Edad sin definir");
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

            tvHeaderSensacionEditar.setText("❄️  Suele tener frío");
        } else {
            cardOpcionCalor.setCardBackgroundColor(Color.parseColor("#EAEFE5"));
            cardOpcionCalor.setStrokeColor(Color.parseColor("#4F6342"));
            tvLabelCalor.setTypeface(null, Typeface.BOLD);
            tvLabelCalor.setTextColor(Color.parseColor("#4F6342"));

            cardFrioOnboarding(Color.WHITE);
            cardOpcionFrio.setCardBackgroundColor(Color.WHITE);
            cardOpcionFrio.setStrokeColor(Color.parseColor("#D4D6CF"));
            tvLabelFrio.setTypeface(null, Typeface.NORMAL);
            tvLabelFrio.setTextColor(Color.parseColor("#686D65"));

            tvHeaderSensacionEditar.setText("☀️  Suele tener calor");
        }
    }

    private void cardFrioOnboarding(int white) {
        cardOpcionFrio.setCardBackgroundColor(white);
    }

    private void actualizarCabecera() {
        String nombreMostrado = (nombreActual != null && !nombreActual.isEmpty()) ? nombreActual : (esTitular ? "Titular" : "Familiar");
        tvSubtituloEditarPerfil.setText(esTitular ? "Tus preferencias personales." : "Pequeños detalles para cuidar de " + nombreMostrado + ".");

        if (!nombreMostrado.isEmpty()) {
            tvAvatarInicialEditar.setText(String.valueOf(nombreMostrado.charAt(0)).toUpperCase());
        } else {
            tvAvatarInicialEditar.setText("—");
        }

        tvHeaderNombreEditar.setText(nombreMostrado + (esTitular ? " (Titular)" : ""));
        tvHeaderEdadEditar.setText(edadActual > 0 ? (edadActual + " años aprox.") : "Edad sin definir");
    }

    private void guardarCambiosFirestore() {
        String nuevoNombre = etNombrePerfilEditar.getText().toString().trim();
        String edadStr = etEdadPerfilEditar.getText().toString().trim();

        if (nuevoNombre.isEmpty()) {
            etNombrePerfilEditar.setError("El nombre es obligatorio");
            etNombrePerfilEditar.requestFocus();
            return;
        }

        if (edadStr.isEmpty()) {
            etEdadPerfilEditar.setError("Indica la edad");
            etEdadPerfilEditar.requestFocus();
            return;
        }

        int nuevaEdad;
        try {
            nuevaEdad = Integer.parseInt(edadStr);
            if (nuevaEdad <= 0 || nuevaEdad > 120) {
                etEdadPerfilEditar.setError("Introduce una edad válida");
                etEdadPerfilEditar.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etEdadPerfilEditar.setError("Formato incorrecto");
            return;
        }

        if (user != null) {
            if (esTitular) {
                // Actualiza los datos raíz del titular de la cuenta
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
                        .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar perfil", Toast.LENGTH_SHORT).show());
            } else {
                // Actualiza el familiar secundario en la subcolección
                Familiar actualizado = new Familiar(idFamiliar, nuevoNombre, "Familiar", colorHexActual, false, nuevaEdad, sensacionActual);
                db.collection("usuarios").document(user.getUid())
                        .collection("familiares").document(idFamiliar)
                        .set(actualizado, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar cambios", Toast.LENGTH_SHORT).show());
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
        String edadMostrada = etEdadPerfilEditar.getText().toString().trim();

        tvTitulo.setText("¿Eliminar el perfil de " + nombreMostrado + "?");
        tvInicial.setText(!nombreMostrado.isEmpty() ? String.valueOf(nombreMostrado.charAt(0)).toUpperCase() : "—");
        tvNombreEdad.setText(nombreMostrado + (!edadMostrada.isEmpty() ? " · " + edadMostrada + " años aprox." : ""));
        tvSensacion.setText(sensacionActual);

        dialog.findViewById(R.id.btnCerrarModalX).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnCancelarEliminarModal).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btnConfirmarEliminarDefinitivo).setOnClickListener(v -> {
            if (user != null && !esTitular) {
                db.collection("usuarios").document(user.getUid())
                        .collection("familiares").document(idFamiliar)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Perfil eliminado", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error al eliminar perfil", Toast.LENGTH_SHORT).show());
            }
        });

        dialog.show();
    }
}