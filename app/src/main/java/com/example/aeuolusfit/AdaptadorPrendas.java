package com.example.aeuolusfit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdaptadorPrendas extends RecyclerView.Adapter<AdaptadorPrendas.ViewHolder> {

    private final List<Prenda> listaPrendas;
    protected View.OnClickListener onClickListener;

    public AdaptadorPrendas(List<Prenda> listaPrendas) {
        this.listaPrendas = listaPrendas;
    }

    public void setOnItemClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nombre, categoria;
        public ImageView foto;

        public ViewHolder(View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.tvNombrePrenda);
            categoria = itemView.findViewById(R.id.tvCategoriaPrenda);
            foto = itemView.findViewById(R.id.ivFotoPrenda);
        }

        public void personaliza(Prenda prenda) {
            nombre.setText(prenda.getNombre());
            categoria.setText(prenda.getCategoria() + " · " + prenda.getClimaIdeal());
            foto.setImageResource(R.drawable.armario);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.elemento_prenda, parent, false);
        view.setOnClickListener(onClickListener);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.personaliza(listaPrendas.get(position));
    }

    @Override
    public int getItemCount() {
        return listaPrendas.size();
    }
}