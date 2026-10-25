package py.jokerpy.app.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.InputStream;

import py.jokerpy.app.R;
import py.jokerpy.app.data.AppDatabase;
import py.jokerpy.app.data.IconoHelper;
import py.jokerpy.app.data.Sitio;
import py.jokerpy.app.data.SitioDao;
import py.jokerpy.app.util.Hilos;

// Formulario para agregar o editar un sitio.
public class AgregarActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "id";

    private EditText campoNombre;
    private EditText campoUrl;
    private ImageView vistaIcono;

    private long idEdicion = -1;
    private String iconoPropio;
    private String iconoWebActual;
    private boolean refrescarWeb = false;

    private final ActivityResultLauncher<String> selector = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) cargarIconoPropio(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar);

        campoNombre = findViewById(R.id.campoNombre);
        campoUrl = findViewById(R.id.campoUrl);
        vistaIcono = findViewById(R.id.vistaIcono);

        idEdicion = getIntent().getLongExtra(EXTRA_ID, -1);
        if (idEdicion >= 0) {
            ((android.widget.TextView) findViewById(R.id.titulo)).setText(R.string.editar_sitio);
            Hilos.IO.execute(() -> {
                Sitio s = AppDatabase.get(getApplicationContext()).sitioDao().buscar(idEdicion);
                if (s != null) runOnUiThread(() -> rellenar(s));
            });
        }

        findViewById(R.id.btnCambiarIcono).setOnClickListener(v -> selector.launch("image/*"));
        findViewById(R.id.btnIconoPagina).setOnClickListener(v -> {
            iconoPropio = null;
            refrescarWeb = true;
            mostrarIcono(iconoWebActual);
            Toast.makeText(this, "Se usará el icono de la página al guardar", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnCancelar).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());
    }

    // Completa el formulario con un sitio existente.
    private void rellenar(Sitio s) {
        campoNombre.setText(s.nombre);
        campoUrl.setText(s.url);
        iconoPropio = s.iconoPropio;
        iconoWebActual = s.iconoWeb;
        mostrarIcono(iconoPropio != null ? iconoPropio : iconoWebActual);
    }

    // Muestra el icono elegido en la vista previa.
    private void mostrarIcono(String ruta) {
        if (ruta != null && new File(ruta).exists()) {
            vistaIcono.setImageBitmap(BitmapFactory.decodeFile(ruta));
        } else {
            vistaIcono.setImageDrawable(null);
        }
    }

    // Copia la imagen elegida a la carpeta de la app.
    private void cargarIconoPropio(Uri uri) {
        Hilos.IO.execute(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                Bitmap bmp = BitmapFactory.decodeStream(in);
                if (bmp == null) throw new IllegalStateException("sin imagen");
                String ruta = IconoHelper.guardar(getApplicationContext(), bmp, "propio");
                runOnUiThread(() -> {
                    iconoPropio = ruta;
                    mostrarIcono(ruta);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "No se pudo leer la imagen", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Agrega https:// si falta y revisa que sea una direccion valida.
    private String normalizar(String texto) {
        String t = texto.trim();
        if (t.isEmpty()) return null;
        if (!t.matches("(?i)^https?://.*")) t = "https://" + t;
        return Patterns.WEB_URL.matcher(t).matches() ? t : null;
    }

    // Guarda el sitio y baja su icono si hace falta.
    private void guardar() {
        final String nombre = campoNombre.getText().toString().trim();
        final String url = normalizar(campoUrl.getText().toString());
        if (nombre.isEmpty()) {
            campoNombre.setError("Escribe un nombre");
            return;
        }
        if (url == null) {
            campoUrl.setError("La dirección no es válida");
            return;
        }
        final String propio = iconoPropio;
        final boolean forzarWeb = refrescarWeb;
        final long id = idEdicion;
        final android.content.Context app = getApplicationContext();

        Hilos.IO.execute(() -> {
            SitioDao dao = AppDatabase.get(app).sitioDao();
            Sitio existente = id >= 0 ? dao.buscar(id) : null;
            Sitio s = existente != null ? existente : new Sitio();

            boolean urlCambio = s.url == null || !s.url.equals(url);
            if (s.iconoPropio != null && !s.iconoPropio.equals(propio)) {
                IconoHelper.borrar(s.iconoPropio);
            }
            s.nombre = nombre;
            s.url = url;
            s.iconoPropio = propio;

            if (existente == null) {
                s.id = dao.insertar(s);
            } else {
                dao.actualizar(s);
            }
            if (propio == null && (urlCambio || forzarWeb || s.iconoWeb == null)) {
                IconoHelper.refrescarIconoWeb(app, s.id);
            }
        });
        finish();
    }
}
