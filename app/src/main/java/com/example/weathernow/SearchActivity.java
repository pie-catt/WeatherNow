package com.example.weathernow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private OkHttpClient httpClient;
    private SharedPreferences mPreferences;
    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> adapter;
    private List<String> cityList = new ArrayList<>();
    private List<Location> locations;
    int selectedCityId;
    private ImageView currentConditionsIcon;
    private TextView currentTemperatureText, lastUpdatedText,
            currentConditionsText, lastUpdatedLabel;
    private Button favButton;
    private ConstraintLayout layout;
    private LinearLayout currentLayout;
    private TableLayout forecastTableLayout;
    // Arrays to hold the view IDs for each day's forecast
    private int[] dateTextViewIds;
    private int[] iconImageViewIds;
    private int[] minTempTextViewIds;
    private int[] maxTempTextViewIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setUpHttpClient();
        setUpUI();
        // Hides UI views
        setVisibleUI(false);
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
     * Handles menu item clicks in the popup menu.
     *
     * @param item The menu item that was clicked
     * @return true if the menu item click was handled, false otherwise
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(item.getItemId() == R.id.id_searchLocation) {
            return true;
        }
        if(item.getItemId() == R.id.id_currentLocation){
            Intent intent = new Intent(this, MainActivity.class);
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
     * Fetches cities based on the search query.
     *
     * @param query The search query for the city.
     */
    private void fetchCities(String query) {
        // Creating the URL for the API search endpoint
        String url = Utils.BASE_URL
                + Utils.SEARCH_API
                + this.getString(R.string.weather_api_key)
                + "&q=" + query;
        // Creating a Request object using the Builder pattern
        Request request = new Request.Builder()
                .url(url)
                .build();
        // Enqueue the request to be executed asynchronously
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Check if the exception is a timeout
                if (e instanceof java.net.SocketTimeoutException) {
                    // Show a Toast message to inform the user
                    runOnUiThread(() -> Toast.makeText(SearchActivity.this,
                            "Request timed out. Please try again.", Toast.LENGTH_LONG).show());
                } else if (e instanceof java.net.UnknownHostException) {
                    // Show a Toast message to inform the user
                    runOnUiThread(() -> Toast.makeText(SearchActivity.this,
                            "Unable to reach the server. " +
                                    "Please check your internet connection and try again.", Toast.LENGTH_LONG).show());
                } else {
                    // Handle other IOExceptions
                    runOnUiThread(() -> Toast.makeText(SearchActivity.this,
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
                    // Define the type for the list of Location objects
                    Type listType = new TypeToken<List<Location>>() {}.getType();
                    // Deserialize the JSON response into a list of Location objects
                    locations = gson.fromJson(jsonResponse, listType);
                    // Run the following code on the UI thread
                    runOnUiThread(() -> {
                        // Clear the current city list
                        cityList.clear();
                        // Add the name and country of each location to the city list
                        for (Location location : locations) {
                            cityList.add(location.getName() + ", " + location.getCountry());
                        }
                        // Create a new ArrayAdapter with the updated city list
                        adapter = new ArrayAdapter<>(SearchActivity.this,
                                R.layout.custom_list_item, cityList);
                        // Notify the adapter that the data has changed
                        adapter.notifyDataSetChanged();
                        // Set the updated adapter to the AutoCompleteTextView
                        autoCompleteTextView.setAdapter(adapter);
                    });
                }
            }
        });
    }

    /**
     * Fetches the weather conditions for the specified city ID.
     *
     * @param cityId The ID of the city
     */
    private void fetchWeatherCondition(int cityId) {
        // Creating the URL for the API forecast endpoint
        String url = Utils.BASE_URL
                + Utils.FORECAST_API
                + this.getString(R.string.weather_api_key)
                + "&q=id:" + cityId + "&days=5";
        // Creating a Request object using the Builder pattern
        Request request = new Request.Builder()
                .url(url)
                .build();
        // Enqueue the request to be executed asynchronously
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Check if the exception is a timeout
                if (e instanceof java.net.SocketTimeoutException) {
                    // Show a Toast message to inform the user
                    runOnUiThread(() -> Toast.makeText(SearchActivity.this,
                            "Request timed out. Please try again.", Toast.LENGTH_LONG).show());
                } else if (e instanceof java.net.UnknownHostException) {
                    // Show a Toast message to inform the user
                    runOnUiThread(() -> Toast.makeText(SearchActivity.this,
                            "Unable to reach the server. " +
                                    "Please check your internet connection and try again.", Toast.LENGTH_LONG).show());
                } else {
                    // Handle other IOExceptions
                    runOnUiThread(() -> Toast.makeText(SearchActivity.this,
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
                        // Extract the minimum and maximum temperatures and icon code for the day
                        JsonObject dayObject = day.getAsJsonObject("day");
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
     * Gets the city ID based on the city name.
     *
     * @param cityName The name of the city.
     * @return The city ID if found, -1 otherwise.
     */
    private int getCityIdByName(String cityName) {
        for (Location location : locations) {
            if ((location.getName() + ", " + location.getCountry()).equals(cityName)) {
                return location.getId();
            }
        }
        return -1;
    }

    /**
     * Updates the UI elements for the current location weather
     * It runs on the UI thread
     *
     * @param currentTemp      The current location temperature
     * @param currentIconCode  The code for the weather icon
     * @param isDay            The day/night code (1 for day, 0 for night)
     * @param weatherCondition String describing weather conditions
     * @param lastUpdated      Date and time of last forecast updated data
     */
    private void updateCurrentWeatherUI(double currentTemp, int currentIconCode, int isDay, String weatherCondition, String lastUpdated) {
        runOnUiThread(() -> {
            currentTemperatureText.setText(String.format("%s°", currentTemp));
            Drawable icon = Utils.getWeatherIcon(SearchActivity.this, currentIconCode, isDay);
            Drawable background = Utils.getBackground(SearchActivity.this, currentIconCode, isDay);
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

            Drawable icon = Utils.getWeatherIcon(SearchActivity.this, iconCode, 1);
            iconView.setImageDrawable(icon);
            String formattedDate = Utils.formatDate(date);
            dateText.setText(formattedDate);
            minTempText.setText(String.format("%s°", minTemp));
            maxTempText.setText(String.format("%s°", maxTemp));
            favButton.setBackgroundResource(R.drawable.white_heart);
            // Set all the UI forecast elements as visible
            setVisibleUI(true);
        });
    }

    /**
     * Sets up the UI elements and initializes view IDs and listeners.
     */
    private void setUpUI() {
        mPreferences = getSharedPreferences(Utils.sharedPrefFile, MODE_PRIVATE);
        layout = findViewById(R.id.search_root_layout);
        currentLayout = findViewById(R.id.currentLayout);
        forecastTableLayout = findViewById(R.id.forecastTableLayout);
        favButton = findViewById(R.id.favButton);
        lastUpdatedLabel = findViewById(R.id.lastUpdateLabel2);
        lastUpdatedText = findViewById(R.id.lastUpdateText2);
        currentTemperatureText = findViewById(R.id.selectedTempText);
        currentConditionsText = findViewById(R.id.selectedConditionsText);
        currentConditionsIcon = findViewById(R.id.selectedConditionsIcon);
        // Arrays to hold the view IDs for each day's selected
        dateTextViewIds = new int[]{(R.id.selectedDateText1), (R.id.selectedDateText2),
                (R.id.selectedDateText3), (R.id.selectedDateText4), (R.id.selectedDateText5)};
        iconImageViewIds = new int[]{(R.id.selectedIconView1), (R.id.selectedIconView2),
                (R.id.selectedIconView3), (R.id.selectedIconView4), (R.id.selectedIconView5)};
        minTempTextViewIds = new int[]{(R.id.selectedMinTempText1), (R.id.selectedMinTempText2),
                (R.id.selectedMinTempText3), (R.id.selectedMinTempText4), (R.id.selectedMinTempText5)};
        maxTempTextViewIds = new int[]{(R.id.selectedMaxTempText1), (R.id.selectedMaxTempText2),
                (R.id.selectedMaxTempText3), (R.id.selectedMaxTempText4), (R.id.selectedMaxTempText5)};
        // Setting of the AutoCompleteTextView
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        // Create an ArrayAdapter to provide suggestions in the AutoCompleteTextView
        adapter = new ArrayAdapter<>(this, R.layout.custom_list_item, cityList);
        // Set the adapter to the AutoCompleteTextView
        autoCompleteTextView.setAdapter(adapter);
        // Set the threshold for showing suggestions
        // (number of characters to type before suggestions appear)
        autoCompleteTextView.setThreshold(3);
        // Add a TextWatcher to handle text changes in the AutoCompleteTextView
        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            // This method is called when the text is being changed
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // If the number of characters typed is 3 or more, fetch city suggestions
                if (s.length() >= 3) {
                    fetchCities(s.toString());
                }
                // If the number of characters typed is less than 3,
                // clear the city list and notify the adapter
                if (s.length() < 3) {
                    cityList.clear();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        // Set an item click listener to handle clicks on the suggested city names
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            // Get the selected city name from the suggestions
            String selectedCity = (String) parent.getItemAtPosition(position);
            // Get the city ID corresponding to the selected city name
            selectedCityId = getCityIdByName(selectedCity);
            // If a valid city ID is found, fetch the weather conditions for that city
            if (selectedCityId != -1) {
                fetchWeatherCondition(selectedCityId);
            }
        });

        // Listener fort Favourites button
        favButton.setOnClickListener(view -> {
            // Get the existing favorites from SharedPreferences as a JSON string
            String favoritesJson = mPreferences.getString("favorites", "[]");
            // Create a Gson instance for JSON conversion
            Gson gson = new Gson();
            // Define the type for a list of integers
            Type type = new TypeToken<List<Integer>>() {}.getType();
            // Deserialize the JSON string into a list of integers (favorite city IDs)
            List<Integer> favorites = gson.fromJson(favoritesJson, type);
            // Ensure the list size doesn't exceed 5 entries
            if (favorites.size() == 5) {
                // Show a Snackbar message indicating the favorite list is full
                showSnackbar(view, "Fav List is full, remove a location first");
                return;
            }
            // Add the new favorite city ID if it's not already present
            if (!favorites.contains(selectedCityId)) {
                favorites.add(selectedCityId);
                // Save the updated favorites list back to SharedPreferences
                SharedPreferences.Editor preferencesEditor = mPreferences.edit();
                String updatedFavoritesJson = gson.toJson(favorites);
                preferencesEditor.putString("favorites", updatedFavoritesJson);
                preferencesEditor.apply();
                // Set the save button icon to the red heart
                favButton.setBackgroundResource(R.drawable.red_heart);
                String name = autoCompleteTextView.getText().toString();
                // Inform the user
                showSnackbar(view, name + " added to Fav List");
            } else {
                showSnackbar(view, "Location already in Fav List");
            }
        });
    }

    /**
     * Sets the visibility of the UI elements based on the provided boolean parameter.
     *
     * @param isVisible If true, sets the views to VISIBLE; if false, sets the views to INVISIBLE.
     */
    private void setVisibleUI(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.INVISIBLE;
        currentTemperatureText.setVisibility(visibility);
        currentLayout.setVisibility(visibility);
        lastUpdatedLabel.setVisibility(visibility);
        lastUpdatedText.setVisibility(visibility);
        forecastTableLayout.setVisibility(visibility);
        favButton.setVisibility(visibility);
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
     * Shows a Snackbar with a message
     *
     * @param view The view to find a parent from.
     * @param message The message to be displayed
     */
    private void showSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        // Get the Snackbar TextView and customize it
        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextSize(25f); // Set text size
        textView.setTextColor(Color.parseColor("#C1E8FF")); // Set text color
        textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD); // Set text style
        textView.setGravity(Gravity.CENTER_HORIZONTAL); // Set text alignment
        snackbar.show();
    }
}