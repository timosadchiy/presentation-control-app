package com.example.b0915218.presentationcontrolapp;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref);

            ListPreference listPreference = (ListPreference) findPreference("band_location");
            if(listPreference.getValue()==null) {
                // to ensure we don't get a null value
                // set first value by default
                listPreference.setValueIndex(0);
            }
            listPreference.setSummary(listPreference.getEntry().toString());
            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ListPreference listPreference = (ListPreference) preference;
                    listPreference.setValue(newValue.toString());
                    preference.setSummary(listPreference.getEntry().toString());
                    return true;
                }
            });
        }
    }

}
