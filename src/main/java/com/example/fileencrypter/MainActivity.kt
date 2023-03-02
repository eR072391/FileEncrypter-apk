package com.example.fileencrypter

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class MainActivity : AppCompatActivity() {
override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val buttonEnc = findViewById<Button>(R.id.button_enc)
        buttonEnc.setOnClickListener{
            encFileLauncher.launch("*/*")
        }

        val buttonDec = findViewById<Button>(R.id.button_dec)
        buttonDec.setOnClickListener{
            decFileLauncher.launch("*/*")
        }
    }

    //ActivityResultLauncherの宣言
    //registerForActivityResult()メソッドで、ActivityResultLauncherを返す
    //GetContent()メソッドは、ファイル選択ダイアログを開くためのアクションを指定する
    private val decFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter Password")

            val input = EditText(this)
            builder.setView(input)

            //OK
            builder.setPositiveButton("OK") { _, _ ->
                var inputPassword = input.text.toString()

                if(inputPassword != null){
                    val context = applicationContext
                    val decryptedBytes: ByteArray = deCrypto(context, uri, inputPassword)
                    saveDecryptedFile(context, uri, decryptedBytes)
                }
            }
            //Chancel
            builder.setNegativeButton("Cancel"){ dialog, _ ->
                dialog.cancel()
            }
            builder.show()

        }
    }

    private fun saveDecryptedFile(context: Context, uri: Uri, decryptedBytes: ByteArray){
        val extension = ".enc"
        val fileName = getFileName(context, uri)

        val filePath = getExternalFilesDir(null)!!.path + fileName!!.substring(0, fileName.length - extension.length)
        Log.d("Test", "path : $filePath")
        val outputStream = BufferedOutputStream(FileOutputStream(filePath))
        outputStream.write(decryptedBytes)
        outputStream.close()

        toast("Decrypted File Save.")
    }



    private fun deCrypto(context: Context, uri: Uri, password: String): ByteArray {
        //ファイルをバイト配列で読み込む
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileBytes = inputStream?.readBytes()
        inputStream?.close()

        //復号化のためのパスワードを設定
        val passwordBytes = password.toByteArray(Charsets.UTF_8)

        // 復号化のための秘密鍵を生成するためのパディングバイト数を計算する
        val paddingLength = if (passwordBytes.size % 32 == 0) {
            0
        } else {
            32 - (passwordBytes.size % 32)
        }

        // パディングしたパスワードバイト列を作成する
        val paddedPassword = ByteArray(passwordBytes.size + paddingLength)
        System.arraycopy(passwordBytes, 0, paddedPassword, 0, passwordBytes.size)

        // 秘密鍵生成
        val cipher = Cipher.getInstance("AES")
        val secretKey = SecretKeySpec(paddedPassword, "AES/CBC/PKCS5Padding")
        val ivParameterSpec = IvParameterSpec(paddedPassword)
        //IVはpasswordを使用
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

        //暗号化されたバイト列を復号化する
        val decryptedBytes = cipher.doFinal(fileBytes)

        return decryptedBytes
    }



    //ActivityResultLauncherの宣言
    //registerForActivityResult()メソッドで、ActivityResultLauncherを返す
    //GetContent()メソッドは、ファイル選択ダイアログを開くためのアクションを指定する
    private val encFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter Password")

            val input = EditText(this)
            builder.setView(input)

            //OK
            builder.setPositiveButton("OK") { _, _ ->
                var inputPassword = input.text.toString()

                if(inputPassword != null){
                    val context = applicationContext
                    val encryptedBytes: ByteArray = enCrypto(context, uri, inputPassword)
                    saveEncryptedFile(context, uri, encryptedBytes)
                }
            }
            //Chancel
            builder.setNegativeButton("Cancel"){ dialog, _ ->
                dialog.cancel()
            }
            builder.show()

        }
    }

    private fun saveEncryptedFile(context: Context, uri: Uri, encryptedBytes: ByteArray){
        val extension = ".enc"
        val fileName = getFileName(context, uri)
        val filePath = getExternalFilesDir(null)!!.path + "$fileName$extension"
        Log.d("Test", "path : $filePath")
        val outputStream = BufferedOutputStream(FileOutputStream(filePath))
        outputStream.write(encryptedBytes)
        outputStream.close()

        toast("Encrypted File Save.")
    }


    //ファイル名を取得する
    @SuppressLint("Range")
    fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                fileName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
        return fileName
    }

    private fun enCrypto(context: Context, uri: Uri, password: String): ByteArray {
        //ファイルをバイト配列で読み込む
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileBytes = inputStream?.readBytes()
        inputStream?.close()

        //入力されたパスワードのバイト数を取得する
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        val passwordLength = passwordBytes.size

        // 秘密鍵を生成するためのパディングバイト数を計算する
        val paddingLength = if (passwordLength % 32 == 0) {
            0
        } else {
            32 - (passwordLength % 32)
        }

        // パディングしたパスワードバイト列を作成する
        val paddedPassword = ByteArray(passwordLength + paddingLength)
        System.arraycopy(passwordBytes, 0, paddedPassword, 0, passwordLength)
        
        // 秘密鍵生成
        val cipher = Cipher.getInstance("AES")
        val secretKey = SecretKeySpec(paddedPassword, "AES/CBC/PKCS5Padding")
        val ivParameterSpec = IvParameterSpec(paddedPassword)
        //IVはpasswordを使用
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
        //暗号化
        val encryptedBytes = cipher.doFinal(fileBytes)

        return encryptedBytes
    }

    //toast("hello")のように呼び出して、文字を表示させられる
    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

