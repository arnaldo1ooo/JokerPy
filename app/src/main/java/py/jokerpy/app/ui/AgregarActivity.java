package py.jokerpy.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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
    private static final long ESPERA_MS = 900;

    private EditText campoNombre;
    private EditText campoUrl;
    private ImageView vistaIcono;
    private TextView estado;
    private CompoundButton switchEscritorio;

    private long idEdicion = -1;
    private String iconoPropio;
    private String iconoWebActual;
    private boolean refrescarWeb = false;

    // Lo que se averiguo de la pagina mientras se escribia la direccion.
    private Bitmap iconoSugerido;
    private String urlSugerida;
    private String ultimaConsulta;
    private int solicitud = 0;
    private boolean nombreEditado = false;
    private boolean poniendoNombre = false;

    private final Handler manejador = new Handler(Looper.getMainLooper());
    private final Runnable consultar = this::consultarPagina;

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
        estado = findViewById(R.id.estado);
        switchEscritorio = findViewById(R.id.switchEscritorio);
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());

        // Si el usuario escribe el nombre a mano, ya no se lo cambiamos.
        campoNombre.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!poniendoNombre) nombreEditado = true;
            }
        });

        // Al escribir o pegar la direccion, espera un momento y busca la pagina.
        campoUrl.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                manejador.removeCallbacks(consultar);
                manejador.postDelayed(consultar, ESPERA_MS);
            }
        });
        campoUrl.setOnFocusChangeListener((v, tieneFoco) -> {
            if (!tieneFoco) {
                manejador.removeCallbacks(consultar);
                consultarPagina();
            }
        });
        campoUrl.setOnEditorActionListener((v, accion, evento) -> {
            manejador.removeCallbacks(consultar);
            consultarPagina();
            return false;
        });

        idEdicion = getIntent().getLongExtra(EXTRA_ID, -1);
        if (idEdicion >= 0) {
            ((TextView) findViewById(R.id.titulo)).setText(R.string.editar_sitio);
            Hilos.IO.execute(() -> {
                Sitio s = AppDatabase.get(getApplicationContext()).sitioDao().buscar(idEdicion);
                if (s != null) runOnUiThread(() -> rellenar(s));
            });
        }

        findViewById(R.id.btnCambiarIcono).setOnClickListener(v -> selector.launch("image/*"));
        findViewById(R.id.btnIconoPagina).setOnClickListener(v -> {
            iconoPropio = null;
            refrescarWeb = true;
            if (iconoSugerido != null) vistaIcono.setImageBitmap(iconoSugerido);
            else mostrarIcono(iconoWebActual);
            Toast.makeText(this, "Se usará el icono de la página al guardar", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnCancelar).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());
    }

    // Busca el titulo y el icono de la pagina para sugerirlos.
    private void consultarPagina() {
        final String url = normalizar(campoUrl.getText().toString());
        if (url == null || url.equals(ultimaConsulta)) return;
        ultimaConsulta = url;
        final int mia = ++solicitud;
        estado.setVisibility(View.VISIBLE);
        Hilos.IO.execute(() -> {
            IconoHelper.Pagina pagina = IconoHelper.leerPagina(url);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || mia != solicitud) return;
                estado.setVisibility(View.GONE);
                aplicarSugerencia(url, pagina);
            });
        });
    }

    // Pone el titulo y el icono encontrados, sin pisar lo que eligio el usuario.
    private void aplicarSugerencia(String url, IconoHelper.Pagina pagina) {
        urlSugerida = url;
        iconoSugerido = pagina.icono;

        boolean nombreVacio = campoNombre.getText().toString().trim().isEmpty();
        if (pagina.titulo != null && (!nombreEditado || nombreVacio)) {
            poniendoNombre = true;
            campoNombre.setText(pagina.titulo);
            campoNombre.setSelection(campoNombre.getText().length());
            poniendoNombre = false;
            nombreEditado = false;
        }
        if (iconoPropio == null && iconoSugerido != null) {
            vistaIcono.setImageBitmap(iconoSugerido);
        }
    }

    // Completa el formulario con un sitio existente.
    private void rellenar(Sitio s) {
        ultimaConsulta = s.url;
        campoNombre.setText(s.nombre);
        nombreEditado = true;
        campoUrl.setText(s.url);
        switchEscritorio.setChecked(s.escritorio);
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

    // Guarda el sitio y deja listo su icono (el elegido o el de la pagina).
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
        manejador.removeCallbacks(consultar);

        final String propio = iconoPropio;
        final boolean forzarWeb = refrescarWeb;
        final long id = idEdicion;
        final boolean escritorio = switchEscritorio.isChecked();
        final Context app = getApplicationContext();
        // Icono que ya se bajo mientras se escribia la direccion (si coincide).
        final Bitmap sugerido = (propio == null && url.equals(urlSugerida)) ? iconoSugerido : null;

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
            s.escritorio = escritorio;

            if (existente == null) {
                s.orden = dao.maxOrden() + 1;
                s.id = dao.insertar(s);
            } else {
                dao.actualizar(s);
            }

            if (propio == null) {
                String ruta = sugerido != null ? IconoHelper.guardar(app, sugerido, "web_" + s.id) : null;
                if (ruta != null) {
                    IconoHelper.borrar(s.iconoWeb);
                    s.iconoWeb = ruta;
                    dao.actualizar(s);
                } else if (urlCambio || forzarWeb || s.iconoWeb == null) {
                    IconoHelper.refrescarIconoWeb(app, s.id);
                }
            }
        });
        finish();
    }

    @Override
    protected void onDestroy() {
        manejador.removeCallbacks(consultar);
        super.onDestroy();
    }

    // Base para observar texto sin escribir los metodos que no se usan.
    private abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void onTextChanged(CharSequence s, int a, int b, int c) {
        }
    }
}
