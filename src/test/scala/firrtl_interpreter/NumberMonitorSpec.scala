// See LICENSE for license details.

package firrtl_interpreter

import org.scalatest.{FreeSpec, Matchers}

//scalastyle:off magic.number
class NumberMonitorSpec extends FreeSpec with Matchers {
  private val rand = scala.util.Random
  private val sampleSize = 10000000
  private val carla = "Carla"

  "NumberMonitor class computes a running average, variance, min, max and histogram of series of inputs" - {
    "constant inputs should create mean equal to constant, variance 0" in {
      val monitor = new NumberMonitor(carla, canBeNegative = true, 16, 8)

      for(_ <- 0 until sampleSize) {
        val value = BigInt(42)
        monitor.update(value)
      }

      monitor.mean should be (42.0)
      monitor.stddev should be (0.0)
      monitor.minValue should be (42.0)
      monitor.maxValue should be (42.0)

      println(s"mean = ${monitor.mean}, σ = ${monitor.stddev} min = ${monitor.minValue} max = ${monitor.maxValue}")
      println(s"bins = ${monitor.showBins}")
    }
  }
  "even distribution inputs should create mean close to zero, variance 0" in {
    val monitor = new NumberMonitor(carla, canBeNegative = true, 16, 8)

    for(_ <- 0 until sampleSize) {
      val value = rand.nextInt(1 << 16) + monitor.minPossible
      monitor.update(value)
    }

    println(s"mean = ${monitor.mean}, σ = ${monitor.stddev} min = ${monitor.minValue} max = ${monitor.maxValue}")
    println(s"bins = ${monitor.showBins}")

//    monitor.mean.abs should be (5.0)
//    monitor.variance should be (0.0)
//    monitor.minValue should be (42.0)
//    monitor.maxValue should be (42.0)

    println(s"mean = ${monitor.mean}, σ = ${monitor.stddev} min = ${monitor.minValue} max = ${monitor.maxValue}")
    println(s"bins = ${monitor.showBins}")
  }

  "gaussian distribution inputs should create mean close to zero, variance 0" in {
    val monitor = new NumberMonitor(carla, canBeNegative = true, 16, 8)

    for(_ <- 0 until sampleSize) {
      val randomNum = (rand.nextGaussian() / 6.0) * (1 << 14)
      val value = BigInt(randomNum.toLong)
      monitor.update(value)
    }

    println(s"mean = ${monitor.mean}, σ = ${monitor.stddev} min = ${monitor.minValue} max = ${monitor.maxValue}")
    println(s"bins = ${monitor.showBins}")

    //    monitor.mean.abs should be (5.0)
    //    monitor.variance should be (0.0)
    //    monitor.minValue should be (42.0)
    //    monitor.maxValue should be (42.0)

    println(s"mean = ${monitor.mean}, σ = ${monitor.stddev} min = ${monitor.minValue} max = ${monitor.maxValue}")
    println(s"bins = ${monitor.showBins}")
  }

  "when possible values less than bins, just use bin per value" in {
    val monitor = new NumberMonitor(carla, canBeNegative = false, 2, 32)
    monitor.divisor should be (Big1)
    monitor.minPossible should be (Big0)
    monitor.maxPossible should be (BigInt(3))
    monitor.adjustedMaxPossible should be (BigInt(3))

    monitor.update(0)
    monitor.update(1)
    monitor.update(2)
    monitor.update(3)

    monitor.bins(0) should be (1L)
    monitor.bins(1) should be (1L)
    monitor.bins(2) should be (1L)
    monitor.bins(3) should be (1L)

  }

  "when possible values less than bins, just use bin per value, using negatives" in {
    val monitor = new NumberMonitor(carla, canBeNegative = true, 2, 32)
    monitor.divisor should be (Big1)
    monitor.minPossible should be (BigInt(-2))
    monitor.maxPossible should be (BigInt(1))
    monitor.adjustedMaxPossible should be (BigInt(3))

    monitor.update(-2)
    monitor.update(-1)
    monitor.update(0)
    monitor.update(1)

    monitor.bins(0) should be (1L)
    monitor.bins(1) should be (1L)
    monitor.bins(2) should be (1L)
    monitor.bins(3) should be (1L)

  }

  "must choose min and max values wisely" in {
    val monitor = new NumberMonitor(carla, canBeNegative = true, bitSize = 4)
    monitor.minValue should be (BigInt(8))
    monitor.maxValue should be (BigInt(-9))
  }

  "number monitor must work with large negative values" in {
    val monitor = new NumberMonitor(carla, canBeNegative = true, bitSize = 128, numberOfBins = 32)

    println(s"monitor.minPossible ${monitor.minPossible}")
    println(s"monitor.maxPossible ${monitor.maxPossible}")

    val lower = BigInt("-9223372036854775808", 10)
    val upper = BigInt("9223372036854775807", 10)

    monitor.minPossible should be (lower)
    monitor.maxPossible should be (upper)

    monitor.update(lower)
    monitor.bins.head should be (1)

    monitor.update(upper)

    monitor.bins.last should be (1)

    monitor.update(lower * BigInt(1000))  // illegal low value treated as low
    monitor.update(upper * BigInt(1000))  // illegal high value treated as high

    monitor.bins.head should be (2)
    monitor.bins.last should be (2)

    // println(s"Bins ${monitor.bins.mkString(",")}")

  }

}
