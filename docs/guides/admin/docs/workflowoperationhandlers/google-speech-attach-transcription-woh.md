# Google Speech Attach Transcription

## Description

Google Speech Attach Transcription converts the json format file received from the Google Speech-to-Text service 
into the desired caption format and adds it to the media package.

## Parameter Table

|configuration keys|description|default value|example|
|------------------|-------|-----------|-------------|
|transcription-job-id|This is filled out by the transcription service when starting the workflow.|EMPTY|**Should always be "${transcriptionJobId}"**|
|line-size|The line size (number of characters) of the transcripts to display at a time. Optional.|EMPTY|100|
|target-flavor|The flavor of the caption/transcription file generated. Mandatory.|EMPTY|captions/timedtext|
|target-tag|The tag to apply to the caption/transcription file generated. Optional.|EMPTY|archive|
|target-caption-format|The caption format to be generated. Optional. If not entered, the raw resulting file will be attached to the media package.|EMPTY|webvtt|

## Example

```xml
<!-- Attach caption/transcript -->

<operation id="google-speech-attach-transcription"
  fail-on-error="true"
  exception-handler-workflow="manchester-error" 
  description="Attach captions/transcription">
  <configurations>
    <!-- This is filled out by the transcription service when starting this workflow -->
    <configuration key="transcription-job-id">${transcriptionJobId}</configuration>
    <configuration key="line-size">100</configuration>
    <configuration key="target-flavor">captions/timedtext</configuration>
    <configuration key="target-tag">archive</configuration>
    <configuration key="target-caption-format">webvtt</configuration>
  </configurations>
</operation>

<!-- Re-publish to OAI-PMH -->

<operation
  id="republish-oaipmh"
  exception-handler-workflow="manchester-error"
  description="REPUBLISH: Update recording in default OAI-PMH repository">
  <configurations>
    <configuration key="source-flavors">dublincore/*,security/*,captions/*</configuration>
    <configuration key="repository">default</configuration>
    <configuration key="merge">true</configuration>
  </configurations>
</operation>
```
