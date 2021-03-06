package fr.atraore.weather_forecast.ui.weather.current

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import fr.atraore.weather_forecast.R
import fr.atraore.weather_forecast.data.internals.WeatherCodes
import fr.atraore.weather_forecast.data.provider.CUSTOM_LOCATION
import fr.atraore.weather_forecast.data.provider.PreferenceProvider
import fr.atraore.weather_forecast.ui.base.ScopedFragment
import kotlinx.android.synthetic.main.current_weather_fragment.*
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.*

class CurrentWeatherFragment : ScopedFragment(), KodeinAware {
  override val kodein by closestKodein()
  private val viewModelFactory by instance<CurrentWeatherViewModelFactory>()
  private var isOnEditMode: Boolean = false

  companion object {
    fun newInstance() =
      CurrentWeatherFragment()
  }

  private lateinit var viewModel: CurrentWeatherViewModel

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.current_weather_fragment, container, false)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    viewModel = ViewModelProvider(this, viewModelFactory).get(CurrentWeatherViewModel::class.java)
    bindUI()
    bindUIEvents()
  }

  private fun bindUIEvents() {
    btn_edit.setOnClickListener {
      if (isOnEditMode) {
        if (edit_city.text.isNotEmpty()) {
          val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
          with(sharedPref.edit()) {
            putString(CUSTOM_LOCATION, edit_city.text.toString())
            apply()
          }
        }
      }
      isOnEditMode = !isOnEditMode
      updateCityVisibility()
    }
  }

  private fun bindUI() = launch {
    val currentWeather = viewModel.weather.await()
    val weatherLocation = viewModel.weatherLocation.await()

    weatherLocation.observe(viewLifecycleOwner, Observer {location ->
      if (location == null) return@Observer
      updateCity(location.name, location.country)
    })

    currentWeather.observe(viewLifecycleOwner, Observer {
      if (it == null) return@Observer

      group_loading.visibility = View.GONE
      updateTemperature(it.temperature, it.feelsLike)
      updatePrecipitation(it.precip)
      updateWind(it.windDir, it.windSpeed)
      updatePressure(it.pressure)
      updateCondition(it.weatherCode)
      updateVisibility(it.visibility)
      updateDate()
    })
  }

  private fun updateCity(city: String, country: String) {
      textview_city.text = "$city, $country"
  }

  private fun updateCityVisibility() {
    if (isOnEditMode) {
      textview_city.visibility = View.GONE
      edit_city.visibility = View.VISIBLE
    } else {
      textview_city.visibility = View.VISIBLE
      edit_city.visibility = View.GONE
    }
  }

  private fun updateDate() {
    val dateTimeFormatter = DateTimeFormatter
      .ofLocalizedDateTime(FormatStyle.SHORT)
      .withLocale(Locale.FRANCE)

    textview_today.text = ZonedDateTime.now().format(dateTimeFormatter)
  }

  private fun updateTemperature(temperature: Int, feelsLike: Int) {
    textview_temperature.text = "$temperature°C"
  }

  private fun updateCondition(weatherCode: Int) {
    if (WeatherCodes.cloudyCodes.contains(weatherCode)) {
      imageview_condition_icon.setImageResource(R.drawable.ic_cloudy_white)
    } else if (WeatherCodes.rainCodes.contains(weatherCode)) {
      imageview_condition_icon.setImageResource(R.drawable.ic_rain_white)
    } else if (WeatherCodes.snowCodes.contains(weatherCode)) {
      imageview_condition_icon.setImageResource(R.drawable.ic_snowflake_white)
    } else if (WeatherCodes.sunnyCodes.contains(weatherCode)) {
      imageview_condition_icon.setImageResource(R.drawable.ic_sun_white)
    } else {
      imageview_condition_icon.setImageResource(R.drawable.ic_cloudy_white)
    }
  }

  private fun updatePrecipitation(precipitationVolume: Int) {
    textview_precipitation.text = "Précipitation: $precipitationVolume mm"
  }

  private fun updateWind(windDirection: String, windSpeed: Int) {
    textview_wind.text = "Vent: $windDirection, $windSpeed kph"
  }

  private fun updatePressure(pressure: Int) {
    textview_pressure.text = "Pression: $pressure hPa"
  }

  private fun updateVisibility(visibilityDistance: Int) {
    textview_visibility.text = "Visibilité: $visibilityDistance km"
  }
}
