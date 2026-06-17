
package com.example.masterenglishfluency



import androidx.compose.runtime.Composable

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import androidx.compose.runtime.key

import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberCoroutineScope

import androidx.navigation3.runtime.entryProvider

import androidx.navigation3.runtime.rememberNavBackStack

import androidx.navigation3.ui.NavDisplay

import com.example.masterenglishfluency.data.UserProgressRepository

import com.example.masterenglishfluency.ui.login.LoginScreen

import com.example.masterenglishfluency.ui.dashboard.DashboardScreen

import com.example.masterenglishfluency.ui.speaking.SpeakingPracticeScreen

import kotlinx.coroutines.launch



@Composable

fun MainNavigation() {

  val repository = remember { UserProgressRepository.getInstance() }

  val isLoggedIn by repository.isLoggedIn.collectAsState(initial = null)

  val scope = rememberCoroutineScope()



  if (isLoggedIn == null) {

    // Show splash or loading while checking database/preferences

    return

  }



  key(isLoggedIn) {

    val startDestination = if (isLoggedIn == true) Dashboard else Login

    val backStack = rememberNavBackStack(startDestination)



    NavDisplay(

      backStack = backStack,

      onBack = { backStack.removeLastOrNull() },

      entryProvider =

        entryProvider {

          entry<Login> {

            LoginScreen(

              onLoginSuccess = {

                // isLoggedIn will trigger key recreation

              }

            )

          }

          entry<Dashboard> {

            DashboardScreen(

              onNavigateToSpeakingPractice = {

                backStack.add(SpeakingPractice)

              },

              onLogout = {

                scope.launch {

                  repository.setLoginState("", false)

                }

              }

            )

          }

          entry<SpeakingPractice> {

            SpeakingPracticeScreen(

              onBack = {

                backStack.removeLastOrNull()

              }

            )

          }

        },

    )

  }

}




