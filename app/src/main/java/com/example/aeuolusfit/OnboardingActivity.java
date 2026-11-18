package com.example.aeuolusfit;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OnboardingActivity extends AppCompatActivity {

    private EditText etEdadOnboarding, etLocalidadOnboarding;
    private TextView tvTituloBienvenida, tvSubtituloBienvenida;
    private TextView tvLabelFrioOnb, tvLabelCalorOnb, tvEstadoGps;
    private MaterialCardView cardFrioOnboarding, cardCalorOnboarding;
    private MaterialCardView cardUsoPersonal, cardUsoFamiliar;
    private RadioButton rbUsoPersonal, rbUsoFamiliar;
    private MaterialButton btnFinalizarOnboarding;

    private String sensacionSeleccionada = "Suele tener frío";
    private String tipoUsoSeleccionado = "personal"; // "personal" o "familiar"
    private double latitud = 39.4699; // Coordenadas por defecto (Valencia)
    private double longitud = -0.3763;

    // Control de flujo: ¿Es el titular en su primera apertura o un nuevo familiar desde FamiliaFragment?
    private boolean esNuevoFamiliar = false;
    private String nombreFamiliar = "";

    private FirebaseFirestore db;
    private FirebaseUser user;

    // Lanzador para solicitar permisos en tiempo de ejecución (Unidad 5 UPV)
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                Boolean fineGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                    obtenerUbicacionYLocalidad();
                } else {
                    tvEstadoGps.setText("Permiso denegado. Puedes escribir tu localidad manualmente.");
                    Toast.makeText(this, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Leer si viene de FamiliaFragment para añadir un familiar secundario
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            esNuevoFamiliar = extras.getBoolean("esNuevoFamiliar", false);
            nombreFamiliar = extras.getString("nombreFamiliar", "");
        }

        inicializarVistas();
        configurarEventos();
    }

    private void inicializarVistas() {
        tvTituloBienvenida = findViewById(R.id.tvTituloBienvenida);
        etEdadOnboarding = findViewById(R.id.etEdadOnboarding);
        etLocalidadOnboarding = findViewById(R.id.etLocalidadOnboarding);
        tvLabelFrioOnb = findViewById(R.id.tvLabelFrioOnb);
        tvLabelCalorOnb = findViewById(R.id.tvLabelCalorOnb);
        tvEstadoGps = findViewById(R.id.tvEstadoGps);

        cardFrioOnboarding = findViewById(R.id.cardFrioOnboarding);
        cardCalorOnboarding = findViewById(R.id.cardCalorOnboarding);
        cardUsoPersonal = findViewById(R.id.cardUsoPersonal);
        cardUsoFamiliar = findViewById(R.id.cardUsoFamiliar);
        rbUsoPersonal = findViewById(R.id.rbUsoPersonal);
        rbUsoFamiliar = findViewById(R.id.rbUsoFamiliar);
        btnFinalizarOnboarding = findViewById(R.id.btnFinalizarOnboarding);

        // Adaptar textos si es un familiar secundario o el dueño de la cuenta
        if (esNuevoFamiliar) {
            tvTituloBienvenida.setText("Configurar perfil de " + nombreFamiliar);
            btnFinalizarOnboarding.setText("Guardar perfil familiar  ✓");
        } else if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            tvTituloBienvenida.setText("¡Hola, " + user.getDisplayName().split(" ")[0] + "!");
        }
    }

    private void configurarEventos() {
        // Selector Frío / Calor
        cardFrioOnboarding.setOnClickListener(v -> seleccionarSensacion(true));
        cardCalorOnboarding.setOnClickListener(v -> seleccionarSensacion(false));

        // Selector Personal / Familiar
        cardUsoPersonal.setOnClickListener(v -> seleccionarTipoUso(true));
        cardUsoFamiliar.setOnClickListener(v -> seleccionarTipoUso(false));

        // Botón GPS
        findViewById(R.id.btnDetectarUbicacion).setOnClickListener(v -> comprobarYSolicitarPermisosUbicacion());

        // Botón Finalizar
        btnFinalizarOnboarding.setOnClickListener(v -> guardarYContinuar());
    }

    private void seleccionarSensacion(boolean sienteFrio) {
        sensacionSeleccionada = sienteFrio ? "Suele tener frío" : "Suele tener calor";

        if (sienteFrio) {
            cardFrioOnboarding.setCardBackgroundColor(Color.parseColor("#EAEFE5"));
            cardFrioOnboarding.setStrokeColor(Color.parseColor("#4F6342"));
            tvLabelFrioOnb.setTypeface(null, Typeface.BOLD);
            tvLabelFrioOnb.setTextColor(Color.parseColor("#4F6342"));

            cardCalorOnboarding.setCardBackgroundColor(Color.WHITE);
            cardCalorOnboarding.setStrokeColor(Color.parseColor("#D4D6CF"));
            tvLabelCalorOnb.setTypeface(null, Typeface.NORMAL);
            tvLabelCalorOnb.setTextColor(Color.parseColor("#686D65"));
        } else {
            cardCalorOnboarding.setCardBackgroundColor(Color.parseColor("#EAEFE5"));
            cardCalorOnboarding.setStrokeColor(Color.parseColor("#4F6342"));
            tvLabelCalorOnb.setTypeface(null, Typeface.BOLD);
            tvLabelCalorOnb.setTextColor(Color.parseColor("#4F6342"));

            cardFrioOnboarding.setCardBackgroundColor(Color.WHITE);
            cardFrioOnboarding.setStrokeColor(Color.parseColor("#D4D6CF"));
            tvLabelFrioOnb.setTypeface(null, Typeface.NORMAL);
            tvLabelFrioOnb.setTextColor(Color.parseColor("#686D65"));
        }
    }

    private void seleccionarTipoUso(boolean esPersonal) {
        tipoUsoSeleccionado = esPersonal ? "personal" : "familiar";
        rbUsoPersonal.setChecked(esPersonal);
        rbUsoFamiliar.setChecked(!esPersonal);

        if (esPersonal) {
            cardUsoPersonal.setCardBackgroundColor(Color.parseColor("#EAEFE5"));
            cardUsoPersonal.setStrokeColor(Color.parseColor("#4F6342"));
            cardUsoFamiliar.setCardBackgroundColor(Color.WHITE);
            cardUsoFamiliar.setStrokeColor(Color.parseColor("#D4D6CF"));
        } else {
            cardUsoFamiliar.setCardBackgroundColor(Color.parseColor("#EAEFE5"));
            cardUsoFamiliar.setStrokeColor(Color.parseColor("#4F6342"));
            cardUsoPersonal.setCardBackgroundColor(Color.WHITE);
            cardUsoPersonal.setStrokeColor(Color.parseColor("#D4D6CF"));
        }
    }

    private void comprobarYSolicitarPermisosUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYLocalidad();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void obtenerUbicacionYLocalidad() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return;

        try {
            Location loc = null;
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (loc == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (loc != null) {
                this.latitud = loc.getLatitude();
                this.longitud = loc.getLongitude();

                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> direcciones = geocoder.getFromLocation(latitud, longitud, 1);
                if (direcciones != null && !direcciones.isEmpty()) {
                    String ciudad = direcciones.get(0).getLocality();
                    if (ciudad == null) ciudad = direcciones.get(0).getSubAdminArea();
                    if (ciudad != null) {
                        etLocalidadOnboarding.setText(ciudad);
                        tvEstadoGps.setText("✓  Ubicación detectada: " + ciudad);
                        tvEstadoGps.setTextColor(Color.parseColor("#4F6342"));
                    }
                }
            } else {
                tvEstadoGps.setText("GPS encendido pero sin señal fija. Escribe tu ciudad.");
            }
        } catch (SecurityException se) {
            tvEstadoGps.setText("Permiso de localización denegado.");
        } catch (Exception e) {
            tvEstadoGps.setText("Coordenadas leídas. Escribe el nombre de tu ciudad.");
        }
    }

    private void guardarYContinuar() {
        String edadStr = etEdadOnboarding.getText().toString().trim();
        String localidad = etLocalidadOnboarding.getText().toString().trim();

        if (edadStr.isEmpty()) {
            etEdadOnboarding.setError("Por favor, introduce tu edad");
            etEdadOnboarding.requestFocus();
            return;
        }

        int edad;
        try {
            edad = Integer.parseInt(edadStr);
            if (edad <= 0 || edad > 120) {
                etEdadOnboarding.setError("Introduce una edad válida");
                etEdadOnboarding.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etEdadOnboarding.setError("Formato de edad incorrecto");
            return;
        }

        if (localidad.isEmpty()) {
            localidad = "Valencia";
        }

        if (user == null) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        btnFinalizarOnboarding.setEnabled(false);

        if (esNuevoFamiliar) {
            // Caso 1: Creación de un familiar secundario desde FamiliaFragment
            String idFamiliar = String.valueOf(System.currentTimeMillis());
            String nombreFinal = (nombreFamiliar != null && !nombreFamiliar.isEmpty()) ? nombreFamiliar : "Familiar";

            Familiar nuevo = new Familiar(idFamiliar, nombreFinal, "Familiar", "#D9E2D5", false, edad, sensacionSeleccionada);
            nuevo.setTipoUso(tipoUsoSeleccionado);
            nuevo.setHistorialMemoriaIA(new ArrayList<>());

            db.collection("usuarios").document(user.getUid())
                    .collection("familiares").document(idFamiliar)
                    .set(nuevo)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Perfil de " + nuevo.getNombre() + " creado con éxito", Toast.LENGTH_SHORT).show();
                        finish(); // Regresa a FamiliaFragment
                    })
                    .addOnFailureListener(e -> {
                        btnFinalizarOnboarding.setEnabled(true);
                        Toast.makeText(this, "Error al guardar familiar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Caso 2: Onboarding inicial del titular de la cuenta
            Map<String, Object> datos = new HashMap<>();
            datos.put("edad", edad);
            datos.put("sensacionTermica", sensacionSeleccionada);
            datos.put("localidad", localidad);
            datos.put("latitud", latitud);
            datos.put("longitud", longitud);
            datos.put("tipoUsoArmario", tipoUsoSeleccionado);
            datos.put("onboardingCompletado", true);
            datos.put("historialMemoriaIA", new ArrayList<String>());

            final String locFinal = localidad;
            db.collection("usuarios").document(user.getUid())
                    .set(datos, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        // Actualizar la meteorología global en Aplicacion con las coordenadas detectadas
                        if (getApplication() instanceof Aplicacion) {
                            ((Aplicacion) getApplication()).consultarMeteorologiaGlobal(latitud, longitud, locFinal);
                        }

                        // Pasar a MainActivity y limpiar la pila de actividades
                        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnFinalizarOnboarding.setEnabled(true);
                        Toast.makeText(this, "Error al guardar perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}