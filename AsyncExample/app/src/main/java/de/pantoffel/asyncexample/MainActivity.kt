package de.pantoffel.asyncexample

import android.app.Activity
import android.arch.persistence.room.*
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference


@Entity
data class User(
        @PrimaryKey(autoGenerate = true)
        var uid: Int = 0,

        @ColumnInfo(name = "first_name")
        var firstName: String? = null,

        @ColumnInfo(name = "last_name")
        var lastName: String? = null
)

@Dao
interface UserDao {
    @get:Query("SELECT * FROM user")
    val all: List<User>

    @Query("SELECT * FROM user WHERE uid IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): List<User>

    @Query("SELECT * FROM user WHERE first_name LIKE :first AND last_name LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): User

    @Insert
    fun insertAll(vararg users: User)

    @Delete
    fun delete(user: User)
}

@Database(entities = [(User::class)], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

fun databaseWriteAndRead(db: AppDatabase): String {
    // Write new entries
    db.userDao().insertAll(
            User(firstName = "Benjamin", lastName = "Blümchen"),
            User(firstName = "Karla", lastName = "Kolumna")
    )

    // Fetch all entries and build a string from them
    return db.userDao().all
            .mapIndexed { idx, user -> "${idx + 1}: ${user.firstName} ${user.lastName}" }
            .joinToString(separator = "\n")
}

suspend fun databaseWriteAndReadCoroutine(db: AppDatabase): String {
    // Write new entries
    db.userDao().insertAll(
            User(firstName = "Benjamin", lastName = "Blümchen"),
            User(firstName = "Karla", lastName = "Kolumna")
    )

    // Fetch all entries and build a string from them
    return db.userDao().all
            .mapIndexed { idx, user -> "${idx + 1}: ${user.firstName} ${user.lastName}" }
            .joinToString(separator = "\n")
}

class DatabaseTask(
        private val context: WeakReference<Context>,
        private val db: WeakReference<AppDatabase>
) : AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg params: Void?) =
            db.get()?.let { databaseWriteAndRead(it) } ?: ""

    override fun onPostExecute(userList: String) =
            Toast.makeText(context.get(), userList, LENGTH_LONG).show()
}

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room
                .databaseBuilder(applicationContext, AppDatabase::class.java, "database-name")
                .fallbackToDestructiveMigration() // Don't do this in production.
                .build()

        // Using Android's AsyncTask
        DatabaseTask(
                WeakReference(applicationContext),
                WeakReference(db))

        // Using Kotlin's coroutine
        launch(UI) {
            val userList = async(CommonPool) { databaseWriteAndReadCoroutine(db) }.await()
            Toast.makeText(applicationContext, userList, LENGTH_LONG).show()
        }

    }
}