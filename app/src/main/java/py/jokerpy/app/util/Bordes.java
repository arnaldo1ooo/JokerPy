package py.jokerpy.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

// Evita que la barra de estado y los botones del sistema tapen la pantalla.
public final class Bordes {

    private Bordes() {
    }

    // Dice si el tema actual es claro.
    public static boolean temaClaro(Context c) {
        int modo = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return modo != Configuration.UI_MODE_NIGHT_YES;
    }

    // Deja un margen igual al de las barras del sistema, la camara y el teclado.
    public static void aplicar(Activity actividad, View raiz, boolean iconosOscuros) {
        Window ventana = actividad.getWindow();
        WindowCompat.setDecorFitsSystemWindows(ventana, false);
        ventana.setStatusBarColor(Color.TRANSPARENT);
        ventana.setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat control = WindowCompat.getInsetsController(ventana, raiz);
        control.setAppearanceLightStatusBars(iconosOscuros);
        control.setAppearanceLightNavigationBars(iconosOscuros);

        ViewCompat.setOnApplyWindowInsetsListener(raiz, (vista, insets) -> {
            Insets m = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            vista.setPadding(m.left, m.top, m.right, m.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(raiz);
    }
}
