package com.dhyey.fanfic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.dhyey.fanfic.navigation.FanficNavGraph
import com.dhyey.fanfic.sync.SyncManager
import com.dhyey.fanfic.ui.theme.FanficReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var ficDao: com.dhyey.fanfic.storage.dao.FicDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // No more seed data

        // Sync on app startup if already logged in
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            lifecycleScope.launch {
                try {
                    syncManager.sync()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        setContent {
            FanficReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FanficNavGraph()
                }
            }
        }
    }
}
