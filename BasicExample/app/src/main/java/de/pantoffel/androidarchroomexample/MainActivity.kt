package de.pantoffel.androidarchroomexample

import android.app.Activity
import android.arch.persistence.room.*
import android.os.Bundle
import android.widget.Toast

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

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room
                .databaseBuilder(applicationContext, AppDatabase::class.java, "database-name")
                .allowMainThreadQueries() // Don't do this in production.
                .fallbackToDestructiveMigration() // Don't do this in production.
                .build()

        db.userDao().insertAll(
                User(firstName = "Benjamin", lastName = "Blümchen"),
                User(firstName = "Karla", lastName = "Kolumna")
        )

        val userList = db.userDao().all
                .mapIndexed { idx, user -> "${idx + 1}: ${user.firstName} ${user.lastName}" }
                .joinToString(separator = "\n")

        Toast.makeText(applicationContext, userList, Toast.LENGTH_LONG).show()
    }
}