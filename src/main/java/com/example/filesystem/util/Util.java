package com.example.filesystem.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author: rachel-lly
 * @Date: 2021-06-12 11:50
 */
public class Util {

    public static boolean isStringEmpty(String string){
        return  (null == string  || "".equals(string) );
    }

    public static boolean isNull(Object object){
        return null == object;
    }

    public static String getCurrentTime() {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String currentTime = dateFormat.format(date);
        return currentTime;
    }
}
