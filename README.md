# Wowza Caption Handlers
The **Caption Handlers** module for [Wowza Streaming Engine™ media server software](https://www.wowza.com/products/streaming-engine) enables you to generate CEA608/708 or WebVTT subtitles for live streams using the Azure speech to text service, or the Whisper ASR system.

## Prerequisites
* Wowza Streaming Engine™ 4.9.4 or later is required.
* Java 21.
* For the Azure Speech to Text module, you will also need an Azure account with the Speech service enabled.
* For the Whisper module, you will need a Whisper server configured.

## Usage
The easiest way to test this module is to use the included docker compose file. This will start a Wowza Streaming Engine instance with the modules installed and configured to use Azure Speech Services or Whisper to create the captions.

## Test
Below are 4 quick steps to test captions using the OpenAI Whisper service.
1. Update docker-compose.yaml with your Wowza Streaming Engine License Key (`$WSE_LICENSE_KEY`)
2. Launch services with docker compose
```
docker compose up
```
3. Push a video to your Wowza Streaming Engine service with `rtmp://wse-demo.wowza.com/whisper/myStream` 

> Example ffmpeg command:
> ```
>ffmpeg -re -y -stream_loop -1 -i <yourfile.mp4> -vcodec libx264 -f flv rtmp://wse-demo.wowza.com/whisper/myStream
>```

4. Playback with the HLS Test Player and select English Captions after ~20s:
https://www.wowza.com/testplayers?src=https://wse-demo.wowza.com/whisper/myStream_delayed/playlist.m3u8

## Build instructions
* Clone repo to local filesystem.
* Update `wseLibDir` variable in the `gradle.properties` file to point to local _Wowza Streaming Engine_ `lib` folder.
* Run `./gradlew build` to build the jar file.

## Configuration
See the included Application.xml files for the module configurations.

### Whisper
To run whisper with GPU and docker compose, uncomment the following lines:
```
...
        # runtime: nvidia
        # deploy:
        #   resources:
        #     reservations:
        #       devices:
        #         - driver: nvidia
        #           capabilities: [gpu]
        #           count: 1
...        

            # - USE_GPU=True
            # - FP16=true
...
```
Update the whisper docker image to use the gpu image:
```
    whisper_server:
        hostname: whisper.server
        image: wowza/whisper_streaming:latest-gpu
```
### Translation
To run translation and docker compose, uncomment the following lines:
```
...
            # - TRANSLATE_HOST=libretranslate.server
            # - TRANSLATE_PORT=5000

...
    # libretranslate_server:
    #     hostname: libretranslate.server
    #     image: libretranslate/libretranslate:latest
    #     # image: libretranslate/libretranslate:latest-cuda 
    #     environment:
    #         - LT_LOAD_ONLY=en,fr,es,de,ja
    #     ports:
    #         - 5001:5000
    #     volumes:
    #         - /tmp/libretranslate_models_cache:/home/libretranslate/.local
...
```
Update the `REPORT_LANGUAGES` environment variable in the Docker Compose file to match what languages you want to translate to.
```
            - REPORT_LANGUAGES=en,fr,es,de,ja
```
Match those languages with this section in [`whisper/Application.xml`](https://github.com/WowzaMediaSystems/wse-plugin-caption-handlers/blob/main/conf/whisper/Application.xml)
```
		<TimedText>
...
			<Properties>
				<Property>
					<Name>captionLiveIngestLanguages</Name>
					<Value>en,fr,es,de,ja</Value>
				</Property>
			</Properties>
```

## More resources
For full install instructions and to use the compiled version of these modules, see the following articles:

[Convert speech to text to generate captions for live streams with Azure AI Speech Services](https://www.wowza.com/docs/convert-speech-to-text-to-generate-captions-for-live-streams-with-azure-ai-speech-services)

[Convert speech to text to generate captions for live streams with OpenAI Whisper](https://www.wowza.com/docs/convert-speech-to-text-to-generate-captions-for-live-streams-with-openai-whisper)

[Wowza Streaming Engine Server-Side API Reference](https://www.wowza.com/resources/serverapi/)

[How to extend Wowza Streaming Engine using the Wowza IDE](https://www.wowza.com/docs/how-to-extend-wowza-streaming-engine-using-the-wowza-ide)

Wowza Media Systems™ provides developers with a platform to create streaming applications and solutions. See [Wowza Developer Tools](https://www.wowza.com/developer) to learn more about our APIs and SDK.

## Contact
[Wowza Media Systems, LLC](https://www.wowza.com/contact)

## License
This code is distributed under the [Wowza Public License](/LICENSE.txt).
