# WeatherNow

WeatherNow is an Android application based on <a href="https://www.weatherapi.com/" title="Free Weather API">WeatherAPI.com</a> web-service that provides real-time weather updates for your current location. The app features a sleek UI, a home screen widget, and allows users to search and set favorite locations for quick weather checks.

## Features

- Real-time weather updates based on your current location
- Weather info for a searched world location
- Favorite locations for quick weather checks
- Home screen widget

### Screenshots
<img src="https://github.com/pie-catt/WeatherNow/assets/33451203/8e18f8e0-c7f0-4de7-a8eb-e69e3dee6783" alt="Main Screen" width="180"/>
&ensp;
<img src="https://github.com/pie-catt/WeatherNow/assets/33451203/ae076575-25c4-4a01-868a-e3825550dbfd" alt="Main Screen2" width="180"/>
&ensp;
<img src="https://github.com/pie-catt/WeatherNow/assets/33451203/c59bb516-6d58-4cd8-b785-469656b11eed" alt="Main Screen3" width="180"/>
&ensp;
<img src="https://github.com/pie-catt/WeatherNow/assets/33451203/220849cd-9d98-4663-a0f1-07f415d343ed" alt="Main Screen4" width="180"/>

## Installation

1. Clone the repository:
```bash
git clone https://github.com/pie-catt/weathernow.git
```
2. Open the project in Android Studio.
  
3. Obtain an API key from the weather service provider <a href="https://www.weatherapi.com/" title="Free Weather API">WeatherAPI.com</a>.
   
4. Add your API key to the ```res/values/strings.xml``` file:
```xml
<string name="weather_api_key">YOUR_API_KEY_HERE</string>
```
6. Build and run the app on your emulator or Android device.

## Usage

To simulate the changing of the current location using the Android emulator, follow these steps:
1. Launch the app to get the current set location and fetch weather data.
   
2. Open the extended controls of the emulator.
   
3. Set a different location in the emulator's location settings.
   
4. In the app, tap the location icon to refresh the location and fetch the updated weather data for the new location.
   
   
