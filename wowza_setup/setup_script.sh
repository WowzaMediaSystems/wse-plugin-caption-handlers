#!/bin/bash

if [ -f "/wowza_setup/downloads/wse-plugin-caption-handlers-1.1.0.jar" ]; then
	echo "File wse-plugin-caption-handlers-1.1.0.jar exists"
else
	echo "Getting wse-plugin-caption-handlers-1.1.0.jar"
	wget -q -P /wowza_setup/downloads/ https://github.com/WowzaMediaSystems/wse-plugin-caption-handlers/releases/download/1.1.0/wse-plugin-caption-handlers-1.1.0.jar
	echo "Getting client-sdk-1.44.0.jar"
	wget -q -P /wowza_setup/downloads/ https://github.com/WowzaMediaSystems/wse-plugin-caption-handlers/releases/download/1.1.0/client-sdk-1.44.0.jar
fi