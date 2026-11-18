package com.example.aeuolusfit;

import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private final String[] nombresPestanas = new String[]{"Hoy", "Familia", "Armario"};
    private final int[] iconosPestanas = new int[]{
            android.R.drawable.ic_menu_day,
            android.R.drawable.ic_menu_myplaces,
            android.R.drawable.ic_menu_gallery
    };

    // Notificaciones (Unidad 6)
    private NotificationManager notificationManager;
    static final String CANAL_ID = "canal_aeolusfit";
    static final int NOTIFICACION_LLUVIA_ID = 1;
    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                Boolean notif = permissions.getOrDefault(android.Manifest.permission.POST_NOTIFICATIONS, false);
                if (Boolean.FALSE.equals(notif)) {
                    Toast.makeText(this, "Sin notificaciones no recibirás avisos de lluvia", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        DatosInicialesFirestore.cargarDatosCompletos(this);

        if (currentUser == null) {
            irAlLogin();
            return;
        }

        // Configurar Pestañas con ViewPager2 y TabLayoutMediator (Unidad 2 - 2.8)
        ViewPager2 viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(new MiPagerAdapter(this));

        TabLayout tabs = findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            tab.setText(nombresPestanas[position]);
            tab.setIcon(iconosPestanas[position]);
        }).attach();

        // Configurar botones de cabecera
        MaterialCardView btnPerfil = findViewById(R.id.btnPerfil);
        btnPerfil.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            irAlLogin();
        });

        TextView tvAcercaDe = findViewById(R.id.tvAcercaDe);
        tvAcercaDe.setOnClickListener(v -> mostrarDialogoAcercaDe());

        crearCanalNotificaciones();
        solicitarPermisosNecesarios();


    }
    private void solicitarPermisosNecesarios() {
        List<String> permisos = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permisos.add(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permisos.add(android.Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        if (!permisos.isEmpty()) {
            requestPermissionsLauncher.launch(permisos.toArray(new String[0]));
        }
    }

    // Adaptador de pestañas según el PDF Ud2
    public static class MiPagerAdapter extends FragmentStateAdapter {
        public MiPagerAdapter(FragmentActivity activity) {
            super(activity);
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new HoyFragment();
                case 1: return new FamiliaFragment();
                case 2: return new ArmarioFragment();
                default: return new HoyFragment();
            }
        }
    }

    // Método público para lanzar alerta climática en barra de estado (Unidad 6)
    public void lanzarNotificacionClimatica(String titulo, String texto) {
        NotificationCompat.Builder notificacion = new NotificationCompat.Builder(this, CANAL_ID)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setAutoCancel(true);
        notificationManager.notify(NOTIFICACION_LLUVIA_ID, notificacion.build());
    }

    private void crearCanalNotificaciones() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CANAL_ID, "Alertas Climáticas AeolusFit",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Avisos de lluvia y recomendaciones del armario IoT");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void mostrarDialogoAcercaDe() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_acerca_de);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        Button btnCerrar = dialog.findViewById(R.id.btnCerrarDialog);
        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void irAlLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}