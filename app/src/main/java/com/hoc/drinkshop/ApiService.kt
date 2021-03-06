package com.hoc.drinkshop

import android.os.Parcelable
import com.facebook.accountkit.Account
import com.facebook.accountkit.AccountKit
import com.facebook.accountkit.AccountKitCallback
import com.facebook.accountkit.AccountKitError
import com.squareup.moshi.Json
import io.reactivex.Flowable
import io.reactivex.Observable
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Date
import kotlin.coroutines.experimental.suspendCoroutine

const val BASE_URL = "https://drink-shop.herokuapp.com/"

data class Error(val message: String)

@Parcelize
data class User(
    val phone: String,
    val name: String,
    val birthday: Date,
    val address: String,
    val imageUrl: String? = null
) : Parcelable

data class Banner(
    @Json(name = "_id") val id: Int,
    val name: String,
    val imageUrl: String
)

@Parcelize
data class Category(
    @Json(name = "_id") val id: Int,
    val name: String,
    val imageUrl: String
) : Parcelable

@Parcelize
data class Drink(
    @Json(name = "_id") val id: Int,
    val name: String,
    val imageUrl: String,
    val price: Double,
    val menuId: Int,
    val starCount: Int,
    val stars: List<String>
) : Parcelable

enum class OrderStatus {
    @Json(name = "CANCELED")
    CANCELED,
    @Json(name = "PLACED")
    PLACED,
    @Json(name = "PROCESSING")
    PROCESSING,
    @Json(name = "SHIPPING")
    SHIPPING,
    @Json(name = "SHIPPED")
    SHIPPED
}

@Parcelize
data class Order(
    @Json(name = "_id") val id: Int? = null,
    val price: Double = 0.0,
    val detail: List<Cart> = emptyList(),
    val comment: String = "",
    val phone: String = "",
    val address: String = "",
    val createdAt: Date? = null,
    val status: OrderStatus? = null
) : Parcelable

enum class SortOrder {
    ASC_SORT_STRING,
    DESC_SORT_STRING,
    ASC_FULL_STRING,
    DESC_FULL_STRING,
    ASC_NUMBER,
    DESC_NUMBER;

    override fun toString() = when (this) {
        ASC_FULL_STRING -> "ascending"
        DESC_FULL_STRING -> "descending"
        ASC_SORT_STRING -> "asc"
        DESC_SORT_STRING -> "desc"
        ASC_NUMBER -> "1"
        DESC_NUMBER -> "-1"
    }
}

data class PriceResponse(val price: Double)

data class Loc(
    private val coordinates: List<Double>,
    val type: String
) {
    val lng get() = coordinates[0]
    val lat get() = coordinates[1]
}

data class Store(
    @Json(name = "_id") val id: Int,
    val name: String,
    val loc: Loc,
    val distance: Double
)

interface ApiService {
    /*  USER */
    @GET("users")
    fun getAllUsers(): Call<List<User>>

    @GET("users/{phone}")
    fun getUserByPhone(@Path("phone") phone: String): Call<User>

    @FormUrlEncoded
    @POST("users")
    fun registerNewUser(
        @Field("phone") phone: String,
        @Field("name") name: String,
        @Field("birthday") birthday: String,
        @Field("address") address: String
    ): Call<User>

    @POST("users/{phone}/image")
    @Multipart
    fun uploadImage(
        @Part body: MultipartBody.Part,
        @Path("phone") phone: String
    ): Call<User>

    /* BANNER */
    @GET("banners")
    fun getBanners(@Query("limit") limit: Int? = null): Call<List<Banner>>

    /* CATEGORY */
    @GET("categories")
    fun getAllCategories(): Call<List<Category>>

    /* DRINK */
    @GET("drinks")
    fun getDrinks(
        @Query("menu_id") menuId: Int? = null,
        @Query("phone") phone: String? = null,
        @Query("name") name: String? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
        @Query("min_star") minStar: Double? = null,
        @Query("max_star") maxStar: Double? = null,
        @Query("sort_name") sortName: SortOrder? = null,
        @Query("sort_star") sortStar: SortOrder? = null,
        @Query("sort_price") sortPrice: SortOrder? = null,
        @Query("limit") limit: Int? = null
    ): Call<List<Drink>>

    @GET("drinks")
    fun getDrinksFlowable(
        @Query("menu_id") menuId: Int? = null,
        @Query("phone") phone: String? = null,
        @Query("name") name: String? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
        @Query("min_star") minStar: Double? = null,
        @Query("max_star") maxStar: Double? = null,
        @Query("sort_name") sortName: SortOrder? = null,
        @Query("sort_star") sortStar: SortOrder? = null,
        @Query("sort_price") sortPrice: SortOrder? = null,
        @Query("limit") limit: Int? = null
    ): Flowable<List<Drink>>

    @GET("drinks/{drink_id}")
    fun getDrinkById(): Call<Drink>

    @GET("drinks/price/min")
    fun getMinPrice(): Observable<PriceResponse>

    @GET("drinks/price/max")
    fun getMaxPrice(): Observable<PriceResponse>

    @FormUrlEncoded
    @POST("drinks/star")
    fun star(
        @Field("phone") phone: String,
        @Field("drink_id") drinkId: Int
    ): Observable<Drink>

    @FormUrlEncoded
    @POST("drinks/unstar")
    fun unstar(
        @Field("phone") phone: String,
        @Field("drink_id") drinkId: Int
    ): Observable<Drink>

    /* ORDER */
    @Headers("Content-Type: application/json")
    @POST("order")
    fun submitOrder(@Body order: Order): Observable<Order>

    @GET("order")
    fun getOrders(
        @Query("status") status: OrderStatus,
        @Query("phone") phone: String
    ): Observable<List<Order>>

    @FormUrlEncoded
    @PUT("order")
    fun cancelOrder(@Field("_id") orderId: Int): Call<Order>

    /* NEARBY STORE */
    @GET("nearby-store")
    fun getNearbyStore(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("max_distance") maxDistanceInMetters: Int
    ): Flowable<List<Store>>
}

sealed class Result<out T : Any> {
    data class Success<out T : Any>(
        val value: T,
        val response: okhttp3.Response? = null
    ) : Result<T>()

    data class Error(val errorBody: ResponseBody, val response: okhttp3.Response) :
        Result<Nothing>()

    data class Exception(val throwable: Throwable) : Result<Nothing>()
}

fun <T : Any> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> value
    else -> null
}

inline fun <T : Any> Result<T>.getOrDefault(crossinline default: () -> T): T = getOrNull()
    ?: default()

fun <T : Any> Result<T>.getOrDefault(default: T): T = getOrDefault { default }

inline fun <T : Any, R : Any> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(value), response)
    is Result.Error -> this
    is Result.Exception -> this
}

inline fun <T : Any> Result<T>.onSuccess(onSuccess: (T) -> Unit): Result<T> {
    getOrNull()?.let(onSuccess)
    return this
}

inline fun <T : Any> Result<T>.onError(onError: (Pair<ResponseBody, okhttp3.Response>) -> Unit): Result<T> {
    if (this is Result.Error) onError(errorBody to response)
    return this
}

inline fun <T : Any> Result<T>.onException(onException: (Throwable) -> Unit): Result<T> {
    if (this is Result.Exception) onException(throwable)
    return this
}

suspend fun <T : Any> Call<T>.await(): T =
    suspendCancellableCoroutine { continuation: CancellableContinuation<T> ->
        enqueue(object : Callback<T> {
            override fun onFailure(call: Call<T>?, t: Throwable) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(t)
            }

            override fun onResponse(call: Call<T>?, response: retrofit2.Response<T>) {
                when {
                    response.isSuccessful -> {
                        val body: T? = response.body()
                        if (body != null) {
                            continuation.resume(body)
                        } else {
                            continuation.resumeWithException(NullPointerException("Reponse body is null"))
                        }
                    }
                    else -> {
                        continuation.resumeWithException(HttpException(response))
                    }
                }
            }
        })

        registerOnCompletion(continuation)
    }

fun <T> Call<*>.registerOnCompletion(continuation: CancellableContinuation<T>) {
    continuation.invokeOnCancellation {
        if (continuation.isCancelled) {
            cancel()
        }
    }
}

suspend fun <T : Any> Call<T>.awaitResult(): Result<T> =
    suspendCancellableCoroutine { continuation: CancellableContinuation<Result<T>> ->
        enqueue(object : Callback<T> {
            override fun onFailure(call: Call<T>?, t: Throwable) {
                if (continuation.isCancelled) return
                continuation.resume(Result.Exception(t))
            }

            override fun onResponse(call: Call<T>?, response: retrofit2.Response<T>) = when {
                response.isSuccessful -> {
                    val body: T? = response.body()
                    when {
                        body != null -> Result.Success(body, response.raw())
                        else -> Result.Exception(NullPointerException("Response body is null"))
                    }
                }
                else -> {
                    val errorBody = response.errorBody()
                    when {
                        errorBody != null -> Result.Error(errorBody, response.raw())
                        else -> Result.Exception(IllegalStateException("Error body is null"))
                    }
                }
            }.let(continuation::resume)
        })

        registerOnCompletion(continuation)
    }

suspend fun getCurrentAccount(): Result<Account> = suspendCoroutine { continuation ->
    AccountKit.getCurrentAccount(object : AccountKitCallback<Account> {
        override fun onSuccess(accout: Account) = continuation.resume(Result.Success(accout))

        override fun onError(error: AccountKitError) =
            error.errorType.message
                .let(::IllegalStateException)
                .let { Result.Exception(it) }
                .let(continuation::resume)
    })
}

inline fun <reified T : Any> Retrofit.parse(responseBody: ResponseBody): T = responseBody.use {
    responseBodyConverter<T>(T::class.java, T::class.java.annotations)
        .convert(it)
}

fun Retrofit.parseResultErrorMessage(responseBody: ResponseBody): String =
    parse<Error>(responseBody).message