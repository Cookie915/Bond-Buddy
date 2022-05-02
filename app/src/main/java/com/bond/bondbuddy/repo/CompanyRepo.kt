package com.bond.bondbuddy.repo

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.bond.bondbuddy.models.Response
import com.bond.bondbuddy.models.User
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class CompanyRepository @Inject constructor(
    private val userRepository: UserRepository,
        ) {
    private val database: FirebaseFirestore = FirebaseFirestore.getInstance()
    val dbUsersRef = database.collection("users")
    val dbCompanyRef = database.collection("companies")
    val tag = "CompanyRepo"

    //  Create
    fun createCompany(name: String, context: Context, cb: () -> Unit) {
        val fUID = FirebaseAuth.getInstance().uid
        val token = FirebaseMessaging.getInstance().token
        val companyRef = dbCompanyRef.document(name)
        companyRef.get().addOnSuccessListener { snapshot ->
            //  company already exists, show toast and make user try new company
            if (snapshot.exists()) {
                Toast.makeText(context, "Company Already Exists, Try Again", Toast.LENGTH_LONG).show()
            } else {
                //  company doesn't exist
                //  create company in db and set token to let users
                //  send notification to owner, set company owner, set user's company to company name
                token.addOnSuccessListener {
                    val company = Company(fUID, it, name)
                    companyRef.set(company, SetOptions.merge())
                }
                dbUsersRef.document(FirebaseAuth.getInstance().currentUser!!.uid).set(mapOf("companyname" to name),
                                                                                      SetOptions.merge())
                FirebaseMessaging.getInstance().subscribeToTopic(name)
                userRepository.setRegistered()
                cb()
            }
        }.addOnFailureListener{
            Log.e(tag, "Error Creating Company: $name, error ${it.localizedMessage}", it)
            Toast.makeText(context,"Error Creating Company, Please Check Network Connection", Toast.LENGTH_LONG).show()
        }
    }

    //  Set
        //  set users companyname to name of company, makes toast if company isn't found
    suspend fun setUserCompany( context: Context, companyName: String, cb: (() -> Unit)?) = withContext(Dispatchers.IO) {
        //  make sure we have most recent firebase Info
        userRepository.firebaseAuth.currentUser!!.reload()
        val uid = userRepository.firebaseAuth.currentUser!!.uid
        if (companyName.isNotBlank()){
            dbCompanyRef.document(companyName).get().addOnSuccessListener { snapshot ->
                //  check if company exists
                if (!snapshot.exists()) {
                    Toast.makeText(
                        context,
                        "Company does not exist, please check spelling and try again",
                        Toast.LENGTH_LONG,
                    ).show()
                    //  update User's companyName
                } else {
                    val data = mapOf(
                        "companyname" to companyName,
                        "registered" to true
                    )
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(FirebaseAuth.getInstance().uid!!)
                        .set(data, SetOptions.merge()).addOnSuccessListener {
                            if (cb != null) {
                                cb()
                            }
                        }
                    addUserToCompany(companyName, uid)
                }
            }
        }
    }

    private fun addUserToCompany(companyName:String, uid:String){
        val user = dbUsersRef.document(uid).path
        val userObj = hashMapOf(
            "user" to user
        )
        dbCompanyRef.document(companyName).collection("memberIDs")
            .document().set(userObj, SetOptions.merge())
    }

    //  Get
        //  fetches list of active companies
    suspend fun getCompanyListFlow() : Flow<Response<List<String>>> = callbackFlow {
        lateinit var listener : ListenerRegistration
        trySend(Response.Loading())
        try {
            launch(Dispatchers.IO){
                listener = dbCompanyRef.addSnapshotListener { companyList, err ->
                    if (err!= null){
                        Log.e(tag, "Error Fetching Company List From Repo", err)
                        trySend(Response.Failure(err.localizedMessage ?: "Error Fetching Company List From Repo"))
                    }
                    if (companyList != null && !companyList.isEmpty){
                        val companies:MutableList<String> = mutableListOf()
                        for (company in companyList){
                            val name = company["companyname"].toString()
                            companies.add(name)
                        }
                        trySend(Response.Success(companies))
                    } else {
                        Log.i(tag, "CompanyList Snapshot is Empty")
                        trySend(Response.Success(mutableListOf()))
                    }
                }
            }
        } catch (err: Error){
            Log.e(tag, "Error Fetching CompanyList from Repo", err)
            trySend(Response.Failure(err.localizedMessage ?: "Error Fetching Flow From Repo"))
        }
        awaitClose {
            listener.remove()
            cancel()
        }
    }

    fun getMembersFlow(companyName: String): Flow<Response<List<User>>> = callbackFlow {
        lateinit var listener: ListenerRegistration
        trySend(Response.Loading())
        try {
            launch(Dispatchers.IO){
                listener = getCompanyMembersCollection(companyName).addSnapshotListener(MetadataChanges.INCLUDE){ members, error ->
                    if (error != null){
                        Log.e(tag, "Error Fetching Company Members ", error)
                        trySend(Response.Failure(message = error.localizedMessage ?: "Error Fetching Members From Firebase"))
                    }
                    if (members != null && !members.isEmpty){
                        val tasks = mutableListOf<Task<DocumentSnapshot>>()
                        val membersObj: MutableList<User> = mutableListOf()
                        for (member in members){
                            val task = database.document(member["user"].toString()).get()
                            tasks.add(task)
                        }
                        Tasks.whenAll(tasks).addOnCompleteListener { task ->
                            if (!task.isSuccessful){
                                Log.i(tag, "getMemberFlow: Failed from task.isSuccessful")
                                trySend(Response.Failure("Error Fetching Members From Database"))
                            } else {
                                tasks.forEach {
                                    val user = it.result.toObject(User::class.java)
                                    if (user != null) {
                                        membersObj.add(user)
                                    }
                                }
                                trySend(Response.Success(membersObj))
                            }
                        }
                    } else {
                        Log.i(tag, "getMemberFlow: Snapshot Is Empty")
                        trySend(Response.Success(mutableListOf()))
                    }
                }
            }
        } catch (err: Exception){
            trySend(Response.Failure(err.localizedMessage ?: "Error Fetching Flow From Repo"))
            channel.close(err)
            cancel(err.cause.toString())
        }
        awaitClose {
            listener.remove()
        }
    }

    fun getUserLocationListTask(user: User): CollectionReference {
        return dbUsersRef.document(user.id!!).collection("locations")
    }

    private fun getCompanyMembersCollection(companyName: String): CollectionReference {
        return dbCompanyRef.document(companyName).collection("memberIDs")
    }

    //  Delete
        // Delete Company Account, Owner Account and Data Associated with them, MAKE SURE TO RE-AUTHENTICATE USER BEFORE CALLING
    fun deleteCompanyAccount(companyName: String){
        val companyRef = dbCompanyRef.document(companyName)
        //  path to user variables
        val usersToReset = mutableListOf<String>()
        CoroutineScope(Dispatchers.IO).launch {
            //  Gets list of Company Users
            companyRef.collection("memberIDs").get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc ->
                        usersToReset.add(doc["user"].toString())
                    }
                    //  Delete Company Member Accounts
                    usersToReset.forEach { userPath ->
                        database.document(userPath).collection("locations").get().addOnSuccessListener { locations ->
                            locations.documents.forEach { doc ->
                                doc.reference.delete()
                            }
                        }
                        database.document(userPath).get().continueWith {
                            val pfpUrl:String = it.result["profilepicurl"].toString()
                            if (pfpUrl.isNotBlank() && pfpUrl != "null"){
                                FirebaseStorage.getInstance().getReferenceFromUrl(pfpUrl).delete()
                            }
                            it.result.reference.delete()
                        }
                        database.document(userPath).delete()
                    }
                    //  Delete Own Account
                    dbUsersRef.document(FirebaseAuth.getInstance().uid!!).collection("locations").get().addOnSuccessListener {
                        if (!it.isEmpty){
                            it.documents.forEach { doc ->
                                doc.reference.delete()
                            }
                        }
                    }
                    dbUsersRef.document(FirebaseAuth.getInstance().uid!!).get().addOnSuccessListener {
                        val pfpUrl:String = it["profilpicurl"].toString()
                        if (pfpUrl.isNotBlank() && pfpUrl != "null"){
                            FirebaseStorage.getInstance().getReferenceFromUrl(pfpUrl).delete()
                        }
                        it.reference.delete()
                    }
                    dbCompanyRef.document(companyName).delete()
                    // Sign out of Firebase and reload
                    FirebaseAuth.getInstance().currentUser!!.delete()
                    FirebaseAuth.getInstance().signOut()
                    FirebaseAuth.getInstance().currentUser?.reload()
                }
        }
    }
}

data class Company(
    val owner: String? = null,
    val token: String? = null,
    val companyname: String? = null,
)