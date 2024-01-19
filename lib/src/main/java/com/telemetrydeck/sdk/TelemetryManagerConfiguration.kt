package com.telemetrydeck.sdk

import java.net.URL
import java.util.*


data class TelemetryManagerConfiguration(
    /**
     * Your app's ID for Telemetry. Set this during initialization.
     */
    var telemetryAppID: UUID,

    /**
     * The domain to send signals to. Defaults to the default Telemetry API server.
     * (Don't change this unless you know exactly what you're doing)
     */
    var apiBaseURL: URL = URL("https://nom.telemetrydeck.com"),

    /**
     * If `true`, sends a "newSessionBegan" Signal on each app foreground or cold launch
     * Defaults to true. Set to false to prevent automatically sending this signal.
     *
     * */
    var sendNewSessionBeganSignal: Boolean = true,


    /**
     * A random identifier for the current user session.
     * On iOS, tvOS, and watchOS, the session identifier will automatically update whenever your app returns from background, or if it is
     * launched from cold storage. On other platforms, a new identifier will be generated each time your app launches. If you'd like
     * more fine-grained session support, write a new random session identifier into this property each time a new session begins.
     * */
    var sessionID: UUID = UUID.randomUUID(),

    /**
     * If `true` any signals sent will be marked as *Testing* signals.
     * Testing signals are only shown when your Telemetry Viewer App is in Testing mode. In live mode, they are ignored.
     *
     * By default, this is the same value as `DEBUG`, i.e. you'll be in Testing Mode when you develop and in live mode when you release. You can manually override this, however.
     * */
    var testMode: Boolean = false,

    /**
     * Log the current status to the signal cache to the console.
     * */
    var showDebugLogs: Boolean = false,

    /**
     * Instead of specifying a user identifier with each `send` call, you can set your user's name/email/identifier here and
     * it will be sent with every signal from now on.
     * Note that just as with specifying the user identifier with the `send` call, the identifier will never leave the device.
     * Instead it is used to create a hash, which is included in your signal to allow you to count distinct users.
     *
     * */
    var defaultUser: String? = null,

    /**
     * This string will be appended to to all user identifiers before hashing them.
     *
     * Set the salt to a random string of 64 letters, integers and special characters to prevent the unlikely
     * possibility of uncovering the original user identifiers through calculation.
     *
     * Note: Once you set the salt, it should not change. If you change the salt, every single one of your
     * user identifers wll be different, so even existing users will look like new users to TelemetryDeck.
     * */
    var salt: String? = null,
) {
    constructor(telemetryAppID: String): this(telemetryAppID=UUID.fromString(telemetryAppID))
}
