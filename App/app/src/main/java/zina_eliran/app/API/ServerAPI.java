package zina_eliran.app.API;

import zina_eliran.app.API.BL;
import zina_eliran.app.BusinessEntities.BEResponse;
import zina_eliran.app.BusinessEntities.BEUser;

/**
 * Created by Zina K on 9/10/2016.
 */
public class ServerAPI {

    public static BEResponse registerUser(BEUser user){
        return BL.registerUser(user);
    }

    public static BEResponse verifyUser(BEUser user){
        return BL.verifyUser(user);
    }

    public static BEResponse getUser(String userId){
        return BL.getUser(userId);
    }

    public static BEResponse updateUser(BEUser user){
        return BL.updateUser(user);
    }
}