package com.ej.fiveads.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.ej.fiveads.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private FirebaseAuth mFirebaseAuth;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            mFirebaseAuth = FirebaseAuth.getInstance();

            EditTextPreference displayNamePref = findPreference("displayName");
            if (displayNamePref != null) {
                String currentDisplayName = Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getDisplayName();
                displayNamePref.setSummary(currentDisplayName);
                displayNamePref.setText(currentDisplayName);
                displayNamePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    mFirebaseAuth.getCurrentUser().updateProfile(
                            new UserProfileChangeRequest.Builder()
                            .setDisplayName(newValue.toString())
                            .build());
                    displayNamePref.setSummary(newValue.toString());
                    displayNamePref.setText(newValue.toString());
                    return false;
                });
            }

            Preference emailPref = findPreference("emailAddress");
            if (emailPref != null) {
                String currentEmail = Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getEmail();
                emailPref.setSummary(currentEmail);
            }

            Preference passwordPref = findPreference("password");
            if (passwordPref != null) {
                passwordPref.setOnPreferenceClickListener(v -> {
                    mFirebaseAuth.sendPasswordResetEmail(Objects.requireNonNull(Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getEmail()));
                    new AlertDialog.Builder(requireContext()).setTitle("Email sent!")
                            .setMessage("We have just sent you an email containing steps to reset your password. " + "\n\n" +
                            "Please follow the steps in the email to change your password.")
                            .setPositiveButton("OK",((dialog, which) -> {dialog.dismiss();}))
                            .create()
                            .show();
                    return false;
                });
            }

            Preference signOutPref = findPreference("signOut");
            if (signOutPref != null) {
                signOutPref.setOnPreferenceClickListener(v -> {
                    mFirebaseAuth.signOut();
                    requireActivity().onBackPressed();
                    return false;
                });
            }

            Preference contactUsPref = findPreference("contactUS");
            PackageManager packageManager = requireActivity().getPackageManager();
            if (contactUsPref != null) {
                contactUsPref.setOnPreferenceClickListener(v -> {
                    Intent email = new Intent(Intent.ACTION_SENDTO);
                    email.setData(Uri.parse("mailto:"));
                    email.putExtra(Intent.EXTRA_EMAIL, new String[]{"fiveadshelp@gmail.com"}); //developer's email
                    email.putExtra(Intent.EXTRA_SUBJECT,"Support Ticket"); //Email's Subject
                    email.putExtra(Intent.EXTRA_TEXT,"Dear Five Chats Team,"); //Email's Greeting text

                    if (packageManager.resolveActivity(email,0) != null) { // there is an activity that can handle it
                        startActivity(email);
                    } else {
                        Toast.makeText(getContext(),"No email app...", Toast.LENGTH_SHORT).show();
                    }
                    return false;
                });
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}