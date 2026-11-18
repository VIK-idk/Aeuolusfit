package com.example.aeuolusfit;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
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

public class ConectarDispositivoActivity extends AppCompatActivity {

    private int pasoActual = 0;
    private boolean conectaArmario = true;
    private String codigoDispositivo = "A001";

    private View barraPaso1, barraPaso2, barraPaso3;
    private TextView tvLabelPaso1, tvLabelPaso2, tvLabelPaso3;
    private TextView tvTituloWizard, tvSubtituloWizard;
    private MaterialCardView cardBadgeEstadoWizard, cardElegirArmario, cardElegirModulo;
    private MaterialCardView cardResumenDispositivoWizard, cardBannerInfoWizard;
    private TextView tvIconoBadgeWizard, tvNombreDispositivoElegido, tvSubestadoDispositivoElegido;
    private TextView tvIconoInst1, tvTituloInst1, tvDescInst1, tvBannerInfoWizard, tvVolverInferiorWizard;
    private LinearLayout layoutSelectorDispositivo, layoutPasosDetalle, layoutInstruccion2;
    private RadioButton rbArmario, rbModulo;
    private MaterialButton btnContinuarWizard;

    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conectar_dispositivo);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        inicializarVistas();
        configurarEventos();
        mostrarPaso(0);
    }

    private void inicializarVistas() {
        barraPaso1 = findViewById(R.id.barraPaso1);
        barraPaso2 = findViewById(R.id.barraPaso2);
        barraPaso3 = findViewById(R.id.barraPaso3);
        tvLabelPaso1 = findViewById(R.id.tvLabelPaso1);
        tvLabelPaso2 = findViewById(R.id.tvLabelPaso2);
        tvLabelPaso3 = findViewById(R.id.tvLabelPaso3);

        tvTituloWizard = findViewById(R.id.tvTituloWizard);
        tvSubtituloWizard = findViewById(R.id.tvSubtituloWizard);
        cardBadgeEstadoWizard = findViewById(R.id.cardBadgeEstadoWizard);
        tvIconoBadgeWizard = findViewById(R.id.tvIconoBadgeWizard);

        layoutSelectorDispositivo = findViewById(R.id.layoutSelectorDispositivo);
        cardElegirArmario = findViewById(R.id.cardElegirArmario);
        cardElegirModulo = findViewById(R.id.cardElegirModulo);
        rbArmario = findViewById(R.id.rbArmario);
        rbModulo = findViewById(R.id.rbModulo);

        layoutPasosDetalle = findViewById(R.id.layoutPasosDetalle);
        cardResumenDispositivoWizard = findViewById(R.id.cardResumenDispositivoWizard);
        tvNombreDispositivoElegido = findViewById(R.id.tvNombreDispositivoElegido);
        tvSubestadoDispositivoElegido = findViewById(R.id.tvSubestadoDispositivoElegido);

        tvIconoInst1 = findViewById(R.id.tvIconoInst1);
        tvTituloInst1 = findViewById(R.id.tvTituloInst1);
        tvDescInst1 = findViewById(R.id.tvDescInst1);
        layoutInstruccion2 = findViewById(R.id.layoutInstruccion2);

        cardBannerInfoWizard = findViewById(R.id.cardBannerInfoWizard);
        tvBannerInfoWizard = findViewById(R.id.tvBannerInfoWizard);

        btnContinuarWizard = findViewById(R.id.btnContinuarWizard);
        tvVolverInferiorWizard = findViewById(R.id.tvVolverInferiorWizard);
    }

    private void configurarEventos() {
        findViewById(R.id.tvVolverArmarioTop).setOnClickListener(v -> finish());

        cardElegirArmario.setOnClickListener(v -> seleccionarDispositivo(true));
        cardElegirModulo.setOnClickListener(v -> seleccionarDispositivo(false));

        btnContinuarWizard.setOnClickListener(v -> {
            if (pasoActual == 0) {
                mostrarPaso(1);
            } else if (pasoActual == 1) {
                mostrarPaso(2);
            } else if (pasoActual == 2) {
                // Pedir código prototipo (A001 / M001) para la demo
                pedirCodigoDispositivo();
            } else if (pasoActual == 3) {
                mostrarPaso(4);
            } else if (pasoActual == 4) {
                finish();
            } else if (pasoActual == 5) {
                mostrarPaso(2);
            }
        });

        tvVolverInferiorWizard.setOnClickListener(v -> {
            if (pasoActual <= 0 || pasoActual == 4) finish();
            else if (pasoActual == 5) mostrarPaso(1);
            else mostrarPaso(pasoActual - 1);
        });
    }

    private void pedirCodigoDispositivo() {
        final EditText input = new EditText(this);
        input.setHint("Ej: " + (conectaArmario ? "A001" : "M001"));
        input.setText(conectaArmario ? "A001" : "M001");
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4)});

        new AlertDialog.Builder(this)
                .setTitle("Código del prototipo")
                .setMessage("Introduce el identificador de 1 letra y 3 números:")
                .setView(input)
                .setPositiveButton("Vincular", (dialog, which) -> {
                    String cod = input.getText().toString().trim().toUpperCase();
                    String codigoValido = conectaArmario ? "A001" : "M001";
                    if (cod.matches("^[A-Z]\\d{3}$") && cod.equals(codigoValido)) {
                        this.codigoDispositivo = cod;
                        mostrarPaso(3);
                    } else {
                        Toast.makeText(this, "Dispositivo no reconocido. Verifica el código.", Toast.LENGTH_LONG).show();
                        mostrarPaso(5); // Pantalla de error
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void seleccionarDispositivo(boolean esArmario) {
        this.conectaArmario = esArmario;
        rbArmario.setChecked(esArmario);
        rbModulo.setChecked(!esArmario);

        if (esArmario) {
            cardElegirArmario.setCardBackgroundColor(Color.parseColor("#EAEFE5"));
            cardElegirArmario.setStrokeColor(Color.parseColor("#4F6342"));
            cardElegirModulo.setCardBackgroundColor(Color.WHITE);
            cardElegirModulo.setStrokeColor(Color.parseColor("#D4D6CF"));
        } else {
            cardElegirModulo.setCardBackgroundColor(Color.parseColor("#EAEFE5"));
            cardElegirModulo.setStrokeColor(Color.parseColor("#4F6342"));
            cardElegirArmario.setCardBackgroundColor(Color.WHITE);
            cardElegirArmario.setStrokeColor(Color.parseColor("#D4D6CF"));
        }
    }

    private void mostrarPaso(int paso) {
        this.pasoActual = paso;
        String nombreDisp = conectaArmario ? "👕  Armario inteligente" : "🌡️  Módulo exterior";
        tvNombreDispositivoElegido.setText(nombreDisp);

        int verde = Color.parseColor("#4F6342");
        int gris = Color.parseColor("#E0DED7");
        barraPaso1.setBackgroundColor(paso >= 1 ? verde : gris);
        barraPaso2.setBackgroundColor(paso >= 2 ? verde : gris);
        barraPaso3.setBackgroundColor((paso == 3 || paso == 4) ? verde : gris);

        tvLabelPaso1.setTypeface(null, paso == 1 ? Typeface.BOLD : Typeface.NORMAL);
        tvLabelPaso2.setTypeface(null, (paso == 2 || paso == 5) ? Typeface.BOLD : Typeface.NORMAL);
        tvLabelPaso3.setTypeface(null, (paso == 3 || paso == 4) ? Typeface.BOLD : Typeface.NORMAL);

        if (paso == 0) {
            layoutSelectorDispositivo.setVisibility(View.VISIBLE);
            layoutPasosDetalle.setVisibility(View.GONE);
            cardBadgeEstadoWizard.setVisibility(View.GONE);
            tvTituloWizard.setText("Conecta tu hogar");
            tvSubtituloWizard.setText("Un pequeño paso para cuidar de tu día. Te acompañamos de principio a fin.");
            btnContinuarWizard.setText("Continuar");
            tvVolverInferiorWizard.setText("Volver a Armario");

        } else if (paso == 1) {
            layoutSelectorDispositivo.setVisibility(View.GONE);
            layoutPasosDetalle.setVisibility(View.VISIBLE);
            cardBadgeEstadoWizard.setVisibility(View.GONE);
            layoutInstruccion2.setVisibility(View.VISIBLE);
            cardBannerInfoWizard.setVisibility(View.GONE);

            tvTituloWizard.setText("Preparar dispositivo");
            tvSubtituloWizard.setText("Empecemos con calma. Deja tu armario listo para conectar.");
            tvSubestadoDispositivoElegido.setText("Dispositivo elegido");
            tvIconoInst1.setText("⏻");
            tvTituloInst1.setText("Comprueba que está encendido");
            tvDescInst1.setText("Antes de continuar, asegúrate de que tu armario esté encendido.");
            btnContinuarWizard.setText("Continuar");
            tvVolverInferiorWizard.setText("Volver");

        } else if (paso == 2) {
            layoutSelectorDispositivo.setVisibility(View.GONE);
            layoutPasosDetalle.setVisibility(View.VISIBLE);
            cardBadgeEstadoWizard.setVisibility(View.GONE);
            layoutInstruccion2.setVisibility(View.GONE);
            cardBannerInfoWizard.setVisibility(View.VISIBLE);

            tvTituloWizard.setText("Conectar");
            tvSubtituloWizard.setText("Vamos a encontrar tu dispositivo e iniciar la vinculación.");
            tvSubestadoDispositivoElegido.setText("Listo para iniciar la búsqueda");
            tvIconoInst1.setText("🔍");
            tvTituloInst1.setText("Inicia la búsqueda");
            tvDescInst1.setText("Mantén el dispositivo encendido y pulsa Continuar para introducir su código.");
            cardBannerInfoWizard.setCardBackgroundColor(Color.parseColor("#EAEFE5"));
            tvBannerInfoWizard.setTextColor(Color.parseColor("#4F6342"));
            tvBannerInfoWizard.setText("🌿  No necesitas hacerlo deprisa. Te guiaremos en cada paso.");
            btnContinuarWizard.setText("Introducir código");
            tvVolverInferiorWizard.setText("Volver");

        } else if (paso == 3) {
            cardBadgeEstadoWizard.setVisibility(View.VISIBLE);
            tvIconoBadgeWizard.setText("↻");
            tvIconoBadgeWizard.setTextColor(Color.parseColor("#4F6342"));
            layoutInstruccion2.setVisibility(View.GONE);
            cardBannerInfoWizard.setVisibility(View.VISIBLE);

            tvTituloWizard.setText("Comprobar conexión");
            tvSubtituloWizard.setText("Dispositivo " + codigoDispositivo + " localizado. Comprobando respuesta...");
            tvSubestadoDispositivoElegido.setText("Vinculado · Comprobación en curso");
            tvIconoInst1.setText("📋");
            tvTituloInst1.setText("Un último vistazo");
            tvDescInst1.setText("Estamos verificando los sensores IoT.");
            cardBannerInfoWizard.setCardBackgroundColor(Color.parseColor("#EAEFE5"));
            tvBannerInfoWizard.setTextColor(Color.parseColor("#4F6342"));
            tvBannerInfoWizard.setText("↻  Verificando conexión. Espera un instante...");
            btnContinuarWizard.setText("Verificando...");
            btnContinuarWizard.setEnabled(false);

            new Thread(() -> {
                SystemClock.sleep(1500);
                runOnUiThread(() -> {
                    btnContinuarWizard.setEnabled(true);
                    if (pasoActual == 3) {
                        mostrarPaso(4);
                    }
                });
            }).start();

        } else if (paso == 4) {
            cardBadgeEstadoWizard.setVisibility(View.VISIBLE);
            tvIconoBadgeWizard.setText("✓");
            tvIconoBadgeWizard.setTextColor(Color.parseColor("#4F6342"));
            layoutInstruccion2.setVisibility(View.GONE);
            cardBannerInfoWizard.setVisibility(View.VISIBLE);

            tvTituloWizard.setText("Conectado correctamente");
            tvSubtituloWizard.setText("Dispositivo " + codigoDispositivo + " conectado y sincronizado con Firestore.");
            tvSubestadoDispositivoElegido.setText("Conectado · Conexión comprobada");
            tvIconoInst1.setText("✓");
            tvTituloInst1.setText("Dispositivo vinculado (" + codigoDispositivo + ")");
            tvDescInst1.setText("✓ Conexión verificada");
            cardBannerInfoWizard.setCardBackgroundColor(Color.parseColor("#EAEFE5"));
            tvBannerInfoWizard.setTextColor(Color.parseColor("#4F6342"));
            tvBannerInfoWizard.setText("🌿  Ya puedes gestionarlo desde Armario.");
            btnContinuarWizard.setText("Continuar");
            tvVolverInferiorWizard.setText("Volver a Armario");

            guardarConexionEnFirestore();

        } else if (paso == 5) {
            cardBadgeEstadoWizard.setVisibility(View.VISIBLE);
            tvIconoBadgeWizard.setText("!");
            tvIconoBadgeWizard.setTextColor(Color.parseColor("#8C5C54"));
            layoutInstruccion2.setVisibility(View.GONE);
            cardBannerInfoWizard.setVisibility(View.VISIBLE);

            tvTituloWizard.setText("No hemos podido conectar");
            tvSubtituloWizard.setText("No se ha encontrado el dispositivo. Recuerda usar el código A001 o M001.");
            tvSubestadoDispositivoElegido.setText("Sin conexión · Vinculación no completada");
            tvIconoInst1.setText("⏻");
            tvTituloInst1.setText("Revisa lo más sencillo");
            tvDescInst1.setText("Comprueba que el dispositivo esté encendido y en rango de cobertura.");
            cardBannerInfoWizard.setCardBackgroundColor(Color.parseColor("#F4E9E6"));
            tvBannerInfoWizard.setTextColor(Color.parseColor("#8C5C54"));
            tvBannerInfoWizard.setText("🤍  No pasa nada. Puedes volver a intentarlo cuando quieras.");
            btnContinuarWizard.setText("Reintentar");
            tvVolverInferiorWizard.setText("Volver al paso anterior");
        }
    }

    private void guardarConexionEnFirestore() {
        if (user == null) return;
        Map<String, Object> datos = new HashMap<>();
        if (conectaArmario) {
            datos.put("armarioConectado", true);
            datos.put("codigoArmario", codigoDispositivo);
        } else {
            datos.put("moduloConectado", true);
            datos.put("codigoModulo", codigoDispositivo);
        }
        db.collection("usuarios").document(user.getUid())
                .collection("configuracion").document("iot")
                .set(datos, SetOptions.merge());
    }
}