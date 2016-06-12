package ulisse.filesystemforensics;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Base64;
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

    private final String ip = "192.168.1.2";
    private final String port = ":8001";
    private final int port_socket = 8889;

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



    public void doTakeAll(){
        Log.e("doInBackground", "doInBackground");
        new Thread(new Runnable() {
            public void run() {
                JSONObject takeAll = takeAll();
                executePostRequest(takeAll);
            }
        }).start();
    }

    public void makeZip(String response){
        response = response.replace("~/","");
        try {
            if (response.contains("emulated"))
                response = response.replace("emulated",Environment.getExternalStorageDirectory().getAbsolutePath());
            else {
                HashSet<String> hash = getExternalMounts();
                Iterator it = hash.iterator();
                response = response.replace("extSdCard",it.next().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i("makeZip", "url file " + response);
        final String path = response;
        Log.i("doInBackground", "doInBackground");
        new Thread(new Runnable() {
            public void run() {
                Log.i("makeZip_OnRun", "Partito");
                try {
                    String nomeFile = path.substring(path.lastIndexOf("/")+1,path.length());
                    Log.i("makeZip_OnRun", nomeFile);

                    //executePostRequest(to_send);
                    URL url = new URL("http://"+ip + port);

                    //byte[] bytes = json_to_post.toString().getBytes("UTF-8");

                    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                    httpCon.setDoOutput(true);
                    httpCon.setUseCaches(false);
                    httpCon.setRequestMethod("POST");
                    httpCon.setRequestProperty("Content-Type", "application/bytearray; charset=UTF-8");
                    httpCon.setRequestProperty("id-device", Build.SERIAL);
                    httpCon.setRequestProperty("filename", nomeFile);

                    byte[] prova = new byte[0];
                    try {
                        prova = Utility.zip(new String[]{path});
                        httpCon.setRequestProperty("result", "ok");
                    } catch (IOException e) {
                        httpCon.setRequestProperty("result", "file non trovato");
                    }
                    JSONObject to_send = new JSONObject();
                    to_send.put("BYTE_ARRAY",prova);
                    to_send.put("SERIAL", Build.SERIAL);
                    httpCon.setFixedLengthStreamingMode(prova.length);

                    OutputStream os = httpCon.getOutputStream();
                    os.write(prova);
                    os.close();


                    Log.e("takeAll-TST", "executePOSTRequest Finished");



                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.i("makeZip_OnRun", "Partito");
            }
        }).start();
    }



        private JSONObject takeAll(){
            
            Log.e("testJSON: ", "takeAll Started");
            HashSet<String> external = getExternalMounts();
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

                if (!external.isEmpty()) {
                    Iterator iterator = external.iterator();
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
                jsonObject.put("DATA", String.valueOf((int) (System.currentTimeMillis() / 1000L)));

            } catch (JSONException e) {
                Log.e("testJSON: ", e.toString());
            }
            Log.e("testJSON: ", "takeAll Finished");
            return jsonObject;
        }

        public String executePostRequest(JSONObject json_to_post) {
            try {
                //constants
                URL url = new URL("http://"+ip + port);

                byte[] bytes = json_to_post.toString().getBytes("UTF-8");

                HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                httpCon.setDoOutput(true);
                httpCon.setUseCaches(false);
                httpCon.setFixedLengthStreamingMode(bytes.length);
                httpCon.setRequestMethod("POST");
                httpCon.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                OutputStream os = httpCon.getOutputStream();
                os.write(bytes);
                os.close();

                Log.e("takeAll-TST", "Il json pesa: "+bytes.length);
                Log.e("takeAll-TST", "executePOSTRequest Finished");
            } catch (IOException e) {
                e.printStackTrace();

            }
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

        @Override
        protected String doInBackground(String... urls) {
            Socket s = null;
            BufferedReader br = null;
            while (true){
                try {
                    s = new Socket(ip, port_socket);
                    DataOutputStream dos = null;
                    DataInputStream dis2 = null;
                    boolean socketAlive = true;
                    String toWrite = "ack";

                    dos = new DataOutputStream(s.getOutputStream());

                    dos.writeUTF(toWrite);

                    //read input stream
                    dis2 = new DataInputStream(s.getInputStream());
                    InputStreamReader disR2 = new InputStreamReader(dis2);
                    br = new BufferedReader(disR2);//create a BufferReader object for input
                    while (socketAlive) {
                        String response = br.readLine(); //read line
                        br = new BufferedReader(new InputStreamReader(s.getInputStream()));

                        Log.i("TSTTTT", response + " " + response.toString() + " " + response.length());

                        if (br == null) {
                            Log.e("TESTSOCKET", "BR NULL");
                            break;
                        }
                        
                        if (response.equals("ack")) {
                            Log.i("TESTSOCKET", "primo");
                            SystemClock.sleep(1000);
                            toWrite = "ack";
                            dos.writeUTF(toWrite);
                        } else if (response.equals("takeAll")){
                            Log.i("TESTSOCKET", "takeAll");
                            toWrite = "OK";
                            doTakeAll();
                        }else {
                            Log.i("TESTSOCKET", "take file");
                            toWrite = "OK";
                            makeZip(response);
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

}
