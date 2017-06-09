# Participation Management Module Configuration

### Participation Feeder Service Configuration

`.../etc/services/org.opencastproject.pm.syllabus.scheduling.ParticipationFeederServiceImpl.properties`

```sh
# Schedule for the feeder to run
# See http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger
# run at 1am
cron=0 0 1 * * ?
organization=mh_default_org
run-on-start=false
schedule=false

# Participation management Room Parameters

capture.rooms = FACULTY_ROOM, CENTRAL_ROOM, CAMERA_ROOM
 
capture.room.FACULTY_ROOM.name = Faculty owned room
capture.room.FACULTY_ROOM.id = A3F2BF771062DBCA7C601F0D029443EB
capture.room.FACULTY_ROOM.options = edit
capture.room.FACULTY_ROOM.workflow = matterhorn-pm

capture.room.CENTRAL_ROOM.name = Centrally managed room
capture.room.CENTRAL_ROOM.id = D782B07C9E6EC4B997D37C335ADEEDCD
capture.room.CENTRAL_ROOM.options = edit
capture.room.CENTRAL_ROOM.workflow = matterhorn-pm

capture.room.CAMERA_ROOM.name = Room equipped with PTZ-camera to track the Lecturer
capture.room.CAMERA_ROOM.id = 
capture.room.CAMERA_ROOM.options = edit, no-camera | camera-sidebyside | camera-only

capture.room.CAMERA_ROOM.option.no-camera = Record Slides and Audio only
capture.room.CAMERA_ROOM.option.camera-sidebyside = Record Slides and Video of Presenter
capture.room.CAMERA_ROOM.option.camera-only = Record Video of Presenter (no Slides)

capture.room.CAMERA_ROOM.workflow = matterhorn-pm-sidebyside
capture.room.CAMERA_ROOM.workflow.no-camera = matterhorn-pm
capture.room.CAMERA_ROOM.workflow.camera-only = matterhorn-pm-fmv
capture.room.CAMERA_ROOM.workflow.camera-sidebyside = matterhorn-pm-sidebyside

# Participation management Type Parameters
# if no capture.types are specified everything will be recorded

capture.types = LECTURE, SEMINAR

capture.type.LECTURE.name = University Lecture
capture.type.LECTURE.id = C2DA2B3E5DBD6FC21E60D349A2A04BC0

capture.type.SEMINAR.name = University Seminar
capture.type.SEMINAR.id = 
```

The Participation Feeder Service Configuration consists of 3 sections:

* The scheduling part defines when the Participation feeder is run. This section is a cron style schedule with a few more parameters.
* The Room Type definitions provide information that is used to determine 
  which Rooms are equipped with Lecture capture equipment. Inside the Syllabus Plus database there 
  is a the table `V_LOCATION_SUITABILITY` which maps the location (ie. physical room) to a `SuitabilityId`. 
  This `SuitabilityId` can be used to group rooms which have recording equipment.<br />
  The section starts with a comma separated list of roomnames   
  `capture.rooms = ` _FACULTY_ROOM_ `, ` _CENTRAL_ROOM_ `, ` _CAMERA_ROOM_`
    * Each room has then a section that describes it which contains 
        * `capture.room.`_CAMERA_ROOM_`.name = ` *Room equipped with PTZ-camera to track the Lecturer*
            * this specifies a human readable name for the room type
        * `capture.room.`_CAMERA_ROOM_`.id = ` *9E1A9A1D143C8EEBFE75626D7CA3B224*
            * UUID that identifies the Room type in the scheduling database
        * `capture.room.`_CAMERA_ROOM_`.workflow = `_matterhorn_pm_
            * workflow to be run on recordings made in this room type
        * `capture.room.`_CAMERA_ROOM_`.options = `_edit_`, `_no-camera_` | `_camera-sidebyside_` | `_camera-only_
            * options for the room type, this specifies options mainly for the Teacher 
              UI in order to select how the recording is to be made. 
            * the options can be specified as comma separated list for selection 
              (checkboxes) or as alternatives in a list separated by ` | ` (radio buttons)
            * You can also specify definitions of each individual option in an additional section 
                * `capture.room.`_CAMERA_ROOM_`.option.`_camera-sidebyside_` = `*Record Slides and Video of Presenter*
                    * human readable title of the option
                * `capture.room.`_CAMERA_ROOM_`.workflow.`_camera-sidebyside_` = `*matterhorn-pm-sidebyside*
                    * workflow that is used by recordings made with this option selected
* The Capture Type definitions provide information that is used to determine which 
  kind of event shall be recorded. This definition is optional. If no Capture type is 
  specified any scheduled event will be recorded. The capture type is related to the 
  `ActivityTypeId` in the `V_ACTIVITY` table in Syllabus Plus<br/>
  The section starts with a comma separated list of capture types

    `capture.types = `_LECTURE_`, `_SEMINAR_

    * Each room has then a section that describes it which contains 
        * `capture.type.`_LECTURE_`.name = `*University Lecture*
            * human readable title of the capture type
        * `capture.type.`_LECTURE_`.id = `*C2DA2B3E5DBD6FC21E60D349A2A04BC0*
            * UUID that identifies the Capture type in the scheduling database

### Schedule Feeder Service  Configuration

`.../etc/services/org.opencastproject.pm.impl.scheduling.ScheduleFeederServiceImpl.properties`

```sh
# Schedule for the feeder to run
# See http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger
# run at 1am
cron = 0 0 1 * * ?
organization=mh_default_org
run-on-start=false
create-new-series=true
schedule=false

# Margin before to (or remove if negative) the scheduled start time in minutes.
start-margin=0

# Margin to add to (or remove if negative) the scheduled end time in minutes.
end-margin=-5

# The number of days to show in the "Upcoming" tab of the Matterhorn adminsitrative ui. 
# If the value is commented out, all scheduled recordings will be shown.
# upcoming.days=8

# Whether the number of upcoming days should be rounded to include all days up to the end
# of the following weekends. In combination with the right value in upcoming.days, this
# setting allows for Matterhorn to always show the following weeks (upcoming.days=1) or
# whatever number of weeks are required (upcoming.days=8,15, ...).
upcoming.round=true

# Schedule for when the list of upcoming recordings should be updated. Ideally, this is
# happening once a day, right after midnight. See the Quartz documentation for help on crontab
# expressions: http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger
# The default is to run the job daily at 1 minute past Midnight
#upcoming.update=0 1 0 * * ?

# Specify the workflow definition (by its identifier) to run on scheduled mediapackages
workflow.definition=matterhorn-pm

# Specify the workflow configuration
#workflow.config.{key}={value}
workflow.config.trimHold=false
workflow.config.archiveOp=true
workflow.config.videoPreview=true
workflow.config.emailAddresses=admin@localhost
workflow.config.publishEngage=true
workflow.config.publishHarvesting=true
```

### Email Sender Service Configuration

`.../etc/services/org.opencastproject.pm.impl.OsgiEmailSenderService.properties`

```sh
# Some definitions (EBNF)
# Boolean = "true" | "false" ;
# OptionalNumber = "none" | PositiveNumber ;

# The opt out url will use the admin server as default 
opt-out-url = https://university.org

# The opt out page on the opt-out-url takes the teacher's email
# as a parameter (%s). The query parameter name is "teacher".
opt-out-page = /pm-teacher?teacher=%s

# The email-sender for the emails to be sent to Presenters 
# to allow them to opt out.
email-sender = Lecture Capture Service

# The email-address is the address where the opt-out emails will be sent from
email-address = lecture-capture@university.org

# The email-subject is the basis for the emails to be sent to Presenters 
# to allow them to opt out.
email-subject = Lecture capture opt-out reminder - please ensure that your lecture recording options are correctly set

# The email-template is the basis for the emails to be sent to Presenters 
# to allow them to opt out.
email-template = etc/pm-templates/mail-template-invitation.ftl

# The verification email address is an address that all sent emails are 
# copied to in order to check that the messages are sent out correctly
# Leave this blank or commented out to only send to recipients.

verification-email = testuser@university.org

# "smtp"
#   | "test-smtp:", test-recipient, ":", OptionalNumber, ":", Boolean
#   | "test-file:", test-file, ":", OptionalNumber, ":", Boolean ;
#
# smtp
#   Production mode. Send emails to real recipients.
#
# test-smtp
#   Test mode. All emails are sent to the given test-recipient.
#   EXAMPLE
#   test-smtp:root@localhost:none:true
#   > Send test emails for all recordings to root@localhost and update database
#   test-smtp:root@localhost:10:false.
#   > Send test emails for only 10 recordings to root@localhost. Do not update the database
#
# test-file
#   Test mode. All emails are rendered as files into the given directory.
#   EXAMPLE
#   test-file:/tmp/pmm-test-mails:10:false
#   Save emails for 10 recordings to directory /tmp/pmm-test-emails. Do not update the database.
mode = test-file:/tmp/pmm-test-mails:10:true

# The format to use to replace the current date in the email template
# Default is yyyy-MM-dd which is 4 digit year, 2 digit month number and 2 digit day of the month.
date.format.pattern=yyyy-MM-dd

# An example course name to use to demo the email template.
# Default is CMPT 220
demo.course.name=CMPT 220

# An example title to use in email templates.
# Default is Dr.
demo.instructor.title=Dr.

# An example name to use for the sender of the email templates.
# Default is Stanley Smith
demo.sender.name=Stanley Smith
```
