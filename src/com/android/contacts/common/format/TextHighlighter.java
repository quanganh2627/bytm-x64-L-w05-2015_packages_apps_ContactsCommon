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

package com.android.contacts.common.format;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.android.contacts.common.util.ContactLocaleUtils;
import com.android.contacts.common.util.ContactLocaleUtils.LookupKey;
import com.android.contacts.common.util.HanziToPinyin.Token;

import java.util.ArrayList;

/**
 * Highlights the text in a text field.
 */
public class TextHighlighter {
    private final String TAG = TextHighlighter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private int mTextStyle;

    private CharacterStyle mTextStyleSpan;

    public TextHighlighter(int textStyle) {
        mTextStyle = textStyle;
        mTextStyleSpan = getStyleSpan();
    }

    /**
     * Sets the text on the given text view, highlighting the word that matches the given prefix.
     *
     * @param view the view on which to set the text
     * @param text the string to use as the text
     * @param prefix the prefix to look for
     */
    public void setPrefixText(TextView view, String text, String prefix) {
        view.setText(applyPrefixHighlight(text, prefix));
    }

    private CharacterStyle getStyleSpan() {
        return new StyleSpan(mTextStyle);
    }

    /**
     * Applies highlight span to the text.
     * @param text Text sequence to be highlighted.
     * @param start Start position of the highlight sequence.
     * @param end End position of the highlight sequence.
     */
    public void applyMaskingHighlight(SpannableString text, int start, int end) {
        /** Sets text color of the masked locations to be highlighted. */
        text.setSpan(getStyleSpan(), start, end, 0);
    }

    /**
     * Returns a CharSequence which highlights the given prefix if found in the given text.
     *
     * @param text the text to which to apply the highlight
     * @param prefix the prefix to look for
     */
    public CharSequence applyPrefixHighlight(CharSequence text, String prefix) {
        if (prefix == null) {
            return text;
        }

        // Skip non-word characters at the beginning of prefix.
        int prefixStart = 0;
        while (prefixStart < prefix.length() &&
                !Character.isLetterOrDigit(prefix.charAt(prefixStart))) {
            prefixStart++;
        }
        final String trimmedPrefix = prefix.substring(prefixStart);

        int index = FormatUtils.indexOfWordPrefix(text, trimmedPrefix);
        if (index != -1) {
            final SpannableString result = new SpannableString(text);
            result.setSpan(mTextStyleSpan, index, index + trimmedPrefix.length(), 0 /* flags */);
            return result;
        } else {
            return text;
        }
    }
    //refdsds
    public CharSequence applyDigitalNameFilter(CharSequence name, String filter) {
        ArrayList<LookupKey> keys = ContactLocaleUtils.getIntance().getNameLookupKeys(
                name.toString());
        if (keys != null && !keys.isEmpty() && filter != null) {
            int keySize = keys.size();
            int filterLength = filter.length();

            int start = -1;
            int end = -1;

            // Compare the whole string
            int i = 0;
            while (i < keySize && (start == -1 || end == -1)) {
                // Seek first filter character
                while (i < keySize && keys.get(i).digits.charAt(0) != filter.charAt(0)) {
                    i++;
                }

                if (i >= keySize) {
                    break;
                }

                int s = i;

                int j = 0;
                while (i < keySize) {
                    LookupKey key = keys.get(i);
                    if (key.digits.charAt(0) != ' ') {
                        int digitsLength = key.digits.length();
                        int k = 0;
                        while (k < digitsLength && j < filterLength &&
                                key.digits.charAt(k) == filter.charAt(j)) {
                            j++; k++;
                        }
                        if (j == filterLength) {
                            start = keys.get(s).position;
                            if (key.type == Token.LATIN) {
                                end = key.position + k;
                            } else {
                                int e = i + 1;
                                end = (e == keySize) ? name.length() : keys.get(e).position;
                            }
                            break;
                        } else if (k != digitsLength) {
                            i = s + 1;
                            break;
                        }
                    }
                    i++;
                }
            }

            // Compare initial characters only
            i = 0;
            while (i < keySize && (start == -1 || end == -1)) {
                // Seek first filter character
                while (i < keySize && keys.get(i).digits.charAt(0) != filter.charAt(0)) {
                    i++;
                }

                if (i >= keySize) {
                    break;
                }

                int s = i;

                int j = 0;
                while (i < keySize && j < filterLength) {
                    char c = keys.get(i).digits.charAt(0);
                    if (c == filter.charAt(j)) {
                        j++;
                    } else if (c != ' ') {
                        break;
                    }
                    i++;
                }

                if (j == filterLength) {
                    start = keys.get(s).position;
                    end = (i == keySize) ? name.length() : keys.get(i).position;
                    break;
                }

                i = s + 1;
            }

            if (start != -1 && end != -1) {
                SpannableString result = new SpannableString(name);
                result.setSpan(mTextStyleSpan, start, end, 0 /* flags */);
                return result;
            }
        }
        return name;
    }

    public CharSequence applyNumberFilter(CharSequence number, String filter) {
        int index = FormatUtils.indexOfWord(number, filter);
        if (index != -1) {
            SpannableString result = new SpannableString(number);
            result.setSpan(mTextStyleSpan, index, index + filter.length(), 0 /* flags */);
            return result;
        } else {
            return number;
        }
    }
}
