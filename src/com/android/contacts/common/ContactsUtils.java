/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.common;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemProperties;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.DisplayPhoto;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.net.Uri;
import com.android.contacts.common.util.Constants;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.test.NeededForTesting;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.util.DualSimConstants;
import com.android.internal.telephony.TelephonyConstants;
import com.android.phone.common.PhoneConstants;
import java.util.List;

public class ContactsUtils {
    private static final String TAG = "ContactsUtils";

    private static int sThumbnailSize = -1;

    // TODO find a proper place for the canonical version of these
    public interface ProviderNames {
        String YAHOO = "Yahoo";
        String GTALK = "GTalk";
        String MSN = "MSN";
        String ICQ = "ICQ";
        String AIM = "AIM";
        String XMPP = "XMPP";
        String JABBER = "JABBER";
        String SKYPE = "SKYPE";
        String QQ = "QQ";
    }

    /**
     * This looks up the provider name defined in
     * ProviderNames from the predefined IM protocol id.
     * This is used for interacting with the IM application.
     *
     * @param protocol the protocol ID
     * @return the provider name the IM app uses for the given protocol, or null if no
     * provider is defined for the given protocol
     * @hide
     */
    public static String lookupProviderNameFromId(int protocol) {
        switch (protocol) {
            case Im.PROTOCOL_GOOGLE_TALK:
                return ProviderNames.GTALK;
            case Im.PROTOCOL_AIM:
                return ProviderNames.AIM;
            case Im.PROTOCOL_MSN:
                return ProviderNames.MSN;
            case Im.PROTOCOL_YAHOO:
                return ProviderNames.YAHOO;
            case Im.PROTOCOL_ICQ:
                return ProviderNames.ICQ;
            case Im.PROTOCOL_JABBER:
                return ProviderNames.JABBER;
            case Im.PROTOCOL_SKYPE:
                return ProviderNames.SKYPE;
            case Im.PROTOCOL_QQ:
                return ProviderNames.QQ;
        }
        return null;
    }

    /**
     * Test if the given {@link CharSequence} contains any graphic characters,
     * first checking {@link TextUtils#isEmpty(CharSequence)} to handle null.
     */
    public static boolean isGraphic(CharSequence str) {
        return !TextUtils.isEmpty(str) && TextUtils.isGraphic(str);
    }

    /**
     * Returns true if two objects are considered equal.  Two null references are equal here.
     */
    @NeededForTesting
    public static boolean areObjectsEqual(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * Returns true if two {@link Intent}s are both null, or have the same action.
     */
    public static final boolean areIntentActionEqual(Intent a, Intent b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return TextUtils.equals(a.getAction(), b.getAction());
    }

    public static boolean areContactWritableAccountsAvailable(Context context) {
        final List<AccountWithDataSet> accounts =
                AccountTypeManager.getInstance(context).getAccounts(true /* writeable */);
        return !accounts.isEmpty();
    }

    public static boolean areGroupWritableAccountsAvailable(Context context) {
        final List<AccountWithDataSet> accounts =
                AccountTypeManager.getInstance(context).getGroupWritableAccounts();
        return !accounts.isEmpty();
	}
//from PEKALL ref
    public static Intent getDualSimCallIntent(String number, int simIndex) {
        return getDualSimCallIntent(number, simIndex, null);
    }

    public static Intent getDualSimCallIntent(String number, int simIndex, String callOrigin) {
        Uri uri = Uri.fromParts(CallUtil.SCHEME_TEL, number, null);
        return getDualSimCallIntent(uri, simIndex, callOrigin);
    }

    public static Intent getDualSimCallIntent(Uri uri, int simIndex, String callOrigin) {
        final Intent intent = new Intent(DualSimConstants.ACTION_DUAL_SIM_CALL, uri);
        intent.putExtra(DualSimConstants.EXTRA_DSDS_CALL_POLICY, getSlotExtra(simIndex));
        if (!TextUtils.isEmpty(callOrigin)) {
            intent.putExtra(PhoneConstants.EXTRA_CALL_ORIGIN, callOrigin);
        }
        return intent;
    }

    public static int getSlotExtra(int simIndex) {
        if (simIndex == DualSimConstants.DSDS_SLOT_2_ID) {
            return DualSimConstants.EXTRA_DCALL_SLOT_2;
        } else {
            return DualSimConstants.EXTRA_DCALL_SLOT_1;
        }
    }

    /**
     * Return an Intent for launching voicemail screen.
     */
    public static Intent getVoicemailIntent() {
        return getVoicemailIntent(DualSimConstants.DSDS_SLOT_1_ID);
    }

    public static Intent getVoicemailIntent(int simIndex) {
        final Intent intent;
        if (ContactsUtils.isDualSimSupported()) {
            intent = new Intent(DualSimConstants.ACTION_DUAL_SIM_CALL);
            final Uri uri;
            if (simIndex == DualSimConstants.DSDS_SLOT_2_ID) {
                uri = Uri.fromParts("voicemail", "", null);
                intent.putExtra(DualSimConstants.EXTRA_DSDS_CALL_POLICY,
                        DualSimConstants.EXTRA_DCALL_SLOT_2);
            } else {
                uri = Uri.fromParts("voicemail", "phone2", null);
                intent.putExtra(DualSimConstants.EXTRA_DSDS_CALL_POLICY,
                        DualSimConstants.EXTRA_DCALL_SLOT_1);
            }
            intent.setData(uri);
        } else {
            intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                    Uri.fromParts("voicemail", "", null));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }    

    /**
     * Returns the size (width and height) of thumbnail pictures as configured in the provider. This
     * can safely be called from the UI thread, as the provider can serve this without performing
     * a database access
     */
    public static int getThumbnailSize(Context context) {
        if (sThumbnailSize == -1) {
            final Cursor c = context.getContentResolver().query(
                    DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI,
                    new String[] { DisplayPhoto.THUMBNAIL_MAX_DIM }, null, null, null);
            try {
                c.moveToFirst();
                sThumbnailSize = c.getInt(0);
            } finally {
                c.close();
            }
        }
        return sThumbnailSize;
    }

    public static TelephonyManager getTelephonyManager2(Context context) {
        return TelephonyManager.get2ndTm();
    }

    private static final int TEST_LAYOUT_NOT_SET = 0;
    private static final int TEST_LAYOUT_1S1S = 1;
    private static final int TEST_LAYOUT_DSDS = 2;

    public static boolean isDualSimSupported() {
        int layoutConfig = SystemProperties.getInt("contacts.layout_config",
                TEST_LAYOUT_NOT_SET);

        boolean val;
        switch (layoutConfig) {
            case TEST_LAYOUT_1S1S:
               val = false;
               break;
            case TEST_LAYOUT_DSDS:
               val = true;
               break;
            default:
               val = TelephonyConstants.IS_DSDS;
               break;
        }
        return val;
    }
}
