package ca.scibrazeau.gpx_tools_v3.model;

import java.util.Arrays;
import java.util.List;

/**
 * Created by pssemr on 2016-11-26.
 */
public class ActivityDurationsAndPowers {

    private final String userId;
    private final long activityId;
    private final String startDateTime;
    private final List<Float> durationSeconds;
    private final List<Float> powerData;
    private int[] powerDataIntArray;

    public ActivityDurationsAndPowers(String userId, long activityId, String s, List<Float> durationSeconds, List<Float> powerData) {
        this.userId = userId;
        this.activityId = activityId;
        this.startDateTime = s;
        this.durationSeconds = durationSeconds;
        this.powerData = powerData;
    }


    public String getUserId() {
        return userId;
    }

    public long getActivityId() {
        return activityId;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public List<Float> getDurationSeconds() {
        return durationSeconds;
    }

    public List<Float> getPowerData() {
        return powerData;
    }

    public Integer[] getPowerDataIntArray() {
        return powerData.stream().map(x -> x == null ? null : ((Float)x).intValue()).toArray(ln -> new Integer[ln]);
    }
}
