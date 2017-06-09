# API Documentation

### Opencast Participation API 

`org.opencastproject.pm.api`

Participation management API provides the general interface to the scheduler 
outside Opencast to be able to create all the information necessary to describe a Lecture

  * `Room` 

    Physical Room in the institution. This is an class to be able to blacklist a room in case of it being out of order.

    | parameter | description |
    | -- | -- |
    | id | Id of the room Object |
    | name | Name of the room |

  * `Building` 

      Physical Building in the institution. This is an class to be able to blacklist an entire building in case of it being out of order (i.e. construction).

    | parameter | description |
    | -- | -- |
    | id | Id of the Building Object |
    | name | Name of the Building |
    | rooms | List of Rooms in the Building |

  * `CaptureAgent` 

      This is an class to be able to blacklist an entire building in case of it being out of order (i.e. construction).

    | parameter | description |
    | -- | -- |
    | id | Id of the CaptureAgent Object |
    | mhAgent | Name of the Building |
    | room | List of Rooms in the Building |

  * `PersonType` 

      This is an class to describe the typer of a person (i.e. Lecturer, Student etc.) .

    | parameter | description |
    | -- | -- |
    | id | Id of the PersonType Object |
    | name | Name of the Person Type|
    | function | Function of the Person |

  * `Person` 

      This is an class to describe a person .

    | parameter | description |
    | -- | -- |
    | id | Id of the Person Object |
    | name | Name of the Person |
    | email | email-address of the person |
    | personTypes | List of Types that apply to the person |

  * `Period` 

      This is an class to describe a time period.

    | parameter | description |
    | -- | -- |
    | id | Id of the Period Object |
    | start | Start date and time of the period|
    | end | end date and time of the period |
    | purpose | Optional description of the period |
    | comment | Optional comment of the period |

  * `Course` 

      This is an class to describe a Course which will be translated into an Opencast Series.

    | parameter | description |
    | -- | -- |
    | id | Id of the Course Object |
    | courseId | Course Id as it is known in the Syllabus Plus reporting Database |
    | seriesId | The Series Id in Opencast |
    | optedOut | boolean flag to mark if a course is to be recorded or not - if this flag is set to true the entire course with all its recordings is **not** recorded |
    | name | name of the Course |
    | description | Course Description |
    | schedulingSource | Optional Source of the scheduling in case there are multiple sources to schedule from. |
    | externalCourseKey| course Key as defined in Syllabus Plus |
    | fingerprint | encoded representation of the course in order to identify a course in a new run of the PM. (The S+ database has no unique Identifier that can be relied on to match two course-entries. The fingerprint creates this identifier.) |

### Opencast Participation Persistence API 

`org.opencastproject.pm.api.persistence`

The Opencast Participation Persistence API provides the general interface to the PM database 
Tables and provides Views presenting the Lecture recordings as well as the Email communication workflows

* `EmailView` 

    * Business object thats provides a view to gives general information if an email has been sent out correctly. This view contains: 

        * the course that was included in the email,
        * the start date of the recordings
        * the room mame where the recordings should take place
        * the list of presenters on the course
        * the email status (`UNSENT` / `SENT` / `FAILED`) - information if the opt-out email has been sent out successfully 
        * the recordig status (`READY` / `OPTED_OUT` / `BLACKLISTED`) - information if the recoding can take place 
        * The number of affected recordings on this course

        <br />

    * The `EmailView` is used in the `pm-admin` user interface to highlight emails that have failed sending out.

    <br />

* `ParticipationManagementDatabase` 

    * Interface to the Participation Management Database 

     <br />

* `ParticipationManagementDatabaseException` 

    * Presents exception that occurs while storing/retrieving participation management data from persistence storage.

     <br />

 * `RecordingQuery` 

    * Represents a query to find recordings.

     <br />

 * `RecordingView` 

    * Business object thats provides a view to give detailed information for a recording This view contains: 

        * the id of the lecture to take place
        * title of the lecture 
        * course title
        * presenter of the lecture
        * start date and time
        * end date and time
        * room where the lecture takes place
        * capture agent that should record
        * recording status (`READY` / `OPTED_OUT` / `BLACKLISTED`)
        * email status (`UNSENT` / `SENT` / `FAILED`)
        * actions - workflow parameter to be used
        * should the recording be edited before its published
        <br />

    * The `RecordingView` is used in the `pm-teacher` user interface to present 
the give the lecturer the opportunity to opt-out his lectures out from being recorded.

### Opencast Participation Schedule API
 
`org.opencastproject.pm.api.scheduling`

The Opencast Participation Schedule API provides the general interface to schedule a Lecture capture. 
This scheduling consists of two parts:

 * Harvesting the information from the Scheduling Database (Syllabus Plus Reporting Database) into the Participation Management tables
 * Scheduling of Recordings inside Opencast 

API Components:

 * `ParticipationFeederService`
    * Interface that specifies the Participation Feeder Service. The Participation Feeder 
      Service harvests the Course and Lecture information from Syllabus Plus and stores the 
      all lectures that can be recorded inside the Participation Management Tables in the 
      Opencast database.

 * `ParticipationManagementSchedulingException`
    * Presents exception that occurs while passing participation management recording 
      to Matterhorn scheduler.

 * `Schedule`
    * Describes a complete schedule containing a List of Recordings combined with 
      their Dublin-Core-Catalogue that represents the Opencast Media-Package.

 * `ScheduleProvider`
    * Provides scheduling data for Matterhorn. Implementations typically connect 
      to a 3rd-party data source, fetching from there and creating MH recording events.

 * `ScheduleFeederService`
    * Interface that specifies the Schedule Feeder Service. The Schedule Feeder Service 
      is used to schedule the events that were harvested by the Participation Feeder Service 
      as recordings inside the Opencast System. This service needs to create the Series 
      for each course as well as the Recording for each Lecture inside Opencast.

 * `SnapCountService`
    * Interface the specifies how to combine the Paricipation Feeder and the Schedule Feeder 
      in a way that the Schedule Feeder can safely run without the risk of loosing Recordings.

### Opencast Participation Utility API

`org.opencastproject.pm.api.util`

The Opencast Participation Utility API provides a simple utility to generate the Opt-Out key to allow external tools to get to the `pm-teacher` user interface.
