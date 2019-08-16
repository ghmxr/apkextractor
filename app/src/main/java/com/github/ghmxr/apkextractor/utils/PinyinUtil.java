package com.github.ghmxr.apkextractor.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class PinyinUtil {

    /**
     * 将字符串中的中文转化为拼音,其他字符不变
     * @param inputString
     * @return
     */
    public static String getPinYin(String inputString) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);

        char[] input = inputString.trim().toCharArray();
        String output = "";

        try {
            for (int i = 0; i < input.length; i++) {
                if (java.lang.Character.toString(input[i]).matches("[\\u4E00-\\u9FA5]+")) {
                    String[] temp = PinyinHelper.toHanyuPinyinStringArray(input[i], format);
                    output += temp[0];
                } else
                    output += java.lang.Character.toString(input[i]);
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            e.printStackTrace();
        }
        catch(java.lang.NullPointerException npex){
            npex.printStackTrace();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        return output;
    }

    /**
     * 获取汉字串拼音首字母，英文字符不变
     * @param chinese 汉字串
     * @return 汉语拼音首字母
     */
    public static String getFirstSpell(String chinese) {
        StringBuffer pybf = new StringBuffer();
        char[] arr = chinese.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        try{
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] > 128) {
                    String[] temp = PinyinHelper.toHanyuPinyinStringArray(arr[i], defaultFormat);
                    if (temp != null) {
                        pybf.append(temp[0].charAt(0));
                    }

                } else {
                    pybf.append(arr[i]);
                }
            }
        }
        catch (BadHanyuPinyinOutputFormatCombination bhpe) {
            bhpe.printStackTrace();
        }
        catch(java.lang.NullPointerException npex){
            npex.printStackTrace();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

        return pybf.toString().replaceAll("\\W", "").trim();
    }

    /**
     * 获取汉字串拼音，英文字符不变
     * @param chinese 汉字串
     * @return 汉语拼音
     */
    public static String getFullSpell(String chinese) {
        StringBuffer pybf = new StringBuffer();
        char[] arr = chinese.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        try{
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] > 128) {
                    pybf.append(PinyinHelper.toHanyuPinyinStringArray(arr[i], defaultFormat)[0]);
                } else {
                    pybf.append(arr[i]);
                }
            }
        }
        catch (BadHanyuPinyinOutputFormatCombination e) {
            e.printStackTrace();
        }
        catch(java.lang.NullPointerException npex){
            npex.printStackTrace();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        return pybf.toString();
    }
}
