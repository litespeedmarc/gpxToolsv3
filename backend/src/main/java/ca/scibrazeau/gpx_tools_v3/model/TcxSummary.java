package ca.scibrazeau.gpx_tools_v3.model;

import java.util.Arrays;

public class TcxSummary {
    String dateTime;
    int  durationSeconds;
    int  rideAveragePowerWatts;

    Interval[] intervals;

    public static class Interval {
        int rest;
        int duration;
        int watts;

        public int getRest() {
            return rest;
        }

        public void setRest(int rest) {
            this.rest = rest;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public int getWatts() {
            return watts;
        }

        public void setWatts(int watts) {
            this.watts = watts;
        }
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getRideAveragePowerWatts() {
        return rideAveragePowerWatts;
    }

    public void setRideAveragePowerWatts(int rideAveragePowerWatts) {
        this.rideAveragePowerWatts = rideAveragePowerWatts;
    }

    public int getNumberOfIntervals() {
        return intervals.length;
    }

    public int getAverageIntervalSeconds() {
        return (int) Arrays.stream(intervals).mapToDouble(i -> i.getDuration()).average().orElse(0);
    }


    public int getAverageRestSeconds() {
        return (int) Arrays.stream(intervals).skip(1).mapToDouble( i -> i.getRest()).average().orElse(0);
    }

    public Interval[] getIntervals() {
        return intervals;
    }

    public void setIntervals(Interval[] intervals) {
        this.intervals = intervals;
    }

    public int getAverageIntervalPowerWatts() {
        double totalTime = Arrays.stream(intervals).mapToDouble( i -> i.getDuration()).sum();
        return (int) Arrays.stream(intervals).mapToDouble(i -> i.getWatts() * i.getDuration() / totalTime).sum();
    }


}
