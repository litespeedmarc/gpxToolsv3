package ca.scibrazeau.gpx_tools.strava;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.*;

public class StoreUtils {

    static final String kCacheName = "cache";

    public static void save(Object toSave, Object ... pathParts) {
        File pathStr = getPath(pathParts);
        pathStr.getParentFile().mkdirs();
        Gson json = new GsonBuilder().create();
        try (
            OutputStream os = new BufferedOutputStream(new FileOutputStream(pathStr), 100000);
            Writer writer = new OutputStreamWriter(os);
        ) {
            json.toJson(toSave, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Take any sort of object, and convert it to a path, handling Strings, Files, Arrays
     * @param pathParts
     * @return Converted path
     */
    private static File getPath(Object ... pathParts) {
        return getPath(true, pathParts);

    }

    private static File getPath(boolean first, Object ... pathParts) {
        StringBuilder path = new StringBuilder(first ? "gpx_tools" : "");
        for (Object o :pathParts) {
            if (o == null) {
                continue;
            }
            String toAppend = null;
            if (o.getClass().isArray()) {
                File toAppendF = getPath(false, (Object[]) o);
                toAppend = toAppendF.getName();
            } else {
                toAppend = o.toString();
                toAppend = toAppend.replace('\\', '/');
                String[] pathParts2 = toAppend.split("/");
                toAppend = "";
                for (String part : pathParts2) {
                    part = part.trim();
                    part = part.replaceAll("[^a-zA-Z0-9_@.-]", "_");
                    if (part.length() > 64) {
                        part = part.substring(0, 64);
                    }
                    if (part.equals("..") || part.length() == 0) {
                        continue;
                    }
                    if (toAppend.length() > 0) {
                        toAppend += "/";
                    }
                    toAppend += part;
                }
            }
            if (StringUtils.isNotEmpty(toAppend)) {
                path.append('/');
                path.append(toAppend);
            }
        }
        return StringUtils.isEmpty(path.toString()) ? null : new File(path.toString());
    }

    public static <T> T load(Class<T> clazz, Object ... pathParts) {
        File cacheFile = getPath(pathParts);
        if (cacheFile == null || !cacheFile.exists() || !cacheFile.canRead() || !cacheFile.isFile()) {
            return null;
        }
        Gson json = new GsonBuilder().create();
           try (
                    InputStream is = new BufferedInputStream(new FileInputStream(cacheFile), 100000);
                    InputStreamReader reader = new InputStreamReader(is);
            ) {
                T toReturn = json.fromJson(reader, clazz);
                return toReturn;
            } catch (Exception e) {
               LoggerFactory.getLogger(StoreUtils.class)
                       .warn("Failed to load " + clazz.getSimpleName() + " from " + cacheFile.getAbsolutePath() + ": " + e.toString());
                // just go to strava
               return null;
            }
    }

    public static File[] list(Object ... parts) {
        File path = getPath(parts);
        return path.listFiles();
    }
}
