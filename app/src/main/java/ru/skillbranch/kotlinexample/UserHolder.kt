package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        return User.makeUser(fullName, email = email, password = password).also {
            if (!map.containsKey(it.login)) map[it.login] = it
            else throw IllegalArgumentException("A user with this email already exists")
        }
    }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        return User.makeUser(fullName, phone = rawPhone).also {
            if (!map.containsKey(it.login)) map[it.login] = it
            else throw IllegalArgumentException("A user with this phone already exists")
        }
    }

    fun loginUser(
        login: String,
        password: String
    ): String? {
        val _login = if(login.startsWith("+")) login.formatPhone() else login
        return map[_login.trim()]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    fun requestAccessCode(login: String) {
        val _login = if(login.startsWith("+")) login.formatPhone() else login
        map[_login]?.requestAccessCode()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }
}