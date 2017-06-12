# Participation Management Modules

### [Opencast Participation API](modules/api.md) 

[`matterhorn-participation-api`](modules/api.md#opencast-participation-api)

* [`org.opencastproject.pm.api`](modules/api.md#opencast-participation-api)
    * Participation management API provides the general interface to the scheduler 
      outside Opencast to be able to create all the information necessary to describe a Lecture

    <br/>

* [`org.opencastproject.pm.api.persistence`](modules/api.md#opencast-participation-persistence-api)
    * The Opencast Participation Persistence API provides the general interface to the PM database 
      Tables and provides Views presenting the Lecture recordings as well as the Email communication workflows

    <br/>

* [`org.opencastproject.pm.api.scheduling`](modules/api.md#opencast-participation-schedule-api)
    * The Opencast Participation Schedule API provides the general interface to schedule a Lecture capture. 
      This scheduling consists of two parts:
        * Harvesting the information from the Scheduling Database (Syllabus Plus Reporting Database) into the Participation Management tables
        * Scheduling of Recordings inside Opencast 

        <br/>
           The API also provides an combined interface that allows the Implementation to provide an automatic triggering of the scheduling inside 
           Opencast if a number of conditions are satisfied.

    <br/>

* [`org.opencastproject.pm.api.util`](modules/api.md#opencast-participation-utility-api)
    * The Opencast Participation Utility API provides a simple utility to generate the Opt-Out key to allow external tools to get to the `pm-teacher` user interface.


`matterhorn-participation-impl`

* `org.opencastproject.pm.impl`

* `org.opencastproject.pm.impl.endpoint`
    * implementation of the participation management REST endpoint. This endpoint 
 provides all the functions o the PM through REST

| endpoint | explanation |
| -- | -- |
| [`/activity/{id}/series`](modules/rest.md#series) | Get the Activity with a given `id` from Syllabus Plus and create it as new Series in Opencast if required  |
| [`/optout/id`](modules/rest.md#optout_id) | Get Opt-Out URI for described person. The Opt-Out  |
| [`/harvest`](modules/rest.md#harvest) | Harvest the S+ activities from the Syllabus Plus reporting database into the participation management tables in Opencast this step does not affect the Opencast Scheduler |
| [`/synchronizeScheduler`](modules/rest.md#synchronizeScheduler) | Synchronize the recordings from the particiaptation management tables with the Opencast Scheduler information |
| [`/resetPM`](modules/rest.md#resetPM) | Reset the participation management database tables inside Opencast |
| [`/resetMatterhorn`](modules/rest.md#resetMatterhorn) | Reset all the events that are related to S+ activities in participation management database events and series created in Matterhorn |
| [`/resetAll`](modules/rest.md#resetAll) | Reset the participation management database und the related events and series created in Matterhorn |
| [`/harvestSync`](modules/rest.md#harvestSync) | Harvest external data in the participation management and synchronize all events with the Matterhorn scheduler |
| [`/recording/{id}/status`](modules/rest.md#recording_id_status) | Modify the recording status |

`matterhorn-participation-persistence`


`matterhorn-participation-management-conductor`


`matterhorn-participation-management`

`matterhorn-participation-management-ui`

`matterhorn-participation-management-ui-vaadin`

`matterhorn-participation-management-ui-common`

`matterhorn-participation-management-ui-teacher`

`matterhorn-participation-management-ui-teacher-vaadin`

`matterhorn-syllabus`
