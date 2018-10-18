package com.github.ghmxr.apkextractor.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class PinYin {
	
	/** 
     * ½«×Ö·û´®ÖÐµÄÖÐÎÄ×ª»¯ÎªÆ´Òô,ÆäËû×Ö·û²»±ä 
     *  
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
     * »ñÈ¡ºº×Ö´®Æ´ÒôÊ××ÖÄ¸£¬Ó¢ÎÄ×Ö·û²»±ä   
     * @param chinese ºº×Ö´®   
     * @return ººÓïÆ´ÒôÊ××ÖÄ¸   
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
     * »ñÈ¡ºº×Ö´®Æ´Òô£¬Ó¢ÎÄ×Ö·û²»±ä   
     * @param chinese ºº×Ö´®   
     * @return ººÓïÆ´Òô   
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
