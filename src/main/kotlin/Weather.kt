import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets

object Weather {
    const val latitude = 52.237049
    const val longitude = 21.017532
    val appId = this.javaClass.getResource("weather")?.readText() ?: error("Cannot load Weather API key")

    var temperature = "NaN"
    var weatherState = "NaN"

    fun load() {
        val url = URL("https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&lang=pl&appid=$appId&units=metric")
        val connection = url.openConnection()
        val inputStream = url.openStream()

        val data = String(inputStream.readAllBytes(), StandardCharsets.UTF_8)

        val jsonObject = JSONObject(data)
        weatherState = jsonObject.getJSONArray("weather").getJSONObject(0)["description"].toString()
        temperature = jsonObject.getJSONObject("main")["temp"].toString()
    }
}