# Wowza Caption Handlers
The **Caption Handlers** module for [Wowza Streaming Engine™ media server software](https://www.wowza.com/products/streaming-engine) enables you to track your streaming connections in the Google Analytics service.

## Prerequisites
* Wowza Streaming Engine™ 4.9.4 or later is required.
* Java 21.
* For the Azure Speech to Text module, you will also need an Azure account with the Speech service enabled.
* For the Whisper module, you will need a Whisper server configured.

## Usage
The easiest way to test this module is to use the included docker compose file. This will start a Wowza Streaming Engine instance with the modules installed and configured to use Azure Speech Services or Whisper to create the captions.

## Build instructions
* Clone repo to local filesystem.
* Update `wseLibDir` variable in the `gradle.properties` file to point to local _Wowza Streaming Engine_ `lib` folder.
* Run `./gradlew build` to build the jar file.

## Configuration
See the included Application.xml files for the module configurations.

## More resources
For full install instructions and to use the compiled version of these modules, see the following articles:
* [Convert speech to text to generate captions for live streams with Azure AI Speech Services](https://www.wowza.com/docs/convert-speech-to-text-to-generate-captions-for-live-streams-with-azure-ai-speech-services)
* [Convert speech to text to generate captions for live streams with OpenAI Whisper](https://www.wowza.com/docs/convert-speech-to-text-to-generate-captions-for-live-streams-with-openai-whisper)

[Wowza Streaming Engine Server-Side API Reference](https://www.wowza.com/resources/serverapi/)

[How to extend Wowza Streaming Engine using the Wowza IDE](https://www.wowza.com/docs/how-to-extend-wowza-streaming-engine-using-the-wowza-ide)

Wowza Media Systems™ provides developers with a platform to create streaming applications and solutions. See [Wowza Developer Tools](https://www.wowza.com/developer) to learn more about our APIs and SDK.

## Contact
[Wowza Media Systems, LLC](https://www.wowza.com/contact)

## License
This code is distributed under the [Wowza Public License](/LICENSE.txt).
