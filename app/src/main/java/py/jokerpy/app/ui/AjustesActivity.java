package py.jokerpy.app.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import py.jokerpy.app.R;
import py.jokerpy.app.util.Bordes;

// Pantalla de configuracion de la app.
public class AjustesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        Bordes.aplicar(this, findViewById(android.R.id.content), Bordes.temaClaro(this));
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contenedor, new AjustesFragment())
                    .commit();
        }
    }
}
