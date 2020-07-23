package sample.utils;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class gzip {

    static byte[] buffer = new byte[1024];
    static int len;

    public static synchronized ByteArrayOutputStream decompressFile(File fileSource) throws IOException {

        GZIPInputStream gis = new GZIPInputStream(new FileInputStream(fileSource));
        ByteArrayOutputStream fos = new ByteArrayOutputStream();

        while((len = gis.read(buffer)) != -1)
            fos.write(buffer, 0, len);

        fos.close();
        gis.close();

        return fos;

    }

    public static synchronized void compressData(ByteArrayInputStream inData, File outFile) throws IOException {

        GZIPOutputStream gzipOS = new GZIPOutputStream(new FileOutputStream(outFile));

        int len;
        while((len=inData.read(buffer)) != -1)
            gzipOS.write(buffer, 0, len);

        gzipOS.close();
        inData.close();
    }

}