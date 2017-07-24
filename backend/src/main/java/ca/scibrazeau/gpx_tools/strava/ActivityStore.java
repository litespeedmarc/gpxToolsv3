package ca.scibrazeau.gpx_tools.strava;

import ca.scibrazeau.gpx_tools_v3.api.UserService;
import ca.scibrazeau.gpx_tools_v3.model.User;
import ca.scibrazeau.gpx_tools_v3.model.ActivityDurationsAndPowers;
import ca.scibrazeau.gpx_tools_v3.model.User;
import javastrava.api.v3.auth.AuthorisationService;
import javastrava.api.v3.auth.impl.retrofit.AuthorisationServiceImpl;
import javastrava.api.v3.auth.model.Token;
import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.model.StravaAthlete;
import javastrava.api.v3.model.StravaStream;
import javastrava.api.v3.model.reference.StravaStreamResolutionType;
import javastrava.api.v3.model.reference.StravaStreamSeriesDownsamplingType;
import javastrava.api.v3.model.reference.StravaStreamType;
import javastrava.api.v3.service.Strava;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by pssemr on 2016-11-26.
 */
public class ActivityStore {

    private static final String kStravaPrivateKey = "93ad62f7e22f49f119335e8c39a770c66dec41cd";
    private static final int kStravaClientId = 4782;

    private static volatile ActivityStore mSingleton;

    public static ActivityStore access() {
        if (mSingleton == null) {
            mSingleton = new ActivityStore();
        }
        return mSingleton;
    }



    public ActivityDurationsAndPowers getUserActivityPowerAndDurations(String userId, long activityId) {
        ActivityDurationsAndPowers cachedValue = StoreUtils.load(ActivityDurationsAndPowers.class, StoreUtils.kCacheName, activityId);
        if (cachedValue != null) {
            return cachedValue;
        }

        AuthorisationService service = new AuthorisationServiceImpl();
        User user = new UserService().fetchUser(userId);
        if (user == null || !user.isRegistered()) {
            throw new RuntimeException("User " + userId + " is not registered");
        }
        Token token = service.tokenExchange(kStravaClientId, kStravaPrivateKey, user.getStravaKey());
        Strava strava = new Strava(token);
        StravaActivity activity = strava.getActivity((int) activityId);
        List<StravaStream> powerAndTimeStream = strava.getActivityStreams(
                (int) activityId,
                StravaStreamResolutionType.HIGH,
                StravaStreamSeriesDownsamplingType.TIME,
                StravaStreamType.TIME,
                StravaStreamType.POWER);

        if (powerAndTimeStream.size() < 2) {
            throw new RuntimeException("Activity does not have any power data");
        }
        List<Float> durationSeconds = powerAndTimeStream.get(0).getData();
        List<Float> powerData = powerAndTimeStream.get(1).getData();

        ActivityDurationsAndPowers toReturn = new ActivityDurationsAndPowers(
                userId,
                activityId,
                activity.getStartDate().toLocalDateTime().toString(),
                durationSeconds,
                powerData
        );

        StoreUtils.save(toReturn, StoreUtils.kCacheName, activityId);

        return toReturn;
    }


    public void getUserInfo(User user) {
        try {
            AuthorisationService service = new AuthorisationServiceImpl();
            Token token = service.tokenExchange(kStravaClientId, kStravaPrivateKey, user.getStravaKey());
            Strava strava = new Strava(token);
            StravaAthlete athlete = strava.getAuthenticatedAthlete();
            user.setDescription(athlete.getFirstname() + " " + athlete.getLastname() + " (" + athlete.getId() + ")");
        } catch (Throwable e) {
            LoggerFactory.getLogger(ActivityStore.class)
                    .warn("Failed to retrieve user id, {}, athlete id {} (strava key {}). Strava error is: {}.  Assuming user is no longer registered",
                            user.getUserId(),
                            user.getAthleteId(),
                            user.getStravaKey(),
                            e.toString()
                    );
        }

    }
}
