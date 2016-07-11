package com.example.b0915218.presentationcontrolapp;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class InterfaceController {
    private Activity activity;
    protected TextView instructionsText;
    protected TextView statusText;
    protected Button startScanBtn;
    protected Button goToSettings;

    public InterfaceController(Activity act) {
        activity = act;
        instructionsText = (TextView) activity.findViewById(R.id.instructionsText);
        statusText = (TextView) activity.findViewById(R.id.statusText);
        startScanBtn = (Button) activity.findViewById(R.id.btnScanBle);
        goToSettings = (Button) activity.findViewById(R.id.settingsBtn);
        goToSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(activity, SettingsActivity.class);
                activity.startActivity(i);
            }
        });
    }

    public void setLookingForDevice() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                instructionsText.setVisibility(View.INVISIBLE);
                startScanBtn.setVisibility(View.INVISIBLE);
                statusText.setVisibility(View.VISIBLE);
                statusText.setText(R.string.looking_for_device);
            }
        });
    }

    public void setDeviceNotFound() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                instructionsText.setVisibility(View.INVISIBLE);
                startScanBtn.setVisibility(View.VISIBLE);
                statusText.setVisibility(View.VISIBLE);
                statusText.setText(R.string.device_not_found);
            }
        });
    }


    public void setDeviceConnected() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                instructionsText.setVisibility(View.INVISIBLE);
                startScanBtn.setVisibility(View.INVISIBLE);
                statusText.setVisibility(View.VISIBLE);
                statusText.setText(R.string.action_neutral);
            }
        });
    }

    public void setDeviceDisconnected() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                instructionsText.setVisibility(View.INVISIBLE);
                startScanBtn.setVisibility(View.VISIBLE);
                statusText.setVisibility(View.VISIBLE);
                statusText.setText(R.string.device_disconnected);
            }
        });
    }

    public void setSwipeRight() {
        String st = activity.getResources().getString(R.string.action_right);
        setActionText(st);
    }

    public void setSwipeLeft() {
        String st = activity.getResources().getString(R.string.action_Left);
        setActionText(st);
    }

    public void setIntentionLocked() {
        String st = activity.getResources().getString(R.string.action_intention_locked);
        setActionText(st);
    }

    public void setIntentionUnlocked() {
        String st = activity.getResources().getString(R.string.action_intention_unlocked);
        setActionText(st);
    }

    public void setInstructionsText(final String t) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusText.setVisibility(View.INVISIBLE);
                startScanBtn.setVisibility(View.INVISIBLE);
                instructionsText.setVisibility(View.VISIBLE);
                instructionsText.setText(t);
            }
        });
    }

    private void setActionText(final String t) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                instructionsText.setVisibility(View.INVISIBLE);
                statusText.setVisibility(View.VISIBLE);
                statusText.setText(t);
            }
        });
    }

}
