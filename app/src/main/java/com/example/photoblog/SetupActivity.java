package com.example.photoblog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private CircleImageView setupImage;
    private Uri mainImageURI=null;
    private EditText setupName;
    private Button setupBtn;
    private ProgressBar setupProgress;
    private String user_id;
    private boolean isChanged=false;
    private String downloadImageUrl,user_Name;

    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Toolbar setupToolbar=findViewById(R.id.setupToolbar);
        setSupportActionBar(setupToolbar);
        getSupportActionBar().setTitle("Account Setting");

        firebaseAuth=FirebaseAuth.getInstance();
        user_id=firebaseAuth.getCurrentUser().getUid();
        storageReference= FirebaseStorage.getInstance().getReference();
        firebaseFirestore=FirebaseFirestore.getInstance();

        setupImage =findViewById(R.id.setup_image);
        setupName=findViewById(R.id.setup_name);
        setupBtn=findViewById(R.id.setup_btn);
        setupProgress=findViewById(R.id.setup_progress);

        setupProgress.setVisibility(View.VISIBLE);
        setupBtn.setEnabled(false);
        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if(task.isSuccessful())
                {
                    if(task.getResult().exists())
                    {

                        String name=task.getResult().getString("name");
                        String image=task.getResult().getString("image");

                        RequestOptions placeholdeRequest= new RequestOptions();
                        placeholdeRequest.placeholder(R.drawable.defaulticon);
                        Toast.makeText(SetupActivity.this,"Data Exists.."+image,Toast.LENGTH_LONG).show();

                        setupName.setText(name);
                        Glide.with(SetupActivity.this).setDefaultRequestOptions(placeholdeRequest).load(image).into(setupImage);
                       // Picasso.get().load(image).into(setupImage);
                    }
                    else
                    {
                        Toast.makeText(SetupActivity.this,"Data doesn't Exists..",Toast.LENGTH_LONG).show();
                    }
                }
                else
                {
                    String error=task.getException().getMessage();
                    Toast.makeText(SetupActivity.this,"FireBaseStore Error:"+error,Toast.LENGTH_LONG).show();
                }
                setupProgress.setVisibility(View.INVISIBLE);
                setupBtn.setEnabled(true);
            }
        });




        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user_Name=setupName.getText().toString();
                setupProgress.setVisibility(View.VISIBLE);
                if(isChanged)
                {
                    if(!TextUtils.isEmpty(user_Name) && mainImageURI!=null)
                    {
                        setupProgress.setVisibility(View.VISIBLE);
                        user_id=firebaseAuth.getCurrentUser().getUid();
                        final StorageReference filePath= storageReference.child("profile_images").child(user_id+".jpg");


                      /*  image_path.putFile(mainImageURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task)
                            {
                                if(task.isSuccessful())
                                {
                                    storeFirestore(task,user_name);
                                }
                                else
                                {
                                    String error=task.getException().getMessage();
                                    Toast.makeText(SetupActivity.this,"Error:"+error,Toast.LENGTH_LONG).show();
                                    setupProgress.setVisibility(View.INVISIBLE);

                                }
                            }
                        });*/

                        final UploadTask uploadTask = filePath.putFile(mainImageURI);


                        uploadTask.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e)
                            {
                                String message = e.toString();
                                Toast.makeText(SetupActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            }
                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                            {
                                Toast.makeText(SetupActivity.this, "Product Image uploaded Successfully...", Toast.LENGTH_SHORT).show();

                                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                    @Override
                                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception
                                    {
                                        if (!task.isSuccessful())
                                        {
                                            throw task.getException();
                                        }

                                        downloadImageUrl = filePath.getDownloadUrl().toString();
                                        return filePath.getDownloadUrl();
                                    }
                                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task)
                                    {
                                        if (task.isSuccessful())
                                        {
                                            downloadImageUrl = task.getResult().toString();

                                            Toast.makeText(SetupActivity.this, "got the Product image Url Successfully...", Toast.LENGTH_SHORT).show();

                                            SaveImageToDatabase();
                                        }
                                    }
                                });
                            }
                        });
                    }
                }
                else
                {
                    SaveImageToDatabase();
                }

            }
        });

        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
                {
                    if(ContextCompat.checkSelfPermission(SetupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
                    {
                        Toast.makeText(SetupActivity.this,"Permission Denied",Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                    }
                    else
                    {
                        BringImagePicker();
                    }
                    /*if(ContextCompat.checkSelfPermission(SetupActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
                    {
                        Toast.makeText(SetupActivity.this,"Permission Denied",Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                    }
                    else
                    {
                        Toast.makeText(SetupActivity.this,"You already have permission ",Toast.LENGTH_LONG).show();
                    }*/

                }
                else
                {
                    BringImagePicker();
                }
            }
        });
    }

    /*private void storeFirestore(Task<UploadTask.TaskSnapshot> task  , String user_name) {

        String download_uri;
        if(task!=null)
        {
            download_uri=task.getResult().getMetadata().getReference().getDownloadUrl().toString();
        }
        else
        {
            download_uri=mainImageURI.toString();
        }


        Map<String,String> userMap=new HashMap<>();
        userMap.put("name",user_name);
        userMap.put("image",download_uri);

        firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful())
                {
                    Toast.makeText(SetupActivity.this,"The user settings are updated",Toast.LENGTH_LONG).show();
                    Intent intent=new Intent(SetupActivity.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                else {

                    String error=task.getException().getMessage();
                    Toast.makeText(SetupActivity.this,"FireBaseStore Error:"+error,Toast.LENGTH_LONG).show();
                }
            }
        });
        setupProgress.setVisibility(View.INVISIBLE);

    }*/

    private void BringImagePicker() {
        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1).start(SetupActivity.this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK)
            {

                mainImageURI= result.getUri();
                setupImage.setImageURI(mainImageURI);
                isChanged=true;

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
            {

                Exception error = result.getError();
            }
        }
    }
    private void SaveImageToDatabase()
    {
        HashMap<String, String> userMap = new HashMap<>();
        userMap.put("name",user_Name);
        userMap.put("image",downloadImageUrl);

        firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                setupProgress.setVisibility(View.VISIBLE);
                if(task.isSuccessful())
                {
                    setupProgress.setVisibility(View.INVISIBLE);
                    Toast.makeText(SetupActivity.this,"The user settings are updated",Toast.LENGTH_LONG).show();
                    Intent intent=new Intent(SetupActivity.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                else {

                    setupProgress.setVisibility(View.INVISIBLE);
                    String error=task.getException().getMessage();
                    Toast.makeText(SetupActivity.this,"FireBaseStore Error:"+error,Toast.LENGTH_LONG).show();
                }
            }
        });
        setupProgress.setVisibility(View.INVISIBLE);
    }
}
