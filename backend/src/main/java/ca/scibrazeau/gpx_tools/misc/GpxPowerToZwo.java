package ca.scibrazeau.gpx_tools.misc;

import com.hs.gpxparser.GPXParser;
import com.hs.gpxparser.PowerParser;
import com.hs.gpxparser.modal.GPX;
import com.hs.gpxparser.modal.Waypoint;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by pssemr on 2018-02-03.
 *
 *
 * <workout_file>
 <author>M.Brazeau (B)</author>
 <name>250.260 for 60</name>
 <description>250.260 for 60</description>
 <sportType>bike</sportType>
 <tags>
 <tag name="RECOVERY"/>
 </tags>
 <workout>
 <SteadyState Duration="60" Power="0.15151515"/>
 <SteadyState Duration="60" Power="0.3030303"/>
 <SteadyState Duration="60" Power="0.45454545"/>
 <SteadyState Duration="60" Power="0.60606061"/>
 <SteadyState Duration="60" Power="0.66666667"/>
 <SteadyState Duration="300" Power="0.72727273"/>
 <SteadyState Duration="300" Power="0.75757576"/>
 <SteadyState Duration="900" Power="0.78787879"/>
 <SteadyState Duration="300" Power="0.81818182"/>
 <SteadyState Duration="300" Power="0.72727273"/>
 <SteadyState Duration="300" Power="0.75757576"/>
 <SteadyState Duration="900" Power="0.78787879"/>
 <SteadyState Duration="300" Power="0.81818182"/>
 <SteadyState Duration="60" Power="0.66666667"/>
 <SteadyState Duration="60" Power="0.60606061"/>
 <SteadyState Duration="60" Power="0.45454545"/>
 <SteadyState Duration="60" Power="0.3030303"/>

 </workout>
 </workout_file>

 */
public class GpxPowerToZwo {
    private final GPX mGpx;
    private final String mName;

    public GpxPowerToZwo(String name, GPX gpx) {
        mName = FilenameUtils.getBaseName(name);
        mGpx = gpx;
    }



    public static void main(String[] arg) {
        File gpxFile = new File(arg[0]);
        File outDir = gpxFile.getParentFile();
        File zwoFile = new File(outDir,FilenameUtils.getBaseName(arg[0]) + ".zwo");
        try (
                FileInputStream fis = new FileInputStream(gpxFile);
                FileOutputStream fos = new FileOutputStream(zwoFile);
        ) {
            GPX gpx = new GPXParser().parseGPX(fis);
            new GpxPowerToZwo(gpxFile.getName(), gpx).writeToZwo(fos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void writeToZwo(FileOutputStream fos) {
        writeToZwo(new PrintStream(fos));
    }
    private void writeToZwo(PrintStream print) {
        print.println("<workout_file>");
        print.println("<author>Marc Brazeau</author>");
        String name =StringEscapeUtils.escapeXml11(mName);
        print.println("<name>" +  name + "</name>");
        print.println("<description>" +  name + "</description>");
        print.println("<sportType>" +  name + "</sportType>");
        print.println("<tags><tag name=\"RECOVERY\"/></tags>");
        print.println("<workout>");

        Waypoint[] lastPoint = new Waypoint[] {null};

        AtomicInteger totalPower = new AtomicInteger(0) ;

        List<Waypoint> points = new ArrayList<>();



        mGpx.getTracks().forEach(track -> {
            track.getTrackSegments().forEach( seg -> {
               seg.getWaypoints().forEach( point -> {
                   points.add(point);
               });
            });
        });

        Segment segment = new Segment();

        for (int i = 1; i < points.size(); i++) {
            Waypoint point = points.get(i);
            long duration = (point.getTime().getTime() - points.get(i - 1).getTime().getTime()) / 1000;
            if (duration < 1) {
                continue;
            }
            PowerParser.Extension extension = (PowerParser.Extension) point.getExtensionData("Power");
            int power = extension.Power == null ? 50 : Math.max(extension.Power, 50);
            if (segment.getDuration() < 30) {
                segment.addPower(duration, power);
                continue;
            }
            double averageNext15 = averageNext(points, i, 15);
            double segAverage = segment.getAverage();
            if (shouldChange(segAverage, averageNext15)) {
                double pow = 330.0;
                pow = segAverage / pow;
                print.println("<SteadyState Duration=\"" + segment.getDuration() + "\" Power=\"" + pow + "\"/><!-- " + segAverage + "-->");
                segment = new Segment();
            } else {
                segment.addPower(duration, power);
            }

        }
        print.println("</workout>");
        print.println("</workout_file>");

    }

    private boolean shouldChange(double segAverage, double averageNext15) {
        double absDiff = Math.abs(averageNext15 - segAverage);
        if (absDiff > 100) {
            return true;
        }
        if (segAverage > 270) {
            return absDiff > 10;
        }
        if (segAverage > 200) {
            return absDiff > 20;
        } else {
            return absDiff > 50;
        }
    }

    private int averageNext(List<Waypoint> points, int startAt, int lnWanted) {
        Segment segment = new Segment();
        for (int i = startAt + 1; i < points.size(); i++) {
            Waypoint point = points.get(i);
            long duration = (point.getTime().getTime() - points.get(i - 1).getTime().getTime()) / 1000;
            if (duration < 1) {
                continue;
            }
            PowerParser.Extension extension = (PowerParser.Extension) point.getExtensionData("Power");
            int power = extension.Power == null ? 50 : Math.max(extension.Power, 50);
            if (segment.getDuration() < lnWanted) {
                segment.addPower(duration, power);
                continue;
            }
        }
        if (segment.mDuration == 0) {
            return 50;
        } else {
            return segment.getAverage();
        }
    }


    private class Segment {
        private int mDuration;
        private int mTotalPower;

        public int getDuration() {
            return mDuration;
        }

        public void addPower(long duration, int power) {
            mDuration += duration;
            mTotalPower += power * duration;
        }

        public int getAverage() {
            return mTotalPower / mDuration;
        }
    }
}
