# Start Google Speech Transcription

## Description

Start Google speech Transcription invokes the Google Speech-to-Text service, passing an audio file to be translated to 
text.

## Parameter Table

|configuration keys|description|default value|example|
|------------------|-------|-----------|-------------|
|source-flavor|The flavor of the audio file to be sent for translation.|EMPTY|presenter/delivery|
|source-tag|The flavor of the audio file to be sent for translation..|EMPTY|transcript-audio|
|skip-if-flavor-exists|If this flavor already exists in the media package, skip this operation.<br/>To be used when the media package already has a transcript file.|false|captions/timedtext|

**One of source-flavor or source-tag must be specified.**

## Example

```xml
<!-- Extract audio from video in ogg/opus format -->

<operation
  id="compose"
  fail-on-error="true"
  exception-handler-workflow="ng-partial-error"
  description="Extract audio for transcript generation">
  <configurations>
    <configuration key="source-tags">engage-download</configuration>
    <configuration key="target-flavor">audio/ogg</configuration>
    <configuration key="target-tags">transcript</configuration>
    <configuration key="encoding-profile">audio-opus</configuration>
    <!-- If there is more than one file that match the source-tags, use only the first one -->
    <configuration key="process-first-match-only">true</configuration>
  </configurations>
</operation>

<!-- Start Google Speech transcription job -->

<operation
  id="start-google-speech-transcription"
  fail-on-error="true"
  exception-handler-workflow="ng-partial-error"
  description="Start Google Speech transcription job">
  <configurations>
    <!--  Skip this operation if flavor already exists. Used for cases when mp already has captions. -->
    <configuration key="skip-if-flavor-exists">captions/timedtext</configuration>
    <!-- Audio to be translated, produced in the previous compose operation -->
    <configuration key="source-tag">transcript</configuration>
  </configurations>
</operation>
```

#### Encoding profile used in example above
```
profile.audio-flac.name = audio-flac
profile.audio-flac.input = stream
profile.audio-flac.output = audio
profile.audio-flac.suffix = -audio.flac
profile.audio-flac.mimetype = audio/flac
profile.audio-flac.ffmpeg.command = -i /#{in.video.path} -ac 1 #{out.dir}/#{out.name}#{out.suffix}
```

