package com.example.weathernow;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * WeatherWidgetProvider is an AppWidgetProvider that updates the home screen widget
 * with the current weather conditions based on the device's location.
 */
public class WeatherWidgetProvider extends AppWidgetProvider {

    private OkHttpClient httpClient;

    /**
     * Called to update the widget at intervals defined in the widget info.
     *
     * @param context The Context in which this receiver is running.
     * @param appWidgetManager The AppWidgetManager instance to update the widget.
     * @param appWidgetIds The appWidgetIds for which an update is needed (only one in this case).
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Set up the client for API call
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();
        // Update the widget directly without looping through appWidgetIds
        // since this is the only widget instance to update
        updateWidget(context);
    }

    /**
     * Initiates the process to update the widget by retrieving the last known location.
     *
     * @param context The Context in which this receiver is running.
     */
    private void updateWidget(Context context) {
        getLastLocation(context);
    }

    /**
     * Retrieves the last known cached location of the device using.
     * If no cached location is found, calls getCurrentLocation()
     *
     * @param context The Context in which this receiver is running.
     */
    @SuppressLint("MissingPermission")
    private void getLastLocation(Context context) {
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(context);
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                // Location found, fetch location name
                fetchCityNameByCoordinates(context, latitude, longitude);
            } else {
                // Fall back to getCurrentLocation if getLastLocation returns null
                getCurrentLocation(context);
            }
            // Fall back to getCurrentLocation if getLastLocation fails
        }).addOnFailureListener(e -> getCurrentLocation(context));
    }

    /**
     * Computes the current location of the device
     *
     * @param context The Context in which this receiver is running.
     */
    @SuppressLint("MissingPermission")
    private void getCurrentLocation(Context context) {
        FusedLocationProviderClient locationClient =
                LocationServices.getFusedLocationProviderClient(context);
        locationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
            @Override
            public boolean isCancellationRequested() {
                return false;
            }

            @NonNull
            @Override
            public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                return this;
            }
        }).addOnSuccessListener(location -> {
            // Location found, fetch location name
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                fetchCityNameByCoordinates(context, latitude, longitude);
            }
        });
    }

    /**
     * Fetches the city name and other location details using the provided latitude and longitude.
     *
     * @param context The Context in which this receiver is running.
     * @param lat Latitude of the location.
     * @param lon Longitude of the location.
     */
    private void fetchCityNameByCoordinates(Context context, double lat, double lon) {
        String url = Utils.BASE_URL
                + Utils.SEARCH_API
                + context.getString(R.string.weather_api_key)
                + "&q=" + lat + "," + lon;
        Request request = new Request.Builder().url(url).build();
        // Enqueue the request to be executed asynchronously
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    // Get the response body as a string
                    String jsonResponse = response.body().string();
                    // Create a new Gson instance for JSON conversion
                    Gson gson = new Gson();
                    // Deserialize the JSON response into a JsonArray
                    JsonArray jsonArray = gson.fromJson(jsonResponse, JsonArray.class);
                    if (jsonArray.size() > 0) {
                        // Get the first JsonObject from the array
                        JsonObject location = jsonArray.get(0).getAsJsonObject();
                        // Extract the city ID, name, and country from the JSON object
                        int cityId = location.get("id").getAsInt();
                        String cityName = location.get("name").getAsString();
                        String country = location.get("country").getAsString();
                        String nameCountry = cityName + ", " + country;
                        // Fetch the weather condition for the city
                        fetchWeatherCondition(context, cityId, nameCountry);
                    }
                }
            }
        });
    }

    /**
     * Fetches the current weather conditions for the specified city ID.
     *
     * @param context The Context in which this receiver is running.
     * @param cityId The ID of the city.
     * @param cityName The name of the city.
     */
    private void fetchWeatherCondition(Context context, int cityId, String cityName) {
        String url = Utils.BASE_URL
                + Utils.FORECAST_API
                + context.getString(R.string.weather_api_key)
                + "&q=id:" + cityId + "&days=1";
        Request request = new Request.Builder().url(url).build();
        // Enqueue the request to be executed asynchronously
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    // Get the response body as a string
                    String jsonResponse = response.body().string();
                    // Create a new Gson instance for JSON conversion
                    Gson gson = new Gson();
                    // Deserialize the JSON response into a JsonObject
                    JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
                    // Get the "current" and "condition" JSON objects from the response
                    JsonObject current = jsonObject.getAsJsonObject("current");
                    JsonObject condition = current.getAsJsonObject("condition");
                    // Extract the weather condition code and temperature
                    int currentIconCode = condition.get("code").getAsInt();
                    double currentTemp = current.get("temp_c").getAsDouble();

                    // Update the widget UI directly here on a background thread
                    updateWidgetUI(context, cityName, currentTemp, currentIconCode);
                }
            }
        });
    }

    /**
     * Updates the widget's UI elements (city name, temperature, weather icon) using RemoteViews.
     *
     * @param context The Context in which this receiver is running.
     * @param cityName The name of the city.
     * @param currentTemp The current temperature.
     * @param iconCode The code of the weather icon.
     */
    private void updateWidgetUI(Context context, String cityName, double currentTemp, int iconCode) {
        // Create a RemoteViews object, which represents the widget's layout
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        // Sets the new resources for the views
        views.setTextViewText(R.id.widget_city_name, cityName);
        views.setTextViewText(R.id.widget_temperature, String.format("%s°", currentTemp));
        views.setImageViewResource(R.id.widget_weather_icon, Utils.getWeatherIconId(context, iconCode, 1));
        // Get an instance of the AppWidgetManager, which allows updating the widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        // Create a ComponentName object representing the widget provider
        ComponentName widget = new ComponentName(context, WeatherWidgetProvider.class);
        // Update the widget with the new RemoteViews layout
        appWidgetManager.updateAppWidget(widget, views);
    }
}
