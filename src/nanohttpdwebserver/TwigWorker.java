/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nanohttpdwebserver;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import fi.iki.elonen.NanoHTTPD;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author francesco
 */
public class TwigWorker {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TwigWorker.class);
    
    public static NanoHTTPD.Response serveFile(String uri, Map<String, String> header, NanoHTTPD.IHTTPSession session, File file, String mimeType,Map<String, Object> context) {

        if (mimeType==null) {
            mimeType="text/html";
        }

        NanoHTTPD.Response res;
        try {
            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());

            // if twig...
            if (file.getName().endsWith(".twig")) {

                PebbleEngine engine = new PebbleEngine();
                PebbleTemplate compiledTemplate;
                try {
                    compiledTemplate = engine.getTemplate(file.getAbsolutePath());

                    Writer writer = new StringWriter();

                    try {
                    if (context!=null) {
                        compiledTemplate.evaluate(writer,context);                        
                    } else {
                        compiledTemplate.evaluate(writer);
                    }
                    
                    } catch (PebbleException ex) {
                        LOGGER.error("Errore in twigworker: {}", ex);
                    }

                    String output = writer.toString();
                    
                    
                    file = File.createTempFile("temp-file-name", ".tmp"); 
                    try (FileWriter out = new FileWriter(file)) {
                        out.write(output);
                    }
                    
                    // Calculate etag
                    etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());
                    
                } catch (PebbleException ex) {
                    LOGGER.error("Errore in twigworker: {}", ex);
                }

            }

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is requested
            long fileLen = file.length();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = createResponse(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
                    FileInputStream fis = new FileInputStream(file) {
                        @Override
                        public int available() throws IOException {
                            return (int) dataLen;
                        }
                    };
                    fis.skip(startFrom);

                    res = createResponse(NanoHTTPD.Response.Status.PARTIAL_CONTENT, mimeType, fis);
                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.get("if-none-match"))) {
                    res = createResponse(NanoHTTPD.Response.Status.NOT_MODIFIED, mimeType, "");
                } else {
                    res = createResponse(NanoHTTPD.Response.Status.OK, mimeType, new FileInputStream(file));
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = getForbiddenResponse("Reading file failed.");
        }

        return res;
    }

    private String readSource(File file) {
        FileReader fileReader = null;
        BufferedReader reader = null;
        try {
            fileReader = new FileReader(file);
            reader = new BufferedReader(fileReader);
            String line;
            StringBuilder sb = new StringBuilder();
            do {
                line = reader.readLine();
                if (line != null) {
                    sb.append(line).append("\n");
                }
            } while (line != null);
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            LOGGER.error("Errore in readSource: {}", e);
            return null;
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {}
        }
    }
    
    
    private static NanoHTTPD.Response createResponse(NanoHTTPD.Response.Status status, String mimeType, InputStream message) {
        NanoHTTPD.Response res = new NanoHTTPD.Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }
    public static NanoHTTPD.Response createResponse(NanoHTTPD.Response.Status status, String mimeType, String message) {
        NanoHTTPD.Response res = new NanoHTTPD.Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }
    private static  NanoHTTPD.Response getForbiddenResponse(String s) {
        return createResponse(NanoHTTPD.Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: "
                + s);
    }
    
}
