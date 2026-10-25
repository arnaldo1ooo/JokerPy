package py.jokerpy.app.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Hilos de fondo para no trabar la pantalla.
public final class Hilos {
    public static final ExecutorService IO = Executors.newFixedThreadPool(3);

    private Hilos() {
    }
}
