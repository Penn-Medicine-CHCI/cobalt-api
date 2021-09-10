package com.cobaltplatform.api.integration.bluejeans

import com.cobaltplatform.api.Configuration
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.http.*
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Transmogrify LLC.
 */

interface BluejeansClient {
    enum class SearchType {
        EMAIL,
        TEXT
    }

    fun getUsers(searchTerm: String? = null,
                 searchType: BluejeansClient.SearchType? = null): UsersListResponse

    fun getUser(userId: Int): Any

    fun createUser(userCreateRequest: UserCreateRequest): Any

    fun deleteUser(userId: Int)

    fun scheduleMeetingForUser(bluejeansUserId: Int,
                               title: String,
                               emailToInvite: String?,
                               addAttendeePasscode: Boolean = false,
                               shouldSendInviteEmail: Boolean = false,
                               timeZone: ZoneId,
                               meetingStartTime: Instant,
                               meetingEndTime: Instant): MeetingResponse

    fun getScheduledMeetingsForUser(bluejeansUserId: Int): List<MeetingResponse>

    fun getScheduledMeetingNumbersForUser(bluejeansUserId: Int, meetingId: Int): MeetingNumbersResponse?

    fun cancelScheduledMeeting(hostUserId: Int,
                               meetingId: Int,
                               shouldSendNotificationEmail: Boolean,
                               message: String? = null)
}

@Singleton
class MockBluejeansClient @Inject constructor() : BluejeansClient {
    override fun getUsers(searchTerm: String?, searchType: BluejeansClient.SearchType?): UsersListResponse {
        return UsersListResponse(0, emptyList())
    }

    override fun getUser(userId: Int): Any {
        return Object()
    }

    override fun createUser(userCreateRequest: UserCreateRequest): Any {
        return Object()
    }

    override fun deleteUser(userId: Int) {
        // do nothing
    }

    override fun scheduleMeetingForUser(bluejeansUserId: Int, title: String, emailToInvite: String?, addAttendeePasscode: Boolean, shouldSendInviteEmail: Boolean, timeZone: ZoneId, meetingStartTime: Instant, meetingEndTime: Instant): MeetingResponse {
        return MeetingResponse(bluejeansUserId, numericMeetingId = "fake-meeting-link")
    }

    override fun getScheduledMeetingsForUser(bluejeansUserId: Int): List<MeetingResponse> {
        return emptyList()
    }

    override fun getScheduledMeetingNumbersForUser(bluejeansUserId: Int, meetingId: Int): MeetingNumbersResponse? {
        return null;
    }

    override fun cancelScheduledMeeting(hostUserId: Int, meetingId: Int, shouldSendNotificationEmail: Boolean, message: String?) {
        // do nothing
    }
}

@Singleton
class DefaultBluejeansClient @Inject constructor(
        private val bluejeansApi: BluejeansApi,
        private val authProvider: BluejeansCredentialsProvider
) : BluejeansClient {



    override fun getUsers(searchTerm: String?,
                          searchType: BluejeansClient.SearchType?): UsersListResponse {
        val response = bluejeansApi.getUsers(
                accessToken = authProvider.authResponse.tokenHeader(),
                enterpriseId = authProvider.authResponse.scope.enterprise,
                emailId = if (searchType != null && searchType == BluejeansClient.SearchType.EMAIL) {
                    searchTerm
                } else {
                    null
                },
                textSearch = if (searchType != null && searchType == BluejeansClient.SearchType.TEXT) {
                    searchTerm
                } else {
                    null
                }
        ).execute()

        response.body()?.let { return it }
        response.errorBody()?.let { throw BluejeansApiError(it.string()) }
        throw BluejeansApiError("Error calling bluejeans API")
    }

    override fun getUser(userId: Int): Any {
        val response = bluejeansApi.getUser(authProvider.authResponse.tokenHeader(), userId).execute()

        response.body()?.let { return it }
        response.errorBody()?.let { throw BluejeansApiError(it.string()) }
        throw BluejeansApiError("Error calling bluejeans API")
    }

    override fun createUser(userCreateRequest: UserCreateRequest): Any {
        val response = bluejeansApi.createUser(authProvider.authResponse.tokenHeader(),
                authProvider.authResponse.scope.enterprise, userCreateRequest).execute()

        response.body()?.let { return it }
        response.errorBody()?.let { throw BluejeansApiError(it.string()) }
        throw BluejeansApiError("Error calling bluejeans API")
    }

    override fun deleteUser(userId: Int) {
        val response = bluejeansApi.deleteUser(authProvider.authResponse.tokenHeader(),
                authProvider.authResponse.scope.enterprise, userId).execute()

        response.errorBody()?.let { throw BluejeansApiError(it.string()) }
    }

    override fun scheduleMeetingForUser(bluejeansUserId: Int,
                                        title: String,
                                        emailToInvite: String?,
                                        addAttendeePasscode: Boolean,
                                        shouldSendInviteEmail: Boolean,
                                        timeZone: ZoneId,
                                        meetingStartTime: Instant,
                                        meetingEndTime: Instant): MeetingResponse {
        var attendees = listOf<MeetingAttendeeRequest>()

        if(emailToInvite != null) {
            attendees = listOf(MeetingAttendeeRequest(emailToInvite))
        }

        val response = bluejeansApi.createScheduledMeeting(authProvider.authResponse.tokenHeader(), bluejeansUserId, shouldSendInviteEmail,
                MeetingCreateRequest(title,
                        addAttendeePasscode = addAttendeePasscode,
                        attendees = attendees,
                        timezone = timeZone.toString(),
                        start = meetingStartTime.toEpochMilli(),
                        end = meetingEndTime.toEpochMilli())

        ).execute()

        response.body()?.let { return it }
        response.errorBody()?.let { throw BluejeansApiError(it.string()) }
        throw BluejeansApiError("Error calling bluejeans API")
    }

    override fun getScheduledMeetingsForUser(bluejeansUserId: Int): List<MeetingResponse> {
        val response = bluejeansApi.getScheduledMeetings(authProvider.authResponse.tokenHeader(), bluejeansUserId).execute()

        response.body()?.let { return it }
        response.errorBody()?.let { throw BluejeansApiError(it.string()) }
        throw BluejeansApiError("Error calling bluejeans API")
    }

    override fun getScheduledMeetingNumbersForUser(bluejeansUserId: Int, meetingId: Int): MeetingNumbersResponse? {
        val response = bluejeansApi.getScheduledMeetingNumbers(authProvider.authResponse.tokenHeader(), bluejeansUserId, meetingId).execute()

        response.body()?.let { return it }
        response.errorBody()?.let { throw BluejeansApiError(it.string()) }
        throw BluejeansApiError("Error calling bluejeans API")
    }

    override fun cancelScheduledMeeting(hostUserId: Int,
                               meetingId: Int,
                               shouldSendNotificationEmail: Boolean,
                               message: String?) {
        val response = bluejeansApi.deleteScheduledMeeting(authProvider.authResponse.tokenHeader(), hostUserId, meetingId,
                shouldSendNotificationEmail, message).execute()

        response.body()?.let { return }
        response.errorBody()?.let { throw BluejeansApiError(it.string()) }
        return
    }

}

const val AUTH = "Authorization"

interface BluejeansApi {

    @POST("/oauth2/token#Application")
    fun authenticate(@Body request: AuthRequest): Call<AuthResponse>

    @POST("/v1/enterprise/{enterpriseId}/users")
    fun createUser(@Header(AUTH) accessToken: String,
                   @Path("enterpriseId") enterpriseId: String,
                   @Body request: UserCreateRequest
    ): Call<Any>

    @DELETE("/v1/enterprise/{enterpriseId}/users/{userId}")
    fun deleteUser(@Header(AUTH) accessToken: String,
                   @Path("enterpriseId") enterpriseId: String,
                   @Path("userId") userId: Int
    ): Call<Any>

    @GET("/v1/enterprise/{enterpriseId}/users")
    fun getUsers(@Header(AUTH) accessToken: String,
                 @Path("enterpriseId") enterpriseId: String,
                 @Query("pageSize") pageSize: Int? = null,
                 @Query("pageNumber") pageNumber: Int? = null,
                 @Query("emailId") emailId: String? = null,
                 @Query("textSearch") textSearch: String? = null
    ): Call<UsersListResponse>

    @GET("/v1/user/{userId}")
    fun getUser(@Header(AUTH) accessToken: String,
                @Path("userId") userId: Int
    ): Call<Any>

    @POST("/v1/user/{userId}/scheduled_meeting")
    fun createScheduledMeeting(@Header(AUTH) accessToken: String,
                               @Path("userId") userId: Int,
                               @Query("email") shouldSendInviteEmail: Boolean,
                               @Body createRequest: MeetingCreateRequest
    ): Call<MeetingResponse>

    @GET("/v1/user/{userId}/scheduled_meeting")
    fun getScheduledMeetings(@Header(AUTH) accessToken: String,
                             @Path("userId") userId: Int
    ): Call<List<MeetingResponse>>

    @GET("/v1/user/{userId}/meetings/{meetingId}/numbers")
    fun getScheduledMeetingNumbers(@Header(AUTH) accessToken: String,
                                   @Path("userId") userId: Int,
                                   @Path("meetingId") meetingId: Int
    ): Call<MeetingNumbersResponse>

    @DELETE("/v1/user/{userId}/scheduled_meeting/{meetingId}")
    fun deleteScheduledMeeting(@Header(AUTH) accessToken: String,
                               @Path("userId") userId: Int,
                               @Path("meetingId") meetingId: Int,
                               @Query("email") shouldSendNotificationEmail: Boolean,
                               @Query("cancellationMessage") message: String? = null
    ): Call<Void>

    @POST("/v1/user/{userId}/live_meetings/{meetingId}/invite")
    fun inviteEmailAddressToMeeting(@Path("userId") userId: Int,
                                    @Path("meetingId") meetingId: Int,
                                    @Body request: InviteToMeetingRequest): Call<Void>

}

data class AuthRequest(val grant_type: String = "client_credentials", val client_id: String, val client_secret: String)
data class AuthResponse(val access_token: String, val scope: AuthScopeResponse) {
    fun tokenHeader(): String {
        return "Bearer ${this.access_token}"
    }

}

data class AuthScopeResponse(val partitionName: String, val enterprise: String)

// Example MeetingResponse JSON:
//
// {
//   "notificationUrl": null,
//   "next": {
//     "start": 1626295152589,
//     "end": 1626298752589
//   },
//   "timezone": "America/New_York",
//   "moderator": {
//     "firstname": "",
//     "id": 3776877,
//     "profile_pic_url": "",
//     "username": "Pennmedtogether",
//     "lastname": ""
//   },
//   "description": null,
//   "timelessMeeting": false,
//   "nextStart": 1626295152589,
//   "title": "Test Passcode Meeting",
//   "uuid": "235a51ad-7e23-43e0-b81b-34d451640848",
//   "endlessMeeting": false,
//   "addAttendeePasscode": true,
//   "endPointVersion": "2.10",
//   "advancedMeetingOptions": {
//     "waitingRoomEnabled": false,
//     "allowParticipantsHijackScreenShare": true,
//     "autoRecord": false,
//     "allowModeratorScreenShare": true,
//     "teleVisitEnabled": false,
//     "allowHighlights": true,
//     "disallowChat": false,
//     "encryptionType": "ENCRYPTED_OR_PSTN_ONLY",
//     "allowInterpreters": false,
//     "moderatorLess": false,
//     "allowStream": false,
//     "allowClosedCaptioning": true,
//     "publishMeeting": false,
//     "videoBestFit": false,
//     "hasBreakoutRoomConfigs": false,
//     "editability": {
//       "allowParticipantsHijackScreenShare": true,
//       "waitingRoomEnabled": true,
//       "autoRecord": true,
//       "enforceMeetingEncryption": false,
//       "addParticipantPasscode": false,
//       "allowModeratorScreenShare": true,
//       "allowHighlights": true,
//       "teleVisitEnabled": true,
//       "disallowChat": true,
//       "allowInterpreters": false,
//       "moderatorLess": true,
//       "allowClosedCaptioning": false,
//       "videoBestFit": true,
//       "enforceMeetingEncryptionAllowPSTN": true,
//       "hasBreakoutRoomConfigs": true,
//       "waitingRoomAdmitSameEnterpriseUsers": true,
//       "allowParticipantsStartScreenShare": true,
//       "videoMuteParticipantsOnEntry": true,
//       "allowLiveTranscription": true,
//       "muteParticipantsOnEntry": true,
//       "showAllAttendeesInMeetingInvite": true
//     },
//     "waitingRoomAdmitSameEnterpriseUsers": false,
//     "videoMuteParticipantsOnEntry": false,
//     "allowParticipantsStartScreenShare": true,
//     "muteParticipantsOnEntry": false,
//     "allowLiveTranscription": false,
//     "showAllAttendeesInMeetingInvite": false,
//     "meetingAccessType": "EVERYONE"
//   },
//   "inviteeJoinOption": 0,
//   "end": 1626298752589,
//   "id": 92686085,
//   "locked": false,
//   "parentMeetingId": null,
//   "customMeetingProperties": null,
//   "sequenceNumber": 0,
//   "isLargeMeeting": false,
//   "icsUid": "Q6bOf_xxc7lZno7s@i665QhO0KLPi1Ud",
//   "last": {
//     "start": 1626295152589,
//     "end": 1626298752589
//   },
//   "attendees": [],
//   "created": 1626291554556,
//   "start": 1626295152589,
//   "endPointType": "WEB_APP",
//   "notificationData": null,
//   "deleted": false,
//   "nextOccurrence": null,
//   "parentMeetingUUID": null,
//   "attendeePasscode": "4819",
//   "numericMeetingId": "967554469",
//   "allow720p": false,
//   "lastModified": 1626291554562,
//   "isExpired": false,
//   "first": {
//     "start": 1626295152589,
//     "end": 1626298752589
//   },
//   "status": null,
//   "nextEnd": 1626298752589,
//   "isPersonalMeeting": false
// }

data class MeetingResponse(
        val id: Int,
        val title: String? = null,
        val start: Long? = null,
        val end: Long? = null,
        val created: Long? = null,
        val attendees: List<Any> = emptyList(),
        val moderation: MeetingModeratorResponse? = null,
        val numericMeetingId: String,
        val attendeePasscode: String? = null
) {
    fun meetingLink(): String {
        return "https://bluejeans.com/$numericMeetingId"
    }
    fun meetingLinkWithAttendeePasscode(): String {
        return "https://bluejeans.com/$numericMeetingId/$attendeePasscode"
    }
}

// Example MeetingNumbersResponse
//
// {
//   "allowDirectDial": true,
//   "partnerIntegratedMeeting": true,
//   "pstnNumbersUrl": "pstnNumbersUrl",
//   "precision": "precision",
//   "numbers": [
//     {
//       "number": "number",
//       "country": "country",
//       "default": true,
//       "premium": true,
//       "city": "city",
//       "custom": true,
//       "id": 0,
//       "countryName": "countryName",
//       "state": "state",
//       "label": {
//         "default": "default"
//       },
//       "defaultSettingsInherited": true,
//       "tollfree": true
//     },
//     {
//       "number": "number",
//       "country": "country",
//       "default": true,
//       "premium": true,
//       "city": "city",
//       "custom": true,
//       "id": 0,
//       "countryName": "countryName",
//       "state": "state",
//       "label": {
//         "default": "default"
//       },
//       "defaultSettingsInherited": true,
//       "tollfree": true
//     }
//   ],
//   "meetingId": "meetingId",
//   "pstnLocalizationSupported": true,
//   "moderatorPasscode": "moderatorPasscode",
//   "useAttendeePasscode": true
// }
data class MeetingNumbersResponse(
    val moderatorPasscode: String?
)

data class MeetingModeratorResponse(
        val id: Int,
        val firstname: String,
        val lastname: String,
        val username: String
)

data class MeetingCreateRequest(val title: String,
                                val description: String? = null,
                                val timezone: String = "America/New_York",
                                val start: Long,
                                val end: Long,
                                val endPointType: String = "WEB_APP",
                                val endPointVersion: String = "2.10",
                                val addAttendeePasscode: Boolean = false,

                                val attendees: List<MeetingAttendeeRequest> = emptyList()
)

data class MeetingAttendeeRequest(val email: String)

data class InviteToMeetingRequest(val invitees: List<String>)

data class UserCreateRequest(val firstName: String,
                             val lastName: String,
                             val password: String,
                             val emailId: String,
                             val company: String,
                             val userName: String
)

data class UsersListResponse(val count: Int, val users: List<UserListUser>)
data class UserListUser(val id: Int, val uri: String)
data class UserResponse(val id: Int,
                        val username: String,
                        val firstName: String,
                        val lastName: String,
                        val emailId: String,
                        val company: String,
                        val profilePicture: String,
                        val channel_id: Int)

class BluejeansApiError(reason: String) : Exception(reason)

@Singleton
class BluejeansCredentialsProvider @Inject constructor(
        val bluejeansApi: BluejeansApi,
        val configuration: Configuration
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    lateinit var authResponse: AuthResponse

    companion object {
        val REFRESH_INTERVAL_MINUTES = 10L
    }

    val scheduledExeuctor = Executors.newScheduledThreadPool(1, object : ThreadFactory {
        override fun newThread(runnable: Runnable): Thread {
            return Executors.defaultThreadFactory().newThread(runnable).apply {
                name = "bluejeans-credentials-refresh"
                isDaemon = true
            }
        }
    }).apply {
        scheduleWithFixedDelay({
            try {
                refresh()
            } catch (e: Exception) {
                logger.error("Error refreshing bluejeans auth token", e)
            }
        }, REFRESH_INTERVAL_MINUTES, REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES)
    }

    init {
        refresh()
    }

    fun refresh() {
        if(configuration.shouldUseRealBluejeans) {

            val response = bluejeansApi.authenticate(
                AuthRequest(
                    client_id = configuration.bluejeansClientKey,
                    client_secret = configuration.bluejeansSecretKey
                )
            ).execute()

            if (response.isSuccessful) {
                response.body()?.let {
                    authResponse = it
                    return
                }
            } else {
                response.errorBody()?.let { throw BluejeansApiError(it.string()) }
                throw BluejeansApiError("Error fetching bluejeans auth token")
            }
        }
    }

    fun start() {
        // executor will start on instantiation
    }

    fun shutdown() {
        scheduledExeuctor.shutdown()
    }
}
