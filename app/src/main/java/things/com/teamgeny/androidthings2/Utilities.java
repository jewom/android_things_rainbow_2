package things.com.teamgeny.androidthings2;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utilities {

    static String getDate() {

        try {
            DateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss", Locale.US);
            Date netDate = (new Date());
            return sdf.format(netDate);
        } catch (Exception ex) {
            return "xx";
        }
    }

    // Converts to celcius
    public static float convertFahrenheitToCelcius(float fahrenheit) {
        return ((fahrenheit - 32) * 5 / 9);
    }

    // Converts to fahrenheit
    static  float convertCelciusToFahrenheit(float celsius) {
        return ((celsius * 9) / 5) + 32;
    }
}
