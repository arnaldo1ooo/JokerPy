package py.jokerpy.app.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
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

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.ByteArrayInputStream;

import py.jokerpy.app.R;
import py.jokerpy.app.util.Ajustes;
import py.jokerpy.app.util.BloqueadorAnuncios;

// Navegador interno con bloqueo de anuncios y video a pantalla completa.
public class NavegadorActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "url";

    private FrameLayout contenedor;
    private WebView web;
    private ProgressBar progreso;
    private View vistaCompleta;
    private WebChromeClient.CustomViewCallback avisoCompleta;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navegador);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null) {
            finish();
            return;
        }

        BloqueadorAnuncios.cargar(this);
        final boolean bloquear = Ajustes.bloquearAnuncios(this);

        contenedor = findViewById(R.id.contenedor);
        web = findViewById(R.id.web);
        progreso = findViewById(R.id.progreso);

        WebSettings ajustes = web.getSettings();
        ajustes.setJavaScriptEnabled(true);
        ajustes.setDomStorageEnabled(true);
        ajustes.setMediaPlaybackRequiresUserGesture(false);
        ajustes.setSupportMultipleWindows(false);
        ajustes.setJavaScriptCanOpenWindowsAutomatically(false);

        web.setWebViewClient(new WebViewClient() {
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
                web.setVisibility(View.GONE);
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
                if (vistaCompleta != null) {
                    salirPantallaCompleta();
                } else if (web.canGoBack()) {
                    web.goBack();
                } else {
                    finish();
                }
            }
        });

        web.loadUrl(url);
    }

    // Vuelve del video a pantalla completa a la pagina.
    private void salirPantallaCompleta() {
        if (vistaCompleta == null) return;
        contenedor.removeView(vistaCompleta);
        vistaCompleta = null;
        if (avisoCompleta != null) avisoCompleta.onCustomViewHidden();
        avisoCompleta = null;
        web.setVisibility(View.VISIBLE);
        barrasDelSistema(true);
    }

    // Muestra u oculta las barras del sistema.
    private void barrasDelSistema(boolean mostrar) {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (mostrar) c.show(WindowInsetsCompat.Type.systemBars());
        else c.hide(WindowInsetsCompat.Type.systemBars());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            contenedor.removeView(web);
            web.destroy();
        }
        super.onDestroy();
    }
}
