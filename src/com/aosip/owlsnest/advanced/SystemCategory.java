/*
 *  Copyright (C) 2015-2018 Android Open Source Illusion Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aosip.owlsnest.advanced;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.aosip.owlsnest.utils.TelephonyUtils;
import com.aosip.owlsnest.utils.Utils;
import com.aosip.support.preference.CustomSeekBarPreference;
import com.aosip.support.preference.SecureSettingSwitchPreference;
import com.aosip.support.preference.SystemSettingSwitchPreference;

import java.util.ArrayList;
import java.util.List;

public class SystemCategory extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {

    private static final String INCALL_VIB_OPTIONS = "incall_vib_options";
    private static final String FLASHLIGHT_ON_CALL = "flashlight_on_call";
    private static final String HEADSET_CONNECT_PLAYER = "headset_connect_player";

    private ListPreference mFlashlightOnCall;
    private ListPreference mLaunchPlayerHeadsetConnection;
    private ListPreference mVelocityFriction;
    private ListPreference mPositionFriction;
    private ListPreference mVelocityAmplitude;
    ContentResolver resolver;

    private static final String SYSUI_ROUNDED_SIZE = "sysui_rounded_size";
    private static final String SYSUI_ROUNDED_CONTENT_PADDING = "sysui_rounded_content_padding";
    private static final String SYSUI_ROUNDED_FWVALS = "sysui_rounded_fwvals";

    private CustomSeekBarPreference mCornerRadius;
    private CustomSeekBarPreference mContentPadding;
    private SecureSettingSwitchPreference mRoundedFwvals;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.OWLSNEST;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system);
        final PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

        mFlashlightOnCall = (ListPreference) findPreference(FLASHLIGHT_ON_CALL);
        Preference FlashOnCall = findPreference("flashlight_on_call");
        if (!Utils.deviceSupportsFlashLight(getActivity())) {
            prefSet.removePreference(FlashOnCall);
        } else {
        int flashlightValue = Settings.System.getInt(getContentResolver(),
                Settings.System.FLASHLIGHT_ON_CALL, 0);
        mFlashlightOnCall.setValue(String.valueOf(flashlightValue));
        mFlashlightOnCall.setSummary(mFlashlightOnCall.getEntry());
        mFlashlightOnCall.setOnPreferenceChangeListener(this);
        }

        PreferenceCategory incallVibCategory = (PreferenceCategory) findPreference(INCALL_VIB_OPTIONS);
        if (!TelephonyUtils.isVoiceCapable(getActivity())) {
			prefSet.removePreference(incallVibCategory);
        }

        mLaunchPlayerHeadsetConnection = (ListPreference) findPreference(HEADSET_CONNECT_PLAYER);
        int mLaunchPlayerHeadsetConnectionValue = Settings.System.getIntForUser(resolver,
                Settings.System.HEADSET_CONNECT_PLAYER, 0, UserHandle.USER_CURRENT);
        mLaunchPlayerHeadsetConnection.setValue(Integer.toString(mLaunchPlayerHeadsetConnectionValue));
        mLaunchPlayerHeadsetConnection.setSummary(mLaunchPlayerHeadsetConnection.getEntry());
        mLaunchPlayerHeadsetConnection.setOnPreferenceChangeListener(this);

        Resources res = null;
        Context ctx = getContext();
        float density = Resources.getSystem().getDisplayMetrics().density;

        try {
            res = ctx.getPackageManager().getResourcesForApplication("com.android.systemui");
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        // Rounded Corner Radius
        mCornerRadius = (CustomSeekBarPreference) findPreference(SYSUI_ROUNDED_SIZE);
        mCornerRadius.setOnPreferenceChangeListener(this);
        int resourceIdRadius = res.getIdentifier("com.android.systemui:dimen/rounded_corner_radius", null, null);
        int cornerRadius = Settings.Secure.getInt(ctx.getContentResolver(), Settings.Secure.SYSUI_ROUNDED_SIZE,
                (int) (res.getDimension(resourceIdRadius) / density));
        mCornerRadius.setValue(cornerRadius / 1);

        // Rounded Content Padding
        mContentPadding = (CustomSeekBarPreference) findPreference(SYSUI_ROUNDED_CONTENT_PADDING);
        mContentPadding.setOnPreferenceChangeListener(this);
        int resourceIdPadding = res.getIdentifier("com.android.systemui:dimen/rounded_corner_content_padding", null,
                null);
        int contentPadding = Settings.Secure.getInt(ctx.getContentResolver(),
                Settings.Secure.SYSUI_ROUNDED_CONTENT_PADDING,
                (int) (res.getDimension(resourceIdPadding) / density));
        mContentPadding.setValue(contentPadding / 1);

        // Rounded use Framework Values
        mRoundedFwvals = (SecureSettingSwitchPreference) findPreference(SYSUI_ROUNDED_FWVALS);
        mRoundedFwvals.setOnPreferenceChangeListener(this);

        float velFriction = Settings.System.getFloatForUser(ctx.getContentResolver(),
                    Settings.System.STABILIZATION_VELOCITY_FRICTION,
                    0.1f,
                    UserHandle.USER_CURRENT);
        mVelocityFriction = (ListPreference) findPreference("stabilization_velocity_friction");
	    mVelocityFriction.setValue(Float.toString(velFriction));
	    mVelocityFriction.setSummary(mVelocityFriction.getEntry());
	    mVelocityFriction.setOnPreferenceChangeListener(this);

	    float posFriction = Settings.System.getFloatForUser(ctx.getContentResolver(),
                    Settings.System.STABILIZATION_POSITION_FRICTION,
                    0.1f,
                    UserHandle.USER_CURRENT);
            mPositionFriction = (ListPreference) findPreference("stabilization_position_friction");
	    mPositionFriction.setValue(Float.toString(posFriction));
	    mPositionFriction.setSummary(mPositionFriction.getEntry());
	    mPositionFriction.setOnPreferenceChangeListener(this);

	    int velAmplitude = Settings.System.getIntForUser(ctx.getContentResolver(),
                    Settings.System.STABILIZATION_VELOCITY_AMPLITUDE,
                    8000,
                    UserHandle.USER_CURRENT);
            mVelocityAmplitude = (ListPreference) findPreference("stabilization_velocity_amplitude");
	    mVelocityAmplitude.setValue(Integer.toString(velAmplitude));
	    mVelocityAmplitude.setSummary(mVelocityAmplitude.getEntry());
	    mVelocityAmplitude.setOnPreferenceChangeListener(this);

    }

    private void restoreCorners() {
        Resources res = null;
        float density = Resources.getSystem().getDisplayMetrics().density;

        try {
            res = getContext().getPackageManager().getResourcesForApplication("com.android.systemui");
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        int resourceIdRadius = res.getIdentifier("com.android.systemui:dimen/rounded_corner_radius", null, null);
        int resourceIdPadding = res.getIdentifier("com.android.systemui:dimen/rounded_corner_content_padding", null,
                null);
        mCornerRadius.setValue((int) (res.getDimension(resourceIdRadius) / density));
        mContentPadding.setValue((int) (res.getDimension(resourceIdPadding) / density));

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();

        if (preference == mFlashlightOnCall) {
            int flashlightValue = Integer.parseInt(((String) newValue).toString());
            Settings.System.putInt(getContentResolver(),
                    Settings.System.FLASHLIGHT_ON_CALL, flashlightValue);
            mFlashlightOnCall.setValue(String.valueOf(flashlightValue));
            mFlashlightOnCall.setSummary(mFlashlightOnCall.getEntry());
            return true;
        }
        else if (preference == mLaunchPlayerHeadsetConnection) {
            int mLaunchPlayerHeadsetConnectionValue = Integer.valueOf((String) newValue);
            int index = mLaunchPlayerHeadsetConnection.findIndexOfValue((String) newValue);
            mLaunchPlayerHeadsetConnection.setSummary(
                    mLaunchPlayerHeadsetConnection.getEntries()[index]);
            Settings.System.putIntForUser(resolver, Settings.System.HEADSET_CONNECT_PLAYER,
                    mLaunchPlayerHeadsetConnectionValue, UserHandle.USER_CURRENT);
            return true;
        }
        if (preference == mCornerRadius) {
            Settings.Secure.putInt(getContext().getContentResolver(), Settings.Secure.SYSUI_ROUNDED_SIZE,
                    ((int) newValue) * 1);
        } else if (preference == mContentPadding) {
            Settings.Secure.putInt(getContext().getContentResolver(), Settings.Secure.SYSUI_ROUNDED_CONTENT_PADDING,
                    ((int) newValue) * 1);
        } else if (preference == mRoundedFwvals) {
            restoreCorners();
        } else if (preference == mVelocityFriction) {
	        String value = (String) newValue;
	        Settings.System.putFloatForUser(getContext().getContentResolver(),
	                 Settings.System.STABILIZATION_VELOCITY_FRICTION, Float.valueOf(value), UserHandle.USER_CURRENT);
	        int valueIndex = mVelocityFriction.findIndexOfValue(value);
	        mVelocityFriction.setSummary(mVelocityFriction.getEntries()[valueIndex]);
        } else if (preference == mPositionFriction) {
	        String value = (String) newValue;
	        Settings.System.putFloatForUser(getContext().getContentResolver(),
	                 Settings.System.STABILIZATION_POSITION_FRICTION, Float.valueOf(value), UserHandle.USER_CURRENT);
	        int valueIndex = mPositionFriction.findIndexOfValue(value);
	        mPositionFriction.setSummary(mPositionFriction.getEntries()[valueIndex]);
        } else if (preference == mVelocityAmplitude) {
	        String value = (String) newValue;
	        Settings.System.putFloatForUser(getContext().getContentResolver(),
	                 Settings.System.STABILIZATION_VELOCITY_AMPLITUDE, Float.valueOf(value), UserHandle.USER_CURRENT);
	        int valueIndex = mVelocityAmplitude.findIndexOfValue(value);
	        mVelocityAmplitude.setSummary(mVelocityAmplitude.getEntries()[valueIndex]);
       }
    	return true;
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                 @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();
                     final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.system;
                    result.add(sir);
                    return result;
                }
                 @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
    };
}

