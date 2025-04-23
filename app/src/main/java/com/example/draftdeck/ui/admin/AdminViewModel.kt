package com.example.draftdeck.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.repository.ThesisRepository
import com.example.draftdeck.data.repository.UserRepository
import com.example.draftdeck.domain.usecase.auth.GetCurrentUserUseCase
import com.example.draftdeck.domain.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val thesisRepository: ThesisRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _currentStudentId = MutableStateFlow<String?>(null)
    
    private val _studentsList = MutableStateFlow<NetworkResult<List<User>>>(NetworkResult.Idle)
    val studentsList: StateFlow<NetworkResult<List<User>>> = _studentsList

    private val _advisorsList = MutableStateFlow<NetworkResult<List<User>>>(NetworkResult.Idle)
    val advisorsList: StateFlow<NetworkResult<List<User>>> = _advisorsList
    
    private val _studentThesesList = MutableStateFlow<NetworkResult<List<Thesis>>>(NetworkResult.Idle)
    val studentThesesList: StateFlow<NetworkResult<List<Thesis>>> = _studentThesesList

    private val _assignAdvisorResult = MutableStateFlow<NetworkResult<Thesis>>(NetworkResult.Idle)
    val assignAdvisorResult: StateFlow<NetworkResult<Thesis>> = _assignAdvisorResult

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collectLatest {
                _currentUser.value = it
            }
        }
    }

    fun loadStudents() {
        viewModelScope.launch {
            _studentsList.value = NetworkResult.Loading
            try {
                val students = userRepository.getUsersByRole(Constants.ROLE_STUDENT)
                // Sort students by last name then first name
                val sortedStudents = students.sortedWith(compareBy({ it.lastName }, { it.firstName }))
                _studentsList.value = NetworkResult.Success(sortedStudents)
                
                // Update thesis counts for each student
                updateStudentThesisCounts()
            } catch (e: Exception) {
                _studentsList.value = NetworkResult.Error(e)
            }
        }
    }

    private fun updateStudentThesisCounts() {
        viewModelScope.launch {
            // Get current students list
            val currentStudents = (_studentsList.value as? NetworkResult.Success)?.data ?: return@launch
            
            // For each student, fetch their theses to get an accurate count
            val updatedStudents = currentStudents.toMutableList()
            
            for (i in updatedStudents.indices) {
                val student = updatedStudents[i]
                try {
                    // Create a query map with student_id parameter
                    val queryParams = mapOf("student_id" to student.id)
                    
                    // Log the query parameters
                    Log.d("AdminViewModel", "Fetching thesis count for student ${student.id} with params: $queryParams")
                    
                    // Create a temporary list to collect the theses
                    val theses = mutableListOf<Thesis>()
                    
                    // Make the API call to collect theses for this specific student
                    thesisRepository.getTheses(null, null, null, queryParams).collect { result ->
                        if (result is NetworkResult.Success) {
                            theses.clear() // Ensure we don't double-count
                            
                            // Add only theses that match this student's ID
                            val studentTheses = result.data.filter { it.studentId == student.id }
                            theses.addAll(studentTheses)
                            
                            Log.d("AdminViewModel", "Found ${theses.size} theses for student ${student.id}")
                            
                            // Update this specific student in our list
                            updatedStudents[i] = student.copy(thesisCount = theses.size)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminViewModel", "Error updating thesis count for student ${student.id}: ${e.message}")
                }
            }
            
            // Update the students list with corrected thesis counts
            _studentsList.value = NetworkResult.Success(updatedStudents)
        }
    }

    fun loadAdvisors() {
        viewModelScope.launch {
            _advisorsList.value = NetworkResult.Loading
            try {
                val advisors = userRepository.getUsersByRole(Constants.ROLE_ADVISOR)
                // Sort advisors by last name then first name
                val sortedAdvisors = advisors.sortedWith(compareBy({ it.lastName }, { it.firstName }))
                _advisorsList.value = NetworkResult.Success(sortedAdvisors)
                
                // Update thesis counts for each advisor
                updateAdvisorThesisCounts()
            } catch (e: Exception) {
                _advisorsList.value = NetworkResult.Error(e)
            }
        }
    }
    
    private fun updateAdvisorThesisCounts() {
        viewModelScope.launch {
            // Get current advisors list
            val currentAdvisors = (_advisorsList.value as? NetworkResult.Success)?.data ?: return@launch
            
            // For each advisor, fetch theses they are advising to get an accurate count
            val updatedAdvisors = currentAdvisors.toMutableList()
            
            for (i in updatedAdvisors.indices) {
                val advisor = updatedAdvisors[i]
                try {
                    // Create a query map with advisor_id parameter
                    val queryParams = mapOf("advisor_id" to advisor.id)
                    
                    // Log the query parameters
                    Log.d("AdminViewModel", "Fetching thesis count for advisor ${advisor.id} with params: $queryParams")
                    
                    // Create a temporary list to collect the theses
                    val theses = mutableListOf<Thesis>()
                    
                    // Make the API call to collect theses for this specific advisor
                    thesisRepository.getTheses(null, null, null, queryParams).collect { result ->
                        if (result is NetworkResult.Success) {
                            theses.clear() // Ensure we don't double-count
                            
                            // Add only theses that match this advisor's ID
                            val advisorTheses = result.data.filter { it.advisorId == advisor.id }
                            theses.addAll(advisorTheses)
                            
                            Log.d("AdminViewModel", "Found ${theses.size} theses for advisor ${advisor.id}")
                            
                            // Update this specific advisor in our list
                            updatedAdvisors[i] = advisor.copy(thesisCount = theses.size)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminViewModel", "Error updating thesis count for advisor ${advisor.id}: ${e.message}")
                }
            }
            
            // Update the advisors list with corrected thesis counts
            _advisorsList.value = NetworkResult.Success(updatedAdvisors)
        }
    }

    fun loadStudentTheses(studentId: String) {
        viewModelScope.launch {
            _currentStudentId.value = studentId
            _studentThesesList.value = NetworkResult.Loading
            try {
                // Create a query map with student_id parameter
                val queryParams = mapOf("student_id" to studentId)
                Log.d("AdminViewModel", "Loading theses for student: $studentId with params: $queryParams")
                thesisRepository.getTheses(null, null, null, queryParams).collectLatest { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            // Filter to ensure we only have theses for this specific student
                            val studentTheses = result.data.filter { it.studentId == studentId }
                            Log.d("AdminViewModel", "Loaded ${studentTheses.size} theses for student: $studentId")
                            
                            // Update the state with the filtered list
                            _studentThesesList.value = NetworkResult.Success(studentTheses)
                        }
                        is NetworkResult.Error -> {
                            Log.e("AdminViewModel", "Error loading theses: ${result.exception.message}", result.exception)
                            _studentThesesList.value = result
                        }
                        else -> {
                            _studentThesesList.value = result
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Exception in loadStudentTheses: ${e.message}", e)
                _studentThesesList.value = NetworkResult.Error(e)
            }
        }
    }

    fun assignAdvisorToThesis(thesisId: String, advisorId: String) {
        viewModelScope.launch {
            _assignAdvisorResult.value = NetworkResult.Loading
            try {
                // Use the admin-specific method that allows assigning any advisor to a thesis
                thesisRepository.adminAssignAdvisorToThesis(thesisId, advisorId).collectLatest { result ->
                    _assignAdvisorResult.value = result
                    // After a successful assignment, refresh the thesis list
                    if (result is NetworkResult.Success) {
                        // Refresh the thesis list if we have an active student
                        val currentStudentId = _currentStudentId.value
                        if (currentStudentId != null) {
                            loadStudentTheses(currentStudentId)
                        }
                    }
                }
            } catch (e: Exception) {
                _assignAdvisorResult.value = NetworkResult.Error(e)
            }
        }
    }

    fun resetAssignAdvisorResult() {
        _assignAdvisorResult.value = NetworkResult.Idle
    }
} 