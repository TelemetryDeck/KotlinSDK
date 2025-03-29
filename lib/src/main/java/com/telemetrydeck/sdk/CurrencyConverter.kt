package com.telemetrydeck.sdk

import kotlin.math.roundToInt

object CurrencyConverter {
    private val currencyCodeToOneUSDExchangeRate = mapOf(
        "AED" to 3.6725,
        "AFN" to 73.1439,
        "ALL" to 94.4244,
        "AMD" to 396.6171,
        "ANG" to 1.7900,
        "AOA" to 915.1721,
        "ARS" to 1058.5000,
        "AUD" to 1.5742,
        "AWG" to 1.7900,
        "AZN" to 1.7002,
        "BAM" to 1.8645,
        "BBD" to 2.0000,
        "BDT" to 121.5449,
        "BGN" to 1.8646,
        "BHD" to 0.3760,
        "BIF" to 2964.2266,
        "BMD" to 1.0000,
        "BND" to 1.3398,
        "BOB" to 6.9305,
        "BRL" to 5.7132,
        "BSD" to 1.0000,
        "BTN" to 86.7994,
        "BWP" to 13.8105,
        "BYN" to 3.2699,
        "BZD" to 2.0000,
        "CAD" to 1.4182,
        "CDF" to 2856.7620,
        "CHF" to 0.8997,
        "CLP" to 946.3948,
        "CNY" to 7.2626,
        "COP" to 4127.8455,
        "CRC" to 507.0750,
        "CUP" to 24.0000,
        "CVE" to 105.1179,
        "CZK" to 23.8700,
        "DJF" to 177.7210,
        "DKK" to 7.1119,
        "DOP" to 62.0869,
        "DZD" to 135.3706,
        "EGP" to 50.6290,
        "ERN" to 15.0000,
        "ETB" to 126.2459,
        "EUR" to 0.9533,
        "FJD" to 2.2940,
        "FKP" to 0.7948,
        "FOK" to 7.1120,
        "GBP" to 0.7948,
        "GEL" to 2.8302,
        "GGP" to 0.7948,
        "GHS" to 15.4508,
        "GIP" to 0.7948,
        "GMD" to 72.6046,
        "GNF" to 8589.0144,
        "GTQ" to 7.7216,
        "GYD" to 209.2593,
        "HKD" to 7.7837,
        "HNL" to 25.5206,
        "HRK" to 7.1828,
        "HTG" to 130.8347,
        "HUF" to 383.5426,
        "IDR" to 16225.1575,
        "ILS" to 3.5481,
        "IMP" to 0.7948,
        "INR" to 86.7955,
        "IQD" to 1307.9508,
        "IRR" to 41993.2160,
        "ISK" to 140.4283,
        "JEP" to 0.7948,
        "JMD" to 157.9457,
        "JOD" to 0.7090,
        "JPY" to 152.3479,
        "KES" to 129.2574,
        "KGS" to 87.4567,
        "KHR" to 4008.1629,
        "KID" to 1.5744,
        "KMF" to 469.0028,
        "KRW" to 1440.3458,
        "KWD" to 0.3085,
        "KYD" to 0.8333,
        "KZT" to 497.5012,
        "LAK" to 21867.2622,
        "LBP" to 89500.0000,
        "LKR" to 295.5196,
        "LRD" to 199.3352,
        "LSL" to 18.3599,
        "LYD" to 4.9073,
        "MAD" to 9.9608,
        "MDL" to 18.8154,
        "MGA" to 4734.8216,
        "MKD" to 58.8122,
        "MMK" to 2099.5486,
        "MNT" to 3439.8970,
        "MOP" to 8.0173,
        "MRU" to 39.9597,
        "MUR" to 46.4371,
        "MVR" to 15.4548,
        "MWK" to 1736.3946,
        "MXN" to 20.3269,
        "MYR" to 4.4350,
        "MZN" to 63.6976,
        "NAD" to 18.3599,
        "NGN" to 1509.8070,
        "NIO" to 36.7984,
        "NOK" to 11.1191,
        "NPR" to 138.8791,
        "NZD" to 1.7453,
        "OMR" to 0.3845,
        "PAB" to 1.0000,
        "PEN" to 3.7091,
        "PGK" to 4.0165,
        "PHP" to 57.7773,
        "PKR" to 279.0304,
        "PLN" to 3.9665,
        "PYG" to 7905.2559,
        "QAR" to 3.6400,
        "RON" to 4.7473,
        "RSD" to 111.7081,
        "RUB" to 91.0874,
        "RWF" to 1405.5288,
        "SAR" to 3.7500,
        "SBD" to 8.6689,
        "SCR" to 14.4355,
        "SDG" to 459.0793,
        "SEK" to 10.6997,
        "SGD" to 1.3398,
        "SHP" to 0.7948,
        "SLE" to 22.8772,
        "SLL" to 22877.1788,
        "SOS" to 571.5471,
        "SRD" to 35.4328,
        "SSP" to 4391.5735,
        "STN" to 23.3563,
        "SYP" to 12933.0491,
        "SZL" to 18.3599,
        "THB" to 33.6413,
        "TJS" to 10.9222,
        "TMT" to 3.5008,
        "TND" to 3.1727,
        "TOP" to 2.3859,
        "TRY" to 36.2290,
        "TTD" to 6.7863,
        "TVD" to 1.5744,
        "TWD" to 32.6576,
        "TZS" to 2592.2504,
        "UAH" to 41.5989,
        "UGX" to 3674.9872,
        "UYU" to 43.2704,
        "UZS" to 12992.6998,
        "VES" to 62.0708,
        "VND" to 25400.2138,
        "VUV" to 123.0591,
        "WST" to 2.8244,
        "XAF" to 625.3371,
        "XCD" to 2.7000,
        "XDR" to 0.7614,
        "XOF" to 625.3371,
        "XPF" to 113.7616,
        "YER" to 247.9730,
        "ZAR" to 18.3601,
        "ZMW" to 28.1645,
        "ZWL" to 26.4365
    )

    /**
     * Converts a price amount in micros from a given currency to USD
     *
     * @param priceAmountMicros The price amount in micros (millionths) in the original currency
     * @param currencyCode The currency code to convert from (e.g., "EUR", "GBP")
     * @return The amount converted to USD, or 0 if the currency code is invalid
     */
    fun convertToUSD(priceAmountMicros: Long, currencyCode: String?): Double {
        val amount = priceAmountMicros / 1_000_000.0
        val convertedAmount = convertToUSD(amount, currencyCode)
        // rounding to 100th to avoid floating point precision issues when dealing with "currency"
        return (convertedAmount * 100).roundToInt() / 100.0
    }



    /**
     * Converts an amount from a given currency to USD.
     *
     * @param amount The amount in the original currency
     * @param currencyCode The currency code to convert from (e.g., "EUR", "GBP")
     * @return The amount converted to USD, or 0 if the currency code is invalid or not found
     */
    fun convertToUSD(amount: Double, currencyCode: String?): Double {
        return when (currencyCode) {
            null -> 0.0
            "USD" -> amount
            else -> {
                val exchangeRate = currencyCodeToOneUSDExchangeRate[currencyCode]
                if (exchangeRate != null && exchangeRate != 0.0) {
                    amount / exchangeRate
                } else {
                    0.0
                }
            }
        }
    }
}