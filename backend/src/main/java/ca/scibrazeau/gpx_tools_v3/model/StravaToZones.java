package ca.scibrazeau.gpx_tools_v3.model;

/**
 * Created by pssemr on 2016-11-27.
 */
public class StravaToZones {
    private String userId;
    private long activityId;
    private int targetTimeInZone;
    private int targetZone;
    private String zones;

    public StravaToZones() {};
    public StravaToZones(String lastUserId) {
        userId = lastUserId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getActivityId() {
        return activityId;
    }

    public void setActivityId(long activityId) {
        this.activityId = activityId;
    }

    public int getTargetTimeInZone() {
        return targetTimeInZone;
    }

    public void setTargetTimeInZone(int targetTimeInZone) {
        this.targetTimeInZone = targetTimeInZone;
    }

    public int getTargetZone() {
        return targetZone;
    }

    public void setTargetZone(int targetZone) {
        this.targetZone = targetZone;
    }

    public String getZones() {
        return zones;
    }

    public void setZones(String zones) {
        this.zones = zones;
    }
}
