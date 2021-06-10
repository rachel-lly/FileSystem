package com.example.filesystem.util;

import java.util.Collection;

public class JudgeUtil {

    public static boolean isStringEmpty(String string){
        return  (null == string  || "".equals(string) );
    }

    public static boolean isNull(Object object){
        return null == object;
    }

    public static boolean isEmpty(Collection<?> collection){

        return  ( null == collection || collection.size() == 0 );
    }

    public static boolean isEmpty(Integer integer) {
        return (null == integer);
    }
}
