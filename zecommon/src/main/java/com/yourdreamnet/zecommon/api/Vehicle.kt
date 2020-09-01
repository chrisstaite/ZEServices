package com.yourdreamnet.zecommon.api

import com.android.volley.Request
import com.android.volley.RequestQueue
import org.json.JSONObject
import rx.Observable

class Vehicle(account: VehicleAccount, vin: String, registration: String) {

    private val _account = account
    private val _vin = vin
    private val _registration = registration

    fun vin(): String
    {
        return _vin
    }

    fun registration(): String
    {
        return _registration
    }

    private fun get(queue: RequestQueue, route: String, version: Int = 1): Observable<JSONObject>
    {
        return _account.
            request(queue, Request.Method.GET, "/kamereon/kca/car-adapter/v$version/cars/$_vin$route", null).
            map { response -> response.getJSONObject("data").getJSONObject("attributes") }
    }

    private fun post(queue: RequestQueue, route: String, parameters: JSONObject?, version: Int = 1): Observable<JSONObject>
    {
        return _account.
            request(queue, Request.Method.POST, "/kamereon/kca/car-adapter/v$version/cars/$_vin$route", parameters)
    }

    fun getBattery(queue: RequestQueue): Observable<JSONObject>
    {
        return get(queue, "/battery-status", 2)
    }

    fun getLocation(queue: RequestQueue): Observable<JSONObject>
    {
        return get(queue, "/location")
    }

    fun getHvacStatus(queue: RequestQueue): Observable<JSONObject>
    {
        return get(queue, "/hvac-status")
    }

    fun getMilage(queue: RequestQueue): Observable<JSONObject>
    {
        return get(queue, "/cockpit", 2)
    }

    fun startPrecondition(queue: RequestQueue): Observable<JSONObject>
    {
        return post(
                queue,
                "/actions/hvac-start",
                JSONObject(hashMapOf(
                        "data" to hashMapOf(
                                "type" to "HvacStart",
                                "attributes" to hashMapOf(
                                        "action" to "start",
                                        "targetTemperature" to 21
                                )
                        )
                ))
        )
    }

    fun startCharge(queue: RequestQueue): Observable<JSONObject>
    {
        return post(
                queue,
                "/actions/charging-start",
                JSONObject(hashMapOf(
                        "data" to hashMapOf(
                                "type" to "ChargingStart",
                                "attributes" to hashMapOf(
                                        "action" to "start"
                                )
                        )
                ))
        )
    }

}
