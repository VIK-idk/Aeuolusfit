package com.example.aeuolusfit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MiPerfilActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_perfil);

        ImageButton btnVolver = findViewById(R.id.btnVolver);
        TextView tvNombre = findViewById(R.id.tvNombrePerfil);
        TextView tvEmail = findViewById(R.id.tvEmailPerfil);
        CardView btnAcercaDe = findViewById(R.id.btnAcercaDe);
        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // Cargar datos del usuario
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            tvEmail.setText(user.getEmail());
            String nombre = user.getEmail().split("@")[0];
            tvNombre.setText(nombre.substring(0, 1).toUpperCase() + nombre.substring(1));
        }

        btnVolver.setOnClickListener(v -> finish());

        // Mostrar el Acerca De (usando el dialog_acerca_de.xml que ya tienes)
        btnAcercaDe.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_acerca_de, null);
            builder.setView(view);
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        // Cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MiPerfilActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}