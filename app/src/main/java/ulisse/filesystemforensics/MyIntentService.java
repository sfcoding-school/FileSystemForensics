package ulisse.filesystemforensics;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.sql.Date;
import java.text.DateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;



public class MyIntentService extends IntentService {

    public MyIntentService() {
        super("MyIntentService");

        Log.e("Service", "MyIntentService");
    }

    public static HashSet<String> getExternalMounts() {
        final HashSet<String> out = new HashSet<String>();
        String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
        String s = "";
        try {
            final Process process = new ProcessBuilder().command("mount")
                    .redirectErrorStream(true).start();
            process.waitFor();
            final InputStream is = process.getInputStream();
            final byte[] buffer = new byte[1024];
            while (is.read(buffer) != -1) {
                s = s + new String(buffer);
            }
            is.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // parse output
        final String[] lines = s.split("\n");
        for (String line : lines) {
            if (!line.toLowerCase(Locale.ITALY).contains("asec")) {
                if (line.matches(reg)) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (part.startsWith("/"))
                            if (!part.toLowerCase(Locale.ITALY).contains("vold"))
                                out.add(part);
                    }
                }
            }
        }
        return out;
    }

    public static String getSha1Hex(String clearString) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(clearString.getBytes("UTF-8"));
            byte[] bytes = messageDigest.digest();
            StringBuilder buffer = new StringBuilder();
            for (byte b : bytes) {
                buffer.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return buffer.toString();
        } catch (Exception ignored) {
            ignored.printStackTrace();
            return null;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("Service", "onStartCommand");
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        connect();

        //new DownloadFilesTask().execute(null, null, null);
        return START_STICKY;
    }

    /**/

    public void doSomething(){
        Log.e("doInBackground", "doInBackground");
        new Thread(new Runnable() {
            public void run() {
                executeGetRequest();
            }
        }).start();
    }


        private JSONObject takeAll(){
            // non ritorna l'emulata ma ritorna un eventuale esterna
            //  ovvero se è vuoto allora non si ha nessuna esterna

            /*
            String pr = "";
            Iterator iterator = ttt.iterator();
            while (iterator.hasNext()) {
                pr += iterator.next();
            }
            */
            Log.e("testJSON: ", "takeAll Started");

            HashSet<String> ttt = getExternalMounts();
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            try {
                File sdcard = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

                Log.e("testJSON: ", "Emulated -> " + sdcard.toString());
                JSONObject what = new JSONObject();

                what.put("nome", "emulated");
                what.put("isDirectory",true);
                what.put("lastModDate", "");
                what.put("sub", lsRecursive(sdcard));
                jsonArray.put(what);

                if (!ttt.isEmpty()) {
                    Iterator iterator = ttt.iterator();
                    while (iterator.hasNext()) {
                        JSONObject what2 = new JSONObject();
                        String temp = iterator.next().toString();
                        Log.e("testJSON: ", "Others -> " + temp);

                        what2.put("isDirectory", true);
                        what2.put("lastModDate", "");
                        what2.put("nome", temp.replace("/storage/", ""));
                        what2.put("sub", lsRecursive(new File(temp)));

                        jsonArray.put(what2);
                    }
                }

                jsonObject.put("FileSystem", jsonArray);
                jsonObject.put("sha1", getSha1Hex(jsonObject.getString("FileSystem").toString())); // ora faccio hash solo del contenuto della chiave "FileSystem"

                jsonObject.put("MODEL", Build.MODEL);
                jsonObject.put("DEVICE", Build.DEVICE); /*Codename*/
                jsonObject.put("MANUFACTURER", Build.MANUFACTURER);
                jsonObject.put("BUILDN", Build.ID); /*Build Number*/
                jsonObject.put("SERIAL", Build.SERIAL);
                jsonObject.put("BRAND", Build.BRAND);

            } catch (JSONException e) {
                Log.e("testJSON: ", e.toString());
            }

            /*
            File sdcard = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("FileSystem", );
                jsonObject.put("sha1", getSha1Hex(jsonObject.getString("FileSystem").toString())); // ora faccio hash solo del contenuto della chiave "FileSystem"
            } catch (JSONException e) {
                e.printStackTrace();
            }
            */
            Log.e("testJSON: ", "takeAll Finished");
            return jsonObject;
        }

        public String executeGetRequest() {
            Log.e("takeAll-TST", "executePOSTRequest");
            JSONObject jsonObject = takeAll();

            //OutputStream os = null;
            InputStream is = null;
            HttpURLConnection httpCon = null;
            try {
                //constants
                URL url = new URL("http://192.168.0.2:8000");

                byte[] bytes = jsonObject.toString().getBytes("UTF-8");

                httpCon = (HttpURLConnection) url.openConnection();
                httpCon.setDoOutput(true);
                httpCon.setUseCaches(false);
                httpCon.setFixedLengthStreamingMode(bytes.length);
                httpCon.setRequestMethod("POST");
                httpCon.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                OutputStream os = httpCon.getOutputStream();
                os.write(bytes);
                os.close();


                Log.e("takeAll-TST", "executePOSTRequest Finished");
            } catch (IOException e) {
                e.printStackTrace();

            }


           /* HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL("http://192.168.0.2:8000");

                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setDoOutput(true);
                // is output buffer writter
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                //urlConnection.setRequestProperty("Accept", "application/json");
//set headers and method
                Writer writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                writer.write(String.valueOf(jsonObject));
// json data
                writer.close();
                Log.e("takeAll-TST", "executePOSTRequest finished" );
            } catch (IOException e) {
                Log.e("takeAll-TST", "executePOSTRequest " + e);
            }*/
            return "";

        }

        private JSONArray lsRecursive(File f) {
            JSONArray jsonArray = new JSONArray();

            File[] dirs = f.listFiles();
            if (dirs == null) return jsonArray;

            for (File ff : dirs) {
                try {
                    Date lastModDate = new Date(ff.lastModified());
                    DateFormat formater = DateFormat.getDateTimeInstance();
                    String date_modify = formater.format(lastModDate);

                    JSONObject what = new JSONObject();

                    try {

                        what.put("isDirectory", ff.isDirectory());
                        what.put("nome", ff.getName()); //.replace(" ", "\\ ")
                        what.put("lastModDate", date_modify);


                        if (ff.isDirectory())
                            what.put("sub", lsRecursive(new File(ff.getPath())));
                        else {
                            what.put("sub", "");
                            what.put("Byte", ff.length());
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    jsonArray.put(what);
                } catch (NullPointerException e) {
                    Log.e("NULLPOINTER", f.toString());
                }

            }


            return jsonArray;
        }



    private class testSocket extends AsyncTask<String, Void, String> {

        private JSONArray lsRecursive(File f) {
            JSONArray jsonArray = new JSONArray();

            File[] dirs = f.listFiles();
            if (dirs == null) return jsonArray;

            for (File ff : dirs) {
                try {
                    Date lastModDate = new Date(ff.lastModified());
                    DateFormat formater = DateFormat.getDateTimeInstance();
                    String date_modify = formater.format(lastModDate);

                    JSONObject what = new JSONObject();

                    try {

                        what.put("isDirectory", ff.isDirectory());
                        what.put("nome", ff.getName()); //.replace(" ", "\\ ")
                        what.put("lastModDate", date_modify);


                        if (ff.isDirectory())
                            what.put("sub", lsRecursive(new File(ff.getPath())));
                        else {
                            what.put("sub", "");
                            what.put("Byte", ff.length());
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    jsonArray.put(what);
                } catch (NullPointerException e) {
                    Log.e("NULLPOINTER", f.toString());
                }

            }


            return jsonArray;
        }

        private JSONArray ls(File f) {
            JSONArray jsonArray = new JSONArray();

            File[] dirs = f.listFiles();
            if (dirs == null) return jsonArray;

            for (File ff : dirs) {
                try {
                    Date lastModDate = new Date(ff.lastModified());
                    DateFormat formater = DateFormat.getDateTimeInstance();
                    String date_modify = formater.format(lastModDate);

                    JSONObject what = new JSONObject();

                    try {

                        what.put("isDirectory", ff.isDirectory());
                        what.put("nome", ff.getName()); //.replace(" ", "\\ ")
                        what.put("lastModDate", date_modify);


                        if (ff.isDirectory())
                            what.put("sub", "");
                        else {
                            what.put("sub", "");
                            what.put("Byte", ff.length());
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    jsonArray.put(what);
                } catch (NullPointerException e) {
                    Log.e("NULLPOINTER", f.toString());
                }

            }


            return jsonArray;
        }

        private JSONObject lsOnPath(String path){
            Log.e("testJSON: ", "lsOnPath Started");
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();

            File sdcard = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

            if (path.equals("root")){
                JSONObject what = new JSONObject();
                try {
                    what.put("nome", "emulated");

                what.put("isDirectory",true);
                what.put("lastModDate", "");
                what.put("sub", "");
                jsonArray.put(what);

                HashSet<String> ttt = getExternalMounts();

                if (!ttt.isEmpty()) {
                    Iterator iterator = ttt.iterator();
                    while (iterator.hasNext()) {
                        JSONObject what2 = new JSONObject();
                        String temp = iterator.next().toString();
                        Log.e("testJSON: ", "Others -> " + temp);

                        what2.put("isDirectory", true);
                        what2.put("lastModDate", "");
                        what2.put("nome", temp.replace("/storage/", ""));
                        what2.put("sub", "");

                        jsonArray.put(what2);
                    }
                }

                jsonObject.put("FileSystem", jsonArray);
                jsonObject.put("sha1", getSha1Hex(jsonObject.getString("FileSystem").toString())); // ora faccio hash solo del contenuto della chiave "FileSystem"

                jsonObject.put("MODEL", Build.MODEL);
                jsonObject.put("DEVICE", Build.DEVICE); /*Codename*/
                jsonObject.put("MANUFACTURER", Build.MANUFACTURER);
                jsonObject.put("BUILDN", Build.ID); /*Build Number*/
                jsonObject.put("SERIAL", Build.SERIAL);
                jsonObject.put("BRAND", Build.BRAND);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } /*else {
                path = path.replace("emulated", "");
                File pathss = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + path);
                jsonArray = ls(pathss);
                try {
                    jsonObject.put("FileSystem", jsonArray);

                jsonObject.put("sha1", getSha1Hex(jsonObject.getString("FileSystem").toString())); // ora faccio hash solo del contenuto della chiave "FileSystem"

                jsonObject.put("MODEL", Build.MODEL);
                jsonObject.put("DEVICE", Build.DEVICE); *//*Codename*//*
                jsonObject.put("MANUFACTURER", Build.MANUFACTURER);
                jsonObject.put("BUILDN", Build.ID); *//*Build Number*//*
                jsonObject.put("SERIAL", Build.SERIAL);
                jsonObject.put("BRAND", Build.BRAND);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }*/
            if (path.equals("takeAll")){
                Log.e("takeAll-TST", "got it");

               // executeGetRequest();
                return new JSONObject();
            }

            return jsonObject;

        }



        private JSONObject takeAll(){
            // non ritorna l'emulata ma ritorna un eventuale esterna
            //  ovvero se è vuoto allora non si ha nessuna esterna

            /*
            String pr = "";
            Iterator iterator = ttt.iterator();
            while (iterator.hasNext()) {
                pr += iterator.next();
            }
            */
            Log.e("testJSON: ", "takeAll Started");

            HashSet<String> ttt = getExternalMounts();
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            try {
                File sdcard = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

                Log.e("testJSON: ", "Emulated -> " + sdcard.toString());
                JSONObject what = new JSONObject();

                what.put("nome", "emulated");
                what.put("isDirectory",true);
                what.put("lastModDate", "");
                what.put("sub", lsRecursive(sdcard));
                jsonArray.put(what);

                if (!ttt.isEmpty()) {
                    Iterator iterator = ttt.iterator();
                    while (iterator.hasNext()) {
                        JSONObject what2 = new JSONObject();
                        String temp = iterator.next().toString();
                        Log.e("testJSON: ", "Others -> " + temp);

                        what2.put("isDirectory", true);
                        what2.put("lastModDate", "");
                        what2.put("nome", temp.replace("/storage/", ""));
                        what2.put("sub", lsRecursive(new File(temp)));

                        jsonArray.put(what2);
                    }
                }

                jsonObject.put("FileSystem", jsonArray);
                jsonObject.put("sha1", getSha1Hex(jsonObject.getString("FileSystem").toString())); // ora faccio hash solo del contenuto della chiave "FileSystem"

                jsonObject.put("MODEL", Build.MODEL);
                jsonObject.put("DEVICE", Build.DEVICE); /*Codename*/
                jsonObject.put("MANUFACTURER", Build.MANUFACTURER);
                jsonObject.put("BUILDN", Build.ID); /*Build Number*/
                jsonObject.put("SERIAL", Build.SERIAL);
                jsonObject.put("BRAND", Build.BRAND);

            } catch (JSONException e) {
                Log.e("testJSON: ", e.toString());
            }

            /*
            File sdcard = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("FileSystem", );
                jsonObject.put("sha1", getSha1Hex(jsonObject.getString("FileSystem").toString())); // ora faccio hash solo del contenuto della chiave "FileSystem"
            } catch (JSONException e) {
                e.printStackTrace();
            }
            */
            Log.e("testJSON: ", "takeAll Finished");
            return jsonObject;
        }

        @Override
        protected String doInBackground(String... urls) {
            Socket s = null;
            BufferedReader br = null;
            while (true){
                try {
                    s = new Socket("192.168.0.2", 8889);

                    DataOutputStream dos = null;

                    DataInputStream dis2 = null;
                    boolean acca = true;
                    String toWrite = "ack";

                    dos = new DataOutputStream(s.getOutputStream());

                    dos.writeUTF(toWrite);

                    //read input stream
                    dis2 = new DataInputStream(s.getInputStream());
                    InputStreamReader disR2 = new InputStreamReader(dis2);
                    br = new BufferedReader(disR2);//create a BufferReader object for input
                    while (acca) {
                        String response = br.readLine(); //read line
                        br = new BufferedReader(new InputStreamReader(s.getInputStream()));

                        Log.e("TSTTTT", response + " " + response.toString() + " " + response.length());

                        if (br == null) {
                            Log.e("TESTSOCKET", "BR NULL");
                            break;
                        }


                        if (response.equals("ack")) {
                            Log.e("TESTSOCKET", "primo");
                            SystemClock.sleep(1000);
                            toWrite = "ack";
                        } else {
                            Log.e("TESTSOCKET", "secondo");
                            toWrite = "OK";
                            doSomething();
                        }

                        dos.writeUTF(toWrite);
                    }


                    dis2.close();
                    s.close();
                    SystemClock.sleep(10000);

                } catch (Exception e) {
                    Log.e("TESTSOCKET-catch", String.valueOf(e));
                }
            }

        }

        @Override
        protected void onPostExecute(String result) {
            Log.e("TESTSOCKET_onPostE",result);
        }
    }

    public void connect() {
        new testSocket().execute(null, null, null);
    }
    /**/

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e("Service", "onHandleIntent");
    }

   /* private class DownloadFilesTask extends AsyncTask<JSONObject, JSONObject, JSONObject> {


        private JSONArray ls(File f) {
            JSONArray jsonArray = new JSONArray();

            File[] dirs = f.listFiles();
            if (dirs == null) return jsonArray;

            for (File ff : dirs) {
                try {
                    Date lastModDate = new Date(ff.lastModified());
                    DateFormat formater = DateFormat.getDateTimeInstance();
                    String date_modify = formater.format(lastModDate);

                    JSONObject what = new JSONObject();

                    try {

                        what.put("isDirectory", ff.isDirectory());
                        what.put("nome", ff.getName()); //.replace(" ", "\\ ")
                        what.put("lastModDate", date_modify);


                        if (ff.isDirectory())
                            what.put("sub", ls(new File(ff.getPath())));
                        else {
                            what.put("sub", "");
                            what.put("Byte", ff.length());
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    jsonArray.put(what);
                } catch (NullPointerException e) {
                    Log.e("NULLPOINTER", f.toString());
                }

            }


            return jsonArray;
        }

        protected void onPostExecute(JSONObject jsonObjects) {
            String aaa = jsonObjects.toString();
            Log.e("testJSON: ", aaa);
            try {
                Toast.makeText(MyIntentService.this, jsonObjects.getString("sha1").toString() + "\t" + aaa.getBytes().length, Toast.LENGTH_SHORT).show();

                Log.e("testJSON: ", "done -> " + jsonObjects.getString("sha1").toString() + " " + aaa.getBytes().length);

                FileWriter file = new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/" + "myFileSystem.json");
                file.write(aaa);
                file.flush();
                file.close();

            } catch (JSONException e) {
                Log.e("MainActivity: ", "JSONException in onPostExecute");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected JSONObject doInBackground(JSONObject... jsonObjects) {

            // non ritorna l'emulata ma ritorna un eventuale esterna
            //  ovvero se è vuoto allora non si ha nessuna esterna

            *//*
            String pr = "";
            Iterator iterator = ttt.iterator();
            while (iterator.hasNext()) {
                pr += iterator.next();
            }
            Log.e("testJSON: ", "done -> " + pr);*//*

            HashSet<String> ttt = getExternalMounts();
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            try {
                File sdcard = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

                Log.e("testJSON: ", "Emulated -> " + sdcard.toString());
                JSONObject what = new JSONObject();

                what.put("nome", "emulated");
                what.put("isDirectory",true);
                what.put("lastModDate", "");
                what.put("sub", ls(sdcard));
                jsonArray.put(what);

                if (!ttt.isEmpty()) {
                    Iterator iterator = ttt.iterator();
                    while (iterator.hasNext()) {
                        JSONObject what2 = new JSONObject();
                        String temp = iterator.next().toString();
                        Log.e("testJSON: ", "Others -> " + temp);

                        what2.put("isDirectory", true);
                        what2.put("lastModDate", "");
                        what2.put("nome", temp.replace("/storage/", ""));
                        what2.put("sub", ls(new File(temp)));

                        jsonArray.put(what2);
                    }
                }

                jsonObject.put("FileSystem", jsonArray);
                jsonObject.put("sha1", getSha1Hex(jsonObject.getString("FileSystem").toString())); // ora faccio hash solo del contenuto della chiave "FileSystem"

                jsonObject.put("MODEL", Build.MODEL);
                jsonObject.put("DEVICE", Build.DEVICE); *//*Codename*//*
                jsonObject.put("MANUFACTURER", Build.MANUFACTURER);
                jsonObject.put("BUILDN", Build.ID); *//*Build Number*//*
                jsonObject.put("SERIAL", Build.SERIAL);
                jsonObject.put("BRAND", Build.BRAND);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            *//*
            File sdcard = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("FileSystem", );
                jsonObject.put("sha1", getSha1Hex(jsonObject.getString("FileSystem").toString())); // ora faccio hash solo del contenuto della chiave "FileSystem"
            } catch (JSONException e) {
                e.printStackTrace();
            }
            *//*
            return jsonObject;
        }
    }
*/

}
