package com.ozzox.featureapptesting

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.ozzox.featureapptesting.ui.theme.FeatureAppTestingTheme

class MainActivity : ComponentActivity() {

    private lateinit var splitInstallManager: SplitInstallManager
    private val moduleName = "dynamicfeature"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splitInstallManager = SplitInstallManagerFactory.create(this)

        setContent {
            FeatureAppTestingTheme {
                var status by remember { mutableStateOf("Idle") }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Module Status: $status")
                        Button(onClick = {
                            installAndLaunchModule { newStatus ->
                                status = newStatus
                            }
                        }) {
                            Text(text = "Install & Launch Dynamic Feature")
                        }
                    }
                }
            }
        }
    }

    private fun installAndLaunchModule(onStatusUpdate: (String) -> Unit) {
        if (splitInstallManager.installedModules.contains(moduleName)) {
            onStatusUpdate("Already installed")
            launchActivity()
            return
        }

        val request = SplitInstallRequest.newBuilder()
            .addModule(moduleName)
            .build()

        val listener = object : SplitInstallStateUpdatedListener {
            override fun onStateUpdate(state: com.google.android.play.core.splitinstall.SplitInstallSessionState) {
                when (state.status()) {
                    SplitInstallSessionStatus.DOWNLOADING -> onStatusUpdate("Downloading...")
                    SplitInstallSessionStatus.INSTALLING -> onStatusUpdate("Installing...")
                    SplitInstallSessionStatus.INSTALLED -> {
                        onStatusUpdate("Installed")
                        splitInstallManager.unregisterListener(this)
                        launchActivity()
                    }
                    SplitInstallSessionStatus.FAILED -> {
                        onStatusUpdate("Failed: ${state.errorCode()}")
                        splitInstallManager.unregisterListener(this)
                    }
                }
            }
        }

        splitInstallManager.registerListener(listener)
        splitInstallManager.startInstall(request)
            .addOnFailureListener {
                onStatusUpdate("Error: ${it.message}")
            }
    }

    private fun launchActivity() {
        // Use Intent.setClassName or reflection to start the activity from the dynamic module
        val intent = Intent().setClassName(
            packageName,
            "com.ozzox.dynamicfeature.ItemDetailHostActivity"
        )
        startActivity(intent)
    }
}
