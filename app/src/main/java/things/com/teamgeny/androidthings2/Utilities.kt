package things.com.teamgeny.androidthings2

fun convertFahrenheitToCelcius(fahrenheit: Float): Float {
    return (fahrenheit - 32) * 5 / 9
}

fun convertCelciusToFahrenheit(celsius: Float): Float {
    return celsius * 9 / 5 + 32
}