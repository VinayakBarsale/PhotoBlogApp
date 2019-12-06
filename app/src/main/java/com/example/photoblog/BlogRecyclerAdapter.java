package com.example.photoblog;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlogRecyclerAdapter extends RecyclerView.Adapter<BlogRecyclerAdapter.ViewHolder> {


    public List<BlogPost> blog_list;
    public Context context;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    public  BlogRecyclerAdapter(List<BlogPost> blog_list)
    {
         this.blog_list=blog_list;
    }
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_list_item,parent,false);
        context=parent.getContext();
        firebaseAuth=FirebaseAuth.getInstance();
        firebaseFirestore=FirebaseFirestore.getInstance();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {


        holder.setIsRecyclable(false);

        final String blogPostId=blog_list.get(position).BlogPostId;
        final String currentUserId=firebaseAuth.getCurrentUser().getUid();

        String desc_data=blog_list.get(position).getDesc();
        holder.setDescText(desc_data);

        String image_url=blog_list.get(position).getImage_url();
        String thumb_uri=blog_list.get(position).getImage_thumb();
        holder.setBlogImage(image_url,thumb_uri);

        String user_id=blog_list.get(position).getUser_id();
        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if(task.isSuccessful()){

                    String userName = task.getResult().getString("name");
                    String userImage = task.getResult().getString("image");

                    holder.setUserData(userName, userImage);


                } else {

                    holder.setUserData("Unkown",null);

                }

            }
        });

        long milliseconds=blog_list.get(position).getTimestamp().getTime();
        String dateString = DateFormat.format("MM/dd/yyyy",new Date(milliseconds)).toString();
        holder.setDate(dateString);

        //like count real time

        firebaseFirestore.collection("Posts/" + blogPostId + "/Likes").addSnapshotListener( new EventListener<QuerySnapshot>() {
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                if(e==null)
                {
                    if(!documentSnapshots.isEmpty()){

                        int count = documentSnapshots.size();

                        holder.updateLikesCount(count);

                    } else {

                        holder.updateLikesCount(0);

                    }
                }
                }


        });

        //for like icon real time

        firebaseFirestore.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {


                if(e==null)
                {
                    if (documentSnapshot.exists()) {

                        holder.blogLikeBtn.setImageDrawable(ContextCompat.getDrawable(context, R.mipmap.action_like_accent));

                    } else {

                        holder.blogLikeBtn.setImageDrawable(ContextCompat.getDrawable(context, R.mipmap.action_like_gray));

                    }
                }
                }

        });


        //for like button
        holder.blogLikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {


                        if(!task.getResult().exists())
                        {
                            Map<String,Object> likesMap=new HashMap<>();
                            likesMap.put("timestamp", FieldValue.serverTimestamp());

                            firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").document(currentUserId).set(likesMap);
                        }
                        else
                        {
                            firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").document(currentUserId).delete();

                        }

                    }
                });


            }
        });

    }

    @Override
    public int getItemCount() {
        return blog_list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        private View mview;
        private TextView descView;
        private ImageView blogImageView;
        private TextView blogUserName;
        private CircleImageView blogUserImage;
        private TextView blogDate;
        private ImageView blogLikeBtn;
        private TextView blogLikeCount;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mview=itemView;
            blogLikeBtn=mview.findViewById(R.id.blog_like_btn);

        }
        public void setDescText(String descText)
        {
            descView=mview.findViewById(R.id.blog_desc);
            descView.setText(descText);

        }
        public void setBlogImage(String downloadUri ,String thumburi)
        {
            blogImageView=mview.findViewById(R.id.blog_image);
            RequestOptions placeholderOption = new RequestOptions();
            placeholderOption.placeholder(R.drawable.image_placeholder);
            if(context!=null)
            {
                Glide.with(context).applyDefaultRequestOptions(placeholderOption).load(downloadUri)
                        .thumbnail(Glide.with(context).load(thumburi))
                        .into(blogImageView);
            }


        }

        public void setUserData(String userName, String userImage) {
            blogUserImage = mview.findViewById(R.id.blog_user_image);
            blogUserName = mview.findViewById(R.id.blog_user_name);

            blogUserName.setText(userName);

            RequestOptions placeholderOption = new RequestOptions();
            placeholderOption.placeholder(R.drawable.profile_placeholder);

            if(context!=null)
            {
                Glide.with(context).applyDefaultRequestOptions(placeholderOption).load(userImage).into(blogUserImage);

            }

        }
        public void setDate(String date)
        {
            blogDate=mview.findViewById(R.id.blog_date);
            blogDate.setText(date);
        }

        public void updateLikesCount(int count)
        {
            blogLikeCount=mview.findViewById(R.id.blog_like_count);
            String s=""+count;
            blogLikeCount.setText(s);
        }
    }
}
