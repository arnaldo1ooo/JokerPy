package py.jokerpy.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import py.jokerpy.app.R;
import py.jokerpy.app.data.Sitio;

// Dibuja la grilla de sitios del menu principal.
public class SitioAdapter extends RecyclerView.Adapter<SitioAdapter.Fila> {

    public interface Listener {
        void abrir(Sitio sitio);

        void opciones(Sitio sitio);
    }

    private final Listener listener;
    private final List<Sitio> sitios = new ArrayList<>();

    public SitioAdapter(Listener listener) {
        this.listener = listener;
    }

    // Reemplaza la lista que se muestra.
    public void setDatos(List<Sitio> nuevos) {
        sitios.clear();
        sitios.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View v = LayoutInflater.from(padre.getContext()).inflate(R.layout.item_sitio, padre, false);
        return new Fila(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        Sitio s = sitios.get(posicion);
        fila.nombre.setText(s.nombre);
        fila.estrella.setVisibility(s.favorito ? View.VISIBLE : View.GONE);

        String ruta = s.iconoPropio != null ? s.iconoPropio : s.iconoWeb;
        if (ruta != null && new File(ruta).exists()) {
            fila.letra.setVisibility(View.GONE);
            fila.icono.setVisibility(View.VISIBLE);
            Glide.with(fila.icono).load(new File(ruta)).into(fila.icono);
        } else {
            fila.icono.setVisibility(View.GONE);
            fila.letra.setVisibility(View.VISIBLE);
            String n = s.nombre == null ? "" : s.nombre.trim();
            fila.letra.setText(n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase());
        }

        fila.itemView.setOnClickListener(v -> {
            int p = fila.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) listener.abrir(sitios.get(p));
        });
        fila.itemView.setOnLongClickListener(v -> {
            int p = fila.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) listener.opciones(sitios.get(p));
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return sitios.size();
    }

    static class Fila extends RecyclerView.ViewHolder {
        final ImageView icono;
        final TextView letra;
        final TextView nombre;
        final TextView estrella;

        Fila(View v) {
            super(v);
            icono = v.findViewById(R.id.icono);
            letra = v.findViewById(R.id.letra);
            nombre = v.findViewById(R.id.nombre);
            estrella = v.findViewById(R.id.estrella);
        }
    }
}
