package py.jokerpy.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

// Acceso a la lista de sitios guardados.
@Dao
public interface SitioDao {

    // Todos los sitios: favoritos primero, luego por nombre.
    @Query("SELECT * FROM sitio ORDER BY favorito DESC, nombre COLLATE NOCASE ASC")
    LiveData<List<Sitio>> todos();

    // Lista simple de todos los sitios (para el respaldo).
    @Query("SELECT * FROM sitio ORDER BY nombre COLLATE NOCASE ASC")
    List<Sitio> lista();

    @Query("SELECT * FROM sitio WHERE id = :id")
    Sitio buscar(long id);

    @Insert
    long insertar(Sitio sitio);

    @Update
    void actualizar(Sitio sitio);

    @Delete
    void eliminar(Sitio sitio);
}
