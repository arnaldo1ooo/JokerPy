package py.jokerpy.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import py.jokerpy.app.R;
import py.jokerpy.app.data.AppDatabase;
import py.jokerpy.app.data.IconoHelper;
import py.jokerpy.app.data.Precarga;
import py.jokerpy.app.data.Sitio;
import py.jokerpy.app.data.SitioDao;
import py.jokerpy.app.util.Ajustes;
import py.jokerpy.app.util.Hilos;

// Pantalla principal: menu con todos los sitios.
public class MainActivity extends AppCompatActivity implements SitioAdapter.Listener {

    private SitioAdapter adaptador;
    private List<Sitio> todos = new ArrayList<>();
    private EditText buscar;
    private TextView vacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Precarga.ejecutarSiHaceFalta(this);

        buscar = findViewById(R.id.buscar);
        vacio = findViewById(R.id.vacio);

        // Cuantas columnas entran segun el ancho de la pantalla.
        int columnas = Math.max(2, getResources().getConfiguration().screenWidthDp / 150);
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new GridLayoutManager(this, columnas));
        adaptador = new SitioAdapter(this);
        lista.setAdapter(adaptador);

        AppDatabase.get(this).sitioDao().todos().observe(this, sitios -> {
            todos = sitios;
            filtrar();
        });

        buscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filtrar();
            }
        });

        findViewById(R.id.btnAgregar).setOnClickListener(v ->
                startActivity(new Intent(this, AgregarActivity.class)));
        findViewById(R.id.btnAjustes).setOnClickListener(v ->
                startActivity(new Intent(this, AjustesActivity.class)));
    }

    // Muestra solo los sitios que coinciden con la busqueda.
    private void filtrar() {
        String q = buscar.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Sitio> visibles = new ArrayList<>();
        for (Sitio s : todos) {
            boolean coincide = q.isEmpty()
                    || (s.nombre != null && s.nombre.toLowerCase(Locale.ROOT).contains(q))
                    || (s.url != null && s.url.toLowerCase(Locale.ROOT).contains(q));
            if (coincide) visibles.add(s);
        }
        adaptador.setDatos(visibles);
        vacio.setVisibility(todos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Abre el sitio en el navegador interno o en el externo.
    @Override
    public void abrir(Sitio sitio) {
        if (Ajustes.abrirExterno(this)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sitio.url)));
                return;
            } catch (ActivityNotFoundException ignorado) {
                // si no hay navegador externo se usa el interno
            }
        }
        Intent i = new Intent(this, NavegadorActivity.class);
        i.putExtra(NavegadorActivity.EXTRA_URL, sitio.url);
        startActivity(i);
    }

    // Menu con las acciones de un sitio (pulsacion larga).
    @Override
    public void opciones(Sitio sitio) {
        String[] acciones = {
                "Editar",
                sitio.favorito ? "Quitar de favoritos" : "Marcar como favorito",
                "Actualizar icono de la página",
                "Eliminar"
        };
        new AlertDialog.Builder(this)
                .setTitle(sitio.nombre)
                .setItems(acciones, (d, cual) -> {
                    switch (cual) {
                        case 0:
                            Intent i = new Intent(this, AgregarActivity.class);
                            i.putExtra(AgregarActivity.EXTRA_ID, sitio.id);
                            startActivity(i);
                            break;
                        case 1:
                            cambiarFavorito(sitio);
                            break;
                        case 2:
                            Hilos.IO.execute(() -> IconoHelper.refrescarIconoWeb(getApplicationContext(), sitio.id));
                            break;
                        default:
                            confirmarEliminar(sitio);
                    }
                })
                .show();
    }

    // Marca o desmarca un sitio como favorito.
    private void cambiarFavorito(Sitio sitio) {
        Hilos.IO.execute(() -> {
            SitioDao dao = AppDatabase.get(getApplicationContext()).sitioDao();
            Sitio actual = dao.buscar(sitio.id);
            if (actual == null) return;
            actual.favorito = !actual.favorito;
            dao.actualizar(actual);
        });
    }

    // Pregunta antes de borrar un sitio.
    private void confirmarEliminar(Sitio sitio) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar " + sitio.nombre)
                .setMessage("Se quitará de tu lista.")
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton("Eliminar", (d, w) -> Hilos.IO.execute(() -> {
                    SitioDao dao = AppDatabase.get(getApplicationContext()).sitioDao();
                    Sitio actual = dao.buscar(sitio.id);
                    if (actual == null) return;
                    IconoHelper.borrar(actual.iconoPropio);
                    IconoHelper.borrar(actual.iconoWeb);
                    dao.eliminar(actual);
                }))
                .show();
    }
}
