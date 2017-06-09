# Participation Management REST endpoint

<p id="series"></p>

`/pm/activity/{id}/series`


This Endpoint creates a series inside Opencast for an Activity in Syllabus plus. 
It uses the the S+ reporting database and combines the Series Title from the 
`V_MODULE` table combining `Name`, `Description` and the course year which is 
worked out from the S+-reference in `UserText4`. This S+-reference is a combination 
of multiple codes :

```html
I3031-LAWS-30672-1161-2SE-007863
━━┳━━ ━━━━┳━━━━━  ┳━  ━━━━┳━━━━━
  ┃       ┃       ┃       ┃
  ┃       ┃       ┃       ┗━━ Internal HostKey
  ┃       ┃       ┗━━ Year of the course (academic Year 2015/16) 
  ┃       ┗━━ Course code
  ┗━━ Faculty code
```

| parameter | description |
| --------- | ----------- |
| id | Syllabus Plus activity Id to create a series from.|

| response | description | explanation |
| -------- | ----------- | ----------- |
| SC_CREATED | Series created | The series is either already existent or was created successfully |
| SC_NOT_FOUND | Activity id not found | The specified ActivityId does not exist in the S+ Reporting database. The Series could therfore not be created |
| SC_INTERNAL_SERVER_ERROR | Series could not be found or created | Series could not be found or created |
| SC_SERVICE_UNAVAILABLE | An external service not available | Can't harvest from external feeder service: service not available! |


<p id="optout_id"> </p>

`/pm/optout/id`

Get OptOut URI for described person

| parameter | description |
| --------- | ----------- |
| email | Person's email address |

| response | description | explanation |
| -------- | ----------- | ----------- |
| SC_OK | Optout ID return | This returns the opt-out id. This id is the value a user has to pass tho the `teacher` parameter when logging in to the Teacher UI: `http://localhost:8080/pm-teacher?teacher=`_<opt-out id\>_ |
| SC_INTERNAL_SERVER_ERROR | Optout ID could not be created - email not found | Optout ID could not be created - email not found |


<p id="harvest"> </p>

`/pm/harvest`

This endpoint initiates the harvesting from the Syllabus Plus scheduling database into the participation management module tables.


| response | description | explanation |
| -------- | ----------- | ----------- |
| SC_OK | Success | Harvesting has been successfully initiated and is running in the background. |
| SC_SERVICE_UNAVAILABLE | External feeder service not available | Can't harvest from external feeder service: service not available! |


<p id="synchronizeScheduler"> </p>

`/pm/synchronizeScheduler`

This endpoint initiates the synchronisation the recordings from particiaptation management module with the Matterhorn Scheduler

| response | description | explanation |
| -------- | ----------- | ----------- |
| SC_OK | Success | Synchronisation has been successfully initiated and is running in the background. |


<p id="resetPM"> </p>

`/pm/resetPM`

This endpoint resets the participation management database.

| response | description | explanation |
| -------- | ----------- | ----------- |
| SC_OK | Success | Participation management database has been successfully reset. |


<p id="resetMatterhorn"> </p>

`/pm/resetMatterhorn`

This endpoint resets the participation management related events and series created in Opencast.

| response | description | explanation |
| -------- | ----------- | ----------- |
| SC_OK | Success | Participation management related events and series created in Opencast have been successfully reset. |


<p id="resetAll"> </p>

`/pm/resetAll`

This endpoint resets the participation management database and the related events and series created in Opencast.

| response | description | explanation |
| -------- | ----------- | ----------- |
| SC_OK | Success | Participation management database and the related events and series created in Opencast have been successfully reset. |


<p id="harvestSync"> </p>

`/pm/harvestSync`

This endpoint initiates the harvesting from the Syllabus Plus scheduling database into the 
participation management module tables and synchronises all events with the Matterhorn scheduler.

| response | description | explanation |
| -------- | ----------- | ----------- |
| SC_OK | Success | Harvesting has been successfully initiated and is running in the background. |
| SC_SERVICE_UNAVAILABLE | External feeder service not available | Can't harvest from external feeder service: service not available! |
| SC_PRECONDITION_FAILED | The participation management module does not allow a combined harvest | The combined havest/sync is not safe for the initial synchronisatin. This is due to the fact that on the initial sync there are vast amounts of changes in the database and it is recommended that the first sync is closely monitored so it should be done through the Admin UI. | 
| SC_PARTIAL_CONTENT | The participation management module did not complete in a way that a schedule update is possible | There is an issue occured that the synchronisation between the Participation management tables and the Opencast scheduler would be unsafe. An email error message has been sent out to the administrator that gives some more details. This can happen if too many harvested events have changed. |


<p id="recording_id_status"> </p>

`/recording/{id}/status`

Modify the recording status

| parameter | description |
| --------- | ----------- |
| id | The recording identifier |
| status | The recording status: OPT_OUT, NO_TRIM, TRIM |

| response | description | explanation |
| -------- | ----------- | ----------- |
| SC_OK | Success | The recordin status has been changed. |
| SC_NOT_FOUND | The recording has not been found. | A recording with the given _id_ does not exist. |
| SC_BAD_REQUEST | Unkown recording status | The recording status provided is not valid. | 

