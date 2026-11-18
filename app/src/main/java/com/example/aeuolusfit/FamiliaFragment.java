package com.example.aeuolusfit;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FamiliaFragment extends Fragment {

    private RecyclerView rvListaFamilia;
    private AdaptadorFamiliaLista adaptador;
    private final List<Familiar> listaFamilia = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_familia, container, false);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        rvListaFamilia = v.findViewById(R.id.rvListaFamilia);
        rvListaFamilia.setLayoutManager(new LinearLayoutManager(getContext()));
        adaptador = new AdaptadorFamiliaLista(listaFamilia);

        // Al pulsar sobre cualquier perfil, abre la pantalla de edición pasando sus datos (Ud3 - 3.2)
        adaptador.setOnFamiliarClickListener(f -> {
            Intent i = new Intent(getContext(), EditarPerfilActivity.class);
            i.putExtra("id", f.getId());
            i.putExtra("nombre", f.getNombre());
            i.putExtra("edad", f.getEdad());
            i.putExtra("sensacion", f.getSensacionTermica());
            i.putExtra("colorHex", f.getColorHex());
            i.putExtra("esTitular", "titular".equals(f.getId()));
            startActivity(i);
        });

        rvListaFamilia.setAdapter(adaptador);
        cargarPerfilDuenioYFamiliares();

        MaterialButton btnAnadir = v.findViewById(R.id.btnAnadirFamiliar);
        btnAnadir.setOnClickListener(view -> mostrarDialogoNuevoFamiliar());

        return v;
    }

    private void cargarPerfilDuenioYFamiliares() {
        if (user == null) return;
        String uid = user.getUid();

        // 1. Obtener los datos del dueño de la cuenta en Firestore
        db.collection("usuarios").document(uid).addSnapshotListener((doc, error) -> {
            String nombreTitular = "Tú";
            int edadTitular = 0;
            String sensacionTitular = "Suele tener frío";
            String tipoUsoTitular = "personal";
            List<String> memoriaTitular = new ArrayList<>();
            String ultTitular = null;
            String ultDescTitular = null;

            if (doc != null && doc.exists()) {
                if (doc.getString("nombre") != null && !doc.getString("nombre").trim().isEmpty()) {
                    nombreTitular = doc.getString("nombre").trim();
                } else if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                    nombreTitular = user.getDisplayName().trim();
                }

                Long edadDoc = doc.getLong("edad");
                if (edadDoc != null) edadTitular = edadDoc.intValue();

                String sensDoc = doc.getString("sensacionTermica");
                if (sensDoc != null && !sensDoc.isEmpty()) sensacionTitular = sensDoc;

                String usoDoc = doc.getString("tipoUsoArmario");
                if (usoDoc != null && !usoDoc.isEmpty()) tipoUsoTitular = usoDoc;

                List<String> histDoc = (List<String>) doc.get("historialMemoriaIA");
                if (histDoc != null) memoriaTitular = histDoc;

                ultTitular = doc.getString("ultimoTituloConjunto");
                ultDescTitular = doc.getString("ultimaDescripcionConjunto");
            }

            final Familiar familiarTitular = new Familiar("titular", nombreTitular, "Titular de la cuenta", "#E8CEBF", true, edadTitular, sensacionTitular);
            familiarTitular.setTipoUso(tipoUsoTitular);
            familiarTitular.setHistorialMemoriaIA(memoriaTitular);
            familiarTitular.setUltimoTituloConjunto(ultTitular);
            familiarTitular.setUltimaDescripcionConjunto(ultDescTitular);

            // 2. Escuchar la subcolección de familiares y agregarlos debajo del dueño
            db.collection("usuarios").document(uid).collection("familiares")
                    .addSnapshotListener((snapshots, e) -> {
                        listaFamilia.clear();
                        // El dueño de la cuenta SIEMPRE va primero
                        listaFamilia.add(familiarTitular);

                        if (snapshots != null) {
                            for (QueryDocumentSnapshot d : snapshots) {
                                Familiar f = d.toObject(Familiar.class);
                                f.setId(d.getId());
                                listaFamilia.add(f);
                            }
                        }
                        adaptador.notifyDataSetChanged();
                    });
        });
    }

    private void mostrarDialogoNuevoFamiliar() {
        if (user == null) return;

        final EditText inputNombre = new EditText(getContext());
        inputNombre.setHint("Nombre del familiar (ej. Pablo)");

        new AlertDialog.Builder(getContext())
                .setTitle("Nuevo perfil familiar")
                .setMessage("Indica el nombre para configurar sus preferencias de vestimenta:")
                .setView(inputNombre)
                .setPositiveButton("Siguiente", (dialog, which) -> {
                    String nombre = inputNombre.getText().toString().trim();
                    if (!nombre.isEmpty()) {
                        // Abrir OnboardingActivity con el cuestionario completo para el nuevo miembro
                        Intent i = new Intent(getContext(), OnboardingActivity.class);
                        i.putExtra("esNuevoFamiliar", true);
                        i.putExtra("nombreFamiliar", nombre);
                        startActivity(i);
                    } else {
                        Toast.makeText(getContext(), "Introduce un nombre", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}