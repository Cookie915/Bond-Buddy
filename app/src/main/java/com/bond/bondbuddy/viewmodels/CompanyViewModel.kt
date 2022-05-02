package com.bond.bondbuddy.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bond.bondbuddy.models.Response
import com.bond.bondbuddy.models.User
import com.bond.bondbuddy.repo.CompanyRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class CompanyViewModel @Inject constructor(
    private val companyRepository: CompanyRepository,
) : ViewModel() {
    val tag = "companyViewModel"

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    val fireStore = FirebaseFirestore.getInstance()

    private var mCompanyMembersFlow = MutableStateFlow<Response<List<User>>>(Response.Loading())
    val companyMembers: StateFlow<Response<List<User>>>
        get() = mCompanyMembersFlow

    private var mCompanyListFlow = MutableStateFlow<Response<List<String>>>(Response.Loading())
    val companyList: StateFlow<Response<List<String>>>
        get() = mCompanyListFlow

    //  Create
    fun createCompany(name: String, context: Context, cb: () -> Unit) = coroutineScope.launch {
        companyRepository.createCompany(name, context, cb)
    }

    //  Set
    fun setUserCompany(ctx: Context, companyName: String, cb: (() -> Unit)? = null) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            companyRepository.setUserCompany(ctx, companyName, cb)
        }
    }

    //  Get
    private fun getCompanyList(){
        viewModelScope.launch {
            companyRepository.getCompanyListFlow().collect{
                mCompanyListFlow = MutableStateFlow(it)
            }
        }
    }

    fun getUserLocationList(user: User): CollectionReference {
        return companyRepository.getUserLocationListTask(user)
    }

    //  Delete
        // Remove Company Member Deleting data, called from AdminHomeScreen
    suspend fun removeCompanyMember(uid: String, companyName: String) = withContext(Dispatchers.IO) {
        val fireStore = FirebaseFirestore.getInstance()
        val memberIDTask = companyRepository.dbCompanyRef.document(companyName).collection("memberIDs").whereEqualTo("user", "users/$uid").get()
        val tokenTask = fireStore.collection("fcmtokens").whereEqualTo("id", uid).get()
        Tasks.whenAll(memberIDTask, tokenTask).addOnSuccessListener {
            val membersToDelete = mutableSetOf<DocumentSnapshot>()
            membersToDelete.addAll(memberIDTask.result.documents)
            membersToDelete.addAll(tokenTask.result.documents)
            memberIDTask.result.documents.forEach { doc ->
                fireStore.document(doc["user"].toString()).get().continueWith {
                    val pfpUrl:String = it.result["profilepicurl"].toString()
                    if (pfpUrl.isNotBlank() && pfpUrl != "null"){
                        FirebaseStorage.getInstance().getReferenceFromUrl(pfpUrl).delete()
                    }
                }
            }
            membersToDelete.forEach {
                it.reference.delete()
            }
            companyRepository.dbUsersRef.document(uid).collection("locations").get().addOnSuccessListener { locations ->
                locations.documents.forEach { loc ->
                    loc.reference.delete()
                }
                companyRepository.dbUsersRef.document(uid).delete()
            }
        }
    }
        //  Delete Company Owner Account
    fun deleteCompanyAccount(companyName: String) {
        companyRepository.deleteCompanyAccount(companyName)
    }

    //  Initializations Called in Nav Graph
    fun initializeCompanyMembers(companyName: String) {
        viewModelScope.launch {
            if (companyMembers.value is Response.Loading){
                companyRepository.getMembersFlow(companyName).collect{
                    mCompanyMembersFlow.value = it
                }
            }
        }
    }

    init {
        Log.i(tag, "Init CompanyViewModel ${this.hashCode()}")
        getCompanyList()
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(tag, "Clearing ViewModel ${this.hashCode()}")
    }
}




