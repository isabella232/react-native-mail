package com.chirag.RNMail;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.Html;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.Callback;

import java.util.List;
import java.io.File;

import android.app.Activity;
import android.support.v4.content.FileProvider;
import android.os.Build;

/**
 * NativeModule that allows JS to open emails sending apps chooser.
 */
public class RNMailModule extends ReactContextBaseJavaModule {

    ReactApplicationContext reactContext;

    public RNMailModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNMail";
    }

    /**
     * Converts a ReadableArray to a String array
     *
     * @param r the ReadableArray instance to convert
     *
     * @return array of strings
     */
    private String[] readableArrayToStringArray(ReadableArray r) {
        int length = r.size();
        String[] strArray = new String[length];

        for (int keyIndex = 0; keyIndex < length; keyIndex++) {
            strArray[keyIndex] = r.getString(keyIndex);
        }

        return strArray;
    }

    private Intent getIntent() {
        Intent i;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // https://stackoverflow.com/a/42856167
            Intent emailSelectorIntent = new Intent(Intent.ACTION_SENDTO);
            emailSelectorIntent.setData(Uri.parse("mailto:"));

            i = new Intent(Intent.ACTION_SEND);
            i.setData(Uri.parse("mailto:"));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            i.setSelector( emailSelectorIntent );
        } else {
            i = new Intent(Intent.ACTION_SEND);
            i.setPackage("com.google.android.gm");
            i.setType("message/rfc822");
            //i.setType("application/json");
        }
        return i;
    }

    private Uri getFileUri(String path) {
        File file = new File(path);
        file.setReadable(true, false);
        final String providerName = reactContext.getPackageName() + ".provider";
        final Activity activity = getCurrentActivity();
        return FileProvider.getUriForFile(activity, providerName, file);
    }

    @ReactMethod
    public void mail(ReadableMap options, Callback callback) {
        Intent i = getIntent();

        if (options.hasKey("subject") && !options.isNull("subject")) {
            i.putExtra(Intent.EXTRA_SUBJECT, options.getString("subject"));
        }

        if (options.hasKey("body") && !options.isNull("body")) {
            String body = options.getString("body");
            if (options.hasKey("isHTML") && options.getBoolean("isHTML")) {
                i.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(body));
            } else {
                i.putExtra(Intent.EXTRA_TEXT, body);
            }
        }

        if (options.hasKey("recipients") && !options.isNull("recipients")) {
            ReadableArray recipients = options.getArray("recipients");
            i.putExtra(Intent.EXTRA_EMAIL, readableArrayToStringArray(recipients));
        }

        if (options.hasKey("ccRecipients") && !options.isNull("ccRecipients")) {
            ReadableArray ccRecipients = options.getArray("ccRecipients");
            i.putExtra(Intent.EXTRA_CC, readableArrayToStringArray(ccRecipients));
        }

        if (options.hasKey("bccRecipients") && !options.isNull("bccRecipients")) {
            ReadableArray bccRecipients = options.getArray("bccRecipients");
            i.putExtra(Intent.EXTRA_BCC, readableArrayToStringArray(bccRecipients));
        }

        if (options.hasKey("attachment") && !options.isNull("attachment")) {
            ReadableMap attachment = options.getMap("attachment");
            if (attachment.hasKey("path") && !attachment.isNull("path")) {
                String path = attachment.getString("path");
                final Uri p = getFileUri(path);
                i.putExtra(Intent.EXTRA_STREAM, p);
            }
        }

        PackageManager manager = reactContext.getPackageManager();
        List<ResolveInfo> list = manager.queryIntentActivities(i, 0);

        if (list == null || list.size() == 0) {
            callback.invoke("not_available");
            return;
        }

        if (list.size() == 1) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                reactContext.startActivity(i);
            } catch (Exception ex) {
                callback.invoke("error");
            }
        } else {
            Intent chooser = Intent.createChooser(i, "Send Mail");
            chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                reactContext.startActivity(chooser);
            } catch (Exception ex) {
                callback.invoke("error");
            }
        }
    }
}
