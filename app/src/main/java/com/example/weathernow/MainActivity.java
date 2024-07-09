package com.example.weathernow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private FusedLocationProviderClient locationClient;
    private OkHttpClient httpClient;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private ImageView currentConditionsIcon;
    private TextView currentTemperatureText, lastUpdatedText,
            currentConditionsText, currentLocationText;
    private ConstraintLayout layout;
    // Arrays to hold the view IDs for each day's forecast
    private int[] dateTextViewIds;
    private int[] iconImageViewIds;
    private int[] minTempTextViewIds;
    private int[] maxTempTextViewIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Set up custom OkHttp client
        setUpHttpClient();
        // Set up UI and assigns Views ids to variables
        setUpUI();
        // If location permissions are granted, gets last location
        checkPermissionAndFetchLocation();
    }

    /**
     * Checks location permissions and fetches the last known location if permissions are granted.
     */
    private void checkPermissionAndFetchLocation() {
        if (checkLocationPermission()) {
            getLastLocation();
        } else {
            requestLocationPermission();
        }
    }

    /**
     * Handles menu item clicks in the navigation popup menu.
     *
     * @param item The menu item that was clicked
     * @return true if the menu item click was handled, false otherwise
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(item.getItemId() == R.id.id_currentLocation){
            return true;
        }
        if(item.getItemId() == R.id.id_searchLocation) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
            return true;
        }
        if(item.getItemId() == R.id.id_favLocations) {
            Intent intent = new Intent(this, FavouriteActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    /**
     * Shows a popup menu for the navigation button
     *
     * @param v The view that triggered the popup menu
     */
    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.nav_menu);
        popup.show();
    }

    /**
     * Checks if the app has location permission.
     *
     * @return true if location permission is granted, false otherwise
     */
    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests location permission from the user.
     */
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }

    /**
     * Shows a dialog indicating that location permission is necessary.
     */
    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Location permission is necessary for this feature. " +
                        "Please grant the permission refreshing your current location by " +
                        "tapping the red location icon")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Handles the result of the location permission request.
     * If the permission is granted it fetches the location, otherwise shows a permission denied dialog
     *
     * @param requestCode  The request code passed in requestPermissions(android.app.Activity, String[], int)
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions which is either
     *                     PackageManager.PERMISSION_GRANTED or PackageManager.PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            // Check if the location permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with getting the last known location
                getLastLocation();
            } else {
                // Permission denied, show a permission denied dialog
                showPermissionDeniedDialog();
            }
        }
    }

    /**
     * Retrieves the most recent cached location from the past currently available.
     * Since this method simply checks caches for pre-computed locations, it is generally cheap and quick to return.
     * If there are no cached locations, it calls the getCurrentLocation() to compute a fresh location.
     */
    private void getLastLocation() {
        if (checkLocationPermission()) {
            locationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    // Location found, fetch location name
                    fetchCityNameByCoordinates(latitude, longitude);
                } else {
                    // Fall back to getCurrentLocation if getLastLocation returns null
                    getCurrentLocation();
                }
            }).addOnFailureListener(e -> {
                // Fall back to getCurrentLocation if getLastLocation fails
                getCurrentLocation();
            });
        }
    }

    /**
     * Returns a single location fix representing the best estimate of the current location of the device.
     * Since this method computes a fresh location, it is slower than the getLastLocation()
     * This method is thus called only if there are no cached locations found with getLastLocation()
     */
    private void getCurrentLocation() {
        if (checkLocationPermission()) {
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
            }).addOnSuccessListener(this, location -> {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    // Location found, fetch location name
                    fetchCityNameByCoordinates(latitude,longitude);
                } else {
                    // Error getting location
                    Toast.makeText(MainActivity.this,
                            "An error occurred. Please try refreshing your location.", Toast.LENGTH_LONG).show();
                }
            }).addOnFailureListener(this, e -> Toast.makeText(MainActivity.this,
                    "An error occurred. Please try refreshing your location.", Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Fetches the city name based on the provided latitude and longitude.
     *
     * @param lat Latitude of the location
     * @param lon Longitude of the location
     */
    private void fetchCityNameByCoordinates(double lat, double lon) {
        // Creating the URL for the API search endpoint
        String url = Utils.BASE_URL
                + Utils.SEARCH_API
                + this.getString(R.string.weather_api_key)
                + "&q=" + lat + "," + lon;
        // Creating a Request object using the Builder pattern
        Request request = new Request.Builder()
                .url(url)          // Set the URL
                .build();          // Build the Request object
        // Enqueue the request to be executed asynchronously
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Check if the exception is a timeout
                if (e instanceof java.net.SocketTimeoutException) {
                    // Show a Toast message to inform the user
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "Request timed out. Please try refreshing your location.", Toast.LENGTH_LONG).show());
                } else if (e instanceof java.net.UnknownHostException) {
                    // Show a Toast message to inform the user
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "Unable to reach the server. " +
                                    "Please check your internet connection and try again.", Toast.LENGTH_LONG).show());
                } else {
                    // Handle other IOExceptions
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "An error occurred. Please try again.", Toast.LENGTH_LONG).show());
                    e.printStackTrace();
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    // Get the response body as a string
                    String jsonResponse = response.body().string();
                    // Create a new Gson instance for JSON conversion
                    Gson gson = new Gson();
                    // Deserialize the JSON string into a JsonArray
                    JsonArray jsonArray = gson.fromJson(jsonResponse, JsonArray.class);
                    if (jsonArray.size() > 0) {
                        // Get the first JsonObject from the array
                        JsonObject location = jsonArray.get(0).getAsJsonObject();
                        // Extract the city ID, name, and country from the JSON object
                        int cityId = location.get("id").getAsInt();
                        String cityName = location.get("name").getAsString();
                        String country = location.get("country").getAsString();
                        String nameCountry = cityName + ", " + country;
                        // Sets the fetched city name and country (on UI thread)
                        runOnUiThread(() -> currentLocationText.setText(nameCountry));
                        // Fetches weather condition for the city
                        fetchWeatherCondition(cityId);
                    } else {
                        // If no location is returned
                        runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                "No city found for the given coordinates, " +
                                        "try refreshing your location",
                                Toast.LENGTH_LONG).show());
                    }
                }
            }
        });
    }

    /**
     * Fetches the current and forecast weather conditions for the specified city ID.
     *
     * @param cityId The ID of the city
     */
    private void fetchWeatherCondition(int cityId) {
        // Creating the URL string for the API forecast endpoint
        String url = Utils.BASE_URL
                + Utils.FORECAST_API
                + this.getString(R.string.weather_api_key)
                + "&q=id:" + cityId + "&days=5";
        // Creating a Request object using the Builder pattern
        Request request = new Request.Builder()
                .url(url)          // Set the URL
                .build();          // Build the Request object
        // Enqueue the request to be executed asynchronously
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Check if the exception is a timeout
                if (e instanceof java.net.SocketTimeoutException) {
                    // Show a Toast message to inform the user
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "Request timed out. Please try refreshing your location.", Toast.LENGTH_LONG).show());
                } else if (e instanceof java.net.UnknownHostException) {
                    // Show a Toast message to inform the user
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "Unable to reach the server. " +
                                    "Please check your internet connection and try again.", Toast.LENGTH_LONG).show());
                } else {
                    // Handle other IOExceptions
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "An error occurred. Please try again.", Toast.LENGTH_LONG).show());
                    e.printStackTrace();
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    // Get the response body as a string
                    String jsonResponse = response.body().string();
                    // Create a new Gson instance for JSON conversion
                    Gson gson = new Gson();
                    // Deserialize the JSON string into a JsonObject
                    JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
                    // Get the "current" and "condition" JSON object from the response
                    JsonObject current = jsonObject.getAsJsonObject("current");
                    JsonObject condition = current.getAsJsonObject("condition");
                    // Extract the weather condition text, icon code, temperature,
                    // last updated time, and day/night flag
                    String weatherCondition = condition.get("text").getAsString();
                    int currentIconCode = condition.get("code").getAsInt();
                    double currentTemp = current.get("temp_c").getAsDouble();
                    String lastUpdated = current.get("last_updated").getAsString();
                    int isDay = current.get("is_day").getAsInt();

                    // Updates the UI elements for the current location weather (on the UI thread)
                    updateCurrentWeatherUI(currentTemp, currentIconCode, isDay, weatherCondition, lastUpdated);

                    // Get the "forecast" JSON array from the response (it's an array of forecast days)
                    JsonArray forecastDays = jsonObject.getAsJsonObject("forecast")
                            .getAsJsonArray("forecastday");
                    // Iterate over the forecast days
                    for (int i = 0; i < forecastDays.size(); i++) {
                        // Get the JSON object for the current day
                        JsonObject day = forecastDays.get(i).getAsJsonObject();
                        // Extract the date and day object from the JSON
                        String date = day.get("date").getAsString();
                        JsonObject dayObject = day.getAsJsonObject("day");
                        // Extract the minimum and maximum temperatures and icon code for the day
                        double minTemp = dayObject.get("mintemp_c").getAsDouble();
                        double maxTemp = dayObject.get("maxtemp_c").getAsDouble();
                        int iconCode = dayObject.getAsJsonObject("condition").get("code").getAsInt();
                        // Update UI for the indexed day forecast (on the UI thread)
                        updateForecastUI(i, date, iconCode, minTemp, maxTemp);
                    }
                }
            }
        });
    }

    /**
     * Updates the UI elements for the current location weather
     * It runs on the UI thread
     *
     * @param currentTemp      The current location temperature
     * @param currentIconCode  The code for the weather icon
     * @param isDay            The day/night code (1 for day, 0 for night)
     * @param weatherCondition String describing weather conditions
     * @param lastUpdated      Date and time of last forecast update
     */
    private void updateCurrentWeatherUI(double currentTemp, int currentIconCode, int isDay, String weatherCondition, String lastUpdated) {
        runOnUiThread(() -> {
            currentTemperatureText.setText(String.format("%s°", currentTemp));
            Drawable icon = Utils.getWeatherIcon(MainActivity.this, currentIconCode, isDay);
            Drawable background = Utils.getBackground(MainActivity.this, currentIconCode, isDay);
            layout.setBackground(background);
            currentConditionsIcon.setImageDrawable(icon);
            currentConditionsText.setText(weatherCondition);
            lastUpdatedText.setText(lastUpdated);
        });
    }

    /**
     * Updates the UI elements for a single forecast day
     * It runs on the UI thread
     *
     * @param index    The index of the UI element
     * @param date     The date of the forecast
     * @param iconCode The code of the weather icon
     * @param minTemp  The minimum temp of the day
     * @param maxTemp  The maximum temp of the day
     */
    private void updateForecastUI(int index, String date, int iconCode, double minTemp, double maxTemp){
        runOnUiThread(() -> {
            TextView dateText = findViewById(dateTextViewIds[index]);
            TextView minTempText = findViewById(minTempTextViewIds[index]);
            TextView maxTempText = findViewById(maxTempTextViewIds[index]);
            ImageView iconView = findViewById(iconImageViewIds[index]);

            Drawable icon = Utils.getWeatherIcon(MainActivity.this, iconCode, 1);
            iconView.setImageDrawable(icon);
            String formattedDate = Utils.formatDate(date);
            dateText.setText(formattedDate);
            minTempText.setText(String.format("%s°", minTemp));
            maxTempText.setText(String.format("%s°", maxTemp));
        });
    }

    /**
     * Sets up the OkHttpClient with custom timeout settings.
     */
    private void setUpHttpClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Sets up the UI elements and listeners and assigns view IDs to variables.
     */
    private void setUpUI() {
        layout = findViewById(R.id.root_layout);
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        ImageView locationIcon = findViewById(R.id.locationIcon);
        locationIcon.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                getCurrentLocation();
            } else {
                requestLocationPermission();
            }
        });
        currentLocationText = findViewById(R.id.currentLocationText);
        lastUpdatedText = findViewById(R.id.lastUpdateText);
        currentTemperatureText = findViewById(R.id.currentTemperatureText);
        currentConditionsText = findViewById(R.id.currentConditionsText);
        currentConditionsIcon = findViewById(R.id.currentConditionsIcon);
        // Arrays to hold the view IDs for each day's forecast
        dateTextViewIds = new int[]{
                (R.id.forecastDateText1), (R.id.forecastDateText2),
                (R.id.forecastDateText3), (R.id.forecastDateText4), (R.id.forecastDateText5)};
        iconImageViewIds = new int[]{
                (R.id.forecastIconView1), (R.id.forecastIconView2),
                (R.id.forecastIconView3), (R.id.forecastIconView4), (R.id.forecastIconView5)};
        minTempTextViewIds = new int[]{
                (R.id.forecastMinTempText1), (R.id.forecastMinTempText2),
                (R.id.forecastMinTempText3), (R.id.forecastMinTempText4), (R.id.forecastMinTempText5)};
        maxTempTextViewIds = new int[]{
                (R.id.forecastMaxTempText1), (R.id.forecastMaxTempText2),
                (R.id.forecastMaxTempText3), (R.id.forecastMaxTempText4), (R.id.forecastMaxTempText5)};
    }
}