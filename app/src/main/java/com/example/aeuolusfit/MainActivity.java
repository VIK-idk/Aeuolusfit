package com.example.aeuolusfit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        // 1. Proteger la pantalla: Si no hay usuario, devolver al Login
        if (currentUser == null) {
            irAlLogin();
            return;
        }

        // 2. Personalizar el saludo con el nombre del usuario
        TextView tvSaludo = findViewById(R.id.tvSaludo);
        String nombre = currentUser.getDisplayName();
        if (nombre != null && !nombre.isEmpty()) {
            tvSaludo.setText("¡Buenos días, " + nombre + "!");
        }

        // 3. Botón de Perfil -> Usado temporalmente para Cerrar Sesión
        MaterialCardView btnPerfil = findViewById(R.id.btnPerfil);
        btnPerfil.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            irAlLogin();
        });

        // 4. Botón Acerca de -> Muestra el Popup
        TextView tvAcercaDe = findViewById(R.id.tvAcercaDe);
        tvAcercaDe.setOnClickListener(v -> {
            mostrarDialogoAcercaDe();
        });

        // 5. Botón Ver Conjunto Completo
        MaterialButton btnVerConjunto = findViewById(R.id.btnVerConjunto);
        btnVerConjunto.setOnClickListener(v -> {
            Toast.makeText(this, "Abriendo detalles del conjunto...", Toast.LENGTH_SHORT).show();
        });

        // 6. Clics en la Barra de Navegación Inferior
        LinearLayout navHoy = findViewById(R.id.navHoy);
        LinearLayout navFamilia = findViewById(R.id.navFamilia);
        LinearLayout navArmario = findViewById(R.id.navArmario);

        navHoy.setOnClickListener(v -> {
            // Ya estamos en Hoy
        });

        navFamilia.setOnClickListener(v -> {
            Toast.makeText(this, "Sección Familia en construcción", Toast.LENGTH_SHORT).show();
        });

        navArmario.setOnClickListener(v -> {
            Toast.makeText(this, "Sección Armario en construcción", Toast.LENGTH_SHORT).show();
        });
    }

    private void mostrarDialogoAcercaDe() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_acerca_de);

        // Hacemos el fondo transparente para que se vean las esquinas redondeadas de tu tarjeta
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Button btnCerrar = dialog.findViewById(R.id.btnCerrarDialog);
        btnCerrar.setOnClickListener(v -> dialog.dismiss()); // Cierra el popup al hacer clic

        dialog.show();
    }

    private void irAlLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        // Limpiamos el historial para que no pueda volver a esta pantalla pulsando "Atrás"
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}