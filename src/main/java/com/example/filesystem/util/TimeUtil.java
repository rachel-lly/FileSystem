package com.example.filesystem.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 21:14
 */
public class TimeUtil {

    public static String getCurrentTime() {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String currentTime = dateFormat.format(date);
        return currentTime;
    }
}
