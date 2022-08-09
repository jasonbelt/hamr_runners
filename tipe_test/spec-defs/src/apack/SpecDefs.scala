// #Sireum #Logika

package apack

import org.sireum._

@datatype class Temperature_i (degrees: F32)

object BaseTypes {
  type Float_32 = F32
}

// library defs
object TempSensor_GUMBO_Library {
  @strictpure def gminTempDegrees: BaseTypes.Float_32 = f32"-50.0"

  @strictpure def gmaxTempDegrees: BaseTypes.Float_32 = f32"150.0"

  @strictpure def gTempSensorRange(tempy: Temperature_i): B =
    tempy.degrees >= gminTempDegrees && tempy.degrees <= gmaxTempDegrees

  @strictpure def buildTemp(degrees: BaseTypes.Float_32): Temperature_i =
    apack.Temperature_i(degrees)
}

object TempSensor {

  // subclause def
  @strictpure def minTempDegrees: BaseTypes.Float_32 = f32"-50.0"

  // subclause def
  @strictpure def defaultTempDegrees(): BaseTypes.Float_32 = f32"72.0"

  var currentTemp: Temperature_i = Temperature_i(defaultTempDegrees())

  def timeTriggered(value: Temperature_i): Unit = {
    Contract(
      Requires(value.degrees >= minTempDegrees),

      Modifies(currentTemp),

      Ensures(
        value == TempSensor_GUMBO_Library.buildTemp(f32"72.0"),
        currentTemp.degrees >= minTempDegrees,
        currentTemp.degrees >= TempSensor_GUMBO_Library.gminTempDegrees,
        currentTemp.degrees <= TempSensor_GUMBO_Library.gmaxTempDegrees,
        TempSensor_GUMBO_Library.gTempSensorRange(currentTemp)
      )
    )
    currentTemp = Temperature_i(defaultTempDegrees())
  }

}
