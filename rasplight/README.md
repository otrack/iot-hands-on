# RaspLight - Simple Lightning Effects with a Raspberry Pi

In this practical, we create a UPnP service to manipulate a LED connected to a Raspberry Pi.
To this end, we first set-up the Raspberry Pi, then we create a UPnP service that allows to light up the LED.
Further, we connect an actual LED to the device and control it by calling the UPnP service.

## 1. Setting-up the Raspberry Pi

Our first step is to install an operating system on the Raspberry Pi.
To this end, we use the *Lite* version of *Raspbian*, the Debian-flavored Linux for Raspberry Pi.
Below, we present an "headless" installation, that is it does not require the use of a monitor but solely connecting the device to the network.

### 1.1 Operating System

To install an operating on the Raspberry Pi, we follow the instructions given [online](https://www.raspberrypi.org/documentation/installation/installing-images/README.md).
Such instructions are recalled for the Linux operating system below.

First of all, we need to [download](https://www.raspberrypi.org/downloads/raspbian) the operating system image.
Assume that the name of the image we downloaded is `image.img`.
We notice that we cannot mount the image as a whole as it actually contains two partitions (and a boot sector as well).
However, we can mount each partition, provided we know their respective offsets in the file.
To find these offsets, we may examine the image as a block device using the command `fdisk -l image.img`.
The output will include a table like this:

    Device Boot Start End Blocks Id System
    image.img1 8192 122879 57344 c W95 FAT32 (LBA)
    image.img2 122880 5785599 2831360 83 Linux

In the table above, we may observe each of the two partitions.
The first one is labelled "FAT32", and the other one "Linux".
Above this table, there is some other information about the device as a whole, including:

    Units: sectors of 1 * 512 = 512 bytes

We can find the offset in bytes by multiplying this unit size by the Start block of the partition.
This means that the first  partition is located at offset `512 * 8192 = 4194304`, while the offset of the second partition is `512 * 122880 = 62914560`.

This information may be used with the offset option of the mount command.
We also have a clue about the type of each partition from `fdisk`.
So, presuming we have directories `/mnt/img/one` and `/mnt/img/two` available as two mount points, we may mount each partition as follows:

    mount -v -o offset=4194304 -t vfat whatever.img /mnt/img/one
    mount -v -o offset=62914560 -t ext4 whatever.img /mnt/img/two

At this point, let us consider that the SD card of the Raspberry PI is readable through `/dev/sdc`.
We load the image containing the two partitions and the boot sector on the SD card as follows:

    sudo dd bs=4M if=image.img of=/dev/sdc

### 1.2 Installing SSH

We now have to start a [SSH](https://en.wikipedia.org/wiki/Secure_Shell) service on the Raspberry Pi.
In what follows, we shall use this service to connect to the device, e.g., for debugging purposes.

Let us first mount the second (Linux) partition, for instance, to `/mnt/img`.

	mount -t ext4 /dev/sdc2 /mnt/img

Our next task is to generate a pair of public and private keys for the server.
To this end, we proceed as follows.

    ssh-keygen -t rsa -f //mnt/img/etc/ssh/ssh_host_rsa_key

Then, we should start the service.
A simple approach is to add the following line to `//mnt/img/etc/rc.local`.

    /etc/init.d/ssh start

We now have to find the [Internet Protocol](https://en.wikipedia.org/wiki/Internet_Protocol) (IP) address of the Raspberry upon a new start.
One way of doing so is to configure the host in `/etc/hostname` and `/etc/hosts`.
For instance, we may write `myRaspberryPI` in the former file, and `127.0.0.1	localhost myRaspberryPI` in the latter.

In case, the [DNS](https://en.wikipedia.org/wiki/Domain_Name_System) of our domain does not allow to register a chosen name, we provide an alternative approach below.
We first boot up the device and write down the IP address to a temporary file.
As an example, we may achieve this with the following addition to `//mnt/img/etc/rc.local`.

    echo $_IP > /root/ip

After having connected the device with a [cable](https://en.wikipedia.org/wiki/Modular_connector#8P8C) to the Ethernet network, we power it up, wait around 10 seconds and power it off.
We are now able to read the IP address with the SD card reader in the file `/root/ip`.
This allows us to connect remotely to the Raspberry Pi through SSH by using a command of the form:

    ssh pi@raspberrypi

Notice that by default the password for the `pi` user is `raspberry`.

## 2. A UPnP Light 

Universal Plug and Play (UPnP) is a [standard](https://openconnectivity.org/resources/specifications/upnp/specifications) that allows mobile devices to discover each others presence and to call remote services.
In this section, we first briefly cover the UPnP standard then the [Cling](http://4thline.org/projects/cling) Java/Android UPnP library.
Further, we define the service that we will have to implement on the Raspberry PI.
Notice that it is not mandatory to employ Cling in this practical.
The reader may use an alternate library, e.g., [cybergarage-upnp](https://github.com/cybergarage/cybergarage-upnp).

### 2.1 UPnP in a nutshell

In what follows, we cover the basics of UPnP.

*(Architecture)*
UPnP allows to declare and control remote services.
A *service* is embedded in a *device*.
A device may contain or more more *embedded* devices.
A client, or *control point*, accesses remote services by sending *actions*.
It may also listen to state modifications of a service via an *eventing* mechanism.
UPnP is based upon well-known Internet technologies, namely [HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol), [SOAP](https://en.wikipedia.org/wiki/SOAP) and [XML](https://en.wikipedia.org/wiki/XML) over IP.
This set of protocols serve to announce a service, provide its description, execute an action on a distant service, or to notify a listener.

*(Addressing)*
The foundation for UPnP networking is IP addressing.
Each device implements a DHCP client.
This client searches for a DHCP server when the device is connected to the network.
If no DHCP server is available, the device must assign itself an address.

*(Discovery)*
Once a device has an IP address, it starts discovering its surroundings.
The UPnP discovery protocol is known as the Simple Service Discovery Protocol ([SSDP](https://en.wikipedia.org/wiki/Simple_Service_Discovery_Protocol)).
To search for a device or to advertise a service, SSDP employs HTTP over UDP and IP multicast, a combination known as [HTTPMU](http://tools.ietf.org/html/draft-goland-http-udp-01).
Responses to search requests are also sent over UDP, but they are simply unicast.

When a device is added to the network, SSDP allows that device to advertise its services via SSDP alive messages.
On the other hand, if a control point enters the network, it employs SSDP to search for devices of interest and to listen passively to alive messages.
UPnP employs the port 1900.
All TCP ports used hereafter are derived from SSDP alive and response messages.

*(Description)*
After a control point discovers a device, it retrieves the description of the device from the URL provided by the device in the discovery message.
The *Device Description* is expressed in XML.
It includes vendor-specific manufacturer information like the model name and number, the serial number, the manufacturer name, and other vendor-specific information.
The device description includes one or more *service descriptions*.
It may also include a list of *embedded* devices.
A service description lists the commands, or *actions*, to which the service responds, and its parameters, or *arguments*, for each action.
The description also includes the *state variables* of the service.
Such variables are described in terms of data type, range, and event characteristics.

*(Control)*
Having retrieved a description of the device, the control point can send actions to a device's service.
To do this, a control point sends a suitable control message to the control URL for the service (provided in the device description).
Control messages are also expressed in XML using the Simple Object Access Protocol (SOAP).
Much like function calls, the service returns any action-specific values in response to the control message.
The effects of the action, if any, are modeled by changes in the variables that describe the run-time state of the service.

*(Event notification)*
Another capability of UPnP is its eventing mechanism.
The event notification protocol defined in UPnP is known as the *General Event Notification Architecture* [GENA](https://en.wikipedia.org/wiki/GENA).
As stated above, a service lists the actions it responds to, as well as the variables that model its state at run time.
The service publishes updates when these variables change, and a control point may subscribe to this eventing mechanism to receive this information.
Event messages contain the names of one or more state variables and their current values.
These messages are also expressed using XML.

## 2.1 The Cling Library (optional)

Cling is a UPnP software stack for Java built upon a [core](http://4thline.org/projects/cling/core) library.
The sources of Cling are open and available on [GitHub](https://github.com/4thline/cling).
The Cling project also contains several tools to manipulate and discover UPnP devices, as well as a media server.
These tools are however not necessary for this practical.

In what follows, we briefly introduce the Cling core library and how to declare a service using it.
For further details, the reader may refer to the [online](http://4thline.org/projects/cling/core/manual/cling-core-manual.html) manual.

Cling uses annotations (JavaSE >= 1.5) to specify a service.
Recall that a [custom annotation](https://en.wikipedia.org/wiki/Java_annotation#Custom_annotations) in Java is of the form `@Something`.
It is similar to a regular interface declaration, where the at-sign (`@`) precedes the interface keyword (`Something`).
For instance,

    public @interface Openable {
      boolean open() default false;
    }

define an interface named `Openable` that includes an `open()` method which returns a boolean whose default value equals `false`.
The above definition is typical of a custom annotation.
In particular, method declarations cannot have parameters or `throws` clauses.

Cling uses annotations to generate the xml file that describes the service.
With more details, to specify a service we annotate the class using the following idiom:

    @UpnpService(
        serviceId = @UpnpServiceId("MyService"),
        serviceType = @UpnpServiceType(value = "MyService", version = 1)
    )


In the code above, `@UpnpServiceId(value = "MyService", version = 1)` is a short-hand for an anonymous object type with methods `value()` and `version()` returning respectively `"SwitchPower"` and `1`.

To define a state variable for a device, we annotate one of the field with `@UpnpStateVariable`.
For instance,

    (defaultValue = "0")
    private boolean myField = false;

declare that `myField` is a state variable whose default value equals `0`.
Similarly, we may define an action in the UPnP service as follows:

    public void setTarget(@UpnpInputArgument(name = "MyField") boolean anArgument){
        // something of interest
	}

Notice that in the above idiom we use `@UpnpOutputArgument(name = "MyField")` to refer to the state variable to use as a type for argument `anArgument`.

In Cling, devices (and embedded devices) are created pro grammatically as an immutable graph of objects.
On that topic, we let the reader refers to the [following](http://4thline.org/projects/cling/core/manual/cling-core-manual.html#section.BindingDevice) part of the online manual.
This manual also contains instructions to create a control point and to communicate with remote devices pro grammatically using the Cling core library.

## 2.2 The Light Service

**[Task]** Create a `Light` class which contains a `boolean status` field.
This field indicates whether the light is on or off.
Define a (public) method `void setStatus(boolean newStatus)` to change the status of the light.
The method `boolean getStatus()` returns the status of the light.

**[Task]** Implement a UPnP service named `Light`that interfaces an object of the `Light` class.

**[Task]** Implement a UPnP device named `RaspLight` that offer the `Light` service.

**[Task]** Start a `RaspLight` device and dialog with it using a dummy control point.
For instance, you may list the name of the device, its `Light` service as well as the actions available through the service.

**[Task]** Implement a more complex control point that allows to remotely call the `setStatus()` action of the `Light` service to switch on/off the light.

**[Task]** Install a Java Development Kit to run the `RaspLight` device on the Raspberry Pi.
Manipulate the device remotely with the client.

## 3. Putting Things Together 

In this section, we connect an actual light to the Raspberry PI and manipulate it via the Light service.
To this end, we have to form a circuit using a resistor.
The resistor depends on the voltage required by the LED.

**[Task]** Choose a LED and an appropriate resistor.
Create a circuit that connects the LED and the resistor to the Raspberry PI using the General-purpose Input/Output Pins.
The information regarding the input/output pins are available in the [following](https://thepihut.com/blogs/raspberry-pi-tutorials/27968772-turning-on-an-led-with-your-raspberry-pis-gpio-pins) blog entry.
In particular, you may test that the LED is working by using the small python script below.

	import RPi.GPIO as GPIO
	import time
	GPIO.setmode(GPIO.BCM)
	GPIO.setwarnings(False)
	GPIO.setup(18,GPIO.OUT)
	print "LED on"
	GPIO.output(18,GPIO.HIGH)
	time.sleep(1)
	print "LED off"
	GPIO.output(18,GPIO.LOW)

**[Task]** Use the [following](http://pi4j.com/) Java GPIO library to switch on/off the LED with the Light service.
