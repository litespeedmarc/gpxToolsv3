package ca.scibrazeau.gpx_tools_v3.api;


import ca.scibrazeau.gpx_tools.strava.ActivityStore;
import ca.scibrazeau.gpx_tools_v3.model.User;
import ca.scibrazeau.gpx_tools.strava.StoreUtils;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;


@RestController()
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/user")
public class UserService {


    @RequestMapping("/{userId}")
    public String getInfo(
            @PathVariable("userId") String userId
    ) {
        User user = fetchUser(userId);
        if (user == null) {
            return null;
        }

        JSONObject obj = new JSONObject();
        obj.put("userId", user.getUserId());
        ActivityStore.access().getUserInfo(user);
        obj.put("athleteId", user.getAthleteId());
        obj.put("isRegistered", user.isRegistered());
        obj.put("description", user.getDescription());
        return obj.toString();
    }

    public User fetchUser(String userId) {
        EmailValidator emailValidator = EmailValidator.getInstance(true);
        Preconditions.checkArgument(emailValidator.isValid(userId) || emailValidator.isValid(userId + "@scibrazeau.ca"), "Invalid userId: " + userId);
        User user = StoreUtils.load(User.class, "users", userId, "info");
        if (user == null) {
            user = new User();
            user.setUserId(userId);
        } else {
            if (!StringUtils.isEmpty(user.getStravaKey())) {
                // test the api & set registered
                try {
                    ActivityStore.access().getUserInfo(user);
                } catch (Exception e) {
                    user.setDescription("");
                }
            }
        }
        return user;
    }

    @RequestMapping(value = "/{userId}", method = RequestMethod.PUT)
    public User update(@PathVariable(name = "userId") String userId, @RequestParam(name="code") String stravaCode) {
        User user = StoreUtils.load(User.class, "users", userId, "info");
        if (user == null) {
            user = new User();
            user.setUserId(userId);
        }

        user.setStravaKey(stravaCode);
        user.setConfirmKey(null);

        update(user);
        return user;
    }

    public String update(User user) {
        StoreUtils.save(user, "users", user.getUserId(), "info");
        return "OK";
    }

}
