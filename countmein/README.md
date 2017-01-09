# IoT Platforms - Developing a Smart Push-button 

In this practical, we create a smart push-button to fulfill successfully an online poll.
To build the smart button, we use a [WeMos D1](https://www.wemos.cc/product/d1.html), a few electronic components, and the [Artik Cloud](https://artik.cloud) IoT platform.

In detail, our work plan is as follows.
First, we connect the WeMos D1 to the Internet, executing some base HTTP requests.
Second, we review the basics of the Artik Cloud platform.
Third, we connect the button to this IoT platform and use it to fulfill the online poll.
In a last step, you will be asked to deploy your own poll service online using
an online Platform-as-a-Service (PaaS).

## 1. WeMos D1 *(60')*

The WeMos D1 is an Arduino board based on the [ESP8266](https://en.wikipedia.org/wiki/ESP8266) chip.
In this practical, we assume using revision R2 of the WeMos D1 board.

### 1.1 The Arduino SDK *(15')*

To operate the WeMos D1, we use the Arduino IDE.
This integrated development environment allows to conveniently code, deploy and execute Arduino-compatible boards.

**[Task]** Install the Arduino IDE (version 1.8.0) following the instructions given [here](https://www.arduino.cc/en/main/software), then start the environment.

In the Arduino IDE, we code for our board using a spin-off of the C++ language, the so-called *Arduino language*.
In comparison to C++, this language offers several convenient shorthands.
For instance, we do not need to write down a *main*.
Instead, we have to fulfill a `setup()` and a `loop()` functions.
The former function is executed once to configure the board.
The latter runs continuously and it contains the core operations of the program.

Every Arduino board contains a chip that belongs to the [AVR](https://en.wikipedia.org/wiki/Atmel_AVR#Instruction_set) family of micro-controllers.
The Arduino IDE cross-compiles our code to the AVR instruction sets, uploads it to our board, then reboot the board in order to start the program.
Further details about this build process are available [online](https://www.arduino.cc/en/Hacking/BuildProcess).

**[Task]** By default, the Arduino IDE does not support boards based on the ESP8266 chip.
To add this support, navigate in *[File - Preferences]*, and copy the URL below in the *Additional Board Manager URLs* text box:

    http://arduino.esp8266.com/stable/package_esp8266com_index.json

Then, navigate to *[Boards - Boards Manager]* and search for "esp8266".
Install version *2.3.0* of the ESP8266 support.
The sources of this support, as well as its documentation are available on the  [following](https://github.com/esp8266/Arduino) repository.

We switch now the build process to our target board and check that everything is functional by executing a simple "Hello world" program.

**[Task]** Navigate to *[Tools - Board]*,, then select *WeMos D1 R2* board.
Connect the WeMos D1 board using a USB type A to micro-B connector, i.e., a common micro-USB plug.
Open the serial port output by navigating to *[Tools - Serial Monitor]*
The speed of the port should be set to *9600 bauds*.
Create a new *test* sketch with *[File - New]*, and copy the following lines to `setup()`:

    Serial.begin(9600);
    Serial.println("Hello World!");

Upload the code to the board and wait that it restarts.
You should notice the words "Hello World!" in the serial monitor window.

### 1.2 Setting-up the WiFi *(15')*

The ESP8266 supports the WiFi 802.11 b/g/n norms.
The steps that follow help you to connect the board to a WiFi station and check that it is functional.

**[Task]** Create a new sketch named *wifi_sketch* and include the library *ESP8266WiFi.h* at the top of your sketch.
Add two global char pointers named respectively `ssid` and `password` that contain respectively the SSID and the password of the WiFi network the board shall connect to.
Add the following code to the `setup()` function of your sketch:

    // Connect to WiFi network
    Serial.begin(9600);
    Serial.println();
    Serial.println();
    Serial.print("Connecting to ");
    Serial.println(ssid);
 
    WiFi.begin(ssid, password);
 
    while (WiFi.status() != WL_CONNECTED) {
      delay(500);
      Serial.print(".");
    }
    Serial.println("done");

Upload the code to the board, and check that it is properly connecting to the WiFi station.

### 1.2 Initiating an HTTP request *(30')*

The *SP8266WiFi.h* library contains several [modules](https://github.com/esp8266/Arduino/blob/master/doc/libraries.md#wifiesp8266wifi-library) of interest.
In particular, the WiFi client module handles a TCP/IP connection with a distant server.
The full documentation of this module is available [here](https://github.com/esp8266/Arduino/blob/master/doc/esp8266wifi/readme.md#client).

**[Task]** Connect to the main server of the Free Software Foundation (`www.fsf.org`) and retrieve the front page.
What is the header of the response returned by this server ?

## 2. The Samsung ARTIK Cloud platform *(60')*

Samsung [ARTIK](https://www.artik.io/) is an IoT platform that allows developers to construct an end-to-end IoT solution.
With more details, this platform includes
- *(Samsung ARTIK Modules)* a family of modules to prototype, integrate and evaluate IoT devices, and
- *(Samsung ARTIK Cloud)* an open data exchange platform designed to connect IoT devices.

In what follows, you will learn how to connect an IoT device to ARTIK Cloud, and how to push data to this platform.
We start with an overview of the platform, explaining how to define a device on the platform using the notion of manifest, then emulate a device to dialog with ARTIK Cloud.
Further, we connect our WeMos D1 and push some data.

### 2.1 Overview *(15')*

ARTIK Cloud provides to the developers of an IoT application an easy-to-use and open RESTful API.
In a nutshell, this API allows them to collect, store, and act on any data source, whether it be a device or some external Cloud service, applying a so-called data-driven development model.

The ARTIK Cloud IoT platform is data agnostic.
This means that to save data or to send a message to/from an external Cloud service, we may use the format of our choice.
The platform will do the work of interpreting it.
By granting access to devices and applications, the platform promotes an ecosystem of services around the data.

Clients can access and aggregate historical data from various sources, thus opening a new perspective on Big Data.
This IoT platform serves as a data broker and enables devices (e.g., sensors) to push data of interest to the Cloud.
For instance, clients that subscribe to a WebSocket may receive data in real-time, enabling different devices and applications to talk to each other.
ARTIK Cloud also provides a publish/subscribe service based on the MQTT standard.
In this practical, we however do not cover these last two capabilities of the platform.

**[Task]** Create a (free) account on the Samsung ARTIK Cloud IoT platform.
Consult the  [following](http://www.digiworldsummit.com/wp-content/uploads/2016/11/DWS16_Luc_JULIA_Samsung_Electronics.pdf) set of slides to have an overall presentation of objectives of the platform.

### 2.2 Declaring a device *(45')*

ARTIK Cloud offers to both devices and external applications a RESTful API to communicate with it.

Below, we briefly review the ideas behind the notion of RESTful API, then we cover some of its base usages in ARTIK Cloud.

#### 2.2.1 Some recalls on REST *(10')*

Representational State Transfer (REST) is an architectural style to communicate between remote services.
This notion is introduced in 2000 by R. Fielding in his doctoral [thesis](https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm).
The REST approach was developed in parallel with the HTTP 1.1 standard during the late 90's.

A RESTful service adheres to the following constraints:

- *(Client-server)* The service follows an asymmetric client-server architecture.
This means that a unique server offers operations, or *services*, that are accessed by one or more clients.
A client that executes a *request* to the service returns a *response* which is compliant to the service definition.

- *(Interface uniformity)* A service allows a client to manipulate one or more *resources*.
When the client accesses the service, she sends a *self-descriptive* message.
For each resource, the client knows a *representation* of the resource (that is, she knows a metadata model of the resource).

- *(Stateless)* The client–server communication is not constrained by a client context being stored on the server between requests.
Each request from any client contains all the information necessary to service the request, and session state is held in the client.

- *(Cacheable)* Clients may cache (full or intermediary) responses.
As a consequence, a response must, implicitly or explicitly, define itself as cacheable to prevent clients from reusing a stale or inappropriate data.

The above constraints ensure that the service is portable, scalable thus efficient, and to some extend easy to use.
Today, the REST architecture is a widely employed by Web services, that is distributed services that operate on the World Wide Web.
We commonly talk about REST-compliant, or *RESTful* Web services.
In this context, a RESTful service is defined by
- A base [URL](https://en.wikipedia.org/wiki/URL) or *end-point*, e.g., `http://api.example.com/resources`,
- A media, or [MIME](https://en.wikipedia.org/wiki/Internet_media_type), type to represent the data.
ARTIK Cloud employs to this end the [JSON](http://lagunita.stanford.edu/c4x/DB/JSON/asset/JSONIntro.pdf) format.
- The [standard HTTP methods](https://en.wikipedia.org/wiki/HTTP_method), that is GET, PUT, POST, and DELETE.
These methods allow respectively to consult, create modify and delete a resource.
We talk about [CRUD](https://en.wikipedia.org/wiki/Create,_read,_update_and_delete) operations.

**[Task]** The [API Console](https://developer.artik.cloud/api-console/) of Artik Cloud allows you to manipulate the REST API of the platform within a web browser.
Use the console to retrieve the information regarding your user profile by executing the method *GET /users/self*.
Save the access token from the "Request Headers* block.
The token is the string next to the word "Bearer", e.g.,

    {
		"Content-Type": "application/json",
        "Authorization": "Bearer 3e93c013ece641f591079d2aec402cb9"
	}

#### 2.2.2 Notion of manifest *(10')*

ARTIK Cloud is data agnostic.
This means that the platform may communicate with any device (or external application) regardless of how data is structured (as long as it follows a JSON representation).
To some extend, this implies that a device may upload data to ARTIK Cloud or communicate with another device in the format of its choice.

To achieve data agnosticism, ARTIK Cloud employs a *manifest*.
For some device type, this manifest describes the communication capabilities of every device of that particular type.
With more details, the manifest defines a set of *fields* and *actions* that determines the data that devices of this type produce and accept.
The [Developer Dashboard](https://developer.artik.cloud/) offers a simple approach to create a manifest using a Web form and a drag-and-drop interface.
This form determines the structure of the JSON payload sent by devices of this type.

**[Task]** Navigate to the *(device types)* menu, and create a new `PushButon`device type.
Create a manifest for the `PushButon` device type which contains a single boolean field named `enabled`.

At this point, we should notice that a developer may use another, so-called complex, approach to define a manifest.
In such a case, the data fields are processed following a script provided to the platform.
The interested reader may consult the [following](https://developer.artik.cloud/documentation/introduction/the-manifest.html) documentation for further details.

#### 2.2.3 Emulating a device *(20')*

In this section, we use the device emulator provided by Samsung to emulate a `MyPushButon` device and send some data to ARTIK Cloud.

**[Task]** In the [main](https://my.artik.cloud) dashboard, navigate to *[Devices - Add Another Device]* and create a `PushButon` device.
You may name this device `myPushButon`.

**[Task]** Generate a device token for `myPushButon`.
When doing so, you may notice several information of interest in the pop-up window:
- The *device ID* identifies uniquely `myPushButon` on the ARTIK platform;
- The *device type ID* indicates to which device type `myPushButon`is; and
- The *device token* serves as an entry pass to communicate from the outside with the platform under the identity of `myPushButon`.

**[Task]** Download the device emulator [here](https://developer.artik.cloud/documentation/downloads/device-simulator.jar) and start it using the following command line:

	java -jar device-simulator.jar -token=${ACCESS_TOKEN}

where `${ACCESS_TOKEN}` stands for your user access token.
Here, let us notice that we need to keep alive our Web session with ARTIK Cloud in order to communicate with the platform using our user access token.

**[Task]** In the device emulator, list your devices using the *ld* command.
This list should include the `myPushButon` device.
Create a (dummy) scenario to emulate the `myPushButon` device using the *gs* command.
Open the scenario file an modify the period to 10 seconds.

**[Task]** Run the above scenario using the *rs* command.
You can access the data pushed by the emulated device to the platform in the *Charts* and *Data Logs* menus of the [dashboard](https://my.artik.cloud).

Observe that because you are registered in the ARTIK Cloud platform as a
[free tier](https://artik.cloud/pricing) user, any of your device may not publish more than 150 messages per day.
On the other hand, you can create any number of devices on the platform to side step temporarily this limit.

## 3. A smart push-button *(90')*

It is now time to create an actual smart push-button.
This device is such that once pressed by the user, it sends a message to the ARTIK platform with the `enabled` flag set to `true`.

**[Task]** Connect a button to the WeMos D1 board following the instructions available in [this](https://www.arduino.cc/en/Tutorial/Button) online tutorial.
Check that your wiring is correct by executing a simple Arduino sketch on the board.

In general, it is more convenient to manipulate the JSON format with the help of a library.
The [ArduinoJson](https://github.com/bblanchon/ArduinoJson/wiki/Encoding-JSON) library has been created for this specific usage.
You may install it in your Arduino IDE by following *[Sketch - Include library - Manage libraries]*, then search for "json"  and install the *ArduinoJson* library.

As we pointed out previously, a device should use an authentication token when it communicates with the ARTIK Cloud platform.
This token has a pre-set expiration time.
As a consequence, you may have to renew the token of your device from time to time.
This situation comes from the fact that the ARTIK Cloud IoT platform requires secure communications.
In particular, a device may communicate autonomously with the platform under [strict conditions](https://developer.artik.cloud/documentation/advanced-features/secure-your-devices.html), that include a [trusted](https://en.wikipedia.org/wiki/Trusted_execution_environment) execution environment.

Another constraint for every IoT device to the platform is the use of the HTTPS protocol when accessing the [REST API](https://developer.artik.cloud/documentation/api-reference/rest-api.html) of ARTIK Cloud.
For the WeMos D1, we shall use to this end the  [ClientSecure](https://github.com/esp8266/Arduino/blob/master/doc/esp8266wifi/client-secure-examples.md) library of the ESP8266.

**[Task]** Complete the smart push-button by connecting it to ARTIK Cloud.

You will find an online poll server at the following [address](http://doodle-datascaletest.rhcloud.com/rest/polls) 
This server manage polls and allows to manipulate them with a REST API.
A full description of the service API used is available [here](https://github.com/otrack/doodle-rest2).

**[Task]** Create a [rule](https://my.artik.cloud/rules) in ARTIK Cloud such that once the smart button is pushed, the platform sends an HTTP message that makes you participate to the [unique](http://doodle-datascaletest.rhcloud.com/rest/polls) poll listed on the server.

To successfully record your name in the poll, we advice you to first check that you forge a correct HTTP request.
For instance, you may use this [web service](http://requestmaker.com) to forge a request, [this](https://jsonformatter.curiousconcept.com) one to check the correctnes of your JSON payload, and [that](https://www.uuidgenerator.net) one to generate a correect UUID.

## 4. Your own poll service *(optional)*

In this last step, we propose you to host your own poll service.
To this end, we will use the free tier of [OpenShift Online](https://www.openshift.com/devpreview/register.html), an online Platform-as-a-Service (PaaS) run by RedHat.

To install a poll server, you will have first to register to the platform and create a [do-it-yourself](https://developers.openshift.com/languages/diy.html) cartridge.
A full set of instructions is available [here](http://fabiomaffioletti.me/blog/2015/12/09/openshift-diy-java-8-spring-boot).

**[Task]** Create your own poll service using the OpenShift Online PaaS.
Notice that, by default, the persistent storage backed by Cassandra is disabled, and everything is stored in memory.
We advice you to keep that setting in order to simplify the service execution.
