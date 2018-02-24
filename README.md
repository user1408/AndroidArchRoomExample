# Example for Android Room

This is a minimal example for Android's Room library. 
**Room** is a persistency helper library for SQLite database of your app.
It's part of Android Archticture Components:

Room landing page:
https://developer.android.com/topic/libraries/architecture/room.html

This example is based on an official Android training: 
https://developer.android.com/training/data-storage/room/index.html

Architecture Components:
https://developer.android.com/topic/libraries/architecture/index.html
There's lots of docs out there for Architecture components.

Room offers only basic functionality for a persistency library and won't take you as far as JPA2 (*Eclpiselink*, *Hibernate*) or *Spring Data* will do. On the other hand, it integrates superb with Android.

## How it works

If you have worked with database abstraction libraries in Java before, *Room* will most probably feel comfortable to you, as it works with the same approach like, for instance, JPA.

You define a model of your data and annotate some information for the object mapper on the type (as an *Entity*) and its members (column name, primary key ...):

```kotlin
@Entity
data class User(

  @PrimaryKey(autoGenerate = true)
  var uid: Int = 0,

  @ColumnInfo(name = "first_name")
  var firstName: String? = null,

  @ColumnInfo(name = "last_name")
  var lastName: String? = null

)
```
Next, you define an *interface* for your desired CURD access the database on that specific entity (so-called *Data Access Object - DAO*). Give the methods speaking names and annotate them with the action to take (like *Insert* or a *Query*). 
Note that Query must be defined by SQL statement. Room will not do that work for you (like Spring Data, for instance).

```kotlin
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
```

You don't need to create an implementation of that interface. *Room* will do that for you during the compilation, that's the (well-known) magic part.

Then you define a class for your database which contain all the type (resp. tables) you have. This class extends *RoomDatabase* and is abstract. Again, Room will create the implementation for you (based on the *@Database* annotation).

```kotlin
@Database(entities = [(User::class)], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao
} 
```

On rumtime, you can get an instance of the generated implementation class by Room's database builder:

	Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database-name")

Then you can use the methods you defined in the DAO:

... like insert ...

```kotlin
db.userDao().insertAll(
  User(firstName = "Benjamin", lastName = "Blümchen"),
  User(firstName = "Karla", lastName = "Kolumna")
)
```

... or get all elements ...

```kotlin
db.userDao().all
```

## Dependencies

Room has two dependencies: the runtime and the annotation processor.
The annotation processor visites the classes and members annotated with Room's annotations (like *@Database* or *@Entity*) and generates implementation code upon that.

```kotlin
apply plugin: 'kotlin-kapt'
...
dependencies {
  implementation "android.arch.persistence.room:runtime:1.0.0"
  kapt "android.arch.persistence.room:compiler:1.0.0"
}
```
Notice the *kapt* for the annotation processor, which is *Kotlin Annotation Processing Tool*. For Java classes, you need to write *annotationProcessor* instead and don't need the kapt plugin.

## Kotlin Version

You need a Kotlin version like 1.2.xx  With older vesion. With older version you might encounter the problem that the @Query annotation don't work with the identifier names from the signature of the annotated method.

```kotlin
@Query("SELECT * FROM user WHERE uid IN (:userIds)")
fun loadAllByIds(userIds: IntArray): List<User>
```

With older Kotlin versions, the indentifer *userIds* is replaces by *arg0* or *p0* or something else and this must be used in the SQL statement.

## Simplifications

In the demo code the database is aquired by 

```kotlin
val db = Room
          .databaseBuilder(applicationContext, AppDatabase::class.java, "database-name")
          .allowMainThreadQueries() // Don't do this in production.
          .fallbackToDestructiveMigration() // Don't do this in production.
          .build()

*allowMainThreadQueries()*- in production code, don't set it, of course. If not set, Room will raise an exception if operations are issued on that main tread. Instead, execute them by 

- Android's good, ol' AsyncTask from 
- CompletableFuture (with ForkJoinPool or your own DB executor)
- Using RxJava (with io scheduler)
- Kotlin coroutine
- ... 

*fallbackToDestructiveMigration()* - Room has migration strategies when you change your database scheme (i.e. when you change your POJOs). However, when you do this, you also need to change the version in the annotation on your database class:

```kotlin
@Database(entities = [(User::class)], version = 2, exportSchema = false)
```

If you have that app already on your device and thus there's already a SQLite DB and thus a scheme for you data types, Room will check that scheme againt the current one. If they differ but the version is identical, Room will raise an exception. 
To work around that *during development* use *fallbackToDestructiveMigration()*.
