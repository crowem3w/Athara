package org.example.shared.authentication.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.shared.designsystem.AppTheme

/**
 * Account type selection, shown after onboarding and before the guest Home.
 * Finishes with RESULT_OK and [EXTRA_ACCOUNT_TYPE] set, or RESULT_CANCELED when the user backs out.
 */
class AccountTypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(start = 24.dp, end = 24.dp, top = 24.dp),
                    ) {
                        AccountTypeScreen(
                            onSelect = { type ->
                                setResult(RESULT_OK, Intent().putExtra(EXTRA_ACCOUNT_TYPE, type.name))
                                finish()
                            },
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ACCOUNT_TYPE = "org.example.shared.authentication.ACCOUNT_TYPE"
    }
}
