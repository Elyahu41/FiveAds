package com.ej.fiveads.ui.login;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ej.fiveads.R;
import com.ej.fiveads.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private FirebaseAuth mAuth;
    private FirebaseUser mFirebaseUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        //final Button loginButton = binding.signIn;
        final ProgressBar loadingProgressBar = binding.loading;

//        loginViewModel.getLoginFormState().observe(this, loginFormState -> {//this is to display errors
//            if (loginFormState == null) {
//                return;
//            }
//            Objects.requireNonNull(loginButton).setEnabled(loginFormState.isDataValid());
//            if (loginFormState.getUsernameError() != null) {
//                usernameEditText.setError(getString(loginFormState.getUsernameError()));
//            }
//            if (loginFormState.getPasswordError() != null) {
//                passwordEditText.setError(getString(loginFormState.getPasswordError()));
//            }
//        });
//
//        loginViewModel.getLoginResult().observe(this, loginResult -> {
//            if (loginResult == null) {
//                return;
//            }
//            loadingProgressBar.setVisibility(View.GONE);
//            if (loginResult.getError() != null) {
//                showLoginFailed(loginResult.getError());
//            }
//            if (loginResult.getSuccess() != null) {
//                updateUiWithUser(loginResult.getSuccess());
//            }
//            setResult(Activity.RESULT_OK);
//
//            finish();//Complete and destroy login activity once successful
//        });
//
//        TextWatcher afterTextChangedListener = new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                // ignore
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                // ignore
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
//                        passwordEditText.getText().toString());
//            }
//        };
//        usernameEditText.addTextChangedListener(afterTextChangedListener);
//        passwordEditText.addTextChangedListener(afterTextChangedListener);
//
//        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
//            if (actionId == EditorInfo.IME_ACTION_DONE) {
//                signIn(usernameEditText.getText().toString(),
//                        passwordEditText.getText().toString());
//                loginViewModel.login(mFirebaseUser);
//            }
//            return false;
//        });
//
//        Objects.requireNonNull(loginButton).setOnClickListener(v -> {
//            loadingProgressBar.setVisibility(View.VISIBLE);
//            signIn(usernameEditText.getText().toString(),
//                    passwordEditText.getText().toString());
//            loginViewModel.login(mFirebaseUser);
//        });
    }

    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    private void createAccount(String email, String password) {//TODO new activity
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("CreateUserSuccess", "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        setFirebaseUser(user);// Sign in success, update UI with the signed-in user's information
                    } else {
                        Log.w("CreateUserFailure", "createUserWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();// If sign in fails, display a message to the user.
                        setFirebaseUser(null);
                    }
                });
//        while (!firebaseTask.isComplete()) {
//            try {
//                Thread.sleep(0);progressbar?
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("SignInSuccess", "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        setFirebaseUser(user);// Sign in success, update UI with the signed-in user's information
                        loginViewModel.login(user);
                    } else {
                        Log.w("SignInFailure", "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();// If sign in fails, display a message to the user.
                        setFirebaseUser(null);
                        loginViewModel.login(null);
                    }
                });
        //        while (!firebaseTask.isComplete()) {
//            try {
//                Thread.sleep(0);progressbar?
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private void getCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();// Name, email address, and profile photo Url

            boolean emailVerified = user.isEmailVerified();// Check if user's email is verified

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getIdToken() instead.
            String uid = user.getUid();
        }
    }

    public void setFirebaseUser(FirebaseUser firebaseUser) {
        mFirebaseUser = firebaseUser;
    }
}