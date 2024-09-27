package com.websarva.wings.android.todo

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val validateHelper = ValidateHelper(this)
    private val dialogHelper = DialogHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // FirebaseAuthのインスタンスを取得
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ToolbarをActionBarとして設定
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(R.string.title_register)
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)

        // ActionBarに戻るボタンを有効化
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)

        // UIの要素を取得
        val userIDEditText = findViewById<EditText>(R.id.userIDEditText)
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)
        val userIDError = findViewById<TextInputLayout>(R.id.userID)
        val usernameError = findViewById<TextInputLayout>(R.id.username)
        val emailError = findViewById<TextInputLayout>(R.id.email)
        val passwordError = findViewById<TextInputLayout>(R.id.password)

        userIDEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたときの処理
                val (result: Boolean, errorMsg: String) = validateHelper.userIDCheck(userIDEditText)
                if (!result) {
                    userIDError.error = errorMsg
                    return@OnFocusChangeListener
                }
                usernameError.error = ""
                clearBordFocus()
            }
        }

        usernameEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたときの処理
                val (result: Boolean, errorMsg: String) = validateHelper.usernameCheck(usernameEditText)
                if (!result) {
                    usernameError.error = errorMsg
                    return@OnFocusChangeListener
                }
                usernameError.error = ""
                clearBordFocus()
            }
        }

        emailEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたときの処理
                val (result: Boolean, errorMsg: String) = validateHelper.emailCheck(emailEditText)
                if (!result) {
                    emailError.error = errorMsg
                    return@OnFocusChangeListener
                }
                emailError.error = ""
                clearBordFocus()
            }
        }

        passwordEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたときの処理
                val (result: Boolean, errorMsg: String) = validateHelper.passwordCheck(passwordEditText)
                if (!result) {
                    passwordError.error = errorMsg
                    return@OnFocusChangeListener
                }
                passwordError.error = ""
                clearBordFocus()
            }
        }

        // 登録ボタンがクリックされたときの処理
        buttonRegister.setOnClickListener {
            clearBordFocus()
            //すべての入力項目のバリデーションチェック
            val (resultUserID: Boolean, userIDMsg: String) = validateHelper.userIDCheck(userIDEditText)
            val (resultUsername: Boolean, usernameMsg: String) = validateHelper.usernameCheck(
                usernameEditText
            )
            val (resultEmail: Boolean, emailMsg: String) = validateHelper.emailCheck(emailEditText)
            val (resultPassword: Boolean, passwordMsg) = validateHelper.passwordCheck(passwordEditText)

            if (!(resultUserID && resultUsername && resultEmail && resultPassword)) {
                usernameError.error = usernameMsg
                emailError.error = emailMsg
                passwordError.error = passwordMsg
                return@setOnClickListener
            }

            val userID = userIDEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            registerUser(userID, email, username, password)
        }
    }

    private fun registerUser(userId: String, email: String, username: String, password: String) {
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val userIDError = findViewById<TextInputLayout>(R.id.userID)
                userIDError.error = "このユーザーIDは既に使用されています"
                return@addOnSuccessListener
            } else {
                checkEmailAndRegister(userId, email, username, password)
            }
        }.addOnFailureListener { showError("ユーザーID確認中にエラーが発生しました", it) }
    }

    private fun checkEmailAndRegister(userId: String, email: String, username: String, password: String) {
        db.collection("users").whereEqualTo("email", email).get().addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val emailError = findViewById<TextInputLayout>(R.id.email)
                emailError.error = "このメールアドレスは既に使用されています"
            } else {
                createFirebaseUser(userId, email, username, password)
            }
        }.addOnFailureListener { showError("メールアドレス確認中にエラーが発生しました", it) }
    }

    private fun createFirebaseUser(userId: String, email: String, username: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveUserToFirestore(userId, email, username)
            } else {
                dialogHelper.showErrorDialog("","ユーザー登録に失敗しました")
            }
        }
    }

    private fun saveUserToFirestore(userId: String, email: String, username: String) {
        val userData = hashMapOf("email" to email, "userId" to userId, "username" to username)
        db.collection("users").document(userId).set(userData).addOnSuccessListener {
            //アカウント作成成功
            val intent = Intent(this,HomeActivity::class.java)
            intent.putExtra("username",username)
            startActivity(intent)
        }.addOnFailureListener {
            dialogHelper.showErrorDialog("","ユーザー情報の保存に失敗しました")
        }
    }

    private fun showError(message: String, e: Exception?) {
        Log.w("Firestore", "$message: ", e)
    }

    // 戻るアイコンがタップされた時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // LoginActivityに遷移する処理
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clearBordFocus(){
        val userIDEditText = findViewById<EditText>(R.id.userIDEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(userIDEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(passwordEditText.windowToken, 0)
        //フォーカスを外す処理
        userIDEditText.clearFocus()
        passwordEditText.clearFocus()
    }
}