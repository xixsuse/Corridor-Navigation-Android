package amos.corridornavigation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.StepManeuver;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

// classes needed to add location layer

public class MainActivity extends MapContext {
    public ArrayAdapter<String> adapter;
    public AutoCompleteTextView autoCompleteTextView;
    private ArrayList<String> previous_autocomplete_results;
    private AutoCompleteTextView addressSearchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);

        initMapView(savedInstanceState);

        addressSearchBar = (AutoCompleteTextView)
                findViewById(R.id.main_searchbar_input);

        this.adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new String[] {""});
        this.adapter.setNotifyOnChange(true);
        addressSearchBar.setAdapter(adapter);

        addressSearchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    onSearchStart(s);
//
                }
            }
        });

        previous_autocomplete_results = new ArrayList<String>();
    }

    public void actionButtonPressed(View view) {
        if (super.originLocation != null) {
            this.setCameraPosition(super.originLocation);
        } else {
            Toast.makeText(this, R.string.user_location_not_available, Toast.LENGTH_LONG).show();
        }
    }

    public void onSearchStart(CharSequence s) {
        String addressPart = s.toString();
        getSuggestions(addressPart);
    }

    private void getSuggestions(String addressPart) {
        MapboxGeocoding client;
        ArrayList<String> autocomplete_results = new ArrayList<>();
        try {

            if (super.originLocation != null) {
                client = MapboxGeocoding.builder()
                        .accessToken(getString(R.string.access_token))
                        .proximity(Point.fromLngLat(super.originLocation.getLongitude(), super.originLocation.getLatitude()))
                        .query(addressPart)
                        .autocomplete(true)
                        .build();
            } else {
                client = MapboxGeocoding.builder()
                        .accessToken(getString(R.string.access_token))
                        .query(addressPart)
                        .autocomplete(true)
                        .build();
            }

            client.enqueueCall(new Callback<GeocodingResponse>() {
                @Override
                public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {

                    List<CarmenFeature> results = response.body().features();
                    if(results.size() > 1)
                    {
                        for (CarmenFeature result : results) {
                            try {
                                autocomplete_results.add(result.placeName()); //.substring(0, result.placeName().indexOf(","))
                            } catch (Exception e) {
                                System.err.println(e.getStackTrace());
                            }
                        }
//                        add_previous_results(autocomplete_results, addressPart);
                        String[] stockArr = new String[autocomplete_results.size()];
                        stockArr = autocomplete_results.toArray(stockArr);
//                        previous_autocomplete_results.clear();
//                        previous_autocomplete_results.addAll(autocomplete_results);
                        adapter.clear();
                        adapter.addAll(stockArr);
                        adapter.notifyDataSetChanged();
                        addressSearchBar.setAdapter(adapter);
                        addressSearchBar.showDropDown();
                    }
                }

                @Override
                public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                    Timber.e("Geocoding Failure: " + throwable.getMessage());
                }
            });

        } catch (ServicesException servicesException) {
            Timber.e("Error geocoding: " + servicesException.toString());
            servicesException.printStackTrace();
        }
    }

    private void add_previous_results(ArrayList<String> autocomplete_results, String adressPart) {
        for (String result : previous_autocomplete_results) {
            if (result.toLowerCase().startsWith(adressPart.toLowerCase()) && autocomplete_results.size() <= 5 && !autocomplete_results.contains(result)) {
                autocomplete_results.add(result);
            }
        }
        System.err.println("Yay !");
    }

    public void onSearchButtonClicked(View view) {

        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(this.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

        EditText editText = findViewById(R.id.main_searchbar_input);
        String address = editText.getText().toString();
        MapboxGeocoding client;
        try {
            if (super.originLocation != null) {
                client = MapboxGeocoding.builder()
                        .accessToken(getString(R.string.access_token))
                        .proximity(Point.fromLngLat(super.originLocation.getLongitude(), super.originLocation.getLatitude()))
                        .query(address)
                        .build();
            } else {
                client = MapboxGeocoding.builder()
                        .accessToken(getString(R.string.access_token))
                        .query(address)
                        .build();
            }
            client.enqueueCall(new Callback<GeocodingResponse>() {
                @Override
                public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {

                    List<CarmenFeature> results = response.body().features();
                    if (results.size() > 1) {

                        LatLng latLng = new LatLng();
                        latLng.setLatitude(results.get(0).center().latitude());
                        latLng.setLongitude(results.get(0).center().longitude());
                        onMapClick(latLng);
                        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13.0)); // mapboxMap came from MapContext
                    }
                }

                @Override
                public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                    Timber.e("Geocoding Failure: " + throwable.getMessage());
                }
            });

        } catch (ServicesException servicesException) {
            Timber.e("Error geocoding: " + servicesException.toString());
            servicesException.printStackTrace();
        }

    }

    public void onNavigateButtonClicked(View view) {

        try {
            boolean simulateRoute = true;
            NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                    .directionsRoute(super.locationMarker.currentRoute)
                    .shouldSimulateRoute(simulateRoute)
                    .build();


            int max=options.directionsRoute().legs().get(0).steps().size();
            for(int i = 0; i<max;i++) {
                Log.d("instruction", options.directionsRoute().legs().get(0).steps().get(i).maneuver().instruction());
            }

            // Call this method with Context from within an Activity
            NavigationLauncher.startNavigation(this, options);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "You may not have selected a route yet.", Toast.LENGTH_LONG).show();
        }
    }
}
