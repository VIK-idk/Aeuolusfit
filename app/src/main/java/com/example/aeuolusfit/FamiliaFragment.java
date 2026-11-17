package com.example.aeuolusfit;

import android.app.AlertDialog;
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
    private AdaptadorPerfilesChip adaptador;
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
        adaptador = new AdaptadorPerfilesChip(listaFamilia);
        rvListaFamilia.setAdapter(adaptador);

        cargarFamiliares();

        MaterialButton btnAnadir = v.findViewById(R.id.btnAnadirFamiliar);
        btnAnadir.setOnClickListener(view -> mostrarDialogoNuevoFamiliar());

        return v;
    }

    private void cargarFamiliares() {
        if (user == null) return;
        db.collection("usuarios").document(user.getUid()).collection("familiares")
                .addSnapshotListener((snapshots, e) -> {
                    listaFamilia.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            listaFamilia.add(doc.toObject(Familiar.class));
                        }
                    }
                    if (listaFamilia.isEmpty()) {
                        listaFamilia.add(new Familiar("1", "Pablo (Demo)", "Pareja", "#D9E2D5", false));
                        listaFamilia.add(new Familiar("2", "Emma (Demo)", "Hija", "#EDE3CE", false));
                    }
                    adaptador.notifyDataSetChanged();
                });
    }

    private void mostrarDialogoNuevoFamiliar() {
        if (user == null) return;
        final EditText input = new EditText(getContext());
        input.setHint("Nombre del familiar (ej. Carlos)");

        new AlertDialog.Builder(getContext())
                .setTitle("Nuevo miembro")
                .setMessage("Se sincronizará automáticamente con la pestaña Hoy:")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nombre = input.getText().toString().trim();
                    if (!nombre.isEmpty()) {
                        String id = String.valueOf(System.currentTimeMillis());
                        Familiar nuevo = new Familiar(id, nombre, "Familiar", "#D9E2D5", false);
                        db.collection("usuarios").document(user.getUid())
                                .collection("familiares").document(id).set(nuevo);
                        Toast.makeText(getContext(), "Familiar añadido a Firestore", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}