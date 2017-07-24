package ca.scibrazeau.gpx_tools_v3.api;

import ca.scibrazeau.gpx_tools.strava.ActivityStore;
import ca.scibrazeau.gpx_tools_v3.model.ActivityDurationsAndPowers;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/stravaActivity")
public class ActivityService {

    @RequestMapping("/{userId}/{activityId}")
    public Integer[] getActivityPowers(@PathVariable(name="userId") String userId, @PathVariable(name = "activityId") String activityId) {
        LoggerFactory.getLogger(ActivityService.class).info("Retrieving activity {} for {}", activityId, userId);
        ActivityDurationsAndPowers result = ActivityStore.access().getUserActivityPowerAndDurations(userId, Integer.parseInt(activityId));
        return result.getPowerDataIntArray();
    }
}
