package ulisse.filesystemforensics;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.security.MessageDigest;
import java.sql.Date;
import java.text.DateFormat;


public class MyIntentService extends IntentService {

    public MyIntentService() {
        super("MyIntentService");

        Log.e("Service", "MyIntentService");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("Service", "onStartCommand");
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        new DownloadFilesTask().execute(null, null, null);
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e("Service", "onHandleIntent");
    }


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
                Toast.makeText(MyIntentService.this, jsonObjects.getString("sha1").toString() + "\t" + aaa.getBytes().length, Toast.LENGTH_SHORT).show();

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
                jsonObject.put("sha1", getSha1Hex(jsonObject.toString())); //faccio hash di tutto.. sarebbe da fare hash solo del ritorno della funzione ls(sdcard)
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return jsonObject;
        }
    }


}
