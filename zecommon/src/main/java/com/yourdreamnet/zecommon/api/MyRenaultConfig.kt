package com.yourdreamnet.zecommon.api

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.JsonRequest
import com.android.volley.toolbox.RequestFuture
import org.json.JSONException
import org.json.JSONObject
import rx.Observable
import rx.schedulers.Schedulers
import rx.subjects.AsyncSubject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*

class MyRenaultConfig(kamareonUrl: String, kamareonApi: String, gigyaUrl: String, gigyaApi: String) : Parcelable
{
    internal val _kamareonUrl = kamareonUrl
    internal val _kamareonApi = kamareonApi
    private val _gigyaUrl = gigyaUrl
    private val _gigyaApi = gigyaApi

    constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!
    )

    class JwtCache(cookie: String, token: String) {
        private val _cookie = cookie
        private val _token = token
        private val _expiry = Date(JSONObject(
                String(Base64.decode(token.split('.')[1], 0))
        ).getLong("exp"))

        fun valid(cookie: String): Boolean {
            return cookie == _cookie && Date().before(_expiry)
        }

        fun token(): String
        {
            return _token
        }
    }
    private var _jwtCache: JwtCache? = null

    companion object {
        // TODO: Possibly need to change the locale?
        const val LOCALE = "en_GB"

        // The URL to get the configuration data from
        const val CONFIG_URL = "https://renault-wrd-prod-1-euw1-myrapp-one.s3-eu-west-1.amazonaws.com/configuration/android/config_{LOCALE}.json"

        // Maximum number of milliseconds to wait for config or a login
        const val REQUEST_TIMEOUT_MS = 10000

        @JvmField
        val CREATOR = object : Parcelable.Creator<MyRenaultConfig> {
            override fun createFromParcel(parcel: Parcel): MyRenaultConfig {
                return MyRenaultConfig(parcel)
            }

            override fun newArray(size: Int): Array<MyRenaultConfig?> {
                return arrayOfNulls(size)
            }
        }

        fun getConfig(queue: RequestQueue): Observable<MyRenaultConfig> {
            val result = RequestFuture.newFuture<MyRenaultConfig>()
            val o = Observable.from(result, Schedulers.io())
            val configRequest: JsonObjectRequest = object : JsonObjectRequest(
                    Request.Method.GET,
                    CONFIG_URL.replace("{LOCALE}", LOCALE),
                    null,
                    Response.Listener { response: JSONObject ->
                        try {
                            val servers = response.getJSONObject("servers")
                            val kamareon = servers.getJSONObject("wiredProd")
                            val kamareonUrl = kamareon.getString("target")
                            //val kamareonApi = kamareon.getString("apikey")
                            val kamareonApi = "VAX7XYKGfa92yMvXculCkEFyfZbuM7Ss"
                            val gigya = servers.getJSONObject("gigyaProd")
                            val gigyaUrl = gigya.getString("target")
                            val gigyaApi = gigya.getString("apikey")
                            result.onResponse(MyRenaultConfig(kamareonUrl, kamareonApi, gigyaUrl, gigyaApi))
                        } catch (e: JSONException) {
                            Log.e("ZEServicesApi", "Unable to parse JSON response", e)
                            result.onErrorResponse(VolleyError("Unable to parse response", e))
                        }
                    }, Response.ErrorListener { error: VolleyError? ->
                result.onErrorResponse(error)
                Log.e("ZEServicesApi", "Bad response to config request", error)
            }) {}

            configRequest.retryPolicy = DefaultRetryPolicy(
                    REQUEST_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            result.setRequest(configRequest)
            queue.add(configRequest)
            return o
        }
    }

    class JsonResponseRequest(
            method: Int,
            url: String?,
            requestBody: String?,
            listener: Response.Listener<JSONObject?>?,
            errorListener: Response.ErrorListener?) :
            JsonRequest<JSONObject>(
                method,
                url,
                requestBody,
                listener,
                errorListener
            )
    {
        override fun getBodyContentType(): String {
            return "application/x-www-form-urlencoded";
        }

        override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
            return try {
                val jsonString = String(
                        response.data,
                        Charset.forName(HttpHeaderParser.parseCharset(response.headers, "utf-8"))
                )
                Response.success(
                        JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response)
                )
            } catch (e: UnsupportedEncodingException) {
                Response.error(ParseError(e))
            } catch (je: JSONException) {
                Response.error(ParseError(je))
            }
        }
    }

    private fun gigyaRequest(queue: RequestQueue, route: String, parameters: Map<String, String>): Observable<JSONObject>
    {
        val result = RequestFuture.newFuture<JSONObject>()
        val o = Observable.from(result, Schedulers.io())

        val builder = Uri.Builder();
        for ((key, value) in parameters) {
            builder.appendQueryParameter(key, value)
        }

        val configRequest = JsonResponseRequest(
                Request.Method.POST,
                "$_gigyaUrl$route",
                builder.build().encodedQuery,
                { response: JSONObject? ->
                    try {
                        val errorCode = response!!.optInt("errorCode", 0)
                        val errorDescription = response.optString("errorDetails", "")
                        if (errorCode > 0) {
                            result.onErrorResponse(VolleyError("Error logging in: $errorDescription ($errorCode"))
                        } else {
                            result.onResponse(response)
                        }
                    } catch (e: JSONException) {
                        Log.e("ZEServicesApi", "Unable to parse JSON response", e)
                        result.onErrorResponse(VolleyError("Unable to parse response", e))
                    }
                }, { error: VolleyError? ->
                    result.onErrorResponse(error)
                    Log.e("ZEServicesApi", "Bad response to gigya request", error)
                }
        )
        configRequest.retryPolicy = DefaultRetryPolicy(
                REQUEST_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        result.setRequest(configRequest)
        queue.add(configRequest)

        return o
    }

    fun login(queue: RequestQueue, username: String, password: String): Observable<String>
    {
        val request = hashMapOf("ApiKey" to _gigyaApi, "loginID" to username, "password" to password)
        return gigyaRequest(queue, "/accounts.login", request).map { data: JSONObject -> data.getJSONObject("sessionInfo").getString("cookieValue")
        }.asObservable()
    }

    fun getPersonId(queue: RequestQueue, cookie: String): Observable<String>
    {
        val request = hashMapOf("ApiKey" to _gigyaApi, "login_token" to cookie)
        return gigyaRequest(queue, "/accounts.getAccountInfo", request).map { data: JSONObject -> data.getJSONObject("data").getString("personId")
        }.asObservable()
    }

    private fun getJwtToken(cookie: String, response: JSONObject): String
    {
        val token = response.getString("id_token")
        _jwtCache = JwtCache(cookie, token)
        return token
    }

    fun getJwtToken(queue: RequestQueue, cookie: String): Observable<String>
    {
        val cache = _jwtCache
        if (cache != null && cache.valid(cookie))
        {
            val o: AsyncSubject<String> = AsyncSubject.create()
            o.publish(cache.token())
            return o.asObservable()
        }

        val request = hashMapOf(
                "ApiKey" to _gigyaApi,
                "login_token" to cookie,
                "fields" to "data.personId,data.gigyaDataCenter",
                "expiration" to "900"
        )
        return gigyaRequest(queue, "/accounts.getJWT", request).map {
            data: JSONObject -> getJwtToken(cookie, data)
        }.asObservable()
    }

    fun country(): String
    {
        return LOCALE.split('_')[1]
    }

    fun getVehicleApi(queue: RequestQueue, username: String, password: String) : Observable<VehicleApi>
    {
        val o: AsyncSubject<VehicleApi> = AsyncSubject.create()
        val cookieRequest = login(queue, username, password)
        cookieRequest.subscribe(
                { cookie ->
                    getPersonId(queue, cookie).map { personId -> VehicleApi(this, cookie, personId) }.subscribe(o)
                },
                { error -> o.onError(error) }
        )
        return o
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(_kamareonUrl)
        parcel.writeString(_kamareonApi)
        parcel.writeString(_gigyaUrl)
        parcel.writeString(_gigyaApi)
    }

    override fun describeContents(): Int {
        return 0
    }

}
