package com.example.aeuolusfit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FamiliaFragment extends Fragment {

    private RecyclerView rvFamilia;
    private AdaptadorFamiliaLista adaptadorFamilia;
    private final List<Familiar> listaFamiliares = new ArrayList<>();
    private final Map<String, Double> mapaAjustesPerfiles = new HashMap<>();

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_familia, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        rvFamilia = v.findViewById(R.id.rvListaFamilia);
        if (rvFamilia != null) {
            rvFamilia.setHasFixedSize(true);
            rvFamilia.setLayoutManager(new LinearLayoutManager(getContext()));

            adaptadorFamilia = new AdaptadorFamiliaLista(listaFamiliares);

            // 🔴 Aquí capturamos el clic y lanzamos EditarPerfilActivity con sus datos
            adaptadorFamilia.setOnItemClickListener(familiar -> {
                Intent intent = new Intent(getContext(), EditarPerfilActivity.class);
                intent.putExtra("id", familiar.getId()); // Ej: "perfil_1"
                intent.putExtra("nombre", familiar.getNombre()); // Ej: "Lucía (Friolera)"

                double ajusteVal = mapaAjustesPerfiles.containsKey(familiar.getId()) ? mapaAjustesPerfiles.get(familiar.getId()) : 0.0;
                intent.putExtra("ajuste", ajusteVal); // Pasa el valor numérico (-5, +5, etc.)

                startActivity(intent);
            });

            rvFamilia.setAdapter(adaptadorFamilia);
        }

        cargarPerfilesEnFamilia();

        return v;
    }

    private void cargarPerfilesEnFamilia() {
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        db.collection("usuarios").document(uid)
                .collection("configuracion").document("perfiles")
                .addSnapshotListener((docPerfiles, error) -> {
                    if (docPerfiles == null || !docPerfiles.exists()) return;

                    listaFamiliares.clear();
                    mapaAjustesPerfiles.clear();

                    for (int i = 1; i <= 4; i++) {
                        String key = "perfil_" + i;
                        if (docPerfiles.contains(key)) {
                            Map<String, Object> data = (Map<String, Object>) docPerfiles.get(key);
                            if (data != null) {
                                String nombre = (String) data.get("nombre");
                                double ajuste = data.containsKey("ajuste") ? ((Number) data.get("ajuste")).doubleValue() : 0.0;

                                mapaAjustesPerfiles.put(key, ajuste);

                                Familiar f = new Familiar();
                                f.setId(key);
                                f.setNombre(nombre != null ? nombre : "Perfil " + i);
                                f.setSensacionTermica("Ajuste térmico: " + ajuste + "°C");
                                f.setColorHex("#4F6342");

                                listaFamiliares.add(f);
                            }
                        }
                    }

                    if (adaptadorFamilia != null) {
                        adaptadorFamilia.notifyDataSetChanged();
                    }
                });
    }
}