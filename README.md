# Weightlifting Application

This is a fairly simple Android app for tracking weightlifting progress at the gym containing some small functionality. The original idea was to build up a basic app, and then gradually add more features typically considered "premium" features in other apps. However, making an app is a huge undertaking for just one person.

## Current features

### Exercise Program Creation

Currently the app is pretty much just a number tracker. It allows for the creation of exercise programs built up of individual sessions. In each session, a list of exercises is made, with the expected reps and sets. There is no limit on the number of programs, sessions, or exercises allowed.

### Management of Programs

On the programs screen, all created programs can be seen. Here they can be opened up to use, or they can be edited/renamed/deleted.

### Workout Screen

When a session in a program is opened up, all of the exercises are loaded in on a swipe-able window. If the session has been completed before, the weights, RPE, and reps will be loaded in from last time.  As each set is completed, the values can be filled in and on hitting enter the new inputs are closed in. When all exercises are completed, it allows the session to be submitted and a comment to be made.

## Future work

* Warmup calculator
	* Include different warmup needs, powerlifting, bodybuilding etc
	* Visualization of plates to put on the bar - in most efficient order to save time in adding weights each set
* Year-round periodisation program building for the advanced lifters
* Graphing of weight progress for exercises
* Journal for each training program to track workouts and comments made on them / on individual exercises
* Wider scope journal to track all activity in all programs
* Temporary storage of current training session in case of app crash

## Bugs

I use the app frequently but haven't worked on it for some time, so some parts have started to become depreciated, these are a few bugs I've noticed appearing lately:
* Resting timers sometimes doesn't start when set is submitted.
* Training session resets without warning after 30 minutes or so.

The phone used to test the app is quite old, so they may be problems unique to that.