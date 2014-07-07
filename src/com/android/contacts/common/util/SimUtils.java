/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.contacts.common.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.CallUtil;

public class SimUtils {
    private static final String TAG = "SimUtils";
    private static final boolean DEBUG = false;

    private static final Uri SIM_CONTENT_URI = Uri.parse("content://icc/adn");
    private static final Uri USIM_CONTENT_URI = Uri.parse("content://usim/adn");
    private static final Uri SIM_2_CONTENT_URI = Uri.parse("content://icc2/adn");
    private static final Uri USIM_2_CONTENT_URI = Uri.parse("content://usim2/adn");

    private static final String PROPERTY_SIM_CAPACITY = "gsm.sim.adnCapacity";
    private static final String PROPERTY_SIM_FREE_ENTRIES_NUMBER = "gsm.sim.freeAdn";
    private static final String PROPERTY_SIM_2_CAPACITY = "gsm.sim2.adnCapacity";
    private static final String PROPERTY_SIM_2_FREE_ENTRIES_NUMBER = "gsm.sim2.freeAdn";

    private static final String PROPERTY_SIM_MAX_NAME_LENGTH = "gsm.sim.maxNameLength";
    private static final String PROPERTY_SIM_MAX_NUMBER_LENGTH = "gsm.sim.maxNumberLength";
    private static final String PROPERTY_SIM_MAX_EMAIL_LENGTH = "gsm.sim.maxEmailLength";
    private static final String PROPERTY_SIM_2_MAX_NAME_LENGTH = "gsm.sim2.maxNameLength";
    private static final String PROPERTY_SIM_2_MAX_NUMBER_LENGTH = "gsm.sim2.maxNumberLength";
    private static final String PROPERTY_SIM_2_MAX_EMAIL_LENGTH = "gsm.sim2.maxEmailLength";

    private static final int SIM_IS_PRIMARY = 1;
    private static final int SIM_IS_SECONDARY = 0;
    private static final int SIM_NOT_SURE = -1;

    public static final int SIM_TYPE_UNKNOWN = 0;
    public static final int SIM_TYPE_SIM = 1;
    public static final int SIM_TYPE_USIM = 2;

    private static int getPrimaryDataSim(Context context) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getPrimaryDataSim - context is null!");
            return DualSimConstants.DSDS_INVALID_SLOT_ID;
        }

        if (ContactsUtils.isDualSimSupported()) {
            Object service = context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (service != null) {
                return ((ConnectivityManager)service).getDataSim();
            }
        }
        return DualSimConstants.DSDS_SLOT_1_ID;
    }

    /**
     * Check whether the given sim is primary data sim.
     *
     * @param context application context to get connectivity service.
     * @param index index of the sim which need be checked.
     * @return 1 if it's primary sim, 0 if it's secondary sim, or -1 if not sure
     */
    private static int isPrimarySimInternal(Context context, int index) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "isPrimarySim - context is null!");
            return -1;
        }

        int isPrimary = SIM_IS_SECONDARY;
        if (ContactsUtils.isDualSimSupported()) {
            switch (index) {
                case DualSimConstants.DSDS_SLOT_1_ID: {
                    if (getPrimaryDataSim(context) == DualSimConstants.DSDS_SLOT_1_ID) {
                        isPrimary = SIM_IS_PRIMARY;
                    }
                    break;
                }
                case DualSimConstants.DSDS_SLOT_2_ID: {
                    if (getPrimaryDataSim(context) == DualSimConstants.DSDS_SLOT_2_ID) {
                        isPrimary = SIM_IS_PRIMARY;
                    }
                    break;
                }
                default: {
                    isPrimary = SIM_NOT_SURE;
                    break;
                }
            }
        } else {
            // in 1S1S mode, always returns SIM_IS_PRIMARY
            return SIM_IS_PRIMARY;
        }
        return isPrimary;
    }

    public static boolean isPrimarySim(Context context, int index) {
        int isPrimary = isPrimarySimInternal(context, index);
        if (isPrimary != SIM_IS_SECONDARY) {
            return true;
        } else {
            return false;
        }
    }

    public static int getSimIndexByImsi(Context context, String imsi) {
        // TODO: caller should buffer the <imsi, simIndex> map
        // to improve the performance, but need consider the map change
        // when phone is switched between normal and flight mode
        if (TextUtils.isEmpty(imsi)) {
            if (DEBUG) Log.d(TAG, "getSimIndexByImsi - imsi is empty!");
            return DualSimConstants.DSDS_INVALID_SLOT_ID;
        }

        if (context == null) {
            if (DEBUG) Log.d(TAG, "getSimIndexByImsi - context is null!");
            return DualSimConstants.DSDS_INVALID_SLOT_ID;
        }

        int primaryDataSim = getPrimaryDataSim(context);
        if (imsi.equals(getPrimaryImsi(context))) {
            if (primaryDataSim == DualSimConstants.DSDS_SLOT_1_ID ||
                    primaryDataSim == DualSimConstants.DSDS_SLOT_2_ID) {
                return primaryDataSim;
            }
        } else if (imsi.equals(getSecondaryImsi(context))) {
            if (primaryDataSim == DualSimConstants.DSDS_SLOT_1_ID) {
                return DualSimConstants.DSDS_SLOT_2_ID;
            } else if (primaryDataSim == DualSimConstants.DSDS_SLOT_2_ID) {
                return DualSimConstants.DSDS_SLOT_1_ID;
            }
        }

        return DualSimConstants.DSDS_INVALID_SLOT_ID;
    }

    private static String getPrimaryImsi(Context context) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getPrimaryImsi - context is null!");
            return null;
        }

        Object service = context.getSystemService(Context.TELEPHONY_SERVICE);
        if (service != null) {
            return ((TelephonyManager)service).getSubscriberId();
        }
        return null;
    }

    private static String getSecondaryImsi(Context context) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getSecondaryImsi - context is null!");
            return null;
        }
        // directly use TelephonyManager to the 2nd phony
        if (ContactsUtils.isDualSimSupported()) {
            TelephonyManager phone2 = ContactsUtils.getTelephonyManager2(context);
            if (phone2 != null) {
                return phone2.getSubscriberId();
            }
        }
        return null;
    }

    private static int getPrimarySimState(Context context) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getPrimarySimState - context is null!");
            return TelephonyManager.SIM_STATE_UNKNOWN;
        }

        Object service = context.getSystemService(Context.TELEPHONY_SERVICE);
        if (service != null) {
            return ((TelephonyManager)service).getSimState();
        }
        return TelephonyManager.SIM_STATE_UNKNOWN;
    }

    private static int getSecondarySimState(Context context) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getSecondarySimState - context is null!");
            return TelephonyManager.SIM_STATE_UNKNOWN;
        }

        if (ContactsUtils.isDualSimSupported()) {
            TelephonyManager phone2 = ContactsUtils.getTelephonyManager2(context);
            if (phone2 != null) {
                return phone2.getSimState();
            }
        }
        return TelephonyManager.SIM_STATE_ABSENT;
    }

    public static int getPrimarySimType() {
        int simType = SystemProperties.getInt("gsm.sim.cardtype", 0);
        if (DEBUG) Log.d(TAG, "gsm.sim.cardtype = " + simType);
        return simType;
    }

    public static int getSecondarySimType() {
        int simType = SystemProperties.getInt("gsm.sim2.cardtype", 0);
        if (DEBUG) Log.d(TAG, "gsm.sim2.cardtype = " + simType);
        return simType;
    }

    public static Uri getPrimaryIccUri() {
        if (getPrimarySimType() == SIM_TYPE_USIM) {
            return USIM_CONTENT_URI;
        } else {
            return SIM_CONTENT_URI;
        }
    }

    public static Uri getSecondaryIccUri() {
        if (getSecondarySimType() == SIM_TYPE_USIM) {
            return USIM_2_CONTENT_URI;
        } else {
            return SIM_2_CONTENT_URI;
        }
    }

    public static Uri getIccUri(Context context, int index) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getIccUri - context is null!");
            return null;
        }

        int isPrimary = isPrimarySimInternal(context, index);
        switch (isPrimary) {
            case SIM_IS_PRIMARY: {
                return getPrimaryIccUri();
            }
            case SIM_IS_SECONDARY: {
                return getSecondaryIccUri();
            }
            default: {
                return null;
            }
        }
    }

    public static int getSimCapacity(Context context, int index) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getSimCapacity - context is null!");
            return -1;
        }

        int isPrimary = isPrimarySimInternal(context, index);
        int number = -1;
        switch (isPrimary) {
            case SIM_IS_PRIMARY: {
                number = SystemProperties.getInt(PROPERTY_SIM_CAPACITY, -1);
                if (DEBUG) Log.d(TAG, PROPERTY_SIM_CAPACITY + " = " + number);
                break;
            }
            case SIM_IS_SECONDARY: {
                number = SystemProperties.getInt(PROPERTY_SIM_2_CAPACITY, -1);
                if (DEBUG) Log.d(TAG, PROPERTY_SIM_2_CAPACITY + " = " + number);
                break;
            }
            default: {
                number = -1;
                break;
            }
        }
        return number;
    }

    public static int getFreeEntriesNumber(Context context, int index) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getFreeEntriesNumber - context is null!");
            return -1;
        }

        int isPrimary = isPrimarySimInternal(context, index);
        int number = -1;
        switch (isPrimary) {
            case SIM_IS_PRIMARY: {
                number = SystemProperties.getInt(PROPERTY_SIM_FREE_ENTRIES_NUMBER, -1);
                if (DEBUG) Log.d(TAG, PROPERTY_SIM_FREE_ENTRIES_NUMBER + " = " + number);
                break;
            }
            case SIM_IS_SECONDARY: {
                number = SystemProperties.getInt(PROPERTY_SIM_2_FREE_ENTRIES_NUMBER, -1);
                if (DEBUG) Log.d(TAG, PROPERTY_SIM_2_FREE_ENTRIES_NUMBER + " = " + number);
                break;
            }
            default: {
                number = -1;
                break;
            }
        }
        return number;
    }

    public static int getMaxNameLength(Context context, int index) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getMaxNameLength - context is null!");
            return -1;
        }

        int isPrimary = isPrimarySimInternal(context, index);
        int length = -1;
        switch (isPrimary) {
            case SIM_IS_PRIMARY: {
                length = SystemProperties.getInt(PROPERTY_SIM_MAX_NAME_LENGTH, -1);
                if (DEBUG) Log.d(TAG, PROPERTY_SIM_MAX_NAME_LENGTH + " = " + length);
                break;
            }
            case SIM_IS_SECONDARY: {
                length = SystemProperties.getInt(PROPERTY_SIM_2_MAX_NAME_LENGTH, -1);
                if (DEBUG) Log.d(TAG, PROPERTY_SIM_2_MAX_NAME_LENGTH + " = " + length);
                break;
            }
            default: {
                length = -1;
                break;
            }
        }
        return length;
    }

    public static int getMaxNumberLength(Context context, int index) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getMaxNumberLength - context is null!");
            return -1;
        }

        int isPrimary = isPrimarySimInternal(context, index);
        int length = -1;
        switch (isPrimary) {
            case SIM_IS_PRIMARY: {
                length = SystemProperties.getInt(PROPERTY_SIM_MAX_NUMBER_LENGTH, -1);
                if (DEBUG) Log.d(TAG, PROPERTY_SIM_MAX_NUMBER_LENGTH + " = " + length);
                break;
            }
            case SIM_IS_SECONDARY: {
                length = SystemProperties.getInt(PROPERTY_SIM_2_MAX_NUMBER_LENGTH, -1);
                if (DEBUG) Log.d(TAG, PROPERTY_SIM_2_MAX_NUMBER_LENGTH + " = " + length);
                break;
            }
            default: {
                length = -1;
                break;
            }
        }
        return length;
    }

    public static int getMaxEmailLength(Context context, int index) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getMaxEmailLength - context is null!");
            return -1;
        }

        int isPrimary = isPrimarySimInternal(context, index);
        int length = -1;
        switch (isPrimary) {
            case SIM_IS_PRIMARY: {
                length = SystemProperties.getInt(PROPERTY_SIM_MAX_EMAIL_LENGTH, -1);
                if (DEBUG) Log.d(TAG, PROPERTY_SIM_MAX_EMAIL_LENGTH + " = " + length);
                break;
            }
            case SIM_IS_SECONDARY: {
                length = SystemProperties.getInt(PROPERTY_SIM_2_MAX_EMAIL_LENGTH, -1);
                if (DEBUG) Log.d(TAG, PROPERTY_SIM_2_MAX_EMAIL_LENGTH + " = " + length);
                break;
            }
            default: {
                length = -1;
                break;
            }
        }
        return length;
    }

    public static String getSimImsi(Context context, int index) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getSimImsi - context is null!");
            return null;
        }
        if (!ContactsUtils.isDualSimSupported()) {
            return null;
        }

        int isPrimary = isPrimarySimInternal(context, index);
        switch (isPrimary) {
            case SIM_IS_PRIMARY: {
                return getPrimaryImsi(context);
            }
            case SIM_IS_SECONDARY: {
                return getSecondaryImsi(context);
            }
            default: {
                return null;
            }
        }
    }

    public static int getSimType(Context context, int index) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getSimType - context is null!");
            return -1;
        }

        int isPrimary = isPrimarySimInternal(context, index);
        switch (isPrimary) {
            case SIM_IS_PRIMARY: {
                return getPrimarySimType();
            }
            case SIM_IS_SECONDARY: {
                return getSecondarySimType();
            }
            default: {
                return SIM_TYPE_UNKNOWN;
            }
        }
    }

    public static int getSimState(Context context, int index) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "getSimState - context is null!");
            return TelephonyManager.SIM_STATE_UNKNOWN;
        }

        int isPrimary = isPrimarySimInternal(context, index);
        switch (isPrimary) {
            case SIM_IS_PRIMARY: {
                return getPrimarySimState(context);
            }
            case SIM_IS_SECONDARY: {
                return getSecondarySimState(context);
            }
            default: {
                return TelephonyManager.SIM_STATE_ABSENT;
            }
        }
    }

    public static boolean isPrimarySimAccessible(Context context) {
        return getPrimarySimState(context) == TelephonyManager.SIM_STATE_READY;
    }

    public static boolean isSecondarySimAccessible(Context context) {
        return getSecondarySimState(context) == TelephonyManager.SIM_STATE_READY;
    }

    public static boolean isSimAccessible(Context context, int index) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "isSimAvailable - context is null!");
            return false;
        }

        int isPrimary = isPrimarySimInternal(context, index);
        switch (isPrimary) {
            case SIM_IS_PRIMARY: {
                return isPrimarySimAccessible(context);
            }
            case SIM_IS_SECONDARY: {
                return isSecondarySimAccessible(context);
            }
            default: {
                return false;
            }
        }
    }

    public static boolean isSim1Ready(Context context) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "isSim1Ready - context is null!");
            return false;
        }

        int state = getSimState(context, DualSimConstants.DSDS_SLOT_1_ID);
        return state == TelephonyManager.SIM_STATE_READY;
    }

    public static boolean isSim2Ready(Context context) {
        if (context == null) {
            if (DEBUG) Log.d(TAG, "isSim2Ready - context is null!");
            return false;
        }

        int state = getSimState(context, DualSimConstants.DSDS_SLOT_2_ID);
        return state == TelephonyManager.SIM_STATE_READY;
    }
}
