package ulisse.filesystemforensics;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.sql.DataSource;

/**
 * Created by matteo on 10/06/16.
 */
public class Utility {

    public static byte[] zip(String[] files) throws IOException {

        BufferedInputStream origin = null;
        //ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        File file = new File(files[0]);
        int BUFFER_SIZE = (int) file.length();

        ByteArrayOutputStream bout = new ByteArrayOutputStream(BUFFER_SIZE);
        ZipOutputStream zos = new ZipOutputStream( bout );

        try {
            byte[] data = new byte[BUFFER_SIZE];

            for (int i = 0; i < files.length; i++) {
                FileInputStream fi = new FileInputStream(files[i]);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                try {
                    ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
                    //out.putNextEntry(entry);
                    //entry.setSize(BUFFER_SIZE);
                    zos.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                        zos.write(data, 0, count);
                        //out.write(data, 0, count);
                    }
                }
                finally {
                    origin.close();
                }
            }

        }
        finally {
            //out.close();
            zos.close();
            return bout.toByteArray();
        }
    }

    public static void unzip(String zipFile, String location) throws IOException {
        try {
            File f = new File(location);
            if(!f.isDirectory()) {
                f.mkdirs();
            }
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
            try {
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {
                    String path = location + ze.getName();

                    if (ze.isDirectory()) {
                        File unzipFile = new File(path);
                        if(!unzipFile.isDirectory()) {
                            unzipFile.mkdirs();
                        }
                    }
                    else {
                        FileOutputStream fout = new FileOutputStream(path, false);
                        try {
                            for (int c = zin.read(); c != -1; c = zin.read()) {
                                fout.write(c);
                            }
                            zin.closeEntry();
                        }
                        finally {
                            fout.close();
                        }
                    }
                }
            }
            finally {
                zin.close();
            }
        }
        catch (Exception e) {
            Log.e("ZIP", "Unzip exception", e);
        }
    }
}
