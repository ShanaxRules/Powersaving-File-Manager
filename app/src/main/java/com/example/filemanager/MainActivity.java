package com.example.filemanager;

import static com.example.filemanager.R.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPreferences";
    private static final String PASSWORD_KEY = "AppPassword";

    private String hashPassword(String password){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b: hashedBytes){
                String hex = Integer.toHexString(0xff & b);
                if (hex.length()==1)hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password" , e);
        }
    }

    private void savePasswordHash(String hash){
        getSharedPreferences(PREFS_NAME , MODE_PRIVATE).edit().putString(PASSWORD_KEY , hash).apply();
    }

    private String getSavedPasswordHash(){
        return getSharedPreferences(PREFS_NAME , MODE_PRIVATE)
                .getString(PASSWORD_KEY , null);
    }

    private void showPasswordPrompt(boolean isFirstLaunch){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isFirstLaunch? "Set Password" : "Enter Password");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton(isFirstLaunch ? "Set" : "Login", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String enteredPassword = input.getText().toString();
                if (isFirstLaunch){
                    savePasswordHash(hashPassword(enteredPassword));
                    Toast.makeText(MainActivity.this , "Password Set successfully" , Toast.LENGTH_SHORT).show();
                }
                else{
                    String savedHash = getSavedPasswordHash();
                    if (savedHash != null && savedHash.equals(hashPassword(enteredPassword))){
                        Toast.makeText(MainActivity.this , "Login Successful" , Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(MainActivity.this , "Incorrect Password" , Toast.LENGTH_SHORT).show();
                        showPasswordPrompt(false);
                    }

                }
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);

        String savedHash = getSavedPasswordHash();
        if (savedHash ==null){
            showPasswordPrompt(true);
        }else{
            showPasswordPrompt(false);
        }


        requestManageExternalStoragePermission();


    }

    private boolean isFileManagerInitialized;

    private boolean[] selection;

    private File[] files;

    List <String> filesList;

    private int filesFoundCount;

    private Button refreshButton;

    private File dir;

    private String currentPath;

    private boolean isLongClick;

    private int selectedItemIndex;

    private String copyPath;

    private void openFile(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", file);
        intent.setDataAndType(uri, getMimeType(file.getAbsolutePath()));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No application found to open this file", Toast.LENGTH_SHORT).show();
        }
    }


    private String getMimeType(String url) {
        String type = "*/*"; // Default type

        String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(url)).toString());
        if (extension != null) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mimeType != null) {
                type = mimeType;
            }
        }
        return type;
    }





    @Override
    protected void onResume(){
        super.onResume();

        if (!isFileManagerInitialized){

            currentPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            final String rootPath = currentPath.substring(0,currentPath.lastIndexOf('/'));


            final TextView pathOutput = findViewById(id.pathOutput);

            final ListView listView = findViewById(id.listView);

            final TextAdapter textAdapter1 = new TextAdapter();
            listView.setAdapter(textAdapter1);
            filesList = new ArrayList<>();



            refreshButton = findViewById(R.id.refresh);
            refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/')+1));
                    dir = new File(currentPath);
                    files = dir.listFiles();
                    filesFoundCount = files.length;
                    selection  = new boolean[filesFoundCount];

                    textAdapter1.setSelection(selection);

                    filesList.clear();



                    for (int i=0;i<filesFoundCount;i++){
                        filesList.add(String.valueOf(files[i].getAbsolutePath()));
                    }

                    textAdapter1.setData(filesList);


                }
            });


            refreshButton.callOnClick();

            final Button goBackButton = findViewById(R.id.goBack);
            goBackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentPath.equals(rootPath)){
                        return;
                    }

                    currentPath = currentPath.substring(0,currentPath.lastIndexOf('/'));

                    refreshButton.callOnClick();

                }
            });


            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!isLongClick){
                                if (files[position].isDirectory()){
                                    currentPath = files[position].getAbsolutePath();
                                    dir = new File(currentPath);
                                    pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/')+1));
                                    refreshButton.callOnClick();
                                    selection = new boolean[files.length] ;
                                    textAdapter1.setSelection(selection);

                                }else{
                                    openFile(files[position]);
                                }

                            }
                        }
                    }, 50);

                }
            });










            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    findViewById(R.id.copy).setVisibility(View.GONE);
                    isLongClick = true;
                    selection[position] = !selection[position];
                    textAdapter1.setSelection(selection);

                    int selectionCount = 0;
                    for (boolean aselection : selection){
                        if(aselection){
                            selectionCount++;
                        }
                    }
                    if (selectionCount>0){

                        if (selectionCount==1){
                            selectedItemIndex = position;
                            findViewById(R.id.rename).setVisibility(View.VISIBLE);
                            if (!files[selectedItemIndex].isDirectory()){
                                findViewById(R.id.copy).setVisibility(View.VISIBLE);
                            }

                        }else{
                            findViewById(R.id.rename).setVisibility(View.GONE);
                            findViewById(R.id.copy).setVisibility(View.GONE);

                        }
                        findViewById(R.id.bottomBar).setVisibility(View.VISIBLE);
                    }else{
                        findViewById(R.id.bottomBar).setVisibility(View.GONE);

                    }

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isLongClick = false;
                        }


                    }, 1000);
                    return false;
                }
            });

            final Button b1 = findViewById(R.id.b1);


            b1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                    deleteDialog.setTitle("Delete");
                    deleteDialog.setMessage("Do you really want to delete the file?");
                    deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (int i=0;i<files.length;i++){
                                if (selection[i]){
                                    deleteFileOrFolder(files[i]);
                                    selection[i]=false;
                                }
                            }

                            refreshButton.callOnClick();
                            findViewById(R.id.bottomBar).setVisibility(View.GONE);




                        }
                    });
                    deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            findViewById(R.id.bottomBar).setVisibility(View.GONE);

                        }
                    });

                    deleteDialog.show();
                }
            });


            final Button createNewFolder = findViewById(R.id.newFolder);

            createNewFolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder newFolderDialog = new AlertDialog.Builder(MainActivity.this);
                    newFolderDialog.setTitle("Create New Folder");
                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    newFolderDialog.setView(input);
                    newFolderDialog.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final File newFolder = new File(currentPath+"/"+input.getText());
                                    if (!newFolder.exists()){
                                        newFolder.mkdir();
                                        refreshButton.callOnClick();
                                        findViewById(R.id.bottomBar).setVisibility(View.GONE);

                                    }

                                }
                            });

                    newFolderDialog.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    findViewById(R.id.bottomBar).setVisibility(View.GONE);

                                }
                            });

                    newFolderDialog.show();



                }
            });

            final Button renameButton = findViewById(R.id.rename);
            renameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder renameDialog = new AlertDialog.Builder(MainActivity.this);
                    renameDialog.setTitle("Rename to:");
                    final EditText input = new EditText(MainActivity.this);
                    final String renamePath = files[selectedItemIndex].getAbsolutePath();
                    input.setText(renamePath.substring(renamePath.lastIndexOf('/')));
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    renameDialog.setView(input);

                    renameDialog.setPositiveButton("Rename",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String s = new File(renamePath).getParent() + "/" + input.getText();
                                    File newFile = new File(s);

                                    new File(renamePath).renameTo(newFile);
                                    refreshButton.callOnClick();
                                    selection = new boolean[files.length];
                                    textAdapter1.setSelection(selection);
                                    findViewById(R.id.bottomBar).setVisibility(View.GONE);

                                }
                            });

                    renameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            refreshButton.callOnClick();
                            findViewById(R.id.bottomBar).setVisibility(View.GONE);

                        }
                    });



                    renameDialog.show();



                }
            });

            final Button copyButton = findViewById(R.id.copy);
            copyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyPath = files[selectedItemIndex].getAbsolutePath();
                   selection = new boolean[files.length];
                   textAdapter1.setSelection(selection);
                   findViewById(R.id.paste).setVisibility(View.VISIBLE);

                }
            });

            final Button pasteButton = findViewById(R.id.paste);
            pasteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pasteButton.setVisibility(View.GONE);
                    String dstPath =currentPath + copyPath.substring(copyPath.lastIndexOf('/'));
                    copy(new File(copyPath) , new File(dstPath));
                    files = new File(currentPath).listFiles();
                    selection = new boolean[files.length];
                    textAdapter1.setSelection(selection);
                    refreshButton.callOnClick();
                    findViewById(R.id.bottomBar).setVisibility(View.GONE);

                }
            });

            isFileManagerInitialized = true;
        }else{
            refreshButton.callOnClick();
        }

    }

    private void copy(File src , File dst){
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            byte buf[] = new byte[1024];
            int len;

            while((len=in.read(buf))>0){
                out.write(buf , 0 , len);
            }

            out.close();
            in.close();



        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }










    private void requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_PERMISSIONS);
            } else {
                // Permission already granted
                Toast.makeText(this, "Manage External Storage permission is granted", Toast.LENGTH_SHORT).show();
            }
        } else {
            // For devices below Android 11, request WRITE_EXTERNAL_STORAGE
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Manage External Storage permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Manage External Storage permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    class TextAdapter extends BaseAdapter{

        private List<String> data = new ArrayList<>();

        private boolean[] selection;

        public void setData(List<String>data){
            if (data!=null){
                this.data.clear();
                if(!data.isEmpty()){
                    this.data.addAll(data);
                }
                notifyDataSetChanged();
            }
        }

        void setSelection(boolean[] selection){
            if (selection!=null){
                this.selection = new boolean[selection.length];
                for (int i=0;i<selection.length;i++){
                    this.selection[i] = selection[i];

                }
                notifyDataSetChanged();
            }
        }




        @Override
        public int getCount(){
            return data.size();
        }
        @Override
        public String getItem(int position){
            return data.get(position);
        }
        @Override
        public long getItemId(int position){
            return 0;
        }
        @Override
        public View getView(int position , View convertView , ViewGroup parent){
            if (convertView==null){
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item , parent, false);
                convertView.setTag(new ViewHolder((TextView)convertView.findViewById(id.textItem)));
            }

            ViewHolder holder = (ViewHolder) convertView.getTag();
            final String item = getItem(position);
            holder.info.setText(item.substring(item.lastIndexOf('/')+1));
            if (selection!=null){
                if(selection[position]){
                    holder.info.setBackgroundColor(Color.argb(100,9,9,9));
                }
                else{
                    holder.info.setBackgroundColor(Color.WHITE);
                }

            }
            return convertView;
        }

        class ViewHolder{
            TextView info;

            ViewHolder(TextView info){
                this.info=info;
            }
        }





    }

    private static final int REQUEST_PERMISSIONS=1234;

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE

    };



    @SuppressLint("NewApi")
    private boolean arePermissionsDenied(){

            return false;
    }






    private void deleteFileOrFolder(File fileOrFolder){
            if (fileOrFolder.isDirectory()){
                if(fileOrFolder.list().length==0){
                    fileOrFolder.delete();
                }else{
                    String files[] = fileOrFolder.list();

                    for (String temp:files){
                        File fileToDelete = new File(fileOrFolder , temp );
                        deleteFileOrFolder(fileToDelete);
                    }
                    if (fileOrFolder.list().length==0){
                        fileOrFolder.delete();
                    }
                }
            }else{
                fileOrFolder.delete();
            }
    }


    @Override
    public void onRequestPermissionsResult(final int requestCode , final String[] permissions , final int[] grantResults){
        super.onRequestPermissionsResult(requestCode , permissions , grantResults);
        if (requestCode == REQUEST_PERMISSIONS && grantResults.length>0){
            if (arePermissionsDenied()){
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }else{
                onResume();
            }
        }
    }





}
















