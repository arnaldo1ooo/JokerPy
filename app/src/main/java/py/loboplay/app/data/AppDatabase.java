package py.loboplay.app.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

// Base de datos local de la app.
@Database(entities = {Sitio.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract SitioDao sitioDao();

    private static volatile AppDatabase instancia;

    // Agrega las columnas nuevas sin perder los sitios guardados.
    private static final Migration DE_1_A_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE sitio ADD COLUMN escritorio INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE sitio ADD COLUMN orden INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE sitio ADD COLUMN ultimoAcceso INTEGER NOT NULL DEFAULT 0");
        }
    };

    // Devuelve la unica base de datos de la app.
    public static AppDatabase get(Context ctx) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                                    ctx.getApplicationContext(), AppDatabase.class, "loboplay.db")
                            .addMigrations(DE_1_A_2)
                            .build();
                }
            }
        }
        return instancia;
    }
}
