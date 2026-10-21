package com.example.aeuolusfit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    private EditText etNombre, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvIrLoginHeader, tvVolverLoginForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        auth = FirebaseAuth.getInstance();

        // Enlazar los IDs exactos de tu XML register
        etNombre = findViewById(R.id.etNombreRegister);
        etEmail = findViewById(R.id.etEmailRegister);
        etPassword = findViewById(R.id.etPasswordRegister);
        etConfirmPassword = findViewById(R.id.etConfirmPasswordRegister);
        btnRegister = findViewById(R.id.btnRegister);
        tvIrLoginHeader = findViewById(R.id.tvIrLoginHeader);
        tvVolverLoginForm = findViewById(R.id.tvVolverLoginForm);

        // Click en Registrar
        btnRegister.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Guardar el nombre en el perfil de Firebase del usuario
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(nombre)
                                        .build();
                                user.updateProfile(profileUpdates);
                            }

                            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                            finish(); // Vuelve al Login
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Clics para volver al Login desde la cabecera o desde el formulario
        tvIrLoginHeader.setOnClickListener(v -> finish());
        tvVolverLoginForm.setOnClickListener(v -> finish());
    }
}