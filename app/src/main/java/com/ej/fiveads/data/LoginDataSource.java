package com.ej.fiveads.data;

import com.ej.fiveads.data.model.LoggedInUser;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    public Result<LoggedInUser> login(FirebaseUser firebaseUser) {
        try {
            LoggedInUser user = new LoggedInUser(firebaseUser.getUid(), firebaseUser.getDisplayName());
            return new Result.Success<>(user);
        } catch (Exception e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }
}