package py.jokerpy.app.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

// Reconoce direcciones de publicidad para bloquearlas.
public final class BloqueadorAnuncios {
    private static Set<String> dominios;

    private BloqueadorAnuncios() {
    }

    // Carga la lista de dominios una sola vez.
    public static synchronized void cargar(Context ctx) {
        if (dominios != null) return;
        Set<String> lista = new HashSet<>();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(ctx.getAssets().open("anuncios.txt"), "UTF-8"))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.trim().toLowerCase(Locale.ROOT);
                if (!linea.isEmpty() && !linea.startsWith("#")) lista.add(linea);
            }
        } catch (Exception ignorado) {
            // sin lista no se bloquea nada
        }
        dominios = lista;
    }

    // Dice si la direccion pertenece a un dominio de publicidad.
    public static boolean esAnuncio(String host) {
        if (dominios == null || host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        while (true) {
            if (dominios.contains(h)) return true;
            int punto = h.indexOf('.');
            if (punto < 0) return false;
            h = h.substring(punto + 1);
        }
    }
}
