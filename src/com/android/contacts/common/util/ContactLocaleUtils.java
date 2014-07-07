/*
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
 * limitations under the License
 */

package com.android.contacts.common.util;

import com.android.contacts.common.util.HanziToPinyin.Token;

import android.provider.ContactsContract.FullNameStyle;
import android.text.TextUtils;
import android.util.SparseArray;

import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * This utility class provides customized sort key and name lookup key according the locale.
 */
public class ContactLocaleUtils {

    private static final HashMap<Character, Character> sLetterDigitMap;
    private static final String sLetterDigitTable =
        "012abcABC3defDEF4ghiGHI5jklJKL6mnoMNO7pqrsPQRS8tuvTUV9wxyzWXYZ";

    static {
        sLetterDigitMap = new HashMap<Character, Character>();
        int length = sLetterDigitTable.length();
        char key = 0;
        for (int i = 0; i < length; i++) {
            char c = sLetterDigitTable.charAt(i);
            if (c >= '0' && c <= '9') {
                key = c;
            }
            sLetterDigitMap.put(c, key);
        }
    }

    public static boolean convertToDigits(String input, StringBuilder sb) {
        if (input == null || sb == null) {
            return false;
        }

        sb.setLength(0);
        int length = input.length();
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            Character digit = sLetterDigitMap.get(c);
            if (digit != null) {
                sb.append(digit);
            } else {
                return false;
            }
        }
        return true;
    }

    public static class LookupKey {
        public final int type;
        public final String source;
        public final String digits;
        public final int position;

        public LookupKey(int type, String source, String digits, int position) {
            this.type = type;
            this.source = source;
            this.digits = digits;
            this.position = position;
        }
    }

    /**
     * This class is the default implementation.
     * <p>
     * It should be the base class for other locales' implementation.
     */
    public class ContactLocaleUtilsBase {
        @SuppressWarnings("unused")
        public ArrayList<LookupKey> getNameLookupKeys(String name) {
            return null;
        }
    }

    /**
     * The classes to generate the Chinese style sort and search keys.
     * <p>
     * The sorting key is generated as each Chinese character' pinyin proceeding with
     * space and character itself. If the character's pinyin unable to find, the character
     * itself will be used.
     * <p>
     * The below additional name lookup keys will be generated.
     * a. Chinese character's pinyin and pinyin's initial character.
     * b. Latin word and the initial character for Latin word.
     * The name lookup keys are generated to make sure the name can be found by from any
     * initial character.
     */
    private class ChineseContactUtils extends ContactLocaleUtilsBase {
        @Override
        public ArrayList<LookupKey> getNameLookupKeys(String name) {
            if (!TextUtils.isEmpty(name) && sLookupKeyCache.containsKey(name)) {
                return sLookupKeyCache.get(name);
            }

            ArrayList<Token> tokens = HanziToPinyin.getInstance().get(name);
            final int tokenCount = tokens.size();
            if (tokenCount == 0) {
                return null;
            }

            final ArrayList<LookupKey> keys = new ArrayList<LookupKey>();
            final StringBuilder sb = new StringBuilder();
            int position = 0;
            for (int i = 0; i < tokenCount; i++) {
                final Token token = tokens.get(i);
                boolean result = true;
                if (Token.PINYIN == token.type) {
                    result = convertToDigits(token.target, sb);
                } else if (Token.LATIN == token.type) {
                    result = convertToDigits(token.source, sb);
                } else if (Token.SPACE == token.type) {
                    sb.append(" ");
                } else {
                    result = false;
                }
                if (result) {
                    if (sb.length() > 0) {
                        keys.add(new LookupKey(token.type, token.source, sb.toString(), position));
                        position += token.source.length();
                        sb.setLength(0);
                    }
                } else {
                    return null;
                }
            }
            if (!keys.isEmpty()) {
                sLookupKeyCache.put(name, keys);
            }
            return keys;
        }
    }

    private static final String CHINESE_LANGUAGE = Locale.CHINESE.getLanguage().toLowerCase();
    private static final String JAPANESE_LANGUAGE = Locale.JAPANESE.getLanguage().toLowerCase();
    private static final String KOREAN_LANGUAGE = Locale.KOREAN.getLanguage().toLowerCase();

    private static ContactLocaleUtils sSingleton;
    private static HashMap<String, ArrayList<LookupKey>> sLookupKeyCache =
            new HashMap<String, ArrayList<LookupKey>>();
    private final SparseArray<ContactLocaleUtilsBase> mUtils =
            new SparseArray<ContactLocaleUtilsBase>();

    private final ContactLocaleUtilsBase mBase = new ContactLocaleUtilsBase();

    private String mLanguage;

    private ContactLocaleUtils() {
        setLocale(null);
    }

    public void setLocale(Locale currentLocale) {
        if (currentLocale == null) {
            mLanguage = Locale.getDefault().getLanguage().toLowerCase();
        } else {
            mLanguage = currentLocale.getLanguage().toLowerCase();
        }
    }

    public static void clearNameLookupKeyCache() {
        sLookupKeyCache.clear();
    }

    public ArrayList<LookupKey> getNameLookupKeys(String name) {
        int nameStyle = FullNameStyle.CHINESE; //guessFullNameStyle(name);
        return getForNameLookup(Integer.valueOf(nameStyle)).getNameLookupKeys(name);
    }

    public ArrayList<LookupKey> getNameLookupKeys(String name, int nameStyle) {
        return getForNameLookup(Integer.valueOf(nameStyle)).getNameLookupKeys(name);
    }

    /**
     *  Determine which utility should be used for generating NameLookupKey.
     *  <p>
     *  a. For Western style name, if the current language is Chinese, the
     *     ChineseContactUtils should be used.
     *  b. For Chinese and CJK style name if current language is neither Japanese or Korean,
     *     the ChineseContactUtils should be used.
     */
    private ContactLocaleUtilsBase getForNameLookup(Integer nameStyle) {
        int nameStyleInt = nameStyle.intValue();
        Integer adjustedUtil = Integer.valueOf(getAdjustedStyle(nameStyleInt));
        if (CHINESE_LANGUAGE.equals(mLanguage) && nameStyleInt == FullNameStyle.WESTERN) {
            adjustedUtil = Integer.valueOf(FullNameStyle.CHINESE);
        }
        return get(adjustedUtil);
    }

    private synchronized ContactLocaleUtilsBase get(Integer nameStyle) {
        ContactLocaleUtilsBase utils = mUtils.get(nameStyle);
        if (utils == null) {
            if (nameStyle.intValue() == FullNameStyle.CHINESE) {
                utils = new ChineseContactUtils();
                mUtils.put(nameStyle, utils);
            }
        }
        return (utils == null) ? mBase : utils;
    }

    public static synchronized ContactLocaleUtils getIntance() {
        if (sSingleton == null) {
            sSingleton = new ContactLocaleUtils();
        }
        return sSingleton;
    }

    private int getAdjustedStyle(int nameStyle) {
        if (nameStyle == FullNameStyle.CJK  && !JAPANESE_LANGUAGE.equals(mLanguage) &&
                !KOREAN_LANGUAGE.equals(mLanguage)) {
            return FullNameStyle.CHINESE;
        } else {
            return nameStyle;
        }
    }

    public static String translateNameStyle(int nameStyle) {
        switch (nameStyle) {
        case FullNameStyle.WESTERN:
            return "WESTERN";
        case FullNameStyle.CJK:
            return "CJK";
        case FullNameStyle.CHINESE:
            return "CHINESE";
        case FullNameStyle.JAPANESE:
            return "JAPANESE";
        case FullNameStyle.KOREAN:
            return "KOREAN";
        default:
            return "UNDEFINED";
        }
    }

    public static int guessFullNameStyle(String name) {
        if (name == null) {
            return FullNameStyle.UNDEFINED;
        }

        int nameStyle = FullNameStyle.UNDEFINED;
        int length = name.length();
        int offset = 0;
        while (offset < length) {
            int codePoint = Character.codePointAt(name, offset);
            if (Character.isLetter(codePoint)) {
                UnicodeBlock unicodeBlock = UnicodeBlock.of(codePoint);

                if (!isLatinUnicodeBlock(unicodeBlock)) {
                    if (isCJKUnicodeBlock(unicodeBlock)) {
                        // We don't know if this is Chinese, Japanese or Korean -
                        // trying to figure out by looking at other characters in the name
                        return guessCJKNameStyle(name, offset + Character.charCount(codePoint));
                    }

                    if (isJapanesePhoneticUnicodeBlock(unicodeBlock)) {
                        return FullNameStyle.JAPANESE;
                    }

                    if (isKoreanUnicodeBlock(unicodeBlock)) {
                        return FullNameStyle.KOREAN;
                    }
                }
                nameStyle = FullNameStyle.WESTERN;
            }
            offset += Character.charCount(codePoint);
        }
        return nameStyle;
    }

    private static int guessCJKNameStyle(String name, int offset) {
        int length = name.length();
        while (offset < length) {
            int codePoint = Character.codePointAt(name, offset);
            if (Character.isLetter(codePoint)) {
                UnicodeBlock unicodeBlock = UnicodeBlock.of(codePoint);
                if (isJapanesePhoneticUnicodeBlock(unicodeBlock)) {
                    return FullNameStyle.JAPANESE;
                }
                if (isKoreanUnicodeBlock(unicodeBlock)) {
                    return FullNameStyle.KOREAN;
                }
            }
            offset += Character.charCount(codePoint);
        }

        return FullNameStyle.CHINESE;
    }

    private static boolean isLatinUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.BASIC_LATIN ||
                unicodeBlock == UnicodeBlock.LATIN_1_SUPPLEMENT ||
                unicodeBlock == UnicodeBlock.LATIN_EXTENDED_A ||
                unicodeBlock == UnicodeBlock.LATIN_EXTENDED_B ||
                unicodeBlock == UnicodeBlock.LATIN_EXTENDED_ADDITIONAL;
    }

    private static boolean isCJKUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                unicodeBlock == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                unicodeBlock == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
                unicodeBlock == UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
                unicodeBlock == UnicodeBlock.CJK_RADICALS_SUPPLEMENT ||
                unicodeBlock == UnicodeBlock.CJK_COMPATIBILITY ||
                unicodeBlock == UnicodeBlock.CJK_COMPATIBILITY_FORMS ||
                unicodeBlock == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                unicodeBlock == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
    }

    private static boolean isKoreanUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.HANGUL_SYLLABLES ||
                unicodeBlock == UnicodeBlock.HANGUL_JAMO ||
                unicodeBlock == UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }

    private static boolean isJapanesePhoneticUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.KATAKANA ||
                unicodeBlock == UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS ||
                unicodeBlock == UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ||
                unicodeBlock == UnicodeBlock.HIRAGANA;
    }
}
