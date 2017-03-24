# Important Concepts in S+

The podcasting system interacts with S+ by means of the Reporting Data Base (RDB). This is a Microsoft SQL database that is a set of views derived from the main S+ application. When S+ is updated by Scientia the goal is to make changes to the core application, but leave the RDB as consistent as possible.


### No Event Specific UUIDs

Imagine an event with a specific date-time-location:

| | Event | |
| - | - | - |
| Date | 	Time | 	Room
| 01-01-2014 | 13:00 | Main Theatre

An expectation would be that the individual event would have a unique identifier, however this is not the case. There is no unique per event ID, only unique identifiers for collective patterns of events. The collections of events are know as Activities.


### S+ Activities

Events are defined by Activities. An activity is a string of events that represent a pattern of events. For example, a student might have the following timetable:

| | | Timetable | |
| - | - | - | - | - |
| Mon | Tues | Wed | Thu | Fri|
| | 10:00 | | 15:00 	
| | Biol101 | | Biol101 	

In terms of activities this would be described as two activities as a single activity cannot occur at multiple different times:

V_ACTIVITY ID 	| Day 	| Time 	|Week Pattern 	|Location
--------------- | ----- | ----- | -------|-----
CCSF5GH4F56GHB4FSG54BH |Tues |10:00 | 111111011111 | Room 1
CCSF5GH4F56GHBA5R768F2 |Thu |15:00 | 111111011111 |Room 2

Each activity has a unique ID and in the above example it runs at the same time, in the same location every week. The activity has a week pattern that describes how it runs across the semester, skipping a week in the 7th week. This timetable must be recorded as two activities rather than one because it has two different start times. This is a result of activities following these rules:

* A single activity cannot have more than a single start time.
* A single activity can occur in multiple locations.
* A single activity can have multiple parent courses that own it (see combined Parent-Child)
* A single activity can have a second activity appended to it in order to give a lecture in a one-off different time-slot (see Variant).
* There are no safeguards to stop two activities being scheduled in the same space-time (a clash). 
