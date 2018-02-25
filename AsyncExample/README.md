# Async Example for Android Room

This example follows up to the *Basic Example for Android Room* in this repository.

Because
- In Android, the GUI events are handles in the Main thread and
- Database access requires IO and thus may block the current thread for arbitary durations
by default, Room does not allow to invoke queries from the Android App's main thread - for a good reason:
In GUI application, never do something timeconsuming / blocking in the thread that draws the GUI and handles
the GUI events. Because if this thread is block, your GUI cannot respond to user action - which results in bad
user experience.

In the basic example, there was a switch set to allow invocation to the Room database on the app's main thread:
```kotlin
Room.databaseBuilder(...).allowMainThreadQueries().build()
```
If we omit this switch, Room raises an IllegalStateException:
```
AndroidRuntime: FATAL EXCEPTION: main
Caused by: java.lang.IllegalStateException:
Cannot access database on the main thread since it may potentially lock the UI for a long period of time.
    at android.arch.persistence.room.RoomDatabase.assertNotMainThread(RoomDatabase.java:164)
    at android.arch.persistence.room.RoomDatabase.beginTransaction(RoomDatabase.java:211)
    at de.pantoffel.asyncexample.UserDao_Impl.insertAll(UserDao_Impl.java:61)
```

In this example, we examine solutions to avoid this flag and perfrom Room queries on another thread.

To improve readability, say we've got our database queries extracted into one single function. 
This function is the code that must be executed async.
```kotlin
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
```

## Using Android's AsyncTask

Android provides it's own [AsyncTask](https://developer.android.com/reference/android/os/AsyncTask.html):

> AsyncTask enables proper and easy use of the UI thread. This class allows you to perform background operations and publish results on the UI thread without having to manipulate threads and/or handlers. [...] AsyncTasks should ideally be used for short operations (a few seconds at the most.) [...] An asynchronous task is defined by a computation that runs on a background thread and whose result is published on the UI thread.

That's what we need!

Now we can write our database query code into a custom class extending AsyncTask like this:
```kotlin
class DatabaseTask(
        private val context: WeakReference<Context>,
        private val db: WeakReference<AppDatabase>
) : AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg params: Void?) =
            db.get()?.let { databaseWriteAndRead(it) } ?: ""

    override fun onPostExecute(userList: String) =
            Toast.makeText(context.get(), userList, LENGTH_LONG).show()
}
```
(Please note I've used *WeakReference* here to avoid leaking the context... or to be honest: 
to get rid of the warning about potential leaking because this async task will finish quickly.)

## Using Kotlin coroutines

Coroutines is a feature of Kotlin that, as of begin of 2018, was recently added to Kotlin and
supportes lightweight asynchronous code by *suspendable functions* (marked by *suspend keyword*).

To use Kotlin coroutines in your Android project, add it to the module dependencies:
```gradle
dependencies {
    compile 'org.jetbrains.kotlinx:kotlinx-coroutines-android:0.22.3'
}
```
and activate it by a switch in *gradle.properties*
```properties
kotlin.coroutines=enable
```

Next, we need to mark our function that does the blocking database queries by *suspend* keyword:
```kotlin
suspend fun databaseWriteAndReadCoroutine(db: AppDatabase): String {
    // Same implementation
}
```


To use this function in an async way, call in with *async { ... }*.
Wrap it into a lauch (UI) { ... } block to invoke update of the UI of the UI thread (=== Main-thread) again.
```kotlin
launch(UI) {
     // Do stuff in a non-blocking coroutine
    val userList = async(CommonPool) { databaseWriteAndReadCoroutine(db) }.await()
    // Invoke UI on the Main thread again
    Toast.makeText(applicationContext, userList, LENGTH_LONG).show()
}
``` 
Look cleaner that Android's AsyncTask, doesn't it? We got rid of the AsyncTask implementation class, completely.

## Using ReactiveX

Potentially, you already have RX in your application. If not, it consider adding it :-)
RX add great features for asynchronous application design (so-called *reactive*).
Of course, we can use it for our database queries to be performed in background.

## Using CompletableFuture

Java8's Completable Future is available from Android API Level 24 (= Android 7.0) and thus not avaiable for a lot of projects.
There's a backport... TODO

## Using Anko