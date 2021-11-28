package ru.skillbranch.kotlinexample.extensions

import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String
    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().uppercaseChar() }
            .joinToString(" ")
    private var phone: String? = null
        set(value) {
            field = value?.formatPhone()
        }

    private var _login: String? = null
    internal var login: String
        set(value) {
            _login = value.lowercase(Locale.getDefault())
        }
        get() = _login!!
    private val salt: String by lazy {
        ByteArray(16).also {
            SecureRandom().nextBytes(it)
        }.toString()
    }
    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        passwordHash = encrypt(password)
    }

    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        val code: String = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code

        sendAccessCodeToUser(rawPhone, code)
    }

    init {
        println("First init block, primary constructor was called")

        check(firstName.isNotBlank()) { "FirstName must be not blank" }
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email or phone must be not blank" }

        phone = rawPhone

        println("Phone validate lenth: ${phone?.length }} ")
        phone?.trim()?.validPhone()

        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
            """.trimIndent()

        println("UserInfo $userInfo")
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalStateException("The entered password does not match the current password")
    }

    fun requestAccessCode() {
        accessCode = generateAccessCode().also {
            passwordHash = encrypt(it)
        }
    }

    private fun encrypt(password: String): String = salt.plus(password).md5()

    private fun String.md5(): String {
        val messageDigest = MessageDigest.getInstance("MD5")
        val digest = messageDigest.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuffer().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String, code: String) {
        println("..... sending access code $code to $phone")
    }

    private fun String.validPhone() {
        if (length != 12 || first() != '+') throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastname) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastname, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastname, email, password)
                else -> throw IllegalArgumentException("Email or phone must not be a null or blank")
            }
        }

        private fun String.fullNameToPair(): Pair<String, String?> =
            this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException("Fullname must contain only firstname and lastname, current split result ${this@fullNameToPair}")
                    }
                }
    }
}
fun String.formatPhone() = this.replace("""[^+\d]""".toRegex(), "")