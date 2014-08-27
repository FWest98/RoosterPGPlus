package com.thomasdh.roosterpgplus.Data;

import android.content.Context;
import android.util.Log;

import com.thomasdh.roosterpgplus.Helpers.AsyncActionCallback;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Models.Leerling;
import com.thomasdh.roosterpgplus.Models.Vak;
import com.thomasdh.roosterpgplus.Models.Week;
import com.thomasdh.roosterpgplus.util.ExceptionHandler;

import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Ophalen van roosterinfo uit opslag of van internet
 */
public class RoosterInfo {
    private static final String WEKEN_FILENAME = "wekenarray";
    private static final String KLASSEN_FILENAME = "klassenarray";
    private static final String LERAREN_FILENAME = "lerarenarray";
    private static final String LEERLINGEN_FILENAME = "leerlingenarray";
    private static final String LOADS_FILENAME = "loadshashtable";
    private static final String WEKEN_UREN_FILENAME = "wekenhashtable";

    //region Weken

    public static void getWeken(Context context, AsyncActionCallback callback) {
        if(HelperFunctions.hasInternetConnection(context)) {
            WebDownloader.getWeken(result -> {
                callback.onAsyncActionComplete(result);
                saveInStorage(WEKEN_FILENAME, context, result);
            }, exception -> {
                Log.e("WebDownloader", "Er ging iets mis met het ophalen", (Exception) exception);
                ExceptionHandler.handleException((Exception) exception, context, ExceptionHandler.HandleType.SIMPLE);
                RoosterInfo.<ArrayList<Week>>getOnError(WEKEN_FILENAME, context, callback);
            });
        } else {
            RoosterInfo.<ArrayList<Week>>getOnError(WEKEN_FILENAME, context, callback);
        }
    }

    public static int getWeekUrenCount(Context context, int week) throws IllegalArgumentException {
        Hashtable<Integer, Integer> weken = getFromStorage(WEKEN_UREN_FILENAME, context);
        if(weken == null) {
            throw new IllegalArgumentException("Week niet opgeslagen");
        }
        return weken.get(week);
    }

    public static void setWeekUrenCount(Context context, int week, int urenCount) {
        Hashtable<Integer, Integer> weken = getFromStorage(WEKEN_UREN_FILENAME, context);
        if(weken == null) weken = new Hashtable<>();
        weken.put(week, urenCount);
        saveInStorage(WEKEN_UREN_FILENAME, context, weken);
    }

    //endregion
    //region Klassen

    public static void getKlassen(Context context, AsyncActionCallback callback) {
        if(HelperFunctions.hasInternetConnection(context)) {
            WebDownloader.getKlassen(klassen -> {
                callback.onAsyncActionComplete(klassen);
                saveInStorage(KLASSEN_FILENAME, context, klassen);
            }, exception -> {
                Log.e("WebDownloader", "Er ging iets mis met het ophalen", (Exception) exception);
                ExceptionHandler.handleException((Exception) exception, context, ExceptionHandler.HandleType.SIMPLE);
                RoosterInfo.<ArrayList<String>>getOnError(KLASSEN_FILENAME, context, callback);
            });
        } else {
            RoosterInfo.<ArrayList<String>>getOnError(KLASSEN_FILENAME, context, callback);
        }
    }

    //endregion
    //region Leraren

    public static void getLeraren(Context context, AsyncActionCallback callback) {
        if(HelperFunctions.hasInternetConnection(context)) {
            WebDownloader.getLeraren(leraren -> {
                callback.onAsyncActionComplete(leraren);
                saveInStorage(LERAREN_FILENAME, context, leraren);
            }, exception -> {
                Log.e("WebDownloader", "Er ging iets mis met het ophalen van de leraren", (Exception) exception);
                ExceptionHandler.handleException((Exception) exception, context, ExceptionHandler.HandleType.SIMPLE);
                RoosterInfo.<ArrayList<Vak>>getOnError(LERAREN_FILENAME, context, callback);
            });
        } else {
            RoosterInfo.<ArrayList<Vak>>getOnError(LERAREN_FILENAME, context, callback);
        }
    }

    //endregion
    //region Leerlingen

    public static void getLeerlingen(Context context, AsyncActionCallback callback) {
        if(HelperFunctions.hasInternetConnection(context)) {
            WebDownloader.getLeerlingen(leerlingen -> {
                callback.onAsyncActionComplete(leerlingen);
                saveInStorage(LEERLINGEN_FILENAME, context, leerlingen);
            }, exception -> {
                Log.e("WebDownloader", "Er ging iets met het het ophalen van de leerlingen", (Exception) exception);
                ExceptionHandler.handleException((Exception) exception, context, ExceptionHandler.HandleType.SIMPLE);
                RoosterInfo.<ArrayList<Leerling>>getOnError(LEERLINGEN_FILENAME, context, callback);
            });
        } else {
            RoosterInfo.<ArrayList<Leerling>>getOnError(LEERLINGEN_FILENAME, context, callback);
        }
    }

    //endregion
    //region LoadTimes

    public static Long getLoad(String itemName, Context context) {
        Hashtable<String, Long> loads = getLoads(context);
        if(loads == null) return null;
        return loads.get(itemName);
    }

    public static Hashtable<String, Long> getLoads(Context context) {
        return getFromStorage(LOADS_FILENAME, context);
    }

    public static void setLoad(String itemName, Long value, Context context) {
        Hashtable<String, Long> loads = getLoads(context);
        if(loads == null) loads = new Hashtable<>();
        loads.put(itemName, value);
        saveInStorage(LOADS_FILENAME, context, loads);
    }

    //endregion
    //region Helpers

    private static <T> void getOnError(String fileName, Context context, AsyncActionCallback callback) {
        T result = getFromStorage(fileName, context);
        try {
            callback.onAsyncActionComplete(result);
        } catch(Exception e) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), "De callback aan getLeraren gaf een fout", e);
        }
    }

    private static <T> T getFromStorage(String fileName, Context context) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(context.openFileInput(fileName));
            Object result = objectInputStream.readObject();
            return (T) result;
        } catch(Exception e) {
            Log.e("RoosterInfo", "Kon "+fileName+" niet openen", e);
            return null;
        }
    }

    private static <T> void saveInStorage(String fileName, Context context, T data) {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(data);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch(Exception e) {
            Log.e("RoosterInfo", "Kon "+fileName+" niet opslaan", e);
        }
    }

    //endregion
}