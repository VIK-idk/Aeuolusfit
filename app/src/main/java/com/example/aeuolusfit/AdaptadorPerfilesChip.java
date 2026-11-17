package com.example.aeuolusfit;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AdaptadorPerfilesChip extends RecyclerView.Adapter<AdaptadorPerfilesChip.ViewHolder> {

    private final List<Familiar> listaPerfiles;
    protected View.OnClickListener onClickListener;

    public AdaptadorPerfilesChip(List<Familiar> listaPerfiles) {
        this.listaPerfiles = listaPerfiles;
    }

    public void setOnItemClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public MaterialCardView cardChip, cardAvatar;
        public TextView tvInicialChip, tvNombreChip;

        public ViewHolder(View itemView) {
            super(itemView);
            cardChip = itemView.findViewById(R.id.cardChip);
            cardAvatar = itemView.findViewById(R.id.cardAvatar);
            tvInicialChip = itemView.findViewById(R.id.tvInicialChip);
            tvNombreChip = itemView.findViewById(R.id.tvNombreChip);
        }

        public void personaliza(Familiar familiar) {
            tvInicialChip.setText(familiar.getInicial());
            try {
                cardAvatar.setCardBackgroundColor(Color.parseColor(familiar.getColorHex()));
            } catch (Exception e) {
                cardAvatar.setCardBackgroundColor(Color.parseColor("#E8CEBF"));
            }

            if (familiar.isSeleccionado()) {
                cardChip.setCardBackgroundColor(Color.parseColor("#4F6342"));
                cardChip.setStrokeWidth(0);
                tvNombreChip.setText(familiar.getNombre() + "  ✓");
                tvNombreChip.setTextColor(Color.WHITE);
                tvNombreChip.setTypeface(null, Typeface.BOLD);
            } else {
                cardChip.setCardBackgroundColor(Color.WHITE);
                cardChip.setStrokeWidth(3);
                cardChip.setStrokeColor(Color.parseColor("#D4D6CF"));
                tvNombreChip.setText(familiar.getNombre());
                tvNombreChip.setTextColor(Color.parseColor("#686D65"));
                tvNombreChip.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_perfil_chip, parent, false);
        view.setOnClickListener(onClickListener);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.personaliza(listaPerfiles.get(position));
    }

    @Override
    public int getItemCount() {
        return listaPerfiles.size();
    }
}