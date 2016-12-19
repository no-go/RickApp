package click.dummer.rickapp;

import android.app.Application;

public class App extends Application{
    public static String TAG;
    public static final int NOTIFYID = 478021;
    public static final String PROJECT_LINK = "https://no-go.github.io/RickApp/";
    public static final String FLATTR_ID = "o6wo7q";

    @Override
    public void onCreate() {
        super.onCreate();
        TAG = getApplicationContext().getPackageName();
    }
}
