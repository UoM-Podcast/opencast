Transcripts (Automated by Google Speech)
=====================================

Overview
--------

The GoogleSpeechTranscriptionService invokes the Google Speech-to-Text service via REST API to translate audio to text.

During the execution of an Opencast workflow, an audio file is extracted from one of the presenter videos and sent 
to the Google Speech-to-Text service. When the results are received, they are converted to the desired caption format and attached to the media package.

Workflow 1 runs:

  - Audio file created
  - Google Speech-to-Text job started
  - Workflow finishes

Translation finishes, workflow 2 is started.

Workflow 2 runs:

  - File with results is converted and attached to media package
  - Media package is republished with captions/transcripts

Google Speech-to-Text service documentation, including which languages are currently supported, can be found
 [here](https://cloud.google.com/speech-to-text/docs/basics).

Configuration
-------------

### Step 1: Activate Google Speech and Google Cloud APIs
* Log in to your Google account and [Activate a 12 months Google Cloud Platform services](https://cloud.google.com/free/)
* [Create a Project to store your credentials and billing information] (https://console.cloud.google.com/getting-started)
   - Click 'Select a project' to create new project or use existing project
* Enable Google Speech API
   - Expand the menu near the title 'Google Cloud Platform
   - Go to 'APIs & Service' > Libraries
   - Find the Cloud Speech API and click 'Enable' to enable the Google Cloud Speech API
* Enable Google Cloud Storage and Google Cloud Storage JSON API
   - Go to 'APIs & Service' > Libraries
   - Find Google Cloud Storage and Google Cloud Storage JSON API and enable them
* Create a cloud storage bucket. This is where you will temporary host the files you want to transcribe
   - Go to https://console.cloud.google.com/home/dashboard
   - Expand the menu and go to Storage > Browser
   - Click 'CREATE BUCKET' to create the a bucket

### Step 2: Get Google Speech credentials
* Log in to your Google account and go to [Google Cloud Platform services](https://cloud.google.com/)
- Go to 'APIs & Service' > Credentials  and click on the tab "OAuth Consent Screen". Fill in a "Project name" and Save it. Don't worry about the other fields.
(screenshot here)

- Then go back to Credentials, click the button that says "Create Credentials" and select "OAuth Client ID"
(screenshot here)

Choose "Web Application" and give it a name.
Add https://developers.google.com/oauthplayground in "Authorized redirect URIs". You will need to use this in the next step to get your refresh token
(screenshot here)

Click Create and take note of your Client ID and Client Secret
(screenshot here)

* Getting your Refresh Token and Authorization enpdpoint
- Go to https://developers.google.com/oauthplayground.
Notes: Make sure you added this URL to your Authorized redirect URIs in the previous step.

In the top right corner, click the settings icon
- Take note of your "Token endpoint"
- Make sure the Access token location is set to: 'Authorization header w/ Bearer prefix'
- Make sure Access type is set to 'Offline'
- Check "Use your own OAuth credentials" and paste your Client ID and Client Secret.
- CLose the settings
(screenshot here)

Select the scope of your APIs
- Find Cloud Speech API and click on 'https://www.googleapis.com/auth/cloud-platform' to select it.
- Find Cloud Storage API from the list, expand it and click on 'https://www.googleapis.com/auth/devstorage.full_control' to select it
- Find Cloud Storage JSON API, expand it and select 'https://www.googleapis.com/auth/devstorage.full_control'
(screenshot here)

Click "Authorize APIs" and allow access to your account when prompted. There will be a few warning prompts, just proceed.
When you get to step 2, click "Exchange authorization code for tokens".
(screenshot here)
You will need the OAuth Client ID, OAuth Client secret ,the Refresh token and Token endpoint for the configuration file

### Step 3: Configure GoogleSpeechTranscriptionService
Edit  _etc/org.opencastproject.transcription.googlespeech.GoogleSpeechTranscriptionService.cfg_:

- Set _enabled_=true
- Use OAuth Client ID, OAuth Client secret, Refresh token and Token endpoint obtained above to set _google.cloud.client.id_ , _google.cloud.client.secret_ , _google.cloud.refresh.token_ and  _google.cloud.token.enpoint_
- Enter the appropriate language in _google.speech.language_, default is (_en-UK_)
- Remove profanity (bad language) from transcription by using _google.speech.profanity.filter_, default is (_false_), not removed by default
- In _workflow_, enter the workflow definition id of the workflow to be used to attach the generated 
transcripts/captions
- Enter a _notification.email_ to get job failure notifications. If not entered, the email in 
etc/custom.properties (org.opencastproject.admin.email) will be used. Configure the SmtpService.
If no email address specified in either _notification.email_ or _org.opencastproject.admin.email_,
email notifications will be disabled. 

```
# Change enabled to true to enable this service. 
enabled=true

# Google Cloud Service details 
google.cloud.client.id=<CLIENT_ID>
google.cloud.client.secret=<CLIENT_SECRET>
google.cloud.refresh.token=1<REFRESH_TOKEN>
google.cloud.token.enpoint=<TOKEN_ENDPOINT>

# google cloud storage bucket
google.cloud.storage.bucket=<BUCKET_NAME>

# Language of the supplied audio. See the Google Speech-to-Text service documentation
# for available languages. If empty, the default will be used ("en-UK").
google.speech.language=en-UK

# Filter out profanities from result. Default is false
google.speech.profanity.filter=false

# Workflow to be executed when results are ready to be attached to media package.
workflow=Manchester-attach-google-speech-transcripts
  
# Interval the workflow dispatcher runs to start workflows to attach transcripts to the media package
# after the transcription job is completed.
# (in seconds) Default is 1 minute.
workflow.dispatch.interval=60
 
# How long it should wait to check jobs after their start date + track duration has passed.
# The default is 5 minutes.
# (in seconds)
completion.check.buffer=300

# How long to wait after a transcription is supposed to finish before marking the job as 
# cancelled in the database. Default is 5 hours.
# (in seconds)
max.processing.time=18000

# How long to keep result files in the working file repository in days.
# The default is 7 days.
cleanup.results.days=7

# Email to send notifications of errors. If not entered, the value from
# org.opencastproject.admin.email in custom.properties will be used.
notification.email=
```

### Step 4: Add encoding profile for extracting audio

The Google Speech-to-Text service has limitations on audio the audio type. [Supported audio type are here] (https://cloud.google.com/speech-to-text/docs/reference/rest/v1p1beta1/RecognitionConfig#AudioEncoding)
Try using the encoding profile suggested in etc/encoding/googlespeech-audio.properties.

### Step 5: Add workflow operations and create new workflow

Add the following operations to your workflow. We suggest adding them after the media package is
published so that users can watch videos without having to wait for the transcription to finish, but it
depends on your use case. The only requirement is to take a snapshot of the media package so that
the second workflow can retrieve it from the archive to attach the caption/transcripts.  

```xml
<!-- Extract audio from one of the presenter videos -->

<operation
  id="compose"
  fail-on-error="true"
  exception-handler-workflow="ng-partial-error"
  description="Extract audio for transcript generation">
  <configurations>
    <configuration key="source-tags">engage-download</configuration>
    <configuration key="target-flavor">audio/flac</configuration>
    <!-- The target tag 'transcript' will be used in the next 'start-google-speech-transcription' operation -->
    <configuration key="target-tags">transcript</configuration>
    <configuration key="encoding-profile">audio-flac</configuration>
    <!-- If there is more than one file that match the source-tags, use only the first one -->
    <configuration key="process-first-match-only">true</configuration>
  </configurations>
</operation>

<!-- Start Google speech recognitions job -->

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
### Step 6: Create a workflow that will add the generated caption/transcript to the media package and republish it
A sample one can be found in etc/workflows/Manchester-attach-google-speech-transcripts.xml

```xml
 <!-- Attach caption/transcript -->

    <operation id="attach-google-speech-transcription"
      fail-on-error="true"
      exception-handler-workflow="ng-partial-error" 
      description="Attach captions/transcription">
      <configurations>
        <!-- This is filled out by the transcription service when starting this workflow -->
        <configuration key="transcription-job-id">${transcriptionJobId}</configuration>
        <configuration key="target-flavor">captions/timedtext</configuration>
        <configuration key="target-tag">archive</configuration>
        <configuration key="target-caption-format">webvtt</configuration>
      </configurations>
    </operation>

    <!-- Re-publish to OAI-PMH -->

    <operation
      id="republish-oaipmh"
      exception-handler-workflow="ng-partial-error"
      description="REPUBLISH: Update recording in default OAI-PMH repository">
      <configurations>
        <configuration key="source-flavors">dublincore/*,security/*,captions/*</configuration>
        <configuration key="repository">default</configuration>
        <configuration key="merge">true</configuration>
      </configurations>
    </operation>
```

Workflow Operations
-------------------

- [start-google-speech-transcription](../workflowoperationhandlers/start-google-speech-transcription-woh.md)
- [attach-google-speech-transcription](../workflowoperationhandlers/attach-google-speech-transcription-woh.md)