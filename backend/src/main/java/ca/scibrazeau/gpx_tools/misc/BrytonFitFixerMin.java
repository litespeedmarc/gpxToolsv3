package ca.scibrazeau.gpx_tools.misc;

import com.garmin.fit.*;
import com.garmin.fit.LocalDateTime;
import com.hs.gpxparser.GPXWriter;
import com.hs.gpxparser.extension.IExtensionParser;
import com.hs.gpxparser.modal.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URL;
import java.text.ParseException;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BrytonFitFixerMin implements MesgListener {

    private Waypoint mCurrentWayPoint;
    private Map<String, Cumulation> mCumulations = new HashMap<>();
    private Double mPosLast;

    private static final String kGoogleMapsApiKey = "AIzaSyAVTeFpeW-4ngyoGdv41M10TQK8uJ6zrAo";

    private static class CumulationValue {
        private long mValue;
        private long mDuration;
    }

    private static class Cumulation {
        private String mName;
        private long   mLastTimeStamp;
        private List<CumulationValue> mCumulations = new ArrayList<>();
        public double getAverage() {
            double total = mCumulations.stream().mapToLong( v -> v.mDuration ).sum();
            return mCumulations.stream().mapToDouble( v -> (double)v.mValue * (double)v.mDuration / total).sum();
        }

        public void cumulate(Long longValue, long lastTimeStamp) {
            long duration = lastTimeStamp - mLastTimeStamp;
            if (duration == 0) {
                return;
            }
            CumulationValue cv = new CumulationValue();
            cv.mDuration = duration;
            cv.mValue = longValue;
            mCumulations.add(cv);
            mLastTimeStamp = lastTimeStamp;
        }
    }

    private final GPX mGpx;
    private final Track mTrack;
    private final TrackSegment mTrackSegment;
    private Set<Integer> mTypes = new HashSet<>();

    private static Set<String> mIgnoredFields = new HashSet<>();

    private long mLastLatLongTs;
    private long mLastTimeStamp;

    public BrytonFitFixerMin() {
        mIgnoredFields.add("serial_number");
        mIgnoredFields.add("time_created");
        mIgnoredFields.add("manufacturer");
        mIgnoredFields.add("product");
        mIgnoredFields.add("sport");
        mIgnoredFields.add("sub_sport");
        mGpx = new GPX();
        mTrack = new Track();
        mTrackSegment = new TrackSegment();
        mTrack.addTrackSegment(mTrackSegment);
        mGpx.addTrack(mTrack);
        mGpx.setMetadata(new Metadata());
    }


    public static void main(String[] args) throws Exception {
        String outfile = FilenameUtils.removeExtension(args[0]) + ".gpx";
        BrytonFitFixerMin fixer = new BrytonFitFixerMin();
        try (
                FileInputStream fis = new FileInputStream(args[0]);
        ) {
            final MesgBroadcaster broadcaster=new MesgBroadcaster(new Decode());
            broadcaster.addListener(fixer);
            broadcaster.run(fis);
        }

        try (
                FileOutputStream fos = new FileOutputStream(outfile);
        ) {
            fixer.write(fos);
        }


    }

    private void write(OutputStream fos) throws TransformerException, ParserConfigurationException, IOException, ParseException {
        // get timezone offset
        List<Waypoint> allWayPoints = mGpx.getTracks().iterator().next().getTrackSegments().iterator().next().getWaypoints();

        Waypoint firstWp = allWayPoints.iterator().next();
        URL url = new URL("http://api.geonames.org/timezoneJSON?lat=" + firstWp.getLatitude() + "&lng=" + firstWp.getLongitude() + "&username=litespeedmarc");
        String json;
        try (
           InputStream urlStream = url.openStream();
        ) {
           json = IOUtils.toString(urlStream);
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode resp = mapper.readTree(json);
        int dteOffset = Integer.parseInt(resp.get("dstOffset").asText());

        // offset all times by this.
        for (Waypoint allWayPoint : allWayPoints) {
            allWayPoint.setTime(addHours(dteOffset, allWayPoint.getTime()));
        }
        mGpx.getMetadata().setTime(firstWp.getTime());


        GPXWriter writer = new GPXWriter();
        AtomicInteger at = new AtomicInteger(0);
        writer.addExtensionParser(new IExtensionParser() {
            @Override
            public String getId() {
                return null;
            }

            @Override
            public Object parseExtensions(Node node) {
                return null;
            }

            @Override
            public void writeExtensions(Node node, Document document) {
                int i = at.getAndIncrement();
                HashMap<String, Object> extensions = allWayPoints.get(i).getExtensionData();
                if (extensions == null || extensions.size() == 0) {
                    return;
                }
                for (String key : extensions.keySet()) {
                    Element childNode = document.createElement(key);
                    childNode.setTextContent(extensions.get(key).toString());
                    node.appendChild(childNode);
                }

            }
        });
        writer.writeGPX(mGpx, fos);
    }

    private Date addHours(int dteOffset, Date time) {
        return new Date(time.getTime() - dteOffset * 60 * 60 * 1000);
    }

    @Override
    public void onMesg(Mesg mesg) {
        mesg.getFields().forEach(this::onMesg);
    }

    private void onMesg(Field f) {
        switch (f.getName()) {
            case "timestamp":
                if (mLastLatLongTs == 0) {
                    mLastLatLongTs = f.getLongValue();
                }
                mLastTimeStamp = f.getLongValue();
                break;
            case "power":
            case "temperature":
            case "cadence":
                cumulate(f);
                break;
            case "position_lat":
                mPosLast = f.getDoubleValue();
                break;
            case "position_long":
                mCurrentWayPoint = new Waypoint(toRadians(mPosLast), toRadians(f.getDoubleValue()));
                mCurrentWayPoint.setTime(toUtcDate(mLastTimeStamp));
                addExtensionData();
                mTrackSegment.addWaypoint(mCurrentWayPoint);
                break;
            case "enhanced_altitude":
            case "altitude":
                mCurrentWayPoint.setElevation(f.getDoubleValue());
                break;
            case "speed":
            case "distance":
            case "unknown":
            case "enhanced_speed":
                // ignore
                break;
        }
    }

    private double toRadians(double doubleValue) {
        // https://www.gps-forums.com/threads/accuracy-of-converting-semicircles-to-degrees.31488/
        return doubleValue * (180.0d / Math.pow(2, 31));
    }

    private Date toUtcDate(long mLastTimeStamp) {
        Date ldt = new LocalDateTime(mLastTimeStamp).getDate();
        ZonedDateTime zdt = ZonedDateTime.ofInstant(ldt.toInstant(), ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    private void addExtensionData() {
        if (mCurrentWayPoint.getExtensionData() == null) {
            mCurrentWayPoint.setExtensionData(new HashMap<>());
        }
        mCumulations.forEach( this::addExtensionData );
        mCumulations.clear();
    }

    private void addExtensionData(String field, Cumulation cumulation) {
        if (field == "temperature") {
            field = "temp";
        }
        double average = cumulation.getAverage();
        mCurrentWayPoint.addExtensionData(field, average);
    }

    private void cumulate(Field f) {
        Cumulation cumulation = mCumulations.get(f.getName());
        if (cumulation == null) {
            cumulation = new Cumulation();
            mCumulations.put(f.getName(), cumulation);
            cumulation.mLastTimeStamp = mLastLatLongTs;
        }
        cumulation.cumulate(f.getLongValue(), mLastTimeStamp);
    }
}
