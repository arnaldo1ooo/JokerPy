package py.jokerpy.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

// Lee y guarda las opciones de la app.
public final class Ajustes {
    public static final String BLOQUEAR = "bloquear_anuncios";
    public static final String EXTERNO = "abrir_externo";
    private static final String PRECARGADO = "precargado";

    private Ajustes() {
    }

    private static SharedPreferences prefs(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c);
    }

    // Dice si el bloqueador de anuncios esta activo.
    public static boolean bloquearAnuncios(Context c) {
        return prefs(c).getBoolean(BLOQUEAR, true);
    }

    // Dice si los sitios se abren en el navegador externo.
    public static boolean abrirExterno(Context c) {
        return prefs(c).getBoolean(EXTERNO, false);
    }

    // Dice si ya se cargaron los canales iniciales.
    public static boolean yaPrecargado(Context c) {
        return prefs(c).getBoolean(PRECARGADO, false);
    }

    // Marca que los canales iniciales ya se cargaron.
    public static void marcarPrecargado(Context c) {
        prefs(c).edit().putBoolean(PRECARGADO, true).apply();
    }
}
