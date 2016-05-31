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

        txt = (TextView) findViewById(R.id.textview);
        txt.setMovementMethod(new ScrollingMovementMethod());
        txt.setText("aòksjdbvpiawejbgvpawebgqwebgjqewh");

        // La cosa più importante dell'universo da Marshmallow in poi a quanto pare
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);


        new DownloadFilesTask().execute(null, null, null);
    }

   /*
   NON ho ancora capito se serve o meno
   @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }*/




    public static String getSha1Hex(String clearString)
    {
        try
        {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(clearString.getBytes("UTF-8"));
            byte[] bytes = messageDigest.digest();
            StringBuilder buffer = new StringBuilder();
            for (byte b : bytes)
            {
                buffer.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return buffer.toString();
        }
        catch (Exception ignored)
        {
            ignored.printStackTrace();
            return null;
        }
    }

    private class DownloadFilesTask extends AsyncTask<JSONObject, JSONObject, JSONObject > {


        private JSONArray ls(File f){
            JSONArray jsonArray = new JSONArray();

            File[]dirs = f.listFiles();

            for(File ff: dirs) {

                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);

                JSONObject what = new JSONObject();

                try {

                    what.put("isDirectory", ff.isDirectory());
                    what.put("nome", ff.getName());
                    what.put("lastModDate", date_modify);


                    if(ff.isDirectory())
                        what.put("sub", ls(new File(ff.getPath())));
                    else {
                        what.put("sub", "");
                        what.put("Byte", ff.length());
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                jsonArray.put(what);

            }

            return jsonArray;
        }

        protected void onPostExecute(JSONObject jsonObjects) {
            String aaa = jsonObjects.toString();
            Log.e("testJSON: ",aaa );
            try {
                txt.setText(jsonObjects.getString("sha1").toString() + "\n" + aaa.getBytes().length);

            Log.e("testJSON: ", "done -> " + jsonObjects.getString("sha1").toString() + " " + aaa.getBytes().length);
            } catch (JSONException e) {
                Log.e("MainActivity: ", "JSONException in onPostExecute");
            }
        }

        @Override
        protected JSONObject doInBackground(JSONObject... jsonObjects) {
            File sdcard = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("FileSystem", ls(sdcard));
                jsonObject.put("sha1", getSha1Hex(jsonObject.toString()));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return jsonObject;
        }
    }

}