package com.websarva.wings.android.todo

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private val validate = ValidateHelper(this)
    private val dialogHelper = DialogHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //Toolbar取得
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitle(R.string.title_login)
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)

        val userIDEditText = findViewById<EditText>(R.id.userIDEditText)
        val userIDError = findViewById<TextInputLayout>(R.id.userID)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val passwordError = findViewById<TextInputLayout>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpButton = findViewById<Button>(R.id.signUpButton)

        userIDEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたときの処理
                val (result: Boolean, errorMsg: String) = validate.userIDCheck(userIDEditText)
                if (!result) {
                    userIDError.error = errorMsg
                    return@OnFocusChangeListener
                }
                userIDError.error = ""
            }
        }
        passwordEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたときの処理
                val (result: Boolean, errorMsg: String) = validate.passwordCheck(passwordEditText)
                if (!result) {
                    passwordError.error = errorMsg
                    return@OnFocusChangeListener
                }
                passwordError.error = ""
            }
        }

        loginButton.setOnClickListener {
            clearBordFocus()
            //すべての入力項目のバリデーションチェック
            val (resultUserID: Boolean, userIDMsg: String) = validate.userIDCheck(userIDEditText)
            val (resultPassword: Boolean, passwordMsg) = validate.passwordCheck(passwordEditText)
            if (!(resultUserID && resultPassword)) {
                userIDError.error = userIDMsg
                passwordError.error = passwordMsg
                return@setOnClickListener
            }
            val userID = userIDEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            loginUser(userID, password)

        }

        signUpButton.setOnClickListener {
            clearBordFocus()
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish() // このアクティビティを終了して、戻れないようにする
        }
    }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    fun loginUser(userID: String, password: String) {
        // Firestoreからユーザー情報を取得
        db.collection("users").document(userID).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val email = document.getString("email")
                    email?.let {
                        // メールアドレスを使用してFirebase Authenticationでログイン
                        auth.signInWithEmailAndPassword(it, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // ログイン成功
                                    val user = auth.currentUser
                                    // 次の画面に遷移する処理をここに追加
                                    val intent = Intent(this,HomeActivity::class.java)
                                    intent.putExtra("userID",user?.uid)
                                    intent.putExtra("email",user?.email)
                                    startActivity(intent)
                                } else {
                                    //パスワードが間違っていた時
                                    // ログイン失敗時にダイアログを表示
                                    dialogHelper.showErrorDialog("ログイン失敗", "ユーザーIDまたはパスワードが間違っています。")
                                }
                            }
                    }
                }
                else {
                    //ユーザーIDが間違っていた時
                    // ログイン失敗時にダイアログを表示
                    dialogHelper.showErrorDialog("ログイン失敗", "ユーザーIDまたはパスワードが間違っています。")
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "エラー: ", e)
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