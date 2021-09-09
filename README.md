# KotlinClient
Kotlin Client for sending Signals to TelemetryDeck

## Spec
In general, TelemetryDeck Signals are sent as HTTP requests to TelemetryDeck's signal ingestion API. Currently, one signal per request is accepted, but in the future, an array of Signal will also be supported, to collect signals before sending them off in bulk. Here's the [documentation for the Swift library](https://telemetrydeck.com/pages/sending-signals.html) for reference.

The library should be initialized at app startup with the app identifier. This is how that looks in Swift:

```swift
let configuration = TelemetryManagerConfiguration(appID: "YOUR-APP-UNIQUE-IDENTIFIER")
TelemetryManager.initialize(with: configuration)
```

Sending signals is one non-blocking call. Here is how that looks in swift:

```swift
// with no additional payload and default user 
TelemetryManager.send("applicationDidFinishLaunching")

// with custom payload and custom user id
TelemetryManager.send(
    "applicationDidFinishLaunching",
    for: "myverycooluser@example.org",
    with: [
        "numberOfTimesPizzaModeHasActivated": "\(dataStore.pizzaMode.count)",
        "pizzaCheeseMode": "\(dataStore.pizzaCheeseMode)"
    ])
```

Further reading: [How to send Signals via JavaScript](https://telemetrydeck.com/pages/website-telemetry.html).

### API Endpoint

Signals should be sent as JSON body to this URL:

```
https://nom.telemetrydeck.com/api/v1/apps/<YOUR-APP-ID>/signals/
```

where `<YOUR-APP-ID>` is the App ID of the app the signal was generated in. Users can get the App ID from the Telemetry Viewer App.

These are the headers that should be sent with the HTTP request:

- Accept: 'application/json'
- Content-Type: 'application/json'

### Signal Type: `type`

Signals always have a type. This a short string that describes the event that caused the signal to be sent. It is recommended to use short, camel-cased half-sentences. 

### Signal Client User: `clientUser`

Signals have a `clientUser` property, which stores a string. All signals with the same client user property will be assumed to originate from the same user. 

Whatever string a developer hands into the `clientUser` property, it **must** be hashed before being sent to the server, so that the information, e.g. an email address, cannot be traced back to that user.

If the developer hands a null value into the the `clientUser` property, the library should use a reasonable default value, such as a device ID, or an app specific ID. On iOS, this default is the [identifierForVendor](https://developer.apple.com/documentation/uikit/uidevice/1620059-identifierforvendor) UUID, which allows us to recognize recurrign users without infringing on their privacy.

### Payload Metadata: `payload`

Signals have a metadata payload dictionary that contains things like platform, os version, and any data a user adds throw in there. This is highly useful for filtering and aggregation insights.

The default client library should automatically send a base payload with these keys:

- platform, i.e. "Android", "Linux" or "Windows"
- systemVersion, i.e. "Android 6.0.1"
- appVersion, the app version
- buildNumber, the apps build number (if applicable)
- modelName, a description of the device the app runs on, e.g. "Samsung Note 4 6.0 MARSHMALLOW"
- architecture, i.e. "arm64" or "intel64"

If the user passes in a dictionary of String keys and String values, this dictionary should be appended to the default payload. If duplicate keys are present, the user's values should take precedent and overwrite the default values.

### Session ID: `sessionID`

Clients are encouraged to pass on a session ID, which should stay the same until the session ends. What defines a session might be different depending on various circumstances. In an app, it should usually be until the app enters the background, or is closed. On a website, a cookie that stays until the browser window is closed might be the correct approach.

### Optional Generation Date: `receivedAt`

The signal JSON can optionally include a date parameter `receivedAt`, which should contain the point in time the signal was generated, in ISO 8601 format with time zone. If this parameter is not present, or set to null, the server will use the point in time the Signal was received. This feature can be used to cache up signals when no network connection is present. When in doubt, default to not setting the `receivedAt` parameter.

### Signal JSON

Here is an example signal:

```json
{
    "type":"PlayAction",
    "clientUser":"C00010FFBAAAAAADDEADFA11",
    "sessionID": "DEADBEEF",
    "payload": {
      "isAppStore": "true",
      "platform": "iOS",
      "targetEnvironment": "native",
      "buildNumber": "2",
      "signalClientUser": "C00010FFBAAAAAADDEADFA11",
      "isSimulator": "false",
      "modelName": "iPhone12,1",
      "operatingSystem": "iOS",
      "isTestFlight": "false",
      "appVersion": "3.12.0",
      "architecture": "arm64",
      "systemVersion": "iOS  14.4.2",
      "signalType": "PlayAction"
    }
}
```
