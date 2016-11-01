package nanohttpdwebserver;

import fi.iki.elonen.NanoHTTPD;
import nanohttpdwebserver.controller.WebServerController;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author francesco
 */
public final class NetworkTCPListener extends NanoHTTPD {

    private static Logger LOGGER = LoggerFactory.getLogger(NetworkTCPListener.class);

    /**
     * Common mime type for dynamic content: binary
     */
    public static final String MIME_DEFAULT_BINARY = "application/octet-stream";
    /**
     * Default Index file names.
     */
    public static final List<String> INDEX_FILE_NAMES = new ArrayList<String>() {
        {
            add("index.html");
            add("index.htm");
            add("index.html.twig");
            add("index.htm.twig");
        }
    };
    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {
        {
            put("css", "text/css");
            put("htm", "text/html");
            put("html", "text/html");
            put("xml", "text/xml");
            put("java", "text/x-java-source, text/java");
            put("md", "text/plain");
            put("txt", "text/plain");
            put("asc", "text/plain");
            put("gif", "image/gif");
            put("jpg", "image/jpeg");
            put("jpeg", "image/jpeg");
            put("png", "image/png");
            put("svg", "image/svg+xml");
            put("svgz", "image/svg+xml");
            put("mp3", "audio/mpeg");
            put("m3u", "audio/mpeg-url");
            put("mp4", "video/mp4");
            put("ogv", "video/ogg");
            put("flv", "video/x-flv");
            put("mov", "video/quicktime");
            put("swf", "application/x-shockwave-flash");
            put("js", "application/javascript");
            put("pdf", "application/pdf");
            put("doc", "application/msword");
            put("ogg", "application/x-ogg");
            put("zip", "application/octet-stream");
            put("exe", "application/octet-stream");
            put("class", "application/octet-stream");
        }
    };

    public Map<String, WebServerController> controllers = new HashMap<>();
    
    private final List<File> rootDirs;
    private final boolean quiet = false;

    /**
     * Used to initialize and customize the server.
     */
    public void init() {

        //URL www=getClass().getClassLoader().getResource("www");
        File www = new File("www").getAbsoluteFile();

        if (rootDirs.isEmpty()) {
            rootDirs.add(www);
            //rootDirs.add(wwwDirectory.getAbsoluteFile());
        }        
    }

//    private File getRootDir() {
//        return rootDirs.get(0);
//    }

    private List<File> getRootDirs() {
        return rootDirs;
    }

//    /**
//     * URL-encodes everything between "/"-characters. Encodes spaces as '%20'
//     * instead of '+'.
//     */
//    private String encodeUri(String uri) {
//        String newUri = "";
//        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
//        while (st.hasMoreTokens()) {
//            String tok = st.nextToken();
//            switch (tok) {
//                case "/":
//                    newUri += "/";
//                    break;
//                case " ":
//                    newUri += "%20";
//                    break;
//                default:
//                    try {
//                        newUri += URLEncoder.encode(tok, "UTF-8");
//                    } catch (UnsupportedEncodingException ignored) {
//                    }
//                    break;
//            }
//        }
//        return newUri;
//    }

    public NetworkTCPListener(int port) {
        super(port);

        this.rootDirs = new ArrayList<>();

        this.init();
    }

    public NetworkTCPListener() {
        this(8777);
    }

    private Response respond(Map<String, String> headers, IHTTPSession session, String uri) {
        // Remove URL arguments
        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }

        // Prohibit getting out of current directory
        if (uri.startsWith("src/main") || uri.endsWith("src/main") || uri.contains("../")) {
            return getForbiddenResponse("Won't serve ../ for security reasons.");
        }

        while (uri.endsWith("/")) {
            uri=uri.substring(0,uri.length()-1);
        }
        
        WebServerController wc=controllers.get(uri);
        
        if (wc!=null) {
            LOGGER.info("Eseguo controller:{}",wc);
            Response resp=wc.execMe(headers, session,  uri,context);
            if (resp!=null) {
                return resp;
            }
        }
        
        
        
        boolean canServeUri = false;
        File homeDir = null;
        List<File> roots = getRootDirs();
        for (int i = 0; !canServeUri && i < roots.size(); i++) {
            homeDir = roots.get(i);
            canServeUri = canServeUri(uri, homeDir);
        }
        if (!canServeUri) {
            return getNotFoundResponse();
        }

        // Browsers get confused without '/' after the directory, send a redirect.
        File f = new File(homeDir, uri);
        if (f.isDirectory() && !uri.endsWith("/")) {
            uri += "/";
            Response res = createResponse(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\""
                    + uri + "\">" + uri + "</a></body></html>");
            res.addHeader("Location", uri);
            return res;
        }

        if (f.isDirectory()) {
            // First look for index files (index.html, index.htm, etc) and if none found, list the directory if readable.
            String indexFile = findIndexFileInDirectory(f);
            if (indexFile == null) {
                if (f.canRead()) {
                    // No index file, list the directory if it is readable
                    //return createResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, listDirectory(uri, f));
                    return createResponse(Response.Status.UNAUTHORIZED, NanoHTTPD.MIME_HTML, "No directory listing");
                } else {
                    return getForbiddenResponse("No directory listing.");
                }
            } else {
                return respond(headers, session, uri + indexFile);
            }
        }

        
        String mimeTypeForFile = getMimeTypeForFile(uri);
        Response response = serveFile(uri, headers, f, mimeTypeForFile);
        return response != null ? response : getNotFoundResponse();
    }

    protected Response getNotFoundResponse() {
        return createResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                "Error 404, file not found.");
    }

    protected Response getForbiddenResponse(String s) {
        return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: "
                + s);
    }

    protected Response getInternalErrorResponse(String s) {
        return createResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
                "INTERNAL ERRROR: " + s);
    }

    private boolean canServeUri(String uri, File homeDir) {
        boolean canServeUri;
        File f = new File(homeDir, uri);
        canServeUri = f.exists();
        return canServeUri;
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    Response serveFile(String uri, Map<String, String> header, File file, String mime) {
        Response res;
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());

            // if twig...
            if (file.getName().endsWith(".twig")) {
                return createResponse(Response.Status.UNAUTHORIZED, NanoHTTPD.MIME_HTML, "Non autorizzato");
                
//                PebbleEngine engine = new PebbleEngine();
//                PebbleTemplate compiledTemplate;
//                try {
//                    compiledTemplate = engine.getTemplate(file.getAbsolutePath());
//
//                    Writer writer = new StringWriter();
//
//                    Map<String, Object> context = new HashMap<>();
//                    context.put("websiteTitle", "My First Website");
//                    context.put("content", "My Interesting Content");
//
//                    try {
//                        compiledTemplate.evaluate(writer,context);
//                    } catch (PebbleException ex) {
//                        logger.error(ex, ex);
//                    }
//
//                    String output = writer.toString();
//                    
//                    
//                    file = File.createTempFile("temp-file-name", ".tmp"); 
//                    try (FileWriter out = new FileWriter(file)) {
//                        out.write(output);
//                    }
//                    
//                    etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());
//                    
//                } catch (PebbleException ex) {
//                    logger.error(ex, ex);
//                }
//
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
                    res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
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

                    res = createResponse(Response.Status.PARTIAL_CONTENT, mime, fis);
                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.get("if-none-match"))) {
                    res = createResponse(Response.Status.NOT_MODIFIED, mime, "");
                } else {
                    res = createResponse(Response.Status.OK, mime, new FileInputStream(file));
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = getForbiddenResponse("Reading file failed.");
        }

        return res;
    }

    // Get MIME type from file name extension, if possible
    private String getMimeTypeForFile(String uri) {
        int dot = uri.lastIndexOf('.');
        String mime = null;
        if (dot >= 0) {
            mime = MIME_TYPES.get(uri.substring(dot + 1).toLowerCase());
        }
        return mime == null ? MIME_DEFAULT_BINARY : mime;
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType, InputStream message) {
        Response res = new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType, String message) {
        Response res = new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    private String findIndexFileInDirectory(File directory) {
        for (String fileName : INDEX_FILE_NAMES) {
            File indexFile = new File(directory, fileName);
            if (indexFile.exists()) {
                return fileName;
            }
        }
        return null;
    }

    @Override
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();
        String uri = session.getUri();

        Iterator<Map.Entry<String, String>> entries = parms.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, String> entry = entries.next();

            String key = entry.getKey();
            String value = entry.getValue();

            LOGGER.info(key + "=" + value);
        }

//        NanoHTTPD.Method method = session.getMethod();
//        System.out.println(method + " '" + uri + "' ");
//
//        String msg = "<html><body><h1>Hello server</h1>\n";
//        if (parms.get("username") == null) {
//            msg
//                    += "<form action='?' method='get'>\n"
//                    + "  <p>Your name: <input type='text' name='comando'></p>\n"
//                    + "</form>\n";
//        } else {
//            msg += "<p>Hello, " + parms.get("username") + "!</p>";
//        }
//
//        msg += "</body></html>\n";
//
//        return new NanoHTTPD.Response(msg);
//    }
        for (File homeDir : getRootDirs()) {
            // Make sure we won't die of an exception later
            if (!homeDir.isDirectory()) {
                return getInternalErrorResponse("given path is not a directory.");// (" + homeDir + ")
            }
        }
        return respond(Collections.unmodifiableMap(header), session, uri);
    }

//    public static URL getFileFromResource(String resourceLocation) throws URISyntaxException {
//        try {
//            return NetworkTCPListener.class.getResource(resourceLocation).toURI().toURL();            
//        } catch(NullPointerException | MalformedURLException e) {
//            logger.error(e);
//            return null;
//        }
//
//
//    }    
}
