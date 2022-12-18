package gr.aueb.tcp3wput.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class TCP3wputServer extends Service {

    // onBind method to be left intact; no overloading required.
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
