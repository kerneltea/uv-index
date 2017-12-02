package net.epictimes.uvindex.query

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.ResultReceiver
import net.epictimes.uvindex.data.model.LatLng
import timber.log.Timber
import java.io.IOException
import java.util.*


class FetchAddressIntentService : IntentService("FetchAddressIntentService") {

    companion object {
        private val MAX_RESULTS = 1

        private val PACKAGE_NAME = "net.epictimes.uvindex.query"
        val KEY_RESULT_RECEIVER = PACKAGE_NAME + ".RESULT_RECEIVER"
        val KEY_LOCATION = PACKAGE_NAME + ".LOCATION"
        val KEY_MESSAGE = PACKAGE_NAME + ".MESSAGE"

        fun startIntentService(context: Context,
                               resultReceiver: ResultReceiver,
                               latLng: LatLng) {
            val intent = Intent(context, FetchAddressIntentService::class.java)
            intent.putExtra(KEY_RESULT_RECEIVER, resultReceiver)
            intent.putExtra(KEY_LOCATION, latLng)
            context.startService(intent)
        }
    }

    override fun onHandleIntent(intent: Intent) {
        val resultReceiver = intent.getParcelableExtra<ResultReceiver>(KEY_RESULT_RECEIVER)
        val latLng = intent.getParcelableExtra<LatLng>(KEY_LOCATION)

        val geocoder = Geocoder(this, Locale.getDefault())

        var errorMessage = ""
        val addresses = mutableListOf<Address>()

        try {
            val results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, MAX_RESULTS)
            addresses.addAll(results)
        } catch (ioException: IOException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.address_service_not_available)
            Timber.e(errorMessage, ioException)
        } catch (illegalArgumentException: IllegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.address_service_invalid_coordinates)
            Timber.e("$errorMessage. Latitude = ${latLng.latitude}, Longitude = ${latLng.longitude}",
                    illegalArgumentException)
        }

        // Handle case where no address was found.
        if (addresses.isEmpty()) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.address_service_no_address_found)
                Timber.e(errorMessage)
            }
            deliverResultToReceiver(resultReceiver, AddressFetchResult.FAIL, errorMessage)
        } else {
            Timber.i(getString(R.string.address_service_address_found))
            val address: Address = addresses.first()
            deliverResultToReceiver(resultReceiver, AddressFetchResult.SUCCESS,
                    "${address.adminArea}, ${address.countryName}")
        }
    }

    private fun deliverResultToReceiver(resultReceiver: ResultReceiver,
                                        fetchResult: AddressFetchResult,
                                        message: String) {
        val bundle = Bundle()
        bundle.putString(KEY_MESSAGE, message)
        resultReceiver.send(fetchResult.ordinal, bundle)
    }
}
