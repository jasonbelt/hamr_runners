// #Sireum

import org.sireum._

val zArray: ISZ[Z] = ISZ(
  0x35, 0x35, 0x35, 0x00, 0x36, 0x36, 0x36, 0x00, 0x37, 0x37, 0x37, 0x00
)

val u8Array = zArray.map((m: Z) => conversions.Z.toU8(m))

val s = conversions.String.fromU8is(u8Array)

val cc = conversions.String.toCis(s)

print("[")
for(c <- cc) {
  print(s"$c,")
}
println("]")

