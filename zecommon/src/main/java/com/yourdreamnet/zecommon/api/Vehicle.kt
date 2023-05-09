package com.yourdreamnet.zecommon.api

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import com.android.volley.Request
import com.android.volley.RequestQueue
import org.json.JSONObject
import rx.Observable
import java.text.SimpleDateFormat
import java.util.*

class Vehicle(private val _account: VehicleAccount,
              private val _vin: String,
              private val _registration: String) : Parcelable {

    constructor(parcel: Parcel) : this(
        VehicleAccount(parcel), parcel.readString()!!, parcel.readString()!!)

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

    @SuppressLint("SimpleDateFormat")
    fun getChargeHistory(queue: RequestQueue, from: Date, to: Date): Observable<JSONObject>
    {
        val format = SimpleDateFormat("yyyyMMdd")
        return get(queue, "/charges?start=" + format.format(from) + "&end=" + format.format(to))
    }

    fun preconditionStatus(queue: RequestQueue): Observable<JSONObject>
    {
        return get(queue, "/hvac-status");
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
                ) as Map<String, Object>)
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
                ) as Map<String, Object>)
        )
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        _account.writeToParcel(parcel, flags)
        parcel.writeString(_vin)
        parcel.writeString(_registration)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Vehicle> {
        override fun createFromParcel(parcel: Parcel): Vehicle {
            return Vehicle(parcel)
        }

        override fun newArray(size: Int): Array<Vehicle?> {
            return arrayOfNulls(size)
        }
    }

}
