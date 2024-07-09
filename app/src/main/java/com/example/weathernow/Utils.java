package com.example.weathernow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Utils {

    /**
     * File path for shared preferences
     */
    public static final String sharedPrefFile = "com.example.weathernow";
    /**
     * Base URL for the weather API.
     */
    public static final String BASE_URL = "https://api.weatherapi.com/v1/";

    /**
     * Endpoint for searching location.
     */
    public static final String SEARCH_API = "search.json?key=";

    /**
     * Endpoint for fetching weather forecast data.
     */
    public static final String FORECAST_API = "forecast.json?key=";

    /**
     * A map that associates weather condition codes with background drawable names.
     */
    private static final Map<Integer, String> weatherBackgrounds = new HashMap<>();
    /*
     * Static block to populate the weatherBackgrounds map with weather condition codes
     * and their corresponding background drawable names.
     * This block ensures that the map is populated only once when the class is loaded.
     */
    static {
        // Clear Sky
        weatherBackgrounds.put(1000, "clear_sky");
        // Cloudy Sky (covers various types of cloudy weather)
        weatherBackgrounds.put(1003, "cloud_sky");
        weatherBackgrounds.put(1006, "cloud_sky");
        weatherBackgrounds.put(1009, "cloud_sky");
        // Light Rain (covers various types of light precipitation)
        weatherBackgrounds.put(1063, "light_rain");
        weatherBackgrounds.put(1069, "light_rain");
        weatherBackgrounds.put(1072, "light_rain");
        weatherBackgrounds.put(1150, "light_rain");
        weatherBackgrounds.put(1153, "light_rain");
        weatherBackgrounds.put(1168, "light_rain");
        weatherBackgrounds.put(1171, "light_rain");
        weatherBackgrounds.put(1180, "light_rain");
        weatherBackgrounds.put(1183, "light_rain");
        weatherBackgrounds.put(1186, "light_rain");
        weatherBackgrounds.put(1189, "light_rain");
        weatherBackgrounds.put(1192, "light_rain");
        weatherBackgrounds.put(1240, "light_rain");
        // Heavy Rain (covers various types of heavy precipitation)
        weatherBackgrounds.put(1087, "heavy_rain_sky");
        weatherBackgrounds.put(1195, "heavy_rain_sky");
        weatherBackgrounds.put(1198, "heavy_rain_sky");
        weatherBackgrounds.put(1201, "heavy_rain_sky");
        weatherBackgrounds.put(1204, "heavy_rain_sky");
        weatherBackgrounds.put(1207, "heavy_rain_sky");
        weatherBackgrounds.put(1243, "heavy_rain_sky");
        weatherBackgrounds.put(1246, "heavy_rain_sky");
        weatherBackgrounds.put(1249, "heavy_rain_sky");
        // Snowy Weather (covers all types of snow)
        weatherBackgrounds.put(1066, "snowy_sky");
        weatherBackgrounds.put(1114, "snowy_sky");
        weatherBackgrounds.put(1117, "snowy_sky");
        weatherBackgrounds.put(1210, "snowy_sky");
        weatherBackgrounds.put(1213, "snowy_sky");
        weatherBackgrounds.put(1216, "snowy_sky");
        weatherBackgrounds.put(1219, "snowy_sky");
        weatherBackgrounds.put(1222, "snowy_sky");
        weatherBackgrounds.put(1225, "snowy_sky");
        weatherBackgrounds.put(1237, "snowy_sky");
        weatherBackgrounds.put(1252, "snowy_sky");
        weatherBackgrounds.put(1255, "snowy_sky");
        weatherBackgrounds.put(1258, "snowy_sky");
        weatherBackgrounds.put(1261, "snowy_sky");
        weatherBackgrounds.put(1264, "snowy_sky");
        // Storm
        weatherBackgrounds.put(1273, "storm_sky");
        weatherBackgrounds.put(1276, "storm_sky");
        weatherBackgrounds.put(1279, "storm_sky");
        weatherBackgrounds.put(1282, "storm_sky");
        // Fog
        weatherBackgrounds.put(1030, "foggy_sky");
        weatherBackgrounds.put(1135, "foggy_sky");
        weatherBackgrounds.put(1147, "foggy_sky");
    }

    /**
     * Formats a date from "yyyy-MM-dd" format to "EEE, dd MMMM" format.
     *
     * @param originalDate The original date string in "yyyy-MM-dd" format.
     * @return The formatted date string in "EEE, dd MMMM" format. If parsing fails, returns the original date string.
     */
    public static String formatDate(@NonNull String originalDate) {
        SimpleDateFormat originalFormat =
                new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        SimpleDateFormat targetFormat =
                new SimpleDateFormat("EEE, dd MMMM", Locale.ENGLISH);
        Date date;
        try {
            date = originalFormat.parse(originalDate);
            if (date != null) {
                return targetFormat.format(date);
            } else return originalDate;
        } catch (ParseException e) {
            e.printStackTrace();
            return originalDate; // Return original date if parsing fails
        }
    }

    /**
     * Gets the Drawable icon resource based on the weather code and day/night indicator.
     *
     * @param context     The context to access resources.
     * @param weatherCode The weather condition code
     * @param isDay       The day/night indicator (1 for day, 0 for night).
     * @return The corresponding drawable icon.
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    @SuppressWarnings("discouragedapi")
    public static Drawable getWeatherIcon(Context context, int weatherCode, int isDay) {
        // Check if it's day or night and append 'n' for night icons
        String suffix = isDay == 1 ? "" : "n";
        // Construct the drawable icon name (_<code><isDay> i.e. "_1000n")
        String iconName = "_" + weatherCode + suffix;
        // Get the resource ID from the drawable name
        int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        // Return the drawable resource
        return context.getDrawable(resId);
    }

    /**
     * Gets the icon resource id based on the weather code and day/night indicator.
     * This method is needed by the app widget
     *
     * @param context     The context to access resources.
     * @param weatherCode The weather condition code.
     * @param isDay       The day/night indicator (1 for day, 0 for night).
     * @return The corresponding drawable background.
     */
    @SuppressWarnings("discouragedapi")
    public static int getWeatherIconId(Context context, int weatherCode, int isDay) {
        // Check if it's day or night and append 'n' for night icons
        String suffix = isDay == 1 ? "" : "n";
        // Construct the drawable icon name (_<code><isDay> i.e. "_1000n")
        String iconName = "_" + weatherCode + suffix;
        // Get the resource ID from the drawable name
        return context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
    }

    /**
     * Gets the drawable background based on the weather code and day/night indicator.
     *
     * @param context     The context to access resources.
     * @param weatherCode The weather condition code.
     * @param isDay       The day/night indicator (1 for day, 0 for night).
     * @return The corresponding drawable background.
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    @SuppressWarnings("discouragedapi")
    public static Drawable getBackground(Context context, int weatherCode, int isDay) {
        String baseBackgroundName = weatherBackgrounds.getOrDefault(weatherCode, "gradient_background");
        String backgroundName = null;
        if (baseBackgroundName != null) {
            backgroundName = baseBackgroundName +
                    (baseBackgroundName.equals("gradient_background") ? "" : getTimeOfDaySuffix(isDay));
        }
        int resId = context.getResources().getIdentifier(backgroundName, "drawable", context.getPackageName());
        return context.getDrawable(resId);

    }

    /**
     * Gets the suffix for the time of day based on the day/night indicator for the background images.
     *
     * @param isDay The day/night indicator (1 for day, 0 for night).
     * @return The suffix for the time of day ("_day" for day, "_night" for night).
     */
    private static String getTimeOfDaySuffix(int isDay) {
        return isDay == 1 ? "_day" : "_night";
    }
}


