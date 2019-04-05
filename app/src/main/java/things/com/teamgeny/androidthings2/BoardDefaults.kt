package things.com.teamgeny.androidthings2

import android.os.Build
import com.google.android.things.pio.PeripheralManager


object BoardDefaults {

    private val DEVICE_EDISON_ARDUINO = "edison_arduino"
    private val DEVICE_EDISON = "edison"
    private val DEVICE_JOULE = "joule"
    private val DEVICE_RPI3 = "rpi3"
    private val DEVICE_PICO = "imx6ul_pico"
    private val DEVICE_VVDN = "imx6ul_iopb"
    private val DEVICE_NXP = "imx6ul"
    private var sBoardVariant = ""

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    val gpioForRedLED: String
        get() {
            when (boardVariant) {
                DEVICE_EDISON_ARDUINO -> return "IO13"
                DEVICE_EDISON -> return "GP45"
                DEVICE_RPI3 -> return "BCM6"
                DEVICE_NXP -> return "GPIO4_IO20"
                else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
            }
        }

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    val gpioForGreenLED: String
        get() {
            when (boardVariant) {
                DEVICE_EDISON_ARDUINO -> return "IO13"
                DEVICE_EDISON -> return "GP45"
                DEVICE_RPI3 -> return "BCM19"
                DEVICE_NXP -> return "GPIO4_IO20"
                else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
            }
        }

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    val gpioForBlueLED: String
        get() {
            when (boardVariant) {
                DEVICE_EDISON_ARDUINO -> return "IO13"
                DEVICE_EDISON -> return "GP45"
                DEVICE_RPI3 -> return "BCM26"
                DEVICE_NXP -> return "GPIO4_IO20"
                else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
            }
        }

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    val gpioForBtnA: String
        get() {
            when (boardVariant) {
                DEVICE_EDISON_ARDUINO -> return "IO13"
                DEVICE_EDISON -> return "GP45"
                DEVICE_RPI3 -> return "BCM21"
                DEVICE_NXP -> return "GPIO4_IO20"
                else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
            }
        }

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    val gpioForBtnB: String
        get() {
            when (boardVariant) {
                DEVICE_EDISON_ARDUINO -> return "IO13"
                DEVICE_EDISON -> return "GP45"
                DEVICE_RPI3 -> return "BCM20"
                DEVICE_NXP -> return "GPIO4_IO20"
                else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
            }
        }

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    val gpioForBtnC: String
        get() {
            when (boardVariant) {
                DEVICE_EDISON_ARDUINO -> return "IO13"
                DEVICE_EDISON -> return "GP45"
                DEVICE_RPI3 -> return "BCM16"
                DEVICE_NXP -> return "GPIO4_IO20"
                else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
            }
        }

    val i2cBus: String
        get() {
            when (boardVariant) {
                DEVICE_EDISON_ARDUINO -> return "I2C6"
                DEVICE_EDISON -> return "I2C1"
                DEVICE_JOULE -> return "I2C0"
                DEVICE_RPI3 -> return "I2C1"
                DEVICE_PICO -> return "I2C2"
                DEVICE_VVDN -> return "I2C4"
                else -> throw IllegalArgumentException("Unknown device: " + Build.DEVICE)
            }
        }

    val spiBus: String
        get() {
            when (boardVariant) {
                DEVICE_EDISON_ARDUINO -> return "SPI1"
                DEVICE_EDISON -> return "SPI2"
                DEVICE_JOULE -> return "SPI0.0"
                DEVICE_RPI3 -> return "SPI0.0"
                DEVICE_PICO -> return "SPI3.0"
                DEVICE_VVDN -> return "SPI1.0"
                else -> throw IllegalArgumentException("Unknown device: " + Build.DEVICE)
            }
        }

    val speakerPwmPin: String
        get() {
            when (boardVariant) {
                DEVICE_EDISON_ARDUINO -> return "IO3"
                DEVICE_EDISON -> return "GP13"
                DEVICE_JOULE -> return "PWM_0"
                DEVICE_RPI3 -> return "PWM1"
                DEVICE_PICO -> return "PWM7"
                DEVICE_VVDN -> return "PWM3"
                else -> throw IllegalArgumentException("Unknown device: " + Build.DEVICE)
            }
        }

    private// For the edison check the pin prefix
    // to always return Edison Breakout pin name when applicable.
    val boardVariant: String
        get() {
            if (!sBoardVariant.isEmpty()) {
                return sBoardVariant
            }
            sBoardVariant = Build.DEVICE
            if (sBoardVariant == DEVICE_EDISON) {
                val pioService = PeripheralManager.getInstance()
                val gpioList = pioService.gpioList
                if (gpioList.size != 0) {
                    val pin = gpioList[0]
                    if (pin.startsWith("IO")) {
                        sBoardVariant = DEVICE_EDISON_ARDUINO
                    }
                }
            }
            return sBoardVariant
        }


}