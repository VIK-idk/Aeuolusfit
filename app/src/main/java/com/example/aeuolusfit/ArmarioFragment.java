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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ArmarioFragment extends Fragment {

    private RecyclerView rvPrendas;
    private AdaptadorPrendas adaptador;
    private final List<Prenda> listaPrendas = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_armario, container, false);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        rvPrendas = v.findViewById(R.id.rvPrendasArmario);
        rvPrendas.setHasFixedSize(true);
        // Usamos GridLayoutManager de 2 columnas (Unidad 3 - 3.7)
        rvPrendas.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adaptador = new AdaptadorPrendas(listaPrendas);

        // Escuchador de clic para borrar prenda con confirmación (Unidad 3 - 3.8.1)
        adaptador.setOnItemClickListener(view -> {
            int pos = rvPrendas.getChildAdapterPosition(view);
            if (pos != RecyclerView.NO_POSITION) {
                Prenda p = listaPrendas.get(pos);
                confirmarBorradoPrenda(p);
            }
        });

        rvPrendas.setAdapter(adaptador);
        cargarPrendasFirestore();

        MaterialButton btnAnadir = v.findViewById(R.id.btnAnadirPrenda);
        btnAnadir.setOnClickListener(view -> mostrarDialogoNuevaPrenda());

        return v;
    }

    private void cargarPrendasFirestore() {
        if (user == null) return;
        db.collection("prendas")
                .whereEqualTo("uidUsuario", user.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    listaPrendas.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            listaPrendas.add(doc.toObject(Prenda.class));
                        }
                    }
                    if (listaPrendas.isEmpty()) {
                        listaPrendas.add(new Prenda("demo1", "Chaqueta Ligera", "Abrigo", "Fresco", user.getUid()));
                        listaPrendas.add(new Prenda("demo2", "Camiseta Básica", "Superior", "Templado", user.getUid()));
                        listaPrendas.add(new Prenda("demo3", "Pantalón Chino", "Inferior", "Todo el año", user.getUid()));
                        listaPrendas.add(new Prenda("demo4", "Chubasquero", "Abrigo", "Lluvia", user.getUid()));
                    }
                    adaptador.notifyDataSetChanged();
                });
    }

    private void mostrarDialogoNuevaPrenda() {
        if (user == null) return;
        final EditText input = new EditText(getContext());
        input.setHint("Nombre de la prenda (ej. Sudadera Verde)");

        new AlertDialog.Builder(getContext())
                .setTitle("Añadir Prenda")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nombre = input.getText().toString().trim();
                    if (!nombre.isEmpty()) {
                        String id = String.valueOf(System.currentTimeMillis());
                        Prenda nueva = new Prenda(id, nombre, "Prenda IoT", "18 °C", user.getUid());
                        db.collection("prendas").document(id).set(nueva);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarBorradoPrenda(Prenda prenda) {
        if (prenda.getId().startsWith("demo")) {
            Toast.makeText(getContext(), "Añade una prenda real para probar el borrado en Firestore", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle("Borrar prenda")
                .setMessage("¿Estás seguro de que quieres eliminar '" + prenda.getNombre() + "' del armario?")
                .setPositiveButton("Sí", (dialog, which) -> db.collection("prendas").document(prenda.getId()).delete())
                .setNegativeButton("No", null)
                .show();
    }
}