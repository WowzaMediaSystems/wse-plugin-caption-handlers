#!/bin/bash

if [ -f "/wowza_setup/downloads/wse-plugin-caption-handlers-1.3.1.jar" ]; then
	echo "File wse-plugin-caption-handlers-1.3.1.jar exists"
else
	rm -f /wowza_setup/downloads/wse-plugin-caption-handlers*
	echo "Getting wse-plugin-caption-handlers-1.3.1.jar"
	wget -q -P /wowza_setup/downloads/ https://github.com/WowzaMediaSystems/wse-plugin-caption-handlers/releases/download/1.3.1/wse-plugin-caption-handlers-1.3.1.jar
fi

if [ -f "/wowza_setup/downloads/client-sdk-1.47.0.jar" ]; then
	echo "File client-sdk-1.47.0.jar exists"
else
	rm -f /wowza_setup/downloads/client-sdk-*
	echo "Getting client-sdk-1.47.0.jar"
	wget -q -P /wowza_setup/downloads/ https://github.com/WowzaMediaSystems/wse-plugin-caption-handlers/releases/download/1.3.1/client-sdk-1.47.0.jar
fi

