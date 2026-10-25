package py.jokerpy.app.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import py.jokerpy.app.R;

// Pantalla de configuracion de la app.
public class AjustesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contenedor, new AjustesFragment())
                    .commit();
        }
    }
}
