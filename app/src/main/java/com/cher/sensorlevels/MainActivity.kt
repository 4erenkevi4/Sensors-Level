package com.cher.sensorlevels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.IOException


class MainActivity : ComponentActivity() {

    private val viewModel: SensorViewModel by viewModel()


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.findViewById<ComposeView>(R.id.compose_view).apply {
            setContent {
                Greeting(context, 0f)
            }
        }
        viewModel.gsmLevel.observe(this) {
            this.findViewById<ComposeView>(R.id.compose_view).apply {
                setContent {
                    Greeting(context, it, true)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Composable
    private fun Greeting(context: Context, fl: Float?, isRepeat: Boolean = false) {
        val onclickState = remember {
            mutableStateOf(isRepeat)
        }

        val test = viewModel.wifiLevel.observeAsState()
        val test3 = viewModel.gsmLevel.observeAsState()
        val test4 = viewModel.micVolumeLevel.observeAsState()
        val nameOperator = viewModel.operatorName.observeAsState()
        Column() {
            CreateCardInfo(
                SensorTypes.GSM,
                fl ?: 0f,
                nameOperator.value

            )
            CreateCardInfo(
                SensorTypes.WIFI,
                test.value ?: 0f,
            )
            CreateCardInfo(
                SensorTypes.MIC,
                test4.value ?: 0f,
                startScanning = onclickState.value,
                context = context
            )

            Button(
                onClick = { onclickState.value = true },
                modifier = Modifier
                    .padding(16.dp)
                    .align(CenterHorizontally),
                enabled = !onclickState.value
            ) {
                Text(text = "Start")
            }
            if (onclickState.value) {
                startScanning(context)
            }
            Button(
                onClick = {
                    onclickState.value = false
                    viewModel.updateGSMLevel(0f)
                    viewModel.updateWifiLevel(0f)
                    viewModel.updateMICLevel(0f)
                },
                modifier = Modifier
                    .padding(16.dp)
                    .align(CenterHorizontally),
                enabled = onclickState.value
            ) {
                Text(text = "Stop")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startScanning(context: Context) {
        viewModel.updateWifiLevel(getWifiLevel(context))
        viewModel.updateGSMLevel(getGSMlevel(context))
        getMicVolumeLevel(context)
    }

    private fun getWifiLevel(context: Context): Float {
        val wifiManager =
            context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val numberOfLevels = 10
        val wifiInfo = wifiManager.connectionInfo
        return WifiManager.calculateSignalLevel(wifiInfo.rssi, numberOfLevels).toFloat()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun getGSMlevel(context: Context): Float {
        val all = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        viewModel.updateOperatorName(all.networkOperatorName)
        val result = all.signalStrength?.level ?: 0f
        return result.toFloat()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getMicVolumeLevel(context: Context) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            getMicLevel(context)
        } else {
            checkAudioPermission(context)
        }
    }

    override fun onStart() {
        super.onStart()
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                getMicLevel(this)
            } else {
                Toast.makeText(
                    this,
                    "Для измерения уровня звука неоходимо разрешение", Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private fun getMicLevel(context: Context) {
        val mRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            return
        }
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        File.createTempFile("musicfiles", "3GPP")
        mRecorder.setOutputFile(getFileStreamPath("musicfiles.3GPP"))
        try {
            mRecorder.prepare()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mRecorder.start()
        lifecycleScope.launch {
            delay(500)
            mRecorder.maxAmplitude
            delay(300)
            val amplitude = mRecorder.maxAmplitude
            viewModel.updateMICLevel(amplitude.toFloat() / 27000f)
        }

        mRecorder.pause()
        getFileStreamPath("musicfiles.3GPP").deleteOnExit()
    }


    private fun checkAudioPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(
                    this,
                    "App required access to audio", Toast.LENGTH_SHORT
                ).show();
            }
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }


    @RequiresApi(Build.VERSION_CODES.P)
    @Composable
    fun CreateCardInfo(
        sensorType: SensorTypes,
        signalLevel: Float,
        description: String? = null,
        startScanning: Boolean = false,
        context: Context? = null
    ) {
        val checkedState = remember { mutableStateOf(true) }
        Card(
            modifier = Modifier
                .padding(15.dp),
            elevation = 10.dp
        ) {
            Column {
                Row(modifier = Modifier.padding(16.dp)) {
                    Image(painter = painterResource(sensorType.imageId), contentDescription = null)

                    Column(verticalArrangement = Arrangement.Top) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = sensorType.sensorName
                        )

                        if (sensorType.description.isNullOrEmpty()
                                .not() || description.isNullOrEmpty().not()
                        )
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp),
                                color = Color.Gray,
                                text = sensorType.description ?: description!!,
                            )
                    }


                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = checkedState.value,
                        onCheckedChange = { checkedState.value = it })
                }
                if(checkedState.value) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {

                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(
                                    CircleShape
                                ), progress = signalLevel,
                            color = sensorType.colorProgress
                        )
                    }
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .fillMaxWidth()
                    ) {
                        DrowProgressItem(true)
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem()
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem(true)
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem()
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem(true)
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem()
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem(true)
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem()
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem(true)
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem()
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem(true)
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem()
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem(true)
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem()
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem(true)
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem()
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem(true)
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem()
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem(true)
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem()
                        Spacer(modifier = Modifier.weight(1f))
                        DrowProgressItem(true)
                    }
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .fillMaxWidth()
                    ) {

                        Text(text = "10", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "20", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "30", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "40", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "50", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "60", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "70", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "80", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "90", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "100 ", color = Color.Gray, fontSize = 10.sp)

                        Text(
                            text = sensorType.parametr,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )

                    }
                }
            }
        }
        context?.let {
            if (startScanning)
                startScanning(it)
        }
    }


    @Composable
    private fun DrowProgressItem(isCharded: Boolean = false) {
        val height = if (isCharded) 10.dp else 6.dp
        Canvas(
            modifier = Modifier
                .size(2.dp, height)
                .clip(shape = RoundedCornerShape(1.dp))
                .fillMaxWidth()
        ) {
            drawRect(
                color = Color.Gray,
                size = Size(size.width, size.height),
            )

        }
    }

    enum class SensorTypes(
        val imageId: Int,
        val sensorName: String,
        val description: String?,
        val colorProgress: Color,
        val parametr: String,
        val signalLevel: Float
    ) {
        GSM(
            imageId = R.drawable.ic_baseline_signal_cellular_alt_24,
            sensorName = "GSM",
            description = null,
            colorProgress = Color.Blue,
            parametr = "dbm",
            signalLevel = 0.6f
        ),
        WIFI(
            imageId = R.drawable.ic_baseline_wifi_24,
            sensorName = "Wi-fi",
            description = "2.4 Ghz",
            colorProgress = Color.Cyan,
            parametr = "dbm",
            signalLevel = 0.3f
        ),
        MIC(
            imageId = R.drawable.ic_baseline_mic_24,
            sensorName = "Microphone",
            description = "10 Hz - 24 Khz",
            colorProgress = Color.Red,
            parametr = "db",
            signalLevel = 0.2f
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Preview
    @Composable
    fun Preview() {
        Column() {
            CreateCardInfo(
                SensorTypes.GSM,
                0.3f,
            )
            CreateCardInfo(
                SensorTypes.WIFI,
                0.2f,
            )
            CreateCardInfo(
                SensorTypes.MIC,
                0.5f,
            )
            Button(
                onClick = { startScanning(this@MainActivity) },
                modifier = Modifier
                    .padding(16.dp)
                    .align(CenterHorizontally)
            ) {
                Text(text = "Start")
            }
        }

    }
}


