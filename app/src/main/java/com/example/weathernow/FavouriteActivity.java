package com.example.weathernow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FavouriteActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private OkHttpClient httpClient;
    private SharedPreferences mPreferences;
    private final int maxFavorites = 5;  // Maximum number of favorite locations

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourite);
        setupDeleteButtons();
        // Set up the client
        setUpHttpClient();
        mPreferences = getSharedPreferences(Utils.sharedPrefFile, MODE_PRIVATE);
        // Retrieve the JSON string associated with the key "favorites".
        // If the key does not exist, use the default value "[]", which represents an empty JSON array.
        String favoritesJson = mPreferences.getString("favorites", "[]");
        // Create a new Gson instance for JSON conversion
        Gson gson = new Gson();
        // Define the type of the data structure to be deserialized.
        // The TypeToken captures the generic type at runtime, which is List<Integer> in this case.
        Type type = new TypeToken<List<Integer>>() {}.getType();
        // Deserialize the JSON string into a List<Integer> object using Gson.
        // This converts the JSON representation of the favorites list back into a Java List<Integer> object.
        List<Integer> favorites = gson.fromJson(favoritesJson, type);
        /*
         Loop through the maximum number of favorite locations
         If the current index is less than the size of the favorites list,
         fetch the weather condition for the city ID at the current index
         Otherwise, clear the row by setting it to its default empty state
        */
        for (int i = 0; i < maxFavorites; i++) {
            if (i < favorites.size()) {
                fetchWeatherCondition(favorites.get(i), i);
            } else {
                updateOrClearRow(i, null, 0, 0, 0, false);
            }
        }
    }

    /**
     * Sets up delete button listeners for each row.
     */
    @SuppressWarnings("discouragedapi")
    private void setupDeleteButtons() {
        for (int i = 0; i < maxFavorites; i++) {
            int deleteButtonId = getResources().getIdentifier("deleteButton" + (i + 1), "id", getPackageName());
            Button deleteButton = findViewById(deleteButtonId);
            int finalI = i;
            deleteButton.setOnClickListener(v -> deleteFavourite(finalI));
        }
    }

    /**
     * Shows a popup menu for the navigation button
     * @param v The view that triggered the popup menu
     */
    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.nav_menu);
        popup.show();
    }

    /**
     * Handles menu item clicks in the popup menu.
     *
     * @param item The menu item that was clicked
     * @return true if the menu item click was handled, false otherwise
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(item.getItemId() == R.id.id_favLocations) {
            return true;
        }
        if(item.getItemId() == R.id.id_currentLocation){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            return true;
        }
        if(item.getItemId() == R.id.id_searchLocation) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    /**
     * Fetches the weather conditions for the specified city ID and updates the corresponding row.
     *
     * @param cityId The ID of the city
     * @param rowIndex The index of the row to update
     */
    private void fetchWeatherCondition(int cityId, int rowIndex) {
        String url = Utils.BASE_URL
                + Utils.FORECAST_API
                + this.getString(R.string.weather_api_key)
                + "&q=id:" + cityId + "&days=1";
        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Check if the exception is a timeout
                if (e instanceof java.net.SocketTimeoutException) {
                    // Show a Toast message to inform the user
                    runOnUiThread(() -> Toast.makeText(FavouriteActivity.this,
                            "Request timed out. Please try again.", Toast.LENGTH_LONG).show());
                } else if (e instanceof java.net.UnknownHostException) {
                    // Show a Toast message to inform the user
                    runOnUiThread(() -> Toast.makeText(FavouriteActivity.this,
                            "Unable to reach the server. " +
                                    "Please check your internet connection and try again.", Toast.LENGTH_LONG).show());
                } else {
                    // Handle other IOExceptions
                    runOnUiThread(() -> Toast.makeText(FavouriteActivity.this,
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
                    // From the "location" JSON object get the city and country name
                    JsonObject location = jsonObject.getAsJsonObject("location");
                    String cityName = location.get("name").getAsString() + ", " + location.get("country").getAsString();
                    // Get the "current" and "condition" JSON object from the response
                    JsonObject current = jsonObject.getAsJsonObject("current");
                    JsonObject condition = current.getAsJsonObject("condition");
                    // Extract the weather icon code and temperature
                    int currentIconCode = condition.get("code").getAsInt();
                    JsonObject forecastDay = jsonObject.getAsJsonObject("forecast").getAsJsonArray("forecastday").get(0).getAsJsonObject();
                    JsonObject day = forecastDay.getAsJsonObject("day");
                    double minTemp = day.get("mintemp_c").getAsDouble();
                    double maxTemp = day.get("maxtemp_c").getAsDouble();
                    // Updates the UI elements for the indexed location
                    runOnUiThread(() -> updateOrClearRow(rowIndex, cityName, currentIconCode, minTemp, maxTemp, true));
                }
            }
        });
    }

    /**
     * Updates or clears the specified row with weather information.
     *
     * @param rowIndex The index of the row to update or clear
     * @param cityName The name of the city
     * @param iconCode The weather icon code
     * @param minTemp The minimum temperature
     * @param maxTemp The maximum temperature
     * @param isUpdate True if updating the row, false if clearing the row
     */
    @SuppressWarnings("discouragedapi")
    private void updateOrClearRow(int rowIndex, String cityName, int iconCode, double minTemp, double maxTemp, boolean isUpdate){
        int cityNameId = getResources().getIdentifier("favLocationText" + (rowIndex + 1), "id", getPackageName());
        int weatherIconId = getResources().getIdentifier("favLocationIcon" + (rowIndex + 1), "id", getPackageName());
        int minTempId = getResources().getIdentifier("favMinTemp" + (rowIndex + 1), "id", getPackageName());
        int maxTempId = getResources().getIdentifier("favMaxTemp" + (rowIndex + 1), "id", getPackageName());
        int deleteButtonId = getResources().getIdentifier("deleteButton" + (rowIndex + 1), "id", getPackageName());

        TextView cityNameTextView = findViewById(cityNameId);
        ImageView weatherIconImageView = findViewById(weatherIconId);
        TextView minTempTextView = findViewById(minTempId);
        TextView maxTempTextView = findViewById(maxTempId);
        Button deleteButton = findViewById(deleteButtonId);

        if(isUpdate) {
            cityNameTextView.setText(cityName);
            weatherIconImageView.setImageDrawable(Utils.getWeatherIcon(this, iconCode, 1));
            minTempTextView.setText(String.format("%s°", minTemp));
            maxTempTextView.setText(String.format("%s°", maxTemp));
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            cityNameTextView.setText("");
            weatherIconImageView.setImageDrawable(null);
            minTempTextView.setText("");
            maxTempTextView.setText("");
            deleteButton.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Deletes a favorite location at the specified index.
     * Clears the row and updates the locations id list in the shared pref file
     *
     * @param rowIndex The index of the row to delete
     */
    private void deleteFavourite(int rowIndex) {
        // Retrieve the JSON string of the favorites list from SharedPreferences
        String favoritesJson = mPreferences.getString("favorites", "[]");
        Gson gson = new Gson();
        // Define the type of the data structure to be deserialized.
        // The TypeToken captures the generic type at runtime, which is List<Integer> in this case.
        Type type = new TypeToken<List<Integer>>() {}.getType();
        // Deserialize the JSON string into a List<Integer> object using Gson.
        List<Integer> favorites = gson.fromJson(favoritesJson, type);
        // Check if the rowIndex is within the bounds of the favorites list
        if (rowIndex < favorites.size()) {
            // Remove the favorite location at the specified index
            favorites.remove(rowIndex);
            // Get a SharedPreferences editor to update the preferences
            SharedPreferences.Editor preferencesEditor = mPreferences.edit();
            // Convert the updated favorites list back to a JSON string
            preferencesEditor.putString("favorites", gson.toJson(favorites));
            // Use commit() to ensure the preferences are updated synchronously
            boolean success = preferencesEditor.commit();
            if (success) {
                // Refresh the activity to load the updated list
                // This way, after a delete, the locations moves up of a row
                recreate();
            } else {
                // Handle the failure case
                Toast.makeText(this,
                        "Failed to delete, try again", Toast.LENGTH_SHORT).show();
            }
        }
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
     * Sets up an Intent to start the search activity for the "+" button
     */
    public void onAddFavButtonClick (View v) {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
    }
}

