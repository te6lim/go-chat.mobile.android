package com.simulatedtez.gochat.view_model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.api_services.AuthApiService
import com.simulatedtez.gochat.remote.api_usecases.LoginUsecase
import com.simulatedtez.gochat.remote.api_usecases.SignupUsecase
import com.simulatedtez.gochat.remote.client
import com.simulatedtez.gochat.repository.SignupEventListener
import com.simulatedtez.gochat.repository.SignupRepository
import com.simulatedtez.gochat.util.AppWideChatEventListener
import com.simulatedtez.gochat.util.androidConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SignupViewModel(
    private val signupRepository: SignupRepository,
): ViewModel(), SignupEventListener {

    private val _isSignupSuccessful = MutableLiveData<Boolean>()
    val isSignupSuccessful: LiveData<Boolean> = _isSignupSuccessful

    private val _isAutoLoginSuccessful = MutableLiveData<Boolean>()
    val isAutoLoginSuccessful: LiveData<Boolean> = _isAutoLoginSuccessful

    private val _isSigningUp = MutableLiveData<Boolean>()
    val isSigningUp: LiveData<Boolean> = _isSigningUp

    fun signUp(username: String, password: String) {
        _isSigningUp.value = true
        viewModelScope.launch(Dispatchers.IO) {
            signupRepository.signUp(username, password)
        }
    }

    override fun onSignUp() {
        _isSigningUp.postValue(false)
        _isSignupSuccessful.postValue(true)
    }

    override fun onSignUpAndLoginSuccess() {
        _isSigningUp.postValue(false)
        _isAutoLoginSuccessful.postValue(true)
    }

    override fun onSignUpFailed(errorResponse: IResponse.Failure<ParentResponse<String>>) {
        _isSigningUp.postValue(false)
        _isSignupSuccessful.postValue(false)
    }

    fun initializeAppWideChatService(context: Context) {
        session.setupAppWideChatService(AppWideChatEventListener.get(context))
    }

    fun cancel() { viewModelScope.cancel() }
}

class SignupViewModelProvider(private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authApiService = AuthApiService(client, androidConfig)
        val repo = SignupRepository(
            SignupUsecase(authApiService),
            LoginUsecase(authApiService),
            session
        )
        return SignupViewModel(repo).apply {
            repo.setEventListener(this)
        } as T
    }
}
