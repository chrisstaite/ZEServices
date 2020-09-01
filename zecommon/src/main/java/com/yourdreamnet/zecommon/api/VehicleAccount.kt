package com.yourdreamnet.zecommon.api

import android.os.Parcel
import android.os.Parcelable
import com.android.volley.Request
import com.android.volley.RequestQueue
import org.json.JSONArray
import org.json.JSONObject
import rx.Observable

class VehicleAccount(api: VehicleApi, accountId: String, country: String) : Parcelable
{

    private val _api = api
    private val _accountId = accountId
    private val _country = country

    constructor(parcel: Parcel) : this(VehicleApi(parcel), parcel.readString()!!, parcel.readString()!!)

    internal fun request(queue: RequestQueue, method: Int, route: String, parameters: JSONObject?): Observable<JSONObject>
    {
        return _api.kamareonRequest(queue, method, "/commerce/v1/accounts/$_accountId$route?country=$_country", parameters)
    }

    private fun toVehicleList(vehicles: JSONArray): List<Vehicle>
    {
        val ids = ArrayList<Vehicle>()
        for (i in 0 until vehicles.length())
        {
            val vehicle = vehicles.getJSONObject(i)
            val vin = vehicle.getString("vin")
            val registration = vehicle.getJSONObject("vehicleDetails").getString("registrationNumber")
            ids.add(Vehicle(this, vin, registration))
        }
        return ids
    }

    fun getVehicles(queue: RequestQueue): Observable<List<Vehicle>>
    {
        return request(
                queue,
                Request.Method.GET,
                "/vehicles",
                null
        ).map{ vehicles -> toVehicleList(vehicles.getJSONArray("vehicleLinks")) }.asObservable()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        _api.writeToParcel(parcel, flags)
        parcel.writeString(_accountId)
        parcel.writeString(_country)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VehicleAccount> {
        override fun createFromParcel(parcel: Parcel): VehicleAccount {
            return VehicleAccount(parcel)
        }

        override fun newArray(size: Int): Array<VehicleAccount?> {
            return arrayOfNulls(size)
        }
    }

}
