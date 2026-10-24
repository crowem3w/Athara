package social.ahtara.app.location

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.domain.placesFromJson
import social.ahtara.authentication.domain.toJsonString
import social.ahtara.authentication.ui.theme.AuthenticationTheme
import social.ahtara.app.location.ui.MapRoute
import org.maplibre.android.MapLibre






class LocationSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        MapLibre.getInstance(this)

        val accountType = AccountType.fromName(intent.getStringExtra(EXTRA_ACCOUNT_TYPE))
        val initial = placesFromJson(intent.getStringExtra(EXTRA_INITIAL_PLACES))
        setContent {
            AuthenticationTheme(accountType) {
                MapRoute(
                    initialPlaces = initial,
                    onBack = { finish() },
                    onConfirm = { places ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(EXTRA_RESULT_PLACES, places.toJsonString()),
                        )
                        finish()
                    },
                )
            }
        }
    }

    internal companion object {
        const val EXTRA_ACCOUNT_TYPE = "account_type"
        const val EXTRA_INITIAL_PLACES = "initial_places"
        const val EXTRA_RESULT_PLACES = "result_places"
    }
}






class PickLocationContract(
    private val accountType: AccountType = AccountType.PERSONAL,
) : ActivityResultContract<List<PlaceSelection>, List<PlaceSelection>?>() {

    override fun createIntent(context: Context, input: List<PlaceSelection>): Intent =
        Intent(context, LocationSelectionActivity::class.java)
            .putExtra(LocationSelectionActivity.EXTRA_ACCOUNT_TYPE, accountType.name)
            .putExtra(LocationSelectionActivity.EXTRA_INITIAL_PLACES, input.toJsonString())

    override fun parseResult(resultCode: Int, intent: Intent?): List<PlaceSelection>? {
        if (resultCode != Activity.RESULT_OK) return null
        return placesFromJson(intent?.getStringExtra(LocationSelectionActivity.EXTRA_RESULT_PLACES))
            .takeIf { it.isNotEmpty() }
    }
}