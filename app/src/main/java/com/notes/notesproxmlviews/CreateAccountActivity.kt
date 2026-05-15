package com.notes.notesproxmlviews

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth


class CreateAccountActivity : AppCompatActivity() {
    var emailEditText: EditText? = null
    var passwordEditText: EditText? = null
    var confirmPasswordEditText: EditText? = null
    var createAccountBtn: Button? = null
    var progressBar: ProgressBar? = null
    var loginBtnTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        emailEditText = findViewById<EditText?>(R.id.email_edit_text)
        passwordEditText = findViewById<EditText?>(R.id.password_edit_text)
        confirmPasswordEditText = findViewById<EditText?>(R.id.confirm_password_edit_text)
        createAccountBtn = findViewById<Button?>(R.id.create_account_btn)
        progressBar = findViewById<ProgressBar?>(R.id.progress_bar)
        loginBtnTextView = findViewById<TextView?>(R.id.login_text_view_btn)

        createAccountBtn!!.setOnClickListener(View.OnClickListener { v: View? -> createAccount() })
        loginBtnTextView!!.setOnClickListener(View.OnClickListener { v: View? -> finish() })
    }

    fun createAccount() {
        val email = emailEditText!!.getText().toString()
        val password = passwordEditText!!.getText().toString()
        val confirmPassword = confirmPasswordEditText!!.getText().toString()

        val isValidated = validateData(email, password, confirmPassword)
        if (!isValidated) {
            return
        }

        createAccountInFirebase(email, password)
    }

    fun createAccountInFirebase(email: String, password: String) {
        changeInProgress(true)

        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(
            this@CreateAccountActivity,
            object : OnCompleteListener<AuthResult?> {

                override fun onComplete(task: Task<AuthResult?>) {
                    changeInProgress(false)
                    if (task.isSuccessful) {
                        // creating acc is done
                        Utility.showToast(
                            this@CreateAccountActivity,
                            "Successfully create account,Check email to verify"
                        )
                        firebaseAuth.currentUser!!.sendEmailVerification()
                        firebaseAuth.signOut()
                        finish()
                    } else {
                        // failure
                        Utility.showToast(
                            this@CreateAccountActivity,
                            task.exception!!.localizedMessage
                        )
                    }
                }
            }
        )
    }

    /**
     * show or hide progress bar
     * @param inProgress
     */
    fun changeInProgress(inProgress: Boolean) {
        progressBar!!.visibility = if (inProgress) View.VISIBLE else View.GONE
        createAccountBtn!!.visibility = if (inProgress) View.GONE else View.VISIBLE
    }

    /**
     * validate the data that are input by user.
     * @param email
     * @param password
     * @param confirmPassword
     * @return true if data is valid, false otherwise
     */
    fun validateData(email: String, password: String, confirmPassword: String?): Boolean {
        //validate the data that are input by user.

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText!!.error = "Email is invalid"
            return false
        }
        if (password.length < 6) {
            passwordEditText!!.error = "Password length is invalid"
            return false
        }
        if (password != confirmPassword) {
            confirmPasswordEditText!!.error = "Password not matched"
            return false
        }
        return true
    }
}