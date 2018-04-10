package com.cloudnine.emailclerk;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.gmail.*;
import com.google.api.services.gmail.model.*;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    static final int PASS_GMAIL_OBJECT = 1;
    static final int RESULT_OKAY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start Gmail Authentication Activity
        Intent i= new Intent(MainActivity.this, GmailAuth.class);
        this.startActivityForResult(i, PASS_GMAIL_OBJECT);

    }

    // Get mService Gmail object from Gmail Authentication Activity end
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PASS_GMAIL_OBJECT) {
            if (resultCode == RESULT_OK) {
                //GmailAuth.SendSerializedService sss = (GmailAuth.SendSerializedService) getIntent().getSerializableExtra("serialize_data");
                //StateController stateController = new StateController(sss.getmService());
                com.google.api.services.gmail.Gmail mService = GmailAuth.mService;
                StateController stateController = new StateController(this, MainActivity.this, MainActivity.this, mService);
            }
        }
    }
}