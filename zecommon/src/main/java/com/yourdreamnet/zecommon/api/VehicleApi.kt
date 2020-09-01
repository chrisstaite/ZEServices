package com.yourdreamnet.zecommon.api

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.RequestFuture
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import rx.Observable
import rx.schedulers.Schedulers
import rx.subjects.AsyncSubject

class VehicleApi(config: MyRenaultConfig, cookie: String, personId: String) : Parcelable
{
    private val _config = config
    private val _cookie = cookie
    private val _personId = personId

    constructor(parcel: Parcel) : this(
            MyRenaultConfig(parcel), parcel.readString()!!, parcel.readString()!!
    )

    private fun kamereonAuthenticatedRequest(queue: RequestQueue, method: Int, route: String, parameters: JSONObject?, token: String): Observable<JSONObject>
    {
        val result = RequestFuture.newFuture<JSONObject>()
        val o = Observable.from(result, Schedulers.io())

        val request: JsonObjectRequest = object : JsonObjectRequest(
                method,
                "${_config._kamareonUrl}$route",
                parameters,
                Response.Listener { response: JSONObject ->
                    try {
                        result.onResponse(response)
                    } catch (e: JSONException) {
                        Log.e("ZEServicesApi", "Unable to parse JSON response", e)
                        result.onErrorResponse(VolleyError("Unable to parse response", e))
                    }
                }, Response.ErrorListener { error: VolleyError? ->
            result.onErrorResponse(error)
            Log.e("ZEServicesApi", "Bad response to kamereon request (API Key: ${_config._kamareonApi}, Gigya Token: $token)", error)
        }) {
            override fun getBodyContentType(): String {
                return "application/vnd.api+json";
            }

            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf("apikey" to _config._kamareonApi, "x-gigya-id_token" to token)
            }
        }
        request.retryPolicy = DefaultRetryPolicy(
                MyRenaultConfig.REQUEST_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        result.setRequest(request)
        queue.add(request)
        return o
    }

    internal fun kamareonRequest(queue: RequestQueue, method: Int, route: String, parameters: JSONObject?): Observable<JSONObject>
    {
        val o: AsyncSubject<JSONObject> = AsyncSubject.create()
        val tokenRequest = _config.getJwtToken(queue, _cookie)
        tokenRequest.subscribe(
                { token -> kamereonAuthenticatedRequest(queue, method, route, parameters, token).subscribe(o) },
                { error -> o.onError(error) }
        )
        return o
    }

    private fun toAccountList(accounts: JSONArray) : List<VehicleAccount>
    {
        val ids = ArrayList<VehicleAccount>()
        for (i in 0 until accounts.length())
        {
            val account = accounts.getJSONObject(i)
            if (account.getString("accountStatus") == "ACTIVE" &&
                    account.getString("accountType") == "MYRENAULT") {
                ids.add(VehicleAccount(
                        this, account.getString("accountId"), account.getString("country")
                ))
            }
        }
        return ids
    }

    fun getAccounts(queue: RequestQueue): Observable<List<VehicleAccount>>
    {
        return kamareonRequest(
                queue,
                Request.Method.GET,
                "/commerce/v1/persons/$_personId?country=${_config.country()}",
                null
            ).
            map { response -> response.getJSONArray("accounts") }.
            map { accounts -> toAccountList(accounts) }.
            asObservable()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        _config.writeToParcel(parcel, flags)
        parcel.writeString(_cookie)
        parcel.writeString(_personId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VehicleApi> {
        override fun createFromParcel(parcel: Parcel): VehicleApi {
            return VehicleApi(parcel)
        }

        override fun newArray(size: Int): Array<VehicleApi?> {
            return arrayOfNulls(size)
        }
    }

}
