package com.hojang.gooddriver.network

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiResponsesStorage(context: Context) {

    private val dbHelper: ApiResponsesDBHelper = ApiResponsesDBHelper(context)

    suspend fun insertResponse(response: String) {
        withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(ApiResponsesDBHelper.COLUMN_RESPONSE_TEXT, response)
                }
                db.insert(ApiResponsesDBHelper.TABLE_NAME, null, values)
                // 사용이 끝난 후에는 닫아주기
                db.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getAllResponses(): List<String> {
        val db = dbHelper.readableDatabase
        val responseList = mutableListOf<String>()

        val cursor = db.query(
            ApiResponsesDBHelper.TABLE_NAME,
            arrayOf(ApiResponsesDBHelper.COLUMN_RESPONSE_TEXT),
            null,
            null,
            null,
            null,
            null
        )

        while (cursor.moveToNext()) {
            val responseText =
                cursor.getString(cursor.getColumnIndex(ApiResponsesDBHelper.COLUMN_RESPONSE_TEXT))
            responseList.add(responseText)
        }

        cursor.close()
        db.close()

        return responseList
    }

    fun close() {
        dbHelper.close()
    }
}

class ApiResponsesDBHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "api_responses.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "api_responses"
        const val COLUMN_ID = "id"
        const val COLUMN_RESPONSE_TEXT = "response_text"
    }

    private val createTableQuery = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_RESPONSE_TEXT TEXT NOT NULL
        );
    """.trimIndent()

    init {
        if (context == null) {
            throw IllegalArgumentException("Context cannot be null")
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle database upgrade if needed
    }
}

