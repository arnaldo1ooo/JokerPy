package py.jokerpy.app.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Busca, baja y guarda los iconos de los sitios.
public final class IconoHelper {
    private static final String AGENTE = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36";
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int TAMANO = 256;

    private IconoHelper() {
    }

    // Baja el icono de la pagina y lo guarda en el sitio.
    public static void refrescarIconoWeb(Context ctx, long idSitio) {
        SitioDao dao = AppDatabase.get(ctx).sitioDao();
        Sitio sitio = dao.buscar(idSitio);
        if (sitio == null) return;
        Bitmap bmp = bajarIcono(sitio.url);
        if (bmp == null) return;
        String ruta = guardar(ctx, bmp, "web_" + idSitio);
        if (ruta == null) return;
        borrar(sitio.iconoWeb);
        sitio.iconoWeb = ruta;
        dao.actualizar(sitio);
    }

    // Busca el icono en la pagina y, si no hay, prueba con favicon.ico.
    private static Bitmap bajarIcono(String paginaUrl) {
        List<String> candidatos = new ArrayList<>();
        try {
            String delaPagina = buscarEnPagina(paginaUrl);
            if (delaPagina != null) candidatos.add(delaPagina);
        } catch (Exception ignorado) {
            // se prueba con favicon.ico
        }
        try {
            candidatos.add(new URL(new URL(paginaUrl), "/favicon.ico").toString());
        } catch (Exception ignorado) {
            // direccion invalida
        }
        for (String url : candidatos) {
            try {
                byte[] datos = bajar(url);
                if (datos == null) continue;
                Bitmap bmp = BitmapFactory.decodeByteArray(datos, 0, datos.length);
                if (bmp != null) return bmp;
            } catch (Exception ignorado) {
                // se prueba el siguiente
            }
        }
        return null;
    }

    // Elige el icono mas grande declarado en la pagina.
    private static String buscarEnPagina(String paginaUrl) throws Exception {
        Document doc = Jsoup.connect(paginaUrl).userAgent(AGENTE).timeout(8000).get();
        String mejor = null;
        int mejorTamano = -1;
        for (Element link : doc.select("link[rel~=(?i)icon]")) {
            String href = link.absUrl("href");
            if (href.isEmpty() || href.toLowerCase().contains(".svg")) continue;
            int tamano = leerTamano(link.attr("sizes"));
            if (tamano < 0) tamano = link.attr("rel").toLowerCase().contains("apple") ? 180 : 16;
            if (tamano > mejorTamano) {
                mejorTamano = tamano;
                mejor = href;
            }
        }
        return mejor;
    }

    // Lee el numero de un texto como 192x192.
    private static int leerTamano(String sizes) {
        Matcher m = Pattern.compile("(\\d+)x\\d+").matcher(sizes == null ? "" : sizes);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    // Descarga un archivo pequeno de internet.
    private static byte[] bajar(String direccion) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(direccion).openConnection();
        con.setConnectTimeout(8000);
        con.setReadTimeout(8000);
        con.setRequestProperty("User-Agent", AGENTE);
        try (InputStream in = con.getInputStream()) {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                salida.write(buffer, 0, n);
                if (salida.size() > MAX_BYTES) return null;
            }
            return salida.toByteArray();
        } finally {
            con.disconnect();
        }
    }

    // Guarda la imagen achicada y devuelve su ruta.
    public static String guardar(Context ctx, Bitmap original, String prefijo) {
        try {
            Bitmap bmp = original;
            int mayor = Math.max(bmp.getWidth(), bmp.getHeight());
            if (mayor > TAMANO) {
                float escala = TAMANO / (float) mayor;
                bmp = Bitmap.createScaledBitmap(bmp,
                        Math.max(1, Math.round(bmp.getWidth() * escala)),
                        Math.max(1, Math.round(bmp.getHeight() * escala)), true);
            }
            File carpeta = new File(ctx.getFilesDir(), "iconos");
            if (!carpeta.exists() && !carpeta.mkdirs()) return null;
            File archivo = new File(carpeta, prefijo + "_" + System.currentTimeMillis() + ".png");
            try (FileOutputStream out = new FileOutputStream(archivo)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            return archivo.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    // Borra un archivo de icono si existe.
    public static void borrar(String ruta) {
        if (ruta == null) return;
        File f = new File(ruta);
        if (f.exists()) //noinspection ResultOfMethodCallIgnored
            f.delete();
    }
}
