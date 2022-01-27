package com.example.trainingmanager

import Utils
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getStringOrNull
import java.io.File

class DatabaseHelper(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("create table if not exists $PROGRAM_TABLE ("+PROGRAM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                PROGRAM_NAME + " TEXT)")
    }
    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        db.execSQL("DROP TABLE IF EXISTS $PROGRAM_TABLE")
        onCreate(db)
    }
    fun getPrograms(): List<String> {
        val db = this.readableDatabase
        val c = db.rawQuery("select $PROGRAM_NAME from $PROGRAM_TABLE",null)
        val list = mutableListOf<String>()
        if (c != null) {
            while (c.moveToNext()) {
                list.add(c.getString(c.getColumnIndex(PROGRAM_NAME)))
            }
            c.close()
        }
        return list
    }
    fun makeProgramCopy(oldTable:String, newTable: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(PROGRAM_NAME,newTable)
        db.insert(PROGRAM_TABLE,null,values)

        val oldTableID = getProgramID(oldTable)
        val newTableID = getProgramID(newTable)
        db.execSQL("create table if not exists [program_$newTableID] as select * from [program_$oldTableID]")
        db.execSQL("create table if not exists [program_${newTableID}_comments] as select * from [program_${oldTableID}_comments]")
        val sessions = getSessions(oldTable)
        if (sessions != null){
            for (session in sessions) {
                insertSession(newTable,session)
                val oldSessionID = getSessionID(oldTable,session)
                val newSessionID = getSessionID(newTable,session)

                values.clear()
                values.put(SESSION_NAME,newSessionID)
                db.update("program_$newTableID",values,"$SESSION_NAME=?", arrayOf(oldSessionID.toString()))

                values.clear()
                values.put(COMMENT_SESSION,newSessionID)
                db.update("program_${newTableID}_comments",values,"${COMMENT_SESSION}=?", arrayOf(oldSessionID.toString()))
            }
        }
    }
    fun getSessions(program: String): List<String>? {
        val db = this.readableDatabase
        val sessionNames = mutableListOf<String>()
        val programID = getProgramID(program)
        val c = db.rawQuery("SELECT $SESSIONS_SESSION from $SESSIONS_TABLE where $PROGRAM_ID = \"$programID\"",null)
        if (c.count != 0) {
            while (c.moveToNext()) {
                sessionNames.add(c.getString(c.getColumnIndex(SESSIONS_SESSION)))
            }
            c.close()
            return sessionNames
        }
        c.close()
        return null
    }
    fun getSessionIDs(program: String): List<String>? {
        val db = this.readableDatabase
        val sessionNames = mutableListOf<String>()
        val programID = getProgramID(program)
        val c = db.rawQuery("SELECT $SESSIONS_ID from $SESSIONS_TABLE where $PROGRAM_ID = \"$programID\"",null)
        if (c.count != 0) {
            while (c.moveToNext()) {
                sessionNames.add(c.getString(c.getColumnIndex(SESSIONS_ID)))
            }
            c.close()
            return sessionNames
        }
        c.close()
        return null
    }
    fun getLastActivity(): Utils.SessionActivity? {
        val db = this.writableDatabase
        val c = db.rawQuery("select * from $ACTIVITY_TABLE where $ACTIVITY_ID =" +
                "(select max($ACTIVITY_ID) from $ACTIVITY_TABLE)",null)
        if (c != null) {
            c.moveToFirst()
            val res =  Utils.SessionActivity(
                c.getString(c.getColumnIndex(ACTIVITY_PROGRAM)),
                c.getString(c.getColumnIndex(ACTIVITY_SESSION)),
                c.getString(c.getColumnIndex(ACTIVITY_STARTTIME))
            )
            c.close()
            return res
        }
        return null
    }
    fun getLastSessionEntry(program: String,session: String): Utils.SessionActivity? {
        val db = this.writableDatabase

        val c = db.rawQuery("select * from $ACTIVITY_TABLE where $ACTIVITY_PROGRAM = \"${getProgramID(program)}\"" +
                " and $ACTIVITY_SESSION = \"${getSessionID(program,session)}\"" +
                "order by $ACTIVITY_ID desc limit 1",null)
        if (c.count != 0) {
            c.moveToFirst()
            val res = Utils.SessionActivity(
                c.getString(c.getColumnIndex(ACTIVITY_PROGRAM)),
                c.getString(c.getColumnIndex(ACTIVITY_SESSION)),
                c.getString(c.getColumnIndex(ACTIVITY_STARTTIME))
            )
            c.close()
            return res
        }
        c.close()
        return null
    }
    fun getLastExerciseData(program:String,session:String,exercise: String,dateTime: String?) : MutableList<Utils.SQLExerciseData>? {
        fun getList(query:String): MutableList<Utils.SQLExerciseData>? {
            val db = this.readableDatabase
            val list = mutableListOf<Utils.SQLExerciseData>()
            val c = db.rawQuery(query,null)

            if (c.count != 0) {
                while(c.moveToNext()) {
                    list.add(Utils.SQLExerciseData(
                        c.getString(c.getColumnIndex(EXERCISE_WEIGHT)),
                        c.getString(c.getColumnIndex(EXERCISE_REPS)),
                        c.getString(c.getColumnIndex(EXERCISE_RPE))
                    ))
                }
                c.close()
                return list
            }
            c.close()
            return null
        }
        var outList: MutableList<Utils.SQLExerciseData>? = null

        if (dateTime != null) {
            outList =
                getList("select * from [exercise_${getExerciseID(exercise)}_data] where $EXERCISE_DATETIME = \"$dateTime\"")
        }
        if (outList != null) {
            return outList
        } else {
            val ex = getOneSessionExercise(program, session, exercise)
            if (ex != null) {
                outList = getList("select * from [exercise_${getExerciseID(exercise)}_data] where " +
                        " $EXERCISE_SET_RANGE = \"${ex.sets}\" " +
                        " and $EXERCISE_REP_RANGE = \"${ex.reps}\"")
                if (outList != null) {
                    return outList
                } else {
                    outList = getList("select * from [exercise_${getExerciseID(exercise)}_data] where " +
                            " $EXERCISE_REP_RANGE = \"${ex.reps}\"")
                    if (outList != null) {
                        if (outList.count() > ex.sets.toInt()) {
                            outList = outList.subList(0,ex.sets.toInt())
                        }
                        return outList
                    }
                }

            }
        }
        return null
    }
    fun getSessionExercises(program: String,session: String): MutableList<Utils.ExerciseProgram> {
        val exercises = mutableListOf<Utils.ExerciseProgram>()
        var programID = getProgramID(program)
        var sessionID = getSessionID(program,session)
        val db = this.readableDatabase

        val c = db.rawQuery("select * from [program_$programID] where $SESSION_NAME = \"$sessionID\"",null)
        if (c.count != 0) {
            while (c.moveToNext()) {
                val exerciseID = c.getString(c.getColumnIndex(EXERCISE_NAME))
                val ex = db.rawQuery("select $EXERCISE_INFO_EXERCISE from $EXERCISE_INFO_TABLE where $EXERCISE_INFO_ID = \"$exerciseID\"",null)
                if (ex.count != 0) {
                    ex.moveToFirst()
                    exercises.add(Utils.ExerciseProgram(
                        ex.getString(ex.getColumnIndex(EXERCISE_INFO_EXERCISE)),
                        c.getString(c.getColumnIndex(EXERCISE_SET_RANGE)),
                        c.getString(c.getColumnIndex(EXERCISE_REP_RANGE))
                    ))
                }
                ex.close()
            }
            c.close()
        }
        return exercises
    }
    fun getOneSessionExercise(program: String, session: String,exercise: String): Utils.ExerciseProgram? {
        var exerciseOut: Utils.ExerciseProgram? = null
        var programID = getProgramID(program)
        var sessionID = getSessionID(program,session)
        val db = this.readableDatabase
        val exerciseID = getExerciseID(exercise)
        val c = db.rawQuery("select * from [program_$programID] where $SESSION_NAME = \"$sessionID\" and $EXERCISE_NAME=\"${exerciseID}\"",null)
        if (c.count != 0) {
            while (c.moveToNext()) {
                val ex = db.rawQuery("select $EXERCISE_INFO_EXERCISE from $EXERCISE_INFO_TABLE where $EXERCISE_INFO_ID = \"$exerciseID\"",null)
                if (ex.count != 0) {
                    ex.moveToFirst()
                    exerciseOut = Utils.ExerciseProgram(
                        ex.getString(ex.getColumnIndex(EXERCISE_INFO_EXERCISE)),
                        c.getString(c.getColumnIndex(EXERCISE_SET_RANGE)),
                        c.getString(c.getColumnIndex(EXERCISE_REP_RANGE))
                    )
                }
                ex.close()
            }
            c.close()
        }
        return exerciseOut
    }
    fun getSessionComment(program: String,session: String,dateTime: String):String? {
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT $COMMENT FROM [program_${getProgramID(program)}_comments] WHERE $COMMENT_DATETIME = \"$dateTime\"",null)
        if (c.count != 0) {
            while (c.moveToNext()) {
                val comment = c.getString(c.getColumnIndex(COMMENT))
                c.close()
                return comment
            }
        }
        c.close()
        return null
    }
    fun getExerciseComment(exercise:String,dateTime: String): String? {
        val db = this.readableDatabase
        val c = db.rawQuery("select $COMMENT from [exercise_${getExerciseID(exercise)}_comments] where $COMMENT_DATETIME = \"$dateTime\"",null)
        if (c.count != 0) {
            while (c.moveToNext()) {
                val comment = c.getString(c.getColumnIndex(COMMENT))
                c.close()
                return comment
            }
        }
        c.close()
        return null
    }
    fun getExerciseInstructions(exercise: String): Utils.SQLExerciseInstructions? {
        val db = this.readableDatabase
        val c = db.rawQuery("select * from $EXERCISE_INFO_TABLE where $EXERCISE_NAME = \"$exercise\"", null)
        if (c.count != 0) {
            while(c.moveToNext()) {
                val res = Utils.SQLExerciseInstructions(
                    c.getStringOrNull(c.getColumnIndex(EXERCISE_INFO_SETUP)),
                    c.getStringOrNull(c.getColumnIndex(EXERCISE_INFO_INSTRUCTIONS))
                )
                c.close()
                return res
            }
        }
        return null
    }
    fun getAllData(): Cursor {
        val db = this.writableDatabase
        val res = db.rawQuery("select * from "+ PROGRAM_TABLE,null)
        return res
    }
    fun insertData(
        name: String?
    ): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(PROGRAM_NAME, name)
        val result =
            db.insert(PROGRAM_TABLE, null, contentValues)
        return if (result == -1L) false else true
    }
    fun updateData(
        id: String,
        name: String?
    ): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(PROGRAM_ID, id)
        contentValues.put(PROGRAM_NAME, name)
        db.update(
            PROGRAM_TABLE,
            contentValues,
            "ID = ?",
            arrayOf(id)
        )
        return true
    }
    fun updateProgram(programName: String,exercises:List<Utils.SQLOutputExercise>) {
        data class SessionIDS(var ID: String,var exists:Boolean,var text: String?)
        val db = this.writableDatabase
        val sessionIDs = mutableListOf<SessionIDS>()
        for (ID in getSessionIDs(programName)!!) {
            sessionIDs.add(SessionIDS(
                ID,
                false,
                null
            ))
        }
        // dropping and recreating the program table
        db.execSQL("drop table [program_${getProgramID(programName)}]")
        db.execSQL("create table if not exists [program_${getProgramID(programName)}] ("+ SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                SESSION_NAME + " TEXT, " +
                EXERCISE_NAME  + " TEXT, " +
                EXERCISE_SET_RANGE + " TEXT, " +
                EXERCISE_REP_RANGE + " TEXT)")
        val values = ContentValues()
        //inserting exercises in the program table
        for (exercise in exercises) {
            insertExercise(exercise.exercise.title)
            if (exercise.sessionID != null) {
                values.put(SESSION_NAME,exercise.sessionID)
                for (i in 0 until sessionIDs.count()) {
                    if (sessionIDs[i].ID == exercise.sessionID) {
                        sessionIDs[i].exists = true
                        sessionIDs[i].text = exercise.exercise.session
                        break
                    }
                }
            } else {
                insertSession(programName,exercise.exercise.session)
                values.put(SESSION_NAME,getSessionID(programName,exercise.exercise.session))
            }
            values.put(EXERCISE_NAME,getExerciseID(exercise.exercise.title))
            values.put(EXERCISE_SET_RANGE,exercise.exercise.sets)
            values.put(EXERCISE_REP_RANGE,exercise.exercise.reps)
            db.insert("program_${getProgramID(programName)}", null, values)
            values.clear()
        }
        //deleting sessions that no longer exist and creating new ones
        for (session in sessionIDs) {
            if (!session.exists) {
                deleteSession(session.ID)
            } else {
                if (session.text != null) {
                    values.clear()
                    values.put(SESSIONS_SESSION,session.text)
                    db.update(SESSIONS_TABLE, values, "$SESSIONS_ID=?", arrayOf(session.ID))
                }
            }
        }
    }
    fun renameProgram(oldName: String,newName:String) {
        val db = this.writableDatabase
        db.execSQL("alter table [$oldName] rename to [$newName]")
        db.execSQL("alter table [${oldName}_comments] rename to [${newName}_comments]")

        val values = ContentValues()
        values.put(PROGRAM_NAME,newName)
        db.update(PROGRAM_TABLE,values,"$PROGRAM_NAME = ?", arrayOf(oldName))
    }
    fun createProgram(program: Utils.Program): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(PROGRAM_NAME, program.title)
        val result =
            db.insert(PROGRAM_TABLE, null, contentValues)
        return result != -1L
    }
    fun createSessions(program: Utils.Program) {
        val ID = "ID"
        val SESSION = "session"
        val EXERCISE_TITLE = "exercise"
        val SETS = "sets"
        val REPS = "reps"
        val PROGRAM_TABLE = "[${Utils().replacePunctuationCheck(program.title)}]"

        val db = this.writableDatabase
        db.execSQL("create table if not exists $PROGRAM_TABLE ("+ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                SESSION + " TEXT, " +
                EXERCISE_TITLE  + " TEXT, " +
                SETS + " TEXT, " +
                REPS + " TEXT)")

        for (session in program.program){
            val contentValues = ContentValues()
            contentValues.put(SESSION,Utils().replacePunctuationCheck(session.title))
            for (exercise in session.exercises) {
                contentValues.put(EXERCISE_TITLE,exercise.title)
                contentValues.put(SETS,exercise.sets)
                contentValues.put(REPS,exercise.reps)
                db.insert(PROGRAM_TABLE,null,contentValues)
            }
        }
    }
    fun createExercise(exercise: Utils.ExerciseStorage) {
        val EXERCISE_TABLE = "[${exercise.exerciseTitle}_data]"
        val EXERCISE_ID = "exercise_id"
        val EXERCISE_SET_RANGE = "exercise_set_range"
        val EXERCISE_REP_RANGE = "exercise_rep_range"
        val EXERCISE_WEIGHT = "exercise_weight"
        val EXERCISE_REPS = "exercise_reps"
        val EXERCISE_RPE = "exercise_RPE"
        val EXERCISE_DATETIME = "exercise_datetime"

        val db = this.writableDatabase
        db.execSQL("create table if not exists $EXERCISE_TABLE ("+EXERCISE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                EXERCISE_SET_RANGE + " TEXT," +
                EXERCISE_REP_RANGE + " TEXT," +
                EXERCISE_WEIGHT + " TEXT," +
                EXERCISE_REPS + " TEXT," +
                EXERCISE_RPE + " TEXT," +
                EXERCISE_DATETIME + " TEXT)")
        for (i in exercise.variants) {
            val contentValues = ContentValues()
            contentValues.put(EXERCISE_SET_RANGE,i.sets)
            contentValues.put(EXERCISE_REP_RANGE,i.reps)
            for (ii in i.instances) {
                for (iii in ii.sets) {
                    contentValues.put(EXERCISE_WEIGHT,iii.weight)
                    contentValues.put(EXERCISE_REPS,iii.repsCompleted)
                    contentValues.put(EXERCISE_RPE,iii.difficult)
                    db.insert(EXERCISE_TABLE,null,contentValues)
                }
            }
        }

    }
    fun createExerciseInfo(exercise: Utils.ExerciseStorage) {
        val ID = "ID"
        val EXERCISE = "exercise"
        val SETUP = "setup"
        val INSTRUCTIONS = "instructions"
        val TABLE = "exercises_info"

        val db = this.writableDatabase
        db.execSQL("create table if not exists $TABLE ("+ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                EXERCISE + " TEXT," +
                SETUP + " TEXT," +
                INSTRUCTIONS + " TEXT)")
        val contentValues = ContentValues()
        contentValues.put(EXERCISE,exercise.exerciseTitle)
        contentValues.put(SETUP,exercise.setupInstructions)
        db.insert(TABLE,null,contentValues)
    }
    fun createComments(context: Context,program: Utils.Program) {
        val ID = "ID"
        val TYPE = "type"
        val LOCATION = "location"
        val COMMENT = "comment"
        val DATETIME = "datetime"
        val TABLE = "[${Utils().replacePunctuationCheck(program.title)}_comments]"

        val db = this.writableDatabase
        db.execSQL("create table if not exists $TABLE ("+ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TYPE + " TEXT," +
                COMMENT + " TEXT," +
                LOCATION + " TEXT," +
                DATETIME + " TEXT)")
        for (session in program.program) {
            val contentValues = ContentValues()
            contentValues.put(TYPE,"session")
            contentValues.put(LOCATION,session.title)
        }
        for (session in program.program) {

            for (ex in session.exercises) {
                val exFile = File(context.filesDir,"/exercises/${ex.title}/exercise.json")
                val exercise = Utils().parseExerciseStorage(exFile.readText())
                val contentValues = ContentValues()
                contentValues.put(LOCATION,exercise.exerciseTitle)
                for (variant in exercise.variants) {
                    for (instance in variant.instances) {
                        contentValues.put(TYPE,"exercise")
                        if (instance.comment.isNotEmpty()) {
                            contentValues.put(COMMENT, instance.comment)
                            db.insert(TABLE,null,contentValues)
                        }
                    }
                }
            }
        }

    }
    fun createExerciseActivityTable() {
        val db = this.writableDatabase
        db.execSQL("create table if not exists $ACTIVITY_TABLE ("+ ACTIVITY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ACTIVITY_PROGRAM + " TEXT, " +
                ACTIVITY_SESSION  + " TEXT, " +
                ACTIVITY_STARTTIME + " TEXT, " +
                ACTIVITY_ENDTIME + " TEXT)")
    }
    fun createCurrentProgramTable() {
        val db = this.writableDatabase
        db.execSQL("drop table if exists $CURRENT_PROGRAM_TABLE")
        db.execSQL("create table if not exists $CURRENT_PROGRAM_TABLE ("+ CURRENT_PROGRAM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CURRENT_PROGRAM_NAME + " TEXT)")
        db.execSQL("INSERT INTO $CURRENT_PROGRAM_TABLE DEFAULT VALUES")
    }
    fun createSessionsTable() {
        val db = this.writableDatabase
        val c = db.rawQuery("select * from program_table",null)
        val programs = getPrograms()
        val v  = ContentValues()
        db.execSQL("drop table if exists active_sessions_table")
        db.execSQL("create table if not exists $SESSIONS_TABLE ("+ SESSIONS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                PROGRAM_ID + " INTEGER, " +
                SESSION_NAME + " TEXT)")
        for (i in 0 until programs.count()) {
            c.moveToPosition(i)
            for (session in getSessions(programs[i])!!) {
                v.put(PROGRAM_ID, c.getInt(c.getColumnIndex(PROGRAM_ID)))
                v.put(SESSION_NAME,session)
                db.insert(SESSIONS_TABLE,null,v)
                v.clear()
            }
        }
        c.close()
    }
    fun changeProgramNames() {
        val db = this.writableDatabase
        db.delete(PROGRAM_TABLE,"$PROGRAM_NAME = ?", arrayOf("4 Day Upper/Lower"))
        val programs = getPrograms()
        for (program in programs) {
            val c = db.rawQuery("SELECT $PROGRAM_ID from $PROGRAM_TABLE where $PROGRAM_NAME = \"$program\"",null)
            if (c.count != 0) {
                c.moveToFirst()
                val programID = c.getInt(c.getColumnIndex(PROGRAM_ID))
                //db.execSQL("ALTER TABLE [${program}] RENAME TO [program_$programID]")
                db.execSQL("ALTER TABLE [${program}_comments] RENAME TO [program_${programID}_comments]")
            }
        }
    }
    fun changeProgramTables() {
        val db = this.writableDatabase
        val programs = getPrograms()
        val v = ContentValues()
        for (program in programs) {
            val sessions = getSessions(program)
            for (session in sessions!!) {
                val exercises = getSessionExercises(program,session)
                for (exercise in exercises) {
                    val c = db.rawQuery("select $EXERCISE_INFO_ID from $EXERCISE_INFO_TABLE where $EXERCISE_INFO_EXERCISE = \"${exercise.title}\"",null)
                    if (c.count != 0) {
                        c.moveToFirst()
                        val exerciseID = c.getInt(c.getColumnIndex(EXERCISE_INFO_ID))
                        v.put(EXERCISE_NAME,exerciseID)
                        db.update("[${program}_comments]",v,"$COMMENT_SESSION=?", arrayOf(exercise.title))
                    }
                    c.close()
                }
            }
        }

    }
    fun updateCurrentProgram(program: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(CURRENT_PROGRAM_NAME,getProgramID(program))
        db.update(CURRENT_PROGRAM_TABLE,values,"$CURRENT_PROGRAM_ID = 1",null)
    }
    fun updateExerciseSetup(setup: String,exercise: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(EXERCISE_INFO_SETUP,setup)
        db.update(EXERCISE_INFO_TABLE,values,"$EXERCISE_INFO_EXERCISE = ?", arrayOf(exercise))
    }
    fun getCurrentProgram() : String? {
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT $CURRENT_PROGRAM_NAME from $CURRENT_PROGRAM_TABLE",null)
        if (c.count != 0) {
            c.moveToFirst()
            val res = c.getString(c.getColumnIndex(CURRENT_PROGRAM_NAME))
            c.close()
            return res
        }
        c.close()
        return null
    }
    fun insertSession(program: String,session: String) {
        if (!sessionExists(program,session)) {
            val db = writableDatabase
            val v = ContentValues()
            v.put(PROGRAM_ID, getProgramID(program))
            v.put(SESSIONS_SESSION, session)
            db.insert(SESSIONS_TABLE, null, v)
        }
    }
    fun insertExercise(exerciseName: String) {
        if (!exerciseExists(exerciseName)) {
            val db = this.writableDatabase
            val values = ContentValues()
            values.put(EXERCISE_INFO_EXERCISE, exerciseName)
            db.insert(EXERCISE_INFO_TABLE, null, values)
            val exerciseID = getExerciseID(exerciseName)
            db.execSQL(
                "create table if not exists [exercise_${exerciseID}_data] (" + EXERCISE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        EXERCISE_SET_RANGE + " TEXT," +
                        EXERCISE_REP_RANGE + " TEXT," +
                        EXERCISE_WEIGHT + " TEXT," +
                        EXERCISE_REPS + " TEXT," +
                        EXERCISE_RPE + " TEXT," +
                        EXERCISE_DATETIME + " TEXT)"
            )
            db.execSQL(
                "create table if not exists [exercise_${exerciseID}_comments] (" + EXERCISE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COMMENT + " TEXT," +
                        COMMENT_DATETIME + " TEXT)"
            )
        }
    }
    fun renameProgramColumns() {
        val db = this.writableDatabase
        db.execSQL("create table if not exists [temp] ("+ SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                SESSION_NAME + " TEXT, " +
                EXERCISE_NAME  + " TEXT, " +
                EXERCISE_SET_RANGE + " TEXT, " +
                EXERCISE_REP_RANGE + " TEXT)")
        val programs = getPrograms()
        db.execSQL("drop table [temp]")
        for(program in programs) {
            db.execSQL("create table if not exists [temp] ("+ SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    SESSION_NAME + " TEXT, " +
                    EXERCISE_NAME  + " TEXT, " +
                    EXERCISE_SET_RANGE + " TEXT, " +
                    EXERCISE_REP_RANGE + " TEXT)")
            db.execSQL("INSERT INTO temp SELECT * FROM [$program]")
            db.execSQL("drop table [$program]")
            db.execSQL("create table if not exists [${program}] ("+ SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    SESSION_NAME + " TEXT, " +
                    EXERCISE_NAME  + " TEXT, " +
                    EXERCISE_SET_RANGE + " TEXT, " +
                    EXERCISE_REP_RANGE + " TEXT)")
            db.execSQL("INSERT INTO [${program}] SELECT * FROM [temp]")
            db.execSQL("drop table [temp]")
        }
    }
    fun createExerciseList() {
        //This function takes all existing exercises and makes sure they have the appropriate database elements created
        val db = this.writableDatabase
        val values = ContentValues()
        val programs = getPrograms()
        for (program in programs) {
            val sessions = getSessions(program)
            for (session in sessions!!) {
                val exercises = getSessionExercises(program,session)
                for (exercise in exercises) {
                    if (!exerciseInfoExists(exercise.title)) {
                        values.put(EXERCISE_INFO_EXERCISE,exercise.title)
                        db.insert(EXERCISE_INFO_TABLE,null,values)
                        values.clear()

                        db.execSQL("create table if not exists [${exercise.title}_data] ("+EXERCISE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                EXERCISE_SET_RANGE + " TEXT," +
                                EXERCISE_REP_RANGE + " TEXT," +
                                EXERCISE_WEIGHT + " TEXT," +
                                EXERCISE_REPS + " TEXT," +
                                EXERCISE_RPE + " TEXT," +
                                EXERCISE_DATETIME + " TEXT)")
                        db.execSQL("create table if not exists [${exercise.title}_comments] ("+EXERCISE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                COMMENT + " TEXT," +
                                COMMENT_DATETIME + " TEXT)")
                    }
                }
            }
        }
    }
    fun exerciseInfoExists(exercise: String): Boolean {
        val db = this.readableDatabase
        val out = db.rawQuery("SELECT EXISTS (SELECT 1 FROM $EXERCISE_INFO_TABLE WHERE $EXERCISE_INFO_EXERCISE = \"$exercise\") AS \"return_value\"",null)
        out.moveToFirst()
        if (out.getInt(out.getColumnIndex("return_value")) == 1) {
            out.close()
            return true
        } else {
            out.close()
            return false
        }
    }
    fun insertProgram(programName: String,exercises:List<Utils.SQLProgramExercise>) {
        if (!programExists(programName)) {
            val db = this.writableDatabase
            val values = ContentValues()
            //placing program in program table
            values.put(PROGRAM_NAME, programName)
            db.insert(PROGRAM_TABLE, null, values)
            values.clear()

            val programID = getProgramID(programName)
            //creating program table and program comments table
            db.execSQL("create table if not exists [program_${programID}] ("+ SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    SESSION_NAME + " TEXT, " +
                    EXERCISE_NAME  + " TEXT, " +
                    EXERCISE_SET_RANGE + " TEXT, " +
                    EXERCISE_REP_RANGE + " TEXT)")
            db.execSQL("create table if not exists [program_${programID}_comments] ("+ COMMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COMMENT  + " TEXT, " +
                    COMMENT_SESSION + " TEXT, " +
                    COMMENT_DATETIME + " TEXT)")
            for (exercise in exercises) {
                insertExercise(exercise.title)
                insertSession(programName,exercise.session)
                values.put(SESSION_NAME,getSessionID(programName,exercise.session))
                values.put(EXERCISE_NAME,getExerciseID(exercise.title))
                values.put(EXERCISE_SET_RANGE,exercise.sets)
                values.put(EXERCISE_REP_RANGE,exercise.reps)
                db.insert("[program_${programID}]", null, values)
                values.clear()
            }
        }
    }
    fun insertActivity(program:String,session:String,startTime:String,endTime: String) {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(ACTIVITY_PROGRAM, getProgramID(program))
        values.put(ACTIVITY_SESSION, getSessionID(program,session))
        values.put(ACTIVITY_STARTTIME, startTime)
        values.put(ACTIVITY_ENDTIME, endTime)
        db.insert(ACTIVITY_TABLE, null, values)
    }
    fun insertExerciseSet(exercise:String,setRange:String,repRange: String,weight: String,reps: String,RPE: String,dateTime: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(EXERCISE_SET_RANGE,setRange)
        values.put(EXERCISE_REP_RANGE,repRange)
        values.put(EXERCISE_WEIGHT,weight)
        values.put(EXERCISE_REPS,reps)
        values.put(EXERCISE_RPE,RPE)
        values.put(EXERCISE_DATETIME,dateTime)
        db.insert("[exercise_${getExerciseID(exercise)}_data]",null,values)
    }
    fun insertComment(exercise: String,comment: String,dateTime: String) {
        if (comment.isNotEmpty()) {
            val db = this.writableDatabase
            val values = ContentValues()
            values.put(COMMENT, comment)
            values.put(COMMENT_DATETIME, dateTime)
            db.insert("[exercise_${getExerciseID(exercise)}_comments]", null, values)
        }
    }
    fun insertSessionComment(program: String,session:String,comment: String,dateTime: String) {
        val db = this.writableDatabase
        val programID = getProgramID(program)
        val sessionID = getSessionID(program,session)
        val values = ContentValues()
        values.put(COMMENT,comment)
        values.put(COMMENT_SESSION,sessionID)
        values.put(COMMENT_DATETIME,dateTime)
        db.insert("[program_${programID}_comments]", null, values)
    }
    fun deleteData(id: String): Int {
        val db = this.writableDatabase
        return db.delete(PROGRAM_TABLE, "ID = ?", arrayOf(id))
    }
    fun deleteProgram(program: String) {
        val programID = getProgramID(program)
        val db = this.writableDatabase
        //deleting entry in program_table
        db.delete(PROGRAM_TABLE, "$PROGRAM_NAME = ?",arrayOf(program))
        //deleting program tables
        db.execSQL("DROP TABLE IF EXISTS [program_${programID}]")
        db.execSQL("DROP TABLE IF EXISTS [program_${programID}_comments]")
        //deleting entries in sessions_table
        db.delete(SESSIONS_TABLE, "$PROGRAM_ID=?", arrayOf(programID.toString()))
        //deleting entries in activity_table
        db.delete(ACTIVITY_TABLE, "$ACTIVITY_PROGRAM=?", arrayOf(programID.toString()))
        //deleting current program if relevant
        val currentID = getCurrentProgram()
        if (currentID != null) {
            if (programID == currentID) {
                val v = ContentValues()
                v.putNull(CURRENT_PROGRAM_NAME)
                db.update(CURRENT_PROGRAM_TABLE,v,"$CURRENT_PROGRAM_ID = 1",null)
            }

        }
    }
    fun deleteSession(sessionID : String) {
        val programID = getProgramIDFromSessionID(sessionID)
        if (programID != null) {
            val db = this.writableDatabase
            db.delete(SESSIONS_TABLE, "$SESSIONS_ID=?", arrayOf(sessionID))
            db.delete(ACTIVITY_TABLE, "$ACTIVITY_SESSION=?", arrayOf(sessionID))
            db.delete("program_$programID","$SESSION_NAME=?", arrayOf(sessionID))
            db.delete("program_${programID}_comments","$COMMENT_SESSION=?", arrayOf(sessionID))
        }
    }
    fun programExists(program: String): Boolean {
        val db = this.readableDatabase
        val out = db.rawQuery("SELECT EXISTS (SELECT 1 FROM $PROGRAM_TABLE WHERE $PROGRAM_NAME = \"$program\") AS \"return_value\"",null)
        out.moveToFirst()
        if (out.getInt(out.getColumnIndex("return_value")) == 1) {
            out.close()
            return true
        } else {
            out.close()
            return false
        }
    }
    fun sessionExists(program:String,session: String): Boolean {
        val programID = getProgramID(program)
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT EXISTS (SELECT 1 FROM $SESSIONS_TABLE WHERE $SESSIONS_SESSION = \"$session\" AND $PROGRAM_ID = $programID) AS \"return_value\"",null)
        c.moveToFirst()
        if (c.getInt(c.getColumnIndex("return_value")) == 1) {
            c.close()
            return true
        }
        c.close()
        return false
    }
    fun exerciseExists(exercise: String): Boolean {
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT EXISTS (SELECT 1 FROM $EXERCISE_INFO_TABLE WHERE $EXERCISE_INFO_EXERCISE = \"$exercise\") AS \"return_value\"",null)
        c.moveToFirst()
        if (c.getInt(c.getColumnIndex("return_value")) == 1) {
            c.close()
            return true
        }
        c.close()
        return false
    }
    private fun getProgramID(program: String): String? {
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT $PROGRAM_ID from $PROGRAM_TABLE where $PROGRAM_NAME = \"$program\"",null)
        if (c.count != 0) {
            c.moveToFirst()
            val out = c.getInt(c.getColumnIndex(PROGRAM_ID))
            c.close()
            return out.toString()
        }
        c.close()
        return null
    }
    private fun getProgramIDFromSessionID(sessionID: String):String? {
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT $PROGRAM_ID from $SESSIONS_TABLE where $SESSIONS_ID = \"$sessionID\"",null)
        if (c.count != 0) {
            c.moveToFirst()
            val res = c.getInt(c.getColumnIndex(PROGRAM_ID))
            c.close()
            return res.toString()
        }
        c.close()
        return null
    }
    fun getProgramName(ID: String): String? {
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT $PROGRAM_NAME from $PROGRAM_TABLE where $PROGRAM_ID = \"$ID\"",null)
        if (c.count != 0) {
            c.moveToFirst()
            val out = c.getString(c.getColumnIndex(PROGRAM_NAME))
            c.close()
            return out
        }
        c.close()
        return null
    }
    fun getSessionID(program: String,session: String): Int? {
        val programID =getProgramID(program)
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT $SESSIONS_ID from $SESSIONS_TABLE where $SESSIONS_SESSION = \"$session\" and $PROGRAM_ID = \"$programID\"",null)
        if (c.count != 0) {
            c.moveToFirst()
            val out = c.getInt(c.getColumnIndex(SESSIONS_ID))
            c.close()
            return out
        }
        c.close()
        return null
    }
    fun getSessionName(ID: String): String? {
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT $SESSIONS_SESSION from $SESSIONS_TABLE where $SESSIONS_ID = \"$ID\"",null)
        if (c.count != 0) {
            c.moveToFirst()
            val out = c.getString(c.getColumnIndex(SESSIONS_SESSION))
            c.close()
            return out
        }
        c.close()
        return null
    }
    private fun getExerciseID(exercise: String): Int? {
        val db = this.readableDatabase
        val c = db.rawQuery("select $EXERCISE_INFO_ID from $EXERCISE_INFO_TABLE where $EXERCISE_INFO_EXERCISE = \"$exercise\"",null)
        if (c.count != 0) {
            c.moveToFirst()
            val out = c.getInt(c.getColumnIndex(EXERCISE_INFO_ID))
            c.close()
            return out
        }
        c.close()
        return null
    }
    private fun getExerciseName(ID: String): String? {
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT $EXERCISE_INFO_EXERCISE from $EXERCISE_INFO_TABLE where $EXERCISE_INFO_ID = \"$ID\"",null)
        if (c.count != 0) {
            c.moveToFirst()
            val out = c.getString(c.getColumnIndex(EXERCISE_INFO_EXERCISE))
            c.close()
            return out
        }
        c.close()
        return null
    }
    fun purge() {
        val db = this.writableDatabase
        db.execSQL("drop table [4 Day Upper/Lower_comments]")
        db.execSQL("drop table [GGG get going g]")
        db.execSQL("drop table [GGG get going g_comments]")
        db.execSQL("drop table [Incline Dumbbell Overhead Extensions_comments]")
        db.execSQL("drop table [Incline Dumbbell Overhead Extensions_data]")
        db.execSQL("drop table [program_2]")
    }
    fun convertProgramToNumbers() {
        fun tableExists(db: SQLiteDatabase,tableName:String): Boolean {
            val cc = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'",null)
            val out = cc.count !=0
            cc.close()
            return out
        }
        data class PROG(var name: String,var ID: Int)
        val programs = mutableListOf<PROG>()
        val db = this.writableDatabase
        val c = db.rawQuery("select * from $PROGRAM_TABLE",null)
        if (c.count!=0) {
            while(c.moveToNext()) {
                programs.add(PROG(
                    c.getString(c.getColumnIndex(PROGRAM_NAME)),
                    c.getInt(c.getColumnIndex(PROGRAM_ID))
                ))
            }
        }
        c.close()
        for (program in programs) {
            if (!tableExists(db,"${program.name}_comments")) {
                db.execSQL("create table if not exists [${program.name}_comments] ("+ COMMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COMMENT  + " TEXT, " +
                        COMMENT_SESSION + " TEXT, " +
                        COMMENT_DATETIME + " TEXT)")

            }
            if (!tableExists(db,"${program.name}")) {
                db.execSQL("create table if not exists [${program.name}] ("+ SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        SESSION_NAME + " TEXT, " +
                        EXERCISE_NAME  + " TEXT, " +
                        EXERCISE_SET_RANGE + " TEXT, " +
                        EXERCISE_REP_RANGE + " TEXT)")
            }
            if (tableExists(db,"program_${program.ID}")) {
                db.execSQL("drop table [${program.name}]")
            } else {
                db.execSQL("ALTER TABLE [${program.name}] RENAME TO [program_${program.ID}]")
            }
            if (tableExists(db,"program_${program.ID}_comments")) {
                db.execSQL("drop table [${program.name}_comments]")
            } else {
                db.execSQL("ALTER TABLE [${program.name}_comments] RENAME TO [program_${program.ID}_comments]")
            }
        }
    }
    fun convertExercisesToNumbers() {
        fun tableExists(db: SQLiteDatabase,tableName:String): Boolean {
            val cc = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'",null)
            val out = cc.count !=0
            cc.close()
            return out
        }
        data class EX(var name: String,var ID: Int)
        val exercises = mutableListOf<EX>()
        val db = this.writableDatabase
        val c = db.rawQuery("select * from $EXERCISE_INFO_TABLE",null)
        if (c.count!=0) {
            while(c.moveToNext()) {
                exercises.add(EX(
                    c.getString(c.getColumnIndex(EXERCISE_INFO_EXERCISE)),
                    c.getInt(c.getColumnIndex(EXERCISE_INFO_ID))
                ))
            }
        }
        c.close()
        for (exercise in exercises) {
            if (!tableExists(db,"${exercise.name}_comments")) {
                db.execSQL(
                        "create table if not exists [${exercise.name}_comments] (" + EXERCISE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                COMMENT + " TEXT," +
                                COMMENT_DATETIME + " TEXT)"
                    )
            }
            if (tableExists(db,"exercise_${exercise.ID}_comments")) {
                db.execSQL("drop table [${exercise.name}_comments]")
            } else {
                db.execSQL("ALTER TABLE [${exercise.name}_comments] RENAME TO [exercise_${exercise.ID}_comments]")
            }
        }
    }
    fun createTempSessionTable() {
        val db = this.writableDatabase
        dropTable(TEMP_SESSION_TABLE)
        dropTable("${TEMP_SESSION_TABLE}_comments")
        db.execSQL("create table if not exists [$TEMP_SESSION_TABLE] ("+ SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                EXERCISE_NAME  + " TEXT, " +
                EXERCISE_WEIGHT + " TEXT, " +
                EXERCISE_REPS + " TEXT, " +
                EXERCISE_RPE + " TEXT, " +
                TEMP_SESSION_PAGE + " TEXT)")
        db.execSQL("create table if not exists [${TEMP_SESSION_TABLE}_comments] ("+ COMMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                EXERCISE_NAME  + " TEXT, " +
                COMMENT + " TEXT, " +
                TEMP_SESSION_PAGE + " TEXT)")
    }
    fun clearTempSessionTable() {
        clearTable(TEMP_SESSION_TABLE)
    }

    fun insertTempExerciseSet(exercise:String,weight: String,reps: String,RPE: String,viewPage:Int) {
        val db = this.writableDatabase
        val values = ContentValues()
        if (weight.isEmpty()) {
            values.putNull(EXERCISE_WEIGHT)
        } else {
            values.put(EXERCISE_WEIGHT,weight)
        }
        if (reps.isEmpty()) {
            values.putNull(EXERCISE_REPS)
        } else {
            values.put(EXERCISE_REPS,reps)
        }
        if (RPE.isEmpty()) {
            values.putNull(EXERCISE_RPE)
        } else {
            values.put(EXERCISE_RPE,RPE)
        }
        values.put(EXERCISE_NAME,getExerciseID(exercise))
        values.put(TEMP_SESSION_PAGE,viewPage)
        db.insert(TEMP_SESSION_TABLE,null,values)
    }
    fun insertTempExerciseComment(exercise:String, comment:String,viewPage:Int) {
        if (comment.isNotEmpty()) {
            val db = this.writableDatabase
            val values = ContentValues()
            values.put(COMMENT, comment)
            values.put(EXERCISE_NAME, getExerciseID(exercise))
            db.insert(TEMP_SESSION_TABLE, null, values)
        }
    }
    fun dropTable(table:String) {
        val db = this.writableDatabase
        val cc = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$table'",null)
        if (cc.count !=0) {
            db.execSQL("drop table [$table]")
        }
        cc.close()
    }
    fun getAllExerciseNames() : ArrayList<String>? {
        val db = readableDatabase
        val c = db.rawQuery("select $EXERCISE_INFO_EXERCISE from $EXERCISE_INFO_TABLE",null)
        if (c.count != 0) {
            val list = arrayListOf<String>()
            while (c.moveToNext()) {
                list.add(c.getString(c.getColumnIndex(EXERCISE_INFO_EXERCISE)))
            }
            c.close()
            return list
        }
        c.close()
        return null
    }
    fun getTempExerciseSet(page: Int) : MutableList<Utils.SQLExerciseData>? {
        fun getList(query:String): MutableList<Utils.SQLExerciseData>? {
            val db = this.readableDatabase
            val list = mutableListOf<Utils.SQLExerciseData>()
            val c = db.rawQuery(query,null)

            if (c.count != 0) {
                while(c.moveToNext()) {
                    list.add(Utils.SQLExerciseData(
                        c.getStringOrNull(c.getColumnIndex(EXERCISE_WEIGHT)),
                        c.getStringOrNull(c.getColumnIndex(EXERCISE_REPS)),
                        c.getStringOrNull(c.getColumnIndex(EXERCISE_RPE))
                    ))
                }
                c.close()
                return list
            }
            c.close()
            return null
        }
        val out = getList("SELECT * FROM [$TEMP_SESSION_TABLE] WHERE $TEMP_SESSION_PAGE=$page")
        if (out != null) {
            return out
        }
        return null
    }
    fun clearTable(table:String) {
        if (tableExists(table)) {
            val db = this.writableDatabase
            db.execSQL("DELETE FROM [$table]")
        }
    }
    fun tableExists(table: String): Boolean {
        val db = this.writableDatabase
        val cc = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$table'",null)
        if (cc.count !=0) {
            cc.close()
            return true
        }
        cc.close()
        return false
    }
    fun tableIsEmpty(table: String): Boolean {
        if (tableExists(table)) {
            val db = this.readableDatabase
            val c = db.rawQuery("SELECT * from [$table]",null)
            if (c.count == 0) {
                c.close()
                return true
            }
            c.close()
        }
        return false
    }
    companion object {
        const val DATABASE_NAME = "app_database.db"
        //table of programs
        const val PROGRAM_TABLE = "program_table"
        const val PROGRAM_ID = "program_ID"
        const val PROGRAM_NAME = "program_name"

        //individual program table
        const val SESSION_NAME = "session"
        const val EXERCISE_NAME = "exercise"
        const val SETS_RANGE = "sets"
        const val REPS_RANGE = "reps"
        const val SESSION_ID = "ID"
        //exercise data table
        const val EXERCISE_ID = "exercise_id"
        const val EXERCISE_SET_RANGE = "exercise_set_range"
        const val EXERCISE_REP_RANGE = "exercise_rep_range"
        const val EXERCISE_WEIGHT = "exercise_weight"
        const val EXERCISE_REPS = "exercise_reps"
        const val EXERCISE_RPE = "exercise_RPE"
        const val EXERCISE_DATETIME = "exercise_datetime"
        const val EXERCISE_PROGRAM = "exercise_program"
        const val EXERCISE_SESSION = "exercise_session"
        //activity table
        const val ACTIVITY_TABLE = "activity_table"
        const val ACTIVITY_ID = "activity_ID"
        const val ACTIVITY_PROGRAM = "activity_program"
        const val ACTIVITY_SESSION = "activity_session"
        const val ACTIVITY_STARTTIME = "activity_starttime"
        const val ACTIVITY_ENDTIME = "activity_endttime"
        //program comments
        const val COMMENT_ID = "ID"
        const val COMMENT_TYPE = "type"
        const val COMMENT_SESSION = "location"
        const val COMMENT = "comment"
        const val COMMENT_DATETIME = "datetime"
        //exercise instruction table
        const val EXERCISE_INFO_TABLE = "exercises_info"
        const val EXERCISE_INFO_ID = "ID"
        const val EXERCISE_INFO_EXERCISE = "exercise"
        const val EXERCISE_INFO_SETUP = "setup"
        const val EXERCISE_INFO_INSTRUCTIONS = "instructions"
        //Current program table
        const val CURRENT_PROGRAM_TABLE = "current_program_table"
        const val CURRENT_PROGRAM_ID = "ID"
        const val CURRENT_PROGRAM_NAME = "program"
        //active programs table
        const val SESSIONS_TABLE = "sessions_table"
        const val SESSIONS_ID = "ID"
        const val SESSIONS_SESSION = "session"
        //temporary session
        const val TEMP_SESSION_TABLE = "temp_session"
        const val TEMP_SESSION_PAGE = "view_page"
    }
}