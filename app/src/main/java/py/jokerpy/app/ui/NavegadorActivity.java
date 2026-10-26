package py.jokerpy.app.ui;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.ByteArrayInputStream;

import py.jokerpy.app.R;
import py.jokerpy.app.util.Ajustes;
import py.jokerpy.app.util.BloqueadorAnuncios;
import py.jokerpy.app.util.Bordes;

// Navegador interno: barra superior, anuncios bloqueados, video a pantalla completa,
// imagen en imagen, modo escritorio y cursor para el control remoto.
public class NavegadorActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_ESCRITORIO = "escritorio";

    private static final String AGENTE_ESCRITORIO =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private FrameLayout contenedor;
    private View panel;
    private View barra;
    private FrameLayout areaWeb;
    private TextView titulo;
    private WebView web;
    private ProgressBar progreso;
    private View vistaCompleta;
    private WebChromeClient.CustomViewCallback avisoCompleta;

    private CursorView cursor;
    private View btnCursor;
    private boolean esTv;
    private boolean cursorActivo;
    private final Handler manejador = new Handler(Looper.getMainLooper());

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navegador);
        getWindow().getDecorView().setBackgroundColor(android.graphics.Color.BLACK);
        Bordes.aplicar(this, findViewById(android.R.id.content), false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null) {
            finish();
            return;
        }

        BloqueadorAnuncios.cargar(this);
        final boolean bloquear = Ajustes.bloquearAnuncios(this);

        contenedor = findViewById(R.id.contenedor);
        panel = findViewById(R.id.panel);
        barra = findViewById(R.id.barra);
        areaWeb = findViewById(R.id.areaWeb);
        titulo = findViewById(R.id.titulo);
        web = findViewById(R.id.web);
        progreso = findViewById(R.id.progreso);
        btnCursor = findViewById(R.id.btnCursor);

        titulo.setText(nombreCorto(url));
        findViewById(R.id.btnAtras).setOnClickListener(v -> retroceder());
        findViewById(R.id.btnRecargar).setOnClickListener(v -> web.reload());
        findViewById(R.id.btnCerrar).setOnClickListener(v -> finish());

        WebSettings ajustes = web.getSettings();
        ajustes.setJavaScriptEnabled(true);
        ajustes.setDomStorageEnabled(true);
        ajustes.setMediaPlaybackRequiresUserGesture(false);
        ajustes.setSupportMultipleWindows(false);
        ajustes.setJavaScriptCanOpenWindowsAutomatically(false);
        if (getIntent().getBooleanExtra(EXTRA_ESCRITORIO, false)) {
            ajustes.setUserAgentString(AGENTE_ESCRITORIO);
            ajustes.setUseWideViewPort(true);
            ajustes.setLoadWithOverviewMode(true);
        }

        web.setWebViewClient(new WebViewClient() {
            // Muestra la direccion de la pagina que se esta cargando.
            @Override
            public void onPageStarted(WebView v, String direccion, Bitmap icono) {
                titulo.setText(nombreCorto(direccion));
            }

            // Corta los pedidos a dominios de publicidad.
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                if (bloquear && BloqueadorAnuncios.esAnuncio(r.getUrl().getHost())) {
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(v, r);
            }

            // Evita saltos a otras apps o a sitios de publicidad.
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if (bloquear && BloqueadorAnuncios.esAnuncio(r.getUrl().getHost())) return true;
                String esquema = r.getUrl().getScheme();
                return !("http".equals(esquema) || "https".equals(esquema));
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int avance) {
                progreso.setProgress(avance);
                progreso.setVisibility(avance < 100 ? View.VISIBLE : View.GONE);
            }

            // Cambia el titulo de la barra por el de la pagina.
            @Override
            public void onReceivedTitle(WebView v, String texto) {
                if (texto != null && !texto.trim().isEmpty()) titulo.setText(texto);
            }

            // Video a pantalla completa.
            @Override
            public void onShowCustomView(View vista, CustomViewCallback aviso) {
                if (vistaCompleta != null) {
                    aviso.onCustomViewHidden();
                    return;
                }
                vistaCompleta = vista;
                avisoCompleta = aviso;
                contenedor.addView(vista, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                panel.setVisibility(View.GONE);
                barrasDelSistema(false);
            }

            @Override
            public void onHideCustomView() {
                salirPantallaCompleta();
            }

            // No deja que la pagina abra ventanas emergentes.
            @Override
            public boolean onCreateWindow(WebView v, boolean dialogo, boolean gesto, android.os.Message msg) {
                return false;
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                retroceder();
            }
        });

        prepararImagenEnImagen();
        prepararCursor();

        web.loadUrl(url);
        web.requestFocus();
    }

    // ---------- imagen en imagen ----------

    // Dice si el dispositivo permite imagen en imagen.
    private boolean pipDisponible() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    // Muestra el boton de imagen en imagen si esta permitido en ajustes.
    private void prepararImagenEnImagen() {
        View boton = findViewById(R.id.btnPip);
        if (pipDisponible() && Ajustes.pipActivado(this)) {
            boton.setVisibility(View.VISIBLE);
            boton.setOnClickListener(v -> entrarPip());
        }
    }

    // Pasa a la ventana pequena.
    private void entrarPip() {
        if (!pipDisponible()) return;
        try {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9))
                    .build();
            enterPictureInPictureMode(params);
        } catch (IllegalStateException ignorado) {
            Toast.makeText(this, "No se pudo abrir la ventana pequeña", Toast.LENGTH_SHORT).show();
        }
    }

    // Hay video en pantalla completa o algo sonando.
    private boolean hayVideo() {
        if (vistaCompleta != null) return true;
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audio != null && audio.isMusicActive();
    }

    // Al salir de la app con un video en marcha, sigue en ventana pequena.
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Ajustes.pipActivado(this) && pipDisponible() && hayVideo()) entrarPip();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean enPip, Configuration config) {
        super.onPictureInPictureModeChanged(enPip, config);
        if (barra != null) barra.setVisibility(enPip ? View.GONE : View.VISIBLE);
        if (cursor != null) cursor.setVisibility(enPip ? View.GONE : (cursorActivo ? View.VISIBLE : View.GONE));
    }

    private boolean enPip() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode();
    }

    // ---------- cursor para control remoto ----------

    // Prepara el cursor si el dispositivo es una TV (o no tiene pantalla tactil).
    private void prepararCursor() {
        UiModeManager modo = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        esTv = (modo != null && modo.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION)
                || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
        if (!esTv) return;

        cursor = new CursorView(this);
        areaWeb.addView(cursor, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        btnCursor.setVisibility(View.VISIBLE);
        btnCursor.setOnClickListener(v -> alternarCursor());
        cursorActivo = Ajustes.cursorTv(this);
        aplicarCursor();
    }

    // Activa o desactiva el cursor.
    private void alternarCursor() {
        cursorActivo = !cursorActivo;
        aplicarCursor();
        if (cursorActivo) {
            web.requestFocus();
            Toast.makeText(this, "Cursor activado. Flechas para mover, OK para tocar.", Toast.LENGTH_SHORT).show();
        }
    }

    private void aplicarCursor() {
        if (cursor != null) cursor.setVisibility(cursorActivo ? View.VISIBLE : View.GONE);
        btnCursor.setSelected(cursorActivo);
    }

    // Maneja las flechas y OK del control remoto cuando el cursor esta activo.
    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        if (cursor != null && cursorActivo && vistaCompleta == null && !enPip()) {
            int tecla = ev.getKeyCode();
            boolean flecha = tecla == KeyEvent.KEYCODE_DPAD_UP || tecla == KeyEvent.KEYCODE_DPAD_DOWN
                    || tecla == KeyEvent.KEYCODE_DPAD_LEFT || tecla == KeyEvent.KEYCODE_DPAD_RIGHT;
            if (flecha) {
                if (ev.getAction() == KeyEvent.ACTION_DOWN) moverCursor(tecla, ev.getRepeatCount());
                return true;
            }
            if (tecla == KeyEvent.KEYCODE_DPAD_CENTER || tecla == KeyEvent.KEYCODE_ENTER
                    || tecla == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (ev.getAction() == KeyEvent.ACTION_UP) tocarConCursor();
                return true;
            }
        }
        if (cursor != null && ev.getKeyCode() == KeyEvent.KEYCODE_MENU
                && ev.getAction() == KeyEvent.ACTION_DOWN && ev.getRepeatCount() == 0) {
            alternarCursor();
            return true;
        }
        return super.dispatchKeyEvent(ev);
    }

    // Mueve el cursor; en los bordes baja o sube la pagina.
    private void moverCursor(int tecla, int repeticiones) {
        float d = getResources().getDisplayMetrics().density;
        float paso = 10 * d * (1 + Math.min(repeticiones, 12) / 3f);
        float borde = 4 * d;

        if (tecla == KeyEvent.KEYCODE_DPAD_UP && cursor.getPosY() <= borde) {
            if (web.getScrollY() > 0) {
                web.scrollBy(0, -(int) (paso * 2));
            } else {
                // Arriba del todo: pasa el foco a la barra superior.
                cursorActivo = false;
                aplicarCursor();
                btnCursor.requestFocus();
            }
            return;
        }
        if (tecla == KeyEvent.KEYCODE_DPAD_DOWN && cursor.getPosY() >= cursor.getHeight() - borde) {
            web.scrollBy(0, (int) (paso * 2));
            return;
        }
        float dx = 0;
        float dy = 0;
        if (tecla == KeyEvent.KEYCODE_DPAD_UP) dy = -paso;
        else if (tecla == KeyEvent.KEYCODE_DPAD_DOWN) dy = paso;
        else if (tecla == KeyEvent.KEYCODE_DPAD_LEFT) dx = -paso;
        else dx = paso;
        cursor.mover(dx, dy);
    }

    // Hace un toque en el lugar donde esta el cursor.
    private void tocarConCursor() {
        final float x = cursor.getPosX();
        final float y = cursor.getPosY();
        long t = SystemClock.uptimeMillis();
        MotionEvent abajo = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0);
        web.dispatchTouchEvent(abajo);
        abajo.recycle();
        manejador.postDelayed(() -> {
            long t2 = SystemClock.uptimeMillis();
            MotionEvent arriba = MotionEvent.obtain(t2, t2, MotionEvent.ACTION_UP, x, y, 0);
            web.dispatchTouchEvent(arriba);
            arriba.recycle();
        }, 60);
    }

    // ---------- navegacion ----------

    // Vuelve a la pagina anterior, o cierra el navegador si no hay mas.
    private void retroceder() {
        if (vistaCompleta != null) {
            salirPantallaCompleta();
        } else if (web.canGoBack()) {
            web.goBack();
        } else {
            finish();
        }
    }

    // Saca solo el nombre del sitio de una direccion.
    private String nombreCorto(String direccion) {
        String host = Uri.parse(direccion).getHost();
        return host == null ? direccion : host;
    }

    // Vuelve del video a pantalla completa a la pagina.
    private void salirPantallaCompleta() {
        if (vistaCompleta == null) return;
        contenedor.removeView(vistaCompleta);
        vistaCompleta = null;
        if (avisoCompleta != null) avisoCompleta.onCustomViewHidden();
        avisoCompleta = null;
        panel.setVisibility(View.VISIBLE);
        barrasDelSistema(true);
    }

    // Muestra u oculta las barras del sistema.
    private void barrasDelSistema(boolean mostrar) {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (mostrar) c.show(WindowInsetsCompat.Type.systemBars());
        else c.hide(WindowInsetsCompat.Type.systemBars());
    }

    // La pagina se pausa solo si la app deja de verse (no en la ventana pequena).
    @Override
    protected void onStop() {
        super.onStop();
        if (web != null && !enPip()) web.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (web != null) web.onResume();
    }

    @Override
    protected void onDestroy() {
        manejador.removeCallbacksAndMessages(null);
        if (web != null) {
            ((ViewGroup) web.getParent()).removeView(web);
            web.destroy();
        }
        super.onDestroy();
    }
}
