package frangsierra.kotlinfirechat.session.store

import dagger.Binds
import dagger.Module
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import frangsierra.kotlinfirechat.core.dagger.AppScope
import frangsierra.kotlinfirechat.core.flux.prefs
import frangsierra.kotlinfirechat.util.taskRunning
import mini.Reducer
import mini.Store
import javax.inject.Inject

@AppScope
class SessionStore @Inject constructor(val controller: SessionController) : Store<SessionState>() {

    @Reducer
    fun loginWithCredentials(action: LoginWithCredentials, state: SessionState): SessionState {
        if (state.loginTask.isRunning()) return state
        controller.loginWithCredentials(action.email, action.password)
        return state.copy(loginTask = taskRunning())
    }

    @Reducer
    fun loginWithProvider(action: LoginWithProviderCredentials, state: SessionState): SessionState {
        if (state.loginTask.isRunning()) return state
        controller.loginWithProviderCredentials(action.credential, action.email)
        return state.copy(loginTask = taskRunning())
    }

    @Reducer(priority = 10)
    fun loginComplete(action: LoginCompleteAction, state: SessionState): SessionState {
        if (!state.loginTask.isRunning()) return state
        prefs.loggedUserId = action.user?.uid //Update the shared prefs of the current user
        prefs.loggedUsername = action.user?.username //Update the shared prefs of the current user
        return state.copy(
            loggedUser = action.user,
            loginTask = action.task,
            verified = action.emailVerified,
            providers = action.associatedProviders)
    }

    @Reducer
    fun createAccountWithCredentials(action: CreateAccountWithCredentialsAction, state: SessionState): SessionState {
        if (state.createAccountTask.isRunning()) return state
        controller.createAccountWithCredentials(action.email, action.password, action.username)
        return state.copy(createAccountTask = taskRunning())
    }

    @Reducer
    fun createAccountWithProvider(action: CreateAccountWithProviderCredentialsAction, state: SessionState): SessionState {
        if (state.createAccountTask.isRunning()) return state
        controller.createAccountWithProviderCredentials(action.credential, action.user)
        return state.copy(createAccountTask = taskRunning())
    }

    @Reducer
    fun createAccountComplete(action: CreateAccountCompleteAction, state: SessionState): SessionState {
        if (!state.createAccountTask.isRunning()) return state
        if (action.task.isSuccessful()) {
            prefs.loggedUserId = action.user!!.uid //Update the shared prefs of the current user
            prefs.loggedUsername = action.user.username //Update the shared prefs of the current user
        }
        return state.copy(
            loggedUser = action.user,
            createAccountTask = action.task,
            verified = action.emailVerified,
            providers = action.associatedProviders)
    }
}

@Module
abstract class SessionModule {
    @Binds
    @AppScope
    @IntoMap
    @ClassKey(SessionStore::class)
    abstract fun provideSessionStore(store: SessionStore): Store<*>

    @Binds
    @AppScope
    abstract fun bindSessionController(impl: SessionControllerImpl): SessionController
}
