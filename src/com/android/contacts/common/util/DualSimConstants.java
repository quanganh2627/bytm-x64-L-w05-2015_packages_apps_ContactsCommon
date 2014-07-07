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

import com.android.internal.telephony.TelephonyConstants;
import com.android.internal.telephony.TelephonyIntents2;

public class DualSimConstants {
    public static final int DSDS_INVALID_SLOT_ID = TelephonyConstants.DSDS_INVALID_SLOT_ID;
    public static final int DSDS_SLOT_1_ID = TelephonyConstants.DSDS_SLOT_1_ID;
    public static final int DSDS_SLOT_2_ID = TelephonyConstants.DSDS_SLOT_2_ID;

    public static final String ACTION_DUAL_SIM_CALL = TelephonyConstants.ACTION_DUAL_SIM_CALL;
    public static final String ACTION_SIM_STATE_CHANGED_2 = TelephonyIntents2.ACTION_SIM_STATE_CHANGED;

    public static final String EXTRA_DSDS_CALL_POLICY = TelephonyConstants.EXTRA_DSDS_CALL_POLICY;
    public static final int EXTRA_DCALL_SLOT_1 = TelephonyConstants.EXTRA_DCALL_SLOT_1;
    public static final int EXTRA_DCALL_SLOT_2 = TelephonyConstants.EXTRA_DCALL_SLOT_2;

    public static final String IMSI_FOR_SIP_CALL = TelephonyConstants.IMSI_FOR_SIP_CALL;

    public static final String CALL_SETTINGS_CLASS_NAME_DS =
            "com.android.phone.CallFeaturesSettingTab";
}
