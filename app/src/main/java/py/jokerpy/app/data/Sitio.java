package py.jokerpy.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Un sitio guardado por el usuario.
@Entity(tableName = "sitio")
public class Sitio {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String nombre;
    public String url;
    public String iconoPropio; // ruta del icono elegido por el usuario
    public String iconoWeb;    // ruta del icono bajado de la pagina
    public boolean favorito;
}
