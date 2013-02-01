ANDROID_SDK_PARENT = ~
ANDROID_SDK = android-sdk-linux
ANDROID_TOOLS = $(ANDROID_SDK_PARENT)/$(ANDROID_SDK)/tools

SERVER = gcm-demo-server
SERVER_SOURCE = $(SERVER)/dist/gcm-demo.war
SERVER_DESTINATION = /usr/share/jetty/webapps/gcm-demo.war

CLIENT = gcm-demo-client
CLIENT_BUILD_DEPS = $(CLIENT)/build.xml $(CLIENT)/proguard-project.txt
CLIENT_APK = $(CLIENT)/bin/GCMDemo-debug.apk

DEVICE_NAME = Android

all: server client

server: $(SERVER_DESTINATION) $(SERVER_SOURCE)

$(SERVER_DESTINATION): $(SERVER_SOURCE)
	service jetty stop
	cp $(SERVER_SOURCE) $(SERVER_DESTINATION)

startServer:
	service jetty start

restartServer:
	service jetty stop
	service jetty start

$(SERVER_SOURCE): $(SERVER)
	ant -f $(SERVER)/build.xml war

installClient: $(CLIENT_APK)
	ant -f $(CLIENT)/build.xml installd

startEmulator:
	$(ANDROID_TOOLS)/emulator -avd $(DEVICE_NAME)

client: $(CLIENT_APK) $(CLIENT_BUILD_DEPS)

$(CLIENT_APK): $(CLIENT_BUILD_DEPS)
	ifconfig
	echo "Get IP address and set it in CommonUtilities"
	emacs -nw $(CLIENT)/src/com/google/android/gcm/demo/app/CommonUtilities.java
	ant -f $(CLIENT)/build.xml clean debug

$(CLIENT_BUILD_DEPS): $(CLIENT)
	$(ANDROID_TOOLS)/android update project --name GCMDemo -p $(CLIENT) --target android-16
