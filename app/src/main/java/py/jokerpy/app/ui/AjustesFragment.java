package py.jokerpy.app.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import py.jokerpy.app.R;
import py.jokerpy.app.util.Hilos;
import py.jokerpy.app.util.Respaldo;

// Opciones: bloqueador, navegador externo y respaldo de la lista.
public class AjustesFragment extends PreferenceFragmentCompat {

    private final ActivityResultLauncher<String> crearArchivo = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) exportar(uri);
            });

    private final ActivityResultLauncher<String[]> abrirArchivo = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) importar(uri);
            });

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferencias, rootKey);

        Preference exportar = findPreference("exportar");
        if (exportar != null) {
            exportar.setOnPreferenceClickListener(p -> {
                avisarSiFalla(() -> crearArchivo.launch("jokerpy-sitios.json"));
                return true;
            });
        }
        Preference importar = findPreference("importar");
        if (importar != null) {
            importar.setOnPreferenceClickListener(p -> {
                avisarSiFalla(() -> abrirArchivo.launch(new String[]{"*/*"}));
                return true;
            });
        }
    }

    // Avisa si el dispositivo no tiene un selector de archivos.
    private void avisarSiFalla(Runnable accion) {
        try {
            accion.run();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.sin_selector, Toast.LENGTH_LONG).show();
        }
    }

    // Guarda la lista en el archivo elegido.
    private void exportar(Uri destino) {
        final Context app = requireContext().getApplicationContext();
        Hilos.IO.execute(() -> {
            try {
                int n = Respaldo.exportar(app, destino);
                mensaje(app, "Lista guardada: " + n + " sitios");
            } catch (Exception e) {
                mensaje(app, "No se pudo guardar el archivo");
            }
        });
    }

    // Agrega los sitios nuevos del archivo elegido.
    private void importar(Uri origen) {
        final Context app = requireContext().getApplicationContext();
        Hilos.IO.execute(() -> {
            try {
                int n = Respaldo.importar(app, origen);
                mensaje(app, n == 0 ? "No hay sitios nuevos para agregar" : "Sitios agregados: " + n);
            } catch (Exception e) {
                mensaje(app, "El archivo no es válido");
            }
        });
    }

    // Muestra un aviso desde cualquier hilo.
    private void mensaje(Context app, String texto) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(app, texto, Toast.LENGTH_LONG).show());
    }
}
