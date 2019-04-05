package things.com.teamgeny.androidthings2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.animation.LinearInterpolator
import android.widget.TextView

import com.google.android.things.contrib.driver.apa102.Apa102
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.pwmspeaker.Speaker
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import kotlinx.android.synthetic.main.activity_thing.*

import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Random

class ThingActivity : AppCompatActivity() {

    private var buttonAInputDriver: ButtonInputDriver? = null
    private var buttonB: Button? = null
    private var buttonC: Button? = null

    private var ledGpioRed: Gpio? = null
    private var ledGpioBlue: Gpio? = null
    private var ledGpioGreen: Gpio? = null

    private var ledstrip: Apa102? = null
    private val NUM_LEDS = 7
    private val mRainbow = IntArray(NUM_LEDS)
    private var rainbowOrder = true

    private var alphaDisplay: AlphanumericDisplay? = null
    private var displayMode = DisplayMode.TEMPERATURE
    private val useFarenheit = true

    private var speaker: Speaker? = null
    private val SPEAKER_READY_DELAY_MS = 300
    private val isSpeakerMute = false

    private var environmentalSensorDriver: Bmx280SensorDriver? = null
    private var sensorManager: SensorManager? = null
    private var lastTemperature: Float = 0.toFloat()
    private var lastPressure: Float = 0.toFloat()

    private var pressureTxt: TextView? = null

    /**
     * Callback for buttonB events.
     */
    private val buttonCallbackB = Button.OnButtonEventListener { button, pressed ->
        if (pressed) {
            displayMode = DisplayMode.PRESSURE
            Log.d(TAG, "button B pressed")
            val rand = Random()
            val colors = IntArray(NUM_LEDS)
            colors[0] = mRainbow[rand.nextInt(NUM_LEDS)]
            colors[1] = mRainbow[rand.nextInt(NUM_LEDS)]
            colors[2] = mRainbow[rand.nextInt(NUM_LEDS)]
            colors[3] = mRainbow[rand.nextInt(NUM_LEDS)]
            colors[4] = mRainbow[rand.nextInt(NUM_LEDS)]
            colors[5] = mRainbow[rand.nextInt(NUM_LEDS)]
            colors[6] = mRainbow[rand.nextInt(NUM_LEDS)]

            soundSpeaker(SOUND_MED)
            runLedStrip(colors)
            showLED(GREEN_LED)
        }
    }

    /**
     * Callback for buttonC events.
     */
    private val buttonCallbackC = Button.OnButtonEventListener { button, pressed ->
        if (pressed) {
            Log.d(TAG, "button C pressed")
            displayMode = DisplayMode.CLEAR
            updateDisplay(CLEAR_DISPLAY)
            soundSpeaker(SOUND_HIGH)
            clearLedStrip()
            showLED(BLUE_LED)
        }
    }

    // Callback used when we register the BMP280 sensor driver with the system's SensorManager.
    private val dynamicSensorCallback = object : SensorManager.DynamicSensorCallback() {
        override fun onDynamicSensorConnected(sensor: Sensor) {
            if (sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                Log.d(TAG, "Ambient temp sensor connected and is receiving temperature data")
                sensorManager!!.registerListener(temperatureListener, sensor,
                        SensorManager.SENSOR_DELAY_NORMAL)


            } else if (sensor.type == Sensor.TYPE_PRESSURE) {
                Log.d(TAG, "Pressure sensor connected and is receiving temperature data")
                sensorManager!!.registerListener(pressureListener, sensor,
                        SensorManager.SENSOR_DELAY_NORMAL)
            }
        }

        override fun onDynamicSensorDisconnected(sensor: Sensor) {
            super.onDynamicSensorDisconnected(sensor)
        }
    }

    // Callback when SensorManager delivers temperature data.
    private val temperatureListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            lastTemperature = event.values[0]
            if (useFarenheit) {
                text_temperature!!.text = "Current Temperature in Farenheit (time reported):\n    " + convertCelciusToFahrenheit(lastTemperature)
            } else {
                text_temperature!!.text = "Current Temperature in Celcius (time reported):\n    " + lastTemperature
            }


            if (displayMode == DisplayMode.TEMPERATURE) {
                updateDisplay(convertCelciusToFahrenheit(lastTemperature))
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Log.d(TAG, "Temp accuracy changed: $accuracy")
        }
    }

    // Callback when SensorManager delivers pressure data.
    private val pressureListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            lastPressure = event.values[0]
            pressureTxt!!.text = "Barometric Pressure in hectoPascals (time reported):\n    " + lastPressure / 100
            if (displayMode == DisplayMode.PRESSURE) {

                updateDisplay(lastPressure)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Log.d(TAG, "Pressure accuracy changed: $accuracy")
        }
    }

    private enum class DisplayMode {
        TEMPERATURE,
        PRESSURE,
        CLEAR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thing)

        Log.d(TAG, "Hello Android Things!")

        // Initialize buttons
        // Method1 to handle key presses, using Input driver, handle Key DOWN/UP
        try {
            buttonAInputDriver = ButtonInputDriver(BoardDefaults.gpioForBtnA,
                    Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_A)
            buttonAInputDriver!!.register()
            Log.d(TAG, "Button A registered, will generate KEYCODE_A")
        } catch (e: IOException) {
            throw RuntimeException("Error initializing GPIO button A", e)
        }

        // Another way to register button events, simpler, but handles single press event
        try {
            buttonB = Button(BoardDefaults.gpioForBtnB,
                    Button.LogicState.PRESSED_WHEN_LOW)
            buttonB!!.setOnButtonEventListener(buttonCallbackB)

            buttonC = Button(BoardDefaults.gpioForBtnC,
                    Button.LogicState.PRESSED_WHEN_LOW)
            buttonC!!.setOnButtonEventListener(buttonCallbackC)
        } catch (e: IOException) {
            Log.e(TAG, "button driver error", e)
        }

        //GPIO Individual Color LED
        try {
            val service = PeripheralManager.getInstance()
            ledGpioRed = service.openGpio(BoardDefaults.gpioForRedLED)
            ledGpioGreen = service.openGpio(BoardDefaults.gpioForGreenLED)
            ledGpioBlue = service.openGpio(BoardDefaults.gpioForBlueLED)
        } catch (e: IOException) {
            throw RuntimeException("Problem connecting to IO Port", e)
        }

        //SPI LED Lightstrip and rainbow color array
        for (i in 0 until NUM_LEDS) {
            val hsv = floatArrayOf(i * 360f / NUM_LEDS, 1.0f, 1.0f)
            mRainbow[i] = Color.HSVToColor(255, hsv)
        }
        try {
            ledstrip = Apa102(BoardDefaults.spiBus, Apa102.Mode.BGR)
            ledstrip!!.brightness = LEDSTRIP_BRIGHTNESS
        } catch (e: IOException) {
            ledstrip = null // Led strip is optional.
        }

        // Alphanumeric Display
        try {
            alphaDisplay = AlphanumericDisplay(BoardDefaults.i2cBus)
            alphaDisplay!!.setEnabled(true)
            alphaDisplay!!.clear()
            Log.d(TAG, "Initialized I2C Display")
        } catch (e: IOException) {
            Log.e(TAG, "Error initializing display", e)
            Log.d(TAG, "Display disabled")
            alphaDisplay = null
        }

        // PWM speaker
        try {
            speaker = Speaker(BoardDefaults.speakerPwmPin)
            soundSpeaker(1)
            Log.d(TAG, "Initialized PWM speaker")
        } catch (e: IOException) {
            throw RuntimeException("Error initializing PWM speaker", e)
        }

        // I2C Sensors - Temperature and Pressure
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        try {
            environmentalSensorDriver = Bmx280SensorDriver(BoardDefaults.i2cBus)
            sensorManager!!.registerDynamicSensorCallback(dynamicSensorCallback)
            environmentalSensorDriver!!.registerTemperatureSensor()
            environmentalSensorDriver!!.registerPressureSensor()
            Log.d(TAG, "Initialized I2C BMP280")
        } catch (e: IOException) {
            throw RuntimeException("Error initializing BMP280", e)
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        //Button
        if (buttonAInputDriver != null) {
            try {
                buttonAInputDriver!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            buttonAInputDriver = null
        }

        if (buttonB != null) {
            // TODO
        }

        // GPIO LEDS
        try {
            ledGpioRed!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            ledGpioBlue!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            ledGpioGreen!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

            ledGpioRed!!.close()
            ledGpioBlue!!.close()
            ledGpioGreen!!.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error on PeripheralIO API", e)
        }

        // LED Lightstrip
        try {
            if (ledstrip != null) {
                try {
                    ledstrip!!.write(IntArray(NUM_LEDS))
                    ledstrip!!.brightness = 0
                    ledstrip!!.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error disabling ledstrip", e)
                } finally {
                    ledstrip = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error on closing LED strip", e)
        }

        // Alphanumeric Display
        if (alphaDisplay != null) {
            try {
                alphaDisplay!!.clear()
                alphaDisplay!!.setEnabled(false)
                alphaDisplay!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error disabling display", e)
            } finally {
                alphaDisplay = null
            }
        }

        // Clean up sensor registrations
        sensorManager!!.unregisterListener(temperatureListener)
        sensorManager!!.unregisterListener(pressureListener)
        sensorManager!!.unregisterDynamicSensorCallback(dynamicSensorCallback)

        // Clean up peripheral.
        if (environmentalSensorDriver != null) {
            try {
                environmentalSensorDriver!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            environmentalSensorDriver = null
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_A) {
            Log.d(TAG, "The button A event was received KEY DOWN")
            displayMode = DisplayMode.TEMPERATURE
            val colors = IntArray(NUM_LEDS)
            // Switches the rainbow from left to right on each press
            if (rainbowOrder) {
                rainbowOrder = false
                colors[0] = mRainbow[6]
                colors[1] = mRainbow[5]
                colors[2] = mRainbow[4]
                colors[3] = mRainbow[3]
                colors[4] = mRainbow[2]
                colors[5] = mRainbow[1]
                colors[6] = mRainbow[0]
            } else {
                rainbowOrder = true
                colors[0] = mRainbow[0]
                colors[1] = mRainbow[1]
                colors[2] = mRainbow[2]
                colors[3] = mRainbow[3]
                colors[4] = mRainbow[4]
                colors[5] = mRainbow[5]
                colors[6] = mRainbow[6]
            }
            soundSpeaker(SOUND_LOW)
            runLedStrip(colors)
            showLED(RED_LED)

            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_A) {
            Log.d(TAG, "The button A event was received KEY UP")
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    /**
     * Helper Method to turn one of 3 LEDs, and turn off the others
     * @param ledType
     */
    private fun showLED(ledType: Int) {
        try {
            ledGpioRed!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            ledGpioBlue!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            ledGpioGreen!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

            when (ledType) {
                1 -> ledGpioRed!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH)
                2 -> ledGpioBlue!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH)
                3 -> ledGpioGreen!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun runLedStrip(colors: IntArray) {
        try {
            ledstrip!!.write(colors)
            ledstrip!!.brightness = LEDSTRIP_BRIGHTNESS
        } catch (e: IOException) {
            Log.e(TAG, "Error setting ledstrip", e)
        }

    }

    private fun clearLedStrip() {
        try {
            ledstrip!!.write(IntArray(NUM_LEDS))
            ledstrip!!.brightness = 0
        } catch (e: IOException) {
            Log.e(TAG, "Error setting ledstrip", e)
        }

    }

    private fun soundSpeaker(soundType: Int) {
        if (!isSpeakerMute) {
            val soundVal = soundType * 100

            val slide = ValueAnimator.ofFloat(soundVal.toFloat(), (440 * 4).toFloat())

            slide.duration = 50
            slide.repeatCount = 5
            slide.interpolator = LinearInterpolator()
            slide.addUpdateListener { animation ->
                try {
                    val v = animation.animatedValue as Float
                    speaker!!.play(v.toDouble())
                } catch (e: IOException) {
                    throw RuntimeException("Error sliding speaker", e)
                }
            }
            slide.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    try {
                        speaker!!.stop()
                    } catch (e: IOException) {
                        throw RuntimeException("Error sliding speaker", e)
                    }

                }
            })
            val handler = Handler(mainLooper)
            handler.postDelayed({ slide.start() }, SPEAKER_READY_DELAY_MS.toLong())
        }
    }

    private fun updateDisplay(value: Float) {
        if (alphaDisplay != null) {
            try {
                if (displayMode == DisplayMode.PRESSURE) {
                    if (value > BAROMETER_RANGE_HIGH) {
                        alphaDisplay!!.display("HIGH")
                    } else if (value < BAROMETER_RANGE_LOW) {
                        alphaDisplay!!.display("LOW")
                    } else {
                        alphaDisplay!!.display("MED")
                    }
                } else if (displayMode == DisplayMode.CLEAR) {
                    alphaDisplay!!.clear()
                } else {
                    alphaDisplay!!.display(value.toDouble())
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error setting display", e)
            }

        }
    }

    companion object {
        private val TAG = "ThingActivity"
        private val RED_LED = 1
        private val BLUE_LED = 2
        private val GREEN_LED = 3
        private val LEDSTRIP_BRIGHTNESS = 1
        private val CLEAR_DISPLAY = 73638.45f
        private val SOUND_LOW = 1
        private val SOUND_MED = 4
        private val SOUND_HIGH = 8
        private val BAROMETER_RANGE_LOW = 965f
        private val BAROMETER_RANGE_HIGH = 1035f

        /**
         * A utility method to return current IP
         *
         * @param useIPv4
         * @return
         */
        private fun getIPAddress(useIPv4: Boolean): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress) {
                            val sAddr = addr.hostAddress
                            val isIPv4 = sAddr.indexOf(':') < 0

                            if (useIPv4) {
                                if (isIPv4)
                                    return sAddr
                            } else {
                                if (!isIPv4) {
                                    val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                                    return if (delim < 0) sAddr.toUpperCase() else sAddr.substring(0, delim).toUpperCase()
                                }
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Exception received getting IP info: $ex", ex)
            }

            return "NO IP ADDRESS FOUND"
        }
    }


}
